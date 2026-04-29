// src/private/myappliances/appliancecard.jsx
import React, { useCallback, useState } from "react";
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

function ApplianceCard({
  appliance,
  baseAppliance,
  rooms,
  onUpdate,
  onRemove,
}) {
  const normaliseWholeNumber = (value, { min = 1, max } = {}) => {
    if (value === "") return undefined;

    const parsed = Number.parseInt(value, 10);
    if (Number.isNaN(parsed)) return undefined;

    const boundedValue = Math.max(min, parsed);
    return max !== undefined ? Math.min(boundedValue, max) : boundedValue;
  };

  const normaliseDecimalNumber = (value, { min = 0.01, max } = {}) => {
    if (value === "") return undefined;

    const parsed = Number.parseFloat(value);
    if (Number.isNaN(parsed)) return undefined;

    const boundedValue = Math.max(min, parsed);
    return max !== undefined ? Math.min(boundedValue, max) : boundedValue;
  };

  const formatNumberForInput = (value) => {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
      return "";
    }

    return Number(value).toFixed(2).replace(/\.?0+$/, "");
  };

  const handleChange = useCallback(
    (field, value) => {
      if (value === undefined) return;
      onUpdate(appliance.id, { [field]: value });
    },
    [appliance.id, onUpdate]
  );

  const isContinuous = appliance.usageType === "continuous";
  const currentRoomId = appliance.roomId ?? null;
  const [useWeeklyInput, setUseWeeklyInput] = useState(false);

  const handleUsesChange = (value) => {
    const parsed = normaliseDecimalNumber(value, {
      max: useWeeklyInput ? 700 : 100,
    });

    if (parsed === undefined) return;

    handleChange("usesPerDay", useWeeklyInput ? parsed / 7 : parsed);
  };

  const displayedUses = useWeeklyInput
    ? formatNumberForInput((appliance.usesPerDay ?? 0) * 7)
    : formatNumberForInput(appliance.usesPerDay);

  return (
    <div className="appliance-card">
      <div className="appliance-identity">
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
            x
          </button>
        </div>

        {baseAppliance && (
          <div className="appliance-subtitle">
            <span className="appliance-base-name">
              {baseAppliance.name} ({baseAppliance.category})
            </span>
          </div>
        )}
      </div>

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

      <div className="field-group">
        <label>Usage Type</label>
        <div className="usage-type-pill">
          {isContinuous ? "Continuous" : "Per use"}
        </div>
      </div>

      <div className={`field-group ${!isContinuous ? "field-group--inactive" : ""}`}>
        <label>Hours per day</label>
        {isContinuous ? (
          <input
            type="number"
            min="1"
            max="24"
            step="1"
            inputMode="numeric"
            value={appliance.hoursPerDay ?? ""}
            onChange={(e) =>
              handleChange(
                "hoursPerDay",
                normaliseWholeNumber(e.target.value, { max: 24 })
              )
            }
            className="input-field"
          />
        ) : (
          <div className="input-field input-field--inactive">N/A</div>
        )}
      </div>

      <div className={`field-group ${isContinuous ? "field-group--inactive" : ""}`}>
        <label>{useWeeklyInput ? "Uses per week" : "Uses per day"}</label>
        {isContinuous ? (
          <div className="input-field input-field--inactive">N/A</div>
        ) : (
          <>
            <input
              type="number"
              min="0.01"
              max={useWeeklyInput ? "700" : "100"}
              step="0.01"
              inputMode="decimal"
              value={displayedUses}
              onChange={(e) => handleUsesChange(e.target.value)}
              className="input-field"
            />
            <label className="weekly-toggle">
              <input
                type="checkbox"
                checked={useWeeklyInput}
                onChange={(e) => setUseWeeklyInput(e.target.checked)}
              />
              Weekly usage
            </label>
            {useWeeklyInput && (
              <span className="usage-equivalent">
                {formatNumberForInput(appliance.usesPerDay)} uses/day
              </span>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default React.memo(ApplianceCard);
