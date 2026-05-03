/**
 * DODTH - Converter Routes
 * Handles all document conversion endpoints.
 */

const express = require('express');
const router = express.Router();
const path = require('path');
const { createUpload, getOutputDir } = require('../uploadConfig');
const converterService = require('../services/converterService');

const upload = createUpload();

// --- Word to PDF ---
router.post('/word-to-pdf', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    const ext = path.extname(req.file.originalname).toLowerCase();
    if (!['.docx', '.doc'].includes(ext)) {
      return res.status(400).json({ detail: 'Please upload a .docx or .doc file' });
    }

    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, ext) + '.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    await converterService.wordToPdf(req.file.path, outputPath);

    res.download(outputPath, outputFilename, (err) => {
      if (err) console.error('Download error:', err);
    });
  } catch (err) {
    console.error('Word to PDF error:', err);
    res.status(500).json({ detail: `Conversion failed: ${err.message}` });
  }
});

// --- Excel to PDF ---
router.post('/excel-to-pdf', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    const ext = path.extname(req.file.originalname).toLowerCase();
    if (!['.xlsx', '.xls'].includes(ext)) {
      return res.status(400).json({ detail: 'Please upload a .xlsx or .xls file' });
    }

    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, ext) + '.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    await converterService.excelToPdf(req.file.path, outputPath);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('Excel to PDF error:', err);
    res.status(500).json({ detail: `Conversion failed: ${err.message}` });
  }
});

// --- PPT to PDF ---
router.post('/ppt-to-pdf', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    const ext = path.extname(req.file.originalname).toLowerCase();
    if (!['.pptx', '.ppt'].includes(ext)) {
      return res.status(400).json({ detail: 'Please upload a .pptx or .ppt file' });
    }

    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, ext) + '.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    await converterService.pptToPdf(req.file.path, outputPath);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('PPT to PDF error:', err);
    res.status(500).json({ detail: `Conversion failed: ${err.message}` });
  }
});

// --- JPG/Images to PDF ---
router.post('/jpg-to-pdf', upload.array('files', 50), async (req, res) => {
  try {
    if (!req.files || req.files.length === 0) {
      return res.status(400).json({ detail: 'No files uploaded' });
    }

    const validExts = ['.jpg', '.jpeg', '.png', '.bmp', '.tiff', '.webp'];
    for (const file of req.files) {
      const ext = path.extname(file.originalname).toLowerCase();
      if (!validExts.includes(ext)) {
        return res.status(400).json({ detail: `Invalid image file: ${file.originalname}` });
      }
    }

    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = 'images_combined.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    const imagePaths = req.files.map(f => f.path);
    await converterService.imagesToPdf(imagePaths, outputPath);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('JPG to PDF error:', err);
    res.status(500).json({ detail: `Conversion failed: ${err.message}` });
  }
});

// --- PDF to JPG ---
router.post('/pdf-to-jpg', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    if (path.extname(req.file.originalname).toLowerCase() !== '.pdf') {
      return res.status(400).json({ detail: 'Please upload a .pdf file' });
    }

    const outputDir = getOutputDir(req.sessionId);
    const baseName = path.basename(req.file.originalname, '.pdf');
    const zipFilename = `${baseName}_images.zip`;
    const zipPath = path.join(outputDir, zipFilename);

    await converterService.pdfToImages(req.file.path, outputDir, zipPath);
    res.download(zipPath, zipFilename);
  } catch (err) {
    console.error('PDF to JPG error:', err);
    res.status(500).json({ detail: `Conversion failed: ${err.message}` });
  }
});

// --- PDF to Word ---
router.post('/pdf-to-word', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    if (path.extname(req.file.originalname).toLowerCase() !== '.pdf') {
      return res.status(400).json({ detail: 'Please upload a .pdf file' });
    }
    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, '.pdf') + '.docx';
    const outputPath = path.join(outputDir, outputFilename);
    await converterService.pdfToWord(req.file.path, outputPath);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('PDF to Word error:', err);
    res.status(500).json({ detail: `Conversion failed: ${err.message}` });
  }
});

// --- PDF to PPT ---
router.post('/pdf-to-ppt', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, '.pdf') + '.pptx';
    const outputPath = path.join(outputDir, outputFilename);
    await converterService.pdfToPpt(req.file.path, outputPath);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('PDF to PPT error:', err);
    res.status(500).json({ detail: `Conversion failed: ${err.message}` });
  }
});

// --- PDF to Excel ---
router.post('/pdf-to-excel', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, '.pdf') + '.xlsx';
    const outputPath = path.join(outputDir, outputFilename);
    await converterService.pdfToExcel(req.file.path, outputPath);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('PDF to Excel error:', err);
    res.status(500).json({ detail: `Conversion failed: ${err.message}` });
  }
});

// --- HTML to PDF ---
router.post('/html-to-pdf', upload.single('file'), async (req, res) => {
  try {
    // Note: Can be a file upload of HTML or a URL (for now handling file)
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, path.extname(req.file.originalname)) + '.pdf';
    const outputPath = path.join(outputDir, outputFilename);
    await converterService.htmlToPdf(req.file.path, outputPath);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('HTML to PDF error:', err);
    res.status(500).json({ detail: `Conversion failed: ${err.message}` });
  }
});

// --- OCR ---
router.post('/ocr', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, '.pdf') + '_ocr.pdf';
    const outputPath = path.join(outputDir, outputFilename);
    await converterService.runOcr(req.file.path, outputPath);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('OCR error:', err);
    res.status(500).json({ detail: `OCR failed: ${err.message}` });
  }
});

module.exports = router;
