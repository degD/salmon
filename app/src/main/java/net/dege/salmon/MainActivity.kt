package net.dege.salmon

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import net.dege.salmon.ui.theme.SalmonTheme
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val viewModel: TunerViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startTuner()
            startTunerInactivityLimit()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startTuner()
                startTunerInactivityLimit()
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

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    val viewModel = TunerViewModel()
    SalmonTheme {
        TunerScreen(viewModel)
    }
}
