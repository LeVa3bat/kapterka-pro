package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val callsign: String = "",
    val unitName: String = "",
    val unitKey: String = "",
    val email: String = "",
    val isLoggedIn: Boolean = false,
    val isProActive: Boolean = false,
    val demoDaysLeft: Int = 3,
    val proDaysLeft: Int = 30,
    val isOnline: Boolean = true,
    val onlineCount: Int = 1
)
