package net.dege.salmon

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TunerScreen(viewModel: TunerViewModel) {
    val state = viewModel.tunerState.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = state.incomingFrequency.toString(), fontSize = 72.sp)
        Text(text = "${String.format("%.2f", state.incomingFrequency)} Hz", fontSize = 24.sp)

        Spacer(modifier = Modifier.height(32.dp))

        // A simple linear indicator for cents offset
        Text(text = "Cents: ${state.incomingFrequency}")
        Slider(
            value = state.incomingFrequency,
            onValueChange = {},
            valueRange = -50f..50f,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}