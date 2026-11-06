package com.docs.app.domain

import java.io.File

/** The produced document, staged in the app cache, ready to save / share / open. */
data class ProcessingOutput(
    val file: File,
    val fileName: String,
    val mimeType: String,
    /** Optional one-line summary shown on the result card (e.g. compression savings). */
    val summary: String? = null
)
