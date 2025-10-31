package com.android.youbike.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.youbike.StationResultItem
import com.android.youbike.data.StationInfo
import com.android.youbike.ui.viewmodel.YouBikeViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@Composable
fun NearbyScreen(
    viewModel: YouBikeViewModel,
    onFavoriteToggle: (StationInfo) -> Unit
) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
                permissionGranted = true
            }
            permissionRequested = true
        }
    )

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    if (permissionGranted) {
        GetLocationAndShowStations(context, viewModel, onFavoriteToggle)
    } else if (permissionRequested) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "此功能需要定位權限才能尋找您附近的 YouBike 站點。",
                textAlign = TextAlign.Center
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun GetLocationAndShowStations(
    context: Context,
    viewModel: YouBikeViewModel,
    onFavoriteToggle: (StationInfo) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var initialLoadDone by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val fetchLocation: () -> Unit = {
        scope.launch {
            if (uiState.nearbyStations.isEmpty()) {
                viewModel.findNearbyStations(0.0, 0.0) // Show loading
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.findNearbyStations(location.latitude, location.longitude)
                } else {
                    viewModel.findNearbyStations(-1.0, -1.0) // Error state
                }
            }.addOnFailureListener {
                viewModel.findNearbyStations(-1.0, -1.0) // Error state
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!initialLoadDone) {
            fetchLocation()
            initialLoadDone = true
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = fetchLocation
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        when {
            uiState.isLoading && uiState.nearbyStations.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.errorMessage!!)
                }
            }
            uiState.nearbyStations.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(
                        items = uiState.nearbyStations,
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
                if(initialLoadDone) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("在您附近找不到任何 YouBike 站點。")
                    }
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