package com.android.youbike.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

// API 基礎 URL
private const val BASE_URL = "https://apis.youbike.com.tw/"

// 建立一個 Json 解析器實例，並設定它忽略在 data class 中未定義的欄位
private val json = Json { ignoreUnknownKeys = true }

// 設定 Retrofit
private val retrofit = Retrofit.Builder()
    // 將我們上面設定好的 Json 解析器傳遞給 Retrofit
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .baseUrl(BASE_URL)
    .build()

// 定義 API 的各個請求方法 (合約)
interface YouBikeApiService {
    // 取得所有站點的基本資訊
    @GET("json/station-min-yb2.json")
    suspend fun getAllStations(): List<StationInfo>

    // 根據站點編號列表，取得詳細的車輛和空位資訊
    @Headers(
        "accept: */*",
        "content-type: application/json"
    )
    @POST("tw2/parkingInfo")
    suspend fun getParkingInfo(@Body stationRequest: StationRequest): ParkingInfoResponse
}

// 建立一個公開的單例 (Singleton) 物件，讓 App 其他地方可以方便地呼叫 API
object YouBikeApi {
    val retrofitService: YouBikeApiService by lazy {
        retrofit.create(YouBikeApiService::class.java)
    }
}