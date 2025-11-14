/*
 * File: hero.jsx
 * Description: Hero section for the public landing page.
 * Author: Liam Connell
 * Date: 2025-11-11
 *
 * Notes:
 * - Displays the main banner image and introductory text for the Currently platform.
 * - This is the first section users see when visiting the site.
 * - Includes a "Get Started" button which can later be linked to the signup route.
 */

import React from "react";
import "../index/css/hero.css"; // adjusted import path for new structure
import HeroImg from "../../assets/img/Hero.png"; // updated relative path

// Functional component for the landing page hero section
export default function Hero() {
  return (
    <main>
      <div className="container">
        {/* main illustrative image for the landing section */}
        <img src={HeroImg} alt="Illustration showing energy monitoring" />

        {/* text content alongside the hero image */}
        <div className="hero-text">
          <h1>See your power, help shape your savings.</h1>
          <p>
            Currently helps you monitor energy usage, map your home appliances,
            and save money while keeping your data secure.
          </p>

          {/* button leading to signup (to be wired to navigation later) */}
          <button
            onClick={() => (window.location.href = "/signup")}
            aria-label="Get started by creating an account"
          >
            Get Started
          </button>
        </div>
      </div>
    </main>
  );
}
