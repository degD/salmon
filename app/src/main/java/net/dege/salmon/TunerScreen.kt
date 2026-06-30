package net.dege.salmon

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
import kotlin.math.round

@Composable
fun TitleSection(
    modifier: Modifier = Modifier,
    onClickModeSwitch: () -> Unit
) {
    var checked by remember { mutableStateOf(true) }

    Row(modifier = modifier
        .fillMaxSize()
        .background(Color.Red),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("AUTO")
        Switch(
            checked,
            {
                checked = it
                onClickModeSwitch()
            })
    }
}

@Composable
fun NoteButton(
    modifier: Modifier = Modifier,
    note: String,
    isNoteCorrect: Boolean,
    onClick: (String) -> Unit
) {
    Box(modifier = modifier
        .size(64.dp)
        .padding(4.dp)
        .background(Color.Yellow, CircleShape)
        .border(
            if (isNoteCorrect) 2.dp else 0.dp,
            Color.Magenta,
        )
        .clickable { onClick(note) },
        contentAlignment = Alignment.Center,
    ) {
        Text(note)
    }
}

@Composable
fun NotesColumn(
    modifier: Modifier = Modifier,
    notesSlice: List<String>,
    isCorrectSlice: List<Boolean>,
    onClickNoteButton: (String) -> Unit
) {
    Column(modifier = modifier
        .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        NoteButton(
            Modifier,
            notesSlice[0],
            isCorrectSlice[0],
            onClickNoteButton
        )
        NoteButton(
            Modifier,
            notesSlice[1],
            isCorrectSlice[1],
            onClickNoteButton
        )
        NoteButton(
            Modifier,
            notesSlice[2],
            isCorrectSlice[2],
            onClickNoteButton
        )
    }
}

@Composable
fun NoteDisplaySection(
    modifier: Modifier = Modifier,
    notes: List<String>,
    isCorrect: List<Boolean>,
    onClickNoteButton: (String) -> Unit
) {
    Row(modifier = modifier
        .fillMaxSize()
        .background(Color.Red),
    ) {
        // Notes on the left
        NotesColumn(
            Modifier.weight(2f),
            notes.slice(0..2),
            isCorrect.slice((0..2)),
            onClickNoteButton
        )

        // Separator for note columns
        Box(Modifier.weight(6f))

        // Notes on the right
        NotesColumn(
            Modifier.weight(2f),
            notes.slice(3..5),
            isCorrect.slice(3..5),
            onClickNoteButton
        )
    }
}

@Composable
fun TuningSliderSection(
    modifier: Modifier = Modifier,
    centsOffset: Float
) {
    BoxWithConstraints(modifier = modifier
        .fillMaxSize()
        .background(Color.Green),
        contentAlignment = Alignment.Center,
    ) {
        val boxWidth = maxWidth
        val offsetText = "${round(centsOffset).toInt()}"

        fun convertCentsOffsetToOffsetX(): Dp {
            var centsOffset = centsOffset
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
                convertCentsOffsetToOffsetX()
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
fun TunerScreen(viewModel: TunerViewModel) {
    val state = viewModel.tunerState.value

    fun onClickNoteButton(note: String) {
        println("Note changed from ${state.selectedNote} to $note")
        viewModel.setSelectedNote(note)
        // TODO: Highlight note button etc.
    }

    fun onClickModeSwitch() {
        viewModel.toggleMode()
        println("Mode changed to ${state.mode}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // title section
        TitleSection(
            modifier = Modifier.weight(1f),
            ::onClickModeSwitch
        )

        // Tuning slider
        TuningSliderSection(
            modifier = Modifier.weight(3f),
            state.centsOffset
        )

        // note display section
        NoteDisplaySection(
            modifier = Modifier.weight(5f),
            state.notes,
            state.isCorrect,
            ::onClickNoteButton
        )

        // Start over button. Maybe settings?
        Column(modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Button({}) {
                Text("Start Over")
            }
        }
    }
}