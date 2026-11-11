package com.appprint.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size as dpSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.CardType

/**
 * Pure-Compose rendering of the «sample card» placeholder — same artwork as the HTML design:
 * Iraqi flag stripe + country marks. Renders identically in previews and on-screen;
 * export renderers draw the print equivalent natively on each platform.
 */
@Composable
fun SampleCardArt(card: CardItem, modifier: Modifier = Modifier) {
    val isPassport = card.type == CardType.PASSPORT
    val radius = 10f // cosmetic; parent clips anyway

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(7.dp))
            .background(if (isPassport) Color(0xFF17365A) else Color(0xFFFDFAF1)),
        contentAlignment = Alignment.Center,
    ) {
        if (isPassport) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .dpSize(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9A51F)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.dpSize(36.dp).clip(CircleShape).background(Color(0xFF17365A)), contentAlignment = Alignment.Center) {
                        Box(Modifier.dpSize(24.dp).clip(CircleShape).background(Color(0xFFD9A51F)))
                    }
                }
                Text(
                    "جمهورية العراق",
                    color = Color(0xFFD9A51F),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "REPUBLIC OF IRAQ",
                    color = Color(0xFFD9A51F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    "PASSPORT",
                    color = Color(0xFFD9A51F),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
            // Iraqi flag stripe
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(0.20f)
                    .clip(RoundedCornerShape(3.dp)),
            ) {
                Box(Modifier.weight(0.34f).fillMaxSize().background(Color(0xFFC8102E)))
                Box(Modifier.weight(0.32f).fillMaxSize().background(Color(0xFFFDFAF1)), contentAlignment = Alignment.Center) {
                    Text(
                        "الله أكبر",
                        color = Color(0xFF007A3D),
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                        maxLines = 1,
                    )
                }
                Box(Modifier.weight(0.34f).fillMaxSize().background(Color(0xFF007A3D)))
            }

            Text(
                text = card.name,
                color = Color(0xFF1C1C1C),
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp),
            )
            Text(
                text = "REPUBLIC OF IRAQ • جمهورية العراق",
                color = Color(0xFF4B453A),
                fontWeight = FontWeight.Medium,
                fontSize = 8.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
            )
            Text(
                text = "IRAQ • العراق",
                color = Color(0xFF0D5B3A),
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp),
            )
            Spacer16()
            Text(
                text = "SAMPLE",
                color = Color(0xFFB0AFA6),
                fontWeight = FontWeight.Bold,
                fontSize = 7.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun Spacer16() {
    Box(Modifier.padding(top = 8.dp))
}
