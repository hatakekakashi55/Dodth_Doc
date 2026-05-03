/**
 * DODTH - Organizer Routes
 * Handles PDF merge, split, rotate, compress operations.
 */

const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
const { createUpload, getOutputDir } = require('../uploadConfig');
const organizerService = require('../services/organizerService');

const upload = createUpload();

// --- Merge PDFs ---
router.post('/merge', upload.array('files', 50), async (req, res) => {
  try {
    if (!req.files || req.files.length < 2) {
      return res.status(400).json({ detail: 'Please upload at least 2 PDF files' });
    }

    for (const file of req.files) {
      if (path.extname(file.originalname).toLowerCase() !== '.pdf') {
        return res.status(400).json({ detail: `Invalid file: ${file.originalname}. Only PDFs allowed.` });
      }
    }

    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = 'merged_document.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    const pdfPaths = req.files.map(f => f.path);
    await organizerService.mergePdfs(pdfPaths, outputPath);

    res.download(outputPath, outputFilename, (err) => {
      if (err) console.error('Download error:', err);
    });
  } catch (err) {
    console.error('Merge error:', err);
    res.status(500).json({ detail: `Merge failed: ${err.message}` });
  }
});

// --- Split PDF ---
router.post('/split', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    if (path.extname(req.file.originalname).toLowerCase() !== '.pdf') {
      return res.status(400).json({ detail: 'Please upload a PDF file' });
    }

    const pages = req.body.pages || 'all';
    const outputDir = getOutputDir(req.sessionId);
    const baseName = path.basename(req.file.originalname, '.pdf');
    const zipFilename = `${baseName}_split.zip`;
    const zipPath = path.join(outputDir, zipFilename);

    await organizerService.splitPdf(req.file.path, outputDir, pages, zipPath);
    res.download(zipPath, zipFilename);
  } catch (err) {
    console.error('Split error:', err);
    res.status(500).json({ detail: `Split failed: ${err.message}` });
  }
});

// --- Rotate PDF ---
router.post('/rotate', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    if (path.extname(req.file.originalname).toLowerCase() !== '.pdf') {
      return res.status(400).json({ detail: 'Please upload a PDF file' });
    }

    const angle = parseInt(req.body.angle) || 90;
    if (![90, 180, 270].includes(angle)) {
      return res.status(400).json({ detail: 'Angle must be 90, 180, or 270' });
    }

    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, '.pdf') + '_rotated.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    await organizerService.rotatePdf(req.file.path, outputPath, angle);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('Rotate error:', err);
    res.status(500).json({ detail: `Rotation failed: ${err.message}` });
  }
});

// --- Compress PDF ---
router.post('/compress', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    if (path.extname(req.file.originalname).toLowerCase() !== '.pdf') {
      return res.status(400).json({ detail: 'Please upload a PDF file' });
    }

    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, '.pdf') + '_compressed.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    const originalSize = fs.statSync(req.file.path).size;
    await organizerService.compressPdf(req.file.path, outputPath);
    const compressedSize = fs.statSync(outputPath).size;
    const reduction = ((1 - compressedSize / originalSize) * 100).toFixed(1);

    res.setHeader('X-Original-Size', originalSize.toString());
    res.setHeader('X-Compressed-Size', compressedSize.toString());
    res.setHeader('X-Reduction', `${reduction}%`);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('Compress error:', err);
    res.status(500).json({ detail: `Compression failed: ${err.message}` });
  }
});

module.exports = router;
