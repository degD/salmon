package net.dege.salmon

data class TunerSettings(
    val darkTheme: Boolean,
    val isCorrectThreshold: Float,
    val simplifyCentsDisplay: Boolean,
    val simplificationFactor: Int,
)

val defaultSettings: TunerSettings = TunerSettings(
    darkTheme = true,
    isCorrectThreshold = 10f,
    simplifyCentsDisplay = false,
    simplificationFactor = 25
)