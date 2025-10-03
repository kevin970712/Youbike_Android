package com.android.youbike.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MySearchBar(
    modifier: Modifier = Modifier,
    // --- ✨ 新增的參數 ✨ ---
    query: String,
    onQueryChange: (String) -> Unit,
    // --- ✨ 原有的參數 ✨ ---
    onSettingsClicked: () -> Unit,
    onSearch: (String) -> Unit
) {
    // --- 移除了內部的 query 和 isFocused 狀態 ---
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query, // 使用外部傳入的 query
        onValueChange = onQueryChange, // 使用外部傳入的 onQueryChange
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("搜尋 Youbike 站點") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon"
            )
        },
        trailingIcon = {
            IconButton(onClick = onSettingsClicked) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings Icon"
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                // 清除焦點的邏輯依然保留
                focusManager.clearFocus()
                onSearch(query)
            }
        ),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    // 為了讓預覽能正常工作，我們需要提供一個假的狀態
    var previewQuery by remember { mutableStateOf("") }
    MySearchBar(
        query = previewQuery,
        onQueryChange = { previewQuery = it },
        onSettingsClicked = {},
        onSearch = {}
    )
}