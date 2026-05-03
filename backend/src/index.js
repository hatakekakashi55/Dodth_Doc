/**
 * DODTH - Document Utility App Backend
 * Node.js + Express server for document processing.
 * Firebase-ready architecture.
 */

const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const os = require('os');

const converterRoutes = require('./routes/converter');
const organizerRoutes = require('./routes/organizer');
const securityRoutes = require('./routes/security');

const app = express();
const PORT = process.env.PORT || 8000;

// --- Ensure directories exist ---
const UPLOAD_DIR = path.join(os.tmpdir(), 'dodth_uploads');
const OUTPUT_DIR = path.join(os.tmpdir(), 'dodth_outputs');
[UPLOAD_DIR, OUTPUT_DIR].forEach(dir => {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
});

// --- Middleware ---
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// --- Routes ---
app.use('/api/convert', converterRoutes);
app.use('/api/organize', organizerRoutes);
app.use('/api/security', securityRoutes);

// --- Health Check ---
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', app: 'DODTH', version: '1.0.0', runtime: 'node.js' });
});

// --- Download processed file ---
app.get('/api/download/:sessionId/:filename', (req, res) => {
  const filePath = path.join(OUTPUT_DIR, req.params.sessionId, req.params.filename);
  if (!fs.existsSync(filePath)) {
    return res.status(404).json({ detail: 'File not found' });
  }
  res.download(filePath, req.params.filename);
});

// --- Cleanup session files ---
app.delete('/api/cleanup/:sessionId', (req, res) => {
  const sessionId = req.params.sessionId;
  const uploadSession = path.join(UPLOAD_DIR, sessionId);
  const outputSession = path.join(OUTPUT_DIR, sessionId);

  [uploadSession, outputSession].forEach(dir => {
    if (fs.existsSync(dir)) {
      fs.rmSync(dir, { recursive: true, force: true });
    }
  });

  res.json({ status: 'cleaned', sessionId });
});

// --- Auto-cleanup: remove files older than 1 hour ---
setInterval(() => {
  const ONE_HOUR = 60 * 60 * 1000;
  [UPLOAD_DIR, OUTPUT_DIR].forEach(baseDir => {
    if (!fs.existsSync(baseDir)) return;
    fs.readdirSync(baseDir).forEach(folder => {
      const folderPath = path.join(baseDir, folder);
      try {
        const stat = fs.statSync(folderPath);
        if (Date.now() - stat.mtimeMs > ONE_HOUR) {
          fs.rmSync(folderPath, { recursive: true, force: true });
        }
      } catch (e) { /* ignore */ }
    });
  });
}, 30 * 60 * 1000); // Run every 30 minutes

// --- Start Server ---
app.listen(PORT, '0.0.0.0', () => {
  console.log(`\n🚀 DODTH Backend running on port ${PORT}`);
});

module.exports = app;
