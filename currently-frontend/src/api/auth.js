// src/api/auth.js
const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

/*
 * Function: parseJsonOrThrow
 * Purpose: Reads backend API responses and turns error payloads into JavaScript errors for Login/Register screens.
 */
async function parseJsonOrThrow(res) {
  // If backend ever returns non-JSON errors, this keeps the error readable.
  const text = await res.text();
  let data;
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = { error: text };
  }

  if (!res.ok) {
    throw new Error(data.error || "Request failed");
  }

  return data;
}

/*
 * Function: register
 * Purpose: Calls POST /api/auth/register with new account details and expects { token } from the Spring Boot backend.
 */
export async function register(payload) {
  const res = await fetch(`${API_BASE}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  // API response: backend returns { token } so React can store the JWT.
  return await parseJsonOrThrow(res);
}

/*
 * Function: login
 * Purpose: Calls POST /api/auth/login with email/password credentials and expects { token } from Spring Security.
 */
export async function login(payload) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  // API response: backend returns { token } so private routes can call protected APIs.
  return await parseJsonOrThrow(res);
}
