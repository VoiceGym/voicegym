package de.voicegym.voicegym.model

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

/**
 * A ViewModel is responsible for preparing the data for the UI/View. So 'Views', e.g. Activities,
 * Fragments, Views should only have access to the model via the ViewModel.
 *
 * For now this only delegates calls, but might do more in the future.
 */
class RecordingListViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance()

    val recordingsList by lazy {
        database.recordingDao().getAll()
    }

    fun deleteRecording(recording: Recording) {
        launch(CommonPool) {
            database.recordingDao().delete(recording)
        }
    }

    fun addRecording(recording: Recording) {
        launch(CommonPool) {
            database.recordingDao().insert(recording)
        }
    }
}
