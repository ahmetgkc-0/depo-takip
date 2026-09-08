package com.example

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stockCode: String,
    val stockName: String,
    val unit: String,
    val currentQuantity: Int,
    val brand: String,
    val barcode: String,
    val location: String,
    val stockLocation: String,
    val description: String,
    val lastUpdated: Long
)
