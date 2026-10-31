package com.ilya.homewatch.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ilya.homewatch.Config
import com.ilya.homewatch.UiState

@Composable
fun AuthScreen(state: UiState.NeedAuth, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            Config.CLIENT_ID.isBlank() -> {
                Text(
                    "Сначала впиши CLIENT_ID и CLIENT_SECRET в Config.kt — см. README",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                )
            }
            state.error != null -> {
                Text(
                    state.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                )
                Spacer(Modifier.height(8.dp))
                Chip(
                    onClick = onRetry,
                    label = { Text("Повторить") },
                    colors = ChipDefaults.primaryChipColors(),
                )
            }
            state.code == null -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Получаю код входа…", style = MaterialTheme.typography.caption1)
            }
            else -> {
                val bitmap = remember(state.code.userCode) {
                    qrBitmap(state.code.verificationUrl, 512)
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR для входа в Яндекс",
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    state.code.userCode,
                    style = MaterialTheme.typography.title2,
                    color = MaterialTheme.colors.primary,
                )
                Text(
                    "Отсканируй QR телефоном (откроется ya.ru/device) и введи код",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}
