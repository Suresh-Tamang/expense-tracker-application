package com.example.data

import com.example.security.CryptoHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getAllExpensesFlow(): Flow<List<Expense>> {
        return expenseDao.getAllExpensesFlow().map { entities ->
            entities.map { entity ->
                decryptEntity(entity)
            }
        }
    }

    suspend fun insertExpense(expense: Expense) {
        val entity = encryptEntity(expense)
        expenseDao.insertExpense(entity)
    }

    suspend fun updateExpense(expense: Expense) {
        val entity = encryptEntity(expense)
        expenseDao.updateExpense(entity)
    }

    suspend fun deleteExpenseById(id: Int) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun clearAllExpenses() {
        expenseDao.clearAllExpenses()
    }

    suspend fun getBudgetForMonth(monthKey: String): BudgetEntity? {
        return expenseDao.getBudgetForMonth(monthKey)
    }

    fun getBudgetForMonthFlow(monthKey: String): Flow<BudgetEntity?> {
        return expenseDao.getBudgetForMonthFlow(monthKey)
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        expenseDao.insertBudget(budget)
    }

    private fun encryptEntity(expense: Expense): ExpenseEntity {
        val amountEnc = CryptoHelper.encrypt(expense.amount.toString())
        val categoryEnc = CryptoHelper.encrypt(expense.category)
        val descriptionEnc = CryptoHelper.encrypt(expense.description)

        return ExpenseEntity(
            id = expense.id,
            encryptedAmount = amountEnc.encryptedBase64,
            ivAmount = amountEnc.ivBase64,
            encryptedCategory = categoryEnc.encryptedBase64,
            ivCategory = categoryEnc.ivBase64,
            encryptedDescription = descriptionEnc.encryptedBase64,
            ivDescription = descriptionEnc.ivBase64,
            timestamp = expense.timestamp
        )
    }

    private fun decryptEntity(entity: ExpenseEntity): Expense {
        val amountStr = CryptoHelper.decrypt(entity.encryptedAmount, entity.ivAmount)
        val category = CryptoHelper.decrypt(entity.encryptedCategory, entity.ivCategory)
        val description = CryptoHelper.decrypt(entity.encryptedDescription, entity.ivDescription)

        val amount = amountStr.toDoubleOrNull() ?: 0.0

        return Expense(
            id = entity.id,
            amount = amount,
            category = if (category.isEmpty()) "Others" else category,
            description = description,
            timestamp = entity.timestamp
        )
    }
}
