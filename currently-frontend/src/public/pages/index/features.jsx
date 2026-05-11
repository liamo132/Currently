import React from "react";
import "../index/css/features.css";

// Landing Page content: key Currently capabilities shown before Signup.
const pillars = [
  {
    title: "Track your highest-cost devices",
    text: "See appliance and room-level cost estimates so you can identify waste quickly.",
  },
  {
    title: "Map your home for better attribution",
    text: "Model your household layout to improve insight accuracy and reduce blind spots.",
  },
  {
    title: "Ranked savings recommendations",
    text: "Get practical actions ordered by impact, effort, and expected monthly return.",
  },
  {
    title: "Secure bill history and trends",
    text: "Keep your billing timeline in one place and compare changes week over week.",
  },
];

/*
 * Component: Features
 * Purpose: Public landing sections that explain Appliance tracking, Map My House, Smart Insights, and Bills Vault value.
 */
export default function Features() {
  return (
    <>
      <section className="landing-features" id="features">
        <div className="landing-container">
          <div className="section-head">
            <p className="section-kicker">Platform Capabilities</p>
            <h2>Everything you need to move from bill confusion to measurable energy savings.</h2>
            <p className="section-lead">
              Clear diagnostics, focused recommendations, and progress tracking designed for real household decisions.
            </p>
          </div>

          <div className="features-grid">
            {pillars.map((item) => (
              <article key={item.title} className="feature-card">
                <h3>{item.title}</h3>
                <p>{item.text}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="how-it-works" id="how-it-works">
        <div className="landing-container">
          <div className="section-head">
            <p className="section-kicker">How It Works</p>
            <h2>A focused conversion path from signup to first savings actions.</h2>
          </div>

          <div className="steps-grid">
            <article className="step-card">
              <span className="step-index">01</span>
              <h3>Create your account</h3>
              <p>Start in minutes with your basic profile and energy cost assumptions.</p>
            </article>

            <article className="step-card">
              <span className="step-index">02</span>
              <h3>Map home and appliances</h3>
              <p>Connect rooms and devices so Currently can estimate your top cost drivers.</p>
            </article>

            <article className="step-card">
              <span className="step-index">03</span>
              <h3>Execute your weekly plan</h3>
              <p>Apply high-impact recommendations and monitor measurable bill improvement.</p>
            </article>
          </div>
        </div>
      </section>

    </>
  );
}
