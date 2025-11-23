@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.portableevm.ui

import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.portableevm.arduino.ArduinoConnectionState
import com.example.portableevm.arduino.ArduinoManager

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Portable EVM",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun HomeScreen(navController: NavController, uiState: EvmUiState, viewModel: EvmViewModel) {
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun handleStartElection() {
        val admin = uiState.admin
        // Always require password when one is set.
        if (admin.isPasswordSet) {
            showPasswordDialog = true
        } else {
            navController.navigate("new_election")
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val admin = uiState.admin
                    if (admin.password == passwordInput) {
                        passwordError = null
                        showPasswordDialog = false
                        passwordInput = ""
                        navController.navigate("new_election")
                    } else {
                        passwordError = "Incorrect password"
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Admin Authentication") },
            text = {
                Column {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Admin password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = passwordError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Portable EVM",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Arduino-powered voting machine",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { handleStartElection() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start New Election") }

            Button(
                onClick = { navController.navigate("previous_elections") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("View Previous Elections") }

            Button(
                onClick = { navController.navigate("admin_settings") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Admin Settings") }

            Button(
                onClick = {
                    (context as? Activity)?.finish()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF1744),
                    contentColor = Color.White
                )
            ) { Text("Exit") }
        }
    }
}

@Composable
fun AdminSettingsScreen(navController: NavController, uiState: EvmUiState, viewModel: EvmViewModel) {
    val admin = uiState.admin
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var requirePassword by remember { mutableStateOf(admin.requirePasswordForNewElection) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = if (admin.isPasswordSet) "Change Admin Password" else "Set Admin Password", fontWeight = FontWeight.Bold)

            if (admin.isPasswordSet) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = requirePassword,
                    onCheckedChange = {
                        requirePassword = it
                        viewModel.updateAdminSettings(it)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text("Require password to start a new election")
            }

            Button(
                onClick = {
                    if (newPassword.isBlank()) {
                        statusMessage = "Password cannot be empty"
                    } else if (admin.isPasswordSet) {
                        viewModel.changeAdminPassword(currentPassword, newPassword, requirePassword) { ok ->
                            statusMessage = if (ok) "Password updated" else "Current password incorrect"
                        }
                    } else {
                        viewModel.setAdminPassword(newPassword, requirePassword)
                        statusMessage = "Password set"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            if (statusMessage != null) {
                Text(statusMessage!!)
            }
        }
    }
}

@Composable
fun NewElectionScreen(navController: NavController, uiState: EvmUiState, viewModel: EvmViewModel) {
    var electionName by remember { mutableStateOf("Current Election") }
    var candidateCount by remember { mutableStateOf(2) }
    var candidateNames by remember { mutableStateOf(List(4) { "" }) }
    var buttonAssignments by remember { mutableStateOf(List<Int?>(4) { null }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    fun Context.showError(msg: String) {
        errorMessage = msg
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Election") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = electionName,
                onValueChange = { electionName = it },
                label = { Text("Election name") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Number of candidates:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { number ->
                    FilterChip(
                        selected = candidateCount == number,
                        onClick = { candidateCount = number },
                        label = { Text(number.toString()) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            repeat(candidateCount) { index ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = candidateNames[index],
                        onValueChange = { name ->
                            candidateNames = candidateNames.toMutableList().also { it[index] = name }
                        },
                        label = { Text("Candidate ${index + 1} name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Assign Arduino button:")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..4).forEach { buttonNumber ->
                            FilterChip(
                                selected = buttonAssignments[index] == buttonNumber,
                                onClick = {
                                    buttonAssignments = buttonAssignments.toMutableList().also { it[index] = buttonNumber }
                                },
                                label = { Text("Button $buttonNumber") }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val names = candidateNames.take(candidateCount)
                    val buttons = buttonAssignments.take(candidateCount)
                    if (names.any { it.isBlank() }) {
                        errorMessage = "All candidates must have a name"
                        return@Button
                    }
                    if (buttons.any { it == null }) {
                        errorMessage = "All candidates must be assigned a button"
                        return@Button
                    }
                    val distinctButtons = buttons.filterNotNull().toSet()
                    if (distinctButtons.size != buttons.size) {
                        errorMessage = "Each candidate must have a unique button"
                        return@Button
                    }

                    val configs = names.zip(buttons.filterNotNull())
                    viewModel.startNewElection(electionName, configs) {
                        navController.navigate("voting") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Election")
            }
        }
    }
}

@Composable
fun VotingScreen(navController: NavController, uiState: EvmUiState, viewModel: EvmViewModel) {
    val context = LocalContext.current
    val active = uiState.activeElection

    if (active == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active election.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack("home", inclusive = false) }) {
                    Text("Back to Home")
                }
            }
        }
        return
    }

    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val arduinoManager = remember { ArduinoManager(context, usbManager) }
    var connectionState by remember { mutableStateOf<ArduinoConnectionState>(ArduinoConnectionState.Disconnected) }
    var lastMessage by remember { mutableStateOf("Waiting for vote...") }
    var hasVoted by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        arduinoManager.start()
        onDispose {
            arduinoManager.stop()
        }
    }

    LaunchedEffect(Unit) {
        try {
            arduinoManager.connectionState.collect { state ->
                connectionState = state
            }
        } catch (e: Exception) {
            // If anything goes wrong with Arduino connection, keep the screen alive
            connectionState = ArduinoConnectionState.Disconnected
            lastMessage = "Arduino connection error: ${e.message ?: "unknown"}"
        }
    }

    LaunchedEffect(active.id) {
        val assignedButtons = active.candidates.map { it.buttonNumber }.toSet()
        try {
            arduinoManager.buttonEvents.collect { buttonNumber ->
                if (!hasVoted) {
                    if (assignedButtons.contains(buttonNumber)) {
                        viewModel.registerVote(buttonNumber) {
                            lastMessage = "Your vote has been accepted ✔️"
                            hasVoted = true
                        }
                    } else {
                        // Ignore presses for buttons that are not assigned to any candidate.
                        lastMessage = "Button $buttonNumber is not assigned to any candidate."
                    }
                }
            }
        } catch (e: Exception) {
            lastMessage = "Error reading button presses: ${e.message ?: "unknown"}"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Voting") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Small status and candidate mapping at the top
                Text("Election: ${active.name}", fontWeight = FontWeight.Bold)

                Text(
                    text = when (connectionState) {
                        is ArduinoConnectionState.Connected -> "Arduino connected"
                        ArduinoConnectionState.Connecting -> "Connecting to Arduino..."
                        ArduinoConnectionState.Disconnected -> "Arduino not connected"
                    },
                    style = MaterialTheme.typography.labelMedium
                )

                Text("Candidates:", fontWeight = FontWeight.Bold)
                active.candidates.forEach { candidate ->
                    // During voting, only show mapping from button to candidate, not live vote counts.
                    Text("Button ${candidate.buttonNumber}: ${candidate.name}")
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            hasVoted = false
                            lastMessage = "Waiting for vote..."
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Next Voter") }

                    Button(
                        onClick = {
                            viewModel.endActiveElection {
                                navController.navigate("results") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF1744),
                            contentColor = Color.White
                        )
                    ) { Text("End Election") }
                }
            }

            if (hasVoted) {
                // Big centered confirmation overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF42A5F5)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Your vote has been accepted ✅",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Please pass the device to the next voter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    hasVoted = false
                                    lastMessage = "Waiting for vote..."
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E88E5),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Next Voter")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsScreen(navController: NavController, uiState: EvmUiState) {
    val election = uiState.previousElections.firstOrNull() ?: uiState.activeElection

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Results") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack("home", inclusive = false) }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (election == null) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No election results available.")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Election: ${election.name}", fontWeight = FontWeight.Bold)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(election.candidates) { candidate ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(candidate.name, fontWeight = FontWeight.Bold)
                                Text("Button ${candidate.buttonNumber}")
                                Text("Votes: ${candidate.votes}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviousElectionsScreen(navController: NavController, uiState: EvmUiState) {
    val elections = uiState.previousElections

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Previous Elections") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (elections.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No previous elections found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(elections) { election ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(election.name, fontWeight = FontWeight.Bold)
                            Text("Candidates: ${election.candidates.size}")
                            Text("Completed: ${if (election.isCompleted) "Yes" else "No"}")
                        }
                    }
                }
            }
        }
    }
}
