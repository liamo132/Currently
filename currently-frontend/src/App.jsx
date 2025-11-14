import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Header from "./public/components/header";
import Footer from "./public/components/footer";
import IndexPage from "./public/pages/index/index.jsx";
import Login from "./public/pages/auth/Login.jsx";
import Signup from "./public/pages/auth/Signup.jsx";
import Dashboard from "./private/Dashboard.jsx";

function PublicLayout() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<IndexPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
      </Routes>
    </>
  );
}

function PrivateRoute({ children }) {
  const token = localStorage.getItem("token");
  return token ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/*" element={<PublicLayout />} />
        <Route
          path="/dashboard"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
