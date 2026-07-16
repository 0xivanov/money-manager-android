package org.moneymanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import org.moneymanager.data.ApiClient
import org.moneymanager.data.TokenStore
import org.moneymanager.notifications.PurchaseNotificationManager
import org.moneymanager.notifications.EXTRA_PUSH_EVENT
import org.moneymanager.notifications.FirebasePushConfiguration
import org.moneymanager.signals.FakePurchaseSignalSource

class MainActivity : ComponentActivity() {
    private lateinit var purchaseNotificationManager: PurchaseNotificationManager
    private val fakePurchaseSignalSource = FakePurchaseSignalSource()
    private var pendingTrackPurchase by mutableStateOf(false)
    private var notificationsEnabled by mutableStateOf(false)
    private var firebaseDeviceToken by mutableStateOf<String?>(null)
    private var pendingPushEvent by mutableStateOf<String?>(null)
    private var simulateAfterPermission = false
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsEnabled = granted
            if (granted && simulateAfterPermission) {
                fakePurchaseSignalSource.simulatePurchaseSignal()
            }
            simulateAfterPermission = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)

        val tokenStore = TokenStore(this)
        val apiClient = ApiClient(BuildConfig.API_BASE_URL)
        if (BuildConfig.DEBUG) {
            purchaseNotificationManager = PurchaseNotificationManager(this).also {
                it.createNotificationChannel()
            }
            fakePurchaseSignalSource.start {
                purchaseNotificationManager.showPurchaseDetectedNotification()
            }
        }
        notificationsEnabled = canPostNotifications()
        pendingTrackPurchase = isTrackPurchaseIntent(intent)
        pendingPushEvent = intent.getStringExtra(EXTRA_PUSH_EVENT)
        FirebasePushConfiguration.initialize(this) { firebaseDeviceToken = it }

        setContent {
            val viewModel: MoneyManagerViewModel = viewModel(
                factory = MoneyManagerViewModelFactory(apiClient, tokenStore),
            )
            val state by viewModel.state.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDarkBars = state.appearance == AppAppearance.Dark ||
                (state.appearance == AppAppearance.System && systemDark)
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !useDarkBars
                    isAppearanceLightNavigationBars = !useDarkBars
                }
            }
            LaunchedEffect(state.token, firebaseDeviceToken) {
                if (state.token != null) firebaseDeviceToken?.let(viewModel::registerPushDevice)
            }
            LaunchedEffect(pendingPushEvent) {
                pendingPushEvent?.let(viewModel::openPushEvent)
                pendingPushEvent = null
            }

            MoneyManagerRoot(
                state = state,
                viewModel = viewModel,
                pendingTrackPurchase = pendingTrackPurchase,
                onTrackPurchaseHandled = { pendingTrackPurchase = false },
                onExportCsv = ::shareCsvFile,
                notificationsEnabled = notificationsEnabled,
                onEnableNotifications = { requestNotificationPermissionIfNeeded(false) },
                onSimulatePurchaseSignal = ::simulatePurchaseSignal,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isTrackPurchaseIntent(intent)) {
            pendingTrackPurchase = true
        }
        intent.getStringExtra(EXTRA_PUSH_EVENT)?.let { pendingPushEvent = it }
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) fakePurchaseSignalSource.stop()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        notificationsEnabled = canPostNotifications()
    }

    private fun shareCsvFile(fileName: String, csv: String) {
        val exportDirectory = File(cacheDir, "exports").apply { mkdirs() }
        exportDirectory.listFiles()
            ?.filter { it.extension.equals("csv", ignoreCase = true) }
            ?.forEach(File::delete)
        val file = File(exportDirectory, fileName)
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export CSV"))
    }

    private fun requestNotificationPermissionIfNeeded(simulateWhenGranted: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            simulateAfterPermission = simulateWhenGranted
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsEnabled = true
            if (simulateWhenGranted) fakePurchaseSignalSource.simulatePurchaseSignal()
        }
    }

    private fun simulatePurchaseSignal() {
        requestNotificationPermissionIfNeeded(simulateWhenGranted = true)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun isTrackPurchaseIntent(intent: Intent?): Boolean =
        intent?.action == ACTION_TRACK_PURCHASE

    private companion object {
        const val ACTION_TRACK_PURCHASE = "org.moneymanager.action.TRACK_PURCHASE"
    }
}
