package com.android.youbike.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- 站點基本資訊的模型 (來自 station-min-yb2.json) ---
@Serializable
data class StationInfo(
    @SerialName("station_no") val stationNo: String,
    @SerialName("name_tw") val name: String,
    @SerialName("district_tw") val district: String,
    @SerialName("address_tw") val address: String,
    @SerialName("lat") val latitude: Double,
    @SerialName("lng") val longitude: Double
)

// --- 車輛詳細資訊的模型 (來自 /parkingInfo) ---

// POST 請求時，我們需要傳送的資料結構
@Serializable
data class StationRequest(
    @SerialName("station_no") val stationNumbers: List<String>
)

// --- ✨ 這是修改的重點區域 ✨ ---

// API 回應的最外層結構
@Serializable
data class ParkingInfoResponse(
    @SerialName("retVal") val retVal: RetValData // 修改這裡，retVal 是一個物件
)

// 代表 retVal 物件的結構，它裡面包含了一個 data 陣列
@Serializable
data class RetValData(
    @SerialName("data") val data: List<VehicleInfo>
)

// --- ✨ 修改區域結束 ✨ ---


// 每一站的車輛資訊
@Serializable
data class VehicleInfo(
    @SerialName("station_no") val stationNo: String,
    @SerialName("empty_spaces") val emptySpaces: Int,
    @SerialName("available_spaces_detail") val vehicleDetails: VehicleDetail
)

// 車輛種類的詳細數量
@Serializable
data class VehicleDetail(
    @SerialName("yb2") val youbike2: Int, // YouBike 2.0
    @SerialName("eyb") val youbike2E: Int // YouBike 2.0E 電輔車
)