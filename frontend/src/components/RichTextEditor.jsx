import React, { useState } from 'react';
import ReactQuill from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';
import { Save, Download, X } from 'lucide-react';

export default function RichTextEditor({ initialHtml, originalFile, onClose }) {
  const [content, setContent] = useState(initialHtml || '');
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      // Create a full HTML document from the content
      const fullHtml = `
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <title>${originalFile.name}</title>
          <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; margin: 40px; }
            p { margin-bottom: 10px; }
            h1, h2, h3 { color: #333; }
          </style>
        </head>
        <body>
          ${content}
        </body>
        </html>
      `;

      // Create a blob from the HTML
      const blob = new Blob([fullHtml], { type: 'text/html' });
      const htmlFile = new File([blob], 'document.html', { type: 'text/html' });

      const formData = new FormData();
      formData.append('file', htmlFile);

      // Send to our backend HTML-to-Word endpoint
      const isNative = window.Capacitor && window.Capacitor.isNativePlatform();
      const baseUrl = isNative ? 'https://dodth-doc.onrender.com' : '';
      const response = await fetch(baseUrl + '/api/convert/html-to-word', {
        method: 'POST',
        body: formData
      });

      if (!response.ok) throw new Error('Failed to save document');

      const resultBlob = await response.blob();
      const downloadUrl = URL.createObjectURL(resultBlob);
      
      const ext = originalFile.name.split('.').pop();
      const baseName = originalFile.name.substring(0, originalFile.name.lastIndexOf('.'));
      let finalExt = 'docx';
      
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = `${baseName}_edited.${finalExt}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(downloadUrl);

    } catch (err) {
      alert("Error saving document: " + err.message);
    } finally {
      setSaving(false);
    }
  };

  const modules = {
    toolbar: [
      [{ 'header': [1, 2, 3, false] }],
      ['bold', 'italic', 'underline', 'strike'],
      [{ 'color': [] }, { 'background': [] }],
      [{ 'list': 'ordered'}, { 'list': 'bullet' }],
      [{ 'align': [] }],
      ['clean']
    ]
  };

  return (
    <div className="editor-container" style={{ marginTop: '20px', border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden' }}>
      <div className="editor-toolbar-custom" style={{ padding: '15px', backgroundColor: 'var(--bg-card)', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ margin: 0 }}>Editing: {originalFile.name}</h3>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn btn-secondary" onClick={onClose}><X size={18} /> Cancel</button>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving...' : <><Download size={18} /> Save & Download</>}
          </button>
        </div>
      </div>
      <div className="editor-workspace" style={{ backgroundColor: '#fff', color: '#000' }}>
        <ReactQuill 
          theme="snow" 
          value={content} 
          onChange={setContent} 
          modules={modules}
          style={{ height: '600px', backgroundColor: '#fff' }}
        />
      </div>
    </div>
  );
}
