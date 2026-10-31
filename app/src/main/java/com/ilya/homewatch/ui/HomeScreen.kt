package com.ilya.homewatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SplitToggleChip
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.ilya.homewatch.AppViewModel
import com.ilya.homewatch.UiState
import com.ilya.homewatch.data.Device

@Composable
fun HomeScreen(vm: AppViewModel, onOpenDevice: (String) -> Unit, onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsState()
    val home = (state as? UiState.Ready)?.home ?: return
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 28.dp),
        ) {
            item { ListHeader { Text("Мой дом") } }

            val byRoom = home.devices.groupBy { it.roomId }
            home.rooms.forEach { room ->
                val roomDevices = byRoom[room.id].orEmpty()
                if (roomDevices.isNotEmpty()) {
                    item { SectionTitle(room.name) }
                    items(roomDevices.size, key = { roomDevices[it].id }) { i ->
                        DeviceChip(roomDevices[i], vm, onOpenDevice)
                    }
                }
            }

            val other = home.devices.filter { device ->
                device.roomId == null || home.rooms.none { it.id == device.roomId }
            }
            if (other.isNotEmpty()) {
                item { SectionTitle("Без комнаты") }
                items(other.size, key = { other[it].id }) { i ->
                    DeviceChip(other[i], vm, onOpenDevice)
                }
            }

            if (home.scenarios.isNotEmpty()) {
                item { SectionTitle("Сценарии") }
                items(home.scenarios.size, key = { home.scenarios[it].id }) { i ->
                    val scenario = home.scenarios[i]
                    Chip(
                        onClick = { vm.runScenario(scenario.id) },
                        label = { Text(scenario.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        icon = {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary,
                            )
                        },
                        colors = ChipDefaults.gradientBackgroundChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    Button(onClick = vm::refresh, colors = ButtonDefaults.secondaryButtonColors()) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Обновить")
                    }
                    Button(onClick = onOpenSettings, colors = ButtonDefaults.secondaryButtonColors()) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Настройки")
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.caption2,
        color = MaterialTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun DeviceChip(device: Device, vm: AppViewModel, onOpen: (String) -> Unit) {
    val status = device.statusLine()
    val secondary: (@Composable RowScope.() -> Unit)? =
        if (status.isEmpty()) null
        else {
            { Text(status, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }

    if (device.onOff != null) {
        SplitToggleChip(
            checked = device.isOn,
            onCheckedChange = { vm.toggle(device.id, it) },
            onClick = { onOpen(device.id) },
            label = {
                Icon(
                    deviceIcon(device.type),
                    contentDescription = null,
                    tint = if (device.isOn) MaterialTheme.colors.primary
                    else MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = secondary,
            toggleControl = { Switch(checked = device.isOn) },
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Chip(
            onClick = { onOpen(device.id) },
            label = { Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            secondaryLabel = secondary,
            icon = { Icon(deviceIcon(device.type), contentDescription = null) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
