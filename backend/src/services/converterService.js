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
        await execPromise(`soffice --headless --convert-to jpg --outdir "${outputDir}" "${pagePdfPath}"`);
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

  static async pdfToWord(inputPath, outputPath) {
    const { exec } = require('child_process');
    const util = require('util');
    const execPromise = util.promisify(exec);

    try {
      await execPromise(`soffice --headless --convert-to docx:"MS Word 2007 XML" --outdir "${path.dirname(outputPath)}" "${inputPath}"`);
      const expectedName = path.basename(inputPath, path.extname(inputPath)) + '.docx';
      const actualPath = path.join(path.dirname(outputPath), expectedName);
      if (fs.existsSync(actualPath) && actualPath !== outputPath) fs.renameSync(actualPath, outputPath);
    } catch (err) {
      console.error('Soffice PDF to Word Error:', err);
      throw new Error('Failed to convert PDF to Word.');
    }
  }

  static async pdfToPpt(inputPath, outputPath) {
    const { exec } = require('child_process');
    const util = require('util');
    const execPromise = util.promisify(exec);

    try {
      await execPromise(`soffice --headless --convert-to pptx:"Impress MS PowerPoint 2007 XML" --outdir "${path.dirname(outputPath)}" "${inputPath}"`);
      const expectedName = path.basename(inputPath, path.extname(inputPath)) + '.pptx';
      const actualPath = path.join(path.dirname(outputPath), expectedName);
      if (fs.existsSync(actualPath) && actualPath !== outputPath) fs.renameSync(actualPath, outputPath);
    } catch (err) {
      console.error('Soffice PDF to PPT Error:', err);
      throw new Error('Failed to convert PDF to PowerPoint.');
    }
  }

  static async pdfToExcel(inputPath, outputPath) {
    const { exec } = require('child_process');
    const util = require('util');
    const execPromise = util.promisify(exec);

    try {
      await execPromise(`soffice --headless --convert-to xlsx:"Calc MS Excel 2007 XML" --outdir "${path.dirname(outputPath)}" "${inputPath}"`);
      const expectedName = path.basename(inputPath, path.extname(inputPath)) + '.xlsx';
      const actualPath = path.join(path.dirname(outputPath), expectedName);
      if (fs.existsSync(actualPath) && actualPath !== outputPath) fs.renameSync(actualPath, outputPath);
    } catch (err) {
      console.error('Soffice PDF to Excel Error:', err);
      throw new Error('Failed to convert PDF to Excel.');
    }
  }

  static async htmlToPdf(htmlOrUrl, outputPath) {
    const { exec } = require('child_process');
    const util = require('util');
    const execPromise = util.promisify(exec);

    try {
      // Use LibreOffice to convert HTML/URL to PDF
      await execPromise(`soffice --headless --convert-to pdf --outdir "${path.dirname(outputPath)}" "${htmlOrUrl}"`);
      const expectedName = path.basename(htmlOrUrl, path.extname(htmlOrUrl)) + '.pdf';
      const actualPath = path.join(path.dirname(outputPath), expectedName);
      if (fs.existsSync(actualPath) && actualPath !== outputPath) fs.renameSync(actualPath, outputPath);
    } catch (err) {
      console.error('Soffice HTML to PDF Error:', err);
      throw new Error('Failed to convert HTML to PDF.');
    }
  }

  static async runOcr(inputPath, outputPath) {
    const { exec } = require('child_process');
    const util = require('util');
    const execPromise = util.promisify(exec);

    try {
      // ocrmypdf --force-ocr input.pdf output.pdf
      await execPromise(`ocrmypdf --force-ocr "${inputPath}" "${outputPath}"`);
    } catch (err) {
      console.error('OCR Error:', err);
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
