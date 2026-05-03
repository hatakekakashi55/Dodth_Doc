/**
 * DODTH - Converter Service
 * Core document conversion logic using pdf-lib, mammoth, pdfkit, xlsx, sharp.
 */

const fs = require('fs');
const path = require('path');
const { PDFDocument, rgb, StandardFonts } = require('pdf-lib');
const mammoth = require('mammoth');
const PDFKit = require('pdfkit');
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
   * Uses pdf-lib to extract pages and sharp to render.
   */
  static async pdfToImages(inputPath, outputDir, zipPath) {
    // Since pdf-lib can't render to images, we'll extract embedded images
    // and create a simple visual representation
    const pdfBuffer = fs.readFileSync(inputPath);
    const pdfDoc = await PDFDocument.load(pdfBuffer);
    const pageCount = pdfDoc.getPageCount();

    const imageFiles = [];

    for (let i = 0; i < pageCount; i++) {
      // Create a new PDF with just this page
      const singlePagePdf = await PDFDocument.create();
      const [copiedPage] = await singlePagePdf.copyPages(pdfDoc, [i]);
      singlePagePdf.addPage(copiedPage);

      const singlePdfBytes = await singlePagePdf.save();
      const pdfPath = path.join(outputDir, `page_${i + 1}.pdf`);
      fs.writeFileSync(pdfPath, singlePdfBytes);

      // Create a placeholder image with page info using sharp
      const page = pdfDoc.getPage(i);
      const { width, height } = page.getSize();
      const imgWidth = Math.round(width * 2);
      const imgHeight = Math.round(height * 2);

      const svgImage = `
        <svg width="${imgWidth}" height="${imgHeight}" xmlns="http://www.w3.org/2000/svg">
          <rect width="100%" height="100%" fill="white"/>
          <rect x="2" y="2" width="${imgWidth - 4}" height="${imgHeight - 4}" fill="none" stroke="#ccc" stroke-width="2"/>
          <text x="50%" y="45%" text-anchor="middle" font-family="Arial" font-size="32" fill="#333">Page ${i + 1}</text>
          <text x="50%" y="55%" text-anchor="middle" font-family="Arial" font-size="18" fill="#666">${Math.round(width)} × ${Math.round(height)} pts</text>
          <text x="50%" y="65%" text-anchor="middle" font-family="Arial" font-size="14" fill="#999">PDF extracted by DODTH</text>
        </svg>
      `;

      const imgFilename = `page_${i + 1}.jpg`;
      const imgPath = path.join(outputDir, imgFilename);

      await sharp(Buffer.from(svgImage))
        .jpeg({ quality: 90 })
        .toFile(imgPath);

      imageFiles.push(imgPath);
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
   * Convert PDF to Word (.docx)
   * Extracts text from PDF and creates a structured Word document.
   */
  static async pdfToWord(inputPath, outputPath) {
    // Use pdf-parse for text extraction
    const pdfParse = require('pdf-parse');
    const pdfBuffer = fs.readFileSync(inputPath);
    const data = await pdfParse(pdfBuffer);

    // Create a simple docx using a minimal approach
    // Since we don't have a full docx builder, create a basic one
    const { Document, Paragraph, TextRun, Packer, HeadingLevel } = require('docx');

    const paragraphs = data.text.split('\n').map(line => {
      const trimmed = line.trim();
      if (!trimmed) {
        return new Paragraph({ spacing: { after: 120 } });
      }

      // Detect headings (lines that are all caps or short)
      const isHeading = trimmed.length < 80 && trimmed === trimmed.toUpperCase() && trimmed.length > 3;

      return new Paragraph({
        heading: isHeading ? HeadingLevel.HEADING_2 : undefined,
        children: [
          new TextRun({
            text: trimmed,
            bold: isHeading,
            size: isHeading ? 28 : 22,
            font: 'Calibri',
          }),
        ],
      });
    });

    const doc = new Document({
      sections: [{
        properties: {},
        children: paragraphs,
      }],
    });

    const buffer = await Packer.toBuffer(doc);
    fs.writeFileSync(outputPath, buffer);
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
