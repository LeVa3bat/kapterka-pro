package com.example.data.notification

import android.util.Log

/**
 * Future Android builds do not talk to a public Telegram relay.
 *
 * Payment/license/email notifications are emitted by the trusted backend,
 * where the bot token and message templates live. These compatibility methods
 * remain so existing ViewModel call sites do not need risky wide refactors.
 */
object TelegramNotifier {
    private const val TAG = "TelegramNotifier"

    suspend fun sendMessage(textHtml: String) {
        Log.d(TAG, "Client Telegram relay disabled; backend owns notifications")
    }

    suspend fun notifyRegistration(
        callsign: String,
        unitName: String,
        unitKey: String,
        email: String
    ) {
        Log.d(TAG, "Registration recorded in registry; client Telegram notification disabled")
    }

    suspend fun notifyPaymentStarted(
        callsign: String,
        email: String,
        amountRub: Int
    ) {
        Log.d(TAG, "Payment-start notification is emitted by backend")
    }

    suspend fun notifyPaymentConfirmed(
        callsign: String,
        email: String,
        licenseKey: String,
        days: Int
    ) {
        Log.d(TAG, "Payment-confirmed notification is emitted by backend")
    }

    suspend fun notifyKeyActivated(
        callsign: String,
        licenseKey: String,
        days: Int
    ) {
        Log.d(TAG, "Key activation notification stays local; no public relay call")
    }

    suspend fun notifyLicenseEmailDispatched(
        callsign: String,
        email: String,
        licenseKey: String,
        subject: String
    ) {
        Log.d(TAG, "License-email notification is emitted by backend")
    }
}
