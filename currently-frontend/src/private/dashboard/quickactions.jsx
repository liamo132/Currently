import React from 'react';

/*
 * Component: QuickActions
 * Purpose: Provides Dashboard shortcuts into Smart Insights, My Appliances, Map My House, and Bills Vault.
 */
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
      <p className="card-subtitle">Do one thing now: generate focused savings actions.</p>

      <button type="button" className="btn btn--primary quick-actions__primary" onClick={onInsights}>
        Get Smart Insights
      </button>

      <div className="quick-actions-list">
        <button type="button" className="btn btn--secondary" onClick={onAppliances}>
          Add appliances
        </button>
        <button type="button" className="btn btn--secondary" onClick={onMap}>
          Map rooms or floors
        </button>
        <button type="button" className="btn btn--tertiary" onClick={onVault}>
          Manage bills vault
        </button>
      </div>
    </section>
  );
}
