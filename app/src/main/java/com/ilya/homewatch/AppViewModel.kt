package com.ilya.homewatch

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilya.homewatch.data.ApiException
import com.ilya.homewatch.data.AuthRepository
import com.ilya.homewatch.data.DeviceCode
import com.ilya.homewatch.data.Home
import com.ilya.homewatch.data.IotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data class NeedAuth(val code: DeviceCode?, val error: String? = null) : UiState
    data class Ready(val home: Home) : UiState
    data class Error(val message: String) : UiState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = AuthRepository(app)
    private val iot = IotRepository()
    private val settings = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state

    private val _keepScreenOn = MutableStateFlow(settings.getBoolean("keep_screen_on", false))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn

    private val timers = app.getSharedPreferences("timers", Context.MODE_PRIVATE)
    private val _timerTick = MutableStateFlow(0)
    val timerTick: StateFlow<Int> = _timerTick

    private var authJob: Job? = null

    init {
        start()
    }

    fun start() {
        if (auth.accessToken == null) beginAuth() else refresh()
    }

    private fun beginAuth() {
        authJob?.cancel()
        _state.value = UiState.NeedAuth(null)
        authJob = viewModelScope.launch {
            try {
                val code = auth.requestDeviceCode()
                _state.value = UiState.NeedAuth(code)
                if (auth.waitForToken(code)) refresh() else beginAuth()
            } catch (e: Exception) {
                _state.value = UiState.NeedAuth(null, e.message ?: "Ошибка авторизации")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val token = auth.accessToken ?: return@launch beginAuth()
            try {
                _state.value = UiState.Ready(iot.fetchHome(token))
            } catch (e: ApiException) {
                if (e.code == 401 || e.code == 403) {
                    auth.logout()
                    beginAuth()
                } else {
                    _state.value = UiState.Error("Ошибка сети (${e.code})")
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Нет соединения")
            }
        }
    }

    fun toggle(deviceId: String, on: Boolean) =
        act(deviceId, "devices.capabilities.on_off", "on", on)

    fun act(deviceId: String, type: String, instance: String, value: Any) {
        val token = auth.accessToken ?: return
        applyLocal(deviceId, type, instance, value)
        viewModelScope.launch {
            try {
                iot.sendAction(token, deviceId, type, instance, value)
            } catch (_: Exception) {
                refresh()
            }
        }
    }

    private fun applyLocal(deviceId: String, type: String, instance: String, value: Any) {
        val ready = _state.value as? UiState.Ready ?: return
        val devices = ready.home.devices.map { device ->
            if (device.id != deviceId) device
            else device.copy(capabilities = device.capabilities.map { cap ->
                when {
                    cap.type == type && cap.instance == instance -> cap.copy(value = value)
                    cap.type == type && type.endsWith(".color_setting") ->
                        cap.copy(instance = instance, value = value)
                    else -> cap
                }
            })
        }
        _state.value = UiState.Ready(ready.home.copy(devices = devices))
    }

    fun runScenario(id: String) {
        val token = auth.accessToken ?: return
        viewModelScope.launch {
            try {
                iot.runScenario(token, id)
            } catch (_: Exception) {
            }
        }
    }

    fun setKeepScreenOn(value: Boolean) {
        _keepScreenOn.value = value
        settings.edit().putBoolean("keep_screen_on", value).apply()
    }

    fun scheduleToggle(deviceId: String, turnOn: Boolean, minutes: Int) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        DelayedActions.schedule(getApplication<Application>(), deviceId, turnOn, at)
        timers.edit().putString(deviceId, "$at|$turnOn").apply()
        _timerTick.value++
    }

    fun cancelTimer(deviceId: String) {
        DelayedActions.cancel(getApplication<Application>(), deviceId)
        timers.edit().remove(deviceId).apply()
        _timerTick.value++
    }

    /** Активный таймер устройства: (момент срабатывания, включить?) или null. */
    fun timerFor(deviceId: String): Pair<Long, Boolean>? {
        val raw = timers.getString(deviceId, null) ?: return null
        val at = raw.substringBefore("|").toLongOrNull() ?: return null
        if (at <= System.currentTimeMillis()) {
            timers.edit().remove(deviceId).apply()
            return null
        }
        return at to raw.substringAfter("|").toBoolean()
    }

    fun logout() {
        auth.logout()
        beginAuth()
    }
}
