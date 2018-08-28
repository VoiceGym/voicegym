package de.voicegym.voicegym.recordings


import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.model.Recording
import de.voicegym.voicegym.recordings.RecordingsFragment.OnListFragmentInteractionListener
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_recordings.createdView
import kotlinx.android.synthetic.main.fragment_recordings.durationView
import kotlinx.android.synthetic.main.fragment_recordings.floatingActionButton2
import kotlinx.android.synthetic.main.fragment_recordings.star1
import kotlinx.android.synthetic.main.fragment_recordings.star2
import kotlinx.android.synthetic.main.fragment_recordings.star3
import java.text.SimpleDateFormat

/**
 * [RecyclerView.Adapter] that can display a [Recording] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class RecordingsAdapter(
        private val context: Context,
        private var values: List<Recording>,
        private val listener: OnListFragmentInteractionListener?,
        private val switchToPlaybackFragmentListener: RecordingsFragment.SwitchToPlaybackFragmentListener)
    : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {


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
        return ViewHolder(context, view, switchToPlaybackFragmentListener)
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

    fun update(recordings: List<Recording>) {
        this.values = recordings
        notifyDataSetChanged()
    }

    operator fun get(position: Int): Recording = values[position]

    class ViewHolder(
            private val context: Context, override val containerView: View, private val listener: RecordingsFragment.SwitchToPlaybackFragmentListener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bindRecording(recording: Recording) {

            val durationText = context.resources.getString(R.string.durationText)

            durationView.text = durationText + " ${recording.duration}s"
            val dateString = recording.fileName
                    .split("/")
                    .last()
                    .replace("_", " ")
                    .dropLast(4)

            val recordedText = context.resources.getString(R.string.recordedText)
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").parse(dateString)
            val dateFormat = android.text.format.DateFormat.getDateFormat(context)
            val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
            createdView.text = recordedText + " " + dateFormat.format(date) + " - " + timeFormat.format(date)
            //            nameView.text = recording.id.toString()
            floatingActionButton2.setOnClickListener {
                listener.onClick(recording.fileName)
            }

            if (recording.rating >= 1) star1.setImageResource(R.drawable.ic_star_on)
            if (recording.rating >= 2) star2.setImageResource(R.drawable.ic_star_on)
            if (recording.rating >= 3) star3.setImageResource(R.drawable.ic_star_on)
        }
    }
}
