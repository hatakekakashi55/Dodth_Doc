package com.docs.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.docs.app.data.ThemeRepository

/**
 * Application entry point.
 * Initialises the on-device PDF engine (PDFBox for Android) and theme preferences.
 */
class DocsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        ThemeRepository.init(this)
    }
}
