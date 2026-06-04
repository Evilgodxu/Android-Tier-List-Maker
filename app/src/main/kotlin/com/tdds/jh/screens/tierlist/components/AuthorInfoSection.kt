package com.tdds.jh.screens.tierlist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdds.jh.R

/**
 * 作者信息区域
 * 显示在屏幕底部，点击可编辑作者名称
 *
 * @param authorName 作者名称
 * @param onClick 点击回调
 */
@Composable
fun AuthorInfoSection(
    authorName: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (authorName.isBlank()) "-${stringResource(R.string.input_author)}-" else "-$authorName-",
            fontSize = 16.sp,
            color = if (authorName.isBlank()) Color.Gray else MaterialTheme.colorScheme.primary
        )
    }
}
