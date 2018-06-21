package de.voicegym.voicegym.recordActivity

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings
import de.voicegym.voicegym.menu.settings.SettingsBundle
import de.voicegym.voicegym.model.AppDatabase
import de.voicegym.voicegym.model.Recording
import de.voicegym.voicegym.recordActivity.RecordActivityState.PLAYBACK
import de.voicegym.voicegym.recordActivity.RecordActivityState.RECORDING
import de.voicegym.voicegym.recordActivity.RecordActivityState.WAITING
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
import de.voicegym.voicegym.util.audio.getDoubleArrayFromShortArray
import de.voicegym.voicegym.util.audio.savePCMInputStreamOnSDCard
import de.voicegym.voicegym.util.math.FourierHelper
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
        instrumentFragment?.resetFragment()
        pcmStorage?.let { recorder?.unSubscribeListener(it) }
        pcmStorage = null
        pcmPlayer?.unSubscribeListener(this)
        pcmPlayer?.destroy()
        pcmPlayer = null
        stopListeningAndFreeRessource()
        // Start up
        recordActivityState = WAITING
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
    private var recordActivityState: RecordActivityState = WAITING

    /**
     * here we store our currently used Feedbackinstrument for example our SpectrogramFragment
     */
    private var instrumentFragment: AbstractInstrumentFragment? = null

    /**
     * RecordModeControlFragment that controls the RecordActivity while being in WAITING or RECORDING MODE
     */
    private val recorderControlFragment = RecordModeControlFragment()


    /**
     * PlaybackModeControlFragment that controls the RecordActivity while being in PLAYBACK MODE
     */
    private val playbackControlFragment = PlaybackModeControlFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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

        // handle fragments
        switchToRecordControlFragment()

        // initialize the instrumentFragment
        instrumentFragment = supportFragmentManager.findFragmentById(R.id.spectrogramFragment) as SpectrogramFragment
        instrumentFragment?.updateFrequencyArray(fourierHelper.frequencyArray())
        instrumentFragment?.updateInstrumentViewSettings(settings)

        // activate the recordhelper to listen to microphone
        startListening()


    }

    override fun onBackPressed() {
        when (recordActivityState) {
            WAITING   -> if (pcmStorage == null) finish()
            RECORDING -> switchToPlayback()
            PLAYBACK  -> restart()
        }
    }

    override fun onDestroy() {
        pcmStorage?.stopListening()
        pcmPlayer?.unSubscribeListener(this)
        pcmPlayer?.destroy()
        this.stopListeningAndFreeRessource()
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
        // Get a handler that can be used to post something on the main thread
        Handler(this.mainLooper).post {
            // execute updateActivity() on the main thread
            updateActivity()
        }
    }

    /**
     * the activity received a new array from the audio recording thread, process that
     * RELEVANT in PLAYBACKMODE
     */
    private fun updateActivity() {
        val shortArray = inputQueue.poll()
        fourierHelper.fft(getDoubleArrayFromShortArray(1.0, shortArray))
        instrumentFragment?.insertNewAmplitudes(fourierHelper.amplitudeArray())
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

    fun getInstrumentFragment(): Fragment? = supportFragmentManager.findFragmentById(R.id.spectrogramFragment)

    /*
    The following methods implement the RecordModeControlListener Interface
     */

    override fun startRecording() {
        if (recordActivityState == WAITING) storePCMSamples()
    }

    override fun finishRecording() {
        if (recordActivityState == RECORDING) switchToPlayback()
    }

    override fun isRecording(): Boolean = when (recordActivityState) {
        WAITING   -> false
        RECORDING -> true
        PLAYBACK  -> false
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
                it.fileName = dateString
                it.duration = size / sampleRate
            })
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

}
