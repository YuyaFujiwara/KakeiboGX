package com.example.myapplication.ui.input

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myapplication.databinding.DialogCalculatorBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CalculatorBottomSheet(
    private val initialAmount: Long,
    private val onResult: (Long) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogCalculatorBinding? = null
    private val binding get() = _binding!!

    private var currentInput = if (initialAmount > 0) initialAmount.toString() else "0"
    private var previousInput = ""
    private var currentOperator = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDisplay()

        val numberButtons = listOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9", binding.btn00 to "00"
        )

        for ((btn, num) in numberButtons) {
            btn.setOnClickListener { onNumber(num) }
        }

        binding.btnPlus.setOnClickListener { onOperator("+") }
        binding.btnMinus.setOnClickListener { onOperator("-") }
        binding.btnMultiply.setOnClickListener { onOperator("*") }
        binding.btnDivide.setOnClickListener { onOperator("/") }
        
        binding.btnEqual.setOnClickListener {
            if (currentOperator.isNotEmpty()) {
                calculate()
            }
        }

        binding.btnClear.setOnClickListener {
            currentInput = "0"
            previousInput = ""
            currentOperator = ""
            updateDisplay()
        }

        binding.btnDel.setOnClickListener {
            if (currentInput.isNotEmpty() && currentInput != "0") {
                currentInput = currentInput.dropLast(1)
                if (currentInput.isEmpty()) {
                    currentInput = "0"
                }
            }
            updateDisplay()
        }

        binding.btnOk.setOnClickListener {
            if (currentOperator.isNotEmpty()) {
                calculate()
            }
            val result = currentInput.toLongOrNull() ?: 0L
            onResult(result)
            dismiss()
        }
    }

    private fun onNumber(num: String) {
        if (currentInput == "0" && num != "00") {
            currentInput = num
        } else if (currentInput != "0") {
            // 入力桁数を制限(約10億の桁まで)
            if (currentInput.length < 10) {
                currentInput += num
            }
        }
        updateDisplay()
    }

    private fun onOperator(op: String) {
        if (currentOperator.isNotEmpty()) {
            calculate()
        }
        previousInput = currentInput
        currentInput = "0"
        currentOperator = op
        updateDisplay()
    }

    private fun calculate() {
        val prev = previousInput.toLongOrNull() ?: 0L
        val curr = currentInput.toLongOrNull() ?: 0L
        val result = when (currentOperator) {
            "+" -> prev + curr
            "-" -> prev - curr
            "*" -> prev * curr
            "/" -> if (curr != 0L) prev / curr else 0L
            else -> curr
        }
        // マイナス金額は0とする
        val nonNegativeResult = if (result < 0) 0L else result
        currentInput = nonNegativeResult.toString()
        currentOperator = ""
        previousInput = ""
        updateDisplay()
    }

    private fun updateDisplay() {
        binding.tvDisplay.text = "¥%,d".format(currentInput.toLongOrNull() ?: 0L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
