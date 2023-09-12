package org.akvo.caddisfly.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import org.akvo.caddisfly.R
import org.akvo.caddisfly.app.CaddisflyApp
import org.akvo.caddisfly.databinding.ActivityAboutBinding
import org.akvo.caddisfly.helper.ApkHelper.isTestDevice
import org.akvo.caddisfly.preference.AppPreferences
import org.akvo.caddisfly.viewmodel.TestListViewModel

/**
 * Activity to display info about the app.
 */
class AboutActivity : BaseActivity() {
    private lateinit var b: ActivityAboutBinding
    private var clickCount = 0
    private var dialog: NoticesDialogFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAboutBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)
        b.textVersion.text = CaddisflyApp.getAppVersion(AppPreferences.isDiagnosticMode())
        setTitle(R.string.about)
    }

    /**
     * Displays legal information.
     */
    fun onSoftwareNoticesClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        if (!isTestDevice(this)) {
            dialog = NoticesDialogFragment.newInstance()
            dialog?.show(supportFragmentManager, "NoticesDialog")
        }
    }

    /**
     * Disables diagnostic mode.
     */
    fun disableDiagnosticsMode(@Suppress("UNUSED_PARAMETER") view: View?) {
        Toast.makeText(
            this, getString(R.string.diagnosticModeDisabled),
            Toast.LENGTH_SHORT
        ).show()
        AppPreferences.disableDiagnosticMode()
        switchLayoutForDiagnosticOrUserMode()
        changeActionBarStyleBasedOnCurrentMode()
        clearTests()
    }

    private fun clearTests() {
        val viewModel = ViewModelProvider(this).get(TestListViewModel::class.java)
        viewModel.clearTests()
    }

    /**
     * Turn on diagnostic mode if user clicks on version section CHANGE_MODE_MIN_CLICKS times.
     */
    fun switchToDiagnosticMode(@Suppress("UNUSED_PARAMETER") view: View?) {
        if (!AppPreferences.isDiagnosticMode()) {
            clickCount++
            if (clickCount >= CHANGE_MODE_MIN_CLICKS) {
                clickCount = 0
                Toast.makeText(
                    this, getString(
                        R.string.diagnosticModeEnabled
                    ), Toast.LENGTH_SHORT
                ).show()
                AppPreferences.enableDiagnosticMode()
                changeActionBarStyleBasedOnCurrentMode()
                switchLayoutForDiagnosticOrUserMode()
                // clear and reload all the tests as diagnostic mode includes experimental tests
                clearTests()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        switchLayoutForDiagnosticOrUserMode()
    }

    /**
     * Show the diagnostic mode layout.
     */
    private fun switchLayoutForDiagnosticOrUserMode() {
        if (AppPreferences.isDiagnosticMode()) {
            findViewById<View>(R.id.layoutDiagnostics).visibility = View.VISIBLE
        } else {
            if (findViewById<View>(R.id.layoutDiagnostics).visibility == View.VISIBLE) {
                findViewById<View>(R.id.layoutDiagnostics).visibility = View.GONE
            }
        }
    }

    fun onHomeClick(@Suppress("UNUSED_PARAMETER") view: View) {
        dialog?.dismiss()
    }

    companion object {
        private const val CHANGE_MODE_MIN_CLICKS = 10
    }
}