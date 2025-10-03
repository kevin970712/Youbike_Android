package com.android.youbike.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.youbike.data.StationInfo
import com.android.youbike.data.StationRequest
import com.android.youbike.data.UserPreferencesRepository
import com.android.youbike.data.VehicleInfo
import com.android.youbike.data.YouBikeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class StationResult(
    val info: StationInfo,
    val isFavorite: Boolean = false,
    val availableBikes: Int? = null,
    val availableEBikes: Int? = null,
    val emptySpaces: Int? = null
)

data class YouBikeUiState(
    val searchResults: List<StationResult> = emptyList(),
    val favoriteStations: List<StationResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false, // ✨ 新增刷新狀態
    val errorMessage: String? = null
)

class YouBikeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(YouBikeUiState())
    val uiState: StateFlow<YouBikeUiState> = _uiState.asStateFlow()

    private var allStationsCache: List<StationInfo>? = null
    private val userPreferencesRepository = UserPreferencesRepository(application)

    init {
        viewModelScope.launch {
            userPreferencesRepository.favoriteStations.collect { favoriteStationInfos ->
                loadAndRefreshFavoriteStations(favoriteStationInfos)
            }
        }
    }

    fun toggleFavorite(stationInfo: StationInfo) {
        viewModelScope.launch {
            val currentFavorites = userPreferencesRepository.favoriteStations.first().toMutableList()
            val existing = currentFavorites.find { it.stationNo == stationInfo.stationNo }

            // 1. 更新 DataStore (永久儲存)
            if (existing != null) {
                currentFavorites.remove(existing)
            } else {
                currentFavorites.add(stationInfo)
            }
            userPreferencesRepository.saveFavoriteStations(currentFavorites)

            // 2. 手動同步更新 searchResults 列表的狀態 (即時 UI 反應)
            _uiState.update { currentState ->
                val updatedSearchResults = currentState.searchResults.map { result ->
                    if (result.info.stationNo == stationInfo.stationNo) {
                        // 找到對應的站點，反轉它的 isFavorite 狀態
                        result.copy(isFavorite = !result.isFavorite)
                    } else {
                        result
                    }
                }
                currentState.copy(searchResults = updatedSearchResults)
            }
            // `favoriteStations` 列表會因為 DataStore 的 collect 自動更新，無需手動處理
        }
    }

    fun searchStations(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSearching = true, errorMessage = null) }
            try {
                val stationsToSearch = allStationsCache ?: fetchAndCacheAllStations()
                val filteredStations = if (query.isBlank()) stationsToSearch else stationsToSearch.filter {
                    it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true) || it.stationNo == query
                }
                val stationsToQuery = if (filteredStations.size > 100) filteredStations.take(100) else filteredStations
                val favoriteIds = userPreferencesRepository.favoriteStations.first().map { it.stationNo }.toSet()
                fetchParkingInfoForSearch(stationsToQuery, favoriteIds)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "發生錯誤：${e.message}") }
            }
        }
    }

    fun clearSearchResults() {
        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, errorMessage = null) }
    }

    // ✨ 新增：刷新收藏站點的方法
    fun refreshFavoriteStations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                val favoriteStationInfos = userPreferencesRepository.favoriteStations.first()
                if (favoriteStationInfos.isNotEmpty()) {
                    val vehicleDataMap = fetchVehicleData(favoriteStationInfos)
                    val results = favoriteStationInfos.map { info ->
                        val vehicleInfo = vehicleDataMap[info.stationNo]
                        StationResult(
                            info = info,
                            isFavorite = true,
                            availableBikes = vehicleInfo?.vehicleDetails?.youbike2,
                            availableEBikes = vehicleInfo?.vehicleDetails?.youbike2E,
                            emptySpaces = vehicleInfo?.emptySpaces
                        )
                    }
                    _uiState.update { it.copy(favoriteStations = results, isRefreshing = false) }
                } else {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = "刷新失敗：${e.message}"
                    )
                }
            }
        }
    }

    // ✨ 新增：刷新搜尋結果的方法
    fun refreshSearchResults(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                val stationsToSearch = allStationsCache ?: fetchAndCacheAllStations()
                val filteredStations = if (query.isBlank()) {
                    stationsToSearch
                } else {
                    stationsToSearch.filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.address.contains(query, ignoreCase = true) ||
                                it.stationNo == query
                    }
                }
                val stationsToQuery = if (filteredStations.size > 100) {
                    filteredStations.take(100)
                } else {
                    filteredStations
                }
                val favoriteIds = userPreferencesRepository.favoriteStations.first()
                    .map { it.stationNo }.toSet()

                val vehicleDataMap = fetchVehicleData(stationsToQuery)
                val results = stationsToQuery.map { stationInfo ->
                    val vehicleInfo = vehicleDataMap[stationInfo.stationNo]
                    StationResult(
                        info = stationInfo,
                        isFavorite = stationInfo.stationNo in favoriteIds,
                        availableBikes = vehicleInfo?.vehicleDetails?.youbike2 ?: 0,
                        availableEBikes = vehicleInfo?.vehicleDetails?.youbike2E ?: 0,
                        emptySpaces = vehicleInfo?.emptySpaces ?: 0
                    )
                }
                _uiState.update { it.copy(searchResults = results, isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = "刷新失敗：${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun fetchAndCacheAllStations(): List<StationInfo> {
        return try {
            val stations = YouBikeApi.retrofitService.getAllStations()
            allStationsCache = stations
            stations
        } catch (e: IOException) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "無法載入站點資料...") }
            emptyList()
        }
    }

    private fun loadAndRefreshFavoriteStations(favoriteStationInfos: List<StationInfo>) {
        viewModelScope.launch {
            if (favoriteStationInfos.isEmpty()) {
                _uiState.update { it.copy(favoriteStations = emptyList()) }
                return@launch
            }
            val initialResults = favoriteStationInfos.map { info ->
                StationResult(info = info, isFavorite = true)
            }
            _uiState.update { it.copy(favoriteStations = initialResults) }
            try {
                val vehicleDataMap = fetchVehicleData(favoriteStationInfos)
                val finalResults = initialResults.map { initialResult ->
                    val vehicleInfo = vehicleDataMap[initialResult.info.stationNo]
                    initialResult.copy(
                        availableBikes = vehicleInfo?.vehicleDetails?.youbike2,
                        availableEBikes = vehicleInfo?.vehicleDetails?.youbike2E,
                        emptySpaces = vehicleInfo?.emptySpaces
                    )
                }
                _uiState.update { it.copy(favoriteStations = finalResults) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "無法更新即時車輛資訊") }
            }
        }
    }

    private suspend fun fetchParkingInfoForSearch(stations: List<StationInfo>, favoriteIds: Set<String>) {
        if (stations.isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
            return
        }
        try {
            val vehicleDataMap = fetchVehicleData(stations)
            val results = stations.map { stationInfo ->
                val vehicleInfo = vehicleDataMap[stationInfo.stationNo]
                StationResult(
                    info = stationInfo,
                    isFavorite = stationInfo.stationNo in favoriteIds,
                    availableBikes = vehicleInfo?.vehicleDetails?.youbike2 ?: 0,
                    availableEBikes = vehicleInfo?.vehicleDetails?.youbike2E ?: 0,
                    emptySpaces = vehicleInfo?.emptySpaces ?: 0
                )
            }
            _uiState.update { it.copy(searchResults = results, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "查詢車輛資訊時發生錯誤：${e.message}") }
        }
    }

    private suspend fun fetchVehicleData(stations: List<StationInfo>): Map<String, VehicleInfo> {
        val stationIds = stations.map { it.stationNo }
        val vehicleDataMap = mutableMapOf<String, VehicleInfo>()
        stationIds.chunked(20).forEach { batchIds ->
            val response = YouBikeApi.retrofitService.getParkingInfo(StationRequest(batchIds))
            response.retVal.data.forEach { vehicleInfo -> vehicleDataMap[vehicleInfo.stationNo] = vehicleInfo }
        }
        return vehicleDataMap
    }
}