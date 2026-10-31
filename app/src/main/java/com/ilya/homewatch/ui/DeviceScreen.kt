package com.ilya.homewatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import com.ilya.homewatch.AppViewModel
import com.ilya.homewatch.UiState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val colorTempPresets = listOf(
    2700 to Color(0xFFFFB46B),
    3400 to Color(0xFFFFD9A6),
    4500 to Color(0xFFFFF2DE),
    5600 to Color(0xFFF2F6FF),
    6500 to Color(0xFFCFE4FF),
)

@Composable
private fun CountdownText(target: Long) {
    var now by remember(target) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(target) {
        while (now < target) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val totalSec = ((target - now) / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val text = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    Text(
        text,
        style = MaterialTheme.typography.title1,
        color = MaterialTheme.colors.primary,
    )
}

@Composable
fun DeviceScreen(vm: AppViewModel, deviceId: String, onSetTimer: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val device = (state as? UiState.Ready)?.home?.devices?.firstOrNull { it.id == deviceId }
    val timerTick by vm.timerTick.collectAsState()
    val timer = remember(timerTick, device?.id) { device?.let { vm.timerFor(it.id) } }
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        if (device == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Устройство не найдено")
            }
        } else {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 28.dp),
            ) {
                item { ListHeader { Text(device.name, textAlign = TextAlign.Center) } }

                if (device.onOff != null) {
                    item {
                        Button(
                            onClick = { vm.toggle(device.id, !device.isOn) },
                            colors = if (device.isOn) ButtonDefaults.primaryButtonColors()
                            else ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(58.dp),
                        ) {
                            Icon(
                                Icons.Rounded.PowerSettingsNew,
                                contentDescription = "Вкл/выкл",
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    item { SectionTitle("Таймер") }
                    if (timer != null) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    if (timer.second) "До включения" else "До выключения",
                                    style = MaterialTheme.typography.caption2,
                                    color = MaterialTheme.colors.onSurfaceVariant,
                                )
                                CountdownText(timer.first)
                            }
                        }
                        item {
                            CompactChip(
                                onClick = { vm.cancelTimer(device.id) },
                                label = { Text("Отменить") },
                                colors = ChipDefaults.secondaryChipColors(),
                            )
                        }
                    } else {
                        item {
                            Chip(
                                onClick = { onSetTimer(device.id) },
                                label = {
                                    Text(if (device.isOn) "Выключить по таймеру" else "Включить по таймеру")
                                },
                                icon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                                colors = ChipDefaults.secondaryChipColors(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                device.capabilities.filter { it.type.endsWith(".range") }.forEach { cap ->
                    item { SectionTitle(rangeLabel(cap.instance)) }
                    item {
                        val current = (cap.value as? Number)?.toFloat() ?: cap.min.toFloat()
                        val steps = (((cap.max - cap.min) / cap.precision).toInt() - 1).coerceIn(0, 20)
                        InlineSlider(
                            value = current,
                            onValueChange = { v -> vm.act(device.id, cap.type, cap.instance, v.roundToInt()) },
                            steps = steps,
                            valueRange = cap.min.toFloat()..cap.max.toFloat(),
                            decreaseIcon = { Icon(InlineSliderDefaults.Decrease, contentDescription = "Меньше") },
                            increaseIcon = { Icon(InlineSliderDefaults.Increase, contentDescription = "Больше") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Text(
                            rangeValueText(cap),
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurfaceVariant,
                        )
                    }
                }

                device.capabilities.filter { it.type.endsWith(".color_setting") }.forEach { cap ->
                    val presets = colorTempPresets.filter { it.first.toDouble() in cap.min..cap.max }
                    if (presets.isNotEmpty()) {
                        item { SectionTitle("Цвет") }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                presets.forEach { (kelvin, color) ->
                                    ColorDot(color) {
                                        vm.act(device.id, cap.type, "temperature_k", kelvin)
                                    }
                                }
                            }
                        }
                    }
                }

                device.capabilities.filter { it.type.endsWith(".toggle") }.forEach { cap ->
                    item {
                        ToggleChip(
                            checked = cap.value == true,
                            onCheckedChange = { vm.act(device.id, cap.type, cap.instance, it) },
                            label = { Text(toggleLabel(cap.instance)) },
                            toggleControl = { Switch(checked = cap.value == true) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                device.capabilities.filter { it.type.endsWith(".mode") }.forEach { cap ->
                    item { SectionTitle(modeInstanceLabel(cap.instance)) }
                    cap.modes.forEach { mode ->
                        item {
                            Chip(
                                onClick = { vm.act(device.id, cap.type, cap.instance, mode) },
                                label = { Text(modeLabel(mode)) },
                                colors = if (cap.value == mode) ChipDefaults.primaryChipColors()
                                else ChipDefaults.secondaryChipColors(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                if (device.properties.isNotEmpty()) {
                    item { SectionTitle("Датчики") }
                    device.properties.forEach { p ->
                        item {
                            Text(
                                propertyLine(p),
                                style = MaterialTheme.typography.body2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
    )
}
