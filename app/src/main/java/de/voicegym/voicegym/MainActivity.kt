package de.voicegym.voicegym

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import de.voicegym.voicegym.activities.MeasuringFourierTransforms
import de.voicegym.voicegym.activities.RecordActivity
import kotlinx.android.synthetic.main.activity_main.measureButton
import kotlinx.android.synthetic.main.activity_main.recordActivityButton

class MainActivity : AppCompatActivity() {

    private val recorder = MediaRecorder()
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        measureButton.setOnClickListener({
            val intent = Intent(this, MeasuringFourierTransforms::class.java).apply { }
            startActivity(intent)
        })

        recordActivityButton.setOnClickListener({
            val intent = Intent(this, RecordActivity::class.java).apply { }
            startActivity(intent)
        })
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = when (requestCode) {
            Companion.REQUEST_RECORD_AUDIO_PERMISSION ->
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            else -> false
        }
        if (!permissionToRecordAccepted) finish()
    }

    companion object {
        // Requesting permission to RECORD_AUDIO
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}

class MyRecordButton(ctx: Context, attrs: AttributeSet) : FloatingActionButton(ctx, attrs) {
    var isRecording = false
}
