/*
 * File: index.jsx
 * Description: Main landing page composition for the public section.
 * Author: Liam Connell
 * Date: 2025-11-11
 *
 * Notes:
 * - Combines the Hero, Features, and Footer components to form the home page.
 * - The Header is not included here because it’s already rendered globally
 *   in App.jsx for all public routes.
 * - This page serves as the first impression of the Currently application.
 */

import React from "react";
import Hero from "./hero.jsx"; // hero section (main banner)
import Features from "./features.jsx"; // feature carousel
import Footer from "../../components/footer.jsx"; 

// Functional component that renders the landing page layout
export default function IndexPage() {
  return (
    <>
      <Hero />
      <Features />
      <Footer />
    </>
  );
}
