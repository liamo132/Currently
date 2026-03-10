import React from 'react';

export default function SystemStatus({ lastSync, warnings = [], vaultActive, onVault }) {
  const last = lastSync ? new Date(lastSync).toLocaleString() : 'Unknown';
  const vaultLabel = vaultActive ? 'Bills Vault set up' : 'Bills Vault not set up';

  return (
    <section className="card system-status">
      <div className="card-header">
        <h2 className="card-title">System Status</h2>
      </div>

      <div className="status-indicator">
        <div className="status-dot" aria-hidden />
        <div className="status-text">
          <div className="status-label">Last data sync</div>
          <div className="status-time">{last}</div>
        </div>
      </div>

      <div className="status-indicator">
        <div className="status-dot" aria-hidden style={{ background: vaultActive ? '#10b981' : '#f59e0b' }} />
        <div className="status-text">
          <div className="status-label">Bills Vault</div>
          <div className="status-time">{vaultLabel}</div>
        </div>
        <button type="button" className="btn btn--tertiary" onClick={onVault}>
          Manage
        </button>
      </div>

      {warnings.length > 0 && (
        <div className="system-status__warnings">
          <h3>Setup notes</h3>
          <ul>
            {warnings.map((w, i) => (
              <li key={`${w}-${i}`}>{w}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
