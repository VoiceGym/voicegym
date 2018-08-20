package de.voicegym.voicegym.recordActivity.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R
import org.jetbrains.anko.runOnUiThread
import kotlin.concurrent.thread


class RecordModeControlFragment : Fragment() {

    var recordActivity: RecordModeControlListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        recordActivity = if (context is RecordModeControlListener) {
            context
        } else {
            throw Error("Needs to be called from a context that implements RecordModeControlListener")
        }
    }

    var recordButton: FloatingActionButton? = null
    var microphoneButton: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_record_mode_control, container, false)
        recordButton = view.findViewById(R.id.recordButton)
        recordButton?.let { button ->
            button.backgroundTintList = ColorStateList.valueOf(resources.getColor(android.R.color.black))
            button.setOnClickListener { recordButtonPressed() }
        }
        microphoneButton = view.findViewById(R.id.microphoneButton)
        microphoneButton?.setOnClickListener { microPhoneButtonPressed() }
        return view
    }

    private fun recordButtonPressed() {
        when (recordActivity?.isRecording()) {
            false -> recordActivity?.startRecording()
            else  -> recordActivity?.finishRecording()
        }

        recordActivity?.let {
            if (it.isRecording()) {
                blink()
            } else {
                stopBlinking()
                recordButton?.backgroundTintList = ColorStateList.valueOf(resources.getColor(android.R.color.black))
            }
        }
    }

    private fun microPhoneButtonPressed() {
        when (recordActivity?.isMicrophoneOn()) {
            true  -> {
                recordActivity?.pauseMicrophone()
                microphoneButton?.setImageDrawable(resources.getDrawable(android.R.drawable.ic_btn_speak_now))
                microphoneButton?.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.item_name))
            }

            false -> {
                recordActivity?.resumeMicrophone()
                microphoneButton?.setImageDrawable(resources.getDrawable(android.R.drawable.ic_btn_speak_now))
                microphoneButton?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#cbcbcb"))
            }
        }

    }


    private var blinkRecording = false
    private var blinkingThread: Thread? = null
    private fun blink() {
        blinkRecording = true
        blinkingThread = thread {
            var switch = true
            while (blinkRecording) {
                when (switch) {
                    true  -> {
                        switch = false
                        context?.runOnUiThread {
                            recordButton?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#df7777"))
                        }
                    }

                    false -> {
                        switch = true
                        context?.runOnUiThread {
                            recordButton?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
                        }

                    }
                }
                Thread.sleep(750)
            }
        }
    }

    private fun stopBlinking() {
        blinkRecording = false
        blinkingThread?.join()
    }

}

interface RecordModeControlListener {
    /**
     * recording button was pressed
     */
    fun startRecording()

    /**
     * recording button was pressed a second time
     */
    fun finishRecording()

    /**
     * control variable to find out whether the listener is currently recording
     */
    fun isRecording(): Boolean

    /**
     * pause taking samples from microphone
     */
    fun pauseMicrophone()

    /**
     * resume taking samples from microphone
     */
    fun resumeMicrophone()

    /**
     * check whether activity is listening to microphone or not
     */
    fun isMicrophoneOn(): Boolean
}
