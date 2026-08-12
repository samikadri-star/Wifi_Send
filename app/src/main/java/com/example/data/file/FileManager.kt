package com.example.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.FileCategory
import com.example.data.model.FileFolderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManager(private val context: Context) {

    private val TAG = "FileManager"

    /**
     * Recursively traverses a folder selected via Storage Access Framework (SAF) DocumentFile
     * and extracts all nested files preserving relative paths.
     */
    suspend fun parseFolderTree(folderUri: Uri): FileFolderItem = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, folderUri)
            ?: throw IllegalArgumentException("لم يتم العثور على المجلد المحدد")

        val rootName = rootDoc.name ?: "مجلد جديد"
        val nestedFiles = mutableListOf<FileFolderItem>()

        fun traverseDirectory(doc: DocumentFile, currentRelativePath: String) {
            val files = doc.listFiles()
            for (file in files) {
                val fileName = file.name ?: "ملف"
                val relativePath = if (currentRelativePath.isEmpty()) fileName else "$currentRelativePath/$fileName"

                if (file.isDirectory) {
                    traverseDirectory(file, relativePath)
                } else {
                    nestedFiles.add(
                        FileFolderItem(
                            id = file.uri.toString(),
                            name = fileName,
                            pathOrUri = file.uri.toString(),
                            uri = file.uri,
                            size = file.length(),
                            category = getCategoryFromMime(file.type),
                            isDirectory = false,
                            relativePath = relativePath,
                            mimeType = file.type ?: "*/*"
                        )
                    )
                }
            }
        }

        traverseDirectory(rootDoc, rootName)

        val totalFolderSize = nestedFiles.sumOf { it.size }

        FileFolderItem(
            id = folderUri.toString(),
            name = rootName,
            pathOrUri = folderUri.toString(),
            uri = folderUri,
            size = totalFolderSize,
            category = FileCategory.FOLDER,
            isDirectory = true,
            childCount = nestedFiles.size,
            relativePath = rootName,
            children = nestedFiles
        )
    }

    /**
     * Get sample local media files and mock folders for testing
     */
    suspend fun getLocalFilesAndFolders(): Map<FileCategory, List<FileFolderItem>> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<FileCategory, List<FileFolderItem>>()

        // 1. Folders Category
        val sampleFolders = listOf(
            FileFolderItem(
                id = "folder_documents",
                name = "مستندات المشروع الكبيرة",
                pathOrUri = "/storage/emulated/0/ProjectDocs",
                size = 450 * 1024 * 1024L, // 450 MB
                category = FileCategory.FOLDER,
                isDirectory = true,
                childCount = 28,
                relativePath = "مستندات المشروع الكبيرة",
                children = listOf(
                    FileFolderItem("f1", "تقرير_الشركة_2026.pdf", "/storage/emulated/0/ProjectDocs/تقرير_الشركة_2026.pdf", null, 12 * 1024 * 1024L, FileCategory.DOCUMENT, relativePath = "مستندات المشروع الكبيرة/تقرير_الشركة_2026.pdf"),
                    FileFolderItem("f2", "عرض_توضيحي.pptx", "/storage/emulated/0/ProjectDocs/عرض_توضيحي.pptx", null, 48 * 1024 * 1024L, FileCategory.DOCUMENT, relativePath = "مستندات المشروع الكبيرة/عرض_توضيحي.pptx")
                )
            ),
            FileFolderItem(
                id = "folder_photos",
                name = "مجلد الصور والميديا 4K",
                pathOrUri = "/storage/emulated/0/Media4K",
                size = 1200 * 1024 * 1024L, // 1.2 GB
                category = FileCategory.FOLDER,
                isDirectory = true,
                childCount = 142,
                relativePath = "مجلد الصور والميديا 4K",
                children = listOf(
                    FileFolderItem("f3", "صورة_جبلية.jpg", "/storage/emulated/0/Media4K/صورة_جبلية.jpg", null, 8 * 1024 * 1024L, FileCategory.IMAGE, relativePath = "مجلد الصور والميديا 4K/صورة_جبلية.jpg")
                )
            ),
            FileFolderItem(
                id = "folder_downloads",
                name = "مجلد التنزيلات الكامل",
                pathOrUri = "/storage/emulated/0/Downloads",
                size = 850 * 1024 * 1024L,
                category = FileCategory.FOLDER,
                isDirectory = true,
                childCount = 53,
                relativePath = "مجلد التنزيلات الكامل"
            )
        )
        map[FileCategory.FOLDER] = sampleFolders

        // 2. Images Category
        val sampleImages = listOf(
            FileFolderItem("img1", "خلفية_طبيعية_عالية_الوضوح.png", "internal_img_1", null, 6 * 1024 * 1024L, FileCategory.IMAGE, mimeType = "image/png"),
            FileFolderItem("img2", "لقطة_شاشة_التطبيق.jpg", "internal_img_2", null, 3 * 1024 * 1024L, FileCategory.IMAGE, mimeType = "image/jpeg"),
            FileFolderItem("img3", "صورة_تذكارية_الواي_فاي.jpg", "internal_img_3", null, 9 * 1024 * 1024L, FileCategory.IMAGE, mimeType = "image/jpeg")
        )
        map[FileCategory.IMAGE] = sampleImages

        // 3. Videos Category
        val sampleVideos = listOf(
            FileFolderItem("vid1", "فيديو_تجريبي_4K_سريع.mp4", "internal_vid_1", null, 350 * 1024 * 1024L, FileCategory.VIDEO, mimeType = "video/mp4"),
            FileFolderItem("vid2", "شرح_ميزة_استئناف_التحميل.mp4", "internal_vid_2", null, 120 * 1024 * 1024L, FileCategory.VIDEO, mimeType = "video/mp4")
        )
        map[FileCategory.VIDEO] = sampleVideos

        // 4. Apps Category
        val sampleApps = listOf(
            FileFolderItem("app1", "تطبيق_الواي_فاي_دايركت.apk", "internal_app_1", null, 42 * 1024 * 1024L, FileCategory.APP, mimeType = "application/vnd.android.package-archive"),
            FileFolderItem("app2", "حزمة_أدوات_سامي_القادري.apk", "internal_app_2", null, 68 * 1024 * 1024L, FileCategory.APP, mimeType = "application/vnd.android.package-archive")
        )
        map[FileCategory.APP] = sampleApps

        // 5. Documents Category
        val sampleDocs = listOf(
            FileFolderItem("doc1", "دليل_استخدام_الواي_فاي_دايركت.pdf", "internal_doc_1", null, 14 * 1024 * 1024L, FileCategory.DOCUMENT, mimeType = "application/pdf"),
            FileFolderItem("doc2", "مخطط_السرعة_والشبكات.docx", "internal_doc_2", null, 5 * 1024 * 1024L, FileCategory.DOCUMENT, mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        )
        map[FileCategory.DOCUMENT] = sampleDocs

        map
    }

    private fun getCategoryFromMime(mime: String?): FileCategory {
        if (mime == null) return FileCategory.OTHER
        return when {
            mime.startsWith("image/") -> FileCategory.IMAGE
            mime.startsWith("video/") -> FileCategory.VIDEO
            mime.startsWith("audio/") -> FileCategory.AUDIO
            mime.contains("pdf") || mime.contains("document") || mime.contains("text") -> FileCategory.DOCUMENT
            mime.contains("android.package-archive") -> FileCategory.APP
            else -> FileCategory.OTHER
        }
    }
}
