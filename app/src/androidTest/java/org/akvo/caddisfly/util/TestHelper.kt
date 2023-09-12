@file:Suppress("DEPRECATION")

package org.akvo.caddisfly.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.os.SystemClock
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.*
import org.akvo.caddisfly.BuildConfig
import org.akvo.caddisfly.R.id
import org.akvo.caddisfly.R.string
import org.akvo.caddisfly.app.CaddisflyApp
import org.akvo.caddisfly.helper.FileHelper
import org.akvo.caddisfly.helper.FileType
import org.akvo.caddisfly.util.TestConstant.EXTERNAL_SURVEY_PACKAGE_NAME
import org.akvo.caddisfly.util.TestUtil.clickListViewItem
import org.akvo.caddisfly.util.TestUtil.nextSurveyPage
import org.akvo.caddisfly.util.TestUtil.sleep
import org.hamcrest.Matchers
import timber.log.Timber
import java.io.File
import java.util.*

lateinit var mDevice: UiDevice

@Suppress("unused")
fun clickStartButton() {
    onView(
        Matchers.allOf(
            withId(id.buttonStart), withText(string.start),
            TestUtil.childAtPosition(
                TestUtil.childAtPosition(
                    withClassName(Matchers.`is`("android.widget.RelativeLayout")),
                    2
                ),
                2
            ),
            isDisplayed()
        )
    ).perform(click())
}

object TestHelper {
    private val STRING_HASH_MAP_EN = HashMap<String, String>()
    private val STRING_HASH_MAP_FR = HashMap<String, String>()
    private val STRING_HASH_MAP_HI = HashMap<String, String>()
    private val CALIBRATION_HASH_MAP: MutableMap<String, String> = HashMap()
    lateinit var currentHashMap: Map<String, String>

    private var screenshotCount = -1

    private fun addCalibration(key: String, colors: String) {
        CALIBRATION_HASH_MAP[key] = colors
    }

    fun getString(@StringRes resourceId: Int): String {
        return getInstrumentation().targetContext.getString(resourceId)
    }

    fun loadData(activity: Context) {
        STRING_HASH_MAP_EN.clear()
        STRING_HASH_MAP_FR.clear()
        STRING_HASH_MAP_HI.clear()
        CALIBRATION_HASH_MAP.clear()
        val currentResources: Resources? = activity.resources
        val assets: AssetManager? = currentResources!!.assets
        val metrics: DisplayMetrics? = currentResources.displayMetrics

        Resources(assets, metrics, currentResources.configuration)
        addCalibration(
            "TestValid", "0.0=255  38  186\n"
                    + "0.5=255  51  129\n"
                    + "1.0=255  59  89\n"
                    + "1.5=255  62  55\n"
                    + "2.0=255  81  34\n"
        )
        addCalibration(
            "TestInvalid", "0.0=255  88  177\n"
                    + "0.5=255  110  15\n"
                    + "1.0=255  138  137\n"
                    + "1.5=253  174  74\n"
                    + "2.0=253  174  76\n"
                    + "2.5=236  172  81\n"
                    + "3.0=254  169  61\n"
        )
        addCalibration(
            "OutOfSequence", "0.0=255  38  186\n"
                    + "0.5=255  51  129\n"
                    + "1.0=255  62  55\n"
                    + "1.5=255  59  89\n"
                    + "2.0=255  81  34\n"
        )
        addCalibration(
            "HighLevelTest", "0.0=255  38  180\n"
                    + "0.5=255  51  129\n"
                    + "1.0=255  53  110\n"
                    + "1.5=255  55  100\n"
                    + "2.0=255  59  89\n"
        )
        addCalibration(
            "TestInvalid2", "0.0=255  88  47\n"
                    + "0.5=255  60  37\n"
                    + "1.0=255  35  27\n"
                    + "1.5=253  17  17\n"
                    + "2.0=254  0  0\n"
        )
        addCalibration(
            "LowLevelTest", "0.0=255  60  37\n"
                    + "0.5=255  35  27\n"
                    + "1.0=253  17  17\n"
                    + "1.5=254  0  0\n"
                    + "2.0=224  0  0\n"
        )
        addCalibration(
            "TestValidChlorine", "0.0=255  38  186\n"
                    + "0.5=255  51  129\n"
                    + "1.0=255  59  89\n"
                    + "1.5=255  62  55\n"
                    + "2.0=255  81  34\n"
                    + "2.5=255  101  24\n"
                    + "3.0=255  121  14\n"
        )
    }

    fun takeScreenshot() {
        takeScreenshot("app", screenshotCount++)
    }

    fun takeScreenshot(name: String) {
        takeScreenshot(name, screenshotCount++)
    }

    fun takeScreenshot(name: String, page: Int) {
        if (BuildConfig.TAKE_SCREENSHOTS
            && VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1
        ) {
            val folder = File(
                getInstrumentation().targetContext.getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                )
                    .toString() + "/screenshots/"
            )
            val path = File(
                folder, name + "-" + Locale.getDefault().language.substring(0, 2) + "-" +
                        String.format("%02d", page + 1) + ".png"
            )

            if (!folder.exists()) {
                folder.mkdirs()
            }

            mDevice.takeScreenshot(path, 0.1f, 30)
        }
    }

    fun goToMainScreen() {
        var found = false
        while (!found) {
            try {
                onView(withId(id.buttonSettings)).check(matches(isDisplayed()))
                found = true
            } catch (e: NoMatchingViewException) {
                Espresso.pressBack()
            }
        }
    }

    @Suppress("unused")
    fun activateTestMode() {
        onView(withText(string.settings)).perform(click())
        onView(withText(string.about)).check(matches(isDisplayed())).perform(click())
        val version: String = CaddisflyApp.getAppVersion(false)
        onView(withText(version)).check(matches(isDisplayed()))
        enterDiagnosticMode()
        goToMainScreen()
        onView(withText(string.settings)).perform(click())
        clickListViewItem(getString(string.testModeOn))
    }

    fun clickExternalAppButton(@StringRes stringId: Int, screenshotName: String) {
        when (BuildConfig.FLAVOR) {
            "water" -> {
                nextSurveyPage(9, getString(string.water_tests_1))
            }
            "soil" -> {
                nextSurveyPage(9, "Soil Tests 2")
            }
        }

        try {
            if (mDevice.findObject(
                    By.text(
                        getInstrumentation().targetContext.getString(
                            string.redo
                        )
                    )
                ) != null
            ) {
                mDevice.findObject(
                    By.text(getInstrumentation().targetContext.getString(stringId))
                ).longClick()
                sleep(500)
                val removeText =
                    getInstrumentation().targetContext.getString(string.remove_response)
                mDevice.findObject(By.text(removeText)).click()
                sleep(1000)
                try {
                    mDevice.findObject(By.text(removeText)).click()
                } catch (e: Exception) {
                    mDevice.findObject(By.text(removeText.toUpperCase())).click()
                }
                sleep(500)
            }
        } catch (e: Exception) {
        }

        if (screenshotName.isNotEmpty()) {
            takeScreenshot(screenshotName)
        }
        mDevice.findObject(By.text(getInstrumentation().targetContext.getString(stringId)))
            .click()
    }

    fun saveCalibration(name: String, id: String) {
        val path: File = FileHelper.getFilesDir(FileType.CALIBRATION, id)
        FileUtil.saveToFile(path, name, CALIBRATION_HASH_MAP[name])
    }

    fun startSurveyApp() {
        val context: Context? = getInstrumentation().context
        val intent =
            context!!.packageManager.getLaunchIntentForPackage(EXTERNAL_SURVEY_PACKAGE_NAME)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        mDevice.waitForIdle()
        SystemClock.sleep(1000)
    }

    fun startSurveyForm() {
        val addButton: UiObject? = mDevice.findObject(
            UiSelector()
                .resourceId("$EXTERNAL_SURVEY_PACKAGE_NAME:id/enter_data")
        )
        try {
            if (addButton!!.exists() && addButton.isEnabled) {
                addButton.click()
            }
        } catch (e: UiObjectNotFoundException) {
            Timber.e(e)
        }
        mDevice.waitForIdle()
        clickListViewItem("Automated Testing")
        mDevice.waitForIdle()
        val goToStartButton: UiObject? = mDevice.findObject(
            UiSelector()
                .resourceId("$EXTERNAL_SURVEY_PACKAGE_NAME:id/jumpBeginningButton")
        )
        try {
            if (goToStartButton!!.exists() && goToStartButton.isEnabled) {
                goToStartButton.click()
            }
        } catch (e: UiObjectNotFoundException) {
            Timber.e(e)
        }
        mDevice.waitForIdle()
    }

    fun gotoSurveyForm() {
        startSurveyApp()
        startSurveyForm()
    }

    fun enterDiagnosticMode() {
        for (i in 0..9) {
            onView(withId(id.textVersion)).perform(click())
        }
    }

    fun leaveDiagnosticMode() {
        goToMainScreen()
        onView(withText(string.settings)).perform(click())
        onView(withId(id.disableDiagnostics)).perform(click())
//        onView(withId(id.fabDisableDiagnostics)).perform(click())
    }

    @Suppress("unused")
    fun clearPreferences(activityTestRule: ActivityTestRule<*>?) {
        val prefs: SharedPreferences? =
            PreferenceManager.getDefaultSharedPreferences(activityTestRule!!.activity)
        prefs!!.edit().clear().apply()
    }

    fun clearPreferences() {
        val prefs =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
        prefs.edit().clear().apply()
    }

    fun isDeviceInitialized(): Boolean {
        return ::mDevice.isInitialized
    }
}