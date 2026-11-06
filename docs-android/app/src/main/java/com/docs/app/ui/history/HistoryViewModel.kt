package com.docs.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docs.app.data.HistoryItem
import com.docs.app.data.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Loads and mutates the recent-outputs store. */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository(application)

    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())
    val items: StateFlow<List<HistoryItem>> = _items.asStateFlow()

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytes: StateFlow<Long> = _totalBytes.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val (list, bytes) = withContext(Dispatchers.IO) {
                repository.list() to repository.totalBytes()
            }
            _items.value = list
            _totalBytes.value = bytes
        }
    }

    fun delete(item: HistoryItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(item.id) }
            refresh()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.clearAll() }
            refresh()
        }
    }

    fun fileFor(item: HistoryItem): File = repository.fileFor(item)
}
