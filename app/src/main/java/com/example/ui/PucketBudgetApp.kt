package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionFormSheet
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.viewmodel.BudgetViewModel
import kotlinx.coroutines.delay

enum class PucketTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    TRANSACTIONS("Transactions", Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_transactions"),
    BUDGET("Budget", Icons.Rounded.PieChart, Icons.Outlined.PieChart, "nav_budget"),
    SAVINGS("Savings", Icons.Rounded.Savings, Icons.Outlined.Savings, "nav_savings"),
    SETTINGS("Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
}

@Composable
fun PucketBudgetApp(viewModel: BudgetViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showReportsInBudget by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (showSplash) {
            delay(1050)
            showSplash = false
        }
    }

    var showTransactionSheet by remember { mutableStateOf(false) }
    var sheetInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val currentTab = PucketTab.entries.getOrElse(selectedTabIndex) { PucketTab.HOME }

    // Back handling when in Reports sub-view or non-Home tabs
    BackHandler(enabled = selectedTabIndex != 0 || showReportsInBudget) {
        if (currentTab == PucketTab.BUDGET && showReportsInBudget) {
            showReportsInBudget = false
        } else {
            selectedTabIndex = 0
            showReportsInBudget = false
        }
    }

    if (showTransactionSheet) {
        TransactionFormSheet(
            initialType = sheetInitialType,
            existingTransaction = editingTransaction,
            currency = uiState.settings.currency,
            onDismiss = {
                showTransactionSheet = false
                editingTransaction = null
            },
            onSave = { id, type, amount, category, desc, time ->
                viewModel.addOrUpdateTransaction(id, type, amount, category, desc, time)
            },
            onDelete = { id -> viewModel.deleteTransaction(id) }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (currentTab == PucketTab.HOME || currentTab == PucketTab.TRANSACTIONS) {
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            sheetInitialType = TransactionType.EXPENSE
                            showTransactionSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add transaction"
                        )
                    }
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        PucketTab.entries.forEachIndexed { index, tab ->
                            val isSelected = selectedTabIndex == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    selectedTabIndex = index
                                    if (tab != PucketTab.BUDGET) {
                                        showReportsInBudget = false
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        PucketTab.entries.forEachIndexed { index, tab ->
                            val isSelected = selectedTabIndex == index
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    selectedTabIndex = index
                                    if (tab != PucketTab.BUDGET) {
                                        showReportsInBudget = false
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        PucketTab.HOME -> HomeScreen(
                            uiState = uiState,
                            onAddExpense = {
                                editingTransaction = null
                                sheetInitialType = TransactionType.EXPENSE
                                showTransactionSheet = true
                            },
                            onAddIncome = {
                                editingTransaction = null
                                sheetInitialType = TransactionType.INCOME
                                showTransactionSheet = true
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                showTransactionSheet = true
                            },
                            onSeeAllTransactions = { selectedTabIndex = PucketTab.TRANSACTIONS.ordinal },
                            onOpenBudget = {
                                showReportsInBudget = false
                                selectedTabIndex = PucketTab.BUDGET.ordinal
                            },
                            onOpenReports = {
                                showReportsInBudget = true
                                selectedTabIndex = PucketTab.BUDGET.ordinal
                            },
                            onOpenSavings = { selectedTabIndex = PucketTab.SAVINGS.ordinal }
                        )

                        PucketTab.TRANSACTIONS -> TransactionsScreen(
                            uiState = uiState,
                            onSearchChange = viewModel::updateSearchQuery,
                            onTypeFilterChange = viewModel::updateTypeFilter,
                            onCategoryFilterChange = viewModel::updateCategoryFilter,
                            onDateFilterChange = viewModel::updateDateFilter,
                            onSortOrderChange = viewModel::updateSortOrder,
                            onResetFilters = viewModel::resetFilters,
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                showTransactionSheet = true
                            },
                            onDeleteTransaction = viewModel::deleteTransaction,
                            onAddTransaction = {
                                editingTransaction = null
                                sheetInitialType = TransactionType.EXPENSE
                                showTransactionSheet = true
                            }
                        )

                        PucketTab.BUDGET -> BudgetScreen(
                            uiState = uiState,
                            showReportsTab = showReportsInBudget,
                            onToggleReportsTab = { showReportsInBudget = it },
                            onSaveBudgets = viewModel::saveBudgets,
                            onSaveCategoryLimit = viewModel::saveCategoryLimit,
                            onOpenSavingsTab = { selectedTabIndex = PucketTab.SAVINGS.ordinal }
                        )

                        PucketTab.SAVINGS -> SavingsScreen(
                            uiState = uiState,
                            onCreateOrUpdateGoal = viewModel::addOrUpdateSavingsGoal,
                            onDeleteGoal = viewModel::deleteSavingsGoal,
                            onContribute = viewModel::addSavingsContribution
                        )

                        PucketTab.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            onCurrencyChange = viewModel::setCurrency,
                            onThemeChange = viewModel::setThemePreference,
                            onNotificationsToggle = viewModel::setBudgetNotificationsEnabled,
                            onLoadSampleData = viewModel::loadSampleData,
                            onClearAllData = viewModel::clearAllData,
                            onNotificationPermissionDenied = {
                                viewModel.showMessage("Notification permission denied. In-app warnings remain active.")
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(durationMillis = 320))
        ) {
            SplashScreen()
        }
    }
}
