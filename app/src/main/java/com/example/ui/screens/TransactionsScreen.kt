package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.data.model.DateFilter
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeCategory
import com.example.data.model.SortOrder
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionItemCard
import com.example.ui.utils.formatDate
import com.example.ui.utils.formatMoney
import com.example.ui.viewmodel.PucketUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: PucketUiState,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onDateFilterChange: (DateFilter, Long?) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onResetFilters: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onAddTransaction: () -> Unit
) {
    val filters = uiState.filterState
    val currency = uiState.settings.currency
    var showSortMenu by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

    if (showCustomDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = filters.customDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            onDateFilterChange(DateFilter.CUSTOM, it)
                        }
                        showCustomDatePicker = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    pendingDeleteTx?.let { txToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTx = null },
            title = { Text("Delete Transaction?") },
            text = {
                Text("Remove \"${txToDelete.description}\" (${formatMoney(txToDelete.amount, currency)})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(txToDelete.id)
                        pendingDeleteTx = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTx = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val categoriesToShow: List<Pair<String, String>> = when (filters.typeFilter) {
        TransactionType.EXPENSE -> ExpenseCategory.entries.map { it.name to it.label }
        TransactionType.INCOME -> IncomeCategory.entries.map { it.name to it.label }
        null -> ExpenseCategory.entries.map { it.name to it.label } +
            IncomeCategory.entries.filter { it != IncomeCategory.OTHER }.map { it.name to it.label }
    }

    val filteredNet = uiState.filteredTransactions.sumOf {
        if (it.type == TransactionType.INCOME.name) it.amount else -it.amount
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Box {
                        OutlinedButton(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("tx_sort_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(filters.sortOrder.label, style = MaterialTheme.typography.labelMedium)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    onClick = {
                                        onSortOrderChange(order)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search Input
            item {
                OutlinedTextField(
                    value = filters.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search description or category...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filters.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_search_input")
                )
            }

            // Type Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filters.typeFilter == null,
                            onClick = { onTypeFilterChange(null) },
                            label = { Text("All Types") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.typeFilter == TransactionType.EXPENSE,
                            onClick = { onTypeFilterChange(TransactionType.EXPENSE) },
                            label = { Text("Expenses") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.typeFilter == TransactionType.INCOME,
                            onClick = { onTypeFilterChange(TransactionType.INCOME) },
                            label = { Text("Income") }
                        )
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filters.categoryFilter == null,
                            onClick = { onCategoryFilterChange(null) },
                            label = { Text("All Categories") }
                        )
                    }
                    items(categoriesToShow, key = { it.first }) { (key, label) ->
                        FilterChip(
                            selected = filters.categoryFilter == key,
                            onClick = {
                                onCategoryFilterChange(if (filters.categoryFilter == key) null else key)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Date Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DateFilter.entries, key = { it.name }) { df ->
                        val isSelected = filters.dateFilter == df
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (df == DateFilter.CUSTOM) {
                                    showCustomDatePicker = true
                                } else {
                                    onDateFilterChange(df, null)
                                }
                            },
                            leadingIcon = if (df == DateFilter.CUSTOM) {
                                { Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            label = {
                                Text(
                                    if (df == DateFilter.CUSTOM && filters.customDateMillis != null) {
                                        formatDate(filters.customDateMillis)
                                    } else {
                                        df.label
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.filteredTransactions.size} records • Net ${formatMoney(filteredNet, currency, showSign = true)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val hasActiveFilters = filters.searchQuery.isNotBlank() ||
                        filters.typeFilter != null ||
                        filters.categoryFilter != null ||
                        filters.dateFilter != DateFilter.ALL
                    if (hasActiveFilters) {
                        TextButton(onClick = onResetFilters) {
                            Text("Reset Filters")
                        }
                    }
                }
            }

            if (uiState.filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("No matching transactions", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Adjust your search or filters, or tap + to add a new expense or income.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onAddTransaction) {
                                Text("Add Transaction")
                            }
                        }
                    }
                }
            } else {
                items(uiState.filteredTransactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        currency = currency,
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { pendingDeleteTx = tx }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}
