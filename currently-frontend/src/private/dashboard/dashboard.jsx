// dashboard/dashboard.jsx
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import HeaderUser from '../../public/components/header-user';

import EnergyCostSettings from './energycostsettings';
import HomeOverview from './homeoverview';
import QuickActions from './quickactions';
import SystemStatus from './systemstatus';
import WeeklyCostHero from './weeklycosthero';
import { getEnergySettings, saveEnergySettings } from '../../api/energy';

import './css/dashboard.css';

// student note: keeping these helper functions here stops us repeating maths in 4 places
const sumDailyKwh = (appliances = []) =>
  (appliances || []).reduce((sum, a) => sum + Number(a.dailyKWh || 0), 0);

const calcCosts = (dailyKwh, pricePerKwh) => {
  const price = Number(pricePerKwh) || 0;
  const daily = dailyKwh * price;
  return { daily, weekly: daily * 7, monthly: daily * 30 };
};

export default function Dashboard() {
  const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [appliances, setAppliances] = useState([]);
  const [vaultActive, setVaultActive] = useState(false);

  // Stored locally for now (DB later). Watch Your Watts reads this automatically.
  const [pricePerKwh, setPricePerKwh] = useState(() => {
    const saved = localStorage.getItem('pricePerKwh');
    return saved ? Number(saved) : 0.30;
  });

  const [providerName, setProviderName] = useState(() => localStorage.getItem('providerName') || '');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastSync, setLastSync] = useState(null);
  const [savingSettings, setSavingSettings] = useState(false);

  const energyRef = useRef(null);
  const overviewRef = useRef(null);
  const statusRef = useRef(null);

  const fetchWithAuth = useCallback(async (url, options = {}) => {
    const token = localStorage.getItem('token');
    if (!token) {
      const err = new Error('No authentication token found. Please log in again.');
      err.status = 401;
      throw err;
    }

    const headers = {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    };

    const res = await fetch(url, { ...options, headers });
    if (!res.ok) {
      const text = await res.text();
      const err = new Error(text || `Request failed with status ${res.status}`);
      err.status = res.status;
      throw err;
    }
    return res;
  }, []);

  const handleAuthError = (err) => {
    if (err?.status === 401 || err?.status === 403) {
      localStorage.removeItem('token');
      setError('Session expired. Please log in again.');
      setLoading(false);
      navigate('/login');
      return true;
    }
    return false;
  };

  const normalizeVaultActive = (payload, fallback) => {
    if (!payload || typeof payload !== 'object') return fallback;
    if (payload.active !== undefined) return Boolean(payload.active);
    if (payload.status) return String(payload.status).toLowerCase() === 'active';
    if (payload.enabled !== undefined) return Boolean(payload.enabled);
    return fallback;
  };

  // Persist settings locally (migration later -> store in DB per user)
  useEffect(() => {
    localStorage.setItem('pricePerKwh', String(pricePerKwh));
  }, [pricePerKwh]);

  useEffect(() => {
    localStorage.setItem('providerName', providerName);
  }, [providerName]);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError('');

        // Energy settings (DB-backed if available)
        try {
          const settings = await getEnergySettings();
          if (settings) {
            if (settings.pricePerKwh !== undefined) setPricePerKwh(Number(settings.pricePerKwh));
            if (settings.providerName !== undefined) setProviderName(settings.providerName || '');
          }
        } catch (err) {
          console.warn('Energy settings fetch failed; using local storage value', err);
        }

        let roomsData = [];
        try {
          const roomsRes = await fetchWithAuth(`${API_BASE}/api/users/me/rooms`);
          roomsData = await roomsRes.json();
        } catch (err) {
          console.warn('Rooms fetch failed; continuing', err);
        }

        let appsData = [];
        try {
          const appsRes = await fetchWithAuth(`${API_BASE}/api/users/me/appliances`);
          appsData = await appsRes.json();
        } catch (err) {
          console.warn('Appliances fetch failed; continuing', err);
        }

        // Bills Vault status: align with /api/vault/status (used on BillsVault page)
        let vault = false;
        try {
          const vaultRes = await fetchWithAuth(`${API_BASE}/api/vault/status`);
          const vaultJson = await vaultRes.json(); // expect { pinSet: boolean }
          vault = vaultJson?.pinSet ? true : normalizeVaultActive(vaultJson, false);
        } catch (err) {
          console.warn('Vault status fetch failed; leaving default false', err);
        }

        setRooms(roomsData);
        setAppliances(appsData);
        setVaultActive(vault);
        setLastSync(new Date());
      } catch (e) {
        console.error(e);
        const handled = handleAuthError(e);
        if (!handled) setError(e.message || 'Failed to load dashboard.');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [API_BASE, fetchWithAuth, vaultActive]);

  const persistEnergySettings = useCallback(
    async (nextPrice, nextProvider) => {
      setSavingSettings(true);
      try {
        await saveEnergySettings({ pricePerKwh: nextPrice, providerName: nextProvider });
      } catch (err) {
        console.warn('Persisting energy settings failed; kept locally', err);
        setError((prev) => prev || 'Saved locally, but failed to save energy cost to server.');
      } finally {
        setSavingSettings(false);
      }
    },
    [setError]
  );

  const roomsCount = rooms.length;
  const appliancesCount = appliances.length;
  const floorsCount = useMemo(() => {
    // Map My House derives floors from room.floorLabel; fall back to other fields if ever present.
    const unique = new Set(
      (rooms || [])
        .map((r) => r?.floorLabel || r?.floor || r?.level || r?.storey || null)
        .filter((v) => v !== null && v !== undefined && v !== '')
    );
    return unique.size;
  }, [rooms]);

  const costs = useMemo(() => {
    const dailyKwh = sumDailyKwh(appliances);
    return calcCosts(dailyKwh, pricePerKwh);
  }, [appliances, pricePerKwh]);

  const warnings = useMemo(() => {
    const w = [];
    if (!pricePerKwh || Number(pricePerKwh) <= 0) w.push('Energy cost per kWh not set.');
    if (roomsCount === 0) w.push('No rooms added yet.');
    if (appliancesCount === 0) w.push('No appliances added yet.');
    return w;
  }, [pricePerKwh, roomsCount, appliancesCount]);

  const scrollTo = (ref) => {
    if (!ref?.current) return;
    ref.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  if (loading) {
    return (
      <div className="dashboard-container">
        <HeaderUser activePage="dashboard" />
        <div className="dashboard-content">
          <h1 className="dashboard-title">Dashboard</h1>
          <p className="dashboard-subtitle">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <HeaderUser activePage="dashboard" />

      <div className="dashboard-content">
        <h1 className="dashboard-title">Dashboard</h1>
        <p className="dashboard-subtitle">Your account, energy settings, and home overview in one place.</p>

        {error && <div className="myappliances-error">{error}</div>}

        <div className="dashboard-grid">
          {/* LEFT COLUMN */}
          <div className="dashboard-left">
            <div ref={energyRef}>
              <EnergyCostSettings
                pricePerKwh={pricePerKwh}
                setPricePerKwh={setPricePerKwh}
                providerName={providerName}
                setProviderName={setProviderName}
                onPersist={persistEnergySettings}
                saving={savingSettings}
              />
            </div>

            <div ref={overviewRef}>
              <HomeOverview
                roomsCount={roomsCount}
                appliancesCount={appliancesCount}
                floorsCount={floorsCount}
              />
            </div>
          </div>

          {/* RIGHT COLUMN */}
          <div className="dashboard-right">
            <WeeklyCostHero weeklyCost={costs.weekly} />

            <QuickActions
              onVault={() => navigate('/watchyourwatts')}
              onMap={() => navigate('/mapmyhouse')}
              onAppliances={() => navigate('/my-appliances')}
              onInsights={() => navigate('/smartinsights')}
            />

            <div ref={statusRef}>
              <SystemStatus
                lastSync={lastSync}
                warnings={warnings}
                vaultActive={vaultActive}
                onVault={() => navigate('/watchyourwatts')}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
