package org.moneymanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import org.moneymanager.data.ApiClient
import org.moneymanager.data.TokenStore
import org.moneymanager.notifications.ACTION_TRACK_PURCHASE
import org.moneymanager.notifications.PurchaseNotificationManager
import org.moneymanager.signals.FakePurchaseSignalSource

class MainActivity : ComponentActivity() {
    private lateinit var purchaseNotificationManager: PurchaseNotificationManager
    private val fakePurchaseSignalSource = FakePurchaseSignalSource()
    private var pendingTrackPurchase by mutableStateOf(false)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStore = TokenStore(this)
        val apiClient = ApiClient(BuildConfig.API_BASE_URL)
        purchaseNotificationManager = PurchaseNotificationManager(this).also {
            it.createNotificationChannel()
        }
        fakePurchaseSignalSource.start {
            purchaseNotificationManager.showPurchaseDetectedNotification()
        }
        requestNotificationPermissionIfNeeded()
        pendingTrackPurchase = isTrackPurchaseIntent(intent)

        setContent {
            val viewModel: MoneyManagerViewModel = viewModel(
                factory = MoneyManagerViewModelFactory(apiClient, tokenStore),
            )
            val state by viewModel.state.collectAsState()

            MoneyManagerRoot(
                state = state,
                viewModel = viewModel,
                pendingTrackPurchase = pendingTrackPurchase,
                onTrackPurchaseHandled = { pendingTrackPurchase = false },
                onExportCsv = ::shareCsvFile,
                onSimulatePurchaseSignal = fakePurchaseSignalSource::simulatePurchaseSignal,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isTrackPurchaseIntent(intent)) {
            pendingTrackPurchase = true
        }
    }

    override fun onDestroy() {
        fakePurchaseSignalSource.stop()
        super.onDestroy()
    }

    private fun shareCsvFile(fileName: String, csv: String) {
        val file = File(cacheDir, fileName)
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export transactions"))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun isTrackPurchaseIntent(intent: Intent?): Boolean =
        intent?.action == ACTION_TRACK_PURCHASE
}
