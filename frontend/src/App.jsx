import React, { useState, useEffect } from 'react';
import { Sun, Moon } from 'lucide-react';
import Dashboard from './pages/Dashboard.jsx';
import ToolPage from './pages/ToolPage.jsx';

export default function App() {
  const [currentTool, setCurrentTool] = useState(null);
  const [theme, setTheme] = useState(() => localStorage.getItem('dodth-theme') || 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('dodth-theme', theme);
  }, [theme]);

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-logo" onClick={() => setCurrentTool(null)} style={{ cursor: 'pointer' }}>
          <div className="logo-icon">D</div>
          <span>DODTH</span>
        </div>
        <div className="header-actions">
          <button className="theme-toggle" onClick={() => setTheme(t => t === 'dark' ? 'light' : 'dark')} title="Toggle theme">
            {theme === 'dark' ? <Sun size={20} /> : <Moon size={20} />}
          </button>
        </div>
      </header>
      <main className="main-content">
        {currentTool
          ? <ToolPage toolId={currentTool} onBack={() => setCurrentTool(null)} />
          : <Dashboard onSelectTool={setCurrentTool} />
        }
      </main>
      <footer className="app-footer"><p>DODTH © 2026 — Document Utility Suite</p></footer>
    </div>
  );
}
