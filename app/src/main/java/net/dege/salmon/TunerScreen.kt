package net.dege.salmon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun NoteButton(
    modifier: Modifier = Modifier,
    note: String,
    freq: Float
) {
    Box(modifier = modifier
        .size(64.dp)
        .padding(4.dp)
        .background(Color.Yellow, CircleShape)
        .clickable {},
        contentAlignment = Alignment.Center,
    ) {
        Text(note)
    }
}

@Composable
fun NotesColumn(
    modifier: Modifier = Modifier,
    tableOfNotesSlice: List<Pair<String, Float>>
) {
    Column(modifier = modifier
        .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        NoteButton(
            Modifier,
            tableOfNotesSlice[0].first,
            tableOfNotesSlice[0].second
        )
        NoteButton(
            Modifier,
            tableOfNotesSlice[1].first,
            tableOfNotesSlice[1].second
        )
        NoteButton(
            Modifier,
            tableOfNotesSlice[2].first,
            tableOfNotesSlice[2].second
        )
    }
}

@Composable
fun NoteDisplaySection(
    modifier: Modifier = Modifier,
    tableOfNotes: List<Pair<String, Float>>
) {
    Row(modifier = modifier
        .fillMaxSize()
        .background(Color.Red),
    ) {
        // Notes on the left
        NotesColumn(
            Modifier.weight(2f),
            tableOfNotes.slice(0..2)
        )

        // Separator for note columns
        Box(Modifier.weight(6f))

        // Notes on the right
        NotesColumn(
            Modifier.weight(2f),
            tableOfNotes.slice(3..5)
        )
    }
}

@Composable
fun TuningSliderSection(
    modifier: Modifier = Modifier,
    offset: Float
) {
    Box(modifier = modifier
        .fillMaxSize()
        .background(Color.Green),
        contentAlignment = Alignment.Center,
    ) {
        val offset = offset.toInt()
        var offsetText = "$offset"

        if (offset > 0) {
            offsetText = "+$offsetText"
        }

        // center (origin) line
        VerticalDivider(
            Modifier.background(Color.Yellow),
            2.dp
        )

        // cursor positioning
        Box(Modifier
            .absoluteOffset(offset.dp),
        ) {
            Box(Modifier
                .zIndex(100f)
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

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Toggle switch section
        Row(modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .background(Color.Red),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AUTO")
            Switch(true, {})
        }

        // Tuning slider
        TuningSliderSection(
            modifier = Modifier.weight(3f), -5f)

        // note display section
        NoteDisplaySection(
            modifier = Modifier.weight(5f), tableOfNotes = state.tableOfNotes)

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