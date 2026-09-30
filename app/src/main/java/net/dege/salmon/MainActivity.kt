package net.dege.salmon

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import net.dege.salmon.ui.theme.SalmonTheme
import java.util.Timer
import java.util.TimerTask
import kotlin.getValue

/**
 * The core entry point for the application. Coordinates runtime microphone permissions,
 * configures an edge-to-edge system UI, initializes background processing loops, and
 * binds the UI architecture to [TunerViewModel].
 */
class MainActivity : ComponentActivity() {
    private val viewModel: TunerViewModel by viewModels()

    private val tunerFunctionality = TunerFunctionality()
    private var gridTimer: Timer? = null
    private var isTunerInitialized = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initTuner()
        } else {
            // If no mic access, no tuner app!
            finishAndRemoveTask()
        }
    }

    /**
     * Connects pitch processing to the presentation layer by forwarding raw pitch data
     * and detection probability directly to the view model.
     */
    private fun startTuner() {
        tunerFunctionality.startTuner { pitch, probability ->
            viewModel.updateIncomingFrequency(pitch, probability)
        }
    }

    /**
     * Initializes the background ticker to continuously evaluate user inactivity thresholds.
     */
    private fun startTunerInactivityLimit() {
        tunerFunctionality.startTunerInactivityLimit {
            viewModel.checkLastDetectionTime()
        }
    }

    /**
     * Schedules a recurring [TimerTask] responsible for animating the grid rendering canvas
     * layer according to config-defined refresh intervals.
     */
    private fun startGridFlow() {
        if (gridTimer != null) return
        gridTimer = Timer().apply {
            schedule(
                object : TimerTask() {
                    override fun run() {
                        viewModel.updateGridShift()
                    }
                },
                0,
                TunerConfig.GRID_FLOW_UPDATE_RATE_MS.toLong()
            )
        }
    }

    /**
     * Groups and starts background tasks. Idempotent so it is safe to call from both
     * `onCreate` and the permission result callback.
     */
    private fun initTuner() {
        if (isTunerInitialized) return
        isTunerInitialized = true
        startTuner()
        startTunerInactivityLimit()
        startGridFlow()
    }

    override fun onDestroy() {
        super.onDestroy()
        tunerFunctionality.stop()
        gridTimer?.cancel()
        gridTimer = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT)
        )

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