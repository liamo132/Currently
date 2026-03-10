import React from "react";
import "../index/css/hero.css";
import HeroImg from "../../assets/img/Hero.png";

export default function Hero() {
  return (
    <main className="landing-main">
      <section className="hero-section" id="top">
        <div className="landing-container hero-grid">
          <div className="hero-copy">
            <p className="hero-kicker">Household Energy Intelligence</p>
            <h1>Know what is driving your bill, then cut waste with clear weekly actions.</h1>
            <p className="hero-subtext">
              Currently translates your household energy data into appliance-level cost visibility and prioritized
              next steps, so every week you know what to change and what savings to expect.
            </p>

            <div className="hero-ctas">
              <a className="hero-btn hero-btn-primary" href="/signup">Start free</a>
              <a className="hero-btn hero-btn-secondary" href="/#how-it-works">See how it works</a>
            </div>

            <div className="trust-strip" aria-label="Trust indicators">
              <span>Private by default</span>
              <span>Secure bill history</span>
              <span>No hardware required</span>
            </div>
          </div>

          <div className="hero-visual" aria-hidden="true">
            <img src={HeroImg} alt="Currently product dashboard preview" />
          </div>
        </div>
      </section>
    </main>
  );
}
