import React from "react";
import "../index/css/features.css";
import trackenergyimg from "../../assets/img/trackenergyimg.png";
import mapurhouseimg from "../../assets/img/mapurhouseimg.png";
import trackbillsimg from "../../assets/img/trackbillsimg.png";
import uifriendlyimg from "../../assets/img/uifriendlyimg.png";
import securedataimg from "../../assets/img/securedataimg.png";

const featureCards = [
  {
    title: "Structured Insights Engine",
    text: "Get ranked recommendations by savings impact, effort, and expected monthly cost reduction.",
    img: uifriendlyimg,
  },
  {
    title: "Appliance Cost Breakdown",
    text: "Track which devices are driving your bill with room-level and appliance-level estimates.",
    img: trackenergyimg,
  },
  {
    title: "Secure Bills Vault",
    text: "Store and review your bill history safely, then compare spend trends over time.",
    img: securedataimg,
  },
  {
    title: "Map My House",
    text: "Build your home layout digitally to improve attribution accuracy and spot hidden waste zones.",
    img: mapurhouseimg,
  },
  {
    title: "WatchYourWatts",
    text: "Monitor consumption patterns and detect unusual spikes before they become expensive.",
    img: trackbillsimg,
  },
];

export default function Features() {
  return (
    <>
      <section className="landing-features" id="features">
        <div className="landing-container">
          <div className="section-head">
            <p className="section-kicker">Product Capabilities</p>
            <h2>Everything you need to understand and reduce household energy spend.</h2>
          </div>

          <div className="features-grid">
            {featureCards.map((item) => (
              <article key={item.title} className="feature-card">
                <div className="feature-image-wrap">
                  <img src={item.img} alt={item.title} />
                </div>
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
            <h2>A practical 3-step workflow focused on measurable savings.</h2>
          </div>

          <div className="steps-grid">
            <article className="step-card">
              <span className="step-index">01</span>
              <h3>Set up your household baseline</h3>
              <p>Add your account, recent bill context, and your pricing assumptions.</p>
            </article>

            <article className="step-card">
              <span className="step-index">02</span>
              <h3>Map appliances and usage</h3>
              <p>Assign key appliances by room and usage profile to reveal cost drivers.</p>
            </article>

            <article className="step-card">
              <span className="step-index">03</span>
              <h3>Execute prioritized actions</h3>
              <p>Follow structured insights with expected savings and track week-over-week improvement.</p>
            </article>
          </div>
        </div>
      </section>

      <section className="final-cta">
        <div className="landing-container final-cta-inner">
          <h2>Start cutting avoidable energy spend this week.</h2>
          <p>Create your account and turn your next bill into a clear action plan.</p>
          <div className="hero-ctas">
            <a className="hero-btn hero-btn-primary" href="/signup">Create free account</a>
            <a className="hero-btn hero-btn-secondary" href="/login">I already have an account</a>
          </div>
        </div>
      </section>
    </>
  );
}
