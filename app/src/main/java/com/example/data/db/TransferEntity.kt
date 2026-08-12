package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransferDirection {
    SEND, RECEIVE
}

enum class TransferStatus {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED
}

@Entity(tableName = "transfer_tasks")
data class TransferTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val deviceName: String,
    val deviceAddress: String,
    val direction: TransferDirection,
    val totalBytes: Long,
    val transferredBytes: Long = 0L,
    val totalFiles: Int,
    val completedFiles: Int = 0,
    val status: TransferStatus = TransferStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val isFolderTransfer: Boolean = false,
    val folderName: String? = null
)

@Entity(tableName = "transfer_files")
data class TransferFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val fileName: String,
    val relativePath: String, // Preserves folder hierarchy e.g. "Documents/Project/report.pdf"
    val fileUriOrPath: String,
    val fileSize: Long,
    val transferredBytes: Long = 0L,
    val status: TransferStatus = TransferStatus.PENDING,
    val mimeType: String = "*/*",
    val checksum: String = ""
)

@Entity(tableName = "p2p_peers")
data class PeerEntity(
    @PrimaryKey
    val deviceAddress: String,
    val deviceName: String,
    val primaryDeviceType: String = "Android Device",
    val isGroupOwner: Boolean = false,
    val lastConnected: Long = System.currentTimeMillis()
)
