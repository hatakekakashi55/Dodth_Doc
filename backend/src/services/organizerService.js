/**
 * DODTH - Organizer Service
 * Core logic for PDF merge, split, rotate, compress using pdf-lib.
 */

const fs = require('fs');
const path = require('path');
const { PDFDocument, degrees } = require('pdf-lib');
const archiver = require('archiver');

class OrganizerService {

  /**
   * Merge multiple PDFs into a single document.
   * Preserves page sizes, annotations, and structure.
   */
  static async mergePdfs(pdfPaths, outputPath) {
    const mergedPdf = await PDFDocument.create();

    for (const pdfPath of pdfPaths) {
      const pdfBytes = fs.readFileSync(pdfPath);
      const pdf = await PDFDocument.load(pdfBytes);
      const copiedPages = await mergedPdf.copyPages(pdf, pdf.getPageIndices());
      copiedPages.forEach(page => mergedPdf.addPage(page));
    }

    // Set document metadata
    mergedPdf.setTitle('Merged Document');
    mergedPdf.setCreator('DODTH Document Suite');
    mergedPdf.setProducer('DODTH v1.0');

    const mergedBytes = await mergedPdf.save();
    fs.writeFileSync(outputPath, mergedBytes);

    return {
      pageCount: mergedPdf.getPageCount(),
      fileCount: pdfPaths.length,
    };
  }

  /**
   * Split a PDF into individual page files.
   * Supports 'all' (each page), or specific pages like '1,3,5-7'.
   */
  static async splitPdf(inputPath, outputDir, pages, zipPath) {
    const pdfBytes = fs.readFileSync(inputPath);
    const pdf = await PDFDocument.load(pdfBytes);
    const totalPages = pdf.getPageCount();

    // Parse page specification
    let pageList;
    if (pages === 'all') {
      pageList = Array.from({ length: totalPages }, (_, i) => i);
    } else {
      pageList = [];
      const parts = pages.split(',');
      for (const part of parts) {
        const trimmed = part.trim();
        if (trimmed.includes('-')) {
          const [start, end] = trimmed.split('-').map(n => parseInt(n.trim()));
          for (let i = start - 1; i < Math.min(end, totalPages); i++) {
            if (i >= 0) pageList.push(i);
          }
        } else {
          const pageNum = parseInt(trimmed) - 1;
          if (pageNum >= 0 && pageNum < totalPages) {
            pageList.push(pageNum);
          }
        }
      }
    }

    const splitFiles = [];

    for (const pageIdx of pageList) {
      const newPdf = await PDFDocument.create();
      const [copiedPage] = await newPdf.copyPages(pdf, [pageIdx]);
      newPdf.addPage(copiedPage);

      const filename = `page_${pageIdx + 1}.pdf`;
      const filePath = path.join(outputDir, filename);
      const newPdfBytes = await newPdf.save();
      fs.writeFileSync(filePath, newPdfBytes);
      splitFiles.push(filePath);
    }

    // Create zip archive
    await new Promise((resolve, reject) => {
      const output = fs.createWriteStream(zipPath);
      const archive = archiver('zip', { zlib: { level: 6 } });

      output.on('close', resolve);
      archive.on('error', reject);

      archive.pipe(output);
      for (const filePath of splitFiles) {
        archive.file(filePath, { name: path.basename(filePath) });
      }
      archive.finalize();
    });

    return { totalPages, splitCount: pageList.length };
  }

  /**
   * Rotate all pages in a PDF by the specified angle.
   */
  static async rotatePdf(inputPath, outputPath, angle) {
    const pdfBytes = fs.readFileSync(inputPath);
    const pdf = await PDFDocument.load(pdfBytes);

    const pages = pdf.getPages();
    for (const page of pages) {
      const currentRotation = page.getRotation().angle;
      page.setRotation(degrees(currentRotation + angle));
    }

    const rotatedBytes = await pdf.save();
    fs.writeFileSync(outputPath, rotatedBytes);

    return { pageCount: pages.length, angle };
  }

  /**
   * Compress a PDF by re-saving with optimized settings.
   * pdf-lib strips unnecessary data during copy operations.
   */
  static async compressPdf(inputPath, outputPath) {
    const pdfBytes = fs.readFileSync(inputPath);
    const originalSize = pdfBytes.length;

    const srcPdf = await PDFDocument.load(pdfBytes);
    const compressedPdf = await PDFDocument.create();

    // Copy all pages (this strips unused objects)
    const pages = await compressedPdf.copyPages(srcPdf, srcPdf.getPageIndices());
    pages.forEach(page => compressedPdf.addPage(page));

    // Copy metadata
    compressedPdf.setCreator('DODTH Document Suite');
    compressedPdf.setProducer('DODTH v1.0 (Compressed)');

    // Save with object stream compression
    const compressedBytes = await compressedPdf.save({
      useObjectStreams: true,
      addDefaultPage: false,
    });

    fs.writeFileSync(outputPath, compressedBytes);

    const compressedSize = compressedBytes.length;
    const reduction = ((1 - compressedSize / originalSize) * 100).toFixed(1);

    return { originalSize, compressedSize, reduction: `${reduction}%` };
  }
}

module.exports = OrganizerService;
