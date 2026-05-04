import React, { useState, useRef, useCallback } from 'react';
import { Download, X, RotateCcw, Bold, Italic, Underline, List, AlignLeft, AlignCenter, AlignRight, Heading1, Heading2, Type, Palette, Strikethrough } from 'lucide-react';

export default function RichTextEditor({ initialHtml, originalFile, onClose }) {
  const editorRef = useRef(null);
  const [saving, setSaving] = useState(false);
  const [saveFormat, setSaveFormat] = useState('docx');

  // Execute formatting commands
  const exec = useCallback((command, value = null) => {
    document.execCommand(command, false, value);
    editorRef.current?.focus();
  }, []);

  const getContent = () => editorRef.current?.innerHTML || '';

  const handleSave = async () => {
    setSaving(true);
    try {
      const content = getContent();
      const fullHtml = `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><title>${originalFile.name}</title>
<style>
body { font-family: Arial, Helvetica, sans-serif; line-height: 1.7; margin: 40px; color: #222; font-size: 14px; }
p { margin-bottom: 10px; }
h1 { font-size: 24px; color: #111; margin-bottom: 16px; }
h2 { font-size: 20px; color: #222; margin-bottom: 12px; }
h3 { font-size: 16px; color: #333; margin-bottom: 10px; }
ul, ol { margin-left: 20px; margin-bottom: 12px; }
blockquote { border-left: 3px solid #ccc; padding-left: 12px; color: #555; margin: 12px 0; }
table { border-collapse: collapse; width: 100%; margin: 12px 0; }
th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
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
      const endpoint = saveFormat === 'docx' ? '/api/convert/html-to-word' : '/api/convert/html-to-pdf';

      const response = await fetch(baseUrl + endpoint, { method: 'POST', body: formData });
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

  const handleReset = () => {
    if (editorRef.current) editorRef.current.innerHTML = initialHtml;
  };

  const ToolBtn = ({ onClick, active, children, title }) => (
    <button
      onMouseDown={(e) => { e.preventDefault(); onClick(); }}
      title={title}
      style={{
        padding: '6px 8px', border: 'none', borderRadius: '4px', cursor: 'pointer',
        background: active ? '#6366f1' : 'transparent', color: active ? '#fff' : '#555',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        transition: 'all 0.15s',
      }}
    >
      {children}
    </button>
  );

  const Separator = () => <div style={{ width: '1px', height: '24px', background: '#e2e8f0', margin: '0 4px' }} />;

  return (
    <div style={{ marginTop: '16px', border: '1px solid var(--border)', borderRadius: 'var(--radius)', overflow: 'hidden' }}>
      {/* Header */}
      <div style={{ padding: '10px 16px', backgroundColor: 'var(--bg-card)', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '1.1rem' }}>📝</span>
          <span style={{ fontWeight: 600, fontSize: '0.9rem', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{originalFile.name}</span>
        </div>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
          <select value={saveFormat} onChange={(e) => setSaveFormat(e.target.value)}
            style={{ padding: '7px 10px', borderRadius: '6px', border: '1px solid var(--border)', background: 'var(--bg-elevated)', color: 'var(--text-primary)', fontFamily: 'var(--font)', fontSize: '0.82rem' }}>
            <option value="docx">Save as DOCX</option>
            <option value="pdf">Save as PDF</option>
          </select>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving} style={{ padding: '7px 18px' }}>
            {saving ? 'Saving...' : <><Download size={15} /> Download</>}
          </button>
          <button className="btn btn-secondary" onClick={handleReset} style={{ padding: '7px 12px' }} title="Reset"><RotateCcw size={15} /></button>
          <button className="btn btn-secondary" onClick={onClose} style={{ padding: '7px 12px' }}><X size={15} /></button>
        </div>
      </div>

      {/* Formatting Toolbar */}
      <div style={{ padding: '6px 12px', background: '#f8fafc', borderBottom: '1px solid #e2e8f0', display: 'flex', flexWrap: 'wrap', gap: '2px', alignItems: 'center', position: 'sticky', top: 0, zIndex: 10 }}>
        <ToolBtn onClick={() => exec('formatBlock', 'H1')} title="Heading 1"><Heading1 size={16} /></ToolBtn>
        <ToolBtn onClick={() => exec('formatBlock', 'H2')} title="Heading 2"><Heading2 size={16} /></ToolBtn>
        <ToolBtn onClick={() => exec('formatBlock', 'P')} title="Normal text"><Type size={16} /></ToolBtn>
        <Separator />
        <ToolBtn onClick={() => exec('bold')} title="Bold"><Bold size={16} /></ToolBtn>
        <ToolBtn onClick={() => exec('italic')} title="Italic"><Italic size={16} /></ToolBtn>
        <ToolBtn onClick={() => exec('underline')} title="Underline"><Underline size={16} /></ToolBtn>
        <ToolBtn onClick={() => exec('strikeThrough')} title="Strikethrough"><Strikethrough size={16} /></ToolBtn>
        <Separator />
        <ToolBtn onClick={() => exec('justifyLeft')} title="Align Left"><AlignLeft size={16} /></ToolBtn>
        <ToolBtn onClick={() => exec('justifyCenter')} title="Align Center"><AlignCenter size={16} /></ToolBtn>
        <ToolBtn onClick={() => exec('justifyRight')} title="Align Right"><AlignRight size={16} /></ToolBtn>
        <Separator />
        <ToolBtn onClick={() => exec('insertUnorderedList')} title="Bullet List"><List size={16} /></ToolBtn>
        <ToolBtn onClick={() => exec('insertOrderedList')} title="Numbered List"><List size={16} /></ToolBtn>
        <Separator />
        <label title="Text Color" style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', padding: '4px' }}>
          <Palette size={16} color="#555" />
          <input type="color" onChange={(e) => exec('foreColor', e.target.value)} style={{ width: 0, height: 0, opacity: 0, position: 'absolute' }} />
        </label>
        <ToolBtn onClick={() => exec('removeFormat')} title="Clear Formatting">
          <span style={{ fontSize: '12px', fontWeight: 700, color: '#999' }}>✕</span>
        </ToolBtn>
      </div>

      {/* Editable Content Area */}
      <div style={{ background: '#fff', maxHeight: '65vh', overflow: 'auto' }}>
        <div
          ref={editorRef}
          contentEditable
          suppressContentEditableWarning
          dangerouslySetInnerHTML={{ __html: initialHtml }}
          style={{
            minHeight: '500px',
            padding: '32px 40px',
            color: '#1a1a1a',
            lineHeight: 1.8,
            fontSize: '15px',
            fontFamily: "'Georgia', 'Times New Roman', serif",
            outline: 'none',
            wordWrap: 'break-word',
          }}
          onPaste={(e) => {
            // Strip formatting on paste for clean content
            e.preventDefault();
            const text = e.clipboardData.getData('text/html') || e.clipboardData.getData('text/plain');
            document.execCommand('insertHTML', false, text);
          }}
        />
      </div>
    </div>
  );
}
