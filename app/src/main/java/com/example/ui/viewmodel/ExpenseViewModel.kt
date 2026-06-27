package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BudgetEntity
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // Format: "YYYY-MM"
    private val _currentMonth = MutableStateFlow(getCurrentMonthKey())
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    // Persistent User Name
    private val _userName = MutableStateFlow("Julian Thorne")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // Persistent User Profile Picture Path
    private val _profilePicturePath = MutableStateFlow<String?>(null)
    val profilePicturePath: StateFlow<String?> = _profilePicturePath.asStateFlow()

    // Persistent User Currency Settings
    private val _selectedCurrency = MutableStateFlow(SUPPORTED_CURRENCIES[0])
    val selectedCurrency: StateFlow<CurrencyInfo> = _selectedCurrency.asStateFlow()

    // All expenses decrypted from local Room DB
    val allExpenses: StateFlow<List<Expense>> = repository.getAllExpensesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered expenses for the active month
    val monthlyExpenses: StateFlow<List<Expense>> = combine(allExpenses, currentMonth) { expenses, month ->
        expenses.filter { isExpenseInMonth(it, month) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected month budget StateFlow
    private val _currentBudget = MutableStateFlow<Double>(1000.0)
    val currentBudget: StateFlow<Double> = _currentBudget.asStateFlow()

    init {
        // Fetch budget for the current month key when ViewModel initializes
        viewModelScope.launch {
            _currentMonth.collect { month ->
                val budgetEntity = repository.getBudgetForMonth(month)
                _currentBudget.value = budgetEntity?.budgetAmount ?: 1000.0
            }
        }
    }

    fun loadUserName(context: Context) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        _userName.value = prefs.getString("user_name", "Julian Thorne") ?: "Julian Thorne"
    }

    fun updateUserName(context: Context, newName: String) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_name", newName).apply()
        _userName.value = newName
    }

    fun loadProfilePicture(context: Context) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        _profilePicturePath.value = prefs.getString("profile_picture_path", null)
    }

    fun updateProfilePicture(context: Context, newPath: String?) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        if (newPath == null) {
            prefs.edit().remove("profile_picture_path").apply()
        } else {
            prefs.edit().putString("profile_picture_path", newPath).apply()
        }
        _profilePicturePath.value = newPath
    }

    fun loadCurrency(context: Context) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val code = prefs.getString("currency_code", "USD") ?: "USD"
        _selectedCurrency.value = SUPPORTED_CURRENCIES.find { it.code == code } ?: SUPPORTED_CURRENCIES[0]
    }

    fun updateCurrency(context: Context, currencyCode: String) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("currency_code", currencyCode).apply()
        _selectedCurrency.value = SUPPORTED_CURRENCIES.find { it.code == currencyCode } ?: SUPPORTED_CURRENCIES[0]
    }

    fun selectMonth(monthKey: String) {
        _currentMonth.value = monthKey
    }

    fun updateBudget(amount: Double) {
        viewModelScope.launch {
            val month = _currentMonth.value
            repository.insertBudget(BudgetEntity(month, amount))
            _currentBudget.value = amount
        }
    }

    fun addExpense(context: Context, amount: Double, category: String, description: String, date: Date) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                category = category,
                description = description,
                timestamp = date.time
            )
            repository.insertExpense(expense)

            // Perform budget check and trigger local notification alerts
            checkBudgetAndAlert(context, expense.timestamp)
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    private suspend fun checkBudgetAndAlert(context: Context, timestamp: Long) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val expenseMonth = String.format(Locale.US, "%04d-%02d", year, month)
        
        // Fetch expenses & budget for that month to compute totals
        // Await the actual first updated list emission from Flow
        val allExp = repository.getAllExpensesFlow().first()
        val monthExpenses = allExp.filter { isExpenseInMonth(it, expenseMonth) }
        val totalSpent = monthExpenses.sumOf { it.amount }
        val budgetAmount = repository.getBudgetForMonth(expenseMonth)?.budgetAmount ?: 1000.0

        NotificationHelper.triggerBudgetAlert(context, expenseMonth, totalSpent, budgetAmount, selectedCurrency.value.symbol)
    }

    private fun getCurrentMonthKey(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    private fun isExpenseInMonth(expense: Expense, monthKey: String): Boolean {
        return try {
            val parts = monthKey.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1 // Calendar months are 0-indexed

            val cal = Calendar.getInstance().apply {
                timeInMillis = expense.timestamp
            }
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        } catch (e: Exception) {
            false
        }
    }
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val displayName: String
)

val SUPPORTED_CURRENCIES = listOf(
    CurrencyInfo("USD", "$", "US Dollar ($)"),
    CurrencyInfo("EUR", "€", "Euro (€)"),
    CurrencyInfo("GBP", "£", "British Pound (£)"),
    CurrencyInfo("JPY", "¥", "Japanese Yen (¥)"),
    CurrencyInfo("INR", "₹", "Indian Rupee (₹)"),
    CurrencyInfo("AUD", "A$", "Australian Dollar (A$)"),
    CurrencyInfo("CAD", "C$", "Canadian Dollar (C$)"),
    CurrencyInfo("CNY", "元", "Chinese Yuan (元)"),
    CurrencyInfo("CHF", "Fr", "Swiss Franc (Fr)"),
    CurrencyInfo("SGD", "S$", "Singapore Dollar (S$)"),
    CurrencyInfo("AED", "د.إ", "UAE Dirham (د.إ)"),
    CurrencyInfo("NPR", "₨", "Nepalese Rupee (₨)")
)
