package com.opencode.nfccardmanager.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opencode.nfccardmanager.core.nfc.model.NfcFlowTone
import com.opencode.nfccardmanager.ui.theme.BlueInfoBg
import com.opencode.nfccardmanager.ui.theme.BluePrimary
import com.opencode.nfccardmanager.ui.theme.GrayBorder
import com.opencode.nfccardmanager.ui.theme.GraySurface
import com.opencode.nfccardmanager.ui.theme.GreenSuccess
import com.opencode.nfccardmanager.ui.theme.GreenSuccessBg
import com.opencode.nfccardmanager.ui.theme.RedDanger
import com.opencode.nfccardmanager.ui.theme.RedDangerBg
import com.opencode.nfccardmanager.ui.theme.TextPrimary
import com.opencode.nfccardmanager.ui.theme.TextSecondary
import com.opencode.nfccardmanager.ui.theme.YellowWarning
import com.opencode.nfccardmanager.ui.theme.YellowWarningBg

enum class StatusTone { SUCCESS, WARNING, ERROR, INFO }

fun NfcFlowTone.toStatusTone(): StatusTone = when (this) {
    NfcFlowTone.SUCCESS -> StatusTone.SUCCESS
    NfcFlowTone.WARNING -> StatusTone.WARNING
    NfcFlowTone.DANGER -> StatusTone.ERROR
    NfcFlowTone.INFO,
    NfcFlowTone.NEUTRAL,
    -> StatusTone.INFO
}

val PagePadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)

fun Modifier.appPagePadding(innerPadding: PaddingValues): Modifier =
    this
        .fillMaxSize()
        .padding(innerPadding)
        .padding(PagePadding)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            }
        },
        actions = {
            if (actionText != null && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Text(actionText)
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GraySurface),
        border = BorderStroke(1.dp, GrayBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun StatusPill(text: String, tone: StatusTone) {
    val (bg, fg) = when (tone) {
        StatusTone.SUCCESS -> GreenSuccessBg to GreenSuccess
        StatusTone.WARNING -> YellowWarningBg to YellowWarning
        StatusTone.ERROR -> RedDangerBg to RedDanger
        StatusTone.INFO -> BlueInfoBg to BluePrimary
    }
    Text(
        text = text,
        color = fg,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
}

@Composable
fun KeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BluePrimary,
            contentColor = Color.White,
        ),
    ) {
        Text(text)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GrayBorder),
    ) {
        Text(text = text, color = TextPrimary)
    }
}

@Composable
fun DangerActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RedDanger,
            contentColor = Color.White,
        ),
    ) {
        Text(text)
    }
}
