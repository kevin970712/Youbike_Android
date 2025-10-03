package com.android.youbike.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 使用委託 (delegation) 在 Context 中建立一個 DataStore 實例
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * 這個類別負責處理使用者偏好設定的讀取和寫入。
 */
class UserPreferencesRepository(private val context: Context) {

    // Json 實例，用於序列化和反序列化
    private val json = Json { ignoreUnknownKeys = true }

    // 用於在 DataStore 中存取我們的最愛站點 JSON 字串的 Key
    private val FAVORITE_STATIONS_JSON = stringPreferencesKey("favorite_stations_json")

    /**
     * 提供一個 Flow，讓外部可以持續監聽最愛站點的 StationInfo 列表。
     */
    val favoriteStations: Flow<List<StationInfo>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[FAVORITE_STATIONS_JSON]
            if (jsonString != null) {
                // 如果字串存在，將其從 JSON 反序列化回物件列表
                try {
                    json.decodeFromString<List<StationInfo>>(jsonString)
                } catch (e: Exception) {
                    // 如果解析失敗，回傳空列表以避免閃退
                    emptyList()
                }
            } else {
                // 如果不存在，回傳空列表
                emptyList()
            }
        }

    /**
     * 儲存更新後的最愛 StationInfo 列表。
     */
    suspend fun saveFavoriteStations(stations: List<StationInfo>) {
        // 將物件列表序列化成 JSON 字串
        val jsonString = json.encodeToString(stations)
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_STATIONS_JSON] = jsonString
        }
    }
}