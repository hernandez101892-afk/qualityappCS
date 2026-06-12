package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.QualityRepository
import com.example.ui.screens.CreateRecordScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RecordDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.QualityViewModel
import com.example.viewmodel.QualityViewModelFactory

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
      androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
      // Essential permissions are requested upfront; logs or internal flags can be set here if needed
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Proactively ask for CAMERA and legacy Storage permissions on launch so the application is immediately ready to use
    val permissionsToRequest = mutableListOf(android.Manifest.permission.CAMERA)
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        QualityAppMain()
      }
    }
  }
}

@Composable
fun QualityAppMain() {
  val context = LocalContext.current
  
  // Local Database + Repository Setup
  val database = AppDatabase.getDatabase(context)
  val repository = QualityRepository(database.qualityDao())
  
  // Central MVVM ViewModel initialized via standard Provider Factory
  val viewModel: QualityViewModel = viewModel(
    factory = QualityViewModelFactory(repository)
  )
  
  // Navigation Controller configuration
  val navController = rememberNavController()
  val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()

  // Dynamic session reactive check
  LaunchedEffect(isUserLoggedIn) {
    if (!isUserLoggedIn) {
      navController.navigate("login") {
        popUpTo(0) { inclusive = true }
      }
    } else {
      navController.navigate("dashboard") {
        popUpTo(0) { inclusive = true }
      }
    }
  }

  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = if (isUserLoggedIn) "dashboard" else "login",
      modifier = Modifier.padding(innerPadding)
    ) {
      // 0. LOGIN OVERVIEW SCREEN
      composable("login") {
        LoginScreen(
          viewModel = viewModel,
          onLoginSuccess = {
            navController.navigate("dashboard") {
              popUpTo("login") { inclusive = true }
            }
          }
        )
      }

      // 1. DASHBOARD OVERVIEW SCREEN
      composable("dashboard") {
        DashboardScreen(
            viewModel = viewModel,
            onNavigateToCreate = { navController.navigate("create_record") },
            onNavigateToDetail = { oc -> navController.navigate("record_detail/$oc") },
            onNavigateToSettings = { navController.navigate("settings") }
        )
      }

      // 2. CREATE QUALITY REJECT RECORD FORM
      composable("create_record") {
        CreateRecordScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
      }

      // 3. EXPEDIENT RECORD DETAIL & SUBSECTIONS
      composable(
        route = "record_detail/{oc}",
        arguments = listOf(navArgument("oc") { type = NavType.StringType })
      ) { backStackEntry ->
        val ocArg = backStackEntry.arguments?.getString("oc") ?: ""
        RecordDetailScreen(
            oc = ocArg,
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
      }

      // 4. SETTINGS & CONFORMANCE LOGS SCREEN
      composable("settings") {
        SettingsScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
      }
    }
  }
}
