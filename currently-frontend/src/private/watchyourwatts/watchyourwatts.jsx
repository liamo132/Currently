// watchyourwatts/watchyourwatts.jsx
import React, { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import HeaderUser from '../../public/components/header-user';
import RoomConsumption from './roomconsumption';
import BiggestEaters from './biggesteaters';
import CostForecast from './costforecast';
import BillsVault from './billsvault';
import '../shared/private-layout.css';
import './css/watchyourwatts.css';
import './css/watchtabs.css';

export default function WatchYourWatts() {
  const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

  const [activeTab, setActiveTab] = useState('usage');
  const [reductionPercent, setReductionPercent] = useState(10);

  const [rooms, setRooms] = useState([]);
  const [appliances, setAppliances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // read-only here (user edits this in Dashboard later)
  const [pricePerKwh, setPricePerKwh] = useState(() => {
    const saved = localStorage.getItem('pricePerKwh');
    return saved ? Number(saved) : 0.30;
  });

  // if dashboard changes it, this keeps it in sync when user returns to this page.
  useEffect(() => {
    const onStorage = (e) => {
      if (e.key === 'pricePerKwh') setPricePerKwh(Number(e.newValue) || 0.30);
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const navRef = useRef(null);
  const [indicator, setIndicator] = useState({ left: 0, width: 0 });
  const appliancesWithCost = useMemo(
    () =>
      (appliances || []).map((a) => {
        const dailyKwh = Number(a.dailyKWh || 0);
        const computedDailyCost = dailyKwh * pricePerKwh || Number(a.estimatedDailyCost || 0);
        return { ...a, computedDailyCost };
      }),
    [appliances, pricePerKwh]
  );

  const tabs = useMemo(
    () => [
      { id: 'usage', label: 'Usage Breakdown' },
      { id: 'cost', label: 'Cost Forecast' },
      { id: 'bills', label: 'Bills Vault' },
    ],
    []
  );

  const fetchWithAuth = useCallback(async (url, options = {}) => {
    const token = localStorage.getItem('token');
    if (!token) throw new Error('No authentication token found. Please log in again.');

    const headers = {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    };

    const res = await fetch(url, { ...options, headers });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || `Request failed with status ${res.status}`);
    }
    return res;
  }, []);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError('');

        const roomsRes = await fetchWithAuth(`${API_BASE}/api/users/me/rooms`);
        const roomsData = await roomsRes.json();

        const appsRes = await fetchWithAuth(`${API_BASE}/api/users/me/appliances`);
        const appsData = await appsRes.json();

        setRooms(roomsData);
        setAppliances(appsData);
      } catch (e) {
        console.error(e);
        setError('Failed to load energy insights.');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [API_BASE, fetchWithAuth]);

  const updateIndicator = useCallback(() => {
    const root = navRef.current;
    if (!root) return;
    const activeBtn = root.querySelector(`[data-tab="${activeTab}"]`);
    if (!activeBtn) return;

    const rootRect = root.getBoundingClientRect();
    const btnRect = activeBtn.getBoundingClientRect();
    setIndicator({ left: btnRect.left - rootRect.left, width: btnRect.width });
  }, [activeTab]);

  useEffect(() => {
    updateIndicator();
    window.addEventListener('resize', updateIndicator);
    return () => window.removeEventListener('resize', updateIndicator);
  }, [updateIndicator]);

  if (loading) {
    return (
      <div className="watchyourwatts-container private-page">
        <HeaderUser activePage="watchyourwatts" />
        <div className="watchyourwatts-content private-content">
          <h1 className="watchyourwatts-title private-title">Watch Your Watts</h1>
          <p className="watchyourwatts-subtitle private-subtitle">visual insights into where your electricity is being used.</p>
          <p>Loading insights...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="watchyourwatts-container private-page">
      <HeaderUser activePage="watchyourwatts" />
      <div className="watchyourwatts-content private-content">
        <h1 className="watchyourwatts-title private-title">Watch Your Watts</h1>
        <p className="watchyourwatts-subtitle private-subtitle">visual insights into where your electricity is being used.</p>

        {error && (
          <div className="myappliances-error" style={{ marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        {/* tabs stay in the same place, just adding new options */}
        <div className="watchtabs-wrap" ref={navRef}>
          <div
            className="watchtabs-indicator"
            style={{ transform: `translateX(${indicator.left}px)`, width: `${indicator.width}px` }}
          />
          {tabs.map((t) => (
            <button
              key={t.id}
              type="button"
              data-tab={t.id}
              className={`watchtabs-btn ${activeTab === t.id ? 'active' : ''}`}
              onClick={() => setActiveTab(t.id)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {activeTab === 'usage' && (
          <div className="watchyourwatts-grid" style={{ marginTop: '1.5rem' }}>
            <RoomConsumption rooms={rooms} appliances={appliancesWithCost} />
            <BiggestEaters rooms={rooms} appliances={appliancesWithCost} />
          </div>
        )}

        {activeTab === 'cost' && (
          <div style={{ marginTop: '1.5rem' }}>
            {/* only keep reduction here. cost per kWh moved to Dashboard. */}
            <div className="watchtabs-controls">
              <label className="watchtabs-label">
                Target reduction (% by end of month)
                <input
                  className="watchtabs-input watchtabs-input--wide"
                  type="number"
                  min="1"
                  max="10"
                  value={reductionPercent}
                  onChange={(e) => setReductionPercent(Number(e.target.value || 1))}
                />
              </label>
            </div>

            <CostForecast
              appliances={appliancesWithCost}
              reductionPercent={reductionPercent}
              pricePerKwh={pricePerKwh}
            />
          </div>
        )}

        {activeTab === 'bills' && (
          <div style={{ marginTop: '1.5rem' }}>
            <BillsVault />
          </div>
        )}
      </div>
    </div>
  );
}
