package com.ilya.homewatch.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Blinds
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.ilya.homewatch.data.Capability
import com.ilya.homewatch.data.Device
import com.ilya.homewatch.data.Property

fun fmt(v: Any?): String = when (v) {
    null -> "—"
    is Number -> if (v.toDouble() % 1.0 == 0.0) v.toLong().toString() else v.toString()
    else -> v.toString()
}

fun Device.statusLine(): String {
    val parts = mutableListOf<String>()
    properties.forEach { p ->
        when (p.instance) {
            "temperature" -> parts += "${fmt(p.value)}°"
            "humidity" -> parts += "${fmt(p.value)}%"
        }
    }
    if (parts.isEmpty() && onOff != null) {
        parts += if (isOn) "Включено" else "Выключено"
    }
    return parts.joinToString(" · ")
}

fun deviceIcon(type: String): ImageVector = when {
    "light" in type -> Icons.Rounded.Lightbulb
    "socket" in type -> Icons.Rounded.Power
    "switch" in type -> Icons.Rounded.ToggleOn
    "thermostat.ac" in type -> Icons.Rounded.AcUnit
    "thermostat" in type -> Icons.Rounded.Thermostat
    "media" in type || "tv" in type -> Icons.Rounded.Tv
    "vacuum" in type -> Icons.Rounded.CleaningServices
    "humidifier" in type -> Icons.Rounded.WaterDrop
    "purifier" in type || "fan" in type -> Icons.Rounded.Air
    "kettle" in type || "cooking" in type -> Icons.Rounded.Coffee
    "washing" in type || "dishwasher" in type -> Icons.Rounded.LocalLaundryService
    "sensor" in type -> Icons.Rounded.Sensors
    "camera" in type -> Icons.Rounded.Videocam
    "curtain" in type || "openable" in type -> Icons.Rounded.Blinds
    else -> Icons.Rounded.Home
}

fun rangeLabel(instance: String): String = when (instance) {
    "brightness" -> "Яркость"
    "temperature" -> "Температура"
    "volume" -> "Громкость"
    "humidity" -> "Влажность"
    "open" -> "Открытие"
    "channel" -> "Канал"
    else -> instance
}

fun rangeValueText(cap: Capability): String {
    val v = fmt(cap.value)
    return when (cap.instance) {
        "brightness", "humidity", "open" -> "$v %"
        "temperature" -> "$v°"
        else -> v
    }
}

fun toggleLabel(instance: String): String = when (instance) {
    "mute" -> "Без звука"
    "backlight" -> "Подсветка"
    "ionization" -> "Ионизация"
    "oscillation" -> "Вращение"
    "keep_warm" -> "Подогрев"
    "pause" -> "Пауза"
    "controls_locked" -> "Блокировка"
    else -> instance
}

fun modeInstanceLabel(instance: String): String = when (instance) {
    "thermostat" -> "Режим"
    "fan_speed" -> "Скорость вентилятора"
    "work_speed" -> "Скорость"
    "cleanup_mode" -> "Режим уборки"
    "program" -> "Программа"
    "input_source" -> "Источник"
    "swing" -> "Наклон"
    "heat" -> "Нагрев"
    else -> instance
}

fun modeLabel(mode: String): String = when (mode) {
    "auto" -> "Авто"
    "eco" -> "Эко"
    "turbo" -> "Турбо"
    "cool" -> "Охлаждение"
    "heat" -> "Обогрев"
    "fan_only" -> "Вентиляция"
    "dry" -> "Осушение"
    "quiet" -> "Тихий"
    "normal" -> "Обычный"
    "low" -> "Низкая"
    "medium" -> "Средняя"
    "high" -> "Высокая"
    "max" -> "Макс."
    "min" -> "Мин."
    "express" -> "Экспресс"
    else -> mode.replaceFirstChar { it.uppercase() }
}

fun propertyLine(p: Property): String {
    val name = when (p.instance) {
        "temperature" -> "Температура"
        "humidity" -> "Влажность"
        "battery_level" -> "Батарея"
        "co2_level" -> "CO₂"
        "illumination" -> "Освещённость"
        "power" -> "Мощность"
        "voltage" -> "Напряжение"
        "amperage" -> "Ток"
        "water_level" -> "Уровень воды"
        "pm2.5_density" -> "PM2.5"
        else -> p.instance
    }
    val unit = when {
        p.unit.endsWith("celsius") -> " °C"
        p.unit.endsWith("percent") -> " %"
        p.unit.endsWith("ppm") -> " ppm"
        p.unit.endsWith("watt") -> " Вт"
        p.unit.endsWith("volt") -> " В"
        p.unit.endsWith("ampere") -> " А"
        p.unit.endsWith("lux") -> " лк"
        else -> ""
    }
    return "$name: ${fmt(p.value)}$unit"
}
