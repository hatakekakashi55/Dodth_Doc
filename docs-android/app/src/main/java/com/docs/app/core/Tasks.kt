package com.docs.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task

/**
 * Bridges a Google Play services [Task] into a suspendable call,
 * so OCR can run inside a coroutine without extra dependencies.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener(OnCompleteListener { task ->
        val exception = task.exception
        if (exception != null) {
            cont.resumeWithException(exception)
        } else {
            @Suppress("UNCHECKED_CAST")
            cont.resume(task.result as T)
        }
    })
}

/** Runs [block] on the IO dispatcher. */
suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
