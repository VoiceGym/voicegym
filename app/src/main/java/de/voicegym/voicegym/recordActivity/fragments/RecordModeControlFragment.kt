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
    var recordActivity: RecordeModeControlListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        recordActivity = if (context is RecordeModeControlListener) {
            context
        } else {
            throw Error("Needs to be called from a context that implements RecordModeControlListener")
        }
    }

    var floatingActionButton: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_record_mode_control, container, false)
        floatingActionButton = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)
        floatingActionButton?.let {
            it.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            it.setOnClickListener { toggleState() }
        }
        return view
    }

    fun toggleState() {
        recordActivity?.let {
            it.toggleRecordMode()
            if (it.isRecording()) {
                floatingActionButton?.backgroundTintList = ColorStateList.valueOf(Color.RED)
            } else {
                floatingActionButton?.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            }
        }
    }


}

interface RecordeModeControlListener {
    fun toggleRecordMode()

    fun isRecording(): Boolean
}
