package com.streampanel

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import com.streampanel.core.designsystem.LocalAppStrings
import com.streampanel.core.designsystem.StreamPanelTheme
import com.streampanel.core.designsystem.stringsFor
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            val language by viewModel.appLanguage.collectAsStateWithLifecycle()
            LaunchedEffect(appearance.keepScreenOn) {
                if (appearance.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            CompositionLocalProvider(LocalAppStrings provides stringsFor(language)) {
                StreamPanelTheme(appearance = appearance) {
                    StreamPanelNavHost()
                }
            }
        }
    }
}
