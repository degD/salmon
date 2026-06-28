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
        Pair("D", 0f), Pair("A", 0f), Pair("E", 0f),
        Pair("G", 0f), Pair("B", 0f), Pair("E", 0f),
    )
)
