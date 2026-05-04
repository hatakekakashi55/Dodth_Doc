import React, { useState, useMemo } from 'react';
import ReactQuill from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';
import { Download, X, RotateCcw } from 'lucide-react';

export default function RichTextEditor({ initialHtml, originalFile, onClose }) {
  const [content, setContent] = useState(initialHtml || '');
  const [saving, setSaving] = useState(false);
  const [saveFormat, setSaveFormat] = useState('docx'); // docx or pdf

  const handleSave = async () => {
    setSaving(true);
    try {
      const fullHtml = `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><title>${originalFile.name}</title>
<style>
body { font-family: Arial, Helvetica, sans-serif; line-height: 1.6; margin: 40px; color: #222; }
p { margin-bottom: 10px; }
h1 { font-size: 24px; color: #111; margin-bottom: 16px; }
h2 { font-size: 20px; color: #222; margin-bottom: 12px; }
h3 { font-size: 16px; color: #333; margin-bottom: 10px; }
ul, ol { margin-left: 20px; margin-bottom: 12px; }
blockquote { border-left: 3px solid #ccc; padding-left: 12px; color: #555; margin: 12px 0; }
</style>
</head>
<body>${content}</body>
</html>`;

      const blob = new Blob([fullHtml], { type: 'text/html' });
      const htmlFile = new File([blob], 'document.html', { type: 'text/html' });

      const formData = new FormData();
      formData.append('file', htmlFile);

      const isNative = window.Capacitor && window.Capacitor.isNativePlatform();
      const baseUrl = isNative ? 'https://dodth-doc.onrender.com' : '';

      const endpoint = saveFormat === 'docx'
        ? '/api/convert/html-to-word'
        : '/api/convert/html-to-pdf';

      const response = await fetch(baseUrl + endpoint, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) throw new Error('Server error during save');

      const resultBlob = await response.blob();
      const downloadUrl = URL.createObjectURL(resultBlob);
      const baseName = originalFile.name.replace(/\.[^.]+$/, '');

      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = `${baseName}_edited.${saveFormat}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(downloadUrl);
    } catch (err) {
      alert('Error saving: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  // Memoize modules to prevent Quill re-render loops
  const modules = useMemo(() => ({
    toolbar: [
      [{ header: [1, 2, 3, false] }],
      ['bold', 'italic', 'underline', 'strike'],
      [{ color: [] }, { background: [] }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      [{ align: [] }],
      [{ indent: '-1' }, { indent: '+1' }],
      ['blockquote'],
      ['link'],
      ['clean'],
    ],
  }), []);

  return (
    <div style={{ marginTop: '16px', border: '1px solid var(--border)', borderRadius: 'var(--radius)', overflow: 'hidden' }}>
      {/* Header bar */}
      <div style={{ padding: '12px 16px', backgroundColor: 'var(--bg-card)', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
          <span style={{ fontSize: '1.2rem' }}>📝</span>
          <span style={{ fontWeight: 600, fontSize: '0.95rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '200px' }}>{originalFile.name}</span>
        </div>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
          <select
            value={saveFormat}
            onChange={(e) => setSaveFormat(e.target.value)}
            style={{ padding: '8px 12px', borderRadius: 'var(--radius-xs)', border: '1px solid var(--border)', background: 'var(--bg-elevated)', color: 'var(--text-primary)', fontFamily: 'var(--font)', fontSize: '0.85rem', cursor: 'pointer' }}
          >
            <option value="docx">Save as DOCX</option>
            <option value="pdf">Save as PDF</option>
          </select>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving} style={{ padding: '8px 20px' }}>
            {saving ? 'Saving...' : <><Download size={16} /> Download</>}
          </button>
          <button className="btn btn-secondary" onClick={() => setContent(initialHtml)} style={{ padding: '8px 14px' }} title="Reset to original">
            <RotateCcw size={16} />
          </button>
          <button className="btn btn-secondary" onClick={onClose} style={{ padding: '8px 14px' }}>
            <X size={16} /> Close
          </button>
        </div>
      </div>

      {/* Quill Editor */}
      <div className="dodth-quill-editor">
        <ReactQuill
          theme="snow"
          value={content}
          onChange={setContent}
          modules={modules}
        />
      </div>

      {/* Scoped Quill styles */}
      <style>{`
        .dodth-quill-editor {
          background: #fff;
        }
        .dodth-quill-editor .ql-toolbar.ql-snow {
          border: none !important;
          border-bottom: 1px solid #e2e8f0 !important;
          background: #f8fafc;
          padding: 10px 12px;
          position: sticky;
          top: 0;
          z-index: 10;
        }
        .dodth-quill-editor .ql-toolbar .ql-stroke {
          stroke: #374151 !important;
        }
        .dodth-quill-editor .ql-toolbar .ql-fill {
          fill: #374151 !important;
        }
        .dodth-quill-editor .ql-toolbar .ql-picker-label {
          color: #374151 !important;
        }
        .dodth-quill-editor .ql-toolbar button:hover .ql-stroke,
        .dodth-quill-editor .ql-toolbar .ql-active .ql-stroke {
          stroke: var(--accent) !important;
        }
        .dodth-quill-editor .ql-toolbar button:hover .ql-fill,
        .dodth-quill-editor .ql-toolbar .ql-active .ql-fill {
          fill: var(--accent) !important;
        }
        .dodth-quill-editor .ql-container.ql-snow {
          border: none !important;
          font-size: 16px;
        }
        .dodth-quill-editor .ql-editor {
          min-height: 500px;
          max-height: 65vh;
          overflow-y: auto;
          padding: 32px 40px;
          color: #1a1a1a;
          line-height: 1.8;
          font-family: 'Georgia', 'Times New Roman', serif;
        }
        .dodth-quill-editor .ql-editor h1,
        .dodth-quill-editor .ql-editor h2,
        .dodth-quill-editor .ql-editor h3 {
          font-family: 'Inter', Arial, sans-serif;
          color: #111;
        }
        .dodth-quill-editor .ql-editor p {
          margin-bottom: 8px;
        }
        .dodth-quill-editor .ql-editor blockquote {
          border-left: 3px solid #6366f1;
          padding-left: 14px;
          color: #555;
        }
        @media (max-width: 600px) {
          .dodth-quill-editor .ql-editor {
            padding: 16px 18px;
            min-height: 350px;
          }
        }
      `}</style>
    </div>
  );
}
