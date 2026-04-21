package com.example.eventnote.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventnote.EventNoteRepository
import com.example.eventnote.data.DataManager
import com.example.eventnote.data.ImportResult
import com.example.eventnote.data.PasswordManager
import com.example.eventnote.data.PasswordValidationResult
import com.example.eventnote.data.local.entity.Category
import com.example.eventnote.data.local.entity.Event
import com.example.eventnote.data.local.entity.EventStatus
import com.example.eventnote.data.local.entity.Priority
import com.example.eventnote.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventNoteUIState(
    val events: List<Event> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val selectedCategoryId: Long? = null  // 当前筛选的分类
)

@HiltViewModel
class EventNoteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: EventNoteRepository,
    private val dataManager: DataManager,
    private val passwordManager: PasswordManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EventNoteUIState())
    val uiState: StateFlow<EventNoteUIState> = _uiState.asStateFlow()
    
    init {
        loadEvents()
        loadCategories()
    }
    
    // ===== Event Methods =====
    
    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.getAllEvents().collect { events ->
                    val filteredEvents = if (_uiState.value.selectedCategoryId != null) {
                        events.filter { it.categoryId == _uiState.value.selectedCategoryId }
                    } else {
                        events
                    }
                    _uiState.value = _uiState.value.copy(
                        events = filteredEvents,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载事件失败"
                )
            }
        }
    }
    
    fun insertEvent(event: Event) {
        viewModelScope.launch {
            repository.insertEvent(event)
            loadEvents()
        }
    }
    
    fun updateEvent(event: Event) {
        viewModelScope.launch {
            repository.updateEvent(event)
            loadEvents()
        }
    }
    
    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteEvent(id)
            loadEvents()
        }
    }
    
    fun searchEvents(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.searchEvents(query).collect { events ->
                    val filteredEvents = if (_uiState.value.selectedCategoryId != null) {
                        events.filter { it.categoryId == _uiState.value.selectedCategoryId }
                    } else {
                        events
                    }
                    _uiState.value = _uiState.value.copy(
                        events = filteredEvents,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "搜索失败"
                )
            }
        }
    }
    
    fun getEventsByStatus(status: EventStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.getEventsByStatus(status).collect { events ->
                    val filteredEvents = if (_uiState.value.selectedCategoryId != null) {
                        events.filter { it.categoryId == _uiState.value.selectedCategoryId }
                    } else {
                        events
                    }
                    _uiState.value = _uiState.value.copy(
                        events = filteredEvents,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "筛选失败"
                )
            }
        }
    }
    
    // ===== 导入导出功能 =====
    
    fun exportData(uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = dataManager.exportToZip(uri)
            _uiState.value = _uiState.value.copy(isLoading = false)
            onResult(result)
        }
    }
    
    fun exportDataTxt(uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = dataManager.exportToTxt(uri)
            _uiState.value = _uiState.value.copy(isLoading = false)
            onResult(result)
        }
    }
    
    fun importData(uri: Uri, onResult: (Result<ImportResult>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // 检测文件类型并选择合适的导入方法
            val result = if (isZipFile(uri)) {
                dataManager.importFromZip(uri)
            } else {
                dataManager.importFromTxt(uri)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
            loadEvents() // 重新加载数据
            onResult(result)
        }
    }
    
    fun importDataZip(uri: Uri, onResult: (Result<ImportResult>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = dataManager.importFromZip(uri)
            _uiState.value = _uiState.value.copy(isLoading = false)
            loadEvents() // 重新加载数据
            onResult(result)
        }
    }
    
    private fun isZipFile(uri: Uri): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            mimeType == "application/zip" || mimeType == "application/x-zip-compressed"
        } catch (e: Exception) {
            false
        }
    }
    
    fun getEventCount(): Int = _uiState.value.events.size
    
    // ===== 免费版限制功能 =====
    
    fun isLimitReached(): Boolean = 
        BuildConfig.FREE_MAX_EVENTS != Int.MAX_VALUE && 
        _uiState.value.events.size >= BuildConfig.FREE_MAX_EVENTS
    
    fun canCreateEvent(): Boolean = !isLimitReached()
    
    fun getMaxEvents(): Int = BuildConfig.FREE_MAX_EVENTS
    
    fun getRemainingCount(): Int = 
        if (BuildConfig.FREE_MAX_EVENTS == Int.MAX_VALUE) Int.MAX_VALUE 
        else BuildConfig.FREE_MAX_EVENTS - _uiState.value.events.size
    
    fun generateExportFileName(): String = dataManager.generateExportFileName()
    
    fun generateZipExportFileName(): String = dataManager.generateZipExportFileName()
    
    // ===== 密码功能 =====
    
    fun isPasswordSet(): Boolean = passwordManager.isPasswordSet()
    
    fun setPassword(password: String): Boolean = passwordManager.setPassword(password)
    
    fun verifyPassword(password: String): PasswordValidationResult = passwordManager.validatePassword(password)
    
    fun clearPassword() = passwordManager.clearPassword()
    
    fun clearAllData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                dataManager.clearAllData()
                loadEvents()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "所有数据已清除"
                )
                onResult(true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "清除数据失败: ${e.message}"
                )
                onResult(false)
            }
        }
    }
    
    // ===== Category Methods =====
    
    fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.getAllCategories().collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "加载分类失败: ${e.message}")
            }
        }
    }
    
    suspend fun insertCategory(category: Category): Long {
        return repository.insertCategory(category)
    }
    
    fun insertCategory(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    onResult(false)
                    return@launch
                }
                
                // 检查是否已存在相同名称的分类
                val existing = _uiState.value.categories.find { it.name == name.trim() }
                if (existing != null) {
                    _uiState.value = _uiState.value.copy(error = "分类已存在")
                    onResult(false)
                    return@launch
                }
                
                val category = Category(name = name.trim())
                repository.insertCategory(category)
                loadCategories()
                _uiState.value = _uiState.value.copy(message = "分类添加成功")
                onResult(true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "添加分类失败: ${e.message}")
                onResult(false)
            }
        }
    }
    
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
                loadCategories()
                _uiState.value = _uiState.value.copy(message = "分类已删除")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "删除分类失败: ${e.message}")
            }
        }
    }
    
    fun filterByCategory(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
        // 立即重新筛选当前事件列表，而不是等待数据库重新发射
        viewModelScope.launch {
            repository.getAllEvents().collect { allEvents ->
                val filteredEvents = if (categoryId != null) {
                    allEvents.filter { it.categoryId == categoryId }
                } else {
                    allEvents
                }
                _uiState.value = _uiState.value.copy(events = filteredEvents)
            }
        }
    }
}
