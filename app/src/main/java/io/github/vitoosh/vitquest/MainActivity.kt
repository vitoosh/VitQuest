package io.github.vitoosh.vitquest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.designsystem.theme.isDark
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.ui.VitQuestApp

/** AppCompatActivity is required for the per-app language API on Android 12 and lower. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val themeMode = (uiState as? MainActivityUiState.Ready)?.preferences?.themeMode ?: ThemeMode.SYSTEM
            val darkTheme = themeMode.isDark()

            // Keep system bar icons readable when the in-app theme differs from the system theme.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            VitQuestTheme(darkTheme = darkTheme) {
                when (val state = uiState) {
                    MainActivityUiState.Loading -> Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) { Box(Modifier.fillMaxSize()) }

                    is MainActivityUiState.Ready -> VitQuestApp(
                        onboardingCompleted = state.preferences.onboardingCompleted,
                    )
                }
            }
        }
    }

    private companion object {
        // Default scrims used by androidx.activity for three-button navigation.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
