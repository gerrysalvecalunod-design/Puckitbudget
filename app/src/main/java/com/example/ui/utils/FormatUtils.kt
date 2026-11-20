package com.example.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.CurrencyOption
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeCategory
import com.example.data.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun formatMoney(amount: Double, currency: CurrencyOption, showSign: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (currency == CurrencyOption.JPY) 0 else 2
        maximumFractionDigits = if (currency == CurrencyOption.JPY) 0 else 2
    }
    val formatted = formatter.format(abs(amount))
    val sign = when {
        amount < 0 -> "-"
        showSign && amount > 0 -> "+"
        else -> ""
    }
    return "$sign${currency.symbol}$formatted"
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun formatShortDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun getCategoryIcon(type: String, categoryName: String): ImageVector {
    return if (type == TransactionType.INCOME.name) {
        when (IncomeCategory.fromNameOrLabel(categoryName)) {
            IncomeCategory.SALARY -> Icons.Rounded.Payments
            IncomeCategory.FREELANCE -> Icons.Rounded.LaptopMac
            IncomeCategory.BUSINESS -> Icons.Rounded.Storefront
            IncomeCategory.ALLOWANCE -> Icons.Rounded.Wallet
            IncomeCategory.INVESTMENT -> Icons.AutoMirrored.Rounded.TrendingUp
            IncomeCategory.GIFT -> Icons.Rounded.CardGiftcard
            IncomeCategory.OTHER -> Icons.Rounded.AccountBalance
        }
    } else {
        when (ExpenseCategory.fromNameOrLabel(categoryName)) {
            ExpenseCategory.FOOD -> Icons.Rounded.Restaurant
            ExpenseCategory.TRANSPORTATION -> Icons.Rounded.DirectionsCar
            ExpenseCategory.EDUCATION -> Icons.Rounded.School
            ExpenseCategory.BILLS -> Icons.AutoMirrored.Rounded.ReceiptLong
            ExpenseCategory.SHOPPING -> Icons.Rounded.ShoppingBag
            ExpenseCategory.ENTERTAINMENT -> Icons.Rounded.Theaters
            ExpenseCategory.HEALTH -> Icons.Rounded.Favorite
            ExpenseCategory.OTHER -> Icons.Rounded.Category
        }
    }
}

fun getCategoryColor(type: String, categoryName: String): Color {
    return if (type == TransactionType.INCOME.name) {
        Color(IncomeCategory.fromNameOrLabel(categoryName).colorHex)
    } else {
        Color(ExpenseCategory.fromNameOrLabel(categoryName).colorHex)
    }
}

fun getCategoryLabel(type: String, categoryName: String): String {
    return if (type == TransactionType.INCOME.name) {
        IncomeCategory.fromNameOrLabel(categoryName).label
    } else {
        ExpenseCategory.fromNameOrLabel(categoryName).label
    }
}
