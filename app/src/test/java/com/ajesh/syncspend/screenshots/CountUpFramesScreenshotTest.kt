package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.ui.components.CountUp
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Eyeballing aid: the Home total at successive moments of the count-up. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class CountUpFramesScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun frames() {
        val final = "1,08,699.50".replace("1,08,", "108,") // en-US grouping, like CurrencyFormatter
        rule.snapshot("countup_frames", dark = true, heightDp = 560) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0f, 150f, 350f, 600f, 900f, 1300f, 1800f, 2200f, 2400f).forEach { t ->
                    Row(Modifier.background(SyncSpendTheme.colors.darkGradient, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("${t.toInt()}ms", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(end = 14.dp, top = 12.dp))
                        Text("₹ ", fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                        Text(
                            buildAnnotatedString {
                                CountUp.frame(final, t).forEach { c ->
                                    if (c.visible) append(c.char) else withStyle(SpanStyle(color = Color.Transparent)) { append(c.char) }
                                }
                            },
                            color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Medium,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                        )
                    }
                }
            }
        }
    }
}
