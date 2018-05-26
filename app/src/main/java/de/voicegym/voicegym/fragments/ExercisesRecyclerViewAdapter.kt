package de.voicegym.voicegym.fragments


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.voicegym.voicegym.R
import de.voicegym.voicegym.fragments.ExercisesFragment.OnListFragmentInteractionListener
import de.voicegym.voicegym.fragments.dummy.ExerciseContent.ExerciseItem
import kotlinx.android.synthetic.main.fragment_exercises.view.content
import kotlinx.android.synthetic.main.fragment_exercises.view.item_number

/**
 * [RecyclerView.Adapter] that can display a [ExerciseItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class ExercisesRecyclerViewAdapter(
        private val mValues: List<ExerciseItem>,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<ExercisesRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as ExerciseItem
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_exercises, parent, false)
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
