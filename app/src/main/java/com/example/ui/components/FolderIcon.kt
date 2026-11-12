package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LauncherItem

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FolderIcon(
    folder: LauncherItem,
    folderItems: List<LauncherItem>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            // Preview of first 4 items
            val previewItems = folderItems.sortedBy { it.rank }.take(4)
            
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    FolderPreviewItem(previewItems.getOrNull(0), Modifier.weight(1f))
                    FolderPreviewItem(previewItems.getOrNull(1), Modifier.weight(1f))
                }
                Row(modifier = Modifier.weight(1f)) {
                    FolderPreviewItem(previewItems.getOrNull(2), Modifier.weight(1f))
                    FolderPreviewItem(previewItems.getOrNull(3), Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = folder.label ?: "",
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FolderPreviewItem(item: LauncherItem?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (item != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
    )
}
