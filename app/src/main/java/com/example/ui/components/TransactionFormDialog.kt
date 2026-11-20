package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.data.model.CurrencyOption
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeCategory
import com.example.data.model.TransactionType
import com.example.ui.utils.formatDate
import com.example.ui.utils.getCategoryIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    initialType: TransactionType,
    existingTransaction: TransactionEntity?,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long?,
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        timestamp: Long
    ) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember {
        mutableStateOf(
            existingTransaction?.let {
                if (it.type == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE
            } ?: initialType
        )
    }
    var amountText by remember {
        mutableStateOf(
            existingTransaction?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var selectedExpenseCategory by remember {
        mutableStateOf(
            if (existingTransaction?.type == TransactionType.EXPENSE.name) {
                ExpenseCategory.fromNameOrLabel(existingTransaction.category)
            } else {
                ExpenseCategory.FOOD
            }
        )
    }
    var selectedIncomeCategory by remember {
        mutableStateOf(
            if (existingTransaction?.type == TransactionType.INCOME.name) {
                IncomeCategory.fromNameOrLabel(existingTransaction.category)
            } else {
                IncomeCategory.SALARY
            }
        )
    }
    var description by remember { mutableStateOf(existingTransaction?.description ?: "") }
    var selectedDateMillis by remember {
        mutableLongStateOf(existingTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm && existingTransaction != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Record?") },
            text = { Text("This will permanently remove this ${selectedType.label.lowercase()} record.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(existingTransaction.id)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTransaction != null) "Edit ${selectedType.label}" else "New ${selectedType.label}",
                    style = MaterialTheme.typography.headlineMedium
                )
                if (existingTransaction != null && onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("sheet_delete_transaction_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TransactionType.entries.size
                        ),
                        modifier = Modifier.testTag("tx_type_${type.name.lowercase()}")
                    ) {
                        Text(type.label)
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountText = filtered
                        amountError = null
                    }
                },
                label = { Text("Amount (${currency.code})") },
                prefix = {
                    Text(
                        text = "${currency.symbol} ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                isError = amountError != null,
                supportingText = amountError?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Note") },
                placeholder = {
                    Text(
                        if (selectedType == TransactionType.EXPENSE) "e.g., Grocery run, Train ticket"
                        else "e.g., Monthly salary, Design project"
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_description_input")
            )

            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (selectedType == TransactionType.EXPENSE) {
                    ExpenseCategory.entries.forEach { cat ->
                        val isSelected = selectedExpenseCategory == cat
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedExpenseCategory = cat },
                            label = { Text(cat.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(TransactionType.EXPENSE.name, cat.name),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else catColor
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("cat_chip_${cat.name.lowercase()}")
                        )
                    }
                } else {
                    IncomeCategory.entries.forEach { cat ->
                        val isSelected = selectedIncomeCategory == cat
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedIncomeCategory = cat },
                            label = { Text(cat.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(TransactionType.INCOME.name, cat.name),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else catColor
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("cat_chip_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("tx_date_picker_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = "Select date",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Date: ${formatDate(selectedDateMillis)}")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val parsed = amountText.toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        amountError = "Enter a valid amount greater than 0"
                        return@Button
                    }
                    val categoryName = if (selectedType == TransactionType.EXPENSE) {
                        selectedExpenseCategory.name
                    } else {
                        selectedIncomeCategory.name
                    }
                    onSave(
                        existingTransaction?.id,
                        selectedType,
                        parsed,
                        categoryName,
                        description,
                        selectedDateMillis
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("tx_save_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (existingTransaction != null) "Update ${selectedType.label}" else "Save ${selectedType.label}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
