package com.docs.app.ui.tool

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docs.app.core.Files
import com.docs.app.core.PickedFile
import com.docs.app.data.DocsTool
import com.docs.app.data.HistoryRepository
import com.docs.app.domain.ProcessingDispatcher
import com.docs.app.domain.ProcessingOptions
import com.docs.app.domain.ProcessingOutput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class WorkPhase { IDLE, PROCESSING, SUCCESS, ERROR }

data class ToolUiState(
    val files: List<PickedFile> = emptyList(),
    val password: String = "",
    val pageRange: String = "all",
    val angle: Int = 90,
    val watermarkText: String = "CONFIDENTIAL",
    val watermarkOpacity: Float = 0.3f,
    val phase: WorkPhase = WorkPhase.IDLE,
    val progress: Float = 0f,
    val output: ProcessingOutput? = null,
    val error: String? = null
)

/**
 * Drives a single tool session: file selection, option editing,
 * processing with a web-style eased progress ticker, and result handling.
 * Temporary inputs/outputs live in cacheDir and are rotated out on reset.
 */
class ToolViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ToolUiState())
    val state: StateFlow<ToolUiState> = _state.asStateFlow()

    private var workJob: Job? = null
    private var tickerJob: Job? = null
    private val history = HistoryRepository(application)

    fun setPicked(uris: List<Uri>, tool: DocsTool) {
        val app = getApplication<Application>()
        val described = uris.map { Files.describe(app, it) }
        _state.update { s ->
            val merged = if (tool.multiple) {
                (s.files + described).distinctBy { it.uri }.take(30)
            } else {
                described.take(1)
            }
            s.copy(files = merged, phase = WorkPhase.IDLE, error = null)
        }
    }

    fun removeFile(index: Int) = _state.update { s ->
        if (index in s.files.indices) s.copy(files = s.files.filterIndexed { i, _ -> i != index }) else s
    }

    fun clearFiles() = _state.update { it.copy(files = emptyList()) }

    fun setPassword(value: String) = _state.update { it.copy(password = value) }
    fun setPageRange(value: String) = _state.update { it.copy(pageRange = value) }
    fun setAngle(value: Int) = _state.update { it.copy(angle = value) }
    fun setWatermarkText(value: String) = _state.update { it.copy(watermarkText = value) }
    fun setWatermarkOpacity(value: Float) = _state.update { it.copy(watermarkOpacity = value) }

    fun process(tool: DocsTool) {
        val snapshot = _state.value
        when {
            snapshot.files.isEmpty() ->
                _state.update { it.copy(phase = WorkPhase.ERROR, error = "Select at least one file first.") }
            tool.needsPassword && snapshot.password.isBlank() ->
                _state.update { it.copy(phase = WorkPhase.ERROR, error = "Enter a password first.") }
            tool.needsWatermark && snapshot.watermarkText.isBlank() ->
                _state.update { it.copy(phase = WorkPhase.ERROR, error = "Enter watermark text first.") }
            else -> startProcessing(tool, snapshot)
        }
    }

    private fun startProcessing(tool: DocsTool, snapshot: ToolUiState) {
        workJob?.cancel()
        tickerJob?.cancel()
        val app = getApplication<Application>()

        _state.update {
            it.copy(phase = WorkPhase.PROCESSING, progress = 0f, error = null, output = null)
        }
        startTicker()

        workJob = viewModelScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) {
                    Files.copyPickedInputs(app, snapshot.files)
                }
                val options = ProcessingOptions(
                    password = snapshot.password,
                    pageRange = snapshot.pageRange.trim().ifEmpty { "all" },
                    angle = snapshot.angle,
                    watermarkText = snapshot.watermarkText.trim(),
                    watermarkOpacity = snapshot.watermarkOpacity
                )
                val result = withContext(Dispatchers.IO) {
                    ProcessingDispatcher.run(tool.id, cached, options, Files.processingDir(app))
                }
                // Best-effort: remember this output in Recents (copy lives in filesDir)
                withContext(Dispatchers.IO) {
                    runCatching {
                        history.add(result.file, result.fileName, result.mimeType, tool.id, tool.name)
                    }.onFailure { Log.w("ToolViewModel", "History save failed", it) }
                }
                tickerJob?.cancel()
                _state.update { it.copy(phase = WorkPhase.SUCCESS, progress = 1f, output = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                tickerJob?.cancel()
                Log.e("ToolViewModel", "Processing failed", e)
                _state.update {
                    it.copy(
                        phase = WorkPhase.ERROR,
                        progress = 0f,
                        error = e.message ?: "Something went wrong while processing this file."
                    )
                }
            }
        }
    }

    /** Web-style eased progress: quick ramp → slow crawl → final push → hold at 95%. */
    private fun startTicker() {
        tickerJob = viewModelScope.launch {
            var progress = 0f
            while (isActive) {
                delay(110)
                val cap = when {
                    progress < 0.25f -> 0.25f
                    progress < 0.85f -> 0.85f
                    else -> 0.95f
                }
                val speed = when {
                    progress < 0.25f -> 0.030f
                    progress < 0.55f -> 0.011f
                    progress < 0.85f -> 0.0045f
                    else -> 0.014f
                }
                progress = (progress + speed).coerceAtMost(cap)
                _state.update { it.copy(progress = progress) }
                if (progress >= 0.95f) break
            }
        }
    }

    /** Stage caption derived from the progress value (no emojis — typographic only). */
    fun stageText(progress: Float): String = when {
        progress < 0.18f -> "Reading files…"
        progress < 0.34f -> "Starting engine…"
        progress < 0.58f -> "Processing document…"
        progress < 0.78f -> "Applying formatting…"
        progress < 0.92f -> "Almost there…"
        else -> "Finalizing output…"
    }

    /** Resets to a blank session and deletes the current staged output. */
    fun reset() {
        workJob?.cancel()
        tickerJob?.cancel()
        _state.value.output?.file?.delete()
        _state.update { ToolUiState() }
    }

    override fun onCleared() {
        workJob?.cancel()
        tickerJob?.cancel()
        _state.value.output?.file?.delete()
        super.onCleared()
    }
}
