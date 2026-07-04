package net.dege.salmon

data class TunerSettings(
    val darkTheme: Boolean,
    val isCorrectThreshold: Float,
    val simplifyCentsDisplay: Boolean,
    val simplificationFactor: Int,
)

val defaultSettings: TunerSettings = TunerSettings(
    true,
    isCorrectThreshold = 15f,
    false,
    0
)
