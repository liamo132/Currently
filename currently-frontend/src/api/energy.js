// src/api/energy.js
// Energy cost settings API helpers.
const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

const authHeaders = () => {
  const token = localStorage.getItem('token');
  if (!token) throw new Error('No authentication token found. Please log in again.');
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
};

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

export async function getEnergySettings() {
  // GET /api/users/me/energy-settings -> { pricePerKwh, providerName }
  const res = await fetch(`${API_BASE}/api/users/me/energy-settings`, {
    method: 'GET',
    headers: authHeaders(),
  });

  // If endpoint not created yet, treat as "no server value" and fall back to local storage.
  if (res.status === 404) return null;

  return parseJsonOrThrow(res);
}

export async function saveEnergySettings(payload) {
  // PUT /api/users/me/energy-settings with { pricePerKwh, providerName }
  const res = await fetch(`${API_BASE}/api/users/me/energy-settings`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(payload),
  });

  return parseJsonOrThrow(res);
}
