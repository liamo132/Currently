import React from 'react';

/**
 * Component: HomeOverview
 * Purpose: Summarize key home metrics (rooms, appliances, floors).
 */
export default function HomeOverview({ roomsCount, appliancesCount, floorsCount }) {
  return (
    <section className="card home-overview">
      <div className="card-header">
        <h2 className="card-title">Home Overview</h2>
      </div>

      <div className="stats-grid">
        <div className="stat-row">
          <span className="stat-label">Rooms</span>
          <span className="stat-value">{roomsCount}</span>
        </div>
        <div className="stat-row">
          <span className="stat-label">Appliances</span>
          <span className="stat-value">{appliancesCount}</span>
        </div>
        <div className="stat-row">
          <span className="stat-label">Floors</span>
          <span className="stat-value">{floorsCount}</span>
        </div>
      </div>
    </section>
  );
}
