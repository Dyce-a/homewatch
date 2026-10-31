package com.ilya.homewatch.data

import android.content.Context
import com.ilya.homewatch.Config
import kotlinx.coroutines.delay

data class DeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val interval: Long,
    val expiresIn: Long,
)

/**
 * Вход в Яндекс по схеме OAuth «код на устройстве»:
 * часы показывают QR и короткий код, пользователь подтверждает на телефоне,
 * часы в это время опрашивают сервер и получают токен.
 */
class AuthRepository(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    val accessToken: String?
        get() = prefs.getString("access_token", null)

    suspend fun requestDeviceCode(): DeviceCode {
        val json = Net.postForm(
            "https://oauth.yandex.ru/device/code",
            mapOf(
                "client_id" to Config.CLIENT_ID,
                "device_name" to "Galaxy Watch",
            ),
        )
        if (!json.has("device_code")) {
            val message = json.optString("error_description", json.optString("error"))
            throw IllegalStateException(if (message.isBlank()) "Не удалось получить код входа" else message)
        }
        return DeviceCode(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUrl = json.optString("verification_url").ifBlank { "https://ya.ru/device" },
            interval = json.optLong("interval", 5),
            expiresIn = json.optLong("expires_in", 300),
        )
    }

    /** Ждёт подтверждения кода. true — токен получен, false — код истёк. */
    suspend fun waitForToken(code: DeviceCode): Boolean {
        val deadline = System.currentTimeMillis() + code.expiresIn * 1000
        while (System.currentTimeMillis() < deadline) {
            delay(code.interval * 1000)
            val json = Net.postForm(
                "https://oauth.yandex.ru/token",
                mapOf(
                    "grant_type" to "device_code",
                    "code" to code.deviceCode,
                    "client_id" to Config.CLIENT_ID,
                    "client_secret" to Config.CLIENT_SECRET,
                ),
            )
            when {
                json.has("access_token") -> {
                    prefs.edit()
                        .putString("access_token", json.getString("access_token"))
                        .putString("refresh_token", json.optString("refresh_token"))
                        .apply()
                    return true
                }
                json.optString("error") == "authorization_pending" -> continue
                json.optString("error").isBlank() -> continue
                else -> {
                    val message = json.optString("error_description", json.getString("error"))
                    throw IllegalStateException(message)
                }
            }
        }
        return false
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
