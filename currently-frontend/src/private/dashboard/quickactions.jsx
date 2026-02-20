import React from 'react';

export default function QuickActions({
  onVault,
  onAppliances,
  onMap,
  onInsights,
}) {
  return (
    <section className="card quick-actions">
      <div className="card-header">
        <h2 className="card-title">Quick Actions</h2>
      </div>
      <div className="quick-actions-grid">
        <button type="button" className="action-btn" onClick={onAppliances}>
          Add Appliances
        </button>
        <button type="button" className="action-btn" onClick={onMap}>
          Map Rooms or Floors
        </button>
        <button type="button" className="action-btn" onClick={onVault}>
          Add Bills to Vault
        </button>
        <button type="button" className="action-btn" onClick={onInsights}>
          Get Smart Insights
        </button>
      </div>
    </section>
  );
}
