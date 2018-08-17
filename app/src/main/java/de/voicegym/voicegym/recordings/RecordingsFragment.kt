package de.voicegym.voicegym.recordings

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.model.Recording
import de.voicegym.voicegym.model.RecordingListViewModel
import java.io.File

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [RecordingsFragment.OnListFragmentInteractionListener] interface.
 */
class RecordingsFragment : Fragment(),
        RecyclerItemTouchHelperListener {

    private var listener: OnListFragmentInteractionListener? = null

    private lateinit var recordingsListViewModel: RecordingListViewModel
    private lateinit var adapter: RecordingsAdapter

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int, position: Int) {
        if (viewHolder is RecordingsAdapter.ViewHolder) {
            val deletedRecording = adapter[position]
            recordingsListViewModel.deleteRecording(deletedRecording)

            // showing snack bar with Undo option
            var undone = false
            Snackbar.make(view!!, deletedRecording.fileName + " removed!", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") { view ->
                        // undo is selected, restore the deleted item
                        recordingsListViewModel.addRecording(deletedRecording)
                        undone = true
                    }
                    .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (!undone) {
                                File(deletedRecording.fileName).delete()
                            }
                        }
                    }
                    )
                    .setActionTextColor(Color.YELLOW)
                    .show()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_recordings_list, container, false) as RecyclerView

        view.layoutManager = LinearLayoutManager(context)
        view.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        view.itemAnimator = DefaultItemAnimator()
        adapter = RecordingsAdapter(emptyList(), listener, listener as SwitchToPlaybackFragmentListener)
        view.adapter = adapter
        val itemTouchHelperCallback = RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this)
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(view)
        recordingsListViewModel = ViewModelProviders.of(this).get(RecordingListViewModel::class.java)
        recordingsListViewModel.recordingsList.observe(this, Observer { recordings ->
            adapter.update(recordings!!)
        })

        return view
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {

        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: Recording)
    }

    interface SwitchToPlaybackFragmentListener {
        fun onClick(fileName: String)
    }
}
