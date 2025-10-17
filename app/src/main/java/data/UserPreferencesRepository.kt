package com.android.youbike.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * 這個類別負責處理使用者偏好設定的讀取和寫入。
 */
class UserPreferencesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val FAVORITE_STATIONS_JSON = stringPreferencesKey("favorite_stations_json")
    private val REFRESH_INTERVAL_SECONDS = intPreferencesKey("refresh_interval_seconds")

    /**
     * 提供一個 Flow，讓外部可以持續監聽最愛站點的 StationInfo 列表。
     */
    val favoriteStations: Flow<List<StationInfo>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[FAVORITE_STATIONS_JSON]
            if (jsonString != null) {
                try {
                    json.decodeFromString<List<StationInfo>>(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

    /**
     * 提供一個 Flow，讓外部可以持續監聽自動刷新頻率的設定值 (秒)。
     */
    val refreshInterval: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[REFRESH_INTERVAL_SECONDS] ?: 0 // 如果找不到值，預設為 0 (永不)
        }

    /**
     * 儲存更新後的最愛 StationInfo 列表。
     */
    suspend fun saveFavoriteStations(stations: List<StationInfo>) {
        val jsonString = json.encodeToString(stations)
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_STATIONS_JSON] = jsonString
        }
    }

    /**
     * 儲存更新後的自動刷新頻率 (秒)。
     */
    suspend fun saveRefreshInterval(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[REFRESH_INTERVAL_SECONDS] = seconds
        }
    }
}