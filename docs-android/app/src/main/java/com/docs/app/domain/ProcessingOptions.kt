package com.docs.app.domain

/** User-supplied options shared across tools. */
data class ProcessingOptions(
    val password: String = "",
    val pageRange: String = "all",
    val angle: Int = 90,
    val watermarkText: String = "",
    val watermarkOpacity: Float = 0.3f
)
