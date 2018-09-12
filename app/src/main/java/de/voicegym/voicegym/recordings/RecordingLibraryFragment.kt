package de.voicegym.voicegym.recordings

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [RecordingLibraryFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [RecordingLibraryFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class RecordingLibraryFragment : Fragment() {


    var openSpectrogramButton: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_recording_library, container, false)
        openSpectrogramButton = view.findViewById(R.id.recordingLibrary_startSpectrogram)
        openSpectrogramButton?.setImageResource(R.drawable.ic_mic_white)
        openSpectrogramButton?.setOnClickListener {
            if (activity is ListRecordingsFragment.ListInteractionListener) {
                (activity as ListRecordingsFragment.ListInteractionListener).startSpectrogram()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transaction = activity?.supportFragmentManager?.beginTransaction();
        transaction?.replace(R.id.recordingLibrary_listContainer, ListRecordingsFragment());
        transaction?.addToBackStack(null);
        transaction?.commit();

    }
}
