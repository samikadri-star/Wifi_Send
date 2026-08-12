package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.*
import com.example.data.file.FileManager
import com.example.data.model.FileCategory
import com.example.data.model.FileFolderItem
import com.example.data.model.TransferProgress
import com.example.data.model.WifiP2pDeviceItem
import com.example.data.p2p.SocketTransferEngine
import com.example.data.p2p.WifiP2pManagerHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class TransferRepository(
    private val context: Context,
    private val transferDao: TransferDao
) {
    val wifiP2pHelper = WifiP2pManagerHelper(context)
    val socketEngine = SocketTransferEngine(context, transferDao)
    val fileManager = FileManager(context)

    val allTasks: Flow<List<TransferTaskEntity>> = transferDao.getAllTasks()
    val allPeers: Flow<List<PeerEntity>> = transferDao.getAllPeers()
    val transferProgress: StateFlow<TransferProgress> = socketEngine.transferProgress
    val discoveredPeers: StateFlow<List<WifiP2pDeviceItem>> = wifiP2pHelper.discoveredPeers
    val connectedDevice: StateFlow<WifiP2pDeviceItem?> = wifiP2pHelper.connectedDevice

    fun startPeerDiscovery() {
        wifiP2pHelper.registerReceiver()
        wifiP2pHelper.startDiscovery()
    }

    fun stopPeerDiscovery() {
        wifiP2pHelper.stopDiscovery()
    }

    fun connectToDevice(device: WifiP2pDeviceItem, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        wifiP2pHelper.connectToPeer(device, onSuccess, onFailure)
    }

    fun disconnectP2p() {
        wifiP2pHelper.disconnect()
    }

    suspend fun getFilesForTask(taskId: Long): Flow<List<TransferFileEntity>> {
        return transferDao.getFilesForTask(taskId)
    }

    suspend fun createAndSendTask(
        selectedItems: List<FileFolderItem>,
        targetDevice: WifiP2pDeviceItem?
    ): Long {
        val totalBytes = selectedItems.sumOf { it.size }
        val isFolder = selectedItems.any { it.isDirectory }
        val folderName = selectedItems.firstOrNull { it.isDirectory }?.name

        val deviceName = targetDevice?.deviceName ?: "جهاز واي فاي دايركت"
        val deviceAddress = targetDevice?.deviceAddress ?: "192.168.49.1"

        val title = if (isFolder) "إرسال مجلد: $folderName" else "إرسال ${selectedItems.size} ملفات"

        // Flatten folder contents into file entities
        val fileEntities = mutableListOf<TransferFileEntity>()

        val task = TransferTaskEntity(
            title = title,
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            direction = TransferDirection.SEND,
            totalBytes = totalBytes,
            totalFiles = 0, // will set below
            isFolderTransfer = isFolder,
            folderName = folderName,
            status = TransferStatus.PENDING
        )

        val taskId = transferDao.insertTask(task)

        for (item in selectedItems) {
            if (item.isDirectory && item.children.isNotEmpty()) {
                for (child in item.children) {
                    fileEntities.add(
                        TransferFileEntity(
                            taskId = taskId,
                            fileName = child.name,
                            relativePath = child.relativePath,
                            fileUriOrPath = child.pathOrUri,
                            fileSize = child.size,
                            mimeType = child.mimeType,
                            status = TransferStatus.PENDING
                        )
                    )
                }
            } else {
                fileEntities.add(
                    TransferFileEntity(
                        taskId = taskId,
                        fileName = item.name,
                        relativePath = item.relativePath.ifBlank { item.name },
                        fileUriOrPath = item.pathOrUri,
                        fileSize = item.size,
                        mimeType = item.mimeType,
                        status = TransferStatus.PENDING
                    )
                )
            }
        }

        transferDao.insertFiles(fileEntities)
        val updatedTask = task.copy(id = taskId, totalFiles = fileEntities.size)
        transferDao.updateTask(updatedTask)

        // Save peer
        transferDao.insertPeer(
            PeerEntity(
                deviceAddress = deviceAddress,
                deviceName = deviceName
            )
        )

        // Launch transfer engine
        socketEngine.sendTaskFiles(deviceAddress, updatedTask, fileEntities)

        return taskId
    }

    suspend fun startReceivingServer(onTaskCreated: (Long) -> Unit) {
        socketEngine.startReceiverServer(onTaskCreated)
    }

    fun pauseTransfer(taskId: Long) {
        socketEngine.pauseTransfer(taskId)
    }

    fun resumeTransfer(taskId: Long, isSender: Boolean) {
        val targetIp = connectedDevice.value?.deviceAddress ?: "192.168.49.1"
        socketEngine.resumeTransfer(taskId, targetIp, isSender)
    }

    suspend fun deleteTask(task: TransferTaskEntity) {
        transferDao.deleteFilesForTask(task.id)
        transferDao.deleteTask(task)
    }

    suspend fun parseFolderFromUri(uri: Uri): FileFolderItem {
        return fileManager.parseFolderTree(uri)
    }

    suspend fun getLocalFilesAndFolders(): Map<FileCategory, List<FileFolderItem>> {
        return fileManager.getLocalFilesAndFolders()
    }

    fun cleanup() {
        wifiP2pHelper.unregisterReceiver()
        socketEngine.stop()
    }
}
