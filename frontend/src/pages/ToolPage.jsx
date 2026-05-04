import React, { useState, useRef, useEffect, useCallback } from 'react';
import { getToolById, formatFileSize } from '../utils/tools.jsx';
import { CheckCircle, XCircle, File, FolderUp, ArrowLeft, X } from 'lucide-react';

export default function ToolPage({ toolId, onBack }) {
  const tool = getToolById(toolId);
  const [files, setFiles] = useState([]);
  const [status, setStatus] = useState('idle');
  const [progress, setProgress] = useState(0);
  const [progressPhase, setProgressPhase] = useState('idle');
  const [error, setError] = useState('');
  const [resultBlob, setResultBlob] = useState(null);
  const [resultName, setResultName] = useState('');
  const [extra, setExtra] = useState({ password: '', pages: 'all', angle: '90', text: '', opacity: '0.3' });
  const inputRef = useRef(null);
  const progressTimer = useRef(null);

  // Smooth progress animation
  useEffect(() => {
    if (progressTimer.current) clearInterval(progressTimer.current);

    if (progressPhase === 'uploading') {
      // Quickly go from 0 to ~25%
      setProgress(5);
      progressTimer.current = setInterval(() => {
        setProgress(prev => {
          if (prev >= 25) { clearInterval(progressTimer.current); return 25; }
          return prev + 1.5;
        });
      }, 80);
    } else if (progressPhase === 'processing') {
      // Slowly crawl from 25% to ~85% (server is working)
      progressTimer.current = setInterval(() => {
        setProgress(prev => {
          if (prev >= 85) { clearInterval(progressTimer.current); return 85; }
          // Slow down as it gets higher
          const speed = prev < 50 ? 0.8 : prev < 70 ? 0.4 : 0.15;
          return prev + speed;
        });
      }, 200);
    } else if (progressPhase === 'finalizing') {
      // Quick jump from current to 95%
      progressTimer.current = setInterval(() => {
        setProgress(prev => {
          if (prev >= 95) { clearInterval(progressTimer.current); return 95; }
          return prev + 2;
        });
      }, 50);
    } else if (progressPhase === 'done') {
      setProgress(100);
    }

    return () => { if (progressTimer.current) clearInterval(progressTimer.current); };
  }, [progressPhase]);

  const getProgressText = useCallback(() => {
    if (progress < 20) return '📤 Uploading file...';
    if (progress < 40) return '⚙️ Starting conversion...';
    if (progress < 60) return '🔄 Processing document...';
    if (progress < 80) return '📝 Applying formatting...';
    if (progress < 95) return '✨ Almost there...';
    return '✅ Finishing up...';
  }, [progress]);
  const dropRef = useRef(null);

  if (!tool) return <div className="tool-page"><p>Tool not found.</p></div>;

  const handleFiles = (newFiles) => {
    const arr = Array.from(newFiles);
    setFiles(tool.single ? arr.slice(0, 1) : prev => [...prev, ...arr]);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    dropRef.current?.classList.remove('active');
    handleFiles(e.dataTransfer.files);
  };

  const handleProcess = async () => {
    if (files.length === 0) return;
    setStatus('uploading');
    setProgress(0);
    setProgressPhase('uploading');
    setError('');

    try {
      const formData = new FormData();
      if (tool.single) {
        formData.append('file', files[0]);
      } else {
        files.forEach(f => formData.append('files', f));
      }
      if (tool.hasPassword) formData.append('password', extra.password);
      if (tool.hasPages) formData.append('pages', extra.pages);
      if (tool.hasAngle) formData.append('angle', extra.angle);
      if (tool.hasWatermark) { formData.append('text', extra.text); formData.append('opacity', extra.opacity); }

      // Determine base URL (Use Render directly for APK/Native)
      const isNative = window.Capacitor && window.Capacitor.isNativePlatform();
      const baseUrl = isNative ? 'https://dodth-doc.onrender.com' : '';
      const fullEndpoint = baseUrl + tool.endpoint;

      // Phase 2: Server processing
      setProgressPhase('processing');
      const response = await fetch(fullEndpoint, { method: 'POST', body: formData });

      // Phase 3: Finalizing
      setProgressPhase('finalizing');

      if (!response.ok) {
        const err = await response.json().catch(() => ({ detail: 'Processing failed' }));
        throw new Error(err.detail);
      }

      const blob = await response.blob();
      const disposition = response.headers.get('content-disposition');
      let filename = tool.name.replace(/\s/g, '_') + '_result';
      if (disposition) {
        const m = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
        if (m) filename = m[1].replace(/['"]/g, '');
      }
      if (!filename.includes('.')) {
        const ct = response.headers.get('content-type') || '';
        if (ct.includes('zip')) filename += '.zip';
        else if (ct.includes('wordprocessingml') || ct.includes('msword')) filename += '.docx';
        else if (ct.includes('spreadsheetml') || ct.includes('excel')) filename += '.xlsx';
        else if (ct.includes('presentationml') || ct.includes('powerpoint')) filename += '.pptx';
        else if (ct.includes('jpeg') || ct.includes('jpg')) filename += '.jpg';
        else if (ct.includes('png')) filename += '.png';
        else filename += '.pdf';
      }

      setResultBlob(blob);
      setResultName(filename);
      setProgressPhase('done');
      setTimeout(() => setStatus('done'), 400);
    } catch (err) {
      setProgressPhase('idle');
      setError(err.message);
      setStatus('error');
    }
  };

  const handleDownload = async () => {
    if (!resultBlob) return;

    // Check if we are running in a native APK environment
    const isNative = window.Capacitor && window.Capacitor.isNativePlatform();

    if (isNative) {
      try {
        const { Filesystem, Directory } = await import('@capacitor/filesystem');
        const { Share } = await import('@capacitor/share');

        // Convert Blob to Base64
        const reader = new FileReader();
        reader.readAsDataURL(resultBlob);
        reader.onloadend = async () => {
          const base64Data = reader.result.split(',')[1];
          
          // Save to Documents storage
          await Filesystem.writeFile({
            path: resultName,
            data: base64Data,
            directory: Directory.Documents,
          });

          alert('✅ File saved to your Documents folder!');
        };
      } catch (err) {
        alert('Download failed: ' + err.message);
      }
    } else {
      // Standard browser download
      const url = URL.createObjectURL(resultBlob);
      const a = document.createElement('a');
      a.href = url;
      a.download = resultName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }
  };

  const reset = () => { setFiles([]); setStatus('idle'); setProgress(0); setProgressPhase('idle'); setError(''); setResultBlob(null); };

  const canProcess = files.length > 0 && !(tool.hasPassword && !extra.password) && !(tool.hasWatermark && !extra.text);

  return (
    <div className="tool-page">
      <button className="back-btn" onClick={onBack}><ArrowLeft size={16} /> Back to Tools</button>
      <div className="tool-header">
        <div className="tool-header-icon" style={{ background: tool.color }}>{tool.icon}</div>
        <div>
          <h1 className="page-title" style={{ fontSize: '1.6rem', marginBottom: '4px' }}>{tool.name}</h1>
          <p className="page-subtitle" style={{ marginBottom: 0 }}>{tool.desc}</p>
        </div>
      </div>

      {status === 'done' ? (
        <div className="result-card">
          <div className="result-icon"><CheckCircle size={48} /></div>
          <h3>Processing Complete!</h3>
          <p>Your file <strong>{resultName}</strong> is ready.</p>
          <div className="result-actions">
            <button className="btn btn-success" onClick={handleDownload}>⬇ Download</button>
            <button className="btn btn-secondary" onClick={reset}>Process Another</button>
          </div>
        </div>
      ) : (
        <>
          <div ref={dropRef} className="dropzone" onClick={() => inputRef.current?.click()}
            onDragOver={e => { e.preventDefault(); dropRef.current?.classList.add('active'); }}
            onDragLeave={() => dropRef.current?.classList.remove('active')}
            onDrop={handleDrop}>
            <div className="dropzone-icon"><FolderUp size={48} /></div>
            <h3>Drag & drop your files here</h3>
            <p>or <span className="browse-link">browse files</span> — Accepts: {tool.accept}</p>
          </div>
          <input ref={inputRef} type="file" accept={tool.accept} multiple={!tool.single} hidden
            onChange={e => { handleFiles(e.target.files); e.target.value = ''; }} />

          {files.length > 0 && (
            <div className="file-list">
              {files.map((file, i) => (
                <div key={i} className="file-item">
                  <span className="file-item-icon"><File size={20} /></span>
                  <div className="file-item-info">
                    <div className="name">{file.name}</div>
                    <div className="size">{formatFileSize(file.size)}</div>
                  </div>
                  <button className="file-item-remove" onClick={() => setFiles(f => f.filter((_, idx) => idx !== i))}><X size={16} /></button>
                </div>
              ))}
            </div>
          )}

          {tool.hasPassword && (
            <div className="form-group"><label>Password</label>
              <input className="form-input" type="password" placeholder="Enter password" value={extra.password} onChange={e => setExtra(p => ({...p, password: e.target.value}))} />
            </div>
          )}
          {tool.hasPages && (
            <div className="form-group"><label>Pages (e.g., "all" or "1,3,5-7")</label>
              <input className="form-input" type="text" placeholder="all" value={extra.pages} onChange={e => setExtra(p => ({...p, pages: e.target.value}))} />
            </div>
          )}
          {tool.hasAngle && (
            <div className="form-group"><label>Rotation Angle</label>
              <select className="form-input" value={extra.angle} onChange={e => setExtra(p => ({...p, angle: e.target.value}))}>
                <option value="90">90°</option><option value="180">180°</option><option value="270">270°</option>
              </select>
            </div>
          )}
          {tool.hasWatermark && (
            <>
              <div className="form-group"><label>Watermark Text</label>
                <input className="form-input" type="text" placeholder="CONFIDENTIAL" value={extra.text} onChange={e => setExtra(p => ({...p, text: e.target.value}))} />
              </div>
              <div className="form-group"><label>Opacity (0.1 - 1.0)</label>
                <input className="form-input" type="number" min="0.1" max="1" step="0.1" value={extra.opacity} onChange={e => setExtra(p => ({...p, opacity: e.target.value}))} />
              </div>
            </>
          )}

          {status === 'uploading' && (
            <div className="progress-container">
              <div className="progress-bar-track"><div className="progress-bar-fill" style={{ width: `${Math.round(progress)}%`, transition: 'width 0.3s ease-out' }}></div></div>
              <div className="progress-text">{getProgressText()} <span style={{ opacity: 0.6 }}>{Math.round(progress)}%</span></div>
            </div>
          )}
          {status === 'error' && (
            <div className="result-card" style={{ borderColor: 'var(--danger)' }}>
              <div className="result-icon"><XCircle size={48} /></div>
              <h3 style={{ color: 'var(--danger)' }}>Processing Failed</h3>
              <p>{error}</p>
              <button className="btn btn-secondary" onClick={reset}>Try Again</button>
            </div>
          )}
          {status === 'idle' && files.length > 0 && (
            <button className="btn btn-primary" onClick={handleProcess} disabled={!canProcess}>
              {tool.icon} Process {tool.name}
            </button>
          )}
        </>
      )}
    </div>
  );
}
