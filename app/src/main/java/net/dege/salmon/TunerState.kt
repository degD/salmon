package net.dege.salmon

enum class TunerMode {
    AUTO, MANUAL
}

data class TunerState(
    val mode: TunerMode = TunerMode.AUTO,
    val incomingFrequency: Float = 400f,
    val isCorrectThreshold: Float = 5f,
    val isCorrect: Boolean = false,
    val tableOfNotes: List<Pair<String, Float>> = listOf(
        Pair("E2", 100f), Pair("B", 150f), Pair("G", 200f), Pair("A", 440f)
    )
)
