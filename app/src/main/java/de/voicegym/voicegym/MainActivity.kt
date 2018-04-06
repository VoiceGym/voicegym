package de.voicegym.voicegym

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import kotlinx.android.synthetic.main.activity_main.playButton
import kotlinx.android.synthetic.main.activity_main.recordButton
import kotlinx.android.synthetic.main.activity_main.measureButton
import java.io.File

class MainActivity : AppCompatActivity() {

    private val recorder = MediaRecorder()
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        playButton.setOnClickListener {
            val player = MediaPlayer()
            player.setDataSource(File(filesDir, "myFile.3gp").absolutePath)
            player.prepare()
            player.start()
        }

        recordButton.setOnClickListener {
            if (recordButton.isRecording) {
                recordButton.setImageResource(android.R.drawable.ic_btn_speak_now)
                recordButton.isRecording = false
                recorder.stop()
                recorder.reset()   // You can reuse the object by going back to setAudioSource() step
                recorder.release() // Now the object cannot be reused
            } else {
                recordButton.setImageResource(android.R.drawable.ic_media_pause)
                recordButton.isRecording = true
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                recorder.setOutputFile(File(filesDir, "myFile.3gp").absolutePath)
                recorder.prepare()
                recorder.start()
            }
        }

        measureButton.setOnClickListener({
            val intent = Intent(this, MeasuringFourierTransforms::class.java).apply { }
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
