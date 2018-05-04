package de.voicegym.voicegym.activities

import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import de.voicegym.voicegym.R
import de.voicegym.voicegym.activities.ActivityState.RECORDING
import de.voicegym.voicegym.activities.ActivityState.WAITING
import de.voicegym.voicegym.audioHelper.AudioHelper.getDezibelFromAmplitude
import de.voicegym.voicegym.audioHelper.PCMStorage
import de.voicegym.voicegym.audioHelper.RecordBufferListener
import de.voicegym.voicegym.audioHelper.RecordHelper
import de.voicegym.voicegym.fourierHelper.FourierHelper
import de.voicegym.voicegym.fourierHelper.PCMUtil
import de.voicegym.voicegym.views.util.HotGradientColorPicker
import kotlinx.android.synthetic.main.activity_record.dummyView
import kotlinx.android.synthetic.main.activity_record.floatingActionButton
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator
import java.util.concurrent.ConcurrentLinkedQueue


class RecordActivity : AppCompatActivity(), RecordBufferListener {
    // configuration
    val sampleRate = 44100
    val collectedSamples = 8192
    val binning = 2
    val fromFrequency: Double = 10.0
    val tillFrequency: Double = 1000.0
    val numberDataPoints: Int = 200

    // start class fixed objects
    val blockSize = collectedSamples / binning
    val inputQueue = ConcurrentLinkedQueue<ShortArray>()
    val fourierHelper = FourierHelper(blockSize, binning, collectedSamples, sampleRate)
    val interpolator = LinearInterpolator()
    val frequencyArray: DoubleArray = fourierHelper.frequencyArray()

    // variables
    var fromIndexF: Int = 0
    var tillIndexF: Int = 0
    var frequencyRangeArray: DoubleArray? = null
    var recorder: RecordHelper? = null
    var activityState: ActivityState = WAITING

    init {
        onRangeChanged()
    }

    private fun onRangeChanged() {
        if (fromFrequency > tillFrequency) throw RuntimeException("fromFrequency must be smaller than tillFrequency")
        if (frequencyArray[0] > fromFrequency) throw RuntimeException("you choose a frequency below available range")
        if (frequencyArray[frequencyArray.size - 1] < tillFrequency) throw RuntimeException("you choose a frequency above available range")

        // find range for which values are displayed
        var idx = 0
        while (frequencyArray[idx] < fromFrequency) idx++
        fromIndexF = idx - 1 // just keep one datapoint outside of range
        while (frequencyArray[idx] < tillFrequency) idx++
        tillIndexF = idx + 1 // just keep two datapoints outside of range
        frequencyRangeArray = frequencyArray.copyOfRange(fromIndexF, tillIndexF)
    }

    private fun calculateColorArrayForSpectrum(amplitude: DoubleArray, normalizationConstant: Double): IntArray {
        val interpolatingFunction = interpolator.interpolate(frequencyRangeArray, amplitude.copyOfRange(fromIndexF, tillIndexF))
        val colors = IntArray(dummyView.getDrawAreaHeight().toInt())
        val deltaFrequency = (tillFrequency - fromFrequency) / colors.size
        for (i in 0 until colors.size) {
            val amplitudeVal = interpolatingFunction.value(fromFrequency + i * deltaFrequency)
            colors[i] = HotGradientColorPicker.pickColor(getDezibelFromAmplitude(amplitudeVal) / normalizationConstant)
        }
        return colors
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_record)
        dummyView.xDataPoints = numberDataPoints
        dummyView.invalidate()
        Log.i("Activity", "onCreate")
        recorder = RecordHelper(collectedSamples)
        recorder?.subscribeListener(this)
        recorder?.start()
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(Color.GREEN);
        floatingActionButton.setOnClickListener {
            when (activityState) {
                WAITING -> recordToFile()
                RECORDING -> stopRecordToFile()
            }

        }
    }

    override fun onDestroy() {
        recorder?.stopRecording()
        super.onDestroy()
    }

    var pcmStorage: PCMStorage? = null

    private fun recordToFile() {
        activityState = RECORDING
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
        Log.e("RecordActivity", "Switched to record")
        pcmStorage = PCMStorage()
        recorder?.subscribeListener(pcmStorage!!)
    }


    private fun stopRecordToFile() {
        activityState = WAITING
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
        Log.e("RecordActivity", "Back to waiting")
        pcmStorage?.stopListening()
        recorder?.unSubscribeListener(pcmStorage!!)
    }


    override fun canHandleBufferSize(bufferSize: Int): Boolean = (collectedSamples == bufferSize)

    override fun onBufferReady(data: ShortArray) {
        inputQueue.add(data)
        // Get a handler that can be used to post to the main thread
        val myRunnable = Runnable {
            updateActivity()
        };
        Handler(this.getMainLooper()).post(myRunnable);

    }

    private fun updateActivity() {
        val shortArray = inputQueue.poll()
        fourierHelper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, shortArray))
        val spectrum = fourierHelper.amplitudeArray()
        val colors = calculateColorArrayForSpectrum(spectrum, 55.0)
        dummyView.insertColorLine(colors)
        dummyView.invalidate()
    }
}

enum class ActivityState {
    WAITING,
    RECORDING,
    DONE
}
