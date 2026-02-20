// src/api/auth.js
const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

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

export async function register(payload) {
  const res = await fetch(`${API_BASE}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  // Expect { token }
  return await parseJsonOrThrow(res);
}

export async function login(payload) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  // Expect { token }
  return await parseJsonOrThrow(res);
}
