package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryLimitEntity
import com.example.data.model.CurrencyOption
import com.example.data.model.ExpenseCategory
import kotlin.math.roundToInt

@Composable
fun EditBudgetsDialog(
    currentBudget: BudgetEntity,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (daily: Double, weekly: Double, monthly: Double, thresholdPercent: Int) -> Unit
) {
    var dailyText by remember {
        mutableStateOf(if (currentBudget.dailyLimit > 0) formatInputNum(currentBudget.dailyLimit) else "")
    }
    var weeklyText by remember {
        mutableStateOf(if (currentBudget.weeklyLimit > 0) formatInputNum(currentBudget.weeklyLimit) else "")
    }
    var monthlyText by remember {
        mutableStateOf(if (currentBudget.monthlyLimit > 0) formatInputNum(currentBudget.monthlyLimit) else "")
    }
    var threshold by remember {
        mutableFloatStateOf(currentBudget.warningThresholdPercent.toFloat())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Budget & Spending Limits") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Set your spending caps for daily, weekly, and monthly periods.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = dailyText,
                    onValueChange = { dailyText = filterDecimal(it) },
                    label = { Text("Daily Budget Limit") },
                    prefix = { Text("${currency.symbol} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("daily_limit_input")
                )

                OutlinedTextField(
                    value = weeklyText,
                    onValueChange = { weeklyText = filterDecimal(it) },
                    label = { Text("Weekly Budget Limit") },
                    prefix = { Text("${currency.symbol} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("weekly_limit_input")
                )

                OutlinedTextField(
                    value = monthlyText,
                    onValueChange = { monthlyText = filterDecimal(it) },
                    label = { Text("Monthly Budget Limit") },
                    prefix = { Text("${currency.symbol} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("monthly_limit_input")
                )

                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Warning Alert Threshold", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${threshold.roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 50f..95f,
                        steps = 8
                    )
                    Text(
                        text = "Triggers alerts when spending reaches ${threshold.roundToInt()}% of a budget.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        dailyText.toDoubleOrNull() ?: 0.0,
                        weeklyText.toDoubleOrNull() ?: 0.0,
                        monthlyText.toDoubleOrNull() ?: 0.0,
                        threshold.roundToInt()
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_budgets_btn")
            ) {
                Text("Save Limits")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditCategoryLimitsDialog(
    existingLimits: List<CategoryLimitEntity>,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSaveLimit: (ExpenseCategory, Double) -> Unit
) {
    val limitMap = remember(existingLimits) {
        existingLimits.associate { it.category to it.monthlyLimit }.toMutableMap()
    }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var limitText by remember(selectedCategory, existingLimits) {
        val current = limitMap[selectedCategory.name] ?: 0.0
        mutableStateOf(if (current > 0.0) formatInputNum(current) else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Category Spending Limits") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select a category to set or clear (0) its monthly spending limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExpenseCategory.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val hasLimit = (limitMap[cat.name] ?: 0.0) > 0.0
                            TextButton(
                                onClick = { selectedCategory = cat },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (hasLimit) "• ${cat.label}" else cat.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = filterDecimal(it) },
                    label = { Text("${selectedCategory.label} Monthly Limit") },
                    prefix = { Text("${currency.symbol} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = limitText.toDoubleOrNull() ?: 0.0
                    onSaveLimit(selectedCategory, value)
                    onDismiss()
                }
            ) {
                Text("Save Category Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun filterDecimal(input: String): String {
    val cleaned = input.filter { it.isDigit() || it == '.' }
    return if (cleaned.count { it == '.' } <= 1) cleaned else cleaned.dropLast(1)
}

private fun formatInputNum(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
