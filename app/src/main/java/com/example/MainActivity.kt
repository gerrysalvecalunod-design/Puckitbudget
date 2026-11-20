package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PucketBudgetApp
import com.example.ui.theme.PucketbudgetTheme
import com.example.ui.viewmodel.BudgetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BudgetViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PucketbudgetTheme(themePreference = uiState.settings.themePreference) {
                PucketBudgetApp(viewModel = viewModel)
            }
        }
    }
}
