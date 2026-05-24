package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val country: String,
    val birthDate: String, // format "YYYY-MM-DD"
    val lifeExpectancy: Double
)
