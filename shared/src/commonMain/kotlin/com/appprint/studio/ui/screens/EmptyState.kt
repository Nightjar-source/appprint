package com.appprint.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.model.CardType
import com.appprint.studio.ui.strings.Str
import com.appprint.studio.ui.TabatiUiState

/**
 * Empty state — the design's hero: big star mark, «ارفع صور بطاقاتك للبدء»,
 * the two CTAs (upload / examples) and the strip of official card samples.
 */
@Composable
fun EmptyState(
    state: TabatiUiState,
    onAddImages: () -> Unit,
    onSamples: () -> Unit,
    onAddSampleOfType: (CardType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        com.appprint.studio.ui.components.TabatiLogoMark(size = 96.dp)
        Text(
            text = Str.empty_title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp, start = 12.dp, end = 12.dp),
        )
        Text(
            text = Str.empty_subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )
        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onAddImages, shape = RoundedCornerShape(percent = 50)) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer8()
                Text(Str.add_images, fontWeight = FontWeight.ExtraBold)
            }
            OutlinedButton(onClick = onSamples, shape = RoundedCornerShape(percent = 50)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer8()
                Text(Str.samples, fontWeight = FontWeight.ExtraBold)
            }
        }
        Text(
            text = Str.sample_title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 26.dp, start = 8.dp, end = 8.dp),
        )
        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
        ) {
            items(CardType.PRINTABLE.filter { it != CardType.CUSTOM }, key = { it.key }) { type ->
                SampleCardTile(type = type, onClick = { onAddSampleOfType(type) })
            }
        }
    }
}

@Composable
private fun Spacer8() {
    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
}

@Composable
private fun SampleCardTile(type: CardType, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val isPassport = type == CardType.PASSPORT
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainerHigh)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(92.dp, 58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isPassport) Color(0xFF17365A) else Color(0xFFFDFAF1)),
        ) {
            if (isPassport) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(type.shortAr, color = Color(0xFFD9A51F), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("PASSPORT", color = Color(0xFFD9A51F), fontSize = 6.sp, letterSpacing = 2.sp)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(0.42f)) {
                        Box(Modifier.weight(0.34f).fillMaxSize().background(Color(0xFFCE1126)))
                        Box(Modifier.weight(0.32f).fillMaxSize().background(Color(0xFFFDFAF1)), contentAlignment = Alignment.Center) {
                            Text("الله أكبر", color = Color(0xFF007A3D), fontWeight = FontWeight.Black, fontSize = 6.sp, maxLines = 1)
                        }
                        Box(Modifier.weight(0.34f).fillMaxSize().background(Color(0xFF007A3D)))
                    }
                    Box(Modifier.weight(0.58f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(type.shortAr, color = Color(0xFF1C1C1C), fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        Text(
            text = type.labelAr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = com.appprint.studio.core.model.CardGuides[type]?.sizeNoteAr ?: "",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
