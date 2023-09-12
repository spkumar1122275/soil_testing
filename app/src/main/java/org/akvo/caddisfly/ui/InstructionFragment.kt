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

package org.akvo.caddisfly.ui

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import org.akvo.caddisfly.R
import org.akvo.caddisfly.common.ConstantKey
import org.akvo.caddisfly.databinding.FragmentInstructionBinding
import org.akvo.caddisfly.databinding.FragmentInstructionsBinding
import org.akvo.caddisfly.helper.InstructionHelper
import org.akvo.caddisfly.model.Instruction
import org.akvo.caddisfly.model.PageIndex
import org.akvo.caddisfly.model.TestInfo
import java.util.*
import kotlin.math.max
import kotlin.math.min

class InstructionFragment : Fragment() {
    private var _binding: FragmentInstructionsBinding? = null
    private val b get() = _binding!!
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var mTestInfo: TestInfo? = null
    private val pageIndex = PageIndex()
    private val instructionList = ArrayList<Instruction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstructionsBinding.inflate(inflater, container, false)

        b.callback = this
        mSectionsPagerAdapter = SectionsPagerAdapter(
            childFragmentManager,
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )

        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            mTestInfo = requireArguments().getParcelable(ConstantKey.TEST_INFO)

            InstructionHelper.setupInstructions(
                mTestInfo!!.instructions!!,
                instructionList, pageIndex, false
            )

            b.imagePageRight.setOnClickListener {
                b.viewPager.currentItem = min(
                    instructionList.size - 1,
                    b.viewPager.currentItem + 1
                )
            }
        }
        b.viewPager.adapter = mSectionsPagerAdapter
        b.imagePageLeft.setOnClickListener {
            b.viewPager.currentItem = max(0, b.viewPager.currentItem - 1)
        }
        b.pagerIndicator.showDots(true)
        b.pagerIndicator.setPageCount(mSectionsPagerAdapter!!.count)
        b.viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // Nothing to do here
            }

            override fun onPageSelected(position: Int) {
                b.pagerIndicator.setActiveIndex(position)
                if (position < 1) {
                    b.imagePageLeft.visibility = View.INVISIBLE
                } else {
                    b.imagePageLeft.visibility = View.VISIBLE
                }
                if (position > mSectionsPagerAdapter!!.count - 2) {
                    b.imagePageRight.visibility = View.INVISIBLE
                } else {
                    b.imagePageRight.visibility = View.VISIBLE
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                // Nothing to do here
            }
        })
        if (mSectionsPagerAdapter!!.count < 2) {
            b.imagePageLeft.visibility = View.GONE
            b.imagePageRight.visibility = View.GONE
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {
        private var fragmentInstructionBinding: FragmentInstructionBinding? = null
        var instruction: Instruction? = null
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            fragmentInstructionBinding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_instruction, container, false
            )
            if (arguments != null) {
                instruction = requireArguments().getParcelable(ARG_SECTION_NUMBER)
                fragmentInstructionBinding!!.instruction = instruction
            }
            return fragmentInstructionBinding!!.root
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private const val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section number.
             *
             * @param instruction The information to to display
             * @return The instance
             */
            fun newInstance(instruction: Instruction?): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putParcelable(ARG_SECTION_NUMBER, instruction)
                fragment.arguments = args
                return fragment
            }
        }
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    internal inner class SectionsPagerAdapter(fm: FragmentManager, behavior: Int) :
        FragmentPagerAdapter(fm, behavior) {
        override fun getItem(position: Int): Fragment {
            return PlaceholderFragment.newInstance(instructionList[position])
        }

        override fun getCount(): Int {
            return instructionList.size
        }
    }

    companion object {
        fun getInstance(testInfo: Parcelable?): InstructionFragment {
            val fragment = InstructionFragment()
            val args = Bundle()
            args.putParcelable(ConstantKey.TEST_INFO, testInfo)
            fragment.arguments = args
            return fragment
        }
    }
}