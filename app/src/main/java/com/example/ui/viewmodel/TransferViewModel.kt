package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.TransferTaskEntity
import com.example.data.model.FileCategory
import com.example.data.model.FileFolderItem
import com.example.data.model.TransferProgress
import com.example.data.model.WifiP2pDeviceItem
import com.example.data.repository.TransferRepository
import com.example.ui.theme.AppThemeStyle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransferViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransferRepository
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(getInitialTheme())
    val currentTheme: StateFlow<AppThemeStyle> = _currentTheme.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransferRepository(application, database.transferDao())
    }

    private fun getInitialTheme(): AppThemeStyle {
        val savedName = prefs.getString("theme_style", AppThemeStyle.IMMERSIVE_CYAN.name)
        return try {
            AppThemeStyle.valueOf(savedName ?: AppThemeStyle.IMMERSIVE_CYAN.name)
        } catch (e: Exception) {
            AppThemeStyle.IMMERSIVE_CYAN
        }
    }

    fun setThemeStyle(themeStyle: AppThemeStyle) {
        _currentTheme.value = themeStyle
        prefs.edit().putString("theme_style", themeStyle.name).apply()
        _uiMessage.value = "تم تغيير مظهر التطبيق إلى: ${themeStyle.titleArabic}"
    }


    val tasks: StateFlow<List<TransferTaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transferProgress: StateFlow<TransferProgress> = repository.transferProgress
    val discoveredPeers: StateFlow<List<WifiP2pDeviceItem>> = repository.discoveredPeers
    val connectedDevice: StateFlow<WifiP2pDeviceItem?> = repository.connectedDevice

    private val _localItemsMap = MutableStateFlow<Map<FileCategory, List<FileFolderItem>>>(emptyMap())
    val localItemsMap: StateFlow<Map<FileCategory, List<FileFolderItem>>> = _localItemsMap.asStateFlow()

    private val _selectedItems = MutableStateFlow<List<FileFolderItem>>(emptyList())
    val selectedItems: StateFlow<List<FileFolderItem>> = _selectedItems.asStateFlow()

    private val _selectedDevice = MutableStateFlow<WifiP2pDeviceItem?>(null)
    val selectedDevice: StateFlow<WifiP2pDeviceItem?> = _selectedDevice.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _isServerListening = MutableStateFlow(false)
    val isServerListening: StateFlow<Boolean> = _isServerListening.asStateFlow()

    init {
        loadLocalFiles()
        repository.startPeerDiscovery()
    }

    fun loadLocalFiles() {
        viewModelScope.launch {
            val map = repository.getLocalFilesAndFolders()
            _localItemsMap.value = map
        }
    }

    fun toggleSelection(item: FileFolderItem) {
        val current = _selectedItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == item.id }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(item)
        }
        _selectedItems.value = current
    }

    fun isItemSelected(item: FileFolderItem): Boolean {
        return _selectedItems.value.any { it.id == item.id }
    }

    fun clearSelection() {
        _selectedItems.value = emptyList()
    }

    fun addFolderFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val folderItem = repository.parseFolderFromUri(uri)
                val currentFolders = _localItemsMap.value[FileCategory.FOLDER]?.toMutableList() ?: mutableListOf()
                currentFolders.add(0, folderItem)
                
                val newMap = _localItemsMap.value.toMutableMap()
                newMap[FileCategory.FOLDER] = currentFolders
                _localItemsMap.value = newMap

                // Auto select newly added SAF folder
                toggleSelection(folderItem)
                _uiMessage.value = "تمت إضافة المجلد: ${folderItem.name} (${folderItem.childCount} ملف)"
            } catch (e: Exception) {
                _uiMessage.value = "خطأ أثناء قراءة المجلد: ${e.message}"
            }
        }
    }

    fun selectPeerDevice(device: WifiP2pDeviceItem) {
        _selectedDevice.value = device
        repository.connectToDevice(
            device = device,
            onSuccess = {
                _uiMessage.value = "تم الاقتران بـ ${device.deviceName} بنجاح!"
            },
            onFailure = { error ->
                _uiMessage.value = error
            }
        )
    }

    fun startPeerScan() {
        repository.startPeerDiscovery()
        _uiMessage.value = "جاري البحث عن أجهزة الواي فاي دايركت المجاورة..."
    }

    fun sendSelectedFiles() {
        val items = _selectedItems.value
        if (items.isEmpty()) {
            _uiMessage.value = "الرجاء تحديد ملفات أو مجلدات أولاً للإرسال"
            return
        }

        val target = _selectedDevice.value ?: connectedDevice.value
        viewModelScope.launch {
            repository.createAndSendTask(items, target)
            _uiMessage.value = "بدأ إرسال ${items.size} عنصر بسرعه فائقة..."
            clearSelection()
        }
    }

    fun startReceivingMode() {
        viewModelScope.launch {
            _isServerListening.value = true
            repository.startReceivingServer { taskId ->
                _uiMessage.value = "تم بدء استقبال الملفات مجدداً"
            }
            _uiMessage.value = "التطبيق جاهز الآن لاستقبال الملفات والمجلدات عبر الواي فاي دايركت"
        }
    }

    fun pauseTransfer(taskId: Long) {
        repository.pauseTransfer(taskId)
        _uiMessage.value = "تم إيقاف التحميل مؤقتاً"
    }

    fun resumeTransfer(taskId: Long, isSender: Boolean) {
        repository.resumeTransfer(taskId, isSender)
        _uiMessage.value = "جاري استئناف التحميل من نقطة التوقف..."
    }

    fun deleteTask(task: TransferTaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _uiMessage.value = "تم حذف السجل"
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
