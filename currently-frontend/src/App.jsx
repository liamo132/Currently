/*
 * File: App.jsx
 * Description: Main application router. Defines public routes (Landing, Login, Signup)
 *              and private routes (Dashboard, My Appliances) protected by JWT.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import Header from "./public/components/header";
import IndexPage from "./public/pages/index/index.jsx";
import Login from "./public/pages/auth/Login.jsx";
import Signup from "./public/pages/auth/Signup.jsx";
import Dashboard from "./private/dashboard/dashboard.jsx";
import MyAppliances from "./private/myappliances/myappliances.jsx";
import MapMyHouse from "./private/mapmyhouse/mapmyhouse.jsx";
import WatchYourWatts from "./private/watchyourwatts/watchyourwatts.jsx";
import SmartInsightsPage from "./private/smartinsights/smartinsightspage.jsx";

/*
 * Component: HashScrollManager
 * Purpose: Supports landing-page hash links such as /#features while accounting for the fixed header height.
 */
function HashScrollManager() {
  const { pathname, hash } = useLocation();

  // Hook: scrolls to top or to a section whenever the public route/hash changes.
  useEffect(() => {
    if (!hash) {
      if (pathname === "/") {
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
      return;
    }

    const id = decodeURIComponent(hash.slice(1));
    const target = document.getElementById(id);
    if (!target) return;

    const headerOffset = 88;
    const top = target.getBoundingClientRect().top + window.pageYOffset - headerOffset;
    window.scrollTo({ top, behavior: "smooth" });
  }, [pathname, hash]);

  return null;
}

/**
 * Component: PublicLayout
 * Purpose:
 *   Wrap all public-facing pages with the public header.
 *   These routes do NOT require authentication.
 */
function PublicLayout() {
  return (
    <>
      <HashScrollManager />
      <Header />
      <Routes>
        <Route path="/" element={<IndexPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
      </Routes>
    </>
  );
}

/**
 * Component: PrivateRoute
 * Purpose:
 *   Guard private pages. If user has JWT token, they may proceed.
 *   Otherwise redirect to the login page.
 */
function PrivateRoute({ children }) {
  const token = localStorage.getItem("token");
  return token ? children : <Navigate to="/login" replace />;
}

/**
 * Component: App
 * Purpose:
 *   Define the complete routing structure for the application.
 *   Public routes -> inside PublicLayout
 *   Private routes -> directly inside Router + wrapped in PrivateRoute
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public pages */}
        <Route path="/*" element={<PublicLayout />} />

        {/* Private pages */}
        <Route
          path="/dashboard"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />

        <Route
          path="/my-appliances"
          element={
            <PrivateRoute>
              <MyAppliances />
            </PrivateRoute>
          }
        />

        <Route
          path="/mapmyhouse"
          element={
            <PrivateRoute>
              <MapMyHouse />
            </PrivateRoute>
          }
        />

        <Route
          path="/watchyourwatts"
          element={
            <PrivateRoute>
              <WatchYourWatts />
            </PrivateRoute>
          }
        />

        <Route
          path="/smartinsights"
          element={
            <PrivateRoute>
              <SmartInsightsPage />
            </PrivateRoute>
          }
        />

        {/* Fallback for unknown paths */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
