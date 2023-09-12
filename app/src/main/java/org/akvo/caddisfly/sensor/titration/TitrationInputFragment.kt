package org.akvo.caddisfly.sensor.titration

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.akvo.caddisfly.R
import org.akvo.caddisfly.databinding.FragmentTitrationInputBinding
import org.akvo.caddisfly.model.TestInfo
import org.akvo.caddisfly.ui.BaseFragment
import org.akvo.caddisfly.util.MathUtil
import org.akvo.caddisfly.util.toLocalString
import timber.log.Timber
import java.util.*

class TitrationInputFragment : BaseFragment() {
    private var _binding: FragmentTitrationInputBinding? = null
    private val b get() = _binding!!
    private var mListener: OnSubmitResultListener? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTitrationInputBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            delay(200L)
            showSoftKeyboard(b.editTitration1)
        }

        b.editTitration1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                b.editTitration1.error = null
                b.editTitration2.error = null
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        b.editTitration2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                b.editTitration1.error = null
                b.editTitration2.error = null
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        if (arguments != null) {
            val testInfo: TestInfo? = requireArguments().getParcelable(ARG_PARAM1)
            if (testInfo != null) {
                if (testInfo.results!!.size > 1 && testInfo.results!![1].display == 1) {
                    b.inputTitleTxt.text = getString(R.string.enter_titration_result)
                    b.textInput1.visibility = View.GONE
                    b.textInput2.visibility = View.GONE
                    b.editTitration2.visibility = View.GONE
                    b.editTitration1.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            if (mListener != null) {
                                val n1String = b.editTitration1.text.toString()
                                if (n1String.isEmpty()) {
                                    b.editTitration1.error = getString(R.string.value_is_required)
                                    b.editTitration1.requestFocus()
                                } else {
                                    closeKeyboard(b.editTitration2)
                                    closeKeyboard(b.editTitration1)
                                    val results = FloatArray(testInfo.results!!.size)
                                    val n1 = n1String.toFloat()
                                    val formula = testInfo.results!![1].formula
                                    results[1] =
                                        MathUtil.eval(String.format(Locale.US, formula!!, n1))
                                            .toFloat()
                                    mListener!!.onSubmitResult(results)
                                }
                            }
                            return@setOnEditorActionListener true
                        }
                        false
                    }
                } else if (testInfo.results!!.size > 1) {
                    b.textInput1.text = testInfo.results!![0].name!!.toLocalString()
                    b.textInput2.text = testInfo.results!![1].name!!.toLocalString()
                    b.editTitration2.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            if (mListener != null) {
                                val n1String = b.editTitration1.text.toString()
                                val n2String = b.editTitration2.text.toString()
                                if (n1String.isEmpty()) {
                                    b.editTitration1.error = getString(R.string.value_is_required)
                                    b.editTitration1.requestFocus()
                                } else {
                                    val results = FloatArray(testInfo.results!!.size)
                                    val n1 = n1String.toFloat()
                                    if (n2String.isEmpty()) {
                                        b.editTitration2.error =
                                            getString(R.string.value_is_required)
                                        b.editTitration2.requestFocus()
                                    } else {
                                        val n2 = n2String.toFloat()
                                        if (n1 > n2) {
                                            b.editTitration1.error = getString(
                                                R.string.titration_entry_error,
                                                b.textInput1.text.toString(),
                                                b.textInput2.text.toString()
                                            )
                                            b.editTitration1.requestFocus()
                                        } else {
                                            closeKeyboard(b.editTitration2)
                                            closeKeyboard(b.editTitration1)
                                            for (i in testInfo.results!!.indices) {
                                                val formula = testInfo.results!![i].formula
                                                if (formula!!.isNotEmpty()) {
                                                    results[i] = MathUtil.eval(
                                                        String.format(
                                                            Locale.US,
                                                            formula,
                                                            n1,
                                                            n2
                                                        )
                                                    ).toFloat()
                                                }
                                            }
                                            mListener!!.onSubmitResult(results)
                                        }
                                    }
                                }
                            }
                            return@setOnEditorActionListener true
                        }
                        false
                    }
                } else {
                    b.inputTitleTxt.text = getString(R.string.enter_titration_result)
                    b.textInput1.visibility = View.GONE
                    b.textInput2.visibility = View.GONE
                    b.editTitration2.visibility = View.GONE
                    b.editTitration1.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            if (mListener != null) {
                                val n1String = b.editTitration1.text.toString()
                                if (n1String.isEmpty()) {
                                    b.editTitration1.error = getString(R.string.value_is_required)
                                    b.editTitration1.requestFocus()
                                } else {
                                    closeKeyboard(b.editTitration2)
                                    closeKeyboard(b.editTitration1)
                                    val results = FloatArray(testInfo.results!!.size)
                                    val n1 = n1String.toFloat()
                                    val formula = testInfo.results!![0].formula
                                    results[0] =
                                        MathUtil.eval(String.format(Locale.US, formula!!, n1))
                                            .toFloat()
                                    mListener!!.onSubmitResult(results)
                                }
                            }
                            return@setOnEditorActionListener true
                        }
                        false
                    }
                }
            }
        }
    }

    private fun closeKeyboard(input: EditText?) {
        try {
            val imm = requireContext().getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            imm.hideSoftInputFromWindow(input!!.windowToken, 0)
            if (activity != null) {
                val view = requireActivity().currentFocus
                if (view != null) {
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun showSoftKeyboard(view: View?) {
        if (activity != null && requireView().requestFocus()) {
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    //    private void hideSoftKeyboard(View view) {
    //        if (getActivity() != null) {
    //            InputMethodManager imm = (InputMethodManager)
    //                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    //            if (imm != null) {
    //                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    //            }
    //        }
    //    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnSubmitResultListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnSubmitResultListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnSubmitResultListener {
        fun onSubmitResult(results: FloatArray)
    }

    companion object {
        private const val ARG_PARAM1 = "param1"

        /**
         * Get the instance.
         */
        fun newInstance(testInfo: TestInfo?): TitrationInputFragment {
            val fragment = TitrationInputFragment()
            val args = Bundle()
            args.putParcelable(ARG_PARAM1, testInfo)
            fragment.arguments = args
            return fragment
        }
    }
}