// watchyourwatts/costforecast.jsx
import React, { useMemo } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { TrendingDown } from 'lucide-react';
import './css/costforecast.css';

const clamp = (v, min, max) => Math.min(max, Math.max(min, v));

const CostForecast = ({
  appliances = [],
  // total % reduction by end of month (1–10)
  reductionPercent = 10,
  pricePerKwh = 0.3,
}) => {
  const { forecastData, totals, label } = useMemo(() => {
    const pct = clamp(Number(reductionPercent) || 1, 1, 10);
    const targetFraction = pct / 100;

    // 1. Baseline WEEKLY cost (recompute with latest price if dailyKWh is present)
    const weeklyCost =
      appliances.reduce((sum, a) => {
        const dailyKwh = Number(a.dailyKWh || 0);
        const computed = Number(a.computedDailyCost ?? dailyKwh * pricePerKwh);
        const fallback = Number(a.estimatedDailyCost || 0);
        const dailyCost = Number.isFinite(computed) && computed > 0 ? computed : fallback;
        return sum + dailyCost;
      }, 0) * 7;

    // 2. Monthly baseline
    const monthlyCost = weeklyCost * 4;

    // 3. Total savings target
    const totalSavings = monthlyCost * targetFraction;
    const weeklySavings = totalSavings / 4;

    // 4. Build linear weekly decline
    const data = Array.from({ length: 4 }, (_, i) => ({
      week: `Week ${i + 1}`,
      current: weeklyCost,
      optimized: weeklyCost - weeklySavings * (i + 1),
    }));

    return {
      forecastData: data,
      totals: {
        totalCurrentCost: monthlyCost,
        totalOptimizedCost: monthlyCost - totalSavings,
        potentialSavings: totalSavings,
        savingsPercentage: pct.toFixed(1),
      },
      label: `${pct}% reduction by Week 4`,
    };
  }, [appliances, reductionPercent, pricePerKwh]);

  return (
    <div className="cost-forecast-card">
      <h2 className="cost-forecast-title">
        <TrendingDown size={24} className="title-icon" />
        Cost Forecast & Savings
      </h2>

      <div className="chart-container">
        <ResponsiveContainer width="100%" height={350}>
          <LineChart data={forecastData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis dataKey="week" stroke="#6B7280" />
            <YAxis stroke="#6B7280" label={{ value: 'Cost (€)', angle: -90, position: 'insideLeft' }} />
            <Tooltip formatter={(v) => `€${Number(v).toFixed(2)}`} />
            <Legend />
            <Line
              type="monotone"
              dataKey="current"
              stroke="#EF4444"
              strokeWidth={3}
              dot={{ r: 5 }}
              name="Current Usage"
            />
            <Line
              type="monotone"
              dataKey="optimized"
              stroke="#10B981"
              strokeWidth={3}
              dot={{ r: 5 }}
              name="Optimized Usage"
            />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <div className="savings-summary">
        <div className="savings-card">
          <p className="savings-label">Total Current Cost (4 weeks)</p>
          <p className="savings-amount current">€{totals.totalCurrentCost.toFixed(2)}</p>
        </div>

        <div className="savings-card">
          <p className="savings-label">Optimized Cost (4 weeks)</p>
          <p className="savings-amount optimized">€{totals.totalOptimizedCost.toFixed(2)}</p>
        </div>

        <div className="savings-card highlight">
          <p className="savings-label">Potential Monthly Savings</p>
          <p className="savings-amount savings">€{totals.potentialSavings.toFixed(2)}</p>
          <p className="savings-percentage">({totals.savingsPercentage}% total • {label})</p>
        </div>
      </div>
    </div>
  );
};

export default CostForecast;
