package com.emanueledipietro.remodex.feature.recovery

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emanueledipietro.remodex.data.connection.PairingQrPayload
import com.emanueledipietro.remodex.data.connection.PairingQrValidationResult
import com.emanueledipietro.remodex.data.connection.validatePairingQrCode
import com.emanueledipietro.remodex.model.RemodexBridgeUpdatePrompt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PairingScannerScreen(
    onDismiss: () -> Unit,
    onPairWithQrPayload: (PairingQrPayload) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var scannerError by remember { mutableStateOf<String?>(null) }
    var bridgeUpdatePrompt by remember { mutableStateOf<RemodexBridgeUpdatePrompt?>(null) }
    var didCopyBridgeUpdateCommand by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isCheckingPermission by remember { mutableStateOf(true) }
    var scanResetCounter by remember { mutableIntStateOf(0) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        isCheckingPermission = false
    }

    fun refreshCameraPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = granted
        isCheckingPermission = false
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
            isCheckingPermission = false
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshCameraPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            isCheckingPermission -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }

            bridgeUpdatePrompt != null -> {
                PairingBridgeUpdateView(
                    prompt = bridgeUpdatePrompt!!,
                    didCopyCommand = didCopyBridgeUpdateCommand,
                    onCopyCommand = { command ->
                        copyToClipboard(context, command)
                        didCopyBridgeUpdateCommand = true
                        scope.launch {
                            delay(1500)
                            didCopyBridgeUpdateCommand = false
                        }
                    },
                    onContinue = {
                        bridgeUpdatePrompt = null
                        didCopyBridgeUpdateCommand = false
                        scanResetCounter += 1
                    },
                )
            }

            hasCameraPermission -> {
                PairingScannerView(
                    onScan = { code ->
                        when (val result = validatePairingQrCode(code)) {
                            is PairingQrValidationResult.Success -> onPairWithQrPayload(result.payload)
                            is PairingQrValidationResult.ScanError -> scannerError = result.message
                            is PairingQrValidationResult.BridgeUpdateRequired -> {
                                didCopyBridgeUpdateCommand = false
                                bridgeUpdatePrompt = result.prompt
                            }
                        }
                    },
                    scanResetCounter = scanResetCounter,
                    modifier = Modifier.fillMaxSize(),
                )
                PairingScannerOverlay(
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> {
                PairingCameraPermissionView(
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                )
            }
        }

        PairingScannerBackButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 8.dp),
        )
    }

    scannerError?.let { message ->
        AlertDialog(
            onDismissRequest = {
                scannerError = null
                scanResetCounter += 1
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scannerError = null
                        scanResetCounter += 1
                    },
                ) {
                    Text("OK")
                }
            },
            title = {
                Text("Scan Error")
            },
            text = {
                Text(message)
            },
        )
    }
}

@Composable
private fun PairingScannerBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Back"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun PairingScannerOverlay(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.6f),
                ),
            ) {}
        }
        Spacer(modifier = Modifier.size(24.dp))
        Text(
            text = "Scan QR code from Remodex CLI",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PairingCameraPermissionView(
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.size(20.dp))
        Text(
            text = "Camera access needed",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "Open Settings and allow camera access to scan the pairing QR code.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(24.dp))
        Button(onClick = onOpenSettings) {
            Text("Open Settings")
        }
    }
}

@Composable
private fun PairingBridgeUpdateView(
    prompt: RemodexBridgeUpdatePrompt,
    didCopyCommand: Boolean,
    onCopyCommand: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val command = prompt.command?.trim()?.takeIf(String::isNotEmpty)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.size(56.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = prompt.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                text = prompt.message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.82f),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (command != null) {
                Text(
                    text = "Run this on your Mac",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                )

                PairingBridgeUpdateStep(
                    number = "1",
                    title = "Update Remodex",
                    detail = command,
                    buttonLabel = if (didCopyCommand) "Copied" else "Copy",
                    onButtonClick = { onCopyCommand(command) },
                )
                PairingBridgeUpdateStep(
                    number = "2",
                    title = "Restart the bridge",
                    detail = "Run remodex up",
                )
                PairingBridgeUpdateStep(
                    number = "3",
                    title = "Show a new QR code",
                    detail = "Use the new QR shown in the terminal",
                )
                PairingBridgeUpdateStep(
                    number = "4",
                    title = "Scan it here",
                    detail = "Then scan the new QR code from this phone",
                )
            } else {
                Text(
                    text = "Do this on your Android phone",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                )

                PairingBridgeUpdateStep(
                    number = "1",
                    title = "Update Remodex",
                    detail = "Install the latest Remodex build on this Android phone.",
                )
                PairingBridgeUpdateStep(
                    number = "2",
                    title = "Try again",
                    detail = "Come back here, then retry the connection or scan a fresh QR code.",
                )
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("I Updated It")
        }

        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun PairingBridgeUpdateStep(
    number: String,
    title: String,
    detail: String,
    buttonLabel: String? = null,
    onButtonClick: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(20.dp),
                shape = CircleShape,
                color = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = number,
                        color = Color.Black,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                ) {
                    Text(
                        text = detail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
                if (buttonLabel != null && onButtonClick != null) {
                    TextButton(onClick = onButtonClick) {
                        Text(buttonLabel, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(
    context: Context,
    command: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Remodex command", command))
}
