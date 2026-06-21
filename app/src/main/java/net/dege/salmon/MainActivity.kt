package net.dege.salmon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.dege.salmon.ui.theme.SalmonTheme
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val viewModel: TunerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SalmonTheme {
                TunerScreen(viewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    val viewModel = TunerViewModel()
    SalmonTheme {
        TunerScreen(viewModel)
    }
}