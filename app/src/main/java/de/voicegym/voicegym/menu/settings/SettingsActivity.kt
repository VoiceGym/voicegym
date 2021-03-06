package de.voicegym.voicegym.menu.settings

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.view.MenuItem
import de.voicegym.voicegym.R

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // This is needed to make the UP button in SettingsActivity Work
        // nothing else     in AndroidManifest.xml required
        if (item.itemId == android.R.id.home) {
            finish() // or go to another activity
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || GeneralPreferenceFragment::class.java.name == fragmentName
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GeneralPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // find all preferences in the xml layout file - see for all preferences
            addPreferencesFromResource(R.xml.pref_fourier_instrumentview)
            setHasOptionsMenu(true)

            // bind these preference keys  and set onChangeListeners when their values are changed
            bindPreferenceSummaryToValue(findPreference("fft_binning"))
            bindPreferenceSummaryToValue(findPreference("fft_blocksize"))
            bindPreferenceSummaryToValue(findPreference("display_logarithmic"))
            bindPreferenceSummaryToValue(findPreference("from_frequency"))
            bindPreferenceSummaryToValue(findPreference("till_frequency"))
            bindPreferenceSummaryToValue(findPreference("display_sample_numbers"))

        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }


    companion object {

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            when (preference) {
                is ListPreference -> {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    val index = preference.findIndexOfValue(stringValue)

                    // Set the summary to reflect the new value.
                    preference.setSummary(when {
                        index >= 0 -> preference.entries[index]
                        else       -> "4096"
                    })
                }

                else              -> preference.summary = stringValue
            }
            true
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.

            val sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(preference.context)

            // select the appropriate datatype
            val value = when (preference) {
                is ListPreference     -> {
                    sharedPreferences.getString(preference.key, "")
                }

                is CheckBoxPreference -> {
                    sharedPreferences.getBoolean(preference.key, false)
                }

                is EditTextPreference -> {
                    sharedPreferences.getString(preference.key, "")
                }

                else                  -> throw Error("Introduced new type of preference ${preference.javaClass}, needs to be caught here")
            }

            // call change listener of the preference
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value)

        }
    }
}
