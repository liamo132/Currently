import React from 'react';
import './css/vaultstatus.css';

export default function VaultStatus({ vaultActive }) {
  return (
    <section className="card vault-status-card">
      <div className="card-header">
        <h2 className="card-title">Bills Vault</h2>
      </div>

      <div className="vault-box">
        <div className="vault-icon" aria-hidden>
          🔒
        </div>
        <p className="vault-text">{vaultActive ? 'Vault active' : 'Vault not set up'}</p>
      </div>
    </section>
  );
}
