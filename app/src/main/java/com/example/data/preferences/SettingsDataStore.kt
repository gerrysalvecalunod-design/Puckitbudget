package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CurrencyOption
import com.example.data.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pucket_settings")

data class UserSettings(
    val currency: CurrencyOption = CurrencyOption.USD,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val budgetNotificationsEnabled: Boolean = true
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val THEME_PREF = stringPreferencesKey("theme_pref")
        val BUDGET_NOTIFICATIONS = booleanPreferencesKey("budget_notifications")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            currency = CurrencyOption.fromCode(prefs[Keys.CURRENCY_CODE] ?: CurrencyOption.USD.code),
            themePreference = ThemePreference.fromName(prefs[Keys.THEME_PREF] ?: ThemePreference.SYSTEM.name),
            budgetNotificationsEnabled = prefs[Keys.BUDGET_NOTIFICATIONS] ?: true
        )
    }

    suspend fun setCurrency(currency: CurrencyOption) {
        context.dataStore.edit { it[Keys.CURRENCY_CODE] = currency.code }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME_PREF] = theme.name }
    }

    suspend fun setBudgetNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BUDGET_NOTIFICATIONS] = enabled }
    }
}
