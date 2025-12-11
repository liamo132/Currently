/*
 * File: appliancecard.jsx
 * Description: Presentational card for a single user-selected appliance, showing
 *              base info from appliances.json and editable usage fields.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

import React from 'react';
import './css/appliancecard.css';

/**
 * Component: ApplianceCard
 * Purpose:
 *   Display an appliance selected by the user, including:
 *   - Base catalogue information (name, category, watts)
 *   - User-specific fields (custom name, hoursPerDay / usesPerDay)
 *   - Derived values (daily kWh, estimated daily cost)
 *
 * Props:
 *   - appliance: UserApplianceResponse from backend
 *       { id, applianceName, customName, usageType,
 *         hoursPerDay, usesPerDay, dailyKWh, estimatedDailyCost }
 *   - baseAppliance: matching catalogue entry from appliances.json
 *       { name, category, usageType, averageWatts, averageWattsPerUse,
 *         defaultHoursPerDay, defaultUsesPerDay }
 *   - onUpdate: function(id, updatedFields) → called when user edits fields
 *   - onRemove: function(id) → called when user clicks remove
 */
export default function ApplianceCard({ appliance, baseAppliance, onUpdate, onRemove }) {
  const isContinuous = appliance.usageType === 'continuous';

  // Function: handleChange
  // Purpose:
  //   Wrap field updates and send them to the parent with the appliance id.
  const handleChange = (field, value) => {
    onUpdate(appliance.id, { [field]: value });
  };

  const displayName = appliance.customName || appliance.applianceName;
  const usageLabel = isContinuous ? 'Hours Used Per Day' : 'Uses Per Day';

  const safeDailyKWh = appliance.dailyKWh ?? 0;
  const safeDailyCost = appliance.estimatedDailyCost ?? 0;

  return (
    <div className="appliance-card">
      {/* Header: editable custom name + remove button */}
      <div className="card-header">
        <input
          type="text"
          value={displayName}
          onChange={(e) => handleChange('customName', e.target.value)}
          className="appliance-name"
        />
        <button
          className="remove-btn"
          onClick={() => onRemove(appliance.id)}
          title="Remove appliance"
        >
          ✕
        </button>
      </div>

      {/* Main grid fields */}
      <div className="card-fields">
        {/* Base catalogue info: name + category */}
        <div className="field-group">
          <label>Catalogue Name</label>
          <div className="readonly-text">
            {baseAppliance ? baseAppliance.name : appliance.applianceName}
          </div>
        </div>

        <div className="field-group">
          <label>Category</label>
          <div className="readonly-text">
            {baseAppliance ? baseAppliance.category : 'N/A'}
          </div>
        </div>

        {/* Base technical info */}
        <div className="field-group">
          <label>{isContinuous ? 'Average Watts' : 'Average Watts per Use'}</label>
          <div className="readonly-text">
            {isContinuous
              ? baseAppliance?.averageWatts ?? 'N/A'
              : baseAppliance?.averageWattsPerUse ?? 'N/A'}
          </div>
        </div>

        {/* Usage input (hoursPerDay or usesPerDay) */}
        <div className="field-group">
          <label>{usageLabel}</label>
          <input
            type="number"
            className="input-field"
            min="0"
            step="0.1"
            value={
              isContinuous
                ? appliance.hoursPerDay ?? baseAppliance?.defaultHoursPerDay ?? 0
                : appliance.usesPerDay ?? baseAppliance?.defaultUsesPerDay ?? 0
            }
            onChange={(e) => {
              const value = parseFloat(e.target.value) || 0;
              if (isContinuous) {
                handleChange('hoursPerDay', value);
              } else {
                handleChange('usesPerDay', value);
              }
            }}
          />
        </div>

        {/* Derived values from backend */}
        <div className="field-group">
          <label>Estimated Daily kWh</label>
          <div className="readonly-text">{safeDailyKWh.toFixed(3)}</div>
        </div>

        <div className="field-group">
          <label>Estimated Daily Cost (€)</label>
          <div className="readonly-text">{safeDailyCost.toFixed(2)}</div>
        </div>
      </div>
    </div>
  );
}
