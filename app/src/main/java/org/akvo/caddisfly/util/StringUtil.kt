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
package org.akvo.caddisfly.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.appcompat.app.AppCompatActivity
import org.akvo.caddisfly.R
import org.akvo.caddisfly.model.TestInfo
import org.akvo.caddisfly.widget.CenteredImageSpan
import java.util.*
import java.util.regex.Pattern

object StringUtil {
    @JvmStatic
    fun getStringResourceByName(context: Context, theKey: String): Spanned {
        return getStringResourceByName(
            context, theKey,
            context.resources.configuration.locale.language
        )
    }

    @JvmStatic
    fun getStringResourceByName(context: Context, theKey: String, language: String): Spanned {
        val key = theKey.trim { it <= ' ' }
        val packageName = context.packageName
        val resId = context.resources.getIdentifier(key, "string", packageName)
        return if (resId == 0) {
            Spannable.Factory.getInstance().newSpannable(fromHtml(key))
        } else {
            if (language.isNotEmpty()) {
                Spannable.Factory.getInstance().newSpannable(
                    getLocalizedResources(context, Locale(language)).getText(resId)
                )
            } else {
                Spannable.Factory.getInstance().newSpannable(context.getText(resId))
            }
        }
    }

    private fun getLocalizedResources(context: Context, desiredLocale: Locale): Resources {
        var conf = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(desiredLocale)
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources
    }

    private fun fromHtml(html: String?): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    @JvmStatic
    fun toInstruction(
        context: AppCompatActivity,
        testInfo: TestInfo?,
        instructionText: String
    ): SpannableStringBuilder {
        var text = instructionText
        val builder = SpannableStringBuilder()
        var isBold = false
        if (text.startsWith("<b>") && text.endsWith("</b>")) {
            isBold = true
            text = text.replace("<b>", "").replace("</b>", "")
        }
        val spanned = getStringResourceByName(context, text)
        builder.append(spanned)
        if (isBold) {
            val boldSpan = StyleSpan(Typeface.BOLD)
            builder.setSpan(
                boldSpan,
                0,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val m = Pattern.compile("\\(\\*(\\w+)\\*\\)").matcher(builder)
        while (m.find()) {
            val resId = context.resources.getIdentifier(
                "button_" + m.group(1),
                "drawable", context.packageName
            )
            if (resId > 0) {
                builder.setSpan(
                    CenteredImageSpan(context, resId),
                    m.start(0), m.end(0), Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                )
            }
        }

        // Set reagent in the string
        replaceReagentTags(testInfo, builder)

        // Set sample quantity in the string
        val m1 = Pattern.compile("%sampleQuantity").matcher(builder)
        while (m1.find()) {
            builder.replace(m1.start(), m1.end(), testInfo!!.sampleQuantity)
        }
        if (testInfo != null) {
            // Set reaction time in the string
            for (i in 1..4) {
                val m2 = Pattern.compile("%reactionTime$i").matcher(builder)
                while (m2.find()) {
                    if (testInfo.getReagent(i - 1).reactionTime != null) {
                        builder.replace(
                            m2.start(), m2.end(),
                            context.resources.getQuantityString(
                                R.plurals.minutes,
                                testInfo.getReagent(i - 1).reactionTime!!,
                                testInfo.getReagent(i - 1).reactionTime
                            )
                        )
                    }
                }
            }
        }
        return builder
    }

    private fun replaceReagentTags(testInfo: TestInfo?, builder: SpannableStringBuilder) {
        for (i in 1..4) {
            val m1 = Pattern.compile("%reagent$i").matcher(builder)
            while (m1.find()) {
                var name = testInfo!!.getReagent(i - 1).name
                val code = testInfo.getReagent(i - 1).code
                if (code!!.isNotEmpty()) {
                    name = String.format("%s (%s)", name, code)
                }
                builder.replace(m1.start(), m1.end(), name)
            }
        }
    }
}