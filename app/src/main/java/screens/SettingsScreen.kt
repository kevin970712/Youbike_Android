package com.android.youbike.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    followSystemTheme: Boolean,
    isDarkMode: Boolean,
    onFollowSystemChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    currentInterval: Int,
    onIntervalSelected: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = "外觀") {
                SettingsSwitchItem(
                    title = "跟隨系統主題",
                    checked = followSystemTheme,
                    onCheckedChange = onFollowSystemChange
                )
                SettingsSwitchItem(
                    title = "深色模式",
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange,
                    enabled = !followSystemTheme
                )
            }

            SettingsSection(title = "資料") {
                RefreshIntervalDropdown(
                    currentInterval = currentInterval,
                    onIntervalSelected = onIntervalSelected
                )
            }

            SettingsSection(title = "關於") {
                Column {
                    Text(
                        text = "版本",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Youbike 站點查詢 v1.0.1",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshIntervalDropdown(
    currentInterval: Int,
    onIntervalSelected: (Int) -> Unit
) {
    val options = mapOf(
        0 to "永不",
        10 to "10 秒",
        15 to "15 秒",
        20 to "20 秒",
        30 to "30 秒"
    )
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "自動刷新頻率",
            style = MaterialTheme.typography.bodyLarge
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = options[currentInterval] ?: "永不",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .width(120.dp),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (seconds, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onIntervalSelected(seconds)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}