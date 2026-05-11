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
import '../shared/private-layout.css';

import './css/dashboard.css';

// Cost Calculation: totals daily kWh from appliance API responses for Dashboard and Forecast summaries.
const sumDailyKwh = (appliances = []) =>
  (appliances || []).reduce((sum, a) => sum + Number(a.dailyKWh || 0), 0);

// Cost Calculation: converts daily kWh and price per kWh into daily, weekly, and monthly estimated cost.
const calcCosts = (dailyKwh, pricePerKwh) => {
  const price = Number(pricePerKwh) || 0;
  const daily = dailyKwh * price;
  return { daily, weekly: daily * 7, monthly: daily * 30 };
};

/*
 * Component: Dashboard
 * Purpose: Private landing page after Login that combines energy settings, home setup progress,
 * system status, quick actions, and estimated weekly Cost.
 */
export default function Dashboard() {
  const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';
  const navigate = useNavigate();

  const [rooms, setRooms] = useState([]);
  const [appliances, setAppliances] = useState([]);
  const [vaultActive, setVaultActive] = useState(false);

  // Frontend State: cached locally so Watch Your Watts and Smart Insights can reuse the latest tariff value.
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

  // API helper: attaches the JWT token to Dashboard calls for Rooms, Appliances, and Bills Vault status.
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

  // Authentication helper: clears stale JWT state and redirects to Login when protected API calls fail.
  const handleAuthError = useCallback((err) => {
    if (err?.status === 401 || err?.status === 403) {
      localStorage.removeItem('token');
      setError('Session expired. Please log in again.');
      setLoading(false);
      navigate('/login');
      return true;
    }
    return false;
  }, [navigate]);

  // Bills Vault helper: supports possible backend status payload shapes while normalizing to a boolean.
  const normalizeVaultActive = useCallback((payload, fallback) => {
    if (!payload || typeof payload !== 'object') return fallback;
    if (payload.active !== undefined) return Boolean(payload.active);
    if (payload.status) return String(payload.status).toLowerCase() === 'active';
    if (payload.enabled !== undefined) return Boolean(payload.enabled);
    return fallback;
  }, []);

  // Hook: keep a client-side Cost cache in sync with the persisted backend value.
  useEffect(() => {
    localStorage.setItem('pricePerKwh', String(pricePerKwh));
  }, [pricePerKwh]);

  useEffect(() => {
    localStorage.setItem('providerName', providerName);
  }, [providerName]);

  /*
   * Hook: Dashboard data load
   * Purpose: Loads energy settings, Rooms, Appliances, and Bills Vault status from protected backend APIs.
   * Important API calls: /api/users/me/rooms, /api/users/me/appliances, /api/vault/status.
   */
  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError('');

        // API call: Database-backed energy settings used by Cost and Forecast calculations.
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
          // API call: Room data used for Map My House progress and Dashboard setup completeness.
          const roomsRes = await fetchWithAuth(`${API_BASE}/api/users/me/rooms`);
          roomsData = await roomsRes.json();
        } catch (err) {
          console.warn('Rooms fetch failed; continuing', err);
        }

        let appsData = [];
        try {
          // API call: Appliance data includes calculated dailyKWh used by Dashboard Cost totals.
          const appsRes = await fetchWithAuth(`${API_BASE}/api/users/me/appliances`);
          appsData = await appsRes.json();
        } catch (err) {
          console.warn('Appliances fetch failed; continuing', err);
        }

        // API call: Bills Vault status checks whether PIN setup is complete.
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
  }, [API_BASE, fetchWithAuth, handleAuthError, normalizeVaultActive]);

  // Event handler: persists edited Dashboard energy settings to the backend and keeps local UI responsive.
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
    // Map My House calculation: derives floors from room.floorLabel; fall back to other fields if ever present.
    const unique = new Set(
      (rooms || [])
        .map((r) => r?.floorLabel || r?.floor || r?.level || r?.storey || null)
        .filter((v) => v !== null && v !== undefined && v !== '')
    );
    return unique.size;
  }, [rooms]);

  // Cost Calculation: memoizes total costs so UI updates when Appliances or pricePerKwh changes.
  const costs = useMemo(() => {
    const dailyKwh = sumDailyKwh(appliances);
    return calcCosts(dailyKwh, pricePerKwh);
  }, [appliances, pricePerKwh]);

  // Validation UI: creates Dashboard setup warnings for missing tariff, Rooms, or Appliances.
  const warnings = useMemo(() => {
    const w = [];
    if (!pricePerKwh || Number(pricePerKwh) <= 0) w.push('Energy cost per kWh not set.');
    if (roomsCount === 0) w.push('No rooms added yet.');
    if (appliancesCount === 0) w.push('No appliances added yet.');
    return w;
  }, [pricePerKwh, roomsCount, appliancesCount]);

  // Dashboard progress: tracks whether the key setup areas are complete.
  const setupItems = useMemo(
    () => [
      { id: 'price', complete: Number(pricePerKwh) > 0 },
      { id: 'rooms', complete: roomsCount > 0 },
      { id: 'appliances', complete: appliancesCount > 0 },
      { id: 'vault', complete: vaultActive },
    ],
    [pricePerKwh, roomsCount, appliancesCount, vaultActive]
  );

  // Dashboard progress: converts completed setup items into a percentage for the Home Overview card.
  const completenessPct = useMemo(() => {
    const done = setupItems.filter((item) => item.complete).length;
    return Math.round((done / setupItems.length) * 100);
  }, [setupItems]);

  // Dashboard next actions: chooses the highest priority setup gaps and links to the relevant feature.
  const missingItems = useMemo(() => {
    const items = [];
    if (Number(pricePerKwh) <= 0) {
      items.push({
        id: 'price',
        label: 'Set your kWh price to unlock accurate cost tracking.',
        actionLabel: 'Set price',
        onClick: () => scrollTo(energyRef),
      });
    }
    if (roomsCount === 0) {
      items.push({
        id: 'rooms',
        label: 'Map at least one room so insights can be room-specific.',
        actionLabel: 'Map rooms',
        onClick: () => navigate('/mapmyhouse'),
      });
    }
    if (appliancesCount === 0) {
      items.push({
        id: 'appliances',
        label: 'Add appliances so weekly estimates are based on real usage.',
        actionLabel: 'Add appliances',
        onClick: () => navigate('/my-appliances'),
      });
    }
    return items.slice(0, 2);
  }, [pricePerKwh, roomsCount, appliancesCount, navigate]);

  // Cost insight: finds the top weekly Appliance cost driver shown in Home Overview.
  const highestCostHint = useMemo(() => {
    if (!Array.isArray(appliances) || appliances.length === 0 || Number(pricePerKwh) <= 0) return null;

    const withCost = appliances
      .map((a) => {
        const weeklyCost = (Number(a?.dailyKWh || 0) * Number(pricePerKwh || 0)) * 7;
        return {
          name: a?.customName || a?.applianceName || 'Appliance',
          weeklyCost,
        };
      })
      .filter((a) => a.weeklyCost > 0)
      .sort((a, b) => b.weeklyCost - a.weeklyCost);

    return withCost[0] || null;
  }, [appliances, pricePerKwh]);

  // Event handler: scrolls to a Dashboard section when a setup action targets an in-page panel.
  const scrollTo = (ref) => {
    if (!ref?.current) return;
    ref.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  if (loading) {
    return (
      <div className="dashboard-container private-page">
        <HeaderUser activePage="dashboard" />
        <div className="dashboard-content private-content">
          <h1 className="dashboard-title private-title">Dashboard</h1>
          <p className="dashboard-subtitle private-subtitle">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-container private-page">
      <HeaderUser activePage="dashboard" />

      <div className="dashboard-content private-content">
        <h1 className="dashboard-title private-title">Dashboard</h1>
        <p className="dashboard-subtitle private-subtitle">Your account, energy settings, and home overview in one place.</p>

        {error && <div className="myappliances-error">{error}</div>}

        <WeeklyCostHero weeklyCost={costs.weekly} />

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
                completenessPct={completenessPct}
                missingItems={missingItems}
                highestCostHint={highestCostHint}
              />
            </div>
          </div>

          {/* RIGHT COLUMN */}
          <div className="dashboard-right">
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
