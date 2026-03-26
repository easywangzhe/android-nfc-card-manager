package com.opencode.nfccardmanager.feature.auth

import androidx.lifecycle.ViewModel
import com.opencode.nfccardmanager.core.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val username: String = "admin",
    val password: String = "123456",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val result = SecurityManager.login(_uiState.value.username, _uiState.value.password)
        result.onSuccess {
            _uiState.update { state -> state.copy(isLoading = false, errorMessage = null) }
            onSuccess()
        }.onFailure {
            _uiState.update { state ->
                state.copy(isLoading = false, errorMessage = it.message ?: "登录失败")
            }
        }
    }
}
