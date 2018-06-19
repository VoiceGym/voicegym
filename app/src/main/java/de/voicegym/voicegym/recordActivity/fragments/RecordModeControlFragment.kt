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

    var floatingActionButton: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_record_mode_control, container, false)
        floatingActionButton = view.findViewById(R.id.floatingActionButton)
        floatingActionButton?.let {
            it.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            it.setOnClickListener { recordButtonPressed() }
        }
        return view
    }

    private fun recordButtonPressed() {
        when (recordActivity?.isRecording()) {
            false -> recordActivity?.startRecording()
            else  -> recordActivity?.finishRecording()
        }

        recordActivity?.let {
            if (it.isRecording()) {
                floatingActionButton?.backgroundTintList = ColorStateList.valueOf(Color.RED)
            } else {
                floatingActionButton?.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            }
        }
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
}
