import React, { useState, useRef } from 'react';
import { ArrowLeft, UploadCloud, File, FileText } from 'lucide-react';
import mammoth from 'mammoth';
import RichTextEditor from '../components/RichTextEditor.jsx';
import PdfEditor from '../components/PdfEditor.jsx';

export default function LiveEditorPage({ onBack }) {
  const [file, setFile] = useState(null);
  const [editorType, setEditorType] = useState(null); // 'rich-text' or 'pdf'
  const [content, setContent] = useState(''); // Initial content for rich text
  const [pdfBytes, setPdfBytes] = useState(null); // ArrayBuffer for PDF
  const [loading, setLoading] = useState(false);
  const fileInput = useRef(null);

  const handleFileSelect = async (e) => {
    const selectedFile = e.target.files[0];
    if (!selectedFile) return;

    setLoading(true);
    try {
      const ext = selectedFile.name.split('.').pop().toLowerCase();
      
      if (ext === 'docx') {
        const arrayBuffer = await selectedFile.arrayBuffer();
        const result = await mammoth.convertToHtml({ arrayBuffer });
        setContent(result.value);
        setEditorType('rich-text');
      } else if (ext === 'txt') {
        const text = await selectedFile.text();
        // Convert plain text to simple HTML paragraphs
        const html = text.split('\n').map(line => `<p>${line}</p>`).join('');
        setContent(html);
        setEditorType('rich-text');
      } else if (ext === 'pdf') {
        const arrayBuffer = await selectedFile.arrayBuffer();
        setPdfBytes(arrayBuffer);
        setEditorType('pdf');
      } else {
        alert("Unsupported file type. Please upload PDF, DOCX, or TXT.");
      }
      
      setFile(selectedFile);
    } catch (err) {
      alert("Error loading file: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="tool-page" style={{ maxWidth: '1000px', margin: '0 auto' }}>
      <div className="tool-header">
        <button className="back-btn" onClick={onBack}><ArrowLeft size={20} /> Back</button>
        <div className="tool-title-group">
          <div className="tool-icon" style={{ '--tool-color': 'var(--primary)' }}><FileText size={24} /></div>
          <div>
            <h2>Live Document Editor</h2>
            <p>Upload a PDF, Word, or TXT file to edit it directly in the browser.</p>
          </div>
        </div>
      </div>

      {!editorType && !loading && (
        <div className="upload-section">
          <input 
            type="file" 
            ref={fileInput} 
            onChange={handleFileSelect} 
            accept=".pdf,.docx,.txt" 
            style={{ display: 'none' }} 
          />
          <div className="drop-zone" onClick={() => fileInput.current.click()}>
            <UploadCloud size={48} />
            <p>Click to browse or drag file here</p>
            <small>Supports PDF, DOCX, TXT</small>
          </div>
        </div>
      )}

      {loading && (
        <div className="loading-state" style={{ padding: '40px', textAlign: 'center' }}>
          <p>Loading document into editor...</p>
        </div>
      )}

      {editorType === 'rich-text' && (
        <RichTextEditor 
          initialHtml={content} 
          originalFile={file} 
          onClose={() => { setEditorType(null); setFile(null); }}
        />
      )}

      {editorType === 'pdf' && (
        <PdfEditor 
          pdfBytes={pdfBytes} 
          originalFile={file} 
          onClose={() => { setEditorType(null); setFile(null); }}
        />
      )}
    </div>
  );
}
