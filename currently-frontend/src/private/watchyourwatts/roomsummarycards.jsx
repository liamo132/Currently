import React, { useMemo } from 'react';
import { Home } from 'lucide-react';
import './css/roomsummarycards.css';

const UNASSIGNED_KEY = '__unassigned__';

export default function RoomSummaryCards({ rooms = [], appliances = [] }) {
  const summaries = useMemo(() => {
    const byRoom = new Map();

    rooms.forEach((room) => {
      byRoom.set(room.id, {
        id: room.id,
        name: room.name,
        floorLabel: room.floorLabel,
        applianceCount: 0,
        dailyKwh: 0,
        dailyCost: 0,
      });
    });

    byRoom.set(UNASSIGNED_KEY, {
      id: UNASSIGNED_KEY,
      name: 'Unassigned',
      floorLabel: '',
      applianceCount: 0,
      dailyKwh: 0,
      dailyCost: 0,
    });

    appliances.forEach((appliance) => {
      const key = appliance.roomId ?? UNASSIGNED_KEY;
      if (!byRoom.has(key)) {
        byRoom.set(key, {
          id: key,
          name: appliance.roomName || 'Unknown Room',
          floorLabel: '',
          applianceCount: 0,
          dailyKwh: 0,
          dailyCost: 0,
        });
      }

      const summary = byRoom.get(key);
      summary.applianceCount += 1;
      summary.dailyKwh += Number(appliance.dailyKWh || 0);
      summary.dailyCost += Number(appliance.computedDailyCost ?? appliance.estimatedDailyCost ?? 0);
    });

    return Array.from(byRoom.values())
      .filter((room) => room.applianceCount > 0 || room.id !== UNASSIGNED_KEY)
      .sort((a, b) => b.dailyCost - a.dailyCost || a.name.localeCompare(b.name));
  }, [rooms, appliances]);

  const totalDailyCost = summaries.reduce((sum, room) => sum + room.dailyCost, 0);

  return (
    <section className="room-summary-section">
      <div className="room-summary-heading">
        <h2>
          <Home size={22} className="title-icon" />
          Room Summary
        </h2>
        <span>Daily room cost: EUR {totalDailyCost.toFixed(2)}</span>
      </div>

      {summaries.length === 0 ? (
        <p className="room-summary-empty">
          Add rooms and assign appliances to compare where electricity is being used.
        </p>
      ) : (
        <div className="room-summary-grid">
          {summaries.map((room) => (
            <article key={room.id} className="room-summary-card">
              <div>
                <h3>{room.name}</h3>
                <p>{room.floorLabel || 'No floor label'}</p>
              </div>
              <div className="room-summary-metrics">
                <span>{room.applianceCount} appliances</span>
                <strong>{room.dailyKwh.toFixed(2)} kWh/day</strong>
                <strong>EUR {room.dailyCost.toFixed(2)}/day</strong>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
