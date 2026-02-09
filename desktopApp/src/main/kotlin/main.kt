@file:Suppress("ktlint:standard:filename")

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.group8.comp2300.App
import java.awt.Dimension

private const val MinWindowWidth = 350
private const val MinWindowHeight = 600

fun main() {
    application {
        Window(
            title = "Comp2300",
            state = rememberWindowState(width = 800.dp, height = 600.dp),
            onCloseRequest = ::exitApplication
        ) {
            window.minimumSize = Dimension(MinWindowWidth, MinWindowHeight)
            App()
        }
    }
}
