package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val encryptedAmount: String,
    val ivAmount: String,
    val encryptedCategory: String,
    val ivCategory: String,
    val encryptedDescription: String,
    val ivDescription: String,
    val timestamp: Long
)
