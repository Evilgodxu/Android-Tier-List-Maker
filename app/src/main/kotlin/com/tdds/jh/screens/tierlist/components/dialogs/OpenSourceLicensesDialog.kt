package com.tdds.jh.screens.tierlist.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 开源库信息
 */
private data class OpenSourceLibrary(
    val name: String,
    val license: String
)

/**
 * 本应用使用的开源库列表
 */
private val libraries = listOf(
    OpenSourceLibrary("AndroidX Core KTX", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Lifecycle", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Activity Compose", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Compose", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Material3", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX DocumentFile", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX DataStore", "Apache License 2.0"),
    OpenSourceLibrary("Kotlin", "Apache License 2.0"),
    OpenSourceLibrary("Kotlinx Coroutines", "Apache License 2.0"),
    OpenSourceLibrary("Coil", "Apache License 2.0"),
    OpenSourceLibrary("Apache Commons Compress", "Apache License 2.0"),
    OpenSourceLibrary("Zip4j", "Apache License 2.0"),
    OpenSourceLibrary("easycrop", "Apache License 2.0"),
    OpenSourceLibrary("Reorderable", "Apache License 2.0")
)

/**
 * 开源许可对话框
 *
 * @param onDismiss 关闭回调
 */
@Composable
fun OpenSourceLicensesDialog(
    onDismiss: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.5f),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = extendedColors.cardBackground
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.open_source_licenses),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(libraries) { library ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = library.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = library.license,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.close),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
