package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetPeriod
import com.example.data.model.BudgetProgress
import com.example.data.model.CurrencyOption
import com.example.data.model.ExpenseCategory
import com.example.ui.components.EditBudgetsDialog
import com.example.ui.components.EditCategoryLimitsDialog
import com.example.ui.components.SpendingBarChart
import com.example.ui.utils.formatMoney
import com.example.ui.viewmodel.PucketUiState

@Composable
fun BudgetScreen(
    uiState: PucketUiState,
    showReportsTab: Boolean,
    onToggleReportsTab: (Boolean) -> Unit,
    onSaveBudgets: (daily: Double, weekly: Double, monthly: Double, threshold: Int) -> Unit,
    onSaveCategoryLimit: (ExpenseCategory, Double) -> Unit,
    onOpenSavingsTab: () -> Unit
) {
    val currency = uiState.settings.currency
    var showEditBudgets by remember { mutableStateOf(false) }
    var showCategoryLimits by remember { mutableStateOf(false) }
    var chartPeriod by remember { mutableStateOf(BudgetPeriod.DAILY) }

    if (showEditBudgets) {
        EditBudgetsDialog(
            currentBudget = uiState.budget,
            currency = currency,
            onDismiss = { showEditBudgets = false },
            onSave = onSaveBudgets
        )
    }

    if (showCategoryLimits) {
        EditCategoryLimitsDialog(
            existingLimits = uiState.categoryLimits,
            currency = currency,
            onDismiss = { showCategoryLimits = false },
            onSaveLimit = onSaveCategoryLimit
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showReportsTab) "Financial Reports" else "Budget & Limits",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (!showReportsTab) {
                            OutlinedButton(
                                onClick = { showEditBudgets = true },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("edit_budgets_btn")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Set Limits")
                            }
                        }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !showReportsTab,
                            onClick = { onToggleReportsTab(false) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("segment_budgets")
                        ) {
                            Text("Budgets")
                        }
                        SegmentedButton(
                            selected = showReportsTab,
                            onClick = { onToggleReportsTab(true) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("segment_reports")
                        ) {
                            Text("Reports")
                        }
                    }
                }
            }

            if (showReportsTab) {
                item {
                    ReportsSectionContent(uiState = uiState)
                }
            } else {
                // Budget Warnings Card
                item {
                    if (uiState.warnings.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Spending Within Safe Threshold",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Warning threshold set to ${uiState.budget.warningThresholdPercent}% of limits.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.warnings.forEach { warning ->
                                val bg = if (warning.isExceeded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                                val fg = if (warning.isExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = bg)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = fg)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = warning.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = fg
                                            )
                                            Text(
                                                text = warning.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = fg
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily, Weekly, Monthly Budget Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BudgetPeriodCard(
                            progress = uiState.dailyProgress,
                            thresholdPercent = uiState.budget.warningThresholdPercent,
                            currency = currency,
                            onEdit = { showEditBudgets = true }
                        )
                        BudgetPeriodCard(
                            progress = uiState.weeklyProgress,
                            thresholdPercent = uiState.budget.warningThresholdPercent,
                            currency = currency,
                            onEdit = { showEditBudgets = true }
                        )
                        BudgetPeriodCard(
                            progress = uiState.monthlyProgress,
                            thresholdPercent = uiState.budget.warningThresholdPercent,
                            currency = currency,
                            onEdit = { showEditBudgets = true }
                        )
                    }
                }

                // Category Spending Limits Button
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Category Spending Limits",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${uiState.categoryLimits.size} custom category caps configured",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(onClick = { showCategoryLimits = true }) {
                                Text("Manage")
                            }
                        }
                    }
                }

                // Spending Chart Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Spending Chart",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                BudgetPeriod.entries.forEachIndexed { idx, period ->
                                    SegmentedButton(
                                        selected = chartPeriod == period,
                                        onClick = { chartPeriod = period },
                                        shape = SegmentedButtonDefaults.itemShape(idx, BudgetPeriod.entries.size)
                                    ) {
                                        Text(period.label)
                                    }
                                }
                            }
                            val pts = when (chartPeriod) {
                                BudgetPeriod.DAILY -> uiState.reportsData.dailyPoints
                                BudgetPeriod.WEEKLY -> uiState.reportsData.weeklyPoints
                                BudgetPeriod.MONTHLY -> uiState.reportsData.monthlyPoints
                            }
                            SpendingBarChart(
                                points = pts,
                                showIncomeComparison = false,
                                currency = currency
                            )
                        }
                    }
                }

                // Savings Progress Summary Card
                item {
                    val reports = uiState.reportsData
                    val savingsPct = if (reports.totalSavingsTarget > 0.0) {
                        (reports.totalSavedAmount / reports.totalSavingsTarget).toFloat().coerceIn(0f, 1f)
                    } else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Savings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Savings Progress",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                TextButton(onClick = onOpenSavingsTab) {
                                    Text("View Goals")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Saved: ${formatMoney(reports.totalSavedAmount, currency)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Target: ${formatMoney(reports.totalSavingsTarget, currency)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            LinearProgressIndicator(
                                progress = { savingsPct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                strokeCap = StrokeCap.Round
                            )

                            Text(
                                text = "${reports.totalSavingsGoalsCount} active goals • ${reports.completedSavingsGoalsCount} completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun BudgetPeriodCard(
    progress: BudgetProgress,
    thresholdPercent: Int,
    currency: CurrencyOption,
    onEdit: () -> Unit
) {
    val animatedRatio by animateFloatAsState(
        targetValue = progress.ratio.coerceIn(0f, 1f),
        animationSpec = tween(550),
        label = "budget_${progress.period.name}"
    )

    val barColor = when {
        progress.limit <= 0.0 -> MaterialTheme.colorScheme.outline
        progress.percentUsed >= 100 -> MaterialTheme.colorScheme.error
        progress.percentUsed >= thresholdPercent -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${progress.period.label} Budget",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (progress.limit > 0.0) {
                    Surface(
                        color = barColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${progress.percentUsed}% used",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = barColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    TextButton(onClick = onEdit) {
                        Text("Set Limit")
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(50)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${formatMoney(progress.spent, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (progress.limit > 0.0) {
                        "Remaining: ${formatMoney(progress.remaining, currency)} / ${formatMoney(progress.limit, currency)}"
                    } else {
                        "No spending limit set"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (progress.limit > 0.0 && progress.remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
