package com.opencode.nfccardmanager.feature.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.database.AuditLogRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuditLogDetailUiState(
    val log: AuditLogRecord? = null,
    val presentation: AuditLogDetailPresentation? = null,
    val isLoading: Boolean = false,
)

class AuditLogDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuditLogDetailUiState())
    val uiState: StateFlow<AuditLogDetailUiState> = _uiState.asStateFlow()

    fun load(logId: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val log = AuditLogManager.findById(logId)
            _uiState.update {
                it.copy(
                    log = log,
                    presentation = log?.let(::buildAuditLogDetailPresentation),
                    isLoading = false,
                )
            }
        }
    }
}
