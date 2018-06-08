package de.voicegym.voicegym.menu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import de.voicegym.voicegym.R
import de.voicegym.voicegym.recordActivity.RecordActivity
import de.voicegym.voicegym.util.SwitchToRecordingViewListener
import de.voicegym.voicegym.menu.dummy.ExerciseContent
import de.voicegym.voicegym.model.Recording
import kotlinx.android.synthetic.main.activity_navigation_drawer.drawer_layout
import kotlinx.android.synthetic.main.activity_navigation_drawer.nav_view
import kotlinx.android.synthetic.main.app_bar_navigation_drawer.toolbar
import org.jetbrains.anko.contentView

class NavigationDrawerActivity :
        AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        RecordingsFragment.OnListFragmentInteractionListener,
        InstrumentsFragment.OnFragmentInteractionListener,
        ExercisesFragment.OnListFragmentInteractionListener,
        ReportsFragment.OnFragmentInteractionListener,
        SwitchToRecordingViewListener {

    override fun switchToRecordingView() {
        val intent = Intent(this, RecordActivity::class.java).apply { }
        startActivity(intent)
    }

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_drawer)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        loadRecordingsFragment()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.navigation_drawer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else                 -> return super.onOptionsItemSelected(item)
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_instruments -> {
                loadInstrumentsFragment()
            }

            R.id.nav_exercises   -> {
                loadExercisesFragment()
            }

            R.id.nav_recordings  -> {
                loadRecordingsFragment()
            }

            R.id.nav_reports     -> {
                loadReportsFragment()
            }

            R.id.nav_share       -> {
                // load share action
            }

            R.id.nav_settings    -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadFragment(fragment: Fragment, TAG: String) {
        contentView!!.post {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            fragmentTransaction.replace(R.id.content_area, fragment, TAG)
            fragmentTransaction.commitAllowingStateLoss()
        }
    }

    private fun loadRecordingsFragment() {
        loadFragment(RecordingsFragment(), "RECORDINGS")
    }

    private fun loadReportsFragment() {
        loadFragment(ReportsFragment(), "REPORTS")
    }

    private fun loadExercisesFragment() {
        loadFragment(ExercisesFragment(), "EXERCISES")
    }

    private fun loadInstrumentsFragment() {
        loadFragment(InstrumentsFragment(), "INSTRUMENTS")
    }

    override fun onListFragmentInteraction(item: ExerciseContent.ExerciseItem?) {
        Log.d("foo", "bar")
    }

    override fun onListFragmentInteraction(item: Recording) {
        Log.d("foo", "bar")
    }

}
