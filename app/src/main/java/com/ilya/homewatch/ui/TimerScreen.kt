package com.ilya.homewatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import com.ilya.homewatch.AppViewModel
import com.ilya.homewatch.UiState

@Composable
fun TimerScreen(vm: AppViewModel, deviceId: String, onDone: () -> Unit) {
    val state by vm.state.collectAsState()
    val device = (state as? UiState.Ready)?.home?.devices?.firstOrNull { it.id == deviceId }
    val hours = rememberPickerState(
        initialNumberOfOptions = 13,
        initiallySelectedOption = 0,
        repeatItems = false,
    )
    val minutes = rememberPickerState(
        initialNumberOfOptions = 60,
        initiallySelectedOption = 5,
        repeatItems = true,
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (device?.isOn == true) "Выключить через" else "Включить через",
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Picker(
                state = hours,
                contentDescription = "Часы",
                modifier = Modifier.size(46.dp, 100.dp),
            ) { index ->
                Text("$index", style = MaterialTheme.typography.display3)
            }
            Text(
                "ч",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
            )
            Picker(
                state = minutes,
                contentDescription = "Минуты",
                modifier = Modifier.size(46.dp, 100.dp),
            ) { index ->
                Text("%02d".format(index), style = MaterialTheme.typography.display3)
            }
            Text(
                "мин",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val total = hours.selectedOption * 60 + minutes.selectedOption
                if (total > 0 && device != null) {
                    vm.scheduleToggle(device.id, !device.isOn, total)
                    onDone()
                }
            },
            colors = ButtonDefaults.primaryButtonColors(),
            modifier = Modifier.size(46.dp),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = "Установить")
        }
    }
}
