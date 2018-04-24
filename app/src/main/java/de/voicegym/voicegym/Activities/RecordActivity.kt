package de.voicegym.voicegym.Activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import de.voicegym.voicegym.FourierHelper.FourierHelper
import de.voicegym.voicegym.R


class RecordActivity : AppCompatActivity() {

    val fourierHelper = FourierHelper(2048, 8, 16384, 44100)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        setContentView(R.layout.activity_record)


    }
}
