package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.sp
import com.example.data.model.FileCategory
import com.example.data.model.FileFolderItem
import com.example.ui.components.FolderTreeDialog
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.TransferViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesFoldersScreen(
    viewModel: TransferViewModel,
    onNavigateToScan: () -> Unit
) {
    val itemsMap by viewModel.localItemsMap.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()

    var selectedTabCategory by remember { mutableStateOf(FileCategory.FOLDER) }
    var folderPreviewItem by remember { mutableStateOf<FileFolderItem?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.addFolderFromUri(it) }
    }

    val currentTabItems = itemsMap[selectedTabCategory] ?: emptyList()
    val totalSelectedSize = selectedItems.sumOf { it.size }

    // Folder Tree Preview Modal
    folderPreviewItem?.let { folder ->
        FolderTreeDialog(
            folderItem = folder,
            onDismiss = { folderPreviewItem = null },
            onConfirmSend = {
                viewModel.toggleSelection(folder)
                onNavigateToScan()
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title & SAF Folder Picker Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "المستعرض والمجلدات",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اختر ملفات أو مجلدات كاملة للمشاركة",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }

                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة مجلد", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Category Tabs
            ScrollableTabRow(
                selectedTabIndex = FileCategory.entries.indexOf(selectedTabCategory),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                FileCategory.entries.forEach { category ->
                    val label = when (category) {
                        FileCategory.FOLDER -> "المجلدات"
                        FileCategory.IMAGE -> "الصور"
                        FileCategory.VIDEO -> "الفيديوهات"
                        FileCategory.APP -> "التطبيقات"
                        FileCategory.DOCUMENT -> "المستندات"
                        else -> "أخرى"
                    }

                    Tab(
                        selected = selectedTabCategory == category,
                        onClick = { selectedTabCategory = category },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (selectedTabCategory == category) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabCategory == category) PrimaryCyan else TextSecondaryDark
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Files & Folders List
            if (currentTabItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد عناصر في هذا القسم", color = TextSecondaryDark)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(currentTabItems) { item ->
                        val isSelected = viewModel.isItemSelected(item)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryCyan else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (item.isDirectory) {
                                        folderPreviewItem = item
                                    } else {
                                        viewModel.toggleSelection(item)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icon Badge
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (item.isDirectory) PrimaryCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item.category) {
                                            FileCategory.FOLDER -> Icons.Default.Folder
                                            FileCategory.IMAGE -> Icons.Default.Image
                                            FileCategory.VIDEO -> Icons.Default.Movie
                                            FileCategory.APP -> Icons.Default.Android
                                            FileCategory.DOCUMENT -> Icons.Default.Description
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = if (item.isDirectory) PrimaryCyan else PrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (item.isDirectory) "مجلد كامل (${item.childCount} ملفات) • ${item.size / (1024 * 1024)} MB"
                                        else "${item.size / (1024 * 1024)} ميغابايت",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleSelection(item) },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryCyan)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Bar when items selected
        if (selectedItems.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تم تحديد ${selectedItems.size} عنصر",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "الحجم الإجمالي: ${totalSelectedSize / (1024 * 1024)} ميغابايت",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryCyan
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.sendSelectedFiles()
                            onNavigateToScan()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال الآن", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
