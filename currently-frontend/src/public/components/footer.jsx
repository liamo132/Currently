import React from "react";
import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="site-footer" id="faq">
      <div className="site-footer-inner">
        <div className="footer-brand">
          <h2 className="footer-title">Currently.</h2>
          <p>Energy-spend intelligence for households that want better decisions, not guesswork.</p>
        </div>

        <div className="footer-col">
          <h3>Product</h3>
          <Link to="/#features">Features</Link>
          <Link to="/#how-it-works">How it works</Link>
          <Link to="/signup">Start free</Link>
        </div>

        <div className="footer-col" id="security">
          <h3>Security</h3>
          <p>Private by default</p>
          <p>Encrypted storage</p>
          <p>Protected bill history</p>
        </div>

        <div className="footer-col">
          <h3>Account</h3>
          <Link to="/login">Log in</Link>
          <Link to="/signup">Create account</Link>
        </div>
      </div>

      <div className="site-footer-bottom">
        <small>2026 Currently. Built for smarter household energy decisions.</small>
      </div>
    </footer>
  );
}
