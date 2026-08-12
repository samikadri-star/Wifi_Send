package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WifiP2pDeviceItem
import com.example.ui.components.RadarPulseAnimation
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.TransferViewModel

@Composable
fun P2pScanScreen(
    viewModel: TransferViewModel,
    onNavigateToTransfers: () -> Unit
) {
    val peers by viewModel.discoveredPeers.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val isServerListening by viewModel.isServerListening.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()

    var isScanning by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "الاقتران المباشر (Wi-Fi Direct)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "البحث عن أجهزة مجاورة لنقل الملفات والمجلدات بسرعه فائقة بدون إنترنت",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Radar Pulse Component
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarPulseAnimation(
                isScanning = isScanning,
                onRadarClick = {
                    isScanning = !isScanning
                    if (isScanning) viewModel.startPeerScan()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الأجهزة المكتشفة (${peers.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                onClick = {
                    isScanning = true
                    viewModel.startPeerScan()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "إعادة البحث",
                    tint = PrimaryCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("إعادة البحث", color = PrimaryCyan, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (peers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "جاري مسح ترددات الواي فاي المباشرة...",
                        color = TextSecondaryDark,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(peers) { device ->
                    val isConnected = connectedDevice?.deviceAddress == device.deviceAddress

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                if (isConnected) PrimaryCyan else Color.Transparent,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                viewModel.selectPeerDevice(device)
                                if (selectedItems.isNotEmpty()) {
                                    viewModel.sendSelectedFiles()
                                    onNavigateToTransfers()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConnected) PrimaryCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = PrimaryCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.deviceName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Wi-Fi Direct • ${device.statusTextArabic}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.selectPeerDevice(device)
                                    if (selectedItems.isNotEmpty()) {
                                        viewModel.sendSelectedFiles()
                                        onNavigateToTransfers()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isConnected) PrimaryCyan else PrimaryBlue
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isConnected) "اقتران نشط" else "اقتران ونقل",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) MaterialTheme.colorScheme.surface else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
