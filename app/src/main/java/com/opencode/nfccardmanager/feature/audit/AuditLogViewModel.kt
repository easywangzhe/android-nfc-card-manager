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

enum class AuditResultFilter {
    ALL,
    SUCCESS,
    FAILED,
}

data class AuditLogUiState(
    val logs: List<AuditLogRecord> = emptyList(),
    val filteredLogs: List<AuditLogListItemPresentation> = emptyList(),
    val isLoading: Boolean = false,
    val operationFilter: String = "ALL",
    val resultFilter: AuditResultFilter = AuditResultFilter.ALL,
    val keyword: String = "",
    val pageSummary: com.opencode.nfccardmanager.feature.support.SupportPageSummary = buildAuditOverviewSummary(),
)

class AuditLogViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuditLogUiState())
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    fun loadLogs() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val logs = AuditLogManager.list()
            updateLogsForTest(logs, isLoading = false)
        }
    }

    internal fun updateLogsForTest(logs: List<AuditLogRecord>, isLoading: Boolean = false) {
        _uiState.update {
            val next = it.copy(logs = logs, isLoading = isLoading)
            next.copy(filteredLogs = applyFilters(next))
        }
    }

    fun onOperationFilterChange(value: String) {
        _uiState.update {
            val next = it.copy(operationFilter = value)
            next.copy(filteredLogs = applyFilters(next))
        }
    }

    fun onResultFilterChange(value: AuditResultFilter) {
        _uiState.update {
            val next = it.copy(resultFilter = value)
            next.copy(filteredLogs = applyFilters(next))
        }
    }

    fun onKeywordChange(value: String) {
        _uiState.update {
            val next = it.copy(keyword = value)
            next.copy(filteredLogs = applyFilters(next))
        }
    }

    fun resetFilters() {
        _uiState.update {
            val next = it.copy(
                operationFilter = "ALL",
                resultFilter = AuditResultFilter.ALL,
                keyword = "",
            )
            next.copy(filteredLogs = applyFilters(next))
        }
    }

    private fun applyFilters(state: AuditLogUiState): List<AuditLogListItemPresentation> {
        return state.logs.filter { log ->
            val matchesOperation = state.operationFilter == "ALL" || log.operationType == state.operationFilter
            val matchesResult = when (state.resultFilter) {
                AuditResultFilter.ALL -> true
                AuditResultFilter.SUCCESS -> log.result == "SUCCESS"
                AuditResultFilter.FAILED -> log.result == "FAILED"
            }
            val keyword = state.keyword.trim()
            val matchesKeyword = keyword.isBlank() || listOf(
                log.operationType,
                log.operatorId,
                log.operatorRole.label,
                log.cardUidMasked,
                log.cardType,
                log.result,
                log.flowStage.label,
                log.authenticity.label,
                log.impactScope.label,
                log.message,
            ).any { it.contains(keyword, ignoreCase = true) }

            matchesOperation && matchesResult && matchesKeyword
        }.map(::buildAuditLogListItem)
    }
}
