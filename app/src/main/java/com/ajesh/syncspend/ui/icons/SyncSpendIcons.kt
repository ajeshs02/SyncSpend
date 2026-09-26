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
    /** Settings gear — closed 6-tooth outline + hub (the earlier hand-simplified path left a gap on its right side). */
    val Cog = build(
        "cog",
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
        circle(12.0, 12.0, 3.0),
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
    val Upload = build("upload", "M12 15.5v-10", "M7.5 9.5 12 5l4.5 4.5", "M4.5 19h15")
    val Spark = build("spark", "M12 3.5l1.9 5.3 5.3 1.9-5.3 1.9L12 18l-1.9-5.4-5.3-1.9 5.3-1.9z")
    val ArrowOut = build("arrowOut", "M8.5 15.5 15.5 8.5", "M9.6 8.5h5.9v5.9")
    val ArrowIn = build("arrowIn", "M15.5 8.5 8.5 15.5", "M14.4 15.5H8.5V9.6")
    val Backspace = build(
        "backspace",
        "M9.2 5.5h10.3a1.5 1.5 0 0 1 1.5 1.5v10a1.5 1.5 0 0 1-1.5 1.5H9.2L3.2 12z",
        "M12 9.6l4.4 4.8", "M16.4 9.6 12 14.4",
    )


    // ---- Extra presets (authored in the design's 24x24 / 1.7 stroke style) ----
    val Wallet = build(
        "wallet",
        "M4.5 7.5h14a1.5 1.5 0 0 1 1.5 1.5v8.5a1.5 1.5 0 0 1-1.5 1.5h-13a1.5 1.5 0 0 1-1.5-1.5V6.5A1.5 1.5 0 0 1 4.5 5H16",
        "M15.5 13.5h4.5",
        circle(16.6, 13.5, 0.5),
    )
    val Gift = build(
        "gift",
        "M4.5 11h15v8.5h-15z", "M3.5 7.5h17V11h-17z", "M12 7.5v12",
        "M12 7.5C10 7.5 8.5 6.8 8.5 5.6S10.6 4 12 7.5Z", "M12 7.5c2 0 3.5-.7 3.5-1.9S13.4 4 12 7.5Z",
    )
    val Film = build(
        "film",
        "M4.5 5.5h15v13h-15z", "M8 5.5v13", "M16 5.5v13",
        "M4.5 9H8", "M4.5 12H8", "M4.5 15H8", "M16 9h3.5", "M16 12h3.5", "M16 15h3.5",
    )
    val Music = build(
        "music",
        "M9 17.5v-11l10-2v11", circle(7.0, 17.5, 2.0), circle(17.0, 15.5, 2.0),
    )
    val Utensils = build(
        "utensils",
        "M6 3.5v5.5a2.2 2.2 0 0 0 4.4 0V3.5", "M8.2 3.5v17",
        "M16.5 20.5v-17c-2 1-3 3.5-3 6.5v2.5h3",
    )
    val Phone = build(
        "phone",
        "M8 3.5h8a1.5 1.5 0 0 1 1.5 1.5v14a1.5 1.5 0 0 1-1.5 1.5H8A1.5 1.5 0 0 1 6.5 19V5A1.5 1.5 0 0 1 8 3.5Z",
        "M10.5 18h3",
    )
    val Wifi = build(
        "wifi",
        "M3.5 9.5a12 12 0 0 1 17 0", "M6.5 12.8a7.8 7.8 0 0 1 11 0", "M9.5 16a3.5 3.5 0 0 1 5 0",
        circle(12.0, 19.0, 0.5),
    )
    val Pill = build(
        "pill",
        "M8.5 15.5l7-7a3.2 3.2 0 0 1 4.5 4.5l-7 7a3.2 3.2 0 0 1-4.5-4.5Z", "M11.7 10.3l4 4",
    )
    val Dumbbell = build(
        "dumbbell",
        "M6.5 8.5v7", "M17.5 8.5v7", "M3.5 10.5v3", "M20.5 10.5v3", "M6.5 12h11",
    )
    val Bus = build(
        "bus",
        "M6.5 4.5h11a1 1 0 0 1 1 1V17h-13V5.5a1 1 0 0 1 1-1Z", "M5.5 11h13", "M5.5 17v2", "M18.5 17v2",
        circle(8.5, 14.2, 0.4), circle(15.5, 14.2, 0.4),
    )
    val Fuel = build(
        "fuel",
        "M6 20.5V5a1.5 1.5 0 0 1 1.5-1.5h5A1.5 1.5 0 0 1 14 5v15.5", "M4.5 20.5h11", "M6 9h8",
        "M14 9.5h1.5a1.5 1.5 0 0 1 1.5 1.5v5a1.2 1.2 0 0 0 2.4 0V8.5L17 6",
    )
    val Book = build(
        "book",
        "M5 4.5h11a2 2 0 0 1 2 2V19.5H7a2 2 0 0 1-2-2V4.5Z", "M5 17.5a2 2 0 0 1 2-2h11",
    )
    val Coin = build(
        "coin",
        circle(12.0, 12.0, 8.0),
        "M14.5 9.5a2.6 2.6 0 0 0-2.5-1.5c-1.4 0-2.5.8-2.5 1.8s1 1.5 2.5 1.8 2.5.8 2.5 1.8-1.1 1.8-2.5 1.8a2.6 2.6 0 0 1-2.5-1.5",
        "M12 6.8V8", "M12 15.6v1.2",
    )
    val Shirt = build(
        "shirt",
        "M8.5 4l-5 3.5 2 3 2-1V20h9V9.5l2 1 2-3-5-3.5c-.5 1.3-1.8 2-3.5 2S9 5.3 8.5 4Z",
    )
    val Gamepad = build(
        "gamepad",
        "M7 8.5h10a4 4 0 0 1 4 4v1.5a3 3 0 0 1-5.2 2L14.5 14.5h-5L8.2 16A3 3 0 0 1 3 14v-1.5a4 4 0 0 1 4-4Z",
        "M8 11v3", "M6.5 12.5h3", circle(15.8, 11.8, 0.4), circle(17.6, 13.2, 0.4),
    )
    val Trend = build(
        "trend",
        "M4 19.5h16", "M6.5 16l4-5 3 3 5-6.5", "M15.5 7.5H19V11",
    )
    val School = build(
        "school",
        "M12 5 2.5 9.5 12 14l9.5-4.5L12 5Z", "M6.5 11.8V16c0 1.2 2.5 2.5 5.5 2.5s5.5-1.3 5.5-2.5v-4.2", "M21.5 9.5V15",
    )
    val Shield = build(
        "shield",
        "M12 3.5l7 2.5v5.5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-2.5Z", "M9 12l2.2 2.2L15.5 10",
    )
    val Tag = build(
        "tag",
        "M4 4.5h7.5l8.5 8.5-7.5 7.5L4 12V4.5Z", circle(8.0, 8.5, 1.2),
    )
    val Bank = build(
        "bank",
        "M3.5 9.5 12 4.5l8.5 5z", "M5.5 10v8", "M9.5 10v8", "M14.5 10v8", "M18.5 10v8", "M3.5 19.5h17",
    )

    /** All icons keyed the same way Room stores `iconKey`. */
    val byKey: Map<String, ImageVector> = mapOf(
        "coffee" to Coffee, "cart" to Cart, "car" to Car, "bag" to Bag, "brief" to Brief,
        "home" to Home, "swap" to Swap, "layers" to Layers, "gear" to Gear, "cog" to Cog,
        "cal" to Cal, "grid4" to Grid4, "receipt" to Receipt, "bell" to Bell, "repeat" to Repeat,
        "card" to Card, "heart" to Heart, "house" to House, "plane" to Plane, "bolt" to Bolt,
        "plus" to Plus, "back" to Back, "prev" to Prev, "next" to Next, "up" to Up, "down" to Down,
        "trash" to Trash, "pencil" to Pencil, "close" to Close, "clock" to Clock,
        "download" to Download, "upload" to Upload, "spark" to Spark, "arrowOut" to ArrowOut, "arrowIn" to ArrowIn,
        "backspace" to Backspace,
        "wallet" to Wallet, "gift" to Gift, "film" to Film, "music" to Music, "utensils" to Utensils,
        "phone" to Phone, "wifi" to Wifi, "pill" to Pill, "dumbbell" to Dumbbell, "bus" to Bus,
        "fuel" to Fuel, "book" to Book, "coin" to Coin, "shirt" to Shirt, "gamepad" to Gamepad,
        "trend" to Trend, "school" to School, "shield" to Shield, "tag" to Tag, "bank" to Bank,
    )

    /** The design's original 12-icon `CYCLE` picker set. */
    val cyclePickerKeys: List<String> = listOf(
        "coffee", "cart", "car", "bag", "house", "plane", "heart", "bolt", "brief", "card", "receipt", "spark",
    )

    /** Preset icons offered on every "+ Add" flow: the design's 12 plus 24 more (6 columns x 6 rows). */
    val pickerKeys: List<String> = listOf(
        "coffee", "utensils", "cart", "bag", "shirt", "car",
        "bus", "fuel", "plane", "house", "bolt", "wifi",
        "phone", "film", "music", "gamepad", "book", "school",
        "heart", "pill", "dumbbell", "gift", "wallet", "coin",
        "card", "bank", "brief", "trend", "receipt", "shield",
        "tag", "bell", "repeat", "clock", "cal", "spark",
    )

    fun iconFor(key: String): ImageVector = byKey[key] ?: Spark
}
