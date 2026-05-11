import React from 'react';

/**
 * Component: HomeOverview
 * Purpose: Summarizes home setup progress, Room/Appliance counts, and the highest weekly Cost driver.
 */
export default function HomeOverview({
  roomsCount,
  appliancesCount,
  floorsCount,
  completenessPct = 0,
  missingItems = [],
  highestCostHint = null,
}) {
  return (
    <section className="card home-overview">
      <div className="card-header">
        <h2 className="card-title">Home Overview</h2>
      </div>

      <div className="overview-progress">
        <div className="overview-progress__row">
          <span>Setup completeness</span>
          <strong>{completenessPct}%</strong>
        </div>
        <div className="overview-progress__bar" role="img" aria-label={`Setup ${completenessPct}% complete`}>
          <span style={{ width: `${Math.max(0, Math.min(100, completenessPct))}%` }} />
        </div>
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

      <div className="home-overview__hint">
        <div className="home-overview__hint-title">Highest cost hint</div>
        {highestCostHint ? (
          <div className="home-overview__hint-body">
            {highestCostHint.name} is currently your top weekly cost driver at about EUR{' '}
            {highestCostHint.weeklyCost.toFixed(2)}/week.
          </div>
        ) : (
          <div className="home-overview__hint-body">
            Add appliances and set kWh cost to surface your top weekly cost driver.
          </div>
        )}
      </div>

      {missingItems.length > 0 && (
        <div className="home-overview__gaps">
          <div className="home-overview__gaps-title">Next setup gap</div>
          {missingItems.map((item) => (
            <div className="home-overview__gap-item" key={item.id}>
              <span>{item.label}</span>
              <button type="button" className="btn btn--tertiary" onClick={item.onClick}>
                {item.actionLabel}
              </button>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
