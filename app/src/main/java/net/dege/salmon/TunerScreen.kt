package net.dege.salmon

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import net.dege.salmon.ui.theme.*
import kotlin.math.abs
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleSection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Salmon",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = state.mode == TunerMode.AUTO,
                    onClick = {
                        if (state.mode != TunerMode.AUTO) viewModel.toggleMode()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Auto", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    selected = state.mode == TunerMode.MANUAL,
                    onClick = {
                        if (state.mode != TunerMode.MANUAL) viewModel.toggleMode()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Manual", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun StringPill(
    modifier: Modifier = Modifier,
    note: String,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value
    val noteIndex = state.notes.indexOf(note)
    val isSelected = note == state.selectedNote
    val isNoteCorrect = if (noteIndex >= 0) state.isCorrect[noteIndex] else false
    val hasDetection = state.lastDetectionTime != null
    val isActiveDetection = isSelected && hasDetection
    val cents = state.centsOffset

    val bgColor by animateColorAsState(
        targetValue = when {
            isNoteCorrect -> TunePerfect.copy(alpha = 0.15f)
            isActiveDetection && abs(cents) > 20f -> TuneOff.copy(alpha = 0.15f)
            isActiveDetection && abs(cents) > 5f -> TuneClose.copy(alpha = 0.15f)
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(300),
        label = "pillBg"
    )

    val accentColor = when {
        isNoteCorrect -> TunePerfect
        isActiveDetection && abs(cents) > 20f -> TuneOff
        isActiveDetection && abs(cents) > 5f -> TuneClose
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    val textColor by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(300),
        label = "pillText"
    )

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable {
                viewModel.setSelectedNote(note)
                viewModel.setModeManual()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = note.filter { !it.isDigit() },
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )

        if (isActiveDetection) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun NoteDisplaySection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StringPill(note = state.notes[0], viewModel = viewModel)
                StringPill(note = state.notes[1], viewModel = viewModel)
                StringPill(note = state.notes[2], viewModel = viewModel)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StringPill(note = state.notes[3], viewModel = viewModel)
                StringPill(note = state.notes[4], viewModel = viewModel)
                StringPill(note = state.notes[5], viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FlowingGrid(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val lineColor = remember { onSurface.copy(alpha = 0.04f) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val gridShiftPx = viewModel.tunerState.value.gridShift.toPx()
        val cellSizePx = TunerConfig.GRID_SIZE_DP.dp.toPx()
        val numOfCellsWidthHalf = (size.width / cellSizePx / 2).toInt()
        val numOfCellsHeightHalf = (size.height / cellSizePx / 2).toInt()
        val centerW = size.width / 2
        val centerH = size.height / 2

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
fun TuningSection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value
    val hasDetection = state.lastDetectionTime != null
    val cents = if (hasDetection) state.centsOffset.coerceIn(-100f, 100f) else 0f
    val noteName = state.selectedNote ?: ""

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        val gaugeWidth = maxWidth - 80.dp

        val cursorFraction by animateFloatAsState(
            targetValue = if (hasDetection) cents / 200f else 0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
            label = "cursorOffset"
        )

        val cursorColor by animateColorAsState(
            targetValue = when {
                !hasDetection -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                abs(cents) <= 5f -> TunePerfect
                abs(cents) <= 20f -> TuneClose
                else -> TuneOff
            },
            animationSpec = tween(200),
            label = "cursorColor"
        )

        val centerMarkerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

        FlowingGrid(viewModel = viewModel)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(gaugeWidth + 40.dp)
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val trackY = size.height / 2
                    val trackStart = 20.dp.toPx()
                    val trackEnd = size.width - 20.dp.toPx()
                    val trackMid = trackStart + (trackEnd - trackStart) / 2f
                    val trackLength = trackEnd - trackStart
                    val cursorX = trackMid + cursorFraction * trackLength

                    drawLine(
                        color = GaugeTrack,
                        start = Offset(trackStart, trackY),
                        end = Offset(trackEnd, trackY),
                        strokeWidth = 3.dp.toPx()
                    )

                    if (hasDetection) {
                        drawLine(
                            color = cursorColor,
                            start = Offset(trackMid, trackY),
                            end = Offset(cursorX, trackY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    drawLine(
                        color = centerMarkerColor,
                        start = Offset(trackMid, trackY - 8.dp.toPx()),
                        end = Offset(trackMid, trackY + 8.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawCircle(
                        color = cursorColor,
                        radius = 7.dp.toPx(),
                        center = Offset(cursorX, trackY)
                    )

                    drawCircle(
                        color = cursorColor.copy(alpha = 0.2f),
                        radius = 13.dp.toPx(),
                        center = Offset(cursorX, trackY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (hasDetection && noteName.isNotEmpty()) {
                Text(
                    text = noteName,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            if (hasDetection) {
                val centsText = "${round(cents).toInt()}"
                val centsLabelColor = when {
                    abs(cents) <= 5f -> TunePerfect
                    abs(cents) <= 20f -> TuneClose
                    else -> TuneOff
                }
                Text(
                    text = "${if (cents > 0) "+" else ""}$centsText\u00A2",
                    style = MaterialTheme.typography.titleSmall,
                    color = centsLabelColor
                )
            } else {
                Text(
                    text = "—\u00A2",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun FooterSection(
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel
) {
    val state = viewModel.tunerState.value

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusText = when {
                state.mode == TunerMode.AUTO -> "Auto-detecting"
                state.selectedNote != null -> "Tuning ${state.selectedNote}"
                else -> "Select a string"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = { viewModel.restoreDefaults() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Start Over",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun TunerScreen(viewModel: TunerViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TitleSection(
                modifier = Modifier.weight(1f),
                viewModel = viewModel
            )

            TuningSection(
                modifier = Modifier.weight(3.5f),
                viewModel = viewModel
            )

            NoteDisplaySection(
                modifier = Modifier.weight(4f),
                viewModel = viewModel
            )

            FooterSection(
                modifier = Modifier.weight(1f),
                viewModel = viewModel
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Image(
                painter = painterResource(R.drawable.guitar_headstock_demo),
                contentDescription = null,
                modifier = Modifier.alpha(0.3f)
            )
        }
    }
}
