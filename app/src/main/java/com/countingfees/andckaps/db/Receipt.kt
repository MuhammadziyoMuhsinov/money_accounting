package com.countingfees.andckaps.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var date: String,
    var store: String,
    var category: String,
    var amount: Double,
    var tax: Double?,
    var imagePath: String?
)
