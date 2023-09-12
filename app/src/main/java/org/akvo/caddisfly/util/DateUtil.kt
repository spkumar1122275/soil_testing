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

import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for date and time.
 */
object DateUtil {
    /**
     * Gets the number of days in between two given dates.
     *
     * @param calendar1 the first date
     * @param calendar2 the second date
     * @return the number days
     */
    @JvmStatic
    fun getDaysDifference(calendar1: Calendar?, calendar2: Calendar?): Int {
        return if (calendar1 == null || calendar2 == null) {
            0
        } else ((calendar2.timeInMillis
                - calendar1.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * Gets the number of hours in between two given dates.
     *
     * @param calendar1 the first date
     * @param calendar2 the second date
     * @return the number hours
     */
    @JvmStatic
    fun getHoursDifference(calendar1: Calendar?, calendar2: Calendar?): Int {
        return if (calendar1 == null || calendar2 == null) {
            0
        } else ((calendar2.timeInMillis
                - calendar1.timeInMillis) / (1000 * 60 * 60)).toInt()
    }

    @JvmStatic
    fun convertStringToDate(dateString: String, format: String): Date? {
        val simpleDateFormat = SimpleDateFormat(format, Locale.US)
        try {
            return simpleDateFormat.parse(dateString.trim { it <= ' ' })
        } catch (e: ParseException) {
            Timber.e(e)
        }
        return null
    }
}