package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val monthKey: String, // format "YYYY-MM" e.g., "2026-06"
    val budgetAmount: Double
)
