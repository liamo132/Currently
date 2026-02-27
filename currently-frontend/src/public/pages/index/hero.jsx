import React from "react";
import "../index/css/hero.css";
import HeroImg from "../../assets/img/Hero.png";

export default function Hero() {
  return (
    <main className="landing-main">
      <section className="hero-section">
        <div className="landing-container hero-grid">
          <div className="hero-copy">
            <p className="hero-kicker">Household Energy Intelligence</p>
            <h1>Cut household energy spend with appliance-level clarity.</h1>
            <p className="hero-subtext">
              Currently turns your usage and bill history into structured weekly actions,
              so you can reduce waste, lower cost, and track real progress.
            </p>

            <div className="hero-ctas">
              <a className="hero-btn hero-btn-primary" href="/signup">Start free</a>
              <a className="hero-btn hero-btn-secondary" href="/#how-it-works">See how it works</a>
            </div>

            <div className="trust-strip" aria-label="Trust indicators">
              <span>Private by default</span>
              <span>Secure bill history</span>
              <span>Built for real household decisions</span>
            </div>
          </div>

          <div className="hero-visual" aria-hidden="true">
            <img src={HeroImg} alt="Currently product dashboard preview" />

          </div>
        </div>
      </section>

      <section className="problem-solution-section">
        <div className="landing-container">
          <div className="problem-row">
            <article>
              <h2>Bills feel unpredictable</h2>
              <p>Monthly totals change, but the reasons stay unclear.</p>
            </article>
            <article>
              <h2>No appliance-level visibility</h2>
              <p>It is hard to identify where energy spend is actually coming from.</p>
            </article>
            <article>
              <h2>Advice is too generic</h2>
              <p>Most tips are not tied to your home, layout, or usage habits.</p>
            </article>
          </div>

          <div className="solution-panel">
            <p>
              <strong>Currently solves this with a structured pipeline:</strong> map your home,
              break down appliance-level costs, and get prioritized savings actions with expected impact.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}
