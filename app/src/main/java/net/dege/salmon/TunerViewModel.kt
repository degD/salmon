package net.dege.salmon

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlin.collections.listOf

class TunerViewModel : ViewModel() {
    private val _tunerState = mutableStateOf(TunerState(
        TunerMode.AUTO,
        400f,
        5f,
        false,
        listOf(
            Pair("D", 0f), Pair("A", 0f), Pair("E", 0f),
            Pair("G", 0f), Pair("B", 0f), Pair("E", 0f),
        )
    ))
    val tunerState: State<TunerState> = _tunerState

    // You will call your TunerFunctionality.kt from here later
    fun startListening() {
        // Mocking a slight change in pitch
        _tunerState.value = TunerState(
            TunerMode.AUTO,
            420f,
            5f,
            false,
            listOf(
                Pair("E2", 100f), Pair("B", 150f), Pair("G", 200f), Pair("A", 440f)
            )
        )
    }
}