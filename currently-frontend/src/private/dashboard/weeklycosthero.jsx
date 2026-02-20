/*
 * File: weeklycosthero.jsx
 * Description: Hero metric that highlights the estimated weekly cost with a one-time count-up animation.
 * Author: Codex (GPT-5)
 * Date: 2026-02-10
 */

import React, { useEffect, useRef, useState } from 'react';

/**
 * Component: WeeklyCostHero
 * Purpose: Show the weekly cost prominently and animate it once on first load.
 */
export default function WeeklyCostHero({ weeklyCost = 0 }) {
  const [displayValue, setDisplayValue] = useState(0);
  const hasAnimatedRef = useRef(false);
  const rafRef = useRef(null);

  useEffect(() => {
    if (weeklyCost == null) return;

    const target = Number(weeklyCost) || 0;

    // Only animate the first time we receive a positive value.
    if (!hasAnimatedRef.current && target > 0) {
      hasAnimatedRef.current = true;
      const duration = 820; // ms
      const start = performance.now();
      const from = 0;
      const to = target;

      const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3);

      const step = (ts) => {
        const elapsed = ts - start;
        const progress = Math.min(elapsed / duration, 1);
        const eased = easeOutCubic(progress);
        const value = from + (to - from) * eased;
        setDisplayValue(value);
        if (progress < 1) {
          rafRef.current = requestAnimationFrame(step);
        }
      };

      rafRef.current = requestAnimationFrame(step);
      return () => {
        if (rafRef.current) cancelAnimationFrame(rafRef.current);
      };
    }

    // If the value changes later (or is zero initially), snap to it without re-animating.
    setDisplayValue(target);
  }, [weeklyCost]);

  const formatted = `€${(Number(displayValue) || 0).toFixed(2)}`;

  return (
    <section className="card weekly-hero">
      <div className="weekly-hero__title">Estimated Weekly Cost</div>
      <div className="weekly-hero__value">{formatted}</div>
      <div className="weekly-hero__subtitle">Based on current appliances & usage</div>
    </section>
  );
}
