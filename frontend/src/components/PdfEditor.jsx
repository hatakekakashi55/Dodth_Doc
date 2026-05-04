import React, { useEffect, useRef, useState, useCallback } from 'react';
import * as pdfjsLib from 'pdfjs-dist';
import { Canvas as FabricCanvas, IText, PencilBrush, FabricImage } from 'fabric';
import { PDFDocument } from 'pdf-lib';
import { Download, X, Type, PenTool, Highlighter, MousePointer2, ChevronLeft, ChevronRight, Eraser } from 'lucide-react';

// Configure PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.mjs',
  import.meta.url
).toString();

export default function PdfEditor({ pdfBytes, originalFile, onClose }) {
  const containerRef = useRef(null);
  const canvasRef = useRef(null);
  const pdfRef = useRef(null);
  const annotationsRef = useRef({}); // Store annotations per page
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [saving, setSaving] = useState(false);
  const [activeTool, setActiveTool] = useState('select');
  const [loaded, setLoaded] = useState(false);
  const [scale, setScale] = useState(1.2);

  // Render a specific page
  const renderPage = useCallback(async (pageNum) => {
    const pdf = pdfRef.current;
    if (!pdf || !containerRef.current) return;

    try {
      // Save current annotations before switching pages
      if (canvasRef.current) {
        const currentAnnotations = canvasRef.current.toJSON();
        annotationsRef.current[currentPage] = currentAnnotations;
        canvasRef.current.dispose();
        canvasRef.current = null;
      }

      const page = await pdf.getPage(pageNum);

      // Calculate scale to fit container width (max ~700px on desktop)
      const containerWidth = Math.min(containerRef.current.parentElement.clientWidth - 40, 750);
      const naturalViewport = page.getViewport({ scale: 1 });
      const fitScale = containerWidth / naturalViewport.width;
      const useScale = Math.min(fitScale, 1.5);
      setScale(useScale);

      const viewport = page.getViewport({ scale: useScale });

      // Render PDF page to offscreen canvas
      const offscreen = document.createElement('canvas');
      offscreen.width = viewport.width;
      offscreen.height = viewport.height;
      const ctx = offscreen.getContext('2d');
      await page.render({ canvasContext: ctx, viewport }).promise;

      // Create Fabric canvas
      containerRef.current.innerHTML = '';
      const el = document.createElement('canvas');
      el.id = 'pdf-fabric-canvas';
      containerRef.current.appendChild(el);

      const fc = new FabricCanvas(el, {
        width: viewport.width,
        height: viewport.height,
      });

      // Set PDF render as background
      const dataUrl = offscreen.toDataURL('image/png');
      const bgImg = await FabricImage.fromURL(dataUrl);
      bgImg.scaleX = viewport.width / bgImg.width;
      bgImg.scaleY = viewport.height / bgImg.height;
      fc.backgroundImage = bgImg;

      // Restore previous annotations for this page
      if (annotationsRef.current[pageNum]) {
        const saved = annotationsRef.current[pageNum];
        if (saved.objects && saved.objects.length > 0) {
          await fc.loadFromJSON(saved);
          // Re-set background since loadFromJSON may clear it
          fc.backgroundImage = bgImg;
        }
      }

      fc.renderAll();
      canvasRef.current = fc;
      setLoaded(true);
    } catch (err) {
      console.error('Error rendering page:', err);
    }
  }, [currentPage]);

  // Initialize PDF
  useEffect(() => {
    let disposed = false;

    const init = async () => {
      try {
        const loadingTask = pdfjsLib.getDocument({ data: new Uint8Array(pdfBytes) });
        const pdf = await loadingTask.promise;
        if (disposed) return;
        pdfRef.current = pdf;
        setTotalPages(pdf.numPages);
        await renderPage(1);
      } catch (err) {
        console.error('Error loading PDF:', err);
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

  // Re-render when page changes
  useEffect(() => {
    if (pdfRef.current) renderPage(currentPage);
  }, [currentPage]);

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
      const handler = (o) => {
        const pointer = fc.getViewportPoint(o.e);
        const text = new IText('Type here...', {
          left: pointer.x,
          top: pointer.y,
          fontFamily: 'Arial',
          fontSize: Math.round(18 * scale),
          fill: '#1a56db',
        });
        fc.add(text);
        fc.setActiveObject(text);
        text.enterEditing();
        text.selectAll();
        setActiveTool('select');
      };
      fc.on('mouse:down', handler);
    } else if (activeTool === 'erase') {
      fc.selection = false;
      const handler = (o) => {
        const target = fc.findTarget(o.e);
        if (target && target !== fc.backgroundImage) {
          fc.remove(target);
          fc.renderAll();
        }
      };
      fc.on('mouse:down', handler);
    }
  }, [activeTool, loaded, scale]);

  // Delete selected objects
  const handleDelete = () => {
    const fc = canvasRef.current;
    if (!fc) return;
    const active = fc.getActiveObjects();
    active.forEach(obj => fc.remove(obj));
    fc.discardActiveObject();
    fc.renderAll();
  };

  // Save: apply annotations to PDF on all pages
  const handleSave = async () => {
    const fc = canvasRef.current;
    if (!fc) return;
    setSaving(true);
    try {
      // Save current page annotations
      annotationsRef.current[currentPage] = fc.toJSON();

      const pdfDocLib = await PDFDocument.load(pdfBytes);
      const pdf = pdfRef.current;

      // Process each page that has annotations
      for (let i = 1; i <= totalPages; i++) {
        const pageAnnotations = annotationsRef.current[i];
        if (!pageAnnotations || !pageAnnotations.objects || pageAnnotations.objects.length === 0) continue;

        // Render the page annotations to a temp canvas
        const page = await pdf.getPage(i);
        const naturalViewport = page.getViewport({ scale: 1 });
        const containerWidth = Math.min(750, 750);
        const fitScale = containerWidth / naturalViewport.width;
        const useScale = Math.min(fitScale, 1.5);
        const viewport = page.getViewport({ scale: useScale });

        // Create temp fabric canvas to render annotations
        const tempEl = document.createElement('canvas');
        tempEl.id = 'temp-export-canvas';
        tempEl.style.display = 'none';
        document.body.appendChild(tempEl);

        const tempFc = new FabricCanvas(tempEl, {
          width: viewport.width,
          height: viewport.height,
        });

        await tempFc.loadFromJSON(pageAnnotations);
        tempFc.backgroundImage = null;
        tempFc.renderAll();

        const overlayDataUrl = tempFc.toDataURL({ format: 'png' });
        tempFc.dispose();
        document.body.removeChild(tempEl);

        // Embed overlay on PDF page
        const overlayBytes = await fetch(overlayDataUrl).then(r => r.arrayBuffer());
        const embeddedImage = await pdfDocLib.embedPng(overlayBytes);
        const pdfPage = pdfDocLib.getPages()[i - 1];
        const { width, height } = pdfPage.getSize();

        // Scale overlay to match original PDF dimensions
        pdfPage.drawImage(embeddedImage, {
          x: 0,
          y: 0,
          width: width,
          height: height,
        });
      }

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
      console.error('Save error:', err);
      alert('Error saving PDF: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const goPage = (dir) => {
    const next = currentPage + dir;
    if (next >= 1 && next <= totalPages) {
      setLoaded(false);
      setCurrentPage(next);
    }
  };

  return (
    <div style={{ marginTop: '16px', border: '1px solid var(--border)', borderRadius: 'var(--radius)', overflow: 'hidden' }}>
      {/* Toolbar */}
      <div style={{ padding: '10px 16px', backgroundColor: 'var(--bg-card)', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
        {/* File name */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
          <span style={{ fontSize: '1.1rem' }}>📄</span>
          <span style={{ fontWeight: 600, fontSize: '0.9rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '160px' }}>{originalFile.name}</span>
        </div>

        {/* Drawing tools */}
        <div style={{ display: 'flex', gap: '4px' }}>
          {[
            { id: 'select', icon: <MousePointer2 size={16} />, title: 'Select' },
            { id: 'text', icon: <Type size={16} />, title: 'Add Text' },
            { id: 'draw', icon: <PenTool size={16} />, title: 'Draw / Sign' },
            { id: 'highlight', icon: <Highlighter size={16} />, title: 'Highlight' },
            { id: 'erase', icon: <Eraser size={16} />, title: 'Erase Object' },
          ].map(t => (
            <button
              key={t.id}
              className={`btn ${activeTool === t.id ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveTool(t.id)}
              title={t.title}
              style={{ padding: '8px 10px' }}
            >
              {t.icon}
            </button>
          ))}
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving} style={{ padding: '8px 16px' }}>
            {saving ? 'Saving...' : <><Download size={16} /> Save</>}
          </button>
          <button className="btn btn-secondary" onClick={onClose} style={{ padding: '8px 12px' }}>
            <X size={16} />
          </button>
        </div>
      </div>

      {/* Page navigation */}
      {totalPages > 1 && (
        <div style={{ padding: '8px 16px', backgroundColor: 'var(--bg-elevated)', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '12px' }}>
          <button className="btn btn-secondary" onClick={() => goPage(-1)} disabled={currentPage <= 1} style={{ padding: '6px 10px' }}>
            <ChevronLeft size={16} />
          </button>
          <span style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)' }}>
            Page {currentPage} of {totalPages}
          </span>
          <button className="btn btn-secondary" onClick={() => goPage(1)} disabled={currentPage >= totalPages} style={{ padding: '6px 10px' }}>
            <ChevronRight size={16} />
          </button>
        </div>
      )}

      {/* Canvas area */}
      <div style={{ backgroundColor: '#2a2d38', padding: '20px', display: 'flex', justifyContent: 'center', overflow: 'auto', maxHeight: '72vh' }}>
        {!loaded && (
          <div style={{ padding: '60px', textAlign: 'center' }}>
            <div style={{ width: '36px', height: '36px', border: '3px solid rgba(255,255,255,0.1)', borderTopColor: 'var(--accent)', borderRadius: '50%', margin: '0 auto 12px', animation: 'spin 0.8s linear infinite' }} />
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Rendering page...</p>
            <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          </div>
        )}
        <div ref={containerRef} style={{ boxShadow: '0 4px 24px rgba(0,0,0,0.3)', lineHeight: 0, display: loaded ? 'block' : 'none' }}></div>
      </div>
    </div>
  );
}
