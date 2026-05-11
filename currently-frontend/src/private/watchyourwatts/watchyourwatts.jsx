// watchyourwatts/watchyourwatts.jsx
import React, { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import HeaderUser from '../../public/components/header-user';
import RoomConsumption from './roomconsumption';
import RoomSummaryCards from './roomsummarycards';
import BiggestEaters from './biggesteaters';
import CostForecast from './costforecast';
import BillsVault from './billsvault';
import '../shared/private-layout.css';
import './css/watchyourwatts.css';
import './css/watchtabs.css';

/*
 * Component: WatchYourWatts
 * Purpose: Private analysis page that combines Room consumption, biggest Appliance costs,
 * Cost Forecast, Bills Vault, and Room Summary tabs.
 */
export default function WatchYourWatts() {
  const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

  const [activeTab, setActiveTab] = useState('usage');
  const [reductionPercent, setReductionPercent] = useState(10);
  const [costPeriod, setCostPeriod] = useState('weekly');

  const [rooms, setRooms] = useState([]);
  const [appliances, setAppliances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Frontend State: read-only tariff here; the user edits pricePerKwh on Dashboard.
  const [pricePerKwh, setPricePerKwh] = useState(() => {
    const saved = localStorage.getItem('pricePerKwh');
    return saved ? Number(saved) : 0.30;
  });

  // Hook: keeps Watch Your Watts synced if Dashboard changes the cached pricePerKwh in another tab.
  useEffect(() => {
    const onStorage = (e) => {
      if (e.key === 'pricePerKwh') setPricePerKwh(Number(e.newValue) || 0.30);
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const navRef = useRef(null);
  const [indicator, setIndicator] = useState({ left: 0, width: 0 });

  // Cost Calculation: recomputes daily Appliance cost from dailyKWh so tariff changes flow into charts.
  const appliancesWithCost = useMemo(
    () =>
      (appliances || []).map((a) => {
        const dailyKwh = Number(a.dailyKWh || 0);
        const computedDailyCost = dailyKwh * pricePerKwh || Number(a.estimatedDailyCost || 0);
        return { ...a, computedDailyCost };
      }),
    [appliances, pricePerKwh]
  );

  // Frontend State: tab definitions drive the Watch Your Watts segmented navigation.
  const tabs = useMemo(
    () => [
      { id: 'usage', label: 'Usage Breakdown' },
      { id: 'cost', label: 'Cost Forecast' },
      { id: 'bills', label: 'Bills Vault' },
      { id: 'rooms', label: 'Room Summary' },
    ],
    []
  );

  /*
   * Forecast calculation: costComparison
   * Purpose: Computes current, optimized, and saving values for daily/weekly/monthly views.
   * Important formula: saving = current cost * selected reduction percent.
   */
  const costComparison = useMemo(() => {
    const pct = Math.min(10, Math.max(1, Number(reductionPercent) || 1));
    const targetFraction = pct / 100;
    const daily = appliancesWithCost.reduce(
      (sum, appliance) => sum + Number(appliance.computedDailyCost || 0),
      0
    );

    const buildPeriod = (current) => {
      const saving = current * targetFraction;
      return {
        current,
        optimized: current - saving,
        saving,
      };
    };

    return {
      daily: buildPeriod(daily),
      weekly: buildPeriod(daily * 7),
      monthly: buildPeriod(daily * 30),
    };
  }, [appliancesWithCost, reductionPercent]);

  // API helper: adds JWT Authorization for protected Room and Appliance backend queries.
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

  /*
   * Hook: Watch Your Watts data load
   * Purpose: Loads Rooms and user Appliances from the backend so charts can group usage by Room and Appliance.
   */
  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError('');

        // API call: Room data supports room-level consumption and summary cards.
        const roomsRes = await fetchWithAuth(`${API_BASE}/api/users/me/rooms`);
        const roomsData = await roomsRes.json();

        // API call: Appliance data includes dailyKWh and estimatedDailyCost calculated by the backend.
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

  // UI helper: positions the animated tab indicator under the active Watch Your Watts tab.
  const updateIndicator = useCallback(() => {
    const root = navRef.current;
    if (!root) return;
    const activeBtn = root.querySelector(`[data-tab="${activeTab}"]`);
    if (!activeBtn) return;

    const rootRect = root.getBoundingClientRect();
    const btnRect = activeBtn.getBoundingClientRect();
    setIndicator({ left: btnRect.left - rootRect.left, width: btnRect.width });
  }, [activeTab]);

  // Hook: updates the tab indicator on tab changes and browser resize.
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

        {/* Watch Your Watts navigation: tab state controls which analysis panel is rendered. */}
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
            {/* Forecast controls: reduction percent changes projected Savings; Cost view changes daily/weekly/monthly summary. */}
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
              <label className="watchtabs-label">
                Cost view
                <select
                  className="watchtabs-input watchtabs-input--wide"
                  value={costPeriod}
                  onChange={(e) => setCostPeriod(e.target.value)}
                >
                  <option value="daily">Daily</option>
                  <option value="weekly">Weekly</option>
                  <option value="monthly">Monthly</option>
                </select>
              </label>
            </div>

            <div className="cost-period-summary">
              <div className="cost-period-card">
                <span>{costPeriod} current</span>
                <strong>EUR {costComparison[costPeriod].current.toFixed(2)}</strong>
              </div>
              <div className="cost-period-card">
                <span>After {Math.min(10, Math.max(1, Number(reductionPercent) || 1))}% target</span>
                <strong>EUR {costComparison[costPeriod].optimized.toFixed(2)}</strong>
              </div>
              <div className="cost-period-card cost-period-card--saving">
                <span>Potential saving</span>
                <strong>EUR {costComparison[costPeriod].saving.toFixed(2)}</strong>
              </div>
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

        {activeTab === 'rooms' && (
          <RoomSummaryCards rooms={rooms} appliances={appliancesWithCost} />
        )}
      </div>
    </div>
  );
}
