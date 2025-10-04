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
import androidx.compose.ui.focus.onFocusChanged
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
    query: String,                       // ✨ 變更點 1: 接收外部傳入的 query
    onQueryChange: (String) -> Unit,     // ✨ 變更點 2: 接收外部傳入的更新函式
    onSettingsClicked: () -> Unit,
    onSearch: () -> Unit                 // ✨ 變更點 3: onSearch 不再需要傳遞字串
) {
    // var query by remember { mutableStateOf("") } // ✨ 變更點 4: 移除內部狀態，實現狀態提升
    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,                   // ✨ 變更點 5: 使用傳入的 query
        onValueChange = onQueryChange,   // ✨ 變更點 6: 使用傳入的 onQueryChange
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (!isFocused) {
                    keyboardController?.hide()
                }
            },
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
                focusManager.clearFocus()
                onSearch() // ✨ 變更點 7: 呼叫新的 onSearch
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
    // 為了讓預覽正常運作，我們在這裡建立一個暫時的狀態
    var previewQuery by remember { mutableStateOf("") }
    MySearchBar(
        query = previewQuery,
        onQueryChange = { previewQuery = it },
        onSettingsClicked = {},
        onSearch = {}
    )
}