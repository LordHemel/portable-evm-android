package com.example.portableevm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.portableevm.PortableEvmApp
import com.example.portableevm.data.AdminRepository
import com.example.portableevm.data.AdminSettingsEntity
import com.example.portableevm.data.ElectionRepository
import com.example.portableevm.data.ElectionWithCandidates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class UiCandidate(
    val id: Long,
    val name: String,
    val buttonNumber: Int,
    val votes: Int,
)

data class UiElection(
    val id: Long,
    val name: String,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val isCompleted: Boolean,
    val candidates: List<UiCandidate>,
)

data class AdminUiState(
    val password: String? = null,
    val requirePasswordForNewElection: Boolean = true,
) {
    val isPasswordSet: Boolean get() = !password.isNullOrEmpty()
}

data class EvmUiState(
    val activeElection: UiElection? = null,
    val previousElections: List<UiElection> = emptyList(),
    val admin: AdminUiState = AdminUiState(),
)

class EvmViewModel(
    private val electionRepository: ElectionRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EvmUiState())
    val uiState: StateFlow<EvmUiState> = _uiState.asStateFlow()

    init {
        // Ensure there is always a default admin password on first launch.
        viewModelScope.launch {
            adminRepository.ensureDefaultSettingsIfMissing()
        }

        viewModelScope.launch {
            combine(
                electionRepository.observeActiveElection(),
                electionRepository.observeElections(),
                adminRepository.observeSettings(),
            ) { active, all, adminSettings ->
                val activeElection = active?.toUi()
                val allUi = all.map { it.toUi() }
                val adminUi = adminSettings.toUi()
                EvmUiState(
                    activeElection = activeElection,
                    previousElections = allUi,
                    admin = adminUi,
                )
            }.collectLatest { _uiState.value = it }
        }
    }

    fun setAdminPassword(newPassword: String, requireForNewElection: Boolean) {
        viewModelScope.launch {
            adminRepository.setPassword(newPassword, requireForNewElection)
        }
    }

    fun changeAdminPassword(currentPassword: String, newPassword: String, requireForNewElection: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val current = adminRepository.observeSettings().collectLatest { settings ->
                val ok = settings?.password == currentPassword
                if (ok) {
                    adminRepository.setPassword(newPassword, requireForNewElection)
                }
                onResult(ok)
            }
        }
    }

    fun updateAdminSettings(requireForNewElection: Boolean) {
        viewModelScope.launch {
            val settingsFlow = adminRepository.observeSettings()
            settingsFlow.collectLatest { settings ->
                val currentPassword = settings?.password
                adminRepository.setPassword(currentPassword, requireForNewElection)
            }
        }
    }

    fun startNewElection(electionName: String, candidates: List<Pair<String, Int>>, onStarted: () -> Unit) {
        viewModelScope.launch {
            electionRepository.startNewElection(electionName, candidates)
            onStarted()
        }
    }

    fun endActiveElection(onEnded: () -> Unit) {
        viewModelScope.launch {
            val current = _uiState.value.activeElection
            if (current != null) {
                electionRepository.endElection(current.id)
            }
            onEnded()
        }
    }

    fun registerVote(buttonNumber: Int, onAccepted: () -> Unit) {
        viewModelScope.launch {
            val current = _uiState.value.activeElection
            if (current != null) {
                // Update DB
                electionRepository.registerVote(current.id, buttonNumber)

                // Optimistically update UI counts so results screen reflects votes immediately.
                val updatedCandidates = current.candidates.map {
                    if (it.buttonNumber == buttonNumber) it.copy(votes = it.votes + 1) else it
                }
                val updatedActive = current.copy(candidates = updatedCandidates)

                _uiState.value = _uiState.value.copy(
                    activeElection = updatedActive,
                    previousElections = _uiState.value.previousElections.map { election ->
                        if (election.id == current.id) election.copy(candidates = updatedCandidates) else election
                    }
                )

                onAccepted()
            }
        }
    }

    private fun ElectionWithCandidates.toUi(): UiElection = UiElection(
        id = election.id,
        name = election.name,
        startTimestamp = election.startTimestamp,
        endTimestamp = election.endTimestamp,
        isCompleted = election.isCompleted,
        candidates = candidates.map {
            UiCandidate(
                id = it.id,
                name = it.name,
                buttonNumber = it.buttonNumber,
                votes = it.votes,
            )
        },
    )

    private fun AdminSettingsEntity?.toUi(): AdminUiState = this?.let {
        AdminUiState(
            password = it.password,
            requirePasswordForNewElection = it.requirePasswordForNewElection,
        )
    } ?: AdminUiState()

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = PortableEvmApp.instance
                val container = app.container
                @Suppress("UNCHECKED_CAST")
                return EvmViewModel(
                    electionRepository = container.electionRepository,
                    adminRepository = container.adminRepository,
                ) as T
            }
        }
    }
}
