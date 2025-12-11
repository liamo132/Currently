/*
 * File: Dashboard.jsx
 * Description: Basic authenticated dashboard entry point. Shows a simple welcome
 *              message and navigation to the My Appliances feature.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

import React from "react";
import { Link } from "react-router-dom";
import HeaderUser from "../public/components/header-user";



/**
 * Component: Dashboard
 * Purpose:
 *   - Act as the first page a user sees after logging in.
 *   - Provide navigation to core private features such as My Appliances.
 */
export default function Dashboard() {
  return (
    <div className="dashboard-container">
      {/* Private header with navigation for logged-in users */}
      <HeaderUser activePage="dashboard" />

      <main style={{ padding: "3rem 4rem" }}>
        <h1 style={{ marginBottom: "1rem" }}>Dashboard</h1>
        <p style={{ marginBottom: "2rem", maxWidth: "600px" }}>
          Welcome to Currently. From here you will be able to see a summary of your
          electricity usage and navigate to tools like <strong>My Appliances</strong>,
          <strong> Map My House</strong>, and <strong>Watch Your Watts</strong>.
        </p>

        {/* Simple navigation into My Appliances */}
        <Link
          to="/my-appliances"
          style={{
            display: "inline-block",
            padding: "0.75rem 1.5rem",
            borderRadius: "0.5rem",
            backgroundColor: "var(--color-accent)",
            color: "var(--color-white)",
            fontWeight: 600,
            textDecoration: "none",
          }}
        >
          Go to My Appliances
        </Link>
      </main>
    </div>
  );
}
