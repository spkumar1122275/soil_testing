/*
 * Copyright (C) Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Caddisfly.
 *
 * Akvo Caddisfly is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Caddisfly is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Caddisfly. If not, see <http://www.gnu.org/licenses/>.
 */
@file:Suppress("DEPRECATION")

package org.akvo.caddisfly.sensor.chamber

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import org.akvo.caddisfly.R
import org.akvo.caddisfly.common.ChamberTestConfig
import org.akvo.caddisfly.common.ConstantKey
import org.akvo.caddisfly.databinding.FragmentRunTestBinding
import org.akvo.caddisfly.entity.Calibration
import org.akvo.caddisfly.helper.SoundUtil.playShortResource
import org.akvo.caddisfly.helper.SwatchHelper.analyzeColor
import org.akvo.caddisfly.model.ColorInfo
import org.akvo.caddisfly.model.ResultDetail
import org.akvo.caddisfly.model.TestInfo
import org.akvo.caddisfly.preference.AppPreferences
import org.akvo.caddisfly.util.AlertUtil
import org.akvo.caddisfly.util.ColorUtil
import org.akvo.caddisfly.util.ImageUtil
import org.akvo.caddisfly.viewmodel.TestInfoViewModel
import java.util.*
import kotlin.math.max

const val MINUTES_IN_AN_HOUR = 60
const val SECONDS_IN_A_MINUTE = 60

open class BaseRunTest : Fragment(), RunTest {
    private var _binding: FragmentRunTestBinding? = null
    protected val b get() = _binding!!

    private val countdown = intArrayOf(0)
    private val results = ArrayList<ResultDetail>()
    private val delayHandler = Handler()

    @JvmField
    protected var cameraStarted = false

    @JvmField
    protected var pictureCount = 0
    private var timeDelay = 0
    private var mHandler: Handler? = null
    private var alertDialogToBeDestroyed: AlertDialog? = null
    private var mTestInfo: TestInfo? = null
    private var mCalibration: Calibration? = null
    private var dilution = 1
    private var mCamera: Camera? = null
    private var mListener: OnResultListener? = null
    private var mCameraPreview: ChamberCameraPreview? = null
    private val mRunnableCode = Runnable {
        if (pictureCount < AppPreferences.samplingTimes) {
            pictureCount++
            playShortResource(requireActivity(), R.raw.beep)
            takePicture()
        } else {
            releaseResources()
        }
    }
    private val mPicture = PictureCallback { data, _ ->
        mCamera!!.startPreview()
        val bitmap = ImageUtil.getBitmap(data)
        getAnalyzedResult(bitmap)
        if (mTestInfo!!.results!![0].timeDelay > 0) { // test has time delay so take the pictures quickly with short delay
            mHandler!!.postDelayed(mRunnableCode, (SHORT_DELAY * 1000).toLong())
        } else {
            mHandler!!.postDelayed(
                mRunnableCode,
                ChamberTestConfig.DELAY_BETWEEN_SAMPLING * 1000.toLong()
            )
        }
    }
    private val mCountdown = Runnable { setCountDown() }
    private fun setCountDown() {
        if (countdown[0] < timeDelay) {
            b.timeLayout.visibility = View.VISIBLE
            countdown[0]++
            if (timeDelay > 10 && (timeDelay - countdown[0]) % 15 == 0) {
                playShortResource(requireActivity(), R.raw.beep)
            }
            //            binding.countdownTimer.setProgress(timeDelay - countdown[0], timeDelay);
            b.textTimeRemaining.text = timeConversion(timeDelay - countdown[0])
            delayHandler.postDelayed(mCountdown, 1000)
        } else {
            b.timeLayout.visibility = View.GONE
            b.layoutWait.visibility = View.VISIBLE
            waitForStillness()
        }
    }

    protected open fun waitForStillness() {}

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (mCalibration != null && activity != null) { // disable the key guard when device wakes up and shake alert is displayed
            requireActivity().window.setFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    protected open fun initializeTest() {
        results.clear()
        mHandler = Handler()
    }

    protected fun setupCamera() {
        // Create our Preview view and set it as the content of our activity.
        mCameraPreview = ChamberCameraPreview(activity)
        mCamera = mCameraPreview!!.camera
        mCameraPreview!!.setupCamera(mCamera!!)
        b.cameraView.addView(mCameraPreview)
        b.cameraView.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    b.cameraView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val parentHeight = (b.cameraView.parent as FrameLayout).measuredHeight
                    val offset = (parentHeight * AppPreferences.cameraCenterOffset
                            / mCamera?.parameters?.pictureSize!!.width)
                    val layoutParams = b.circleView.layoutParams as FrameLayout.LayoutParams
                    val r = context?.resources
                    val offsetPixels = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        offset.toFloat(),
                        r?.displayMetrics
                    ).toInt()
                    layoutParams.setMargins(0, 0, 0, offsetPixels)
                    b.circleView.layoutParams = layoutParams
                }
            })
    }

    protected fun stopPreview() {
        if (mCamera != null) {
            mCamera!!.stopPreview()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRunTestBinding.inflate(inflater, container, false)
        pictureCount = 0
        if (arguments != null) {
            mTestInfo = requireArguments().getParcelable(ConstantKey.TEST_INFO)
        }
        if (mTestInfo != null) {
            mTestInfo!!.dilution = dilution
        }
        val model = ViewModelProviders.of(this).get(TestInfoViewModel::class.java)
        model.setTest(mTestInfo)
        b.vm = model
        initializeTest()
        if (mCalibration != null) {
            b.textDilution.text = mCalibration!!.value.toString()
        } else {
            b.textDilution.text = resources
                .getQuantityString(R.plurals.dilutions, dilution, dilution)
        }
        countdown[0] = 0
        // If the test has a time delay config then use that otherwise use standard delay
        if (mTestInfo!!.results!![0].timeDelay > 10) {
            timeDelay = max(SHORT_DELAY, mTestInfo!!.results!![0].timeDelay.toDouble()).toInt()
            b.timeLayout.visibility = View.VISIBLE
            b.countdownTimer.setProgress(timeDelay, timeDelay)
            setCountDown()
        } else {
            waitForStillness()
        }
        return b.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnResultListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnResultListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun takePicture() {
        if (!cameraStarted) {
            return
        }
        mCamera!!.startPreview()
        turnFlashOn()
        mCamera!!.takePicture(null, null, mPicture)
    }

    /**
     * Get the test result by analyzing the bitmap.
     *
     * @param bmp the bitmap of the photo taken during analysis
     */
    private fun getAnalyzedResult(bmp: Bitmap) {
        val bitmap = ImageUtil.rotateImage(requireActivity(), bmp)
        var croppedBitmap = ImageUtil.getCroppedBitmap(
            bitmap,
            ChamberTestConfig.SAMPLE_CROP_LENGTH_DEFAULT
        )
        //Extract the color from the photo which will be used for comparison
        if (mTestInfo!!.results!![0].grayScale) {
            croppedBitmap = ImageUtil.getGrayscale(croppedBitmap)
        }
        val photoColor: ColorInfo = ColorUtil.getColorFromBitmap(
            croppedBitmap,
            ChamberTestConfig.SAMPLE_CROP_LENGTH_DEFAULT
        )
        if (mCalibration != null) {
            mCalibration!!.color = photoColor.color
            mCalibration!!.date = Date().time
        }
        val resultDetail = analyzeColor(
            mTestInfo!!.swatches.size,
            photoColor, mTestInfo!!.swatches
        )
        resultDetail.bitmap = bitmap
        resultDetail.croppedBitmap = croppedBitmap
        resultDetail.dilution = dilution
        resultDetail.quality = photoColor.quality
        //            Timber.d("Result is: " + String.valueOf(resultDetail.getResult()));
        results.add(resultDetail)
        if (mListener != null && pictureCount >= AppPreferences.samplingTimes) { // ignore the first two results
            for (i in 0 until ChamberTestConfig.SKIP_SAMPLING_COUNT) {
                if (results.size > 1) {
                    results.removeAt(0)
                }
            }
            mListener!!.onResult(results, mCalibration)
        }
    }

    override fun setCalibration(item: Calibration?) {
        mCalibration = item
    }

    override fun setDilution(dilution: Int) {
        this.dilution = dilution
    }

    protected fun startRepeatingTask() {
        mRunnableCode.run()
    }

    private fun stopRepeatingTask() {
        mHandler!!.removeCallbacks(mRunnableCode)
    }

    protected open fun startTest() {
        if (!cameraStarted) {
            setupCamera()
            cameraStarted = true
            playShortResource(requireActivity(), R.raw.futurebeep2)
            var initialDelay = 0
            //If the test has a time delay config then use that otherwise use standard delay
            if (mTestInfo!!.results!![0].timeDelay < 5) {
                initialDelay =
                    ChamberTestConfig.DELAY_INITIAL + ChamberTestConfig.DELAY_BETWEEN_SAMPLING
            }
            b.layoutWait.visibility = View.VISIBLE
            delayHandler.postDelayed(mRunnableCode, initialDelay * 1000.toLong())
        }
    }

    /**
     * Turn flash off.
     */
    protected fun turnFlashOff() {
        if (mCamera == null) {
            return
        }
        val parameters = mCamera!!.parameters
        val flashMode = Camera.Parameters.FLASH_MODE_OFF
        parameters.flashMode = flashMode
        mCamera!!.parameters = parameters
    }

    /**
     * Turn flash on.
     */
    protected fun turnFlashOn() {
        if (mCamera == null) {
            return
        }
        val parameters = mCamera!!.parameters
        val flashMode = Camera.Parameters.FLASH_MODE_TORCH
        parameters.flashMode = flashMode
        try {
            mCamera!!.parameters = parameters
        } catch (e: Exception) {
        }
    }

    protected open fun releaseResources() {
        if (mCamera != null) {
            mCamera!!.stopPreview()
            mCamera!!.setPreviewCallback(null)
            mCameraPreview!!.holder.removeCallback(mCameraPreview)
            mCamera!!.release()
            mCamera = null
        }
        if (mCameraPreview != null) {
            mCameraPreview!!.destroyDrawingCache()
        }
        delayHandler.removeCallbacksAndMessages(null)
        if (alertDialogToBeDestroyed != null) {
            alertDialogToBeDestroyed!!.dismiss()
        }
        stopRepeatingTask()
        cameraStarted = false
    }

    /**
     * Show an error message dialog.
     *
     * @param message the message to be displayed
     * @param bitmap  any bitmap image to displayed along with error message
     */
    fun showError(
        message: String,
        bitmap: Bitmap?,
        activity: Activity
    ) {
        stopScreenPinning()
        releaseResources()
        playShortResource(requireActivity(), R.raw.err)
        alertDialogToBeDestroyed = AlertUtil.showError(
            activity,
            R.string.error, message, bitmap, R.string.ok,
            { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                activity.setResult(Activity.RESULT_CANCELED)
                stopScreenPinning()
                activity.finish()
            }, null, null
        )
    }

    private fun stopScreenPinning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                requireActivity().stopLockTask()
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onPause() {
        super.onPause()
        releaseResources()
    }

    interface OnResultListener {
        fun onResult(resultDetails: ArrayList<ResultDetail>, calibration: Calibration?)
    }

    companion object {
        private const val SHORT_DELAY = 1.0
        private fun timeConversion(sec: Int): String {
            var seconds = sec
            var minutes = seconds / SECONDS_IN_A_MINUTE
            seconds -= minutes * SECONDS_IN_A_MINUTE
            val hours = minutes / MINUTES_IN_AN_HOUR
            minutes -= hours * MINUTES_IN_AN_HOUR
            return String.format(Locale.US, "%02d", hours) + ":" +
                    String.format(Locale.US, "%02d", minutes) + ":" +
                    String.format(Locale.US, "%02d", seconds)
        }
    }
}