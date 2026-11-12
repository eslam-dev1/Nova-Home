package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun AppItemPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.app_info)) },
            onClick = {
                onAppInfo()
                onDismissRequest()
            },
            leadingIcon = { Icon(Icons.Default.Info, null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.uninstall)) },
            onClick = {
                onUninstall()
                onDismissRequest()
            },
            leadingIcon = { Icon(Icons.Default.Delete, null) }
        )
        onRemove?.let {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_home)) },
                onClick = {
                    it()
                    onDismissRequest()
                },
                leadingIcon = { Icon(Icons.Default.Settings, null) }
            )
        }
    }
}
