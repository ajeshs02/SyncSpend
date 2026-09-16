package com.ajesh.syncspend.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * All ~35 line icons ported verbatim (same SVG path data, same 24x24
 * viewBox, same 1.7 stroke width, round caps/joins, no fill) from the
 * approved design (SyncSpendPhone.dc.html's `P` icon table). Stroke color is
 * a placeholder `Color.Black` here — every call site tints via
 * `Icon(tint = ...)` / `LocalContentColor`, so this value is never seen.
 *
 * Not reproduced: the design's occasional low-opacity *filled* variant of an
 * icon (used only for the active bottom-nav glyph, e.g. `fillOpacity: .32`)
 * — a cosmetic nuance on open stroke paths that doesn't fill cleanly without
 * a second hand-authored closed-path per icon. Judgment call: skipped as not
 * worth doubling this file's size for a barely-visible halo; the active nav
 * icon is still fully readable via its stroke tint alone.
 */
object SyncSpendIcons {

    private fun circle(cx: Double, cy: Double, r: Double): String {
        val d = 2 * r
        return "M${cx - r},$cy a$r,$r 0 1,0 $d,0 a$r,$r 0 1,0 ${-d},0"
    }

    private fun build(name: String, vararg d: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            d.forEach { segment ->
                addPath(
                    pathData = addPathNodes(segment),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.7f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Coffee = build(
        "coffee",
        "M17 8h1a4 4 0 0 1 0 8h-1",
        "M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V8Z",
        "M7 2v2.5", "M11 2v2.5", "M15 2v2.5",
    )
    val Cart = build(
        "cart",
        "M4 5h2l2.2 9.5h9.2L19 8H6.6",
        circle(9.5, 19.0, 1.5), circle(17.0, 19.0, 1.5),
    )
    val Car = build(
        "car",
        "M4.5 13.5 6.4 8.8A2 2 0 0 1 8.3 7.5h7.4a2 2 0 0 1 1.9 1.3l1.9 4.7",
        "M3.5 13.5h17V18h-17z",
        circle(7.5, 18.5, 1.3), circle(16.5, 18.5, 1.3),
    )
    val Bag = build(
        "bag",
        "M6.5 7.5h11L18.5 20h-13z",
        "M9.5 7.5a2.5 2.5 0 0 1 5 0",
    )
    val Brief = build(
        "brief",
        "M3.5 8.5h17V19h-17z",
        "M9 8.5V6.5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2",
    )
    val Home = build("home", "M4.5 11 12 5.2 19.5 11v7.5a1 1 0 0 1-1 1h-13a1 1 0 0 1-1-1z")
    val Swap = build("swap", "M4.5 9h14l-3.2-3.2", "M19.5 15h-14l3.2 3.2")
    val Layers = build("layers", "M12 3.5 3.5 8 12 12.5 20.5 8z", "M3.5 13 12 17.5 20.5 13")
    val Gear = build(
        "gear",
        circle(12.0, 12.0, 3.0),
        "M12 3.5v2.2M12 18.3v2.2M4.9 7.7l1.9 1.1M17.2 15.2l1.9 1.1M4.9 16.3l1.9-1.1M17.2 8.8l1.9-1.1",
    )
    val Cog = build(
        "cog",
        circle(12.0, 12.0, 3.1),
        "M19.1 14.6a1.4 1.4 0 0 0 .28 1.55l.05.05a1.7 1.7 0 1 1-2.4 2.4l-.05-.05a1.4 1.4 0 0 0-2.38 1v.14a1.7 1.7 0 0 1-3.4 0v-.07a1.4 1.4 0 0 0-2.38-1l-.05.05a1.7 1.7 0 1 1-2.4-2.4l.05-.05a1.4 1.4 0 0 0-1-2.38H5.3a1.7 1.7 0 0 1 0-3.4h.14a1.4 1.4 0 0 0 1-2.38l-.05-.05a1.7 1.7 0 1 1 2.4-2.4l.05.05a1.4 1.4 0 0 0 2.38-1V4.2a1.7 1.7 0 0 1 3.4 0v.07a1.4 1.4 0 0 0 2.38 1l.05-.05a1.7 1.7 0 1 1 2.4 2.4l-.05.05a1.4 1.4 0 0 0-.28 1.55",
    )
    val Cal = build(
        "cal",
        "M4.5 6.5h15v13h-15z", "M4.5 10.5h15", "M8.5 4v3", "M15.5 4v3",
    )
    val Grid4 = build(
        "grid4",
        "M4.6 4.6h5.6v5.6H4.6z", "M13.8 4.6h5.6v5.6h-5.6z",
        "M4.6 13.8h5.6v5.6H4.6z", "M13.8 13.8h5.6v5.6h-5.6z",
    )
    val Receipt = build(
        "receipt",
        "M6.5 3.5h11v17l-2.75-1.8-2.75 1.8-2.75-1.8L6.5 20.5z",
        "M9.5 8.5h5", "M9.5 12.5h5",
    )
    val Bell = build(
        "bell",
        "M6.5 10a5.5 5.5 0 0 1 11 0c0 4.5 1.8 5.5 1.8 5.5H4.7S6.5 14.5 6.5 10Z",
        "M10 18.5a2 2 0 0 0 4 0",
    )
    val Repeat = build(
        "repeat",
        "M4.5 9.5A4 4 0 0 1 8.5 6h10l-2.8-2.6",
        "M19.5 14.5a4 4 0 0 1-4 3.5h-10l2.8 2.6",
    )
    val Card = build("card", "M2.5 6.5h19v11h-19z", "M2.5 10.5h19")
    val Heart = build("heart", "M12 19.5S4.5 15 4.5 10.2A3.7 3.7 0 0 1 12 8a3.7 3.7 0 0 1 7.5 2.2C19.5 15 12 19.5 12 19.5Z")
    val House = build(
        "house",
        "M4.5 11 12 5.2 19.5 11v7.5a1 1 0 0 1-1 1h-13a1 1 0 0 1-1-1z",
        "M10 19.5v-5h4v5",
    )
    val Plane = build("plane", "M3 13.5l18-6-7 12-2.6-4.4z")
    val Bolt = build("bolt", "M13.5 3 6 13.5h5l-1 7.5 8-11h-5z")
    val Plus = build("plus", "M12 5.5v13", "M5.5 12h13")
    val Back = build("back", "M14.5 6.5 9 12l5.5 5.5")
    val Prev = build("prev", "M13.5 7.5 9.5 12l4 4.5")
    val Next = build("next", "M10.5 7.5l4 4.5-4 4.5")
    val Up = build("up", "M12 18.5v-13", "M6.5 11 12 5.5 17.5 11")
    val Down = build("down", "M12 5.5v13", "M6.5 13 12 18.5 17.5 13")
    val Trash = build("trash", "M4.5 7h15", "M9.5 7V5h5v2", "M6.5 7l1 12.5h9L18 7")
    val Pencil = build("pencil", "M4.5 19.5h4L19 9l-4-4L4.5 15.5z", "M14.5 5.5 18.5 9.5")
    val Close = build("close", "M6.5 6.5l11 11", "M17.5 6.5l-11 11")
    val Clock = build("clock", circle(12.0, 12.0, 8.0), "M12 7.5V12l3.2 2")
    val Download = build("download", "M12 4.5v10", "M7.5 10.5 12 15l4.5-4.5", "M4.5 19h15")
    val Spark = build("spark", "M12 3.5l1.9 5.3 5.3 1.9-5.3 1.9L12 18l-1.9-5.4-5.3-1.9 5.3-1.9z")
    val ArrowOut = build("arrowOut", "M8.5 15.5 15.5 8.5", "M9.6 8.5h5.9v5.9")
    val ArrowIn = build("arrowIn", "M15.5 8.5 8.5 15.5", "M14.4 15.5H8.5V9.6")
    val Backspace = build(
        "backspace",
        "M9.2 5.5h10.3a1.5 1.5 0 0 1 1.5 1.5v10a1.5 1.5 0 0 1-1.5 1.5H9.2L3.2 12z",
        "M12 9.6l4.4 4.8", "M16.4 9.6 12 14.4",
    )

    /** All icons keyed the same way Room stores `iconKey`. */
    val byKey: Map<String, ImageVector> = mapOf(
        "coffee" to Coffee, "cart" to Cart, "car" to Car, "bag" to Bag, "brief" to Brief,
        "home" to Home, "swap" to Swap, "layers" to Layers, "gear" to Gear, "cog" to Cog,
        "cal" to Cal, "grid4" to Grid4, "receipt" to Receipt, "bell" to Bell, "repeat" to Repeat,
        "card" to Card, "heart" to Heart, "house" to House, "plane" to Plane, "bolt" to Bolt,
        "plus" to Plus, "back" to Back, "prev" to Prev, "next" to Next, "up" to Up, "down" to Down,
        "trash" to Trash, "pencil" to Pencil, "close" to Close, "clock" to Clock,
        "download" to Download, "spark" to Spark, "arrowOut" to ArrowOut, "arrowIn" to ArrowIn,
        "backspace" to Backspace,
    )

    /** Curated icon-picker subset — matches the design's `CYCLE` list exactly. */
    val cyclePickerKeys: List<String> = listOf(
        "coffee", "cart", "car", "bag", "house", "plane", "heart", "bolt", "brief", "card", "receipt", "spark",
    )

    fun iconFor(key: String): ImageVector = byKey[key] ?: Spark
}
