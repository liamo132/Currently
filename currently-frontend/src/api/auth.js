/*
 * File: auth.js
 * Description: Simple auth API calls to Spring Boot backend.
 * Author: Liam Connell
 * Date: 2025-11-11
 */

const BASE_URL = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export async function register({ username, email, password }) {
  const res = await fetch(`${BASE_URL}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, email, password }),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(text || "Registration failed");
  // text is like: "Registration successful. Token: <JWT>"
  const token = text.split("Token:")[1]?.trim();
  return { token, raw: text };
}

export async function login({ email, password }) {
  const res = await fetch(
    `${BASE_URL}/api/auth/login?email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`,
    { method: "POST" }
  );
  const text = await res.text();
  if (!res.ok) throw new Error(text || "Login failed");
  const token = text.split("Token:")[1]?.trim();
  return { token, raw: text };
}
