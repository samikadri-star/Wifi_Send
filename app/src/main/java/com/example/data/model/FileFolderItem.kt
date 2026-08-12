package com.example.data.model

import android.net.Uri

enum class FileCategory {
    FOLDER, IMAGE, VIDEO, AUDIO, APP, DOCUMENT, OTHER
}

data class FileFolderItem(
    val id: String,
    val name: String,
    val pathOrUri: String,
    val uri: Uri? = null,
    val size: Long,
    val category: FileCategory,
    val isDirectory: Boolean = false,
    val childCount: Int = 0,
    val mimeType: String = "*/*",
    val relativePath: String = "", // Used for folder hierarchies, e.g. "MyFolder/Sub/file.pdf"
    val children: List<FileFolderItem> = emptyList(),
    val isSelected: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)
