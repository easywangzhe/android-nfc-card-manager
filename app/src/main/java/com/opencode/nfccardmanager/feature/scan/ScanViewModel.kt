package com.opencode.nfccardmanager.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.nfc.CardCapabilityResolver
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.feature.read.ReadResultStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScanViewModel(
    private val capabilityResolver: CardCapabilityResolver = CardCapabilityResolver(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ScanUiEffect>()
    val uiEffect: SharedFlow<ScanUiEffect> = _uiEffect.asSharedFlow()

    fun init(mode: ScanMode) {
        _uiState.update {
            it.copy(mode = mode)
        }
    }

    fun startScan(isNfcAvailable: Boolean = true, isNfcEnabled: Boolean = true) {
        _uiState.update {
            when {
                !isNfcAvailable -> it.copy(
                    stage = ScanStage.ERROR,
                    isNfcAvailable = false,
                    isNfcEnabled = false,
                    message = "当前设备不支持 NFC",
                )

                !isNfcEnabled -> it.copy(
                    stage = ScanStage.ERROR,
                    isNfcAvailable = true,
                    isNfcEnabled = false,
                    message = "NFC 未开启，请先打开系统 NFC 开关",
                )

                else -> it.copy(
                    stage = ScanStage.SCANNING,
                    isNfcAvailable = true,
                    isNfcEnabled = true,
                    message = "扫描中，请将卡片保持贴近手机背部",
                )
            }
        }
    }

    suspend fun onTagDiscovered(result: ReadCardResult) {
        val cardInfo = result.cardInfo
        if (_uiState.value.cardInfo?.uid == cardInfo.uid && _uiState.value.stage == ScanStage.SUCCESS) {
            return
        }

        val capability = result.capability
        _uiState.update {
            it.copy(
                stage = ScanStage.TAG_DETECTED,
                message = "检测到卡片，正在解析能力",
            )
        }

        _uiState.update {
            it.copy(
                stage = ScanStage.SUCCESS,
                cardInfo = cardInfo,
                capability = capability,
                message = buildString {
                    append("已识别 ${cardInfo.techType.name}，UID：${cardInfo.uid}")
                    if (result.records.isNotEmpty()) {
                        append("，NDEF 记录：${result.records.size}")
                    }
                },
            )
        }

        if (_uiState.value.mode == ScanMode.READ) {
            AuditLogManager.save(
                operationType = "READ",
                cardUid = cardInfo.uid,
                cardType = cardInfo.techType.name,
                result = "SUCCESS",
                message = "读卡成功，识别到 ${result.records.size} 条 NDEF 记录",
            )
            ReadResultStore.save(result)
            _uiEffect.emit(ScanUiEffect.NavigateToReadResult(result))
        }
    }

    fun onError(message: String) {
        _uiState.update {
            it.copy(stage = ScanStage.ERROR, cardInfo = null, capability = null, message = message)
        }
    }

    fun simulateReadCard() {
        _uiState.update {
            it.copy(stage = ScanStage.TAG_DETECTED, message = "检测到卡片，正在解析能力")
        }
        viewModelScope.launch {
            val cardInfo = demoCardInfo()
            onTagDiscovered(
                ReadCardResult(
                    cardInfo = cardInfo,
                    capability = capabilityResolver.resolve(cardInfo),
                    ndefMessageCount = 1,
                    records = emptyList(),
                    detailItems = emptyList(),
                    rawTechList = listOf("android.nfc.tech.Ndef"),
                    isNdefTag = true,
                    readStatus = "EMPTY_NDEF",
                    readReason = "演示数据：当前是 NDEF 标签，但没有记录内容。",
                    debugMessage = "demo=true; isNdefTag=true; ndefMessageCount=1; recordCount=0",
                )
            )
        }
    }

    fun reset() {
        _uiState.update {
            it.copy(
                stage = ScanStage.IDLE,
                cardInfo = null,
                capability = null,
                message = "请将卡片贴近手机背部 NFC 区域",
            )
        }
    }

    fun demoCardInfo(): CardInfo = CardInfo(
        uid = "04A1B2C3D4",
        techType = TechType.NDEF,
        summary = "演示卡片：标准 NDEF 标签",
    )
}
