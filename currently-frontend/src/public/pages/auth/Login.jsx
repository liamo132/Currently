/*
 * File: Login.jsx
 * Description: Login page for authenticating existing users.
 * Author: Liam Connell
 * Date: 2025-11-11
 *
 * Notes:
 * - Integrates directly with the backend /api/auth/login endpoint.
 * - Uses the shared API wrapper in src/api/auth.js for HTTP requests.
 * - Stores JWT token in localStorage on successful login and redirects to /dashboard.
 */

import React, { useState } from "react";
import "../index/css/features.css";
import "./auth.css";   // keep this; it’s in the same folder
import { login } from "../../../api/auth"; // API call handler

export default function Login() {
  const [formData, setFormData] = useState({ email: "", password: "" });
  const [errors, setErrors] = useState({});
  const [serverMsg, setServerMsg] = useState("");

  // ----- Handle input change -----
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: "" }));
  };

  // ----- Simple client-side validation -----
  const validateForm = () => {
    const newErrors = {};
    if (!formData.email) {
      newErrors.email = "Email is required";
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = "Email is invalid";
    }
    if (!formData.password) {
      newErrors.password = "Password is required";
    }
    return newErrors;
  };

  // ----- Submit form -----
  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerMsg("");
    const newErrors = validateForm();

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    try {
      // Call backend login endpoint
      const { token } = await login({
        email: formData.email,
        password: formData.password,
      });

      // Save JWT in local storage for session persistence
      localStorage.setItem("token", token);

      // Redirect to private dashboard
      window.location.href = "/dashboard";
    } catch (err) {
      setServerMsg(err.message || "Login failed");
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h1 className="auth-title">Welcome back</h1>
        <p className="auth-subtitle">
          Login to access your Currently dashboard
        </p>

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className={errors.email ? "error" : ""}
              placeholder="you@example.com"
            />
            {errors.email && (
              <span className="error-message">{errors.email}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              className={errors.password ? "error" : ""}
              placeholder="Enter your password"
            />
            {errors.password && (
              <span className="error-message">{errors.password}</span>
            )}
          </div>

          {/* Server feedback */}
          {serverMsg && <p className="server-message">{serverMsg}</p>}

          <button type="submit" className="auth-button">
            Login
          </button>
        </form>

        <p className="auth-footer">
          Don’t have an account? <a href="/signup">Sign up</a>
        </p>
      </div>
    </div>
  );
}
