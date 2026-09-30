package net.dege.salmon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import net.dege.salmon.ui.theme.*
import kotlin.math.round

@Composable
fun TitleSection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value
    Row(modifier = modifier
        .fillMaxSize()
        .background(SalmonColor3)
        .padding(16.dp, 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Salmon",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SalmonColor1,
            )
        }
        Column(
            Modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Switch(
                checked = state.mode == TunerMode.AUTO,
                onCheckedChange = { viewModel.toggleMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SalmonColor4,
                    checkedTrackColor = SalmonColor1,
                    checkedBorderColor = Color.Transparent,
                    checkedIconColor = SalmonColor4,
                    uncheckedThumbColor = SalmonColor3,
                    uncheckedTrackColor = SalmonColor2,
                    uncheckedBorderColor = Color.Transparent,
                    uncheckedIconColor = SalmonColor4,
                )
            )
            Text(
                if (state.mode == TunerMode.AUTO) "AUTO" else "MANUAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = SalmonColor1,
            )
        }
    }
}

@Composable
fun NoteButton(
    modifier: Modifier = Modifier,
    note: String,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value
    val noteIndex = state.notes.indexOf(note)
    val noteFreq = tableOfFreq[note]
    val isNoteCorrect = if (noteIndex >= 0) state.isCorrect[noteIndex] else false

    Box(modifier = modifier
        .size(64.dp)
        .padding(8.dp)
        .background(
            SalmonColor2,
            CircleShape,
        )
        .border(
            if (isNoteCorrect) 2.dp else 0.dp,
            if (isNoteCorrect) SalmonColor5 else Color.Transparent,
            CircleShape
        )
        .clickable {
            val freq = noteFreq ?: return@clickable
            viewModel.setSelectedNote(note)
            viewModel.setModeManual()
            viewModel.playNote(freq = freq)
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            note,
            color = if (note != state.selectedNote) SalmonColor4 else SalmonColor1
        )
    }
}

@Composable
fun NotesColumn(
    modifier: Modifier = Modifier,
    range: IntRange,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value
    Column(
        modifier = modifier
            .fillMaxHeight(0.6f)
            .absoluteOffset(0.dp, (-12).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        range.forEach { rangeIndex ->
            val note = state.notes.getOrNull(rangeIndex) ?: return@forEach
            NoteButton(Modifier, note, viewModel)
        }
    }
}

@Composable
fun NoteDisplaySection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(SalmonColor3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotesColumn(Modifier.weight(2f), 0..2, viewModel)

        Box(Modifier.weight(6f))

        NotesColumn(Modifier.weight(2f), 3..5, viewModel)
    }
}

@Composable
fun FlowingGrid(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridShiftPx = viewModel.tunerState.value.gridShift.toPx()
        val cellSizePx = TunerConfig.GRID_SIZE_DP.dp.toPx()
        val numOfCellsWidthHalf = (size.width / cellSizePx / 2).toInt()
        val numOfCellsHeightHalf = (size.height / cellSizePx / 2).toInt()
        val centerW = size.width / 2
        val centerH = size.height / 2

        // Draw a growing line while the current reading is within the correct threshold.
        val correctStartTime = viewModel.tunerState.value.correctStartTime
        var durationMs = correctStartTime?.elapsedNow()?.inWholeMilliseconds ?: 0
        durationMs = if (durationMs < TunerConfig.CORRECT_TIME_MS)
            durationMs else TunerConfig.CORRECT_TIME_MS.toLong()
        val correctLineLength = size.height * durationMs / TunerConfig.CORRECT_TIME_MS
        val correctLineStart = centerH - correctLineLength / 2
        val correctLineEnd = centerH + correctLineLength / 2
        drawLine(
            SalmonColor1,
            Offset(centerW, correctLineStart),
            Offset(centerW, correctLineEnd),
            strokeWidth = 4.dp.toPx()
        )

        // Draw a full-height green line when the selected note has been confirmed correct.
        val selectedNote = viewModel.tunerState.value.selectedNote
        if (selectedNote != null) {
            val noteIndex = viewModel.tunerState.value.notes.indexOf(selectedNote)
            if (noteIndex >= 0 && viewModel.tunerState.value.isCorrect[noteIndex]) {
                drawLine(
                    SalmonColor5,
                    Offset(centerW, 0f),
                    Offset(centerW, size.height),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }

        val lineColor = SalmonColor4.copy(alpha = 0.2f)

        for (i in 0..numOfCellsWidthHalf) {
            drawLine(
                lineColor,
                Offset(centerW + i * cellSizePx, 0f),
                Offset(centerW + i * cellSizePx, size.height),
                strokeWidth = 1.dp.toPx()
            )
            if (i > 0) {
                drawLine(
                    lineColor,
                    Offset(centerW - i * cellSizePx, 0f),
                    Offset(centerW - i * cellSizePx, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        for (i in 0..numOfCellsHeightHalf + 1) {
            drawLine(
                lineColor,
                Offset(0f, gridShiftPx + centerH + i * cellSizePx),
                Offset(size.width, gridShiftPx + centerH + i * cellSizePx),
                strokeWidth = 1.dp.toPx()
            )
            if (i > 0) {
                drawLine(
                    lineColor,
                    Offset(0f, gridShiftPx + centerH - i * cellSizePx),
                    Offset(size.width, gridShiftPx + centerH - i * cellSizePx),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun TuningSliderSection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value
    val settings = viewModel.tunerSettings.value
    val lastDetectionTime = state.lastDetectionTime
    val selectedNote = state.selectedNote

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SalmonColor3),
        contentAlignment = Alignment.Center,
    ) {
        val boxWidth = maxWidth
        var offsetText = ""

        if (lastDetectionTime != null) {
            offsetText = if (settings.simplifyCentsDisplay) {
                val factor = settings.simplificationFactor.coerceAtLeast(1)
                "${round(state.centsOffset / factor).toInt()}"
            } else {
                "${round(state.centsOffset).toInt()}"
            }
        }

        fun convertCentsOffsetToOffsetX(): Dp {
            var centsOffset = state.centsOffset
            if (centsOffset < -100) centsOffset = -100f
            else if (centsOffset > 100) centsOffset = 100f
            return (boxWidth - 40.dp) * centsOffset / 200f
        }

        FlowingGrid(viewModel = viewModel)

        Box(
            Modifier.absoluteOffset(
                if (lastDetectionTime != null) convertCentsOffsetToOffsetX() else 0.dp,
                (-60).dp
            )
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(SalmonColor6, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(offsetText, color = SalmonColor1)
            }
        }

        if (lastDetectionTime != null && selectedNote != null) {
            Box(Modifier.absoluteOffset(0.dp, 60.dp)) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(SalmonColor2, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selectedNote, color = SalmonColor4)
                }
            }
        }
    }
}

@Composable
fun FooterSection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    Column(modifier = modifier
        .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Button(
            onClick = { viewModel.restoreDefaults() },
            colors = ButtonDefaults.buttonColors(
                containerColor = SalmonColor2,
                contentColor = SalmonColor4,
                disabledContainerColor = SalmonColor2,
                disabledContentColor = SalmonColor4,
            )
        ) {
            Text("Start Over")
        }
    }
}

@Composable
fun TunerScreen(viewModel: TunerViewModel) {
    Box(
        modifier = Modifier
            .background(SalmonColor6)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SalmonColor3),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            TitleSection(
                modifier = Modifier.weight(1f),
                viewModel = viewModel
            )

            TuningSliderSection(
                modifier = Modifier.weight(3f),
                viewModel = viewModel
            )

            NoteDisplaySection(
                modifier = Modifier.weight(5f),
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            androidx.compose.foundation.Image(
                modifier = Modifier
                    .fillMaxSize()
                    .absoluteOffset(2.dp),
                alignment = Alignment.BottomCenter,
                painter = painterResource(R.drawable.piemaster_gretsch_jet_firebird_headstock2),
                contentDescription = ""
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
            contentAlignment = Alignment.BottomEnd
        ) {
            FooterSection(
                modifier = Modifier.fillMaxHeight(0.1f),
                viewModel = viewModel
            )
        }
    }
}