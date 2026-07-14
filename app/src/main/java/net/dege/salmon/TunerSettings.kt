package net.dege.salmon

import net.dege.salmon.ui.settings.TuningPreset
import net.dege.salmon.ui.theme.ThemeMode

data class TunerSettings(
    val referencePitch: Float = 440f,
    val correctThreshold: Float = 15f,
    val tuningPreset: TuningPreset = TuningPreset.STANDARD,
    val autoMode: Boolean = true,
    val showGrid: Boolean = true,
    val hapticFeedback: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
)

val defaultSettings: TunerSettings = TunerSettings()
