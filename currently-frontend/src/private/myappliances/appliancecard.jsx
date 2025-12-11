// src/private/myappliances/appliancecard.jsx
import React from "react";
import "./css/appliancecard.css";

/*
 * Props:
 *  - appliance: {
 *      id, applianceName, customName,
 *      usageType ("continuous" | "perUse"),
 *      hoursPerDay, usesPerDay,
 *      roomId, roomName
 *    }
 *  - baseAppliance: from /api/appliances for extra metadata (optional)
 *  - rooms: [{ id, name, floorLabel }]
 *  - onUpdate(id, partialFields)
 *  - onRemove(id)
 */

export default function ApplianceCard({
  appliance,
  baseAppliance,
  rooms,
  onUpdate,
  onRemove,
}) {
  const handleChange = (field, value) => {
    onUpdate(appliance.id, { [field]: value });
  };

  const isContinuous = appliance.usageType === "continuous";
  const usageLabel = isContinuous ? "Hours used per day" : "Uses per day";

  const currentRoomId = appliance.roomId ?? null;

  return (
    <div className="appliance-card">
      <div className="card-header">
        <input
          type="text"
          value={appliance.customName || appliance.applianceName || ""}
          onChange={(e) => handleChange("customName", e.target.value)}
          className="appliance-name"
          placeholder="Appliance name"
        />
        <button
          className="remove-btn"
          onClick={() => onRemove(appliance.id)}
          title="Remove appliance"
        >
          ✕
        </button>
      </div>

      {/* Optional subtext showing base appliance info */}
      {baseAppliance && (
        <div className="appliance-subtitle">
          <span className="appliance-base-name">
            {baseAppliance.name} ({baseAppliance.category})
          </span>
        </div>
      )}

      <div className="card-fields">
        {/* Room selection */}
        <div className="field-group">
          <label>Room</label>
          <select
            value={currentRoomId !== null ? String(currentRoomId) : "none"}
            onChange={(e) => {
              const val = e.target.value;
              if (val === "none") {
                handleChange("roomId", null);
              } else {
                handleChange("roomId", Number(val));
              }
            }}
            className="select-field"
          >
            <option value="none">Unassigned</option>
            {rooms.map((room) => (
              <option key={room.id} value={room.id}>
                {room.name} {room.floorLabel ? `(${room.floorLabel})` : ""}
              </option>
            ))}
          </select>
        </div>

        {/* Usage type (read-only text) */}
        <div className="field-group">
          <label>Usage Type</label>
          <div className="usage-type-pill">
            {isContinuous ? "Continuous" : "Per use"}
          </div>
        </div>

        {/* Hours per day */}
        <div className="field-group">
          <label>Hours per day</label>
          <input
            type="number"
            min="0"
            step="0.1"
            value={appliance.hoursPerDay ?? ""}
            onChange={(e) =>
              handleChange(
                "hoursPerDay",
                e.target.value === "" ? null : Number(e.target.value)
              )
            }
            className="input-field"
            disabled={!isContinuous}
          />
        </div>

        {/* Uses per day */}
        <div className="field-group">
          <label>Uses per day</label>
          <input
            type="number"
            min="0"
            step="0.1"
            value={appliance.usesPerDay ?? ""}
            onChange={(e) =>
              handleChange(
                "usesPerDay",
                e.target.value === "" ? null : Number(e.target.value)
              )
            }
            className="input-field"
            disabled={isContinuous}
          />
        </div>
      </div>
    </div>
  );
}
