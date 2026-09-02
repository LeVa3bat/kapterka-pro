package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val callsign: String = "лева",
    val unitName: String = "1-е Подразделение",
    val unitKey: String = "kapt_59e13b",
    val email: String = "alex.666.881@gmail.com",
    val isLoggedIn: Boolean = true,
    val isProActive: Boolean = false,
    val demoDaysLeft: Int = 2,
    val proDaysLeft: Int = 29,
    val isOnline: Boolean = true,
    val onlineCount: Int = 1
)
