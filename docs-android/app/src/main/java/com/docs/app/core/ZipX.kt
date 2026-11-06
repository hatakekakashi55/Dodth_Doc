package com.docs.app.core

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Minimal ZIP authoring helpers (pure java.util.zip). */
object ZipX {

    /** Writes [entries] (name -> bytes) into a single archive at [output]. */
    fun writeZip(entries: List<Pair<String, ByteArray>>, output: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zos ->
            for ((name, bytes) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
    }

    /** Zips existing files, using [entryNames] as archive member names. */
    fun zipFiles(files: List<File>, entryNames: List<String>, output: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zos ->
            files.forEachIndexed { index, file ->
                zos.putNextEntry(ZipEntry(entryNames[index]))
                file.inputStream().use { input -> input.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
}
