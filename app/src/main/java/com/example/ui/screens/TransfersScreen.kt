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
import com.example.data.db.TransferDirection
import com.example.data.db.TransferStatus
import com.example.data.db.TransferTaskEntity
import com.example.ui.components.SpeedometerWidget
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.TransferViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(
    viewModel: TransferViewModel
) {
    val tasks by viewModel.tasks.collectAsState()
    val progress by viewModel.transferProgress.collectAsState()

    var selectedFilterTab by remember { mutableStateOf("الكل") }

    val filteredTasks = when (selectedFilterTab) {
        "نشطة" -> tasks.filter { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.PENDING }
        "موقوفة" -> tasks.filter { it.status == TransferStatus.PAUSED }
        "مكتملة" -> tasks.filter { it.status == TransferStatus.COMPLETED }
        else -> tasks
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "سجل وعمليات النقل",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "إدارة التحميلات النشطة ودعم استئناف النقل التلقائي عند انقطاع الاتصال",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Active Speedometer Gauge Widget if transferring
        if (progress.isTransferring) {
            SpeedometerWidget(
                speedMBps = progress.speedMBps,
                progressPercentage = progress.progressPercentage,
                transferredMb = progress.transferredBytes / (1024 * 1024),
                totalMb = progress.totalBytes / (1024 * 1024),
                etaSeconds = progress.remainingSeconds
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filter Tabs
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("الكل", "نشطة", "موقوفة", "مكتملة").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedFilterTab == label,
                    onClick = { selectedFilterTab = label },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                ) {
                    Text(label, fontWeight = if (selectedFilterTab == label) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد عمليات نقل في هذا القسم",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredTasks) { task ->
                    TaskCardItem(
                        task = task,
                        onPause = { viewModel.pauseTransfer(task.id) },
                        onResume = { viewModel.resumeTransfer(task.id, isSender = task.direction == TransferDirection.SEND) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCardItem(
    task: TransferTaskEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val progressRatio = if (task.totalBytes > 0) task.transferredBytes.toFloat() / task.totalBytes.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                when (task.status) {
                    TransferStatus.IN_PROGRESS -> PrimaryCyan
                    TransferStatus.PAUSED -> Color(0xFFF59E0B)
                    else -> Color.Transparent
                },
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (task.direction == TransferDirection.SEND) PrimaryBlue.copy(alpha = 0.2f) else PrimaryCyan.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (task.direction == TransferDirection.SEND) Icons.Default.Upload else Icons.Default.Download,
                            contentDescription = null,
                            tint = if (task.direction == TransferDirection.SEND) PrimaryBlue else PrimaryCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "${if (task.direction == TransferDirection.SEND) "إرسال إلى" else "استقبال من"}: ${task.deviceName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = when (task.status) {
                    TransferStatus.COMPLETED -> PrimaryCyan
                    TransferStatus.PAUSED -> Color(0xFFF59E0B)
                    else -> PrimaryBlue
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${task.transferredBytes / (1024 * 1024)} / ${task.totalBytes / (1024 * 1024)} ميغابايت (${(progressRatio * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark,
                    fontWeight = FontWeight.SemiBold
                )

                // Resumable Action Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (task.status) {
                        TransferStatus.IN_PROGRESS -> {
                            Button(
                                onClick = onPause,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إيقاف مؤقت", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        TransferStatus.PAUSED, TransferStatus.FAILED -> {
                            Button(
                                onClick = onResume,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.surface)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استئناف النقل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                            }
                        }
                        TransferStatus.COMPLETED -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "مكتمل بنجاح",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
