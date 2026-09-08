package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_transactions")
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true) val transactionId: Int = 0,
    val barcode: String,
    val transactionType: String, // "IN" or "OUT"
    val quantity: Int,
    val timestamp: Long
)
