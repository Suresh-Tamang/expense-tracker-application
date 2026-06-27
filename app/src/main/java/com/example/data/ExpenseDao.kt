package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesFlow(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getBudgetForMonth(monthKey: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey LIMIT 1")
    fun getBudgetForMonthFlow(monthKey: String): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)
}
