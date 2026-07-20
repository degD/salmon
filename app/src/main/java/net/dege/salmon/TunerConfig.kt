package net.dege.salmon

/**
 * Global configuration constants for the tuner architecture.
 * Contains central values managing DSP properties, audio threshold values,
 * rendering animations, and pipeline calculations.
 */
object TunerConfig {

    // Tuner functionality related configs:
    const val SAMPLE_RATE = 44100
    const val AUDIO_BUFFER_SIZE = 4096
    const val BUFFER_OVERLAP = 0
    const val PROBABILITY_THRESHOLD = 0.9f
    const val CORRECT_TIME_MS = 1000
    const val LAST_DETECT_TIME_MS = 3000
    const val GRID_FLOW_UPDATE_RATE_MS = 100
    const val GRID_FLOW_STEP_DP = 1
    const val GRID_SIZE_DP = 25
    const val NOTE_AUDIO_DURATION_SEC = 0.8f
    const val SIMPLIFYING_FACTOR = 5
    const val CENTS_SMOOTHING_FACTOR = 0.5f
    const val TUNER_FUNC_AMPLIFICATION_FACTOR = 4.0
}