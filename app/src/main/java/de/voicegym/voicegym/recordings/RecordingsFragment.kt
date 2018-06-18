package de.voicegym.voicegym.recordings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.model.AppDatabase
import de.voicegym.voicegym.model.Recording
import kotlinx.android.synthetic.main.fragment_recordings_list.recordingsList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [RecordingsFragment.OnListFragmentInteractionListener] interface.
 */
class RecordingsFragment : Fragment(),
        RecyclerItemTouchHelperListener {

    val recordings = mutableListOf<Recording>()
    lateinit var  adapter: RecordingsRecyclerViewAdapter

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int, position: Int) {
        if (viewHolder is RecordingsRecyclerViewAdapter.ViewHolder) {
            // get the removed item name to display it in snack bar
            val name = recordings[viewHolder.getAdapterPosition()].fileName

            // backup of removed item for undo purpose
            val deletedItem = recordings.get(viewHolder.getAdapterPosition());
            val deletedIndex = viewHolder.getAdapterPosition();

            // remove the item from recycler view
            adapter.removeItem(viewHolder.getAdapterPosition());

//            // showing snack bar with Undo option
            val snackbar = Snackbar
                    .make(recordingsList, name + " removed from cart!", Snackbar.LENGTH_LONG).show()
//            snackbar.setAction("UNDO", view ->
//
//                    // undo is selected, restore the deleted item
//                    adapter.restoreItem(deletedItem, deletedIndex);
//
//            )
//            snackbar.setActionTextColor(Color.YELLOW);
//            snackbar.show();
        }
    }

    // TODO: Customize parameters
    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_recordings_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            view.layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
            view.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            val launch = launch(UI) {
                val dbRecordings = async(CommonPool) {
                    db.recordingDao().getAll()
                }.await()
                recordings.addAll(dbRecordings)
                view.adapter = RecordingsRecyclerViewAdapter(recordings.sortedByDescending { it.createdAt }.toMutableList(), listener)
                adapter = view.adapter as RecordingsRecyclerViewAdapter
            }
            view.itemAnimator = DefaultItemAnimator()
            val itemTouchHelperCallback = RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this)
            ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(view)

        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        db = AppDatabase.getInstance(context)!!

        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
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

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
                RecordingsFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }
}
