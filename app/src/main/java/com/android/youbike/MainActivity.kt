package com.android.youbike

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.youbike.data.StationInfo
import com.android.youbike.ui.components.MySearchBar
import com.android.youbike.ui.screens.FavoritesScreen
import com.android.youbike.ui.screens.NearbyScreen
import com.android.youbike.ui.screens.SettingsScreen
import com.android.youbike.ui.theme.YoubikeTheme
import com.android.youbike.ui.viewmodel.StationResult
import com.android.youbike.ui.viewmodel.ViewModelFactory
import com.android.youbike.ui.viewmodel.YouBikeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var followSystemTheme by remember { mutableStateOf(true) }
            var isDarkMode by remember { mutableStateOf(false) }
            val useDarkTheme = when {
                followSystemTheme -> isSystemInDarkTheme()
                else -> isDarkMode
            }

            YoubikeTheme(darkTheme = useDarkTheme) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        val insetsController = WindowCompat.getInsetsController(window, view)
                        insetsController.isAppearanceLightStatusBars = !useDarkTheme
                    }
                }

                val context = LocalContext.current
                val viewModel: YouBikeViewModel = viewModel(
                    factory = ViewModelFactory(context.applicationContext as Application)
                )

                val rootNavController = rememberNavController()
                NavHost(navController = rootNavController, startDestination = "main") {
                    composable("main") {
                        YouBikeAppScaffold(
                            viewModel = viewModel,
                            onNavigateToSettings = { rootNavController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        val currentInterval by viewModel.userPreferencesRepository.refreshInterval.collectAsState(
                            initial = 0
                        )
                        SettingsScreen(
                            navController = rootNavController,
                            followSystemTheme = followSystemTheme,
                            isDarkMode = isDarkMode,
                            onFollowSystemChange = { followSystemTheme = it },
                            onDarkModeChange = { isDarkMode = it },
                            currentInterval = currentInterval,
                            onIntervalSelected = { seconds ->
                                viewModel.viewModelScope.launch {
                                    viewModel.userPreferencesRepository.saveRefreshInterval(seconds)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouBikeAppScaffold(
    viewModel: YouBikeViewModel,
    onNavigateToSettings: () -> Unit
) {
    val bottomBarNavController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val stableOnFavoriteToggle: (StationInfo) -> Unit = remember(viewModel) {
        { stationInfo -> viewModel.toggleFavorite(stationInfo) }
    }

    BackHandler {
        if (uiState.isSearching) {
            viewModel.clearSearchResults()
            query = ""
        } else if (bottomBarNavController.currentBackStackEntry?.destination?.route != "favorites") {
            bottomBarNavController.popBackStack()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.userPreferencesRepository.refreshInterval.flatMapLatest { interval ->
                if (interval > 0) {
                    flow {
                        delay(interval * 1000L)
                        while (true) {
                            emit(Unit)
                            delay(interval * 1000L)
                        }
                    }
                } else {
                    emptyFlow()
                }
            }.collect {
                val currentState = viewModel.uiState.value
                if (!currentState.isRefreshing) {
                    if (currentState.isSearching) {
                        viewModel.refreshSearchResults(currentState.currentQuery)
                    } else {
                        when (bottomBarNavController.currentDestination?.route) {
                            "favorites" -> viewModel.refreshFavoriteStations()
                            "nearby" -> {
                                // For nearby, trigger a location refresh.
                                // We need to pass the context somehow, or abstract location logic.
                                // For simplicity, let's leave this part for now.
                            }
                        }
                    }
                }
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(key1 = uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    Scaffold(
        topBar = {
            MySearchBar(
                modifier = Modifier.statusBarsPadding(),
                query = query,
                onQueryChange = { query = it },
                onSettingsClicked = onNavigateToSettings,
                onSearch = {
                    if (bottomBarNavController.currentDestination?.route != "favorites") {
                        bottomBarNavController.navigate("favorites") {
                            popUpTo(bottomBarNavController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                    viewModel.searchStations(query)
                    focusManager.clearFocus()
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = bottomBarNavController)
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomBarNavController,
            startDestination = "favorites",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("favorites") {
                FavoritesScreen(
                    viewModel = viewModel,
                    onFavoriteToggle = stableOnFavoriteToggle
                )
            }
            composable("nearby") {
                NearbyScreen(
                    viewModel = viewModel,
                    onFavoriteToggle = stableOnFavoriteToggle
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        "favorites" to Icons.Default.Favorite,
        "nearby" to Icons.Default.LocationOn
    )
    val labels = mapOf(
        "favorites" to "最愛",
        "nearby" to "附近站點"
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { (screen, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = labels[screen]) },
                label = { Text(labels[screen]!!) },
                selected = currentDestination?.hierarchy?.any { it.route == screen } == true,
                onClick = {
                    navController.navigate(screen) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun StationResultItem(
    result: StationResult,
    onFavoriteClicked: (StationInfo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = result.info.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = result.info.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    BikeInfo("YouBike 2.0", result.availableBikes?.toString() ?: "--")
                    BikeInfo("Youbike 2.0E", result.availableEBikes?.toString() ?: "--")
                    BikeInfo("可停空位", result.emptySpaces?.toString() ?: "--")
                }
            }
            IconButton(
                onClick = { onFavoriteClicked(result.info) },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (result.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (result.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BikeInfo(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}