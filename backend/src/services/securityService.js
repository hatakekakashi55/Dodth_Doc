/**
 * DODTH - Security Service
 * PDF encryption, decryption, and watermarking using pdf-lib.
 */

const fs = require('fs');
const { PDFDocument, rgb, StandardFonts, degrees } = require('pdf-lib');

class SecurityService {

  /**
   * Add password protection to a PDF using qpdf.
   */
  static async protectPdf(inputPath, outputPath, password) {
    const { exec } = require('child_process');
    const util = require('util');
    const execPromise = util.promisify(exec);

    try {
      // Use qpdf to encrypt with 256-bit AES
      // We set both user and owner password to the same value
      await execPromise(`qpdf --encrypt "${password}" "${password}" 256 -- "${inputPath}" "${outputPath}"`);
      return { status: 'protected' };
    } catch (err) {
      console.error('QPDF Protect Error:', err);
      throw new Error('Failed to protect PDF. Ensure it is a valid document.');
    }
  }

  /**
   * Remove password from a protected PDF using qpdf.
   */
  static async unlockPdf(inputPath, outputPath, password) {
    const { exec } = require('child_process');
    const util = require('util');
    const execPromise = util.promisify(exec);

    try {
      // Use qpdf to decrypt
      await execPromise(`qpdf --decrypt --password="${password}" "${inputPath}" "${outputPath}"`);
      return { status: 'unlocked' };
    } catch (err) {
      console.error('QPDF Unlock Error:', err);
      throw new Error('Incorrect password or invalid PDF.');
    }
  }

  /**
   * Add a diagonal text watermark to all pages.
   * The watermark is semi-transparent and centered.
   */
  static async addWatermark(inputPath, outputPath, text, opacity = 0.3) {
    const pdfBytes = fs.readFileSync(inputPath);
    const pdfDoc = await PDFDocument.load(pdfBytes);
    const font = await pdfDoc.embedFont(StandardFonts.HelveticaBold);

    const pages = pdfDoc.getPages();
    const fontSize = 50;

    for (const page of pages) {
      const { width, height } = page.getSize();
      const textWidth = font.widthOfTextAtSize(text, fontSize);

      // Center the watermark text
      const x = (width - textWidth) / 2;
      const y = height / 2;

      page.drawText(text, {
        x,
        y,
        size: fontSize,
        font,
        color: rgb(0.7, 0.7, 0.7),
        opacity,
        rotate: degrees(45),
      });
    }

    pdfDoc.setCreator('DODTH Document Suite');
    pdfDoc.setProducer('DODTH v1.0 (Watermarked)');

    const watermarkedBytes = await pdfDoc.save();
    fs.writeFileSync(outputPath, watermarkedBytes);

    return { status: 'watermarked', pages: pages.length };
  }
}

module.exports = SecurityService;
