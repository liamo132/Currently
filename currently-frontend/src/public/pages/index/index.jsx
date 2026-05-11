/*
 * File: index.jsx
 * Description: Main landing page composition for the public section.
 * Project: Currently
 * Author: Liam Connell
 * Date: 2025-11-11
 *
 * Notes:
 * - Combines the Hero and Features components to form the home page.
 * - The Header is not included here because it is already rendered globally
 *   in App.jsx for all public routes.
 * - This page serves as the first impression of the Currently application.
 */

import React from "react";
import Hero from "./hero.jsx";
import Features from "./features.jsx";

// Component: renders the public landing page layout.
export default function IndexPage() {
  return (
    <>
      <Hero />
      <Features />
    </>
  );
}
