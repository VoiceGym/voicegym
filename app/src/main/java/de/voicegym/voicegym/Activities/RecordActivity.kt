package de.voicegym.voicegym.Activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import de.voicegym.voicegym.Activities.InstrumentViews.ColorGradientPicker
import de.voicegym.voicegym.FourierHelper.FourierHelper
import de.voicegym.voicegym.R
import kotlinx.android.synthetic.main.activity_record.dummyView
import kotlinx.android.synthetic.main.activity_record.floatingActionButton
import java.util.Random


class RecordActivity : AppCompatActivity() {

    val fourierHelper = FourierHelper(2048, 8, 16384, 44100)

    val heatMap = ColorGradientPicker.getHeatMap()

    val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        setContentView(R.layout.activity_record)

        dummyView.xDataPoints = 50
        dummyView.refreshColorArray()

        floatingActionButton.setOnClickListener({
            val addColors = IntArray(dummyView.getDrawAreaHeight().toInt())
            for (i in 0 until addColors.size) {
                addColors[i] = heatMap.pickColor(random.nextFloat())
            }
            dummyView.insertColorLine(addColors)
            dummyView.invalidate()
        })
    }
}
