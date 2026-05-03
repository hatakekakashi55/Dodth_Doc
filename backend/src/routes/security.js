/**
 * DODTH - Security Routes
 * Handles PDF protection, unlocking, and watermarking.
 */

const express = require('express');
const router = express.Router();
const path = require('path');
const { createUpload, getOutputDir } = require('../uploadConfig');
const securityService = require('../services/securityService');

const upload = createUpload();

// --- Protect PDF (add password) ---
router.post('/protect', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    if (path.extname(req.file.originalname).toLowerCase() !== '.pdf') {
      return res.status(400).json({ detail: 'Please upload a PDF file' });
    }
    if (!req.body.password) {
      return res.status(400).json({ detail: 'Password is required' });
    }

    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, '.pdf') + '_protected.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    await securityService.protectPdf(req.file.path, outputPath, req.body.password);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('Protect error:', err);
    res.status(500).json({ detail: `Protection failed: ${err.message}` });
  }
});

// --- Unlock PDF (remove password) ---
router.post('/unlock', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    if (path.extname(req.file.originalname).toLowerCase() !== '.pdf') {
      return res.status(400).json({ detail: 'Please upload a PDF file' });
    }
    if (!req.body.password) {
      return res.status(400).json({ detail: 'Password is required' });
    }

    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, '.pdf') + '_unlocked.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    await securityService.unlockPdf(req.file.path, outputPath, req.body.password);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('Unlock error:', err);
    res.status(500).json({ detail: `Unlock failed: ${err.message}` });
  }
});

// --- Watermark PDF ---
router.post('/watermark', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ detail: 'No file uploaded' });
    if (path.extname(req.file.originalname).toLowerCase() !== '.pdf') {
      return res.status(400).json({ detail: 'Please upload a PDF file' });
    }
    if (!req.body.text) {
      return res.status(400).json({ detail: 'Watermark text is required' });
    }

    const opacity = parseFloat(req.body.opacity) || 0.3;
    const outputDir = getOutputDir(req.sessionId);
    const outputFilename = path.basename(req.file.originalname, '.pdf') + '_watermarked.pdf';
    const outputPath = path.join(outputDir, outputFilename);

    await securityService.addWatermark(req.file.path, outputPath, req.body.text, opacity);
    res.download(outputPath, outputFilename);
  } catch (err) {
    console.error('Watermark error:', err);
    res.status(500).json({ detail: `Watermark failed: ${err.message}` });
  }
});

module.exports = router;
