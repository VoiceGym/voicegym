package de.voicegym.voicegym.recordActivity

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings
import de.voicegym.voicegym.menu.settings.SettingsBundle
import de.voicegym.voicegym.model.AppDatabase
import de.voicegym.voicegym.model.Recording
import de.voicegym.voicegym.recordActivity.RecordActivityState.LIVEVIEW
import de.voicegym.voicegym.recordActivity.RecordActivityState.PLAYBACK
import de.voicegym.voicegym.recordActivity.RecordActivityState.PLAYBACK_FROM_FILE
import de.voicegym.voicegym.recordActivity.RecordActivityState.RECORDING
import de.voicegym.voicegym.recordActivity.TouchPlaybackState.RELEASED
import de.voicegym.voicegym.recordActivity.TouchPlaybackState.TOUCHED
import de.voicegym.voicegym.recordActivity.TouchPlaybackState.TOUCHED_WHILE_PLAYING
import de.voicegym.voicegym.recordActivity.fragments.AbstractInstrumentFragment
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlFragment
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlListener
import de.voicegym.voicegym.recordActivity.fragments.RecordModeControlFragment
import de.voicegym.voicegym.recordActivity.fragments.RecordModeControlListener
import de.voicegym.voicegym.recordActivity.fragments.SpectrogramFragment
import de.voicegym.voicegym.util.RecordBufferListener
import de.voicegym.voicegym.util.audio.PCMPlayer
import de.voicegym.voicegym.util.audio.PCMPlayerListener
import de.voicegym.voicegym.util.audio.PCMStorage
import de.voicegym.voicegym.util.audio.RecordHelper
import de.voicegym.voicegym.util.audio.SoundFile
import de.voicegym.voicegym.util.audio.getDoubleArrayFromShortArray
import de.voicegym.voicegym.util.audio.getVoiceGymFolder
import de.voicegym.voicegym.util.audio.savePCMInputStreamOnSDCard
import de.voicegym.voicegym.util.math.FourierHelper
import kotlinx.android.synthetic.main.fragment_spectrogram.spectrogramView
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue


class RecordActivity : AppCompatActivity(),
        RecordBufferListener,
        RecordModeControlListener,
        PlaybackModeControlListener,
        PCMPlayerListener {

    /**
     * reset the activity and start over from the beginning
     */
    private fun restart() {
        // Clean up
        unLockScreenPosition()
        instrumentFragment?.resetFragment()
        pcmStorage?.let { recorder?.unSubscribeListener(it) }
        pcmStorage = null
        pcmPlayer?.unSubscribeListener(this)
        pcmPlayer?.destroy()
        pcmPlayer = null
        stopListeningAndFreeRessource()
        // Start up
        recordActivityState = LIVEVIEW
        startListening()
        switchToRecordControlFragment()
    }

    /**
     * a queue where incoming PCMSamples from the RecordHelper are stored before they can be processed
     */
    private val inputQueue = ConcurrentLinkedQueue<ShortArray>()

    /**
     * Access to the frequency space
     */
    private lateinit var fourierHelper: FourierHelper

    /**
     * here the FourierInstrumentViewSettings are stored that were active when the activity was started
     */
    private lateinit var settings: FourierInstrumentViewSettings

    /**
     * RecordHelper object that is responsible for accessing the microphone
     */
    private var recorder: RecordHelper? = null

    /**
     * PCMPlayer that will play a recorded set of pcmSamples
     */
    private var pcmPlayer: PCMPlayer? = null
    /**
     * The RecordActivityState the RecordActivity currently resides in
     */
    private var recordActivityState: RecordActivityState = LIVEVIEW

    /**
     * will only be different from null when activity is started with a filename of a stored audio file
     */
    private var filenameForPlaybackFromFile: String? = null

    /**
     * here we store our currently used Feedbackinstrument for example our SpectrogramFragment
     */
    private var instrumentFragment: AbstractInstrumentFragment? = null

    /**
     * RecordModeControlFragment that controls the RecordActivity while being in LIVEVIEW or RECORDING MODE
     */
    private val recorderControlFragment = RecordModeControlFragment()


    /**
     * PlaybackModeControlFragment that controls the RecordActivity while being in PLAYBACK MODE
     */
    private val playbackControlFragment = PlaybackModeControlFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_record)
        window.decorView.setBackgroundColor(Color.BLACK)

        // obtain settings and initialize the FourierHelper
        settings = SettingsBundle.getFourierInstrumentViewSettings(this)
        fourierHelper = FourierHelper(
                settings.blockSize,
                settings.binning,
                settings.samplesPerDatapoint,
                SettingsBundle.sampleRate)


        val recordFileName = if (intent.hasExtra(AUDIO_FILE)) {
            recordActivityState = PLAYBACK_FROM_FILE
            intent.getStringExtra(AUDIO_FILE)
        } else null


        // initialize the instrumentFragment
        instrumentFragment = supportFragmentManager.findFragmentById(R.id.spectrogramFragment) as SpectrogramFragment
        instrumentFragment?.updateFrequencyArray(fourierHelper.frequencyArray())
        instrumentFragment?.updateInstrumentViewSettings(settings)

        when (recordActivityState) {
            LIVEVIEW           -> {
                switchToRecordControlFragment()
                startListening()
            }

            PLAYBACK_FROM_FILE -> {
                switchToPlaybackControlFragment()
                filenameForPlaybackFromFile = recordFileName
            }

        // deactivate sleep mode
        }

        // deactivate sleep mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    }

    /**
     * Places the RecordControlFragment into the controlFragmentSpace (right screen position)
     */
    private fun switchToRecordControlFragment() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.controlFragmentSpace, recorderControlFragment)
                .addToBackStack(null)
                .commit()
    }

    /**
     * Places the PlayBackControlFragment into the controlFragmentSpace (right screen position)
     */
    private fun switchToPlaybackControlFragment() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.controlFragmentSpace, playbackControlFragment)
                .addToBackStack(null)
                .commit()
    }


    override fun onBackPressed() {
        when (recordActivityState) {
            LIVEVIEW           -> if (pcmStorage == null) finish()
            RECORDING          -> switchToPlayback()
            PLAYBACK           -> restart()
            PLAYBACK_FROM_FILE -> finish()
        }
    }

    var wasListeningBeforeStop: Boolean = false

    override fun onPause() {
        super.onPause()
        instrumentFragment?.stopRendering()
    }

    override fun onStop() {
        wasListeningBeforeStop = recorder?.shouldRecord ?: false
        stopListeningAndFreeRessource()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        instrumentFragment?.startRendering()
        when (recordActivityState) {
            LIVEVIEW, RECORDING -> {
                if (wasListeningBeforeStop) startListening()
            }


            PLAYBACK_FROM_FILE  -> {
                instrumentFragment?.spectrogramView?.viewTreeObserver?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        instrumentFragment?.spectrogramView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                        loadFromSdCard(filenameForPlaybackFromFile
                                ?: throw Error("Filename not set while loading from file"))
                    }
                })
            }
        }
    }


    override fun onDestroy() {
        this.stopListeningAndFreeRessource()
        pcmStorage?.stopListening()
        pcmPlayer?.unSubscribeListener(this)
        pcmPlayer?.destroy()
        super.onDestroy()
    }

    /**
     * Creates a RecordHelper object, that tries to gain access to the microphone and then starts
     * listening to the microphone
     */
    private fun startListening() {
        // start microphone listening
        recorder = RecordHelper(settings.samplesPerDatapoint)
        recorder?.let {
            it.subscribeListener(this)
            it.start()
        }
    }

    /**
     * stops the RecordHelper, releases the Microphone and then deletes the RecordHelper
     */
    private fun stopListeningAndFreeRessource() {
        recorder?.let {
            it.unSubscribeListener(this)
            it.stopRecording()
        }
        recorder = null
    }

    private var pcmStorage: PCMStorage? = null

    /**
     * sets the RecordActivity into RECORDING_MODE and also starts collecting of audio samples
     */
    private fun storePCMSamples() {
        recordActivityState = RECORDING
        lockScreenPosition()
        pcmStorage = PCMStorage(SettingsBundle.sampleRate)
        instrumentFragment?.startRecording() // from here on the instrumentFragment will keep the pcmSamples
        pcmStorage?.let { recorder?.subscribeListener(it) }  // from here on the pcmStorage will collect pcm samples
    }

    /**
     * the datestring of the finished record
     */
    private var dateString = ""

    /**
     *  finish-up recording switch to playback
     */
    private fun switchToPlayback() {
        pcmStorage?.let { storage ->
            recordActivityState = PLAYBACK
            storage.stopListening() // from here pcmStorage won't accept new samples
            this.stopListeningAndFreeRessource() // from here RecordActivity won't accept new pcmSamples
            recorder?.unSubscribeListener(storage)
            dateString = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ENGLISH).format(Calendar.getInstance().time)
            switchToPlaybackControlFragment()
            instrumentFragment?.doneRecordingSwitchToPlayback()
            instrumentFragment?.cutToMaximumSampleNumber(storage.size)

            pcmPlayer = PCMPlayer(storage.sampleRate, storage.asShortBuffer(), this)
            pcmPlayer?.subscribeListener(this)
        }
    }

    /**
     * is called by the audio player (PCMPlayer) to signal the currently played position within the displayed record
     * RELEVANT in PLAYBACKMODE
     */
    override fun isAtPosition(sampleNumber: Int) {
        instrumentFragment?.seekToSamplePosition(sampleNumber)
    }

    override fun canHandleBufferSize(bufferSize: Int): Boolean = (settings.samplesPerDatapoint == bufferSize)

    /**
     * callback for the audio recording thread, still executed on the recording thread
     * RELEVANT in RECORDMODE
     */
    override fun onBufferReady(data: ShortArray) {
        inputQueue.add(data)
        updateActivity()
    }

    /**
     * the activity received a new array from the audio recording thread, process that
     * RELEVANT in PLAYBACKMODE
     */
    private fun updateActivity() {
        val shortArray = inputQueue.poll()
        fourierHelper.fft(getDoubleArrayFromShortArray(1.0, shortArray))
        instrumentFragment?.insertNewAmplitudes(fourierHelper.amplitudeArray())
        instrumentFragment?.invalidateFromBackground()
    }


    fun getInstrumentFragment(): Fragment? = supportFragmentManager.findFragmentById(R.id.spectrogramFragment)

    /*
    The following methods implement the RecordModeControlListener Interface
     */

    override fun startRecording() {
        if (recordActivityState == LIVEVIEW) storePCMSamples()
    }

    override fun finishRecording() {
        if (recordActivityState == RECORDING) switchToPlayback()
    }

    override fun isRecording(): Boolean = when (recordActivityState) {
        LIVEVIEW           -> false
        RECORDING          -> true
        PLAYBACK           -> false
        PLAYBACK_FROM_FILE -> false
    }


    /*
     The following methods implement the PlaybackModeControlListener Interface
     */

    override fun playPause() {
        pcmPlayer?.let {
            when (it.playing) {
                true  -> it.stop()
                false -> it.play()
            }
        }
    }

    override fun openRatingDialog() {
        val ratingDialog = RatingDialog(this)
        ratingDialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        ratingDialog.playbackModeControlListener = this
        ratingDialog.show()
    }

    override fun receiveRating(rating: Int) {
        //TODO get rating into datamodel

        Log.i("Rated", "Record was rated $rating")
    }

    override fun saveToSdCard() {
        launch(CommonPool) {
            pcmStorage?.let {
                it.rewind()
                savePCMInputStreamOnSDCard(dateString, it, it.sampleRate, 128000)
            }

            val recordingDao = AppDatabase.getInstance().recordingDao()
            val size = pcmStorage?.size ?: 0
            val sampleRate = pcmStorage?.sampleRate ?: 41000
            recordingDao.insert(Recording().also {
                it.fileName = getVoiceGymFolder()?.absolutePath + "/${dateString}.m4a"
                it.duration = size / sampleRate
            })
        }
    }

    private fun loadFromSdCard(fileName: String) {
        Log.i("RecordActivity", "Load from SDCard called")
        val soundFile = SoundFile.create(fileName, null)
        if (soundFile != null) {
            val samplesBuffer = soundFile.samples
                    ?: throw Error("Error probably while processing Audiofile. Samples null.")

            pcmStorage = PCMStorage(soundFile.sampleRate)
            instrumentFragment?.startRecording()

            while (samplesBuffer.hasRemaining()) {
                val samplesArray = ShortArray(settings.samplesPerDatapoint)
                samplesBuffer.get(samplesArray)
                pcmStorage?.onBufferReady(samplesArray)
                onBufferReady(samplesArray.copyOf())
            }
            pcmStorage?.stopListening()

            instrumentFragment?.doneRecordingSwitchToPlayback()

            pcmPlayer = PCMPlayer(soundFile.sampleRate, pcmStorage!!.asShortBuffer(), this)


        }
    }
    /*
    The following variables and functions handle TouchEvents during PlaybackMode
     */

    // control variable to remember whether the player was active before touching the screen
    private var playbackState = RELEASED

    override fun playbackTouched() {
        playbackState = when {
            pcmPlayer?.playing == true -> TOUCHED_WHILE_PLAYING
            else                       -> TOUCHED
        }

        if (playbackState == TOUCHED_WHILE_PLAYING) playPause()


        startingPosition = instrumentFragment?.getCurrentSamplePosition() ?: 0

    }

    /**
     * position on screen where the first TouchEvent was called
     */
    private var startingPosition: Int = 0

    /**
     * position on screen where the series of touchevents is currently targeting
     */
    private var targetSamplePosition: Int = 0

    override fun playbackSeekTo(relativeMovement: Float) {
        if (playbackState != RELEASED) {

            instrumentFragment?.let {
                val relativeSamples = (relativeMovement * it.settings.displayedDatapoints * it.settings.samplesPerDatapoint).toInt()
                targetSamplePosition = startingPosition - relativeSamples
                it.seekToSamplePosition(targetSamplePosition)
            }
        }
    }

    override fun playbackReleased() {
        if (playbackState != RELEASED) {
            instrumentFragment?.seekToSamplePosition(targetSamplePosition)
            pcmPlayer?.seekTo(targetSamplePosition)
            if (playbackState == TOUCHED_WHILE_PLAYING) playPause()
            playbackState = RELEASED
        }
    }

    fun lockScreenPosition() {

        val rotation = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        requestedOrientation =
                when (rotation) {
                    Surface.ROTATION_0   -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_90  -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    else                 -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }

    }

    fun unLockScreenPosition() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }


    companion object {
        const val AUDIO_FILE = "recordAudioFileName"
    }
}
