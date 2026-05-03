/**
 * DODTH - Multer upload configuration
 * Centralized file upload handling with session-based directories.
 */

const multer = require('multer');
const path = require('path');
const fs = require('fs');
const os = require('os');
const crypto = require('crypto');

const UPLOAD_DIR = path.join(os.tmpdir(), 'dodth_uploads');

/**
 * Creates a multer instance that saves files into a unique session directory.
 * The session ID is attached to req.sessionId after upload.
 */
function createUpload() {
  const storage = multer.diskStorage({
    destination: (req, file, cb) => {
      if (!req.sessionId) {
        req.sessionId = crypto.randomUUID();
      }
      const sessionDir = path.join(UPLOAD_DIR, req.sessionId);
      if (!fs.existsSync(sessionDir)) {
        fs.mkdirSync(sessionDir, { recursive: true });
      }
      cb(null, sessionDir);
    },
    filename: (req, file, cb) => {
      // Preserve original filename but sanitize
      const safeName = file.originalname.replace(/[^a-zA-Z0-9._-]/g, '_');
      cb(null, safeName);
    },
  });

  return multer({
    storage,
    limits: {
      fileSize: 100 * 1024 * 1024, // 100MB max
    },
  });
}

/**
 * Ensures the output session directory exists and returns its path.
 */
function getOutputDir(sessionId) {
  const OUTPUT_DIR = path.join(os.tmpdir(), 'dodth_outputs');
  const sessionDir = path.join(OUTPUT_DIR, sessionId);
  if (!fs.existsSync(sessionDir)) {
    fs.mkdirSync(sessionDir, { recursive: true });
  }
  return sessionDir;
}

module.exports = { createUpload, getOutputDir };
