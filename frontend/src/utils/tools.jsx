import React from 'react';
import { 
  FileText, FileSpreadsheet, Presentation, Image as ImageIcon, 
  FileOutput, FileImage, Combine, Split, RotateCw, 
  Shrink, Lock, Unlock, Droplet, Edit3
} from 'lucide-react';

export const TOOL_CATEGORIES = [
  {
    title: 'Live Editing',
    tools: [
      { id: 'live-editor', name: 'Live Editor', desc: 'Edit PDF & Word directly', icon: <Edit3 size={24} />, color: 'var(--primary)', accept: '.pdf,.docx,.txt', endpoint: '', single: true, isEditor: true },
    ],
  },
  {
    title: 'Convert to PDF',
    tools: [
      { id: 'word-to-pdf', name: 'Word to PDF', desc: 'Convert DOCX to PDF', icon: <FileText size={24} />, color: 'var(--word-pdf)', accept: '.docx,.doc', endpoint: '/api/convert/word-to-pdf', single: true },
      { id: 'excel-to-pdf', name: 'Excel to PDF', desc: 'Convert XLSX to PDF', icon: <FileSpreadsheet size={24} />, color: 'var(--excel-pdf)', accept: '.xlsx,.xls', endpoint: '/api/convert/excel-to-pdf', single: true },
      { id: 'ppt-to-pdf', name: 'PPT to PDF', desc: 'Convert PPTX to PDF', icon: <Presentation size={24} />, color: 'var(--ppt-pdf)', accept: '.pptx,.ppt', endpoint: '/api/convert/ppt-to-pdf', single: true },
      { id: 'jpg-to-pdf', name: 'JPG to PDF', desc: 'Images to PDF', icon: <ImageIcon size={24} />, color: 'var(--jpg-pdf)', accept: '.jpg,.jpeg,.png,.webp,.bmp', endpoint: '/api/convert/jpg-to-pdf', single: false },
      { id: 'html-to-pdf', name: 'HTML to PDF', desc: 'Web page to PDF', icon: <FileOutput size={24} />, color: 'var(--watermark)', accept: '.html,.htm', endpoint: '/api/convert/html-to-pdf', single: true },
    ],
  },
  {
    title: 'Convert from PDF',
    tools: [
      { id: 'pdf-to-word', name: 'PDF to Word', desc: 'PDF to editable DOCX', icon: <FileOutput size={24} />, color: 'var(--pdf-word)', accept: '.pdf', endpoint: '/api/convert/pdf-to-word', single: true },
      { id: 'pdf-to-ppt', name: 'PDF to PPT', desc: 'PDF to PowerPoint', icon: <Presentation size={24} />, color: 'var(--ppt-pdf)', accept: '.pdf', endpoint: '/api/convert/pdf-to-ppt', single: true },
      { id: 'pdf-to-excel', name: 'PDF to Excel', desc: 'PDF to Spreadsheet', icon: <FileSpreadsheet size={24} />, color: 'var(--excel-pdf)', accept: '.pdf', endpoint: '/api/convert/pdf-to-excel', single: true },
      { id: 'pdf-to-jpg', name: 'PDF to JPG', desc: 'PDF pages to images', icon: <FileImage size={24} />, color: 'var(--pdf-jpg)', accept: '.pdf', endpoint: '/api/convert/pdf-to-jpg', single: true },
      { id: 'ocr', name: 'OCR PDF', desc: 'Make scanned PDF editable', icon: <FileText size={24} />, color: 'var(--word-pdf)', accept: '.pdf', endpoint: '/api/convert/ocr', single: true },
    ],
  },
  {
    title: 'Organize PDF',
    tools: [
      { id: 'merge', name: 'Merge PDF', desc: 'Combine multiple PDFs', icon: <Combine size={24} />, color: 'var(--merge)', accept: '.pdf', endpoint: '/api/organize/merge', single: false },
      { id: 'split', name: 'Split PDF', desc: 'Extract pages', icon: <Split size={24} />, color: 'var(--split)', accept: '.pdf', endpoint: '/api/organize/split', single: true, hasPages: true },
      { id: 'rotate', name: 'Rotate PDF', desc: 'Rotate pages', icon: <RotateCw size={24} />, color: 'var(--rotate)', accept: '.pdf', endpoint: '/api/organize/rotate', single: true, hasAngle: true },
      { id: 'compress', name: 'Compress PDF', desc: 'Reduce file size', icon: <Shrink size={24} />, color: 'var(--compress)', accept: '.pdf', endpoint: '/api/organize/compress', single: true },
      { id: 'add-page-numbers', name: 'Page Numbers', desc: 'Add page numbers', icon: <Droplet size={24} />, color: 'var(--watermark)', accept: '.pdf', endpoint: '/api/organize/add-page-numbers', single: true },
      { id: 'delete-pages', name: 'Remove Pages', desc: 'Delete specific pages', icon: <Split size={24} />, color: 'var(--split)', accept: '.pdf', endpoint: '/api/organize/delete-pages', single: true, hasPages: true },
    ],
  },
  {
    title: 'PDF Security',
    tools: [
      { id: 'protect', name: 'Protect PDF', desc: 'Add password', icon: <Lock size={24} />, color: 'var(--protect)', accept: '.pdf', endpoint: '/api/security/protect', single: true, hasPassword: true },
      { id: 'unlock', name: 'Unlock PDF', desc: 'Remove password', icon: <Unlock size={24} />, color: 'var(--unlock)', accept: '.pdf', endpoint: '/api/security/unlock', single: true, hasPassword: true },
      { id: 'watermark', name: 'Watermark', desc: 'Add text stamp', icon: <Droplet size={24} />, color: 'var(--watermark)', accept: '.pdf', endpoint: '/api/security/watermark', single: true, hasWatermark: true },
    ],
  },
];

export function getToolById(id) {
  for (const cat of TOOL_CATEGORIES) {
    const found = cat.tools.find(t => t.id === id);
    if (found) return found;
  }
  return null;
}

export function formatFileSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}
