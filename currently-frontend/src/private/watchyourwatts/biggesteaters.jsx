// watchyourwatts/biggesteaters.jsx
import React, { useMemo } from 'react';
import { TrendingDown } from 'lucide-react';
import './css/biggesteaters.css';

/*
 * Component: BiggestEaters
 * Purpose: Shows the top five Appliances by daily Cost so users can focus on the highest-impact devices.
 */
const BiggestEaters = ({ appliances = [] }) => {
  // Cost ranking: sort by daily Cost and take the top five so the card stays focused.
  const top5 = useMemo(() => {
    return [...(appliances || [])]
      .sort(
        (a, b) =>
          Number(b.computedDailyCost ?? b.estimatedDailyCost ?? 0) -
          Number(a.computedDailyCost ?? a.estimatedDailyCost ?? 0)
      )
      .slice(0, 5);
  }, [appliances]);

  // Empty state: explains why the Watch Your Watts ranking is unavailable before Appliances are added.
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
          // Display helper: custom name takes priority, then catalogue Appliance name.
          const name = a.customName || a.applianceName || 'Unknown Appliance';

          // Cost display: show kWh when present, but always show daily Cost for comparison.
          const dailyKwh = Number(a.dailyKWh || 0);
          const dailyCost = Number(a.computedDailyCost ?? a.estimatedDailyCost ?? 0);

          return (
            <div key={a.id ?? `${name}-${index}`} className="appliance-row">
              <div className="appliance-rank">
                <span className="rank-number">{index + 1}</span>
              </div>

              <div className="appliance-info">
                <div className="appliance-name">{name}</div>

                <div className="appliance-stats">
                  EUR {dailyCost.toFixed(2)}/day
                  {dailyKwh > 0 ? ` - ${dailyKwh.toFixed(2)} kWh/day` : ''}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default BiggestEaters;
