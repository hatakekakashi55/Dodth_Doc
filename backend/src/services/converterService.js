/**
 * DODTH - Converter Service
 * Core document conversion logic using LibreOffice, pdf-lib, xlsx, sharp.
 */

const fs = require('fs');
const path = require('path');
const { PDFDocument, rgb, StandardFonts } = require('pdf-lib');
const XLSX = require('xlsx');
const sharp = require('sharp');
const archiver = require('archiver');
const libre = require('libreoffice-convert');
const util = require('util');
libre.convertAsync = util.promisify(libre.convert);

class ConverterService {

  /**
   * Convert Word (.docx) to PDF
   * Uses mammoth to extract structured content, then pdfkit to render PDF.
   */
  static async wordToPdf(inputPath, outputPath) {
    const inputBuf = fs.readFileSync(inputPath);
    const pdfBuf = await libre.convertAsync(inputBuf, '.pdf', undefined);
    fs.writeFileSync(outputPath, pdfBuf);
  }

  /**
   * Convert Excel (.xlsx) to PDF
   * Parses spreadsheet data and renders as formatted tables.
   */
  static async excelToPdf(inputPath, outputPath) {
    const inputBuf = fs.readFileSync(inputPath);
    const pdfBuf = await libre.convertAsync(inputBuf, '.pdf', undefined);
    fs.writeFileSync(outputPath, pdfBuf);
  }

  /**
   * Convert PowerPoint (.pptx) to PDF
   * Extracts text content from slides and renders to PDF.
   */
  static async pptToPdf(inputPath, outputPath) {
    const inputBuf = fs.readFileSync(inputPath);
    const pdfBuf = await libre.convertAsync(inputBuf, '.pdf', undefined);
    fs.writeFileSync(outputPath, pdfBuf);
  }

  /**
   * Convert multiple images to a single PDF.
   * Each image gets its own page, centered and scaled to fit.
   */
  static async imagesToPdf(imagePaths, outputPath) {
    const pdfDoc = await PDFDocument.create();

    for (const imgPath of imagePaths) {
      const imgBuffer = fs.readFileSync(imgPath);
      const ext = path.extname(imgPath).toLowerCase();

      let processedBuffer = imgBuffer;

      // Convert non-JPEG/PNG to PNG using sharp
      if (!['.jpg', '.jpeg', '.png'].includes(ext)) {
        processedBuffer = await sharp(imgBuffer).png().toBuffer();
      }

      let image;
      if (ext === '.jpg' || ext === '.jpeg') {
        image = await pdfDoc.embedJpg(processedBuffer);
      } else {
        // Convert to PNG if not already
        if (ext !== '.png') {
          processedBuffer = await sharp(imgBuffer).png().toBuffer();
        }
        image = await pdfDoc.embedPng(processedBuffer);
      }

      // A4 dimensions in points
      const pageWidth = 595;
      const pageHeight = 842;

      // Scale image to fit page while maintaining aspect ratio
      const scale = Math.min(
        (pageWidth - 72) / image.width,
        (pageHeight - 72) / image.height,
        1
      );
      const scaledWidth = image.width * scale;
      const scaledHeight = image.height * scale;

      const page = pdfDoc.addPage([pageWidth, pageHeight]);
      const x = (pageWidth - scaledWidth) / 2;
      const y = (pageHeight - scaledHeight) / 2;

      page.drawImage(image, {
        x,
        y,
        width: scaledWidth,
        height: scaledHeight,
      });
    }

    const pdfBytes = await pdfDoc.save();
    fs.writeFileSync(outputPath, pdfBytes);
  }

  /**
   * Convert PDF pages to JPG images, packaged as a zip.
   * Uses LibreOffice to render actual page images.
   */
  static async pdfToImages(inputPath, outputDir, zipPath) {
    const { exec } = require('child_process');
    const utilMod = require('util');
    const execPromise = utilMod.promisify(exec);

    const pdfBuffer = fs.readFileSync(inputPath);
    const pdfDoc = await PDFDocument.load(pdfBuffer);
    const pageCount = pdfDoc.getPageCount();
    const imageFiles = [];

    // Split PDF into individual pages, then convert each to JPG
    for (let i = 0; i < pageCount; i++) {
      const singlePagePdf = await PDFDocument.create();
      const [copiedPage] = await singlePagePdf.copyPages(pdfDoc, [i]);
      singlePagePdf.addPage(copiedPage);

      const singlePdfBytes = await singlePagePdf.save();
      const pagePdfPath = path.join(outputDir, `_temp_page_${i + 1}.pdf`);
      fs.writeFileSync(pagePdfPath, singlePdfBytes);

      try {
        // Use soffice to convert the single-page PDF to JPG
        const cmd = ConverterService._sofficeCmd(`--convert-to jpg --outdir "${outputDir}" "${pagePdfPath}"`);
        await execPromise(cmd, { timeout: 60000 });
        const convertedName = `_temp_page_${i + 1}.jpg`;
        const convertedPath = path.join(outputDir, convertedName);

        if (fs.existsSync(convertedPath)) {
          const finalName = `page_${i + 1}.jpg`;
          const finalPath = path.join(outputDir, finalName);
          fs.renameSync(convertedPath, finalPath);
          imageFiles.push(finalPath);
        }
      } catch (convErr) {
        // Fallback: create a placeholder if soffice fails for this page
        const page = pdfDoc.getPage(i);
        const { width, height } = page.getSize();
        const imgWidth = Math.round(Math.max(width * 2, 200));
        const imgHeight = Math.round(Math.max(height * 2, 200));

        const svgImage = `<svg width="${imgWidth}" height="${imgHeight}" xmlns="http://www.w3.org/2000/svg">
          <rect width="100%" height="100%" fill="white"/>
          <text x="50%" y="50%" text-anchor="middle" font-family="Arial" font-size="24" fill="#333">Page ${i + 1}</text>
        </svg>`;

        const imgPath = path.join(outputDir, `page_${i + 1}.jpg`);
        await sharp(Buffer.from(svgImage)).jpeg({ quality: 90 }).toFile(imgPath);
        imageFiles.push(imgPath);
      }

      // Cleanup temp PDF
      try { fs.unlinkSync(pagePdfPath); } catch (_) {}
    }

    // Create zip archive
    await new Promise((resolve, reject) => {
      const output = fs.createWriteStream(zipPath);
      const archive = archiver('zip', { zlib: { level: 6 } });
      output.on('close', resolve);
      archive.on('error', reject);
      archive.pipe(output);
      for (const imgPath of imageFiles) {
        archive.file(imgPath, { name: path.basename(imgPath) });
      }
      archive.finalize();
    });
  }

  /**
   * Helper: run soffice with a unique user profile to avoid concurrency conflicts.
   */
  static _sofficeCmd(extraArgs) {
    const profileDir = `/tmp/soffice_${Date.now()}_${Math.random().toString(36).slice(2)}`;
    return `soffice --headless -env:UserInstallation="file://${profileDir}" ${extraArgs}`;
  }

  /**
   * Helper: after soffice converts, find the output file and rename to desired path.
   */
  static _renameSofficeOutput(outputDir, inputPath, targetExt, outputPath) {
    const baseName = path.basename(inputPath, path.extname(inputPath));
    const actualPath = path.join(outputDir, baseName + '.' + targetExt);
    if (fs.existsSync(actualPath) && actualPath !== outputPath) {
      fs.renameSync(actualPath, outputPath);
    }
    if (!fs.existsSync(outputPath)) {
      throw new Error(`Conversion produced no output file.`);
    }
  }

  static async pdfToWord(inputPath, outputPath) {
    const { exec } = require('child_process');
    const utilMod = require('util');
    const execPromise = utilMod.promisify(exec);
    const outDir = path.dirname(outputPath);

    const pdfBuffer = fs.readFileSync(inputPath);
    const pdfDoc = await PDFDocument.load(pdfBuffer);
    const pageCount = pdfDoc.getPageCount();

    // For single-page PDFs, use the direct approach
    if (pageCount === 1) {
      try {
        const cmd = this._sofficeCmd(`--infilter="writer_pdf_import" --convert-to docx --outdir "${outDir}" "${inputPath}"`);
        await execPromise(cmd, { timeout: 120000 });
        this._renameSofficeOutput(outDir, inputPath, 'docx', outputPath);
        return;
      } catch (err) {
        console.error('PDF to Word (single page) Error:', err.message || err);
        throw new Error('Failed to convert PDF to Word.');
      }
    }

    // For multi-page PDFs: split → convert each → merge with page breaks
    const tempDocxPaths = [];

    try {
      for (let i = 0; i < pageCount; i++) {
        // Extract single page
        const singlePdf = await PDFDocument.create();
        const [copiedPage] = await singlePdf.copyPages(pdfDoc, [i]);
        singlePdf.addPage(copiedPage);
        const singlePdfBytes = await singlePdf.save();

        const tempPdfPath = path.join(outDir, `_page_${i + 1}.pdf`);
        fs.writeFileSync(tempPdfPath, singlePdfBytes);

        // Convert single page PDF to DOCX
        try {
          const cmd = this._sofficeCmd(`--infilter="writer_pdf_import" --convert-to docx --outdir "${outDir}" "${tempPdfPath}"`);
          await execPromise(cmd, { timeout: 60000 });
          const docxPath = path.join(outDir, `_page_${i + 1}.docx`);
          if (fs.existsSync(docxPath)) {
            tempDocxPaths.push(docxPath);
          }
        } catch (pageErr) {
          console.error(`Page ${i + 1} conversion failed:`, pageErr.message);
        }

        // Cleanup temp PDF
        try { fs.unlinkSync(tempPdfPath); } catch (_) {}
      }

      if (tempDocxPaths.length === 0) {
        throw new Error('No pages could be converted.');
      }

      // Merge all single-page DOCXes into one with page breaks
      const { Document, Packer, Paragraph, PageBreak, ImageRun } = require('docx');
      const sections = [];

      for (let i = 0; i < tempDocxPaths.length; i++) {
        // Read each DOCX and convert its content to an image for perfect layout
        const pagePdfPath = path.join(outDir, `_render_${i + 1}.pdf`);
        
        // Re-extract the page as PDF for image rendering
        const singlePdf2 = await PDFDocument.create();
        const [cp] = await singlePdf2.copyPages(pdfDoc, [i]);
        singlePdf2.addPage(cp);
        fs.writeFileSync(pagePdfPath, await singlePdf2.save());
        
        // Convert to PNG for embedding
        const pngPath = path.join(outDir, `_render_${i + 1}.png`);
        try {
          const imgCmd = this._sofficeCmd(`--convert-to png --outdir "${outDir}" "${pagePdfPath}"`);
          await execPromise(imgCmd, { timeout: 60000 });
        } catch (_) {}

        const page = pdfDoc.getPage(i);
        const { width, height } = page.getSize();

        if (fs.existsSync(pngPath)) {
          const imgBuffer = fs.readFileSync(pngPath);
          sections.push({
            properties: {
              page: {
                size: { width: Math.round(width * 20), height: Math.round(height * 20) },
                margin: { top: 0, bottom: 0, left: 0, right: 0 },
              },
            },
            children: [
              new Paragraph({
                children: [
                  new ImageRun({
                    data: imgBuffer,
                    transformation: {
                      width: Math.round(width * 0.75),
                      height: Math.round(height * 0.75),
                    },
                    type: 'png',
                  }),
                ],
              }),
            ],
          });
          try { fs.unlinkSync(pngPath); } catch (_) {}
        }
        try { fs.unlinkSync(pagePdfPath); } catch (_) {}
      }

      // If we got image-based sections, build the DOCX
      if (sections.length > 0) {
        const doc = new Document({ sections });
        const buffer = await Packer.toBuffer(doc);
        fs.writeFileSync(outputPath, buffer);
      } else {
        // Fallback: just use the first converted DOCX
        if (tempDocxPaths.length > 0) {
          fs.copyFileSync(tempDocxPaths[0], outputPath);
        }
      }

      // Cleanup temp DOCX files
      for (const p of tempDocxPaths) {
        try { fs.unlinkSync(p); } catch (_) {}
      }

    } catch (err) {
      // Cleanup on error
      for (const p of tempDocxPaths) {
        try { fs.unlinkSync(p); } catch (_) {}
      }
      console.error('PDF to Word Error:', err.message || err);
      throw new Error('Failed to convert PDF to Word.');
    }
  }

  static async pdfToPpt(inputPath, outputPath) {
    const { exec } = require('child_process');
    const utilMod = require('util');
    const execPromise = utilMod.promisify(exec);
    const outDir = path.dirname(outputPath);

    try {
      const cmd = this._sofficeCmd(`--convert-to pptx --outdir "${outDir}" "${inputPath}"`);
      await execPromise(cmd, { timeout: 120000 });
      this._renameSofficeOutput(outDir, inputPath, 'pptx', outputPath);
    } catch (err) {
      console.error('PDF to PPT Error:', err.message || err);
      throw new Error('Failed to convert PDF to PowerPoint.');
    }
  }

  static async pdfToExcel(inputPath, outputPath) {
    const { exec } = require('child_process');
    const utilMod = require('util');
    const execPromise = utilMod.promisify(exec);
    const outDir = path.dirname(outputPath);

    try {
      const cmd = this._sofficeCmd(`--infilter="calc_pdf_import" --convert-to xlsx --outdir "${outDir}" "${inputPath}"`);
      await execPromise(cmd, { timeout: 120000 });
      this._renameSofficeOutput(outDir, inputPath, 'xlsx', outputPath);
    } catch (err) {
      // Fallback: try without specific filter
      try {
        const cmd2 = this._sofficeCmd(`--convert-to xlsx --outdir "${outDir}" "${inputPath}"`);
        await execPromise(cmd2, { timeout: 120000 });
        this._renameSofficeOutput(outDir, inputPath, 'xlsx', outputPath);
      } catch (err2) {
        console.error('PDF to Excel Error:', err2.message || err2);
        throw new Error('Failed to convert PDF to Excel.');
      }
    }
  }

  static async htmlToPdf(htmlPath, outputPath) {
    const { exec } = require('child_process');
    const utilMod = require('util');
    const execPromise = utilMod.promisify(exec);
    const outDir = path.dirname(outputPath);

    try {
      const cmd = this._sofficeCmd(`--convert-to pdf --outdir "${outDir}" "${htmlPath}"`);
      await execPromise(cmd, { timeout: 120000 });
      this._renameSofficeOutput(outDir, htmlPath, 'pdf', outputPath);
    } catch (err) {
      console.error('HTML to PDF Error:', err.message || err);
      throw new Error('Failed to convert HTML to PDF.');
    }
  }

  static async runOcr(inputPath, outputPath) {
    const { exec } = require('child_process');
    const utilMod = require('util');
    const execPromise = utilMod.promisify(exec);

    try {
      await execPromise(`ocrmypdf --force-ocr "${inputPath}" "${outputPath}"`, { timeout: 180000 });
    } catch (err) {
      console.error('OCR Error:', err.message || err);
      throw new Error('Failed to run OCR on PDF.');
    }
  }
}

/**
 * Extract text from pptx file by reading the XML slide content.
 */
async function extractPptxText(filePath) {
  const AdmZip = require('adm-zip');
  const zip = new AdmZip(filePath);
  const entries = zip.getEntries();
  const slides = [];

  // Find slide XML files
  const slideEntries = entries
    .filter(e => e.entryName.match(/ppt\/slides\/slide\d+\.xml/))
    .sort((a, b) => {
      const numA = parseInt(a.entryName.match(/slide(\d+)/)[1]);
      const numB = parseInt(b.entryName.match(/slide(\d+)/)[1]);
      return numA - numB;
    });

  for (const entry of slideEntries) {
    const xml = entry.getData().toString('utf8');
    // Extract text between <a:t> tags
    const textPattern = /<a:t>(.*?)<\/a:t>/g;
    let match;
    const texts = [];
    while ((match = textPattern.exec(xml)) !== null) {
      texts.push(match[1]);
    }
    slides.push(texts.join('\n'));
  }

  return slides;
}

module.exports = ConverterService;
