package de.voicegym.voicegym.recordings


import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.NavigationDrawerActivity
import de.voicegym.voicegym.model.Recording
import de.voicegym.voicegym.recordActivity.RecordActivity
import de.voicegym.voicegym.util.ISetTextable
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_recordings.createdView
import kotlinx.android.synthetic.main.fragment_recordings.durationView
import kotlinx.android.synthetic.main.fragment_recordings.nameView
import kotlinx.android.synthetic.main.fragment_recordings.playAudioFileButton
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
        private val listInteractionListener: ListRecordingsFragment.ListInteractionListener)
    : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_recordings, parent, false)
        return ViewHolder(context, view, listInteractionListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.bindRecording(item)

        with(holder.containerView) {
            tag = item
        }
    }

    override fun getItemCount(): Int = values.size

    fun update(recordings: List<Recording>) {
        this.values = recordings
        notifyDataSetChanged()
    }

    operator fun get(position: Int): Recording = values[position]

    class ViewHolder(
            private val context: Context, override val containerView: View, private val listener: ListRecordingsFragment.ListInteractionListener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bindRecording(recording: Recording) {


            durationView.text = " ${recording.duration}sec"
            val dateString = recording.fileName
                    .split("/")
                    .last()
                    .replace("_", " ")
                    .dropLast(4)


            val date = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").parse(dateString)
            val dateFormat = android.text.format.DateFormat.getDateFormat(context)
            val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
            createdView.text = dateFormat.format(date) + " - " + timeFormat.format(date)
            nameView.text = when {
                recording.title == null -> dateString
                recording.title == ""   -> dateString
                else                    -> recording.title
            }

            nameView.setOnClickListener {
                val callback = object : ISetTextable {
                    override fun setText(text: String) {
                        RecordActivity.setNameOfFile(recording.fileName, text)
                    }

                }
                (context as NavigationDrawerActivity).showInputDialog(nameView.text.toString(), callback)
            }
            playAudioFileButton.setOnClickListener {
                listener.openAudioFileInPlaybackMode(recording.fileName)
            }

            setStar(star1, recording.rating >= 1)
            setStarCallbackActions(star1, recording.fileName, recording.rating, 1)
            setStar(star2, recording.rating >= 2)
            setStarCallbackActions(star2, recording.fileName, recording.rating, 2)
            setStar(star3, recording.rating >= 3)
            setStarCallbackActions(star3, recording.fileName, recording.rating, 3)
        }

        private fun setStar(star: ImageView, active: Boolean) {
            when (active) {
                true  -> star.setImageResource(R.drawable.ic_star_on)
                false -> star.setImageResource(R.drawable.ic_star_off_bright_background)
            }
        }

        private fun setStarCallbackActions(star: ImageView, filename: String, currentRating: Int, targetRating: Int) {
            star.setOnClickListener {
                RecordActivity.setRatingOfFile(filename, when {
                    targetRating > currentRating  -> targetRating
                    currentRating == targetRating -> targetRating - 1
                    else                          -> targetRating
                })
            }

        }
    }
}
