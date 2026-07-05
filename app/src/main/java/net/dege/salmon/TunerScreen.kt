package net.dege.salmon

import android.content.pm.PackageManager
import android.widget.Switch
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.checkSelfPermission
import java.util.jar.Manifest
import kotlin.math.round

@Composable
fun TitleSection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value
    Row(modifier = modifier
        .fillMaxSize()
        .background(Color.DarkGray),
        horizontalArrangement = Arrangement.End,
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
            CircleShape
        )
        .border(
            if (isNoteCorrect) 2.dp else 0.dp,
            Color.White,
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
        VerticalDivider(
            Modifier.background(Color.Yellow),
            2.dp
        )

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