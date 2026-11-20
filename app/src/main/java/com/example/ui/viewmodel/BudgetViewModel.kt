package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryLimitEntity
import com.example.data.local.PucketDatabase
import com.example.data.local.SavingsContributionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.BudgetPeriod
import com.example.data.model.BudgetProgress
import com.example.data.model.BudgetWarning
import com.example.data.model.CategoryBreakdown
import com.example.data.model.CurrencyOption
import com.example.data.model.DateFilter
import com.example.data.model.ExpenseCategory
import com.example.data.model.PeriodChartPoint
import com.example.data.model.ReportsData
import com.example.data.model.SortOrder
import com.example.data.model.ThemePreference
import com.example.data.model.TransactionType
import com.example.data.preferences.SettingsDataStore
import com.example.data.preferences.UserSettings
import com.example.data.repository.BudgetRepository
import com.example.notifications.BudgetNotificationHelper
import com.example.ui.utils.formatMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TransactionFilterState(
    val searchQuery: String = "",
    val typeFilter: TransactionType? = null,
    val categoryFilter: String? = null,
    val dateFilter: DateFilter = DateFilter.ALL,
    val customDateMillis: Long? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC
)

data class PucketUiState(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val budget: BudgetEntity = BudgetEntity(),
    val categoryLimits: List<CategoryLimitEntity> = emptyList(),
    val dailyProgress: BudgetProgress = BudgetProgress(BudgetPeriod.DAILY, 0.0, 0.0),
    val weeklyProgress: BudgetProgress = BudgetProgress(BudgetPeriod.WEEKLY, 0.0, 0.0),
    val monthlyProgress: BudgetProgress = BudgetProgress(BudgetPeriod.MONTHLY, 0.0, 0.0),
    val warnings: List<BudgetWarning> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val savingsContributions: List<SavingsContributionEntity> = emptyList(),
    val reportsData: ReportsData = ReportsData(),
    val settings: UserSettings = UserSettings(),
    val filterState: TransactionFilterState = TransactionFilterState()
)

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BudgetRepository

    init {
        val db = PucketDatabase.getInstance(application)
        val settingsDataStore = SettingsDataStore(application)
        repository = BudgetRepository(db.pucketDao(), settingsDataStore)
        BudgetNotificationHelper.ensureChannel(application)
    }

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val coreDataFlow = combine(
        repository.transactions,
        repository.budget,
        repository.categoryLimits,
        repository.savingsGoals,
        repository.savingsContributions
    ) { transactions, budget, categoryLimits, goals, contributions ->
        CoreBundle(transactions, budget, categoryLimits, goals, contributions)
    }

    val uiState: StateFlow<PucketUiState> = combine(
        coreDataFlow,
        repository.userSettings,
        _filterState
    ) { bundle, settings, filters ->
        buildUiState(bundle, settings, filters)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PucketUiState()
    )

    private data class CoreBundle(
        val transactions: List<TransactionEntity>,
        val budget: BudgetEntity,
        val categoryLimits: List<CategoryLimitEntity>,
        val goals: List<SavingsGoalEntity>,
        val contributions: List<SavingsContributionEntity>
    )

    private fun buildUiState(
        bundle: CoreBundle,
        settings: UserSettings,
        filters: TransactionFilterState
    ): PucketUiState {
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(now)
        val todayEnd = endOfDay(now)
        val weekStart = startOfWeek(now)
        val monthStart = startOfMonth(now)
        val monthEnd = endOfMonth(now)

        val allTx = bundle.transactions
        val totalIncome = allTx.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val totalExpenses = allTx.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val totalBalance = totalIncome - totalExpenses

        val expenses = allTx.filter { it.type == TransactionType.EXPENSE.name }
        val dailySpent = expenses.filter { it.timestamp in todayStart..todayEnd }.sumOf { it.amount }
        val weeklySpent = expenses.filter { it.timestamp in weekStart..todayEnd }.sumOf { it.amount }
        val monthlySpent = expenses.filter { it.timestamp in monthStart..monthEnd }.sumOf { it.amount }

        val dailyProgress = BudgetProgress(BudgetPeriod.DAILY, bundle.budget.dailyLimit, dailySpent)
        val weeklyProgress = BudgetProgress(BudgetPeriod.WEEKLY, bundle.budget.weeklyLimit, weeklySpent)
        val monthlyProgress = BudgetProgress(BudgetPeriod.MONTHLY, bundle.budget.monthlyLimit, monthlySpent)

        val warnings = computeWarnings(
            dailyProgress,
            weeklyProgress,
            monthlyProgress,
            bundle.budget.warningThresholdPercent,
            bundle.categoryLimits,
            expenses.filter { it.timestamp in monthStart..monthEnd },
            settings.currency
        )

        val filtered = applyTransactionFilters(allTx, filters)
        val reports = buildReportsData(allTx, bundle.goals, now)

        return PucketUiState(
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            budget = bundle.budget,
            categoryLimits = bundle.categoryLimits,
            dailyProgress = dailyProgress,
            weeklyProgress = weeklyProgress,
            monthlyProgress = monthlyProgress,
            warnings = warnings,
            recentTransactions = allTx.take(5),
            filteredTransactions = filtered,
            allTransactions = allTx,
            savingsGoals = bundle.goals,
            savingsContributions = bundle.contributions,
            reportsData = reports,
            settings = settings,
            filterState = filters
        )
    }

    private fun computeWarnings(
        daily: BudgetProgress,
        weekly: BudgetProgress,
        monthly: BudgetProgress,
        threshold: Int,
        categoryLimits: List<CategoryLimitEntity>,
        monthExpenses: List<TransactionEntity>,
        currency: CurrencyOption
    ): List<BudgetWarning> {
        val list = mutableListOf<BudgetWarning>()
        listOf(daily, weekly, monthly).forEach { progress ->
            if (progress.limit > 0.0 && progress.percentUsed >= threshold) {
                val exceeded = progress.spent > progress.limit
                list.add(
                    BudgetWarning(
                        id = "warn_${progress.period.name}",
                        title = if (exceeded) "${progress.period.label} Budget Exceeded" else "${progress.period.label} Budget Warning",
                        message = if (exceeded) {
                            "Spent ${formatMoney(progress.spent, currency)} of ${formatMoney(progress.limit, currency)} (${progress.percentUsed}%)."
                        } else {
                            "Reached ${progress.percentUsed}% of your ${progress.period.label.lowercase()} limit (${formatMoney(progress.remaining, currency)} left)."
                        },
                        isExceeded = exceeded
                    )
                )
            }
        }
        categoryLimits.forEach { limit ->
            if (limit.monthlyLimit > 0.0) {
                val catSpent = monthExpenses
                    .filter { it.category == limit.category }
                    .sumOf { it.amount }
                val pct = ((catSpent / limit.monthlyLimit) * 100).toInt()
                if (pct >= threshold) {
                    val catLabel = ExpenseCategory.fromNameOrLabel(limit.category).label
                    val exceeded = catSpent > limit.monthlyLimit
                    list.add(
                        BudgetWarning(
                            id = "warn_cat_${limit.category}",
                            title = if (exceeded) "$catLabel Limit Exceeded" else "$catLabel Limit Alert",
                            message = "$catLabel spending is at $pct% (${formatMoney(catSpent, currency)} / ${formatMoney(limit.monthlyLimit, currency)}).",
                            isExceeded = exceeded
                        )
                    )
                }
            }
        }
        return list
    }

    private fun applyTransactionFilters(
        list: List<TransactionEntity>,
        filters: TransactionFilterState
    ): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(now)
        val todayEnd = endOfDay(now)
        val weekStart = startOfWeek(now)
        val monthStart = startOfMonth(now)
        val monthEnd = endOfMonth(now)

        return list.filter { tx ->
            val matchesQuery = filters.searchQuery.isBlank() ||
                tx.description.contains(filters.searchQuery, ignoreCase = true) ||
                tx.category.contains(filters.searchQuery, ignoreCase = true)

            val matchesType = filters.typeFilter == null || tx.type == filters.typeFilter.name

            val matchesCategory = filters.categoryFilter == null || tx.category == filters.categoryFilter

            val matchesDate = when (filters.dateFilter) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> tx.timestamp in todayStart..todayEnd
                DateFilter.THIS_WEEK -> tx.timestamp in weekStart..todayEnd
                DateFilter.THIS_MONTH -> tx.timestamp in monthStart..monthEnd
                DateFilter.CUSTOM -> {
                    val target = filters.customDateMillis
                    if (target == null) true
                    else tx.timestamp in startOfDay(target)..endOfDay(target)
                }
            }

            matchesQuery && matchesType && matchesCategory && matchesDate
        }.let { filtered ->
            when (filters.sortOrder) {
                SortOrder.DATE_DESC -> filtered.sortedWith(compareByDescending<TransactionEntity> { it.timestamp }.thenByDescending { it.id })
                SortOrder.DATE_ASC -> filtered.sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id })
                SortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
                SortOrder.AMOUNT_ASC -> filtered.sortedBy { it.amount }
            }
        }
    }

    private fun buildReportsData(
        transactions: List<TransactionEntity>,
        goals: List<SavingsGoalEntity>,
        now: Long
    ): ReportsData {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        // Last 7 days daily points
        val dailyPoints = (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val start = startOfDay(cal.timeInMillis)
            val end = endOfDay(cal.timeInMillis)
            val exp = transactions.filter { it.type == TransactionType.EXPENSE.name && it.timestamp in start..end }.sumOf { it.amount }
            val inc = transactions.filter { it.type == TransactionType.INCOME.name && it.timestamp in start..end }.sumOf { it.amount }
            PeriodChartPoint(label = dayFormat.format(Date(start)), expense = exp, income = inc)
        }

        // Last 4 weeks weekly points
        val weeklyPoints = (3 downTo 0).map { weekOffset ->
            val endCal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.WEEK_OF_YEAR, -weekOffset)
            }
            val startCal = (endCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -6)
            }
            val start = startOfDay(startCal.timeInMillis)
            val end = endOfDay(endCal.timeInMillis)
            val exp = transactions.filter { it.type == TransactionType.EXPENSE.name && it.timestamp in start..end }.sumOf { it.amount }
            val inc = transactions.filter { it.type == TransactionType.INCOME.name && it.timestamp in start..end }.sumOf { it.amount }
            val label = if (weekOffset == 0) "This Wk" else "W-${weekOffset}"
            PeriodChartPoint(label = label, expense = exp, income = inc)
        }

        // Last 6 months monthly points
        val monthlyPoints = (5 downTo 0).map { monthOffset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.MONTH, -monthOffset)
            }
            val start = startOfMonth(cal.timeInMillis)
            val end = endOfMonth(cal.timeInMillis)
            val exp = transactions.filter { it.type == TransactionType.EXPENSE.name && it.timestamp in start..end }.sumOf { it.amount }
            val inc = transactions.filter { it.type == TransactionType.INCOME.name && it.timestamp in start..end }.sumOf { it.amount }
            PeriodChartPoint(label = monthFormat.format(Date(start)), expense = exp, income = inc)
        }

        val dailyTotal = dailyPoints.lastOrNull()?.expense ?: 0.0
        val dailyAvg = if (dailyPoints.isNotEmpty()) dailyPoints.sumOf { it.expense } / dailyPoints.size else 0.0
        val weeklyTotal = weeklyPoints.lastOrNull()?.expense ?: 0.0
        val weeklyAvg = if (weeklyPoints.isNotEmpty()) weeklyPoints.sumOf { it.expense } / weeklyPoints.size else 0.0
        val monthlyTotal = monthlyPoints.lastOrNull()?.expense ?: 0.0
        val monthlyAvg = if (monthlyPoints.isNotEmpty()) monthlyPoints.sumOf { it.expense } / monthlyPoints.size else 0.0

        val totalInc = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val totalExp = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val netSavings = totalInc - totalExp
        val savingsRate = if (totalInc > 0.0) (((totalInc - totalExp) / totalInc) * 100).toInt().coerceIn(0, 100) else 0

        val expenseByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { ExpenseCategory.fromNameOrLabel(it.category) }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }

        val breakdowns = expenseByCategory.map { (cat, amount) ->
            CategoryBreakdown(
                category = cat,
                amount = amount,
                percentage = if (totalExp > 0.0) (amount / totalExp).toFloat() else 0f
            )
        }

        val completedGoals = goals.count { it.currentAmount >= it.targetAmount && it.targetAmount > 0.0 }

        return ReportsData(
            dailyPoints = dailyPoints,
            weeklyPoints = weeklyPoints,
            monthlyPoints = monthlyPoints,
            dailyTotal = dailyTotal,
            dailyAverage = dailyAvg,
            weeklyTotal = weeklyTotal,
            weeklyAverage = weeklyAvg,
            monthlyTotal = monthlyTotal,
            monthlyAverage = monthlyAvg,
            totalIncome = totalInc,
            totalExpenses = totalExp,
            netSavings = netSavings,
            savingsRatePercent = savingsRate,
            categoryBreakdowns = breakdowns,
            totalSavingsGoalsCount = goals.size,
            completedSavingsGoalsCount = completedGoals,
            totalSavedAmount = goals.sumOf { it.currentAmount },
            totalSavingsTarget = goals.sumOf { it.targetAmount }
        )
    }

    // --- Actions ---
    fun addOrUpdateTransaction(
        existingId: Long?,
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = existingId ?: 0L,
                type = type.name,
                amount = amount,
                category = category,
                description = description.trim().ifBlank {
                    if (type == TransactionType.EXPENSE) {
                        ExpenseCategory.fromNameOrLabel(category).label
                    } else {
                        category
                    }
                },
                timestamp = timestamp
            )
            if (existingId != null && existingId > 0L) {
                repository.updateTransaction(entity)
                showMessage("${type.label} updated")
            } else {
                repository.addTransaction(entity)
                showMessage("${type.label} saved")
            }

            if (type == TransactionType.EXPENSE) {
                checkBudgetAndNotifyAfterExpense(amount, timestamp)
            }
        }
    }

    private fun checkBudgetAndNotifyAfterExpense(addedAmount: Double, expenseTime: Long) {
        val state = uiState.value
        if (!state.settings.budgetNotificationsEnabled) return

        val budget = state.budget
        val threshold = budget.warningThresholdPercent
        val now = System.currentTimeMillis()
        val isToday = expenseTime in startOfDay(now)..endOfDay(now)
        val isThisMonth = expenseTime in startOfMonth(now)..endOfMonth(now)

        if (isToday && budget.dailyLimit > 0.0) {
            val projectedDaily = state.dailyProgress.spent + addedAmount
            val pct = ((projectedDaily / budget.dailyLimit) * 100).toInt()
            if (pct >= threshold) {
                BudgetNotificationHelper.showBudgetWarningNotification(
                    context = getApplication(),
                    notificationId = 1001,
                    title = if (projectedDaily > budget.dailyLimit) "Daily Budget Exceeded" else "Daily Budget Alert ($pct%)",
                    message = "You've spent ${formatMoney(projectedDaily, state.settings.currency)} out of your ${formatMoney(budget.dailyLimit, state.settings.currency)} daily limit."
                )
            }
        }

        if (isThisMonth && budget.monthlyLimit > 0.0) {
            val projectedMonthly = state.monthlyProgress.spent + addedAmount
            val pct = ((projectedMonthly / budget.monthlyLimit) * 100).toInt()
            if (pct >= threshold) {
                BudgetNotificationHelper.showBudgetWarningNotification(
                    context = getApplication(),
                    notificationId = 1003,
                    title = if (projectedMonthly > budget.monthlyLimit) "Monthly Budget Exceeded" else "Monthly Budget Warning ($pct%)",
                    message = "Monthly spending reached ${formatMoney(projectedMonthly, state.settings.currency)} of ${formatMoney(budget.monthlyLimit, state.settings.currency)}."
                )
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            showMessage("Transaction deleted")
        }
    }

    fun saveBudgets(
        dailyLimit: Double,
        weeklyLimit: Double,
        monthlyLimit: Double,
        warningThreshold: Int
    ) {
        viewModelScope.launch {
            repository.saveBudget(dailyLimit, weeklyLimit, monthlyLimit, warningThreshold)
            showMessage("Budget limits updated")
        }
    }

    fun saveCategoryLimit(category: ExpenseCategory, limit: Double) {
        viewModelScope.launch {
            repository.saveCategoryLimit(category, limit)
            showMessage("${category.label} limit updated")
        }
    }

    fun addOrUpdateSavingsGoal(
        existingGoal: SavingsGoalEntity?,
        title: String,
        targetAmount: Double,
        initialOrCurrentAmount: Double,
        targetDateMillis: Long?,
        colorIndex: Int
    ) {
        viewModelScope.launch {
            if (existingGoal != null) {
                repository.updateSavingsGoal(
                    existingGoal.copy(
                        title = title.trim(),
                        targetAmount = targetAmount,
                        currentAmount = initialOrCurrentAmount,
                        targetDateMillis = targetDateMillis,
                        colorIndex = colorIndex
                    )
                )
                showMessage("Savings goal updated")
            } else {
                repository.addSavingsGoal(
                    title = title,
                    targetAmount = targetAmount,
                    initialAmount = initialOrCurrentAmount,
                    targetDateMillis = targetDateMillis,
                    colorIndex = colorIndex
                )
                showMessage("Savings goal created")
            }
        }
    }

    fun deleteSavingsGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
            showMessage("Savings goal deleted")
        }
    }

    fun addSavingsContribution(goalId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            repository.contributeToSavingsGoal(goalId, amount, note)
            showMessage("Contribution added")
        }
    }

    // --- Filters ---
    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateTypeFilter(type: TransactionType?) {
        _filterState.value = _filterState.value.copy(typeFilter = type, categoryFilter = null)
    }

    fun updateCategoryFilter(category: String?) {
        _filterState.value = _filterState.value.copy(categoryFilter = category)
    }

    fun updateDateFilter(dateFilter: DateFilter, customDateMillis: Long? = _filterState.value.customDateMillis) {
        _filterState.value = _filterState.value.copy(dateFilter = dateFilter, customDateMillis = customDateMillis)
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        _filterState.value = _filterState.value.copy(sortOrder = sortOrder)
    }

    fun resetFilters() {
        _filterState.value = TransactionFilterState()
    }

    // --- Settings ---
    fun setCurrency(currency: CurrencyOption) {
        viewModelScope.launch { repository.setCurrency(currency) }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { repository.setThemePreference(theme) }
    }

    fun setBudgetNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBudgetNotificationsEnabled(enabled) }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleData()
            showMessage("Sample budget & transaction data loaded")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            showMessage("All financial records cleared")
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    // --- Calendar Boundaries ---
    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun endOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun startOfWeek(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfMonth(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun endOfMonth(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
