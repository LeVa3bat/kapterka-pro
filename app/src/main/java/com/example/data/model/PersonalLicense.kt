package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_license")
data class PersonalLicense(
    @PrimaryKey val licenseKey: String,
    val fighterCallsign: String,
    val fighterEmail: String,
    val deviceId: String,
    val activatedAt: Long,
    val expiresAt: Long,
    val durationDays: Int = 30,
    val isProActive: Boolean = true,
    val paymentProvider: String = "ЮKassa (СБП/Карта)",
    val paymentId: String = ""
)
