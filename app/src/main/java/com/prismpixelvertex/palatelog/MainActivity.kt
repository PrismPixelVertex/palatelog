package com.prismpixelvertex.palatelog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.PalateLogTheme
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreen
import com.prismpixelvertex.palatelog.core.update.InAppUpdateHelper
import com.prismpixelvertex.palatelog.feature.navigation.PalateLogNavGraph
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var updateHelper: InAppUpdateHelper
    private var debugUpdateReceiver: BroadcastReceiver? = null
    private var notificationTargetScreen by mutableStateOf<String?>(null)
    private var notificationRestaurantId by mutableStateOf<String?>(null)

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        updateHelper.resetFlowState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        enableEdgeToEdge()

        updateHelper = InAppUpdateHelper(applicationContext)
        updateHelper.registerListener()
        updateHelper.checkForAppUpdate(updateLauncher)

        if (BuildConfig.DEBUG) {
            debugUpdateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val state = (intent?.getStringExtra("state")
                        ?: intent?.getStringExtra("action")
                        ?: intent?.getStringExtra("status"))?.uppercase(Locale.ROOT)
                    Log.i("MainActivity", "Received debug update broadcast with state: $state")
                    when (state) {
                        "DOWNLOADED" -> {
                            updateHelper.setUpdateDownloadedForTesting(downloaded = true)
                            Toast.makeText(this@MainActivity, "Debug: Update DOWNLOADED simulated", Toast.LENGTH_SHORT).show()
                        }
                        "RESET", "CLEAR" -> {
                            updateHelper.setUpdateDownloadedForTesting(downloaded = false)
                            Toast.makeText(this@MainActivity, "Debug: Update state RESET", Toast.LENGTH_SHORT).show()
                        }
                        "RESUME" -> {
                            updateHelper.checkStatusOnResume(updateLauncher)
                            Toast.makeText(this@MainActivity, "Debug: checkStatusOnResume() triggered", Toast.LENGTH_SHORT).show()
                        }
                        "AVAILABLE" -> {
                            Toast.makeText(this@MainActivity, "Debug: Update AVAILABLE received (use DOWNLOADED to test install prompt)", Toast.LENGTH_LONG).show()
                        }
                        else -> Log.w("MainActivity", "Unknown debug update state: $state (use --es state DOWNLOADED or --es action DOWNLOADED)")
                    }
                }
            }
            ContextCompat.registerReceiver(
                this,
                debugUpdateReceiver,
                IntentFilter("com.prismpixelvertex.palatelog.TEST_UPDATE"),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }

        setContent {
            PalateLogTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { /* permissions handled */ }

                val isUpdateDownloaded by updateHelper.isUpdateDownloaded.collectAsState()
                var showRestartDialog by rememberSaveable { mutableStateOf(value = false) }

                LaunchedEffect(isUpdateDownloaded) {
                    if (isUpdateDownloaded) {
                        showRestartDialog = true
                    }
                }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()

                    val fineLocationGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!fineLocationGranted) {
                        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        val notificationGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!notificationGranted) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PalateLogNavGraph(
                            initialTarget = notificationTargetScreen,
                            initialRestaurantId = notificationRestaurantId
                        )

                        if (BuildConfig.DEBUG && showRestartDialog && isUpdateDownloaded) {
                            AlertDialog(
                                onDismissRequest = { showRestartDialog = false },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.DownloadDone,
                                        contentDescription = null,
                                        tint = VerdictGoGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                title = {
                                    Text(
                                        text = stringResource(R.string.update_downloaded_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text(
                                        text = stringResource(R.string.update_downloaded_msg),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            updateHelper.completeUpdate()
                                            Toast.makeText(this@MainActivity, "Debug: completeUpdate() invoked", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.update_action_restart_now),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showRestartDialog = false }
                                    ) {
                                        Text(text = stringResource(R.string.update_action_later))
                                    }
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = isUpdateDownloaded && (!BuildConfig.DEBUG || !showRestartDialog),
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DownloadDone,
                                            contentDescription = null,
                                            tint = VerdictGoGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.update_downloaded_title),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = stringResource(R.string.update_banner_subtext),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            updateHelper.completeUpdate()
                                            if (BuildConfig.DEBUG) {
                                                Toast.makeText(this@MainActivity, "Debug: completeUpdate() invoked", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.update_action_restart),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager.ACTION_STOP_RADAR) {
            com.prismpixelvertex.palatelog.core.service.PalateRadarService.stopRadar(this)
        }
        notificationTargetScreen = intent.getStringExtra(com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager.EXTRA_NAV_TARGET)
        notificationRestaurantId = intent.getStringExtra(com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager.EXTRA_RESTAURANT_ID)
    }

    override fun onResume() {
        super.onResume()
        updateHelper.checkStatusOnResume(updateLauncher)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (BuildConfig.DEBUG) {
            debugUpdateReceiver?.let { unregisterReceiver(it) }
        }
        updateHelper.unregisterListener()
    }
}
