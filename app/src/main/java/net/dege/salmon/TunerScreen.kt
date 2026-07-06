package net.dege.salmon

import android.content.pm.PackageManager
import android.widget.Switch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.checkSelfPermission
import kotlinx.coroutines.flow.Flow
import java.util.jar.Manifest
import kotlin.concurrent.thread
import kotlin.math.round

@Composable
fun TitleSection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value
    Row(modifier = modifier
        .fillMaxSize()
        .background(Color.DarkGray)
        .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Salmon")
        }
        Row(
            Modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AUTO")
            Switch(
                state.mode == TunerMode.AUTO,
                {
                    viewModel.toggleMode()
                })
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
    val isNoteCorrect = if (noteIndex >= 0)
        state.isCorrect[noteIndex] else false

    Box(modifier = modifier
        .size(64.dp)
        .padding(4.dp)
        .background(
            if (note != state.selectedNote) Color.Yellow else Color.Magenta,
            CircleShape,
        )
        .border(
            if (isNoteCorrect) 4.dp else 0.dp,
            Color.Green,
            CircleShape
        )
        .clickable {
            viewModel.setSelectedNote(note)
            viewModel.setModeManual()
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(note)
    }
}

@Composable
fun NotesColumn(
    modifier: Modifier = Modifier,
    range: IntRange,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value
    Column(modifier = modifier
        .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        range.forEach {
            rangeIndex ->
                NoteButton(
                    Modifier,
                    state.notes[rangeIndex],
                    viewModel
                )
        }
    }
}

@Composable
fun NoteDisplaySection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    Row(modifier = modifier
        .fillMaxSize()
        .background(Color.DarkGray),
    ) {
        // Notes on the left
        NotesColumn(
            Modifier.weight(2f),
            0..2,
            viewModel
        )

        // Separator for note columns
        Box(Modifier.weight(6f))

        // Notes on the right
        NotesColumn(
            Modifier.weight(2f),
            3..5,
            viewModel
        )
    }
}

var a = 4

@Composable
fun FlowingGrid(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        val gridShiftPx = viewModel.tunerState.value.gridShift.toPx()
        val cellSizePx = TunerConfig.GRID_SIZE_DP.dp.toPx()
        val numOfCellsWidthHalf = (size.width / cellSizePx / 2).toInt()
        val numOfCellsHeightHalf = (size.height / cellSizePx / 2).toInt()
        val centerW = size.width / 2
        val centerH = size.height / 2

        // TODO: Later move to Color object.
        val lineColor = Color.LightGray.copy(alpha = 0.2f)

        // Draw grid vertical lines.
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

        // Draw grid horizontal lines.
        for (i in 0..numOfCellsHeightHalf+1) {
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
    val lastDetectionTime = viewModel.tunerState.value.lastDetectionTime

    BoxWithConstraints(modifier = modifier
        .fillMaxSize()
        .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val boxWidth = maxWidth
        var offsetText = ""

        if (lastDetectionTime != null) {
            offsetText = "${round(state.centsOffset).toInt()}"
        }

        fun convertCentsOffsetToOffsetX(): Dp {
            var centsOffset = state.centsOffset
            if (centsOffset < -100) centsOffset = -100f
            else if (centsOffset > 100) centsOffset = 100f
            return (boxWidth - 40.dp) * centsOffset / 200f
        }

        // center (origin) line
        FlowingGrid(viewModel = viewModel)

        // cursor positioning
        Box(Modifier
            .absoluteOffset(
                if (lastDetectionTime != null) convertCentsOffsetToOffsetX() else 0.dp
            ),
        ) {
            Box(Modifier
                .size(40.dp)
                .background(Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(offsetText)
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
        .fillMaxSize()
        .background(Color.DarkGray),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Button({
            viewModel.restoreDefaults()
        }) {
            Text("Start Over")
        }
    }
}

@Composable
fun TunerScreen(viewModel: TunerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // title section
        TitleSection(
            modifier = Modifier.weight(1f),
            viewModel
        )

        // Tuning slider
        TuningSliderSection(
            modifier = Modifier.weight(3f),
            viewModel
        )

        // note display section
        NoteDisplaySection(
            modifier = Modifier.weight(5f),
            viewModel
        )

        // Start over button. Maybe settings?
        FooterSection(
            modifier = Modifier.weight(1f),
            viewModel
        )
    }
}