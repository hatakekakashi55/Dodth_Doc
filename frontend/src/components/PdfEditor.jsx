import React, { useEffect, useRef, useState } from 'react';
import * as pdfjsLib from 'pdfjs-dist';
import { Canvas as FabricCanvas, IText, PencilBrush, FabricImage } from 'fabric';
import { PDFDocument } from 'pdf-lib';
import { Download, X, Type, PenTool, Highlighter, MousePointer2 } from 'lucide-react';

// Configure PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.mjs',
  import.meta.url
).toString();

export default function PdfEditor({ pdfBytes, originalFile, onClose }) {
  const containerRef = useRef(null);
  const canvasRef = useRef(null); // Fabric canvas instance
  const [pdfDoc, setPdfDoc] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [saving, setSaving] = useState(false);
  const [activeTool, setActiveTool] = useState('select');
  const [loaded, setLoaded] = useState(false);

  // Initialize PDF and Canvas
  useEffect(() => {
    let disposed = false;

    const init = async () => {
      try {
        const loadingTask = pdfjsLib.getDocument({ data: new Uint8Array(pdfBytes) });
        const pdf = await loadingTask.promise;
        if (disposed) return;
        setPdfDoc(pdf);
        setTotalPages(pdf.numPages);

        // Render first page
        const page = await pdf.getPage(1);
        const viewport = page.getViewport({ scale: 1.5 });

        // Render PDF page to an offscreen canvas
        const offscreen = document.createElement('canvas');
        offscreen.width = viewport.width;
        offscreen.height = viewport.height;
        const ctx = offscreen.getContext('2d');
        await page.render({ canvasContext: ctx, viewport }).promise;
        if (disposed) return;

        // Create Fabric canvas
        if (containerRef.current) {
          containerRef.current.innerHTML = '';
          const el = document.createElement('canvas');
          el.id = 'pdf-fabric-canvas';
          el.width = viewport.width;
          el.height = viewport.height;
          containerRef.current.appendChild(el);

          const fc = new FabricCanvas('pdf-fabric-canvas', {
            width: viewport.width,
            height: viewport.height,
          });

          // Set PDF render as background image
          const dataUrl = offscreen.toDataURL('image/png');
          const bgImg = await FabricImage.fromURL(dataUrl);
          bgImg.scaleX = viewport.width / bgImg.width;
          bgImg.scaleY = viewport.height / bgImg.height;
          fc.backgroundImage = bgImg;
          fc.renderAll();

          canvasRef.current = fc;
          setLoaded(true);
        }
      } catch (err) {
        console.error('Error loading PDF:', err);
        alert('Could not load PDF for editing.');
      }
    };

    init();

    return () => {
      disposed = true;
      if (canvasRef.current) {
        canvasRef.current.dispose();
        canvasRef.current = null;
      }
    };
  }, [pdfBytes]);

  // Handle Tool Changes
  useEffect(() => {
    const fc = canvasRef.current;
    if (!fc) return;

    fc.isDrawingMode = false;
    fc.selection = true;
    fc.off('mouse:down');

    if (activeTool === 'draw') {
      fc.isDrawingMode = true;
      fc.freeDrawingBrush = new PencilBrush(fc);
      fc.freeDrawingBrush.color = '#e53e3e';
      fc.freeDrawingBrush.width = 3;
    } else if (activeTool === 'highlight') {
      fc.isDrawingMode = true;
      fc.freeDrawingBrush = new PencilBrush(fc);
      fc.freeDrawingBrush.color = 'rgba(255, 255, 0, 0.35)';
      fc.freeDrawingBrush.width = 20;
    } else if (activeTool === 'text') {
      fc.selection = false;
      fc.on('mouse:down', (o) => {
        const pointer = fc.getScenePoint(o.e);
        const text = new IText('Type here...', {
          left: pointer.x,
          top: pointer.y,
          fontFamily: 'Arial',
          fontSize: 22,
          fill: '#1a56db',
        });
        fc.add(text);
        fc.setActiveObject(text);
        text.enterEditing();
        text.selectAll();
        setActiveTool('select');
      });
    }
  }, [activeTool, loaded]);

  const handleSave = async () => {
    const fc = canvasRef.current;
    if (!fc) return;
    setSaving(true);
    try {
      // Export annotations only (remove background temporarily)
      const bg = fc.backgroundImage;
      fc.backgroundImage = null;
      fc.renderAll();
      const overlayDataUrl = fc.toDataURL({ format: 'png', multiplier: 1 });
      fc.backgroundImage = bg;
      fc.renderAll();

      // Load original PDF
      const pdfDocLib = await PDFDocument.load(pdfBytes);
      const pages = pdfDocLib.getPages();
      const pageToEdit = pages[currentPage - 1];

      // Embed overlay
      const overlayBytes = await fetch(overlayDataUrl).then(r => r.arrayBuffer());
      const embeddedImage = await pdfDocLib.embedPng(overlayBytes);

      const { width, height } = pageToEdit.getSize();
      pageToEdit.drawImage(embeddedImage, { x: 0, y: 0, width, height });

      // Download
      const modifiedBytes = await pdfDocLib.save();
      const blob = new Blob([modifiedBytes], { type: 'application/pdf' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      const baseName = originalFile.name.replace(/\.[^.]+$/, '');
      link.download = `${baseName}_edited.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (err) {
      alert('Error saving PDF: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="editor-container" style={{ marginTop: '20px', border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden' }}>
      <div className="editor-toolbar-custom" style={{ padding: '12px 16px', backgroundColor: 'var(--bg-card)', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
        <h3 style={{ margin: 0, fontSize: '1rem' }}>📄 {originalFile.name}</h3>

        <div style={{ display: 'flex', gap: '6px' }}>
          <button className={`btn ${activeTool === 'select' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTool('select')} title="Select"><MousePointer2 size={16} /></button>
          <button className={`btn ${activeTool === 'text' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTool('text')} title="Add Text"><Type size={16} /></button>
          <button className={`btn ${activeTool === 'draw' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTool('draw')} title="Draw / Sign"><PenTool size={16} /></button>
          <button className={`btn ${activeTool === 'highlight' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTool('highlight')} title="Highlight"><Highlighter size={16} /></button>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button className="btn btn-secondary" onClick={onClose}><X size={16} /> Close</button>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving...' : <><Download size={16} /> Save PDF</>}
          </button>
        </div>
      </div>

      <div style={{ backgroundColor: 'var(--bg-card)', padding: '20px', display: 'flex', justifyContent: 'center', overflow: 'auto', maxHeight: '75vh' }}>
        {!loaded && <p style={{ padding: '40px', opacity: 0.6 }}>Loading PDF...</p>}
        <div ref={containerRef} style={{ boxShadow: '0 2px 16px rgba(0,0,0,0.18)', lineHeight: 0 }}></div>
      </div>
    </div>
  );
}
