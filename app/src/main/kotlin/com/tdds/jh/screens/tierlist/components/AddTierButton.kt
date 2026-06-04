package com.tdds.jh.screens.tierlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 添加层级按钮
 * 显示在层级列表底部，点击添加新层级
 *
 * @param onClick 点击回调
 */
@Composable
fun AddTierButton(onClick: () -> Unit) {
    val extendedColors = LocalExtendedColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(50.dp)
            .background(
                extendedColors.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(R.string.add_tier),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
