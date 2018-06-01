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
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlFragment
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlListener
import de.voicegym.voicegym.recordActivity.fragments.RecordModeControlFragment
import de.voicegym.voicegym.recordActivity.fragments.RecordeModeControlListener
import de.voicegym.voicegym.recordActivity.fragments.SpectrogramFragment
import de.voicegym.voicegym.util.RecordBufferListener
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

class RecordActivity : AppCompatActivity(), RecordBufferListener, RecordeModeControlListener, PlaybackModeControlListener {

    override fun playPause() {

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
        pcmStorage?.let { recorder?.unSubscribeListener(it) }
        pcmStorage = null
        stopListeningAndFreeRessource()
        // Start up
        recorderState = WAITING
        startListening()
        switchToRecordControlFragment()
    }

    override fun toggleRecordMode() {
        when (recorderState) {
            WAITING   -> storePCMSamples()
            RECORDING -> doneRecordingSwitchToPlayback()
        }
    }

    override fun isRecording(): Boolean = when (recorderState) {
        WAITING   -> false
        RECORDING -> true
        PLAYBACK  -> false
    }


    // configuration
    val sampleRate = 44100
    val collectedSamples = 8192
    val binning = 2


    // start class fixed objects
    private val blockSize = collectedSamples / binning
    private val inputQueue = ConcurrentLinkedQueue<ShortArray>()
    private val fourierHelper = FourierHelper(blockSize, binning, collectedSamples, sampleRate)


    private var recorder: RecordHelper? = null
    private var recorderState: RecordActivityState = WAITING

    val spectrogramBundle = Bundle()

    var spectrogramFragment: SpectrogramFragment? = null
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
        spectrogramFragment = supportFragmentManager.findFragmentById(R.id.spectrogramFragment) as SpectrogramFragment
        spectrogramFragment?.let {
            it.arguments = spectrogramBundle
        }

        startListening()
    }


    override fun onBackPressed() {
        when (recorderState) {
            WAITING   -> if (pcmStorage == null) finish()
            RECORDING -> doneRecordingSwitchToPlayback()
            PLAYBACK  -> restart()
        }
    }

    override fun onDestroy() {
        pcmStorage?.stopListening()
        this.stopListeningAndFreeRessource()
        super.onDestroy()
    }

    private fun startListening() {
        // start microphone listening
        recorder = RecordHelper(collectedSamples)
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
    }


    var dateString = ""

    private fun doneRecordingSwitchToPlayback() {
        pcmStorage?.let {
            recorderState = PLAYBACK
            it.stopListening()
            this.stopListeningAndFreeRessource()
            recorder?.unSubscribeListener(it)
            dateString = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ENGLISH).format(Calendar.getInstance().time)
            switchToPlaybackControlFragment()

            spectrogramFragment?.spectrogramView?.let {
                it.rewindDequesSkippingImage()
                it.clearBitmapAndBuffer()
                val samples = pcmStorage?.size ?: 0
                it.windForward(samples)
                it.invalidate()
            }

        }
    }


    override fun canHandleBufferSize(bufferSize: Int): Boolean = (collectedSamples == bufferSize)

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
        spectrogramFragment?.insertNewAmplitudes(fourierHelper.amplitudeArray())
    }


    private fun switchToRecordControlFragment() = supportFragmentManager.beginTransaction().let {
        it.replace(R.id.controlFragmentSpace, recorderControlFragment)
        it.addToBackStack(null)
        it.commit()
    }

    private fun switchToPlaybackControlFragment() = supportFragmentManager.beginTransaction().let {
        it.replace(R.id.controlFragmentSpace, playbackControlFragment)
        it.addToBackStack(null)
        it.commit()
    }

    fun getInstrumentFragment() = supportFragmentManager.findFragmentById(R.id.spectrogramFragment)
}

enum class RecordActivityState {
    WAITING,
    RECORDING,
    PLAYBACK
}


