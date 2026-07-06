package net.dege.salmon

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.TimeSource

enum class TunerMode {
    AUTO, MANUAL
}

val tableOfFreq: Map<String, Float> = mapOf(
    "C0" to 16.35f,  "C#0" to 17.32f,  "D0" to 18.35f,  "D#0" to 19.45f,  "E0" to 20.60f,  "F0" to 21.83f,  "F#0" to 23.12f,  "G0" to 24.50f,  "G#0" to 25.96f,  "A0" to 27.50f,  "A#0" to 29.14f,  "B0" to 30.87f,
    "C1" to 32.70f,  "C#1" to 34.65f,  "D1" to 36.71f,  "D#1" to 38.89f,  "E1" to 41.20f,  "F1" to 43.65f,  "F#1" to 46.25f,  "G1" to 49.00f,  "G#1" to 51.91f,  "A1" to 55.00f,  "A#1" to 58.27f,  "B1" to 61.74f,
    "C2" to 65.41f,  "C#2" to 69.30f,  "D2" to 73.42f,  "D#2" to 77.78f,  "E2" to 82.41f,  "F2" to 87.31f,  "F#2" to 92.50f,  "G2" to 98.00f,  "G#2" to 103.83f, "A2" to 110.00f, "A#2" to 116.54f, "B2" to 123.47f,
    "C3" to 130.81f, "C#3" to 138.59f, "D3" to 146.83f, "D#3" to 155.56f, "E3" to 164.81f, "F3" to 174.61f, "F#3" to 185.00f, "G3" to 196.00f, "G#3" to 207.65f, "A3" to 220.00f, "A#3" to 233.08f, "B3" to 246.94f,
    "C4" to 261.63f, "C#4" to 277.18f, "D4" to 293.66f, "D#4" to 311.13f, "E4" to 329.63f, "F4" to 349.23f, "F#4" to 369.99f, "G4" to 392.00f, "G#4" to 415.30f, "A4" to 440.00f, "A#4" to 466.16f, "B4" to 493.88f,
    "C5" to 523.25f, "C#5" to 554.37f, "D5" to 587.33f, "D#5" to 622.25f, "E5" to 659.25f, "F5" to 698.46f, "F#5" to 739.99f, "G5" to 783.99f, "G#5" to 830.61f, "A5" to 880.00f, "A#5" to 932.33f, "B5" to 987.77f,
    "C6" to 1046.50f, "C#6" to 1108.73f, "D6" to 1174.66f, "D#6" to 1244.51f, "E6" to 1318.51f, "F6" to 1396.91f, "F#6" to 1479.98f, "G6" to 1567.98f, "G#6" to 1661.22f, "A6" to 1760.00f, "A#6" to 1864.66f, "B6" to 1975.53f,
    "C7" to 2093.00f, "C#7" to 2217.46f, "D7" to 2349.32f, "D#7" to 2489.02f, "E7" to 2637.02f, "F7" to 2793.83f, "F#7" to 2959.96f, "G7" to 3135.96f, "G#7" to 3322.44f, "A7" to 3520.00f, "A#7" to 3729.31f, "B7" to 3951.07f,
    "C8" to 4186.01f, "C#8" to 4434.92f, "D8" to 4698.63f, "D#8" to 4978.03f, "E8" to 5274.04f, "F8" to 5587.65f, "F#8" to 5919.91f, "G8" to 6271.93f, "G#8" to 6644.88f, "A8" to 7040.00f, "A#8" to 7458.62f, "B8" to 7902.13f
) // TODO: Assuming they are correct. Check.

val defaultTunerState: TunerState = TunerState(
    TunerMode.AUTO,
    -1f,
    0f,
    listOf(false, false, false, false, false, false),
    listOf("D3", "A2", "E2", "G3", "B3", "E4"),
    null,
    0f,
    null,
    null,
    0.dp
)

data class TunerState(
    val mode: TunerMode,
    val incomingFrequency: Float,
    val incomingFrequencyProbability: Float,
    val isCorrect: List<Boolean>,
    val notes: List<String>,
    val selectedNote: String?,
    val centsOffset: Float,
    val correctStartTime: TimeSource.Monotonic.ValueTimeMark?,
    val lastDetectionTime: TimeSource.Monotonic.ValueTimeMark?,
    val gridShift: Dp
)
