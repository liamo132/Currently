// watchyourwatts/roomconsumption.jsx
import React, { useMemo } from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import { Zap } from 'lucide-react';
import './css/roomconsumption.css';

/*
 * Component: RoomConsumption
 * Purpose: Builds a pie chart showing how daily kWh usage is distributed across Rooms.
 */
const RoomConsumption = ({ rooms, appliances }) => {
  // Chart data: sums dailyKWh by Room and converts each Room into a percentage of total usage.
  const data = useMemo(() => {
    const byRoomName = new Map();

    // Room grouping: seed buckets by Room id so assigned Appliances are counted correctly.
    (rooms || []).forEach((r) => byRoomName.set(r.id, { name: r.name, value: 0 }));

    const UNASSIGNED_KEY = '__unassigned__';
    byRoomName.set(UNASSIGNED_KEY, { name: 'Unassigned', value: 0 });

    (appliances || []).forEach((a) => {
      const kwh = Number(a.dailyKWh || 0);
      const key = a.roomId ?? UNASSIGNED_KEY;

      if (!byRoomName.has(key)) {
        byRoomName.set(key, { name: 'Unknown Room', value: 0 });
      }

      byRoomName.get(key).value += kwh;
    });

    const rows = Array.from(byRoomName.values())
      .filter((x) => x.value > 0)
      .sort((a, b) => b.value - a.value);

    const total = rows.reduce((sum, r) => sum + r.value, 0);

    return rows.map((r) => ({
      ...r,
      percentage: total > 0 ? Math.round((r.value / total) * 100) : 0,
    }));
  }, [rooms, appliances]);

  const COLORS = ['#F97316', '#3B82F6', '#8B5CF6', '#06B6D4', '#10B981', '#6B7280', '#EF4444'];

  return (
    <div className="room-consumption-card">
      <h2 className="room-consumption-title">
        <Zap size={24} className="title-icon" />
        Consumption by Room
      </h2>

      <div className="chart-container">
        {data.length === 0 ? (
          <p style={{ padding: '1rem' }}>No consumption data yet. Assign appliances to rooms to see breakdown.</p>
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percentage }) => `${name} ${percentage}%`}
                outerRadius={100}
                dataKey="value"
              >
                {data.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip formatter={(value) => `${Number(value).toFixed(2)} kWh/day`} />
            </PieChart>
          </ResponsiveContainer>
        )}
      </div>

      {data.length > 0 && (
        <div className="room-legend">
          {data.map((room, idx) => (
            <div key={room.name} className="legend-item">
              <div className="legend-color" style={{ backgroundColor: COLORS[idx % COLORS.length] }}></div>
              <span className="legend-text">
                {room.name}: {room.value.toFixed(2)} kWh/day
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default RoomConsumption;
