package org.akvo.caddisfly.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import org.akvo.caddisfly.BuildConfig
import org.akvo.caddisfly.R
import org.akvo.caddisfly.app.CaddisflyApp
import org.akvo.caddisfly.common.NavigationController
import org.akvo.caddisfly.databinding.ActivityMainBinding
import org.akvo.caddisfly.helper.ApkHelper
import org.akvo.caddisfly.helper.PermissionsDelegate
import org.akvo.caddisfly.model.TestSampleType
import org.akvo.caddisfly.model.TestType
import org.akvo.caddisfly.preference.AppPreferences
import org.akvo.caddisfly.preference.SettingsActivity
import org.akvo.caddisfly.util.AlertUtil
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

const val PERMISSION_WATER = 1
const val PERMISSION_SOIL = 2

class MainActivity : AppUpdateActivity() {
    private lateinit var b: ActivityMainBinding
    private val permissionsDelegate = PermissionsDelegate(this)
    private var navigationController: NavigationController? = null
    private var runTest = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationController = NavigationController(this)
        b = ActivityMainBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)
        setTitle(R.string.app_name)
        try {
            if (BuildConfig.BUILD_TYPE.equals("release", ignoreCase = true) &&
                ApkHelper.isNonStoreVersion(this)
            ) {
                val appExpiryDate = GregorianCalendar.getInstance()
                appExpiryDate.time = BuildConfig.BUILD_TIME
                appExpiryDate.add(Calendar.DAY_OF_YEAR, 15)
                val df: DateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
                b.textVersionExpiry.text =
                    String.format("Version expiry: %s", df.format(appExpiryDate.time))
                b.textVersionExpiry.visibility = View.VISIBLE
            } else {
                if (ApkHelper.isNonStoreVersion(this)) {
                    b.textVersionExpiry.text = CaddisflyApp.getAppVersion(true)
                    b.textVersionExpiry.visibility = View.VISIBLE
                }
            }
            // If app has expired then close this activity
            ApkHelper.isAppVersionExpired(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        b.buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    /**
     * Show the diagnostic mode layout.
     */
    private fun switchLayoutForDiagnosticOrUserMode() {
        if (AppPreferences.isDiagnosticMode()) {
            b.textDiagnostics.visibility = View.VISIBLE
        } else {
            b.textDiagnostics.visibility = View.GONE
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onResume() {
        super.onResume()
        switchLayoutForDiagnosticOrUserMode()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionsDelegate.resultGranted(grantResults)) {
            when (requestCode) {
                PERMISSION_WATER -> if (runTest) {
                    startTest(TestSampleType.WATER)
                }
                PERMISSION_SOIL -> if (runTest) {
                    startTest(TestSampleType.SOIL)
                }
            }
        } else {
            val message = ""
            AlertUtil.showSettingsSnackbar(
                this,
                window.decorView.rootView, message
            )
        }
    }

    fun onRunTestClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        runTest = true
        startTest(TestSampleType.ALL)
    }

    private fun startTest(testSampleType: TestSampleType) {
        navigationController!!.navigateToTestType(TestType.ALL, testSampleType, runTest)
    }
}