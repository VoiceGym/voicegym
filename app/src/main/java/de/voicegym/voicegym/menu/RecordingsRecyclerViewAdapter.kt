package de.voicegym.voicegym.menu


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.RecordingsFragment.OnListFragmentInteractionListener
import de.voicegym.voicegym.menu.dummy.RecordingsContent.RecordingItem
import kotlinx.android.synthetic.main.fragment_recordings.view.content
import kotlinx.android.synthetic.main.fragment_recordings.view.item_number

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class RecordingsRecyclerViewAdapter(
        private val mValues: List<RecordingItem>,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<RecordingsRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: OnClickListener

    init {
        mOnClickListener = OnClickListener { v ->
            val item = v.tag as RecordingItem
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_recordings, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mIdView.text = item.id
        holder.mContentView.text = item.content

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.item_number
        val mContentView: TextView = mView.content

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
