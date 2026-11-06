package com.docs.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the appearance preference (system / light / dark)
 * and exposes it as a cold StateFlow for the Compose UI.
 */
object ThemeRepository {

    private const val PREFS = "docs_theme"
    private const val KEY_MODE = "theme_mode"

    const val MODE_SYSTEM = "system"
    const val MODE_LIGHT = "light"
    const val MODE_DARK = "dark"

    @Volatile private var prefs: SharedPreferences? = null
    private val _mode = MutableStateFlow(MODE_SYSTEM)
    val mode: StateFlow<String> = _mode.asStateFlow()

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _mode.value = p.getString(KEY_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
    }

    fun setMode(value: String) {
        prefs?.edit()?.putString(KEY_MODE, value)?.apply()
        _mode.value = value
    }
}
