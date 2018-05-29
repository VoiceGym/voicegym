package de.voicegym.voicegym.recordActivity

import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import de.voicegym.voicegym.R
import de.voicegym.voicegym.recordActivity.ActivityState.DONE
import de.voicegym.voicegym.recordActivity.ActivityState.RECORDING
import de.voicegym.voicegym.recordActivity.ActivityState.WAITING
import de.voicegym.voicegym.recordActivity.fragments.SpectrogramFragment
import de.voicegym.voicegym.util.RecordBufferListener
import de.voicegym.voicegym.util.audio.PCMStorage
import de.voicegym.voicegym.util.audio.RecordHelper
import de.voicegym.voicegym.util.audio.getDoubleArrayFromShortArray
import de.voicegym.voicegym.util.audio.savePCMInputStreamOnSDCard
import de.voicegym.voicegym.util.math.FourierHelper
import kotlinx.android.synthetic.main.activity_record.floatingActionButton
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class RecordActivity : AppCompatActivity(), RecordBufferListener {
    // configuration
    private val sampleRate = 44100
    private val collectedSamples = 8192
    private val binning = 2

    // start class fixed objects
    private val blockSize = collectedSamples / binning
    private val inputQueue = ConcurrentLinkedQueue<ShortArray>()
    private val fourierHelper = FourierHelper(blockSize, binning, collectedSamples, sampleRate)


    private var recorder: RecordHelper? = null
    private var activityState: ActivityState = WAITING

    val spectrogramBundle = Bundle()

    var spectrogramFragment: SpectrogramFragment? = null

    init {
        spectrogramBundle.putDoubleArray("frequencyArray", fourierHelper.frequencyArray())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_record)
        window.decorView.setBackgroundColor(Color.BLACK)

        spectrogramFragment = supportFragmentManager.findFragmentById(R.id.spectrogramFragment) as SpectrogramFragment
        spectrogramFragment?.let {
            it.arguments = spectrogramBundle
        }
        recorder = RecordHelper(collectedSamples)
        recorder?.let {
            it.subscribeListener(this)
            it.start()
        }


        floatingActionButton.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
        floatingActionButton.setOnClickListener {
            when (activityState) {
                WAITING   -> storePCMSamples()
                RECORDING -> saveStoredPCMSamplesOnSDCard()
                DONE      -> throw NotImplementedError()
            }
        }
    }


    override fun onDestroy() {
        recorder?.stopRecording()
        super.onDestroy()
    }

    private var pcmStorage: PCMStorage? = null

    private fun storePCMSamples() {
        activityState = RECORDING
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
        Log.e("RecordActivity", "Switched to record")
        pcmStorage = PCMStorage(sampleRate)
        recorder?.subscribeListener(pcmStorage!!)
    }

    private fun saveStoredPCMSamplesOnSDCard() {
        if (pcmStorage != null) {
            activityState = WAITING
            floatingActionButton.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            Log.e("RecordActivity", "Back to waiting")
            pcmStorage!!.stopListening()
            recorder?.unSubscribeListener(pcmStorage!!)
            thread {
                savePCMInputStreamOnSDCard(pcmStorage!!, pcmStorage!!.sampleRate, 128000)
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


}

enum class ActivityState {
    WAITING,
    RECORDING,
    DONE
}


