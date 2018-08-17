package de.voicegym.voicegym

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import de.voicegym.voicegym.model.AppDatabase
import de.voicegym.voicegym.model.Recording
import de.voicegym.voicegym.util.audio.getVoiceGymFolder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch


class App : Application() {

    private lateinit var db : AppDatabase
    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(baseContext)
        // TODO: We don't really need this, but it's nice for developement
        launch (CommonPool) {
            // we don't have access permission on first start, since permission are required in the first activity
            if ( isStoragePermissionGranted() ) {
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

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else { //permission is automatically granted on sdk<23 upon installation
            true
        }
    }
}
