package de.voicegym.voicegym.menu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.settings.SettingsActivity
import de.voicegym.voicegym.model.Recording
import de.voicegym.voicegym.recordActivity.RecordActivity
import de.voicegym.voicegym.recordings.RecordingsFragment
import de.voicegym.voicegym.util.SwitchToRecordingViewListener
import kotlinx.android.synthetic.main.activity_navigation_drawer.drawer_layout
import kotlinx.android.synthetic.main.activity_navigation_drawer.nav_view
import kotlinx.android.synthetic.main.app_bar_navigation_drawer.toolbar
import org.jetbrains.anko.contentView

class NavigationDrawerActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        RecordingsFragment.OnListFragmentInteractionListener,
        RecordingsFragment.SwitchToPlaybackFragmentListener,
        SwitchToRecordingViewListener {


    override fun switchToRecordingView() {
        switchToRecordingView(null)
    }

    override fun switchToRecordingView(startWithFileName: String?) {
        val intent = Intent(this, RecordActivity::class.java).apply { }
        if (startWithFileName != null) {
            val argumentsBundle = Bundle()
            argumentsBundle.putString(RecordActivity.AUDIO_FILE, startWithFileName)
            intent.putExtras(argumentsBundle)
        }
        startActivity(intent)
    }


    private fun requestPermission() {

        val permissionAudioResult = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
        val permissionStorageReadResult = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        val permissioStorageWriteResult = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)

        /**
         * READ and WRITE belong to the same permission group (STORAGE).
         * For now(API 27), giving permission to one, doesn't force user interaction for the other.
         */
        val permissionsToRequest = listOf(permissionAudioResult, permissionStorageReadResult, permissioStorageWriteResult)
                .zip(listOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .filter { it.first != PackageManager.PERMISSION_GRANTED }
                .map { it.second }


        if (permissionsToRequest.isNotEmpty()) {
            // only show dialog, if permission was denied earlier
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.permission_alert_dialog_title)
                        .setMessage(R.string.permission_alert_dialog_text)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            ActivityCompat.requestPermissions(
                                    this,
                                    permissionsToRequest.toTypedArray(),
                                    REQUEST_PERMISSIONS)
                        }.create().show()
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        permissionsToRequest.toTypedArray(),
                        REQUEST_PERMISSIONS)
            }
        } else {
            // permission already granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_drawer)
        setSupportActionBar(toolbar)
        cView = contentView
        fManager = supportFragmentManager

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        requestPermission()
        // always load recordingsfragment since, if we are adding more fragments we need a better way to launch the spectrogram
        loadRecordingsFragment()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_instruments -> {
                switchToRecordingView()
            }

            R.id.nav_recordings  -> {
                loadRecordingsFragment()
            }

            R.id.nav_settings    -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }


    private fun loadRecordingsFragment() {
        loadFragment(RecordingsFragment(), "RECORDINGS")
    }


    override fun onListFragmentInteraction(item: Recording) {
        Log.d("foo", "bar")
    }

    override fun onClick(fileName: String) {
        switchToRecordingView(fileName)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, "Permission NOT Granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val REQUEST_PERMISSIONS = 100

        var cView: View? = null

        var fManager: FragmentManager? = null

        private fun <T : Fragment> loadFragment(fragment: T, TAG: String): T {
            fManager?.let { manager ->
                cView?.post {
                    manager.beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.content_area, fragment, TAG)
                            .commitAllowingStateLoss()
                }

            }

            return fragment
        }
    }
}
