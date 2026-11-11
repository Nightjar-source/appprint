package com.appprint.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appprint.studio.core.model.CardType
import com.appprint.studio.shared.resources.Res
import com.appprint.studio.shared.resources.guide_cards_subtitle
import com.appprint.studio.shared.resources.guide_cards_title
import com.appprint.studio.shared.resources.issuer
import com.appprint.studio.shared.resources.legal_note
import com.appprint.studio.shared.resources.official_size
import com.appprint.studio.shared.resources.print_tips_title
import com.appprint.studio.shared.resources.usage
import com.appprint.studio.ui.components.SectionHeader
import org.jetbrains.compose.resources.stringResource

private val PRINT_TIPS = listOf(
    "اطبع على ورق مقوّى 250 غرام حتى تتحمل البطاقة القص بعد الطباعة.",
    "استخدم جودة 300 DPI وفعّل خيار «بدون تصغير» — الطباعة بقياس 100%.",
    "قصّ على الخط المتقطع تماماً، واستخدم مقصاً حاداً أو أداة قص دوّارة.",
    "امسح الوجهين بنفس الدقة وثبّت الورقة بأشرطة حتى لا تنزلق أثناء المسح.",
    "لا تقص داخل الحافة الملونة من البطاقات الرسمية.",
    "حافظ على الباركود واضحاً بلا خدش أو طيّ.",
    "جرّب طباعة تجريبية على ورق عادي وقِس بالمسطرة قبل الورق المقوّى.",
    "اطبع بمقياس 100% — أي «تصغير للملاءمة» يفسد القياس الرسمي.",
    "استخدم حبر أسود عالي الكثافة مع ورق أبيض غير لامع للنصوص.",
    "رتّب بطاقات من نفس النوع في نفس الصف لتقليل الهدر.",
    "احفظ الملف PDF للطابعة واطبع مباشرة — لا تعيد تحويله عبر برامج أخرى.",
    "بعد القص، غلّف البطاقات بغلاف بلاستيكي حراري لطول العمر.",
)

private val CARD_TYPES = CardType.PRINTABLE.filter { it != CardType.CUSTOM }

@Composable
fun GuideScreen() {
    var expanded by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(end = 2.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
    ) {
        item(contentType = "header") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = stringResource(Res.string.guide_cards_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(Res.string.guide_cards_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        items(CARD_TYPES, key = { it.key }, contentType = { "card" }) { card ->
            CardFactCard(
                card = card,
                expanded = expanded == card.key,
                onToggle = { expanded = if (expanded == card.key) null else card.key },
            )
        }

        item(contentType = "tips-header") {
            SectionHeader(stringResource(Res.string.print_tips_title), modifier = Modifier.padding(top = 8.dp))
        }
        items(PRINT_TIPS.indices.toList(), key = { it }, contentType = { "tip" }) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = PRINT_TIPS[index],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(contentType = "legal") {
            Box(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(16.dp),
            ) {
                Column {
                    Text(
                        text = "تنبيه مهم",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = stringResource(Res.string.legal_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFactCard(
    card: CardType,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onToggle)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(width = 46.dp, height = 34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(card.brandColorHex)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = card.shortAr,
                    color = Color(0xFFFDFAF1),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(card.labelAr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    card.sizeNoteAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (expanded) "▴" else "▾",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(Modifier.padding(top = 12.dp, start = 2.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                FactLine(stringResource(Res.string.usage), card.useAr)
                FactLine(stringResource(Res.string.issuer), card.issuerAr)
                FactLine(stringResource(Res.string.official_size), card.sizeNoteAr)
                Text(
                    text = "نصائح خاصة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                card.tipsAr.forEach { tip ->
                    Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                        Text(tip, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun FactLine(label: String, value: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
    }
}
