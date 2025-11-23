package com.example.portableevm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.portableevm.ui.AdminSettingsScreen
import com.example.portableevm.ui.EvmViewModel
import com.example.portableevm.ui.HomeScreen
import com.example.portableevm.ui.NewElectionScreen
import com.example.portableevm.ui.PreviousElectionsScreen
import com.example.portableevm.ui.ResultsScreen
import com.example.portableevm.ui.SplashScreen
import com.example.portableevm.ui.VotingScreen
import com.example.portableevm.ui.theme.PortableEvmTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PortableEvmAppRoot()
        }
    }
}

@Composable
fun PortableEvmAppRoot() {
    PortableEvmTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            var showSplash by remember { mutableStateOf(true) }
            val viewModel: EvmViewModel = viewModel(factory = EvmViewModel.Factory)
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

            LaunchedEffect(Unit) {
                delay(1500)
                showSplash = false
            }

            if (showSplash) {
                SplashScreen()
            } else {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController, uiState, viewModel) }
                    composable("admin_settings") { AdminSettingsScreen(navController, uiState, viewModel) }
                    composable("new_election") { NewElectionScreen(navController, uiState, viewModel) }
                    composable("voting") { VotingScreen(navController, uiState, viewModel) }
                    composable("results") { ResultsScreen(navController, uiState) }
                    composable("previous_elections") { PreviousElectionsScreen(navController, uiState) }
                }
            }
        }
    }
}
