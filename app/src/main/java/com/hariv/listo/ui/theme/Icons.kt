package com.hariv.listo.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Thin line icons drawn on a 24-unit grid (shapes adapted from the Lucide set, ISC licence). */
object ListoIcons {
    private fun line(name: String, vararg paths: String, width: Float = 1.75f): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { d ->
                    addPath(
                        pathData = addPathNodes(d),
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = width,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
            .build()

    val Tasks: ImageVector by lazy { line("tasks", "m3 17 2 2 4-4", "m3 7 2 2 4-4", "M13 6h8", "M13 12h8", "M13 18h8") }
    val Repeat: ImageVector by lazy { line("repeat", "m17 2 4 4-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "m7 22-4-4 4-4", "M21 13v1a4 4 0 0 1-4 4H3") }
    val Archive: ImageVector by lazy {
        line("archive", "M3 3h18a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z", "M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8", "M10 12h4")
    }
    val Settings: ImageVector by lazy {
        line("settings", "M20 7h-9", "M14 17H5", "M17 14a3 3 0 1 0 0 6a3 3 0 1 0 0-6z", "M7 4a3 3 0 1 0 0 6a3 3 0 1 0 0-6z")
    }
    val Plus: ImageVector by lazy { line("plus", "M12 5v14", "M5 12h14", width = 2f) }
    val ArrowUp: ImageVector by lazy { line("arrow-up", "m5 12 7-7 7 7", "M12 19V5", width = 2f) }
    val Close: ImageVector by lazy { line("close", "M18 6 6 18", "M6 6l12 12") }
    val Back: ImageVector by lazy { line("back", "m12 19-7-7 7-7", "M19 12H5") }
    val ChevronDown: ImageVector by lazy { line("chevron-down", "m6 9 6 6 6-6") }
    val ChevronUp: ImageVector by lazy { line("chevron-up", "m18 15-6-6-6 6") }
    val ChevronRight: ImageVector by lazy { line("chevron-right", "m9 18 6-6-6-6") }
    val Check: ImageVector by lazy { line("check", "M20 6 9 17l-5-5", width = 2.5f) }
    val Pencil: ImageVector by lazy { line("pencil", "M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z") }
    val Trash: ImageVector by lazy {
        line("trash", "M3 6h18", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2")
    }
    val More: ImageVector by lazy { line("more", "M12 5h.01", "M12 12h.01", "M12 19h.01", width = 3f) }
    val Help: ImageVector by lazy {
        line("help", "M12 22a10 10 0 1 0 0-20a10 10 0 1 0 0 20z", "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.01")
    }
    val Folder: ImageVector by lazy {
        line("folder", "M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z")
    }
    val Save: ImageVector by lazy { line("save", "M12 3v12", "m7 10 5 5 5-5", "M5 21h14") }
    val Restore: ImageVector by lazy { line("restore", "M12 21V9", "m7 14 5-5 5 5", "M5 3h14") }
    val Battery: ImageVector by lazy {
        line("battery", "M4 7h13a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z", "M22 11v2", "M6 10v4")
    }
    val Widget: ImageVector by lazy {
        line("widget", "M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z",
            "M15 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z",
            "M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z",
            "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z")
    }
    val Update: ImageVector by lazy { line("update", "M21 12a9 9 0 1 1-3-6.7L21 8", "M21 3v5h-5") }
    val Calendar: ImageVector by lazy {
        line("calendar", "M8 2v4", "M16 2v4", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M3 10h18")
    }
    val Pause: ImageVector by lazy { line("pause", "M9 5v14", "M15 5v14", width = 2f) }
    val Play: ImageVector by lazy { line("play", "m7 4 13 8-13 8z") }
}
