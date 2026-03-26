package com.emanueledipietro.remodex

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emanueledipietro.remodex.feature.appshell.AppViewModel
import com.emanueledipietro.remodex.feature.appshell.AppViewModelFactory
import com.emanueledipietro.remodex.feature.appshell.RemodexApp
import com.emanueledipietro.remodex.platform.notifications.parseThreadIdFromRoute
import com.emanueledipietro.remodex.ui.theme.RemodexTheme

class MainActivity : ComponentActivity() {
    private val appContainer
        get() = (application as RemodexApplication).container

    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(appContainer.appRepository)
    }
    private var pendingThreadDeepLinkId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        pendingThreadDeepLinkId = intent.threadDeepLinkId()
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            RemodexTheme(
                appearanceMode = uiState.appearanceMode,
                appFontStyle = uiState.appFontStyle,
            ) {
                RemodexApp(
                    viewModel = viewModel,
                    notificationManager = appContainer.notificationManager,
                    pendingThreadDeepLinkId = pendingThreadDeepLinkId,
                    onThreadDeepLinkHandled = {
                        pendingThreadDeepLinkId = null
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingThreadDeepLinkId = intent.threadDeepLinkId()
    }
}

private fun Intent.threadDeepLinkId(): String? {
    return parseThreadIdFromRoute(dataString)
        ?: extras?.getString("threadId")
        ?: extras?.getString("thread_id")
}
