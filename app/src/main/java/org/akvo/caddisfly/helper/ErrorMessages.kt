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
package org.akvo.caddisfly.helper

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import org.akvo.caddisfly.R
import org.akvo.caddisfly.common.ConstantKey
import org.akvo.caddisfly.model.TestInfo
import org.akvo.caddisfly.sensor.chamber.ChamberTestActivity
import org.akvo.caddisfly.util.AlertUtil
import org.akvo.caddisfly.util.toLocalString

object ErrorMessages {
    private const val MESSAGE_TWO_LINE_FORMAT = "%s%n%n%s"
    private const val TWO_SENTENCE_FORMAT = "%s%n%n%s"

    /**
     * Error message for configuration not loading correctly.
     */
    @JvmStatic
    fun alertCouldNotLoadConfig(activity: Activity) {
        val message = String.format(
            TWO_SENTENCE_FORMAT,
            activity.getString(R.string.error_loading_config),
            activity.getString(R.string.please_contact_support)
        )
        AlertUtil.showError(
            activity, R.string.error, message, null, R.string.ok,
            { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }, null, null
        )
    }

    /**
     * Alert message for calibration incomplete or invalid.
     */
    @JvmStatic
    fun alertCalibrationIncomplete(
        activity: Activity, testInfo: TestInfo,
        isInternal: Boolean, finishActivity: Boolean
    ) {
        var message = activity.getString(
            R.string.error_calibration_incomplete,
            testInfo.name!!.toLocalString()
        )
        message = String.format(
            MESSAGE_TWO_LINE_FORMAT, message,
            activity.getString(R.string.do_you_want_to_calibrate)
        )

        AlertUtil.showAlert(
            activity, R.string.cannot_start_test, message, R.string.calibrate,
            { dialogInterface: DialogInterface, _: Int ->
                val intent = Intent(activity, ChamberTestActivity::class.java)
                intent.putExtra(ConstantKey.TEST_INFO, testInfo)
                intent.putExtra(ConstantKey.IS_INTERNAL, isInternal)
                activity.startActivity(intent)
                activity.setResult(Activity.RESULT_CANCELED)
                dialogInterface.dismiss()
            },
            { dialogInterface: DialogInterface, _: Int ->
                activity.setResult(Activity.RESULT_CANCELED)
                dialogInterface.dismiss()
                if (finishActivity) {
                    activity.finish()
                }
            },
            { dialogInterface: DialogInterface ->
                activity.setResult(Activity.RESULT_CANCELED)
                dialogInterface.dismiss()
                if (finishActivity) {
                    activity.finish()
                }
            }
        )
    }

    fun alertCalibrationExpired(activity: Activity) {
        val message = String.format(
            MESSAGE_TWO_LINE_FORMAT,
            activity.getString(R.string.error_calibration_expired),
            activity.getString(R.string.order_fresh_batch)
        )
        AlertUtil.showAlert(
            activity, R.string.cannot_start_test,
            message, R.string.ok,
            { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                activity.finish()
            }, null,
            { dialogInterface: DialogInterface ->
                dialogInterface.dismiss()
                activity.finish()
            }
        )
    }
}