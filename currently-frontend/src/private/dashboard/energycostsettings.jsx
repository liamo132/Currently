import React, { useEffect, useState } from 'react';
import './css/energycostsettings.css';

export default function EnergyCostSettings({
  pricePerKwh,
  setPricePerKwh,
  providerName,
  setProviderName,
}) {
  const [draftCents, setDraftCents] = useState(() => Number(pricePerKwh || 0) * 100);

  useEffect(() => {
    setDraftCents(Number(pricePerKwh || 0) * 100);
  }, [pricePerKwh]);

  const savePrice = () => {
    const normalized = Number.isFinite(draftCents) ? draftCents : 0;
    setPricePerKwh(normalized / 100);
  };

  return (
    <section className="card energy-cost-settings">
      <div className="card-header">
        <h2 className="card-title">Energy Cost Settings</h2>
      </div>
      <p className="card-subtitle">
        Different providers charge different rates, so this drives all cost maths.
      </p>

      <form className="form-grid" onSubmit={(e) => e.preventDefault()}>
        <div className="form-group">
          <label className="form-label" htmlFor="cost-per-kwh">
            Cost per kWh (cents)
          </label>
          <input
            id="cost-per-kwh"
            className="form-input"
            type="number"
            min="0"
            step="1"
            value={Number.isFinite(draftCents) ? Math.round(draftCents) : 0}
            onChange={(e) => {
              const cents = Number(e.target.value || 0);
              setDraftCents(cents);
            }}
            placeholder="30.0"
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
            value={providerName}
            onChange={(e) => setProviderName(e.target.value)}
            placeholder="e.g. Electric Ireland"
          />
        </div>

        <p className="form-note">
          Saved locally for now. Later this will be stored per-user in Postgres.
        </p>
        <div className="form-actions">
          <button type="button" className="action-btn" onClick={savePrice}>
            Save
          </button>
        </div>
      </form>
    </section>
  );
}
