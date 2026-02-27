import React, { useEffect, useMemo, useState, useCallback } from 'react';
import HeaderUser from '../../public/components/header-user';
import SmartInsights from './smartinsights';
import '../shared/private-layout.css';
import '../watchyourwatts/css/watchyourwatts.css';

export default function SmartInsightsPage() {
  const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

  const [appliances, setAppliances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // keeping this in sync with dashboard setting for now
  const [pricePerKwh, setPricePerKwh] = useState(() => {
    const saved = localStorage.getItem('pricePerKwh');
    return saved ? Number(saved) : 0.30;
  });

  useEffect(() => {
    const onStorage = (e) => {
      if (e.key === 'pricePerKwh') setPricePerKwh(Number(e.newValue) || 0.30);
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const appliancesWithCost = useMemo(
    () =>
      (appliances || []).map((a) => {
        const dailyKwh = Number(a.dailyKWh || 0);
        const computedDailyCost = dailyKwh * pricePerKwh || Number(a.estimatedDailyCost || 0);
        return { ...a, computedDailyCost };
      }),
    [appliances, pricePerKwh]
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

        const appsRes = await fetchWithAuth(`${API_BASE}/api/users/me/appliances`);
        const appsData = await appsRes.json();
        setAppliances(appsData);
      } catch (e) {
        console.error(e);
        setError('Failed to load smart insights data.');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [API_BASE, fetchWithAuth]);

  return (
    <div className="watchyourwatts-container private-page">
      <HeaderUser activePage="smartinsights" />
      <div className="watchyourwatts-content private-content">
        <h1 className="watchyourwatts-title private-title">Smart Insights</h1>
        <p className="watchyourwatts-subtitle private-subtitle">smart recommendations based on your current appliance usage.</p>

        {error && (
          <div className="myappliances-error" style={{ marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        {loading ? (
          <p>Loading insights...</p>
        ) : (
          <SmartInsights appliances={appliancesWithCost} pricePerKwh={pricePerKwh} />
        )}
      </div>
    </div>
  );
}
