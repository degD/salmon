package net.dege.salmon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        // Toggle switch section
        Row(modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .background(Color.Red)
        ) {
            Text("AUTO")
            Switch(true, {})
        }

        // Tuning slider
        Box(modifier = Modifier
            .fillMaxSize()
            .weight(3f)
            .background(Color.Green)
        ) { }

        // Note displays section
        Row(modifier = Modifier
            .fillMaxSize()
            .weight(5f)
            .background(Color.Blue)
        ) {
            // Notes on the left
            Column(modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color.Red)
            ) {
                Box(modifier = Modifier
                    .width(32.dp).height(32.dp)
                    .background(Color.Yellow)
                ) {
                    Text("D")
                }
                Box(modifier = Modifier
                    .width(32.dp).height(32.dp)
                    .background(Color.Yellow)
                ) {
                    Text("A")
                }
                Box(modifier = Modifier
                    .width(32.dp).height(32.dp)
                    .background(Color.Yellow)
                ) {
                    Text("E")
                }
            }

            // Image of guitar head
            Column(modifier = Modifier
                .fillMaxSize()
                .weight(8f)
                .background(Color.Red)
            ) {

            }

            // Notes on the right
            Column(modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color.Red)
            ) {
                Box(modifier = Modifier
                    .width(32.dp).height(32.dp)
                    .background(Color.Yellow)
                ) {
                    Text("G")
                }
                Box(modifier = Modifier
                    .width(32.dp).height(32.dp)
                    .background(Color.Yellow)
                ) {
                    Text("B")
                }
                Box(modifier = Modifier
                    .width(32.dp).height(32.dp)
                    .background(Color.Yellow)
                ) {
                    Text("E")
                }
            }
        }

        // Start over button. Maybe settings?
        Box(modifier = Modifier.fillMaxSize().weight(1f).background(Color.Black)) { }
    }
}