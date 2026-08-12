package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.FilesFoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.P2pScanScreen
import com.example.ui.screens.TransfersScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryCyan
import com.example.ui.viewmodel.TransferViewModel

enum class AppNavDestination(val labelArabic: String, val icon: ImageVector) {
    HOME("الرئيسية", Icons.Default.Home),
    FILES("الملفات والمجلدات", Icons.Default.FolderOpen),
    SCAN("اقتران واي فاي", Icons.Default.WifiTethering),
    TRANSFERS("سجل التحميلات", Icons.Default.SwapVert),
    ABOUT("حول التطبيق", Icons.Default.Info)
}

class MainActivity : ComponentActivity() {

    private val viewModel: TransferViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppStructure(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppStructure(viewModel: TransferViewModel) {
    var currentDestination by remember { mutableStateOf(AppNavDestination.HOME) }
    val uiMessage by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentDestination) {
                            AppNavDestination.HOME -> "واي فاي دايركت"
                            AppNavDestination.FILES -> "مستعرض الملفات والمجلدات"
                            AppNavDestination.SCAN -> "رادار الواي فاي دايركت"
                            AppNavDestination.TRANSFERS -> "إدارة التحميلات والمشاركات"
                            AppNavDestination.ABOUT -> "حول التطبيق والمعلومات"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                AppNavDestination.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.labelArabic,
                                tint = if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = destination.labelArabic,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppNavDestination.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToFiles = { currentDestination = AppNavDestination.FILES },
                    onNavigateToP2pScan = { currentDestination = AppNavDestination.SCAN },
                    onNavigateToTransfers = { currentDestination = AppNavDestination.TRANSFERS }
                )
                AppNavDestination.FILES -> FilesFoldersScreen(
                    viewModel = viewModel,
                    onNavigateToScan = { currentDestination = AppNavDestination.SCAN }
                )
                AppNavDestination.SCAN -> P2pScanScreen(
                    viewModel = viewModel,
                    onNavigateToTransfers = { currentDestination = AppNavDestination.TRANSFERS }
                )
                AppNavDestination.TRANSFERS -> TransfersScreen(
                    viewModel = viewModel
                )
                AppNavDestination.ABOUT -> AboutScreen()
            }
        }
    }
}
