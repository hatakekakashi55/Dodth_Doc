package com.docs.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.docs.app.core.Files

/**
 * The complete Docs tool catalogue — 19 professional document utilities,
 * grouped exactly as in the web suite. Everything runs on-device.
 */
object ToolCatalog {

    private val PDF = listOf("application/pdf")
    private val WORD = listOf(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword",
        "application/octet-stream"
    )
    private val EXCEL = listOf(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel",
        "application/octet-stream"
    )
    private val PPT = listOf(
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.ms-powerpoint",
        "application/octet-stream"
    )
    private val IMAGES = listOf("image/*")
    private val HTML = listOf("text/html", "application/xhtml+xml", "application/octet-stream")

    val categories: List<ToolCategory> = listOf(

        ToolCategory(
            title = "Convert to PDF",
            tools = listOf(
                DocsTool(
                    id = "word-to-pdf", name = "Word to PDF",
                    description = "Convert DOCX to PDF with layout, headings and embedded images",
                    icon = Icons.Filled.Description, accent = 0xFF2563EB,
                    accepts = WORD, acceptLabel = ".docx, .doc", multiple = false,
                    keywords = "word docx doc convert pdf"
                ),
                DocsTool(
                    id = "excel-to-pdf", name = "Excel to PDF",
                    description = "Convert XLSX spreadsheets to clean, paginated PDF tables",
                    icon = Icons.Filled.TableChart, accent = 0xFF059669,
                    accepts = EXCEL, acceptLabel = ".xlsx, .xls", multiple = false,
                    keywords = "excel xlsx xls spreadsheet convert pdf table"
                ),
                DocsTool(
                    id = "ppt-to-pdf", name = "PPT to PDF",
                    description = "Convert PPTX slides to PDF, one slide per page",
                    icon = Icons.Filled.Slideshow, accent = 0xFFDC2626,
                    accepts = PPT, acceptLabel = ".pptx, .ppt", multiple = false,
                    keywords = "powerpoint ppt pptx slides convert pdf"
                ),
                DocsTool(
                    id = "jpg-to-pdf", name = "JPG to PDF",
                    description = "Combine JPG, PNG, WebP or BMP images into one PDF",
                    icon = Icons.Filled.Image, accent = 0xFF0284C7,
                    accepts = IMAGES, acceptLabel = ".jpg, .jpeg, .png, .webp, .bmp", multiple = true,
                    footnote = "Multiple images supported — one per page",
                    keywords = "jpg jpeg png image photo convert pdf combine"
                ),
                DocsTool(
                    id = "html-to-pdf", name = "HTML to PDF",
                    description = "Render an HTML document into a PDF file",
                    icon = Icons.Filled.Code, accent = 0xFF6366F1,
                    accepts = HTML, acceptLabel = ".html, .htm", multiple = false,
                    keywords = "html htm web page convert pdf code"
                ),
            )
        ),

        ToolCategory(
            title = "Convert from PDF",
            tools = listOf(
                DocsTool(
                    id = "pdf-to-word", name = "PDF to Word",
                    description = "Extract PDF content into an editable DOCX document",
                    icon = Icons.Filled.Article, accent = 0xFFD97706,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    keywords = "pdf word docx convert extract text edit"
                ),
                DocsTool(
                    id = "pdf-to-ppt", name = "PDF to PPT",
                    description = "Turn each PDF page into a 16:9 PowerPoint slide",
                    icon = Icons.Filled.PresentToAll, accent = 0xFFDC2626,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    keywords = "pdf powerpoint ppt pptx slides convert presentation"
                ),
                DocsTool(
                    id = "pdf-to-excel", name = "PDF to Excel",
                    description = "Extract tabular text from PDF into an XLSX spreadsheet",
                    icon = Icons.Filled.GridView, accent = 0xFF059669,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    keywords = "pdf excel xlsx spreadsheet table extract convert"
                ),
                DocsTool(
                    id = "pdf-to-jpg", name = "PDF to JPG",
                    description = "Render every PDF page into a high-resolution JPG, zipped",
                    icon = Icons.Filled.Photo, accent = 0xFFD97706,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    footnote = "Output: one JPG per page, packed as .zip",
                    keywords = "pdf jpg jpeg image export render pages"
                ),
                DocsTool(
                    id = "ocr", name = "OCR PDF",
                    description = "Recognize scanned text with on-device OCR and rebuild a searchable PDF",
                    icon = Icons.Filled.DocumentScanner, accent = 0xFF2563EB,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    footnote = "On-device ML Kit · needs the OCR model once",
                    keywords = "ocr scan scanned text recognize searchable image"
                ),
            )
        ),

        ToolCategory(
            title = "Organize PDF",
            tools = listOf(
                DocsTool(
                    id = "merge", name = "Merge PDF",
                    description = "Combine multiple PDFs into a single document, in order",
                    icon = Icons.Filled.MergeType, accent = 0xFF6366F1,
                    accepts = PDF, acceptLabel = ".pdf", multiple = true,
                    footnote = "Select 2 or more PDFs — merged in selection order",
                    keywords = "merge combine join pdf documents"
                ),
                DocsTool(
                    id = "split", name = "Split PDF",
                    description = "Extract pages into individual PDFs, packed as a zip",
                    icon = Icons.Filled.CallSplit, accent = 0xFF3B82F6,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    needsPageRange = true,
                    footnote = "Use “all”, or a range like 1,3,5-7",
                    keywords = "split extract pages pdf separate"
                ),
                DocsTool(
                    id = "rotate", name = "Rotate PDF",
                    description = "Rotate every page by 90, 180 or 270 degrees",
                    icon = Icons.Filled.RotateRight, accent = 0xFF8B5CF6,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    needsAngle = true,
                    keywords = "rotate turn orientation pdf sideways"
                ),
                DocsTool(
                    id = "compress", name = "Compress PDF",
                    description = "Rewrite and streamline a PDF to reduce file size",
                    icon = Icons.Filled.Compress, accent = 0xFFF59E0B,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    keywords = "compress shrink reduce size pdf optimize"
                ),
                DocsTool(
                    id = "add-page-numbers", name = "Page Numbers",
                    description = "Stamp subtle centered page numbers on every page",
                    icon = Icons.Filled.FormatListNumbered, accent = 0xFF6366F1,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    keywords = "page numbers paginate stamp pdf footer"
                ),
                DocsTool(
                    id = "delete-pages", name = "Remove Pages",
                    description = "Delete specific pages from a PDF document",
                    icon = Icons.Filled.PlaylistRemove, accent = 0xFF3B82F6,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    needsPageRange = true,
                    footnote = "Pages to delete, e.g. 2 or 4-6 · ranges supported",
                    keywords = "delete remove pages pdf"
                ),
            )
        ),

        ToolCategory(
            title = "PDF Security",
            tools = listOf(
                DocsTool(
                    id = "protect", name = "Protect PDF",
                    description = "Encrypt a PDF with 256-bit AES password protection",
                    icon = Icons.Filled.Lock, accent = 0xFFDC2626,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    needsPassword = true,
                    footnote = "You will need this password to open the file",
                    keywords = "protect password encrypt lock pdf security aes"
                ),
                DocsTool(
                    id = "unlock", name = "Unlock PDF",
                    description = "Remove password protection from an encrypted PDF",
                    icon = Icons.Filled.LockOpen, accent = 0xFF059669,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    needsPassword = true,
                    footnote = "Enter the current password to decrypt",
                    keywords = "unlock remove password decrypt pdf open security"
                ),
                DocsTool(
                    id = "watermark", name = "Watermark",
                    description = "Stamp a diagonal text watermark across every page",
                    icon = Icons.Filled.WaterDrop, accent = 0xFF6366F1,
                    accepts = PDF, acceptLabel = ".pdf", multiple = false,
                    needsWatermark = true,
                    keywords = "watermark stamp text pdf confidential draft"
                ),
            )
        ),
    )

    val toolCount: Int = categories.sumOf { it.tools.size }

    fun findById(id: String?): DocsTool? {
        if (id == null) return null
        categories.forEach { category ->
            category.tools.forEach { tool -> if (tool.id == id) return tool }
        }
        return null
    }

    /** Case-insensitive search across names, descriptions and keywords. */
    fun filter(query: String): List<ToolCategory> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return categories
        return categories.mapNotNull { category ->
            val matches = category.tools.filter { tool ->
                tool.name.lowercase().contains(q) ||
                    tool.description.lowercase().contains(q) ||
                    tool.keywords.contains(q) ||
                    category.title.lowercase().contains(q)
            }
            if (matches.isEmpty()) null else category.copy(tools = matches)
        }
    }
}
