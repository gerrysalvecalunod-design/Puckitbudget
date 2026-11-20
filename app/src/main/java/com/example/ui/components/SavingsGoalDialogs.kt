package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.SavingsGoalEntity
import com.example.data.model.CurrencyOption
import com.example.ui.theme.SavingsGoalPalette
import com.example.ui.utils.formatDate
import com.example.ui.utils.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSavingsGoalDialog(
    existingGoal: SavingsGoalEntity?,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        targetAmount: Double,
        currentOrInitialAmount: Double,
        targetDateMillis: Long?,
        colorIndex: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(existingGoal?.title ?: "") }
    var targetText by remember {
        mutableStateOf(
            existingGoal?.targetAmount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var initialText by remember {
        mutableStateOf(
            existingGoal?.currentAmount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var targetDateMillis by remember { mutableStateOf(existingGoal?.targetDateMillis) }
    var colorIndex by remember { mutableIntStateOf(existingGoal?.colorIndex ?: 0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        targetDateMillis = pickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("Set Date")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        targetDateMillis = null
                        showDatePicker = false
                    }
                ) {
                    Text("Clear")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingGoal != null) "Edit Savings Goal" else "New Savings Goal") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMsg = null
                    },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g., Emergency Fund, Vacation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = {
                        val cleaned = it.filter { ch -> ch.isDigit() || ch == '.' }
                        if (cleaned.count { ch -> ch == '.' } <= 1) {
                            targetText = cleaned
                            errorMsg = null
                        }
                    },
                    label = { Text("Target Amount") },
                    prefix = { Text("${currency.symbol} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_target_input")
                )

                OutlinedTextField(
                    value = initialText,
                    onValueChange = {
                        val cleaned = it.filter { ch -> ch.isDigit() || ch == '.' }
                        if (cleaned.count { ch -> ch == '.' } <= 1) {
                            initialText = cleaned
                        }
                    },
                    label = { Text(if (existingGoal != null) "Current Saved Amount" else "Initial Deposit (Optional)") },
                    prefix = { Text("${currency.symbol} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_initial_input")
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = targetDateMillis?.let { "Target: ${formatDate(it)}" } ?: "Set Target Date (Optional)"
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Goal Color", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SavingsGoalPalette.forEachIndexed { idx, color ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (colorIndex == idx) {
                                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { colorIndex = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorIndex == idx) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected color",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                errorMsg?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTarget = targetText.toDoubleOrNull()
                    if (title.isBlank()) {
                        errorMsg = "Please enter a goal name"
                        return@Button
                    }
                    if (parsedTarget == null || parsedTarget <= 0.0) {
                        errorMsg = "Please enter a valid target amount"
                        return@Button
                    }
                    val parsedInitial = initialText.toDoubleOrNull() ?: 0.0
                    onSave(title, parsedTarget, parsedInitial, targetDateMillis, colorIndex)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_goal_btn")
            ) {
                Text(if (existingGoal != null) "Update Goal" else "Create Goal")
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
fun AddContributionDialog(
    goal: SavingsGoalEntity,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onContribute: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val projectedTotal = goal.currentAmount + parsedAmount
    val projectedPct = ((projectedTotal / goal.targetAmount.coerceAtLeast(1.0)) * 100).toInt().coerceIn(0, 100)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${goal.title}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Saved: ${formatMoney(goal.currentAmount, currency)} / ${formatMoney(goal.targetAmount, currency)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(25, 50, 100, 250).forEach { preset ->
                        AssistChip(
                            onClick = {
                                val curr = amountText.toDoubleOrNull() ?: 0.0
                                val updated = curr + preset
                                amountText = if (updated % 1.0 == 0.0) updated.toLong().toString() else updated.toString()
                                errorMsg = null
                            },
                            label = { Text("+${currency.symbol}$preset", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        val cleaned = it.filter { ch -> ch.isDigit() || ch == '.' }
                        if (cleaned.count { ch -> ch == '.' } <= 1) {
                            amountText = cleaned
                            errorMsg = null
                        }
                    },
                    label = { Text("Contribution Amount") },
                    prefix = { Text("${currency.symbol} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contribution_amount_input")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g., Weekly deposit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (parsedAmount > 0.0) {
                    Text(
                        text = "New progress: ${formatMoney(projectedTotal, currency)} ($projectedPct%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                errorMsg?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMsg = "Enter an amount greater than 0"
                        return@Button
                    }
                    onContribute(amount, note)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_contribution_btn")
            ) {
                Text("Add Savings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
