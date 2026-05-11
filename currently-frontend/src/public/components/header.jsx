import React from "react";
import { Link } from "react-router-dom";

/*
 * Component: Header
 * Purpose: Public navigation for landing, Login, and Signup routes.
 */
const Header = () => {
  return (
    <header className="site-header">
      <nav className="site-nav" aria-label="Primary">
        <Link to="/" className="logo" aria-label="Currently home">
          Currently.
        </Link>

        <ul className="site-nav-links">
          <li><Link to="/#how-it-works">How it works</Link></li>
          <li><Link to="/#features">Features</Link></li>
        </ul>

        <div className="site-nav-actions">
          <Link to="/login" className="btn-nav btn-nav-secondary">Log in</Link>
          <Link to="/signup" className="btn-nav btn-nav-primary">Start free</Link>
        </div>
      </nav>
    </header>
  );
};

export default Header;
