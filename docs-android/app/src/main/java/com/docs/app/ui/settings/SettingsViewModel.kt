package com.docs.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docs.app.core.Files
import com.docs.app.data.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Cache + history accounting for the Settings screen. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val history = HistoryRepository(application)

    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

    private val _historyBytes = MutableStateFlow(0L)
    val historyBytes: StateFlow<Long> = _historyBytes.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val (cache, hist) = withContext(Dispatchers.IO) {
                Files.cacheSize(getApplication()) to history.totalBytes()
            }
            _cacheBytes.value = cache
            _historyBytes.value = hist
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Files.clearProcessingCache(getApplication())
            }
            refresh()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                history.clearAll()
            }
            refresh()
        }
    }
}
