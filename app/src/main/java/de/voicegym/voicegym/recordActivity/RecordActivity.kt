package de.voicegym.voicegym.recordActivity

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings
import de.voicegym.voicegym.menu.settings.SettingBundle
import de.voicegym.voicegym.menu.settings.SettingBundle.sampleRate
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class RecordActivity : AppCompatActivity(),
        RecordBufferListener,
        RecordModeControlListener,
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


        startingPosition = instrumentFragment?.getCurrentSamplePosition() ?: 0

    }

    private var startingPosition: Int = 0
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

    override fun playPause() {
        pcmPlayer?.let {
            when (it.playing) {
                true  -> it.stop()
                false -> it.play()
            }
        }
    }

    override fun rate() {
        var rating = 0
        val ratingDialog = RatingDialog(this);
        ratingDialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        ratingDialog.show();
    }

    override fun saveToSdCard() {
        thread {
            pcmStorage?.let {
                it.rewind()
                savePCMInputStreamOnSDCard(dateString, it, it.sampleRate, 128000)
            }
        }
    }

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
        recorderState = WAITING
        startListening()
        switchToRecordControlFragment()
    }

    override fun startRecording() {
        if (recorderState == WAITING) storePCMSamples()
    }

    override fun finishRecording() {
        if (recorderState == RECORDING) switchToPlayback()
    }

    override fun isRecording(): Boolean = when (recorderState) {
        WAITING   -> false
        RECORDING -> true
        PLAYBACK  -> false
    }


    private val inputQueue = ConcurrentLinkedQueue<ShortArray>()

    private var fourierHelper = FourierHelper(4096, 2, 8192, sampleRate)

    private var settings: FourierInstrumentViewSettings? = null

    private var recorder: RecordHelper? = null
    private var pcmPlayer: PCMPlayer? = null
    private var recorderState: RecordActivityState = WAITING

    private var instrumentFragment: AbstractInstrumentFragment? = null
    private val recorderControlFragment = RecordModeControlFragment()
    private val playbackControlFragment = PlaybackModeControlFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_record)
        window.decorView.setBackgroundColor(Color.BLACK)

        settings = SettingBundle.getFourierInstrumentViewSettings(this)
        fourierHelper = settings?.let { FourierHelper(it.blockSize, it.binning, it.samplesPerDatapoint, sampleRate) } ?: throw Error("Settings were not obtained")

        // handle fragments
        switchToRecordControlFragment()
        instrumentFragment = supportFragmentManager.findFragmentById(R.id.spectrogramFragment) as SpectrogramFragment
        instrumentFragment?.updateFrequencyArray(fourierHelper.frequencyArray())
        settings?.let {
            instrumentFragment?.updateInstrumentViewSettings(it)
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
        recorder = RecordHelper(settings?.samplesPerDatapoint
                ?: throw Error("settings not obtained"))
        recorder?.let {
            it.subscribeListener(this)
            it.start()
        }
    }

    private fun stopListeningAndFreeRessource() {
        recorder?.let {
            it.unSubscribeListener(this)
            it.stopRecording()
        }
        recorder = null
    }


    private var pcmStorage: PCMStorage? = null

    private fun storePCMSamples() {
        recorderState = RECORDING
        pcmStorage = PCMStorage(sampleRate)
        recorder?.subscribeListener(pcmStorage!!)
        instrumentFragment?.startRecording()
    }


    private var dateString = ""

    private fun switchToPlayback() {
        pcmStorage?.let {
            recorderState = PLAYBACK
            it.stopListening()
            this.stopListeningAndFreeRessource()
            recorder?.unSubscribeListener(it)
            dateString = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ENGLISH).format(Calendar.getInstance().time)
            switchToPlaybackControlFragment()
            instrumentFragment?.doneRecordingSwitchToPlayback()

            pcmPlayer = PCMPlayer(it.sampleRate, it.asShortBuffer(), this)
            pcmPlayer?.subscribeListener(this)

        }
    }

    override fun isAtPosition(sampleNumber: Int) {
        instrumentFragment?.seekToSamplePosition(sampleNumber)
    }

    override fun canHandleBufferSize(bufferSize: Int): Boolean = (settings?.samplesPerDatapoint == bufferSize)

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

    fun getInstrumentFragment(): Fragment? = supportFragmentManager.findFragmentById(R.id.spectrogramFragment)
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
