package com.appprint.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.ui.StudioViewModel
import com.appprint.studio.ui.components.FlagCardGlyph
import com.appprint.studio.ui.components.SectionHeader
import com.appprint.studio.ui.strings.Str

private const val VERSION_NAME = "1.0.0"
private const val VERSION_CODE = 1

private val DEPENDENCIES = listOf(
    Triple("AndroidX Core KTX", "1.19.1", "Apache-2.0 · Google"),
    Triple("AndroidX Core SplashScreen", "1.2.0", "Apache-2.0 · Google"),
    Triple("AndroidX Activity & Compose", "1.13.0", "Apache-2.0 · Google"),
    Triple("Compose Multiplatform", "1.11.1", "Apache-2.0 · JetBrains"),
    Triple("Material 3 (Compose)", "1.4.0", "Apache-2.0 · Google"),
    Triple("Navigation 3", "1.2.0", "Apache-2.0 · Google/JetBrains"),
    Triple("Lifecycle (Compose)", "2.11.0", "Apache-2.0 · JetBrains"),
    Triple("Kotlin Coroutines", "1.11.0", "Apache-2.0 · JetBrains"),
    Triple("Kotlinx Serialization", "1.12.0", "Apache-2.0 · JetBrains"),
    Triple("Kotlin", "2.4.21", "Apache-2.0 · JetBrains"),
    Triple("Android Gradle Plugin", "9.2.1", "Apache-2.0 · Google"),
)

@Composable
fun SettingsScreen(viewModel: StudioViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(end = 2.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
    ) {
        // Appearance
        item(contentType = "appearance") {
            SectionHeader(Str.appearance)
            val options = listOf(
                AppTheme.SYSTEM to Str.theme_auto,
                AppTheme.LIGHT to Str.theme_light,
                AppTheme.DARK to Str.theme_dark,
            )
            options.forEach { (option, labelRes) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.theme == option) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = state.theme == option,
                        onClick = { viewModel.setTheme(option) },
                    )
                    Text(
                        labelRes,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        // Privacy
        item(contentType = "privacy") {
            SectionHeader(Str.privacy_title)
            Text(
                Str.privacy_body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text(
                Str.offline_note,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        // Permissions
        item(contentType = "permissions") {
            SectionHeader(Str.permissions_title)
            Text(
                Str.permission_camera,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        // About
        item(contentType = "about") {
            SectionHeader(Str.about_title)
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FlagCardGlyph(sizeDp = 64.dp)
                Column {
                    Text(
                        text = "مطبعة البطاقات العراقية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "الإصدار $VERSION_NAME ($VERSION_CODE)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Text(
                "IRAQI ID PRINT STUDIO — قصّ، رتّب، اطبع بقياس حقيقي 1:1",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        // Dependencies & licenses
        item(contentType = "licenses-header") {
            SectionHeader(Str.licenses_title)
        }
        items(DEPENDENCIES.size, contentType = { "license" }) { index ->
            val (name, version, license) = DEPENDENCIES[index]
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        license,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    version,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(contentType = "footer") {
            Spacer(Modifier.height(8.dp))
            Text(
                "صُنع للشعب العراقي وبأيادٍ عراقية — بقياس حقيقي 1:1.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}
