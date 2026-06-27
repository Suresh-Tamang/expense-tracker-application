package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.ui.ExpenseTrackerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.ExpenseViewModelFactory

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Local SQLite Room Database & Repository
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = ExpenseRepository(database.expenseDao())
    
    // Setup ViewModel
    val factory = ExpenseViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, factory)[ExpenseViewModel::class.java]

    setContent {
      MyApplicationTheme(darkTheme = true) { // Force sleek dark mode as default for elegant finance UI
        ExpenseTrackerApp(viewModel = viewModel, activity = this)
      }
    }
  }
}
