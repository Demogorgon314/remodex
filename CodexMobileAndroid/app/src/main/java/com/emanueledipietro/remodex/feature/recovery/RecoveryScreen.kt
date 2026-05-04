package com.emanueledipietro.remodex.feature.recovery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.emanueledipietro.remodex.data.connection.PairingQrPayload
import com.emanueledipietro.remodex.data.connection.PairingQrValidationResult
import com.emanueledipietro.remodex.data.connection.SecureConnectionSnapshot
import com.emanueledipietro.remodex.data.connection.SecureConnectionState
import com.emanueledipietro.remodex.data.connection.statusLabel
import com.emanueledipietro.remodex.data.connection.validatePairingQrCode
import com.emanueledipietro.remodex.model.RemodexBridgeUpdatePrompt
import com.emanueledipietro.remodex.model.remodexLocalizedText
import com.emanueledipietro.remodex.ui.RemodexBrandMark

@Composable
fun RecoveryScreen(
    recoveryState: SecureConnectionSnapshot,
    connectionHeadline: String,
    connectionMessage: String,
    onPairWithQrPayload: (PairingQrPayload) -> Unit,
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var cameraDenied by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var bridgeUpdatePrompt by remember { mutableStateOf<RemodexBridgeUpdatePrompt?>(null) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraDenied = !granted
        showScanner = granted
    }

    fun requestScanner() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            showScanner = true
            cameraDenied = false
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            shape = RoundedCornerShape(30.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RemodexBrandMark(
                    modifier = Modifier.size(88.dp),
                    cornerRadius = 22.dp,
                )
                Text(
                    text = remodexLocalizedText("QR 配对仍然可用", "QR pairing stays in the loop"),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = remodexLocalizedText(
                        "Android 会保留显式恢复入口, 避免过期的可信 session 让你卡住.",
                        "Android keeps recovery explicit so a stale trusted session never leaves you stranded.",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        RecoveryChecklistCard(
            headline = connectionHeadline,
            message = connectionMessage,
            onOpenScanner = {
                scanError = null
                bridgeUpdatePrompt = null
                requestScanner()
            },
            onRetryConnection = onRetryConnection,
        )

        RecoveryConnectionCard(
            recoveryState = recoveryState,
            onRetryConnection = onRetryConnection,
            onOpenScanner = { requestScanner() },
        )

        if (cameraDenied) {
            CameraPermissionCard(
                onRetry = { requestScanner() },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                },
            )
        }

        if (showScanner) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = RoundedCornerShape(26.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = remodexLocalizedText("扫描配对 QR", "Scan pairing QR"),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = remodexLocalizedText(
                            "将摄像头对准 remodex up 打印出的 QR code. Android 客户端会先校验内容, 再开始安全 bootstrap.",
                            "Point the camera at the QR printed by remodex up. The Android client validates the payload before it tries any secure bootstrap.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PairingScannerView(
                        onScan = { code ->
                            when (val result = validatePairingQrCode(code)) {
                                is PairingQrValidationResult.Success -> {
                                    scanError = null
                                    bridgeUpdatePrompt = null
                                    showScanner = false
                                    onPairWithQrPayload(result.payload)
                                }

                                is PairingQrValidationResult.ShortCode -> {
                                    scanError = "Use Pair with Code from the previous screen."
                                    bridgeUpdatePrompt = null
                                    showScanner = false
                                }

                                is PairingQrValidationResult.ScanError -> {
                                    scanError = result.message
                                    bridgeUpdatePrompt = null
                                    showScanner = false
                                }

                                is PairingQrValidationResult.BridgeUpdateRequired -> {
                                    bridgeUpdatePrompt = result.prompt
                                    scanError = null
                                    showScanner = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                    )
                }
            }
        }

        scanError?.let { message ->
            RecoveryStatusCard(
                title = remodexLocalizedText("配对 QR 被拒绝", "Pairing QR rejected"),
                body = message,
                actionLabel = remodexLocalizedText("扫描其他 QR", "Scan a different QR"),
                onAction = {
                    scanError = null
                    requestScanner()
                },
            )
        }

        bridgeUpdatePrompt?.let { prompt ->
            val body = buildString {
                append(prompt.message)
                prompt.command?.trim()?.takeIf(String::isNotEmpty)?.let { command ->
                    append(remodexLocalizedText("\n\n运行: ", "\n\nRun: "))
                    append(command)
                }
            }
            RecoveryStatusCard(
                title = prompt.title,
                body = body,
                actionLabel = remodexLocalizedText("更新后重新扫描", "Scan again after updating"),
                onAction = {
                    bridgeUpdatePrompt = null
                    requestScanner()
                },
            )
        }
    }
}

@Composable
private fun RecoveryConnectionCard(
    recoveryState: SecureConnectionSnapshot,
    onRetryConnection: () -> Unit,
    onOpenScanner: () -> Unit,
) {
    val body = buildString {
        append(recoveryState.phaseMessage)
        recoveryState.macDeviceId?.let { macDeviceId ->
            append(remodexLocalizedText("\n\n电脑: ", "\n\nComputer: "))
            append(macDeviceId)
        }
        recoveryState.macFingerprint?.let { fingerprint ->
            append(remodexLocalizedText("\n指纹: ", "\nFingerprint: "))
            append(fingerprint)
        }
        recoveryState.bridgeUpdateCommand?.let { command ->
            append(remodexLocalizedText("\n\n更新命令: ", "\n\nUpdate command: "))
            append(command)
        }
    }
    val action = when (recoveryState.secureState) {
        SecureConnectionState.TRUSTED_MAC,
        SecureConnectionState.LIVE_SESSION_UNRESOLVED,
        SecureConnectionState.RECONNECTING,
        SecureConnectionState.HANDSHAKING -> remodexLocalizedText(
            "重试可信重连",
            "Retry trusted reconnect",
        ) to onRetryConnection

        SecureConnectionState.NOT_PAIRED,
        SecureConnectionState.REPAIR_REQUIRED,
        SecureConnectionState.UPDATE_REQUIRED -> remodexLocalizedText("打开 QR 扫描", "Open QR scanner") to onOpenScanner

        SecureConnectionState.ENCRYPTED -> null
    }

    RecoveryStatusCard(
        title = recoveryState.secureState.statusLabel,
        body = body,
        actionLabel = action?.first,
        onAction = action?.second,
    )
}

@Composable
private fun RecoveryChecklistCard(
    headline: String,
    message: String,
    onOpenScanner: () -> Unit,
    onRetryConnection: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = remodexLocalizedText(
                    "1. 在电脑上运行 remodex up.\n2. 保持配对 QR code 可见.\n3. 在这里重试可信重连, 如果安全 session 已轮换就重新扫描.",
                    "1. Run remodex up on your computer.\n2. Keep the pairing QR visible.\n3. Retry trusted reconnect here or rescan when the secure session has rotated.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRetryConnection) {
                Text(remodexLocalizedText("重试可信重连", "Retry trusted reconnect"))
            }
            Button(onClick = onOpenScanner) {
                Text(remodexLocalizedText("打开 QR 扫描", "Open QR scanner"))
            }
        }
    }
}

@Composable
private fun CameraPermissionCard(
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = remodexLocalizedText("相机权限未开启", "Camera access is off"),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = remodexLocalizedText(
                    "Android 只会在打开扫描器时请求相机权限. 授权后即可扫描新的配对 QR code. 如果之前已拒绝, 请打开系统设置.",
                    "Android only asks for camera permission when you open the scanner. Grant access to scan a fresh pairing QR, or open system settings if permission was denied earlier.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry) {
                Text(remodexLocalizedText("重新请求相机权限", "Try camera permission again"))
            }
            Button(onClick = onOpenSettings) {
                Text(remodexLocalizedText("打开应用设置", "Open app settings"))
            }
        }
    }
}

@Composable
private fun RecoveryStatusCard(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
