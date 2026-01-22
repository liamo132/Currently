// watchyourwatts/watchyourwatts.jsx
import React, { useEffect, useState, useCallback } from 'react';
import HeaderUser from '../../public/components/header-user';
import RoomConsumption from './roomconsumption';
import BiggestEaters from './biggesteaters';
import CostForecast from './costforecast';
import './css/watchyourwatts.css';

export default function WatchYourWatts() {
  const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

  const [rooms, setRooms] = useState([]);
  const [appliances, setAppliances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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

  if (loading) {
    return (
      <div className="watchyourwatts-container">
        <HeaderUser activePage="watchyourwatts" />
        <div className="watchyourwatts-content">
          <h1 className="watchyourwatts-title">Watch Your Watts</h1>
          <p className="watchyourwatts-subtitle">Loading insights...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="watchyourwatts-container">
      <HeaderUser activePage="watchyourwatts" />

      <div className="watchyourwatts-content">
        <h1 className="watchyourwatts-title">Watch Your Watts</h1>
        <p className="watchyourwatts-subtitle">Real-time insights into your energy consumption</p>

        {error && (
          <div className="myappliances-error" style={{ marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        <div className="watchyourwatts-grid">
          <RoomConsumption rooms={rooms} appliances={appliances} />
          <BiggestEaters rooms={rooms} appliances={appliances} />
        </div>

        <CostForecast appliances={appliances} />
      </div>
    </div>
  );
}
