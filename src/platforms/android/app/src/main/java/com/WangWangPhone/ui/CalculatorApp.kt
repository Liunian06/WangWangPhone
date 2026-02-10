package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorAppScreen(onClose: () -> Unit) {
    var displayText by remember { mutableStateOf("0") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    var isWaitingForOperand by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    val buttons = listOf(
        listOf("AC", "+/-", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    fun onNumberClick(number: String) {
        if (isWaitingForOperand) {
            displayText = number
            isWaitingForOperand = false
        } else {
            displayText = if (displayText == "0") number else displayText + number
        }
    }

    fun onOperatorClick(op: String) {
        val currentVal = displayText.toDoubleOrNull() ?: 0.0
        if (operand1 == null) {
            operand1 = currentVal
        } else if (operator != null && !isWaitingForOperand) {
            val result = calculate(operand1!!, currentVal, operator!!)
            displayText = formatResult(result)
            operand1 = result
        }
        operator = op
        isWaitingForOperand = true
    }

    fun onEqualsClick() {
        val currentVal = displayText.toDoubleOrNull() ?: 0.0
        if (operand1 != null && operator != null) {
            val result = calculate(operand1!!, currentVal, operator!!)
            displayText = formatResult(result)
            operand1 = null
            operator = null
            isWaitingForOperand = true
        }
    }

    fun onClearClick() {
        displayText = "0"
        operand1 = null
        operator = null
        isWaitingForOperand = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(bottom = 20.dp)
    ) {
        // Display
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = displayText,
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1
            )
        }

        // Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { label ->
                        val isOrange = label in listOf("÷", "×", "-", "+", "=")
                        val isGray = label in listOf("AC", "+/-", "%")
                        val weight = if (label == "0") 2f else 1f
                        
                        CalculatorButton(
                            label = label,
                            backgroundColor = when {
                                isOrange -> Color(0xFFF1A33C)
                                isGray -> Color(0xFFA5A5A5)
                                else -> Color(0xFF333333)
                            },
                            contentColor = if (isGray) Color.Black else Color.White,
                            modifier = Modifier.weight(weight),
                            onClick = {
                                when {
                                    label == "AC" -> onClearClick()
                                    label == "=" -> onEqualsClick()
                                    label in listOf("÷", "×", "-", "+") -> onOperatorClick(label)
                                    label == "+/-" -> {
                                        val currentVal = displayText.toDoubleOrNull()
                                        if (currentVal != null) {
                                            displayText = formatResult(currentVal * -1)
                                        }
                                    }
                                    label == "%" -> {
                                        val currentVal = displayText.toDoubleOrNull()
                                        if (currentVal != null) {
                                            displayText = formatResult(currentVal / 100.0)
                                        }
                                    }
                                    label == "." -> {
                                        if (isWaitingForOperand) {
                                            displayText = "0."
                                            isWaitingForOperand = false
                                        } else if (!displayText.contains(".")) {
                                            displayText += "."
                                        }
                                    }
                                    else -> onNumberClick(label)
                                }
                            }
                        )
                    }
                }
            }
        }
        
        // Home Indicator
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier
            .width(120.dp).height(5.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.8f))
            .align(Alignment.CenterHorizontally))
    }
}

@Composable
fun CalculatorButton(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(if (label == "0") 2.2f else 1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun calculate(op1: Double, op2: Double, operator: String): Double {
    return when (operator) {
        "+" -> op1 + op2
        "-" -> op1 - op2
        "×" -> op1 * op2
        "÷" -> if (op2 != 0.0) op1 / op2 else 0.0
        else -> op2
    }
}

private fun formatResult(result: Double): String {
    return if (result % 1.0 == 0.0) {
        result.toInt().toString()
    } else {
        String.format("%.8g", result).trimEnd('0').trimEnd('.')
    }
}
