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

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ColorUtilTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        ClassUtil.assertUtilityClassWellDefined(ColorUtil::class.java)
    }

    @Test
    fun testGetDistance() {
        val distance = ColorUtil.getColorDistance(Color.rgb(200, 200, 200), Color.rgb(100, 100, 100))
        assertEquals(173.20508075688772, distance, 0.0)
    }

    @Test
    fun testGetColorRgbString() {
        val rgb: String = ColorUtil.getColorRgbString(-13850285)
        assertEquals("44  169  83", rgb)
    }

//    @Test
//    public void testAutoGenerateColors() {
//        TestInfo testInfo = new TestInfo();
//
//        for (int i = 0; i < 5; i++) {
//            Swatch swatch = new Swatch(((double) i * 10) / 10f, Color.TRANSPARENT, Color.TRANSPARENT);
//            testInfo.addSwatch(swatch);
//        }
//
//        List<Swatch> list = SwatchHelper.generateGradient(testInfo.getSwatches(), ColorUtil.DEFAULT_COLOR_MODEL);
//
//        assertEquals(1001, list.size());
//
//        for (int i = 0; i < list.size(); i++) {
//            assertEquals(true, list.get(i).getColor() == Color.BLACK ||
//                    list.get(i).getColor() == Color.TRANSPARENT);
//        }
//    }


    @Test
    fun testGetColorFromRgb() {
        val color: Int = ColorUtil.getColorFromRgb("44  169  83")
        assertEquals(-13850285, color.toLong())
    }

    @Test
    fun testGetBrightness() {
        val brightness = ColorUtil.getBrightness(Color.rgb(200, 255, 30))
        assertEquals(233, brightness.toLong())
    }
}