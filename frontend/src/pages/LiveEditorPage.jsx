import React, { useState, useRef } from 'react';
import { ArrowLeft, UploadCloud, FileText, FileType, File } from 'lucide-react';
import mammoth from 'mammoth';
import RichTextEditor from '../components/RichTextEditor.jsx';
import PdfEditor from '../components/PdfEditor.jsx';

export default function LiveEditorPage({ onBack }) {
  const [file, setFile] = useState(null);
  const [editorType, setEditorType] = useState(null);
  const [content, setContent] = useState('');
  const [pdfBytes, setPdfBytes] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const fileInput = useRef(null);

  const handleFileSelect = async (e) => {
    const selectedFile = e.target.files?.[0];
    if (!selectedFile) return;

    setLoading(true);
    setError('');
    try {
      const ext = selectedFile.name.split('.').pop().toLowerCase();

      if (ext === 'docx' || ext === 'doc') {
        const arrayBuffer = await selectedFile.arrayBuffer();
        const result = await mammoth.convertToHtml({ arrayBuffer });
        setContent(result.value);
        setEditorType('rich-text');
      } else if (ext === 'txt') {
        const text = await selectedFile.text();
        const html = text
          .split('\n')
          .map((line) => (line.trim() ? `<p>${line}</p>` : '<p><br></p>'))
          .join('');
        setContent(html);
        setEditorType('rich-text');
      } else if (ext === 'pdf') {
        const arrayBuffer = await selectedFile.arrayBuffer();
        setPdfBytes(arrayBuffer);
        setEditorType('pdf');
      } else {
        setError('Unsupported file. Please upload PDF, DOCX, or TXT.');
        return;
      }

      setFile(selectedFile);
    } catch (err) {
      console.error('File load error:', err);
      setError('Failed to load file: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setEditorType(null);
    setFile(null);
    setPdfBytes(null);
    setContent('');
    setError('');
    // Reset file input so same file can be re-uploaded
    if (fileInput.current) fileInput.current.value = '';
  };

  const getFileIcon = () => {
    if (!file) return <UploadCloud size={48} />;
    const ext = file.name.split('.').pop().toLowerCase();
    if (ext === 'pdf') return <File size={48} />;
    if (ext === 'docx' || ext === 'doc') return <FileType size={48} />;
    return <FileText size={48} />;
  };

  return (
    <div className="tool-page">
      {/* Header */}
      <button className="back-btn" onClick={onBack}>
        <ArrowLeft size={18} /> Back to Dashboard
      </button>

      <div style={{ marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '1.6rem', fontWeight: 700, marginBottom: '0.5rem' }}>
          ✏️ Live Document Editor
        </h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          Upload a PDF, Word, or TXT file to edit it directly in the browser.
        </p>
      </div>

      {/* Upload Zone - only show when no editor is active */}
      {!editorType && !loading && (
        <>
          <input
            type="file"
            ref={fileInput}
            onChange={handleFileSelect}
            accept=".pdf,.docx,.doc,.txt"
            style={{ display: 'none' }}
          />
          <div
            className="dropzone"
            onClick={() => fileInput.current?.click()}
          >
            <div className="dropzone-icon">{getFileIcon()}</div>
            <h3>Click to browse or drag file here</h3>
            <p>Supports <span className="browse-link">PDF</span>, <span className="browse-link">DOCX</span>, <span className="browse-link">TXT</span></p>
          </div>

          {error && (
            <div style={{ padding: '12px 16px', background: 'rgba(239,68,68,0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--radius-sm)', color: 'var(--danger)', marginTop: '12px', fontSize: '0.9rem' }}>
              {error}
            </div>
          )}
        </>
      )}

      {/* Loading */}
      {loading && (
        <div style={{ padding: '60px 20px', textAlign: 'center' }}>
          <div style={{ width: '40px', height: '40px', border: '3px solid var(--border)', borderTopColor: 'var(--accent)', borderRadius: '50%', margin: '0 auto 16px', animation: 'spin 0.8s linear infinite' }} />
          <p style={{ color: 'var(--text-secondary)' }}>Loading document...</p>
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
      )}

      {/* Rich Text Editor */}
      {editorType === 'rich-text' && (
        <RichTextEditor
          initialHtml={content}
          originalFile={file}
          onClose={handleClose}
        />
      )}

      {/* PDF Editor */}
      {editorType === 'pdf' && (
        <PdfEditor
          pdfBytes={pdfBytes}
          originalFile={file}
          onClose={handleClose}
        />
      )}
    </div>
  );
}
