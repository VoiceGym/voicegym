package de.voicegym.voicegym.recordActivity

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import de.voicegym.voicegym.R
import de.voicegym.voicegym.recordActivity.RecordActivityState.PLAYBACK
import de.voicegym.voicegym.recordActivity.RecordActivityState.RECORDING
import de.voicegym.voicegym.recordActivity.RecordActivityState.WAITING
import de.voicegym.voicegym.recordActivity.TouchPlaybackState.*
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlFragment
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlListener
import de.voicegym.voicegym.recordActivity.fragments.RecordModeControlFragment
import de.voicegym.voicegym.recordActivity.fragments.RecordeModeControlListener
import de.voicegym.voicegym.recordActivity.fragments.SpectrogramFragment
import de.voicegym.voicegym.recordActivity.fragments.UserSettings
import de.voicegym.voicegym.recordActivity.views.SpectrogramViewState
import de.voicegym.voicegym.util.RecordBufferListener
import de.voicegym.voicegym.util.audio.PCMPlayer
import de.voicegym.voicegym.util.audio.PCMPlayerListener
import de.voicegym.voicegym.util.audio.PCMStorage
import de.voicegym.voicegym.util.audio.RecordHelper
import de.voicegym.voicegym.util.audio.getDoubleArrayFromShortArray
import de.voicegym.voicegym.util.audio.savePCMInputStreamOnSDCard
import de.voicegym.voicegym.util.math.FourierHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class RecordActivity : AppCompatActivity(),
        RecordBufferListener,
        RecordeModeControlListener,
        PlaybackModeControlListener,
        PCMPlayerListener {

    // control variable to remember whether the player was active before touching the screen
    private var playbackState = RELEASED

    override fun playbackTouched() {
        playbackState = when {
            pcmPlayer?.playing == true -> TOUCHED_WHILE_PLAYING
            else                       -> TOUCHED
        }

        if (playbackState == TOUCHED_WHILE_PLAYING) playPause()

        // TODO REMOVE DIRECT ACCESS TO SPECTROGRAMVIEW
        if (instrumentFragment != null && instrumentFragment!!.spectrogramView != null && instrumentFragment!!.userSettings != null) {
            startingPosition = instrumentFragment!!.spectrogramView!!.currentDequePosition * instrumentFragment?.userSettings!!.samplesPerDatapoint
        } else Error("Problem setting up the activity, missing spectrogramView, Fragment or settings")
    }

    private var startingPosition: Int = 0;
    private var targetSamplePosition: Int = 0

    override fun playbackSeekTo(relativeMovement: Float) {
        if (playbackState != RELEASED) {

            //TODO REMOVE DIRECT ACCESS
            instrumentFragment?.let {
                val relativeSamples = (relativeMovement * it.userSettings!!.numberDataPoints * it.userSettings!!.samplesPerDatapoint).toInt()
                targetSamplePosition = startingPosition - relativeSamples
                it.spectrogramView?.seekTo(targetSamplePosition)
                it.spectrogramView?.invalidate()
            }
        }
    }

    override fun playbackReleased() {
        if (playbackState != RELEASED) {
            //TODO REMOVE DIRECT ACCESS
            instrumentFragment?.spectrogramView?.let {
                it.seekTo(targetSamplePosition)
                it.invalidate()
            }

            pcmPlayer?.seekTo(targetSamplePosition)

            if (playbackState == TOUCHED_WHILE_PLAYING) playPause()
            playbackState = RELEASED
        }
    }

    override fun playPause() {
        pcmPlayer?.let {
            when (it.playing) {
                true  -> it.stop()
                false -> it.play()
            }
        }
    }

    override fun rate() {

    }

    override fun saveToSdCard() {
        thread {
            pcmStorage?.let {
                it.rewind()
                savePCMInputStreamOnSDCard(dateString, it, it.sampleRate, 128000)
            }
        }
    }

    fun restart() {
        // Clean up
        //TODO REMOVE DIRECT ACCESS
        instrumentFragment?.spectrogramView?.let {
            it.clearBitmapAndBuffer()
            it.spectrogramViewState = SpectrogramViewState.LIVE_DISPLAY
            it.invalidate()
        }
        pcmStorage?.let { recorder?.unSubscribeListener(it) }
        pcmStorage = null
        pcmPlayer?.unSubscribeListener(this)
        pcmPlayer?.destroy()
        pcmPlayer = null
        stopListeningAndFreeRessource()
        // Start up
        recorderState = WAITING
        startListening()
        switchToRecordControlFragment()
    }

    override fun toggleRecordMode() {
        when (recorderState) {
            WAITING   -> storePCMSamples()
            RECORDING -> switchToPlayback()
        }
    }

    override fun isRecording(): Boolean = when (recorderState) {
        WAITING   -> false
        RECORDING -> true
        PLAYBACK  -> false
    }


    // configuration
    val sampleRate = 44100
    val samplesPerDatapoint = 8192
    val binning = 2


    // start class fixed objects
    private val blockSize = samplesPerDatapoint / binning
    private val inputQueue = ConcurrentLinkedQueue<ShortArray>()
    private val fourierHelper = FourierHelper(blockSize, binning, samplesPerDatapoint, sampleRate)


    private var recorder: RecordHelper? = null
    private var pcmPlayer: PCMPlayer? = null
    private var recorderState: RecordActivityState = WAITING

    val spectrogramBundle = Bundle()

    var instrumentFragment: SpectrogramFragment? = null
    val recorderControlFragment = RecordModeControlFragment()
    val playbackControlFragment = PlaybackModeControlFragment()

    init {
        spectrogramBundle.putDoubleArray("frequencyArray", fourierHelper.frequencyArray())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_record)
        window.decorView.setBackgroundColor(Color.BLACK)

        // handle fragments
        switchToRecordControlFragment()
        instrumentFragment = supportFragmentManager.findFragmentById(R.id.spectrogramFragment) as SpectrogramFragment
        instrumentFragment?.let {
            it.arguments = spectrogramBundle
        }
        startListening()
    }


    override fun onBackPressed() {
        when (recorderState) {
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

    private fun startListening() {
        // start microphone listening
        recorder = RecordHelper(samplesPerDatapoint)
        recorder?.let {
            it.subscribeListener(this)
            it.start()
        }
        instrumentFragment?.updateUserSettings(UserSettings(10.0, 1000.0, 100, samplesPerDatapoint))

    }

    private fun stopListeningAndFreeRessource() {
        recorder?.let {
            it.unSubscribeListener(this)
            it.stopRecording()
        }
        recorder = null
    }


    var pcmStorage: PCMStorage? = null

    private fun storePCMSamples() {
        recorderState = RECORDING
        pcmStorage = PCMStorage(sampleRate)
        recorder?.subscribeListener(pcmStorage!!)
        // TODO REFACTOR the explicit access out of here -> INTERFACE?
        instrumentFragment?.spectrogramView?.spectrogramViewState = SpectrogramViewState.KEEP_SAMPLES
    }


    var dateString = ""

    private fun switchToPlayback() {
        pcmStorage?.let {
            recorderState = PLAYBACK
            it.stopListening()
            this.stopListeningAndFreeRessource()
            recorder?.unSubscribeListener(it)
            dateString = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ENGLISH).format(Calendar.getInstance().time)
            switchToPlaybackControlFragment()

            // TODO REFACTOR the explicit access out of here -> INTERFACE?
            instrumentFragment?.spectrogramView?.let {
                it.rewindDequesToStart()
                it.clearBitmapAndBuffer()
                it.forwardWindDequesToEnd()
                it.spectrogramViewState = SpectrogramViewState.PLAYBACK
                it.invalidate()
            }

            pcmPlayer = PCMPlayer(it.sampleRate, it.asShortBuffer(), this)
            pcmPlayer?.subscribeListener(this)

        }
    }

    private var lastPosition = 0

    override fun isAtPosition(sampleNumber: Int) {
        lastPosition = sampleNumber
        //TODO REMOVE DIRECT ACCESS
        instrumentFragment?.spectrogramView?.let {
            it.seekTo(sampleNumber)
            it.invalidate()
        }

    }

    override fun canHandleBufferSize(bufferSize: Int): Boolean = (samplesPerDatapoint == bufferSize)

    override fun onBufferReady(data: ShortArray) {
        inputQueue.add(data)
        // Get a handler that can be used to post to the main thread
        Handler(this.mainLooper).post {
            updateActivity()
        }
    }

    private fun updateActivity() {
        val shortArray = inputQueue.poll()
        fourierHelper.fft(getDoubleArrayFromShortArray(1.0, shortArray))
        instrumentFragment?.insertNewAmplitudes(fourierHelper.amplitudeArray())
    }


    private fun switchToRecordControlFragment() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.controlFragmentSpace, recorderControlFragment)
                .addToBackStack(null)
                .commit()
    }


    private fun switchToPlaybackControlFragment() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.controlFragmentSpace, playbackControlFragment)
                .addToBackStack(null)
                .commit()
    }

    fun getInstrumentFragment() = supportFragmentManager.findFragmentById(R.id.spectrogramFragment)
}

enum class RecordActivityState {
    WAITING,
    RECORDING,
    PLAYBACK
}


enum class TouchPlaybackState {
    TOUCHED,
    TOUCHED_WHILE_PLAYING,
    RELEASED
}
