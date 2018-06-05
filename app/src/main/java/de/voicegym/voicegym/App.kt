package de.voicegym.voicegym

import android.app.Application
import de.voicegym.voicegym.model.AppDatabase
import de.voicegym.voicegym.model.Recording
import de.voicegym.voicegym.util.audio.getVoiceGymFolder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class App : Application() {

    private val db : AppDatabase by lazy {
        AppDatabase.getInstance(baseContext)!!
    }
    override fun onCreate() {
        super.onCreate()

        launch (CommonPool) {
            getVoiceGymFolder()?.also {
                val files = it.listFiles()
                files.map {file ->
                    db.recordingDao().getByFileName(file.path) ?: db.recordingDao().insert(Recording().apply {
                        fileName = file.path
                        createdAt = file.lastModified()
                        updatedAt = file.lastModified()
                    })
                }
            }
        }
    }
}
