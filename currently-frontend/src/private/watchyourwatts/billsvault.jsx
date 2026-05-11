import React, { useEffect, useMemo, useState, useCallback } from 'react';
import './css/billsvault.css';

/*
 * Component: BillsVault
 * Purpose: Frontend UI for the PIN-protected bill upload, list, download, and delete workflow.
 */
export default function BillsVault() {
  const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

  const [step, setStep] = useState('loading');
  const [pin, setPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [files, setFiles] = useState([]);
  const [file, setFile] = useState(null);
  const [customFilename, setCustomFilename] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [sessionPin, setSessionPin] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Frontend State: summarizes stored bills for the unlocked Vault dashboard.
  const fileSummary = useMemo(() => {
    const totalBytes = files.reduce((sum, f) => sum + Number(f.fileSize || 0), 0);
    const newest = files
      .map((f) => f.uploadedAt)
      .filter(Boolean)
      .sort()
      .at(-1);

    return {
      count: files.length,
      totalMb: totalBytes / (1024 * 1024),
      newestLabel: newest ? new Date(newest).toLocaleDateString() : 'No uploads yet',
    };
  }, [files]);

  // API helper: attaches JWT Authorization for protected Bills Vault endpoints.
  const fetchWithAuth = useCallback(async (url, options = {}) => {
    const token = localStorage.getItem('token');
    if (!token) throw new Error('No auth token');

    const headers = {
      ...(options.headers || {}),
      'Authorization': `Bearer ${token}`,
    };

    const res = await fetch(url, {
      ...options,
      headers,
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || 'Request failed');
    }

    return res;
  }, []);

  /*
   * Hook: Bills Vault initial status
   * Purpose: Calls /api/vault/status to decide whether to show PIN setup or locked unlock UI.
   */
  useEffect(() => {
    const init = async () => {
      try {
        const res = await fetchWithAuth(`${API_BASE}/api/vault/status`);
        const data = await res.json();
        setStep(data.pinSet ? 'locked' : 'setup');
      } catch (err) {
        setError('Failed to initialise Bills Vault: ' + err.message);
      }
    };
    init();
  }, [API_BASE, fetchWithAuth]);

  /*
   * Event handler: Create PIN
   * Purpose: Validates matching 4-digit PINs, calls POST /api/vault/pin, and opens the Vault after setup.
   */
  const createPin = async () => {
    setError('');
    setIsLoading(true);

    if (pin.length !== 4 || !/^\d{4}$/.test(pin)) {
      setError('PIN must be exactly 4 digits.');
      setIsLoading(false);
      return;
    }

    if (pin !== confirmPin) {
      setError('PINs must match.');
      setIsLoading(false);
      return;
    }

    try {
      await fetchWithAuth(`${API_BASE}/api/vault/pin`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin }),
      });

      setSessionPin(pin);
      setPin('');
      setConfirmPin('');
      setStep('unlocked');
      loadFiles(pin);
    } catch (err) {
      setError('Failed to create PIN: ' + err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Event handler: verifies a 4-digit PIN through POST /api/vault/unlock before showing files.
  const unlockVault = async () => {
    setError('');
    setIsLoading(true);

    if (pin.length !== 4 || !/^\d{4}$/.test(pin)) {
      setError('PIN must be 4 digits.');
      setIsLoading(false);
      return;
    }

    try {
      await fetchWithAuth(`${API_BASE}/api/vault/unlock`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin }),
      });

      setSessionPin(pin);
      setPin('');
      setStep('unlocked');
      loadFiles(pin);
    } catch {
      setError('Incorrect PIN.');
    } finally {
      setIsLoading(false);
    }
  };

  // API function: loads bill metadata from POST /api/vault/files/list using the current session PIN.
  const loadFiles = async (pinToUse = sessionPin) => {
    try {
      const res = await fetchWithAuth(`${API_BASE}/api/vault/files/list`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin: pinToUse }),
      });
      const data = await res.json();
      setFiles(data);
    } catch (err) {
      setError('Failed to load files: ' + err.message);
    }
  };

  // Event handler: stores the selected PDF and pre-fills a display filename without changing file content.
  const handleFileSelect = (e) => {
    const selectedFile = e.target.files[0];
    setFile(selectedFile);
    
    if (selectedFile) {
      const nameWithoutExt = selectedFile.name.replace(/\.pdf$/i, '');
      setCustomFilename(nameWithoutExt);
    } else {
      setCustomFilename('');
    }
  };

  /*
   * Event handler: Upload PDF
   * Purpose: Sends a selected PDF to POST /api/vault/files with the session PIN; backend handles Upload Validation,
   * Encryption, Database storage, and Audit Logging.
   */
  const uploadFile = async () => {
    setError('');
    setInfo('');
    setIsLoading(true);

    if (!file) {
      setError('Please select a PDF file.');
      setIsLoading(false);
      return;
    }

    const finalFilename = customFilename.trim() 
      ? `${customFilename.trim()}.pdf` 
      : file.name;
    
    const renamedFile = new File([file], finalFilename, { type: file.type });

    const formData = new FormData();
    formData.append('file', renamedFile);

    try {
      await fetchWithAuth(
        `${API_BASE}/api/vault/files?pin=${encodeURIComponent(sessionPin)}`,
        {
          method: 'POST',
          body: formData,
        }
      );

      setFile(null);
      setCustomFilename('');
      setInfo('File uploaded successfully.');
      loadFiles();
    } catch (err) {
      setError('Upload failed: ' + err.message);
    } finally {
      setIsLoading(false);
    }
  };

  /*
   * Event handler: Download PDF
   * Purpose: Calls the backend download endpoint with the PIN, receives decrypted PDF bytes, and triggers a browser download.
   */
  const downloadFile = async (id, filename) => {
    setError('');
    setIsLoading(true);

    try {
      const res = await fetchWithAuth(`${API_BASE}/api/vault/files/${id}/download`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin: sessionPin }),
      });

      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      setError('Download failed: ' + err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Event handler: deletes one Vault file after confirmation and refreshes the metadata list.
  const deleteFile = async (id) => {
    if (!window.confirm('Delete this file?')) return;

    setIsLoading(true);
    try {
      await fetchWithAuth(
        `${API_BASE}/api/vault/files/${id}?pin=${encodeURIComponent(sessionPin)}`,
        { method: 'DELETE' }
      );
      loadFiles();
    } catch (err) {
      setError('Failed to delete file: ' + err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Event handler: clears the in-memory session PIN and returns the Vault to the locked state.
  const lockVault = () => {
    setSessionPin('');
    setStep('locked');
    setFiles([]);
    setFile(null);
    setCustomFilename('');
  };

  // Render state: the Vault switches between loading, setup, locked, and unlocked screens.

  if (step === 'loading') {
    return (
      <div className="vault-container">
        <p>Loading Bills Vault...</p>
      </div>
    );
  }

  if (step === 'setup') {
    return (
      <div className="vault-container">
        <h2 className="vault-title">Bills Vault</h2>
        <p className="vault-subtitle">Create a 4-digit PIN to secure your bills.</p>

        {error && <div className="vault-alert vault-alert-error">{error}</div>}

        <input
          type="password"
          maxLength={4}
          placeholder="Enter PIN"
          value={pin}
          onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))}
          className="vault-input"
        />

        <input
          type="password"
          maxLength={4}
          placeholder="Confirm PIN"
          value={confirmPin}
          onChange={(e) => setConfirmPin(e.target.value.replace(/\D/g, ''))}
          className="vault-input"
        />

        <button 
          onClick={createPin} 
          disabled={isLoading} 
          className="vault-btn vault-btn-primary"
        >
          {isLoading ? 'Creating...' : 'Create PIN'}
        </button>
      </div>
    );
  }

  if (step === 'locked') {
    return (
      <div className="vault-container">
        <h2 className="vault-title">Bills Vault</h2>
        <p className="vault-subtitle">Enter your 4-digit PIN to unlock.</p>

        {error && <div className="vault-alert vault-alert-error">{error}</div>}

        <input
          type="password"
          maxLength={4}
          placeholder="PIN"
          value={pin}
          onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))}
          className="vault-input"
        />

        <button 
          onClick={unlockVault} 
          disabled={isLoading} 
          className="vault-btn vault-btn-primary"
        >
          {isLoading ? 'Unlocking...' : 'Unlock'}
        </button>
      </div>
    );
  }

  // UNLOCKED
  return (
    <div className="vault-container">
      <div className="vault-header">
        <h2 className="vault-title">Bills Vault</h2>
        <button onClick={lockVault} className="vault-btn vault-btn-secondary">
          🔒 Lock
        </button>
      </div>

      {error && <div className="vault-alert vault-alert-error">{error}</div>}
      {info && <div className="vault-alert vault-alert-success">{info}</div>}

      <div className="vault-summary-grid">
        <div className="vault-summary-card">
          <span>Bills stored</span>
          <strong>{fileSummary.count}</strong>
        </div>
        <div className="vault-summary-card">
          <span>Storage used</span>
          <strong>{fileSummary.totalMb.toFixed(2)} MB</strong>
        </div>
        <div className="vault-summary-card">
          <span>Newest bill</span>
          <strong>{fileSummary.newestLabel}</strong>
        </div>
      </div>

      <div className="vault-upload-section">
        <input
          type="file"
          accept="application/pdf"
          onChange={handleFileSelect}
          className="vault-file-input"
        />
        
        {file && (
          <div className="vault-filename-section">
            <label className="vault-label">Display Name:</label>
            <input
              type="text"
              placeholder="e.g., January 2024 Electric Bill"
              value={customFilename}
              onChange={(e) => setCustomFilename(e.target.value)}
              className="vault-input"
            />
            <small className="vault-hint">This is how the file will appear in your list</small>
          </div>
        )}

        <button 
          onClick={uploadFile} 
          disabled={isLoading || !file} 
          className="vault-btn vault-btn-primary"
        >
          {isLoading ? 'Uploading...' : 'Upload PDF'}
        </button>
      </div>

      {files.length === 0 ? (
  <p className="vault-empty-message">No bills uploaded yet.</p>
) : (
  <div className="vault-file-list-wrapper">
    <ul className="vault-file-list">
      {files.map((f) => (
        <li key={f.id} className="vault-file-item">
          <span className="vault-filename">{f.originalFilename}</span>
          <div className="vault-file-actions">
            <button 
              onClick={() => downloadFile(f.id, f.originalFilename)} 
              disabled={isLoading}
              className="vault-btn vault-btn-download"
            >
              📥 Download
            </button>
            <button 
              onClick={() => deleteFile(f.id)} 
              disabled={isLoading}
              className="vault-btn vault-btn-delete"
            >
              🗑️ Delete
            </button>
          </div>
        </li>
      ))}
    </ul>
  </div>
)}
    </div>
  );
}
