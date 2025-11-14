/*
 * File: features.jsx
 * Description: Rotating feature carousel for the public landing page.
 * Author: Liam Connell
 * Date: 2025-11-11
 */

import React, { useState, useEffect, useRef } from "react";
import "../index/css/features.css";
import trackenergyimg from "../../assets/img/trackenergyimg.png";
import mapurhouseimg from "../../assets/img/mapurhouseimg.png";
import trackbillsimg from "../../assets/img/trackbillsimg.png";
import uifriendlyimg from "../../assets/img/uifriendlyimg.png";
import securedataimg from "../../assets/img/securedataimg.png";

const images = [
  trackenergyimg,
  mapurhouseimg,
  trackbillsimg,
  uifriendlyimg,
  securedataimg,
];

const captions = [
  "Track your home's energy usage in real-time.",
  "Map your house for smarter room-level insights.",
  "Never miss a bill — manage payments and reminders.",
  "A clean, intuitive interface designed for speed.",
  "Bank-grade security to keep your data safe.",
];

export default function Features() {
  const [index, setIndex] = useState(0);
  const autoplayRef = useRef(null);

  useEffect(() => {
    autoplayRef.current = () => setIndex((i) => (i + 1) % images.length);
  });

  useEffect(() => {
    const play = () => autoplayRef.current();
    const id = setInterval(play, 6000);
    return () => clearInterval(id);
  }, []);

  const goTo = (i) => setIndex(i);

  return (
    <section className="features">
      <div className="features-inner container">
        <div className="what-to-expect">
          <h2>What to expect here at Currently.</h2>
          <p className="subtitle">
            Take a scroll through and find what we have to offer.
          </p>

          <div className="carousel">
            <div className="carousel-track">
              {images.map((src, i) => (
                <div
                  key={src}
                  className={`carousel-slide ${i === index ? "active" : ""}`}
                  aria-hidden={i !== index}
                >
                  <img src={src} alt={`Feature ${i + 1}`} />
                </div>
              ))}
            </div>

            <div className="carousel-caption" aria-live="polite">
              {captions[index]}
            </div>

            <div className="carousel-dots">
              {images.map((_, i) => (
                <button
                  key={i}
                  className={`dot ${i === index ? "active" : ""}`}
                  onClick={() => goTo(i)}
                  aria-label={`Go to slide ${i + 1}`}
                />
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
