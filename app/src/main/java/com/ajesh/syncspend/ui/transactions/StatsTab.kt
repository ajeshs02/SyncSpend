package com.ajesh.syncspend.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/** Stats tab: top-category card, 2x2 stat grid, findings, and the month-over-month card. */
@Composable
fun StatsTab(stats: StatsUi, modifier: Modifier = Modifier) {
    val colors = SyncSpendTheme.colors
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp, bottom = SyncSpendChrome.screenBottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.mintGradient, RoundedCornerShape(22.dp))
                .padding(17.dp),
        ) {
            Text(stats.kicker, fontSize = 11.sp, color = colors.msub)
            Row(
                modifier = Modifier.padding(top = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(SyncSpendIcons.iconFor(stats.topIconKey), null, tint = colors.mink, modifier = Modifier.size(18.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stats.topName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.mink, maxLines = 1)
                    Text(stats.topShareLine, fontSize = 11.sp, color = colors.msub, modifier = Modifier.padding(top = 2.dp))
                }
                Text(stats.topTotal, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.19).sp, color = colors.mink)
            }
        }

        stats.tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { tile ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(colors.card, RoundedCornerShape(18.dp))
                            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                    ) {
                        Text(tile.label, fontSize = 10.5.sp, lineHeight = 13.sp, color = colors.sub, maxLines = 2)
                        Text(
                            tile.value,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.19).sp,
                            color = when (tile.tone) {
                                StatTone.NEUTRAL -> colors.ink
                                StatTone.POSITIVE -> colors.pos
                                StatTone.NEGATIVE -> colors.neg
                            },
                            modifier = Modifier.padding(top = 7.dp),
                        )
                        Text(tile.note, fontSize = 10.sp, color = colors.sub, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.cardGradient, RoundedCornerShape(20.dp))
                .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text("Findings", style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp), color = colors.ink)
            Column(modifier = Modifier.padding(top = 11.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                stats.findings.forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.padding(top = 5.dp).size(6.dp).background(colors.acc, RoundedCornerShape(50)))
                        Text(line, fontSize = 11.5.sp, lineHeight = 16.7.sp, color = colors.ink)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.darkGradient, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text("Month over month", fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(stats.currentLabel, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.6f))
                    Text(stats.currentValue, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(top = 3.dp))
                }
                Column {
                    Text(stats.previousLabel, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.6f))
                    Text(stats.previousValue, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}
