package com.opencode.nfccardmanager.feature.read

import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ReadResultStore {
    private val _latestResult = MutableStateFlow<ReadCardResult?>(null)
    val latestResult: StateFlow<ReadCardResult?> = _latestResult.asStateFlow()

    fun save(result: ReadCardResult) {
        _latestResult.value = result
    }

    fun clear() {
        _latestResult.value = null
    }
}
