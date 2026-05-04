import React, { useEffect, useRef, useState } from 'react';
import * as pdfjsLib from 'pdfjs-dist/build/pdf';
import * as fabric from 'fabric';
import { PDFDocument } from 'pdf-lib';
import { Save, Download, X, Type, PenTool, Highlighter, MousePointer2 } from 'lucide-react';

// Configure PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.js`;

export default function PdfEditor({ pdfBytes, originalFile, onClose }) {
  const containerRef = useRef(null);
  const [fabricCanvas, setFabricCanvas] = useState(null);
  const [pdfDoc, setPdfDoc] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [saving, setSaving] = useState(false);
  const [activeTool, setActiveTool] = useState('select'); // select, text, draw, highlight

  // Initialize PDF and Canvas
  useEffect(() => {
    let canvasInst;
    
    const initPdf = async () => {
      try {
        const loadingTask = pdfjsLib.getDocument({ data: new Uint8Array(pdfBytes) });
        const pdf = await loadingTask.promise;
        setPdfDoc(pdf);
        setTotalPages(pdf.numPages);
        
        renderPage(pdf, 1);
      } catch (err) {
        console.error("Error loading PDF:", err);
      }
    };

    const renderPage = async (pdf, pageNum) => {
      const page = await pdf.getPage(pageNum);
      const viewport = page.getViewport({ scale: 1.5 });

      // Setup hidden canvas for PDF rendering
      const pdfCanvas = document.createElement('canvas');
      const context = pdfCanvas.getContext('2d');
      pdfCanvas.height = viewport.height;
      pdfCanvas.width = viewport.width;

      const renderContext = {
        canvasContext: context,
        viewport: viewport
      };
      
      await page.render(renderContext).promise;

      // Setup Fabric canvas
      if (containerRef.current) {
        containerRef.current.innerHTML = '';
        const fabCanvasEl = document.createElement('canvas');
        fabCanvasEl.id = 'fabric-canvas';
        containerRef.current.appendChild(fabCanvasEl);

        canvasInst = new fabric.Canvas('fabric-canvas', {
          width: viewport.width,
          height: viewport.height,
          selection: true
        });

        // Set PDF as background
        fabric.Image.fromURL(pdfCanvas.toDataURL('image/png'), (img) => {
          canvasInst.setBackgroundImage(img, canvasInst.renderAll.bind(canvasInst));
        });

        setFabricCanvas(canvasInst);
      }
    };

    initPdf();

    return () => {
      if (canvasInst) canvasInst.dispose();
    };
  }, [pdfBytes]);

  // Handle Tool Changes
  useEffect(() => {
    if (!fabricCanvas) return;

    fabricCanvas.isDrawingMode = false;
    fabricCanvas.selection = true;
    
    // Remove previous listeners
    fabricCanvas.off('mouse:down');

    if (activeTool === 'draw') {
      fabricCanvas.isDrawingMode = true;
      fabricCanvas.freeDrawingBrush.color = 'red';
      fabricCanvas.freeDrawingBrush.width = 3;
    } else if (activeTool === 'highlight') {
      fabricCanvas.isDrawingMode = true;
      fabricCanvas.freeDrawingBrush.color = 'rgba(255, 255, 0, 0.4)';
      fabricCanvas.freeDrawingBrush.width = 20;
    } else if (activeTool === 'text') {
      fabricCanvas.selection = false;
      fabricCanvas.on('mouse:down', (o) => {
        const pointer = fabricCanvas.getPointer(o.e);
        const text = new fabric.IText('Type here...', {
          left: pointer.x,
          top: pointer.y,
          fontFamily: 'Arial',
          fontSize: 24,
          fill: 'blue'
        });
        fabricCanvas.add(text);
        fabricCanvas.setActiveObject(text);
        text.enterEditing();
        text.selectAll();
        setActiveTool('select'); // revert to select mode
      });
    }
  }, [activeTool, fabricCanvas]);

  const handleSave = async () => {
    if (!fabricCanvas) return;
    setSaving(true);
    try {
      // 1. Export annotations (without background)
      const bg = fabricCanvas.backgroundImage;
      fabricCanvas.backgroundImage = null;
      const overlayDataUrl = fabricCanvas.toDataURL({ format: 'png', multiplier: 1 });
      fabricCanvas.setBackgroundImage(bg, fabricCanvas.renderAll.bind(fabricCanvas));

      // 2. Load original PDF with pdf-lib
      const pdfDocLib = await PDFDocument.load(pdfBytes);
      const pages = pdfDocLib.getPages();
      const pageToEdit = pages[currentPage - 1]; // 0-indexed

      // 3. Embed the overlay PNG
      const overlayImageBytes = await fetch(overlayDataUrl).then(res => res.arrayBuffer());
      const embeddedImage = await pdfDocLib.embedPng(overlayImageBytes);

      // 4. Draw image over the page
      const { width, height } = pageToEdit.getSize();
      pageToEdit.drawImage(embeddedImage, {
        x: 0,
        y: 0,
        width: width,
        height: height,
      });

      // 5. Save and download
      const modifiedPdfBytes = await pdfDocLib.save();
      const blob = new Blob([modifiedPdfBytes], { type: 'application/pdf' });
      const url = URL.createObjectURL(blob);
      
      const link = document.createElement('a');
      link.href = url;
      const baseName = originalFile.name.substring(0, originalFile.name.lastIndexOf('.'));
      link.download = `${baseName}_edited.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (err) {
      alert("Error saving PDF: " + err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="editor-container" style={{ marginTop: '20px', border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden' }}>
      <div className="editor-toolbar-custom" style={{ padding: '15px', backgroundColor: 'var(--bg-card)', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
        <h3 style={{ margin: 0 }}>Editing PDF: {originalFile.name}</h3>
        
        <div className="editor-tools" style={{ display: 'flex', gap: '5px' }}>
          <button className={`btn ${activeTool === 'select' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTool('select')} title="Select"><MousePointer2 size={18} /></button>
          <button className={`btn ${activeTool === 'text' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTool('text')} title="Add Text"><Type size={18} /></button>
          <button className={`btn ${activeTool === 'draw' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTool('draw')} title="Draw/Sign"><PenTool size={18} /></button>
          <button className={`btn ${activeTool === 'highlight' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTool('highlight')} title="Highlight"><Highlighter size={18} /></button>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn btn-secondary" onClick={onClose}><X size={18} /> Cancel</button>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving...' : <><Download size={18} /> Save & Download</>}
          </button>
        </div>
      </div>
      
      <div className="editor-workspace" style={{ backgroundColor: '#e9ecef', padding: '20px', display: 'flex', justifyContent: 'center', overflow: 'auto', minHeight: '600px' }}>
        <div ref={containerRef} style={{ boxShadow: '0 4px 12px rgba(0,0,0,0.15)', backgroundColor: 'white' }}></div>
      </div>
    </div>
  );
}
