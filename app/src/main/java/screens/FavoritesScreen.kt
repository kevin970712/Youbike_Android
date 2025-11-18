package com.android.youbike.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.youbike.StationResultItem
import com.android.youbike.data.StationInfo
import com.android.youbike.ui.viewmodel.YouBikeViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FavoritesScreen(
    viewModel: YouBikeViewModel,
    onFavoriteToggle: (StationInfo) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val onRefresh = {
        if (uiState.isSearching) {
            viewModel.refreshSearchResults(uiState.currentQuery)
        } else {
            viewModel.refreshFavoriteStations()
        }
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = onRefresh
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        val stationsToShow = if (uiState.isSearching) uiState.searchResults else uiState.favoriteStations

        when {
            uiState.isLoading && stationsToShow.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null && !uiState.isSearching -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.errorMessage!!)
                }
            }
            stationsToShow.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(
                        items = stationsToShow,
                        key = { it.info.stationNo }
                    ) { result ->
                        StationResultItem(
                            result = result,
                            onFavoriteClicked = onFavoriteToggle
                        )
                    }
                }
            }
            else -> {
                val message = if (uiState.isSearching) "找不到符合條件的站點" else "點擊卡片右上角的愛心來收藏站點"
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = message)
                }
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}