// src/api/energy.js
// Energy cost settings API helpers.
const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

// API helper: adds the JWT Authorization header required by protected Dashboard energy settings endpoints.
const authHeaders = () => {
  const token = localStorage.getItem('token');
  if (!token) throw new Error('No authentication token found. Please log in again.');
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
};

// API helper: parses JSON success/error responses from the Spring Boot backend.
const parseJsonOrThrow = async (res) => {
  const text = await res.text();
  let data;
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = { error: text };
  }

  if (!res.ok) {
    const msg = data?.error || `Request failed with status ${res.status}`;
    const err = new Error(msg);
    err.status = res.status;
    throw err;
  }

  return data;
};

/*
 * Function: getEnergySettings
 * Purpose: Loads pricePerKwh and providerName for Dashboard, Forecast, and Savings calculations.
 */
export async function getEnergySettings() {
  // API call: GET /api/users/me/energy-settings -> { pricePerKwh, providerName }
  const res = await fetch(`${API_BASE}/api/users/me/energy-settings`, {
    method: 'GET',
    headers: authHeaders(),
  });

  // If the request fails, let the caller fall back to its cached client value.
  if (res.status === 404) return null;

  return parseJsonOrThrow(res);
}

/*
 * Function: saveEnergySettings
 * Purpose: Persists the user's electricity tariff settings to the backend database.
 */
export async function saveEnergySettings(payload) {
  // API call: PUT /api/users/me/energy-settings with { pricePerKwh, providerName }
  const res = await fetch(`${API_BASE}/api/users/me/energy-settings`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(payload),
  });

  return parseJsonOrThrow(res);
}
