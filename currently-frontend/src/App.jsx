/*
 * File: App.jsx
 * Description: Main application router. Defines public routes (Landing, Login, Signup)
 *              and private routes (Dashboard, My Appliances) protected by JWT.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Header from "./public/components/header";
import Footer from "./public/components/footer";
import IndexPage from "./public/pages/index/index.jsx";
import Login from "./public/pages/auth/Login.jsx";
import Signup from "./public/pages/auth/Signup.jsx";
import Dashboard from "./private/Dashboard.jsx";
import MyAppliances from "./private/myappliances/myappliances.jsx";

/**
 * Component: PublicLayout
 * Purpose:
 *   Wrap all public-facing pages with the public header.
 *   These routes do NOT require authentication.
 */
function PublicLayout() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<IndexPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
      </Routes>
      {/* <Footer /> — optional */}
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

        {/* Fallback for unknown paths */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
