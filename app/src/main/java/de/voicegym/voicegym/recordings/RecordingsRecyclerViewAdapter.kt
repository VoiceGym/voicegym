package de.voicegym.voicegym.recordings


import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.recordings.RecordingsFragment.OnListFragmentInteractionListener
import de.voicegym.voicegym.model.Recording
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_recordings.createdView
import kotlinx.android.synthetic.main.fragment_recordings.durationView
import kotlinx.android.synthetic.main.fragment_recordings.floatingActionButton2

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class RecordingsRecyclerViewAdapter(
        private val values: MutableList<Recording>,
        private val listener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<RecordingsRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: OnClickListener

    init {
        mOnClickListener = OnClickListener { v ->
            val item = v.tag as Recording
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            listener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_recordings, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.bindRecording(item)

        with(holder.containerView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = values.size

    fun removeItem(position: Int) {
        values.removeAt(position)
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position)
    }

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bindRecording(recording: Recording) {
            durationView.text = "${recording.duration}s"
            createdView.text = recording.fileName
                    .split("/")
                    .last()
                    .replace("_"," ")
                    .dropLast(4)
//            nameView.text = recording.id.toString()
            floatingActionButton2.setOnClickListener {
                Snackbar.make(it, recording.fileName, Snackbar.LENGTH_SHORT ).show()
            }
        }
    }
}
