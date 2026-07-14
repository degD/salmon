package net.dege.salmon

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import net.dege.salmon.ui.settings.SettingsStore
import net.dege.salmon.ui.theme.AppTheme
import net.dege.salmon.ui.theme.SalmonTheme
import java.util.Timer
import java.util.TimerTask
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val viewModel: TunerViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startTuner()
            startTunerInactivityLimit()
            startGridFlow()
        } else {
            finishAndRemoveTask()
        }
    }

    fun startTuner() {
        TunerFunctionality().startTuner { pitch, probability ->
            viewModel.updateIncomingFrequency(pitch, probability)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SettingsStore.init(this)
        AppTheme.themeMode = SettingsStore.load().themeMode

        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startTuner()
                startTunerInactivityLimit()
                startGridFlow()
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }

        setContent {
            SalmonTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TunerScreen(viewModel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    val viewModel = TunerViewModel()
    SalmonTheme {
        TunerScreen(viewModel)
    }
}
