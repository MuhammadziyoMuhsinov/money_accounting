package com.countingfees.andckaps.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class User(
    @PrimaryKey val id: Int = 1, // Single user
    var username: String,
    var photoUrl: String?,       // optional image path or URI
    var currency: String,
    var dateFormat: String
)
