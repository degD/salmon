package net.dege.salmon

object TunerConfig {

    // Tuner functionality related configs:
    const val SAMPLE_RATE = 44100
    const val AUDIO_BUFFER_SIZE = 2048
    const val BUFFER_OVERLAP = 3
    const val PROBABILITY_THRESHOLD = 0.96f
    const val CORRECT_TIME_MS = 1500
    const val LAST_DETECT_TIME_MS = 4000
    const val GRID_FLOW_UPDATE_RATE_MS = 100
    const val GRID_FLOW_STEP_DP = 1
    const val GRID_SIZE_DP = 25
    const val NOTE_AUDIO_DURATION_SEC = 0.8f
}