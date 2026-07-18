package net.dege.salmon

import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import net.dege.salmon.ui.theme.SalmonTheme
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val viewModel: TunerViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initTuner()
        }
        else {
            // If no mic access, no tuner app!
            finishAndRemoveTask()
        }
    }

    fun startTuner() {
        TunerFunctionality().startTuner {
            pitch, probability ->
                viewModel.updateIncomingFrequency(
                    pitch,
                    probability
                )
        }
    }

    fun startTunerInactivityLimit() {
        TunerFunctionality().startTunerInactivityLimit {
            viewModel.checkLastDetectionTime()
        }
    }

    fun startGridFlow() {
        Timer().schedule(
            object : TimerTask() {
                override fun run() {
                    viewModel.updateGridShift()
                }
            },
            0,
            TunerConfig.GRID_FLOW_UPDATE_RATE_MS.toLong()
        )
    }

    private fun initTuner() {
        startTuner()
        startTunerInactivityLimit()
        startGridFlow()
    }

    // TODO: Some functions may have to be private.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = Color.TRANSPARENT
            )
        )
        // TODO: Make sure that edge-to-edge usage is correct.

        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                initTuner()
            }
            else -> {
                requestPermissionLauncher.launch(
                    android.Manifest.permission.RECORD_AUDIO
                )
            }
        }

        setContent {
            SalmonTheme {
                TunerScreen(viewModel)
            }
        }
    }
}

