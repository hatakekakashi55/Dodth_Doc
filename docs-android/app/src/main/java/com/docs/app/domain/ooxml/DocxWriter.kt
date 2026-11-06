package com.docs.app.domain.ooxml

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One line of output text in the generated Word document. */
data class DocxOutLine(
    val text: String,
    val bold: Boolean = false,
    val underline: Boolean = false,
    val sizePt: Float = 10.5f,
    /** Rendered as Heading 1 when > 14pt and bold — purely cosmetic for the reader. */
    val heading: Boolean = false
)

/**
 * Writes a minimal, valid .docx (WordprocessingML) file without any external
 * dependencies. Produces: content types, package relationships, document.xml,
 * and docProps for a professional file identity.
 */
object DocxWriter {

    fun write(output: File, title: String, lines: List<DocxOutLine>) {
        val documentXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
            append("<w:body>")
            for (line in lines) {
                append(paragraphXml(line))
            }
            append("""<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1280" w:right="1280" w:bottom="1280" w:left="1280" w:header="720" w:footer="720" w:gutter="0"/></w:sectPr>""")
            append("</w:body></w:document>")
        }

        val coreXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>${XmlEsc.esc(title)}</dc:title><dc:creator>Docs — Document Utility Suite</dc:creator><cp:lastModifiedBy>Docs</cp:lastModifiedBy><dcterms:created xsi:type="dcterms:W3CDTF">2026-01-01T00:00:00Z</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">2026-01-01T00:00:00Z</dcterms:modified></cp:coreProperties>"""

        val appXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Docs</Application><DocSecurity>0</DocSecurity><ScaleCrop>false</ScaleCrop><Company>Docs</Company><LinksUpToDate>false</LinksUpToDate><SharedDoc>false</SharedDoc><HyperlinksChanged>false</HyperlinksChanged><AppVersion>1.0</AppVersion></Properties>"""

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/></Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>"""

        ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zos ->
            fun put(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            put("[Content_Types].xml", contentTypes)
            put("_rels/.rels", rels)
            put("word/document.xml", documentXml)
            put("docProps/core.xml", coreXml)
            put("docProps/app.xml", appXml)
        }
    }

    private fun paragraphXml(line: DocxOutLine): String {
        val clean = XmlEsc.stripIllegal(line.text).replace("\r", "")
        if (clean.isEmpty()) return "<w:p/>"
        val sb = StringBuilder()
        sb.append("<w:p>")
        if (line.heading) {
            sb.append("""<w:pPr><w:spacing w:before="240" w:after="160"/></w:pPr>""")
        } else {
            sb.append("""<w:pPr><w:spacing w:after="120"/></w:pPr>""")
        }
        val size = (line.sizePt * 2).toInt().coerceIn(8, 192)
        val segments = clean.split('\n')
        segments.forEachIndexed { index, segment ->
            sb.append("<w:r><w:rPr>")
            if (line.bold) sb.append("<w:b/><w:bCs/>")
            if (line.underline) sb.append("""<w:u w:val="single"/>""")
            sb.append("<w:sz w:val=\"$size\"/><w:szCs w:val=\"$size\"/>")
            sb.append("</w:rPr>")
            sb.append("<w:t xml:space=\"preserve\">").append(XmlEsc.esc(segment)).append("</w:t>")
            sb.append("</w:r>")
            if (index < segments.lastIndex) sb.append("<w:r><w:br/></w:r>")
        }
        sb.append("</w:p>")
        return sb.toString()
    }
}
