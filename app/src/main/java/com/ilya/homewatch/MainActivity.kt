package com.ilya.homewatch

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.ilya.homewatch.ui.AuthScreen
import com.ilya.homewatch.ui.DeviceScreen
import com.ilya.homewatch.ui.HomeScreen
import com.ilya.homewatch.ui.HomeWatchTheme
import com.ilya.homewatch.ui.SettingsScreen
import com.ilya.homewatch.ui.TimerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val keepScreenOn by vm.keepScreenOn.collectAsState()
            val state by vm.state.collectAsState()
            val keepOn = keepScreenOn || state is UiState.NeedAuth
            LaunchedEffect(keepOn) {
                if (keepOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            HomeWatchTheme {
                Root(vm)
            }
        }
    }
}

@Composable
private fun Root(vm: AppViewModel) {
    val nav = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navController = nav, startDestination = "main") {
        // Корневой экран: свайп назад отсюда выходит из приложения в список приложений.
        composable("main") {
            val state by vm.state.collectAsState()
            when (val s = state) {
                is UiState.Loading -> LoadingScreen()
                is UiState.NeedAuth -> AuthScreen(s, onRetry = vm::start)
                is UiState.Error -> ErrorScreen(s.message, onRetry = vm::refresh)
                is UiState.Ready -> HomeScreen(
                    vm,
                    onOpenDevice = { nav.navigate("device/$it") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
        }
        composable("device/{id}") { entry ->
            DeviceScreen(
                vm,
                entry.arguments?.getString("id").orEmpty(),
                onSetTimer = { nav.navigate("timer/$it") },
            )
        }
        composable("timer/{id}") { entry ->
            TimerScreen(
                vm,
                entry.arguments?.getString("id").orEmpty(),
                onDone = { nav.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(vm)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.body2)
        Spacer(Modifier.height(8.dp))
        Chip(
            onClick = onRetry,
            label = { Text("Повторить") },
            colors = ChipDefaults.primaryChipColors(),
        )
    }
}
