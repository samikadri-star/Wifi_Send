package com.example.data.model

data class TransferProgress(
    val taskId: Long = 0,
    val taskTitle: String = "",
    val totalBytes: Long = 0,
    val transferredBytes: Long = 0,
    val speedBytesPerSec: Double = 0.0,
    val currentFileName: String = "",
    val currentFileRelativePath: String = "",
    val currentFileTransferred: Long = 0,
    val currentFileSize: Long = 0,
    val activeFilesCount: Int = 0,
    val totalFilesCount: Int = 0,
    val isTransferring: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
) {
    val progressPercentage: Float
        get() = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val speedMBps: Double
        get() = speedBytesPerSec / (1024.0 * 1024.0)

    val remainingSeconds: Long
        get() {
            if (speedBytesPerSec <= 0 || transferredBytes >= totalBytes) return 0
            val remainingBytes = totalBytes - transferredBytes
            return (remainingBytes / speedBytesPerSec).toLong()
        }
}

data class WifiP2pDeviceItem(
    val deviceName: String,
    val deviceAddress: String,
    val primaryDeviceType: String = "Android Device",
    val status: Int = STATUS_AVAILABLE,
    val signalLevel: Int = 4, // 1 to 4
    val isGroupOwner: Boolean = false
) {
    companion object {
        const val STATUS_CONNECTED = 0
        const val STATUS_INVITED = 1
        const val STATUS_FAILED = 2
        const val STATUS_AVAILABLE = 3
        const val STATUS_UNAVAILABLE = 4
    }

    val statusTextArabic: String
        get() = when (status) {
            STATUS_CONNECTED -> "متصل"
            STATUS_INVITED -> "جاري الاتصال..."
            STATUS_FAILED -> "فشل الاتصال"
            STATUS_AVAILABLE -> "متاح للربط"
            else -> "غير متاح"
        }
}
