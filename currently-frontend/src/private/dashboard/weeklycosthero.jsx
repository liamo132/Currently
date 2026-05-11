/*
 * File: weeklycosthero.jsx
 * Description: Hero metric that highlights the estimated weekly cost with a one-time count-up animation.
 */

import React, { useEffect, useRef, useState } from 'react';

/*
 * Component: WeeklyCostHero
 * Purpose: Highlights estimated weekly electricity Cost and animates the first non-zero value.
 */
export default function WeeklyCostHero({ weeklyCost = 0 }) {
  const [displayValue, setDisplayValue] = useState(0);
  const hasAnimatedRef = useRef(false);
  const rafRef = useRef(null);

  // Hook: animates the displayed weekly Cost once, then directly updates for later tariff/appliance changes.
  useEffect(() => {
    if (weeklyCost == null) return;

    const target = Number(weeklyCost) || 0;

    if (!hasAnimatedRef.current && target > 0) {
      hasAnimatedRef.current = true;
      const duration = 820;
      const start = performance.now();
      const from = 0;
      const to = target;
      // Animation helper: easeOutCubic makes the Cost number settle smoothly instead of moving linearly.
      const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3);

      const step = (ts) => {
        const elapsed = ts - start;
        const progress = Math.min(elapsed / duration, 1);
        const eased = easeOutCubic(progress);
        const value = from + (to - from) * eased;
        setDisplayValue(value);
        if (progress < 1) rafRef.current = requestAnimationFrame(step);
      };

      rafRef.current = requestAnimationFrame(step);
      return () => {
        if (rafRef.current) cancelAnimationFrame(rafRef.current);
      };
    }

    setDisplayValue(target);
    return undefined;
  }, [weeklyCost]);

  const fmt = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' });
  const formatted = fmt.format(Number(displayValue) || 0);

  return (
    <section className="card weekly-hero">
      <div className="weekly-hero__title">Estimated Weekly Cost</div>
      <div className="weekly-hero__value">{formatted}</div>
      <div className="weekly-hero__subtitle">Based on current appliances and saved energy settings.</div>
    </section>
  );
}
