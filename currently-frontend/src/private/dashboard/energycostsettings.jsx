import React, { useEffect, useState } from 'react';
import './css/energycostsettings.css';

export default function EnergyCostSettings({
  pricePerKwh,
  setPricePerKwh,
  providerName,
  setProviderName,
  onPersist,
  saving = false,
}) {
  const [draftCents, setDraftCents] = useState(() => Number(pricePerKwh || 0) * 100);
  const [draftProvider, setDraftProvider] = useState(() => providerName || '');
  const [isEditing, setIsEditing] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setDraftCents(Number(pricePerKwh || 0) * 100);
    setDraftProvider(providerName || '');
  }, [pricePerKwh, providerName]);

  const savePrice = async () => {
    if (!Number.isFinite(draftCents) || draftCents < 0) {
      setError('Please enter a valid non-negative cost per kWh.');
      return;
    }
    setError('');
    const normalized = Number.isFinite(draftCents) ? Number(draftCents) : 0;
    setPricePerKwh(normalized / 100);
    setProviderName(draftProvider.trim());
    if (onPersist) {
      await onPersist(normalized / 100, draftProvider.trim());
    }
    setIsEditing(false);
  };

  const cancelEdit = () => {
    setDraftCents(Number(pricePerKwh || 0) * 100);
    setDraftProvider(providerName || '');
    setError('');
    setIsEditing(false);
  };

  const centsLabel = `${Math.round((Number(pricePerKwh) || 0) * 100)}c/kWh`;
  const providerLabel = providerName?.trim() ? providerName.trim() : 'Provider not set';

  return (
    <section className="card energy-cost-settings">
      <div className="card-header">
        <h2 className="card-title">Energy Cost Settings</h2>
        <button
          type="button"
          className="btn btn--tertiary"
          onClick={() => setIsEditing((prev) => !prev)}
          aria-expanded={isEditing}
        >
          {isEditing ? 'Close' : 'Edit'}
        </button>
      </div>

      <p className="card-subtitle">Used for all cost estimates and Smart Insights recommendations.</p>

      <div className="energy-settings-summary">
        <div className="energy-settings-summary__value">
          {centsLabel} <span className="energy-settings-summary__sep">-</span> {providerLabel}
        </div>
        <div className="energy-settings-summary__meta">Saved to account and cached locally.</div>
      </div>

      {isEditing && (
        <form className="form-grid energy-settings-form" onSubmit={(e) => e.preventDefault()}>
          <div className="form-group">
            <label className="form-label" htmlFor="cost-per-kwh">
              Cost per kWh (cents)
            </label>
            <input
              id="cost-per-kwh"
              className={`form-input ${error ? 'form-input--error' : ''}`}
              type="number"
              min="0"
              step="1"
              value={Number.isFinite(draftCents) ? Math.round(draftCents) : 0}
              onChange={(e) => {
                const cents = Number(e.target.value || 0);
                setDraftCents(cents);
                if (error) setError('');
              }}
              placeholder="30"
              aria-invalid={Boolean(error)}
              aria-describedby={error ? 'energy-settings-error' : undefined}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="provider-name">
              Provider Name (optional)
            </label>
            <input
              id="provider-name"
              className="form-input"
              type="text"
              value={draftProvider}
              onChange={(e) => setDraftProvider(e.target.value)}
              placeholder="e.g. Electric Ireland"
            />
          </div>

          {error && (
            <p className="form-error" id="energy-settings-error" role="alert">
              {error}
            </p>
          )}

          <div className="form-actions">
            <button type="button" className="btn btn--secondary" onClick={cancelEdit} disabled={saving}>
              Cancel
            </button>
            <button type="button" className="btn btn--primary" onClick={savePrice} disabled={saving}>
              {saving ? 'Saving...' : 'Save settings'}
            </button>
          </div>
          <p className="form-note">
            Keep this up to date to ensure weekly trend comparisons stay accurate.
          </p>
        </form>
      )}
    </section>
  );
}
