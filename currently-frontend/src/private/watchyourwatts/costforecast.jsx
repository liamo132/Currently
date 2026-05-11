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

// Math helper: keeps the reduction percent inside the supported Forecast range.
const clamp = (v, min, max) => Math.min(max, Math.max(min, v));

/*
 * Component: CostForecast
 * Purpose: Renders a four-week line chart comparing current electricity Cost with an optimized reduction target.
 */
const CostForecast = ({
  appliances = [],
  reductionPercent = 10,
  pricePerKwh = 0.3,
}) => {
  /*
   * Chart data calculation
   * Purpose: Converts Appliance daily costs into weekly/monthly totals, applies the selected reduction target,
   * and creates four weekly chart points for Recharts.
   */
  const { forecastData, totals, label } = useMemo(() => {
    const pct = clamp(Number(reductionPercent) || 1, 1, 10);
    const targetFraction = pct / 100;

    // Cost Calculation: recompute from daily kWh where possible so Dashboard price changes flow through.
    const dailyCost = appliances.reduce((sum, a) => {
      const dailyKwh = Number(a.dailyKWh || 0);
      const computed = Number(a.computedDailyCost ?? dailyKwh * pricePerKwh);
      const fallback = Number(a.estimatedDailyCost || 0);
      const applianceDailyCost = Number.isFinite(computed) && computed > 0 ? computed : fallback;
      return sum + applianceDailyCost;
    }, 0);

    const weeklyCost = dailyCost * 7;
    const monthlyCost = dailyCost * 30;

    // Savings Calculation: totalSavings is monthly cost multiplied by the chosen reduction percentage.
    const totalSavings = monthlyCost * targetFraction;
    const weeklySavings = totalSavings / 4;

    // Chart data: build a simple linear weekly decline toward the end-of-month target.
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
            <YAxis stroke="#6B7280" label={{ value: 'Cost (EUR)', angle: -90, position: 'insideLeft' }} />
            <Tooltip formatter={(v) => `EUR ${Number(v).toFixed(2)}`} />
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
          <p className="savings-amount current">EUR {totals.totalCurrentCost.toFixed(2)}</p>
        </div>

        <div className="savings-card">
          <p className="savings-label">Optimized Cost (4 weeks)</p>
          <p className="savings-amount optimized">EUR {totals.totalOptimizedCost.toFixed(2)}</p>
        </div>

        <div className="savings-card highlight">
          <p className="savings-label">Potential Monthly Savings</p>
          <p className="savings-amount savings">EUR {totals.potentialSavings.toFixed(2)}</p>
          <p className="savings-percentage">({totals.savingsPercentage}% total - {label})</p>
        </div>
      </div>
    </div>
  );
};

export default CostForecast;
