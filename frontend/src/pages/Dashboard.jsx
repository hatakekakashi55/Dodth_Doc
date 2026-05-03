import React from 'react';
import { TOOL_CATEGORIES } from '../utils/tools.jsx';

export default function Dashboard({ onSelectTool }) {
  return (
    <div className="dashboard">
      <h1 className="page-title">All-in-One Document Suite</h1>
      <p className="page-subtitle">Convert, merge, compress, protect — everything your documents need.</p>
      {TOOL_CATEGORIES.map(category => (
        <section key={category.title}>
          <h2 className="section-title">{category.title}</h2>
          <div className="tools-grid">
            {category.tools.map(tool => (
              <div key={tool.id} className="tool-card" style={{ '--card-color': tool.color }} onClick={() => onSelectTool(tool.id)} id={`tool-${tool.id}`}>
                <div className="tool-card-icon">{tool.icon}</div>
                <h3>{tool.name}</h3>
                <p>{tool.desc}</p>
              </div>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
