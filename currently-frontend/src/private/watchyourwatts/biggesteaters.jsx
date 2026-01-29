// watchyourwatts/biggesteaters.jsx
import React, { useMemo } from 'react';
import { TrendingDown } from 'lucide-react';
import './css/biggesteaters.css';

const BiggestEaters = ({ appliances = [] }) => {
  // Simple + readable: sort by € cost per day (what users actually care about)
  // Then just take the top 5 so the card stays clean and doesn’t grow forever.
  const top5 = useMemo(() => {
    return [...(appliances || [])]
      .sort((a, b) => Number(b.estimatedDailyCost || 0) - Number(a.estimatedDailyCost || 0))
      .slice(0, 5);
  }, [appliances]);

  // Tiny “empty state” so it doesn’t look broken if user has no appliances yet.
  if (!top5.length) {
    return (
      <div className="biggest-eaters-card">
        <h2 className="biggest-eaters-title">
          <TrendingDown size={24} className="title-icon" />
          Biggest Energy Eaters
        </h2>
        <p style={{ color: '#6B7280' }}>Add appliances to see your biggest energy users.</p>
      </div>
    );
  }

  return (
    <div className="biggest-eaters-card">
      <h2 className="biggest-eaters-title">
        <TrendingDown size={24} className="title-icon" />
        Biggest Energy Eaters
      </h2>

      <div className="appliances-list">
        {top5.map((a, index) => {
          // Name fallback chain (student-proof): custom name -> catalogue name -> "Unknown"
          const name = a.customName || a.applianceName || 'Unknown Appliance';

          // If dailyKWh exists, show it. If not, we still show €/day so the UI always has meaning.
          const dailyKwh = Number(a.dailyKWh || 0);
          const dailyCost = Number(a.estimatedDailyCost || 0);

          return (
            <div key={a.id ?? `${name}-${index}`} className="appliance-row">
              <div className="appliance-rank">
                <span className="rank-number">{index + 1}</span>
              </div>

              <div className="appliance-info">
                <div className="appliance-name">{name}</div>

                <div className="appliance-stats">
                  €{dailyCost.toFixed(2)}/day
                  {dailyKwh > 0 ? ` • ${dailyKwh.toFixed(2)} kWh/day` : ''}
                </div>
              </div>

              {/* Removed the Reduce button:
                  - functionality that didn’t exist yet */}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default BiggestEaters;