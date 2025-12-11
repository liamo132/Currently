// pages/mapmyhouse/floornav.jsx
import React from 'react';
import { Plus, Trash2, ChevronDown, ChevronRight, MapPin, Home } from 'lucide-react';
import { getRoomIcon } from './roomUtils';
import './css/floornav.css';

export default function FloorNav({
  floors,
  selectedFloor,
  expandedFloors,
  totalRooms,
  onSelectFloor,
  onToggleFloor,
  onAddFloor,
  onDeleteFloor
}) {
  return (
    <div className="floornav-container">
      {/* Floor List */}
      <div className="floornav-panel">
        <div className="floornav-header">
          <h2 className="floornav-title">Floors</h2>
          <span className="floornav-badge">{floors.length}</span>
        </div>

        <div className="floornav-list">
          {floors.map((floor) => (
            <div key={floor.id} className="floor-item">
              <div
                className={`floor-item-header ${selectedFloor === floor.id ? 'active' : ''}`}
                onClick={() => onSelectFloor(floor.id)}
              >
                <div className="floor-item-left">
                  <button
                    className="floor-toggle-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleFloor(floor.id);
                    }}
                  >
                    {expandedFloors.includes(floor.id) ? (
                      <ChevronDown className="icon-sm" />
                    ) : (
                      <ChevronRight className="icon-sm" />
                    )}
                  </button>
                  <span className="floor-name">{floor.name}</span>
                  <span className="floor-count">({floor.rooms.length})</span>
                </div>
                <button
                  className="floor-delete-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    onDeleteFloor(floor.id);
                  }}
                >
                  <Trash2 className="icon-sm" />
                </button>
              </div>

              {expandedFloors.includes(floor.id) && floor.rooms.length > 0 && (
                <div className="floor-rooms-list">
                  {floor.rooms.map(room => (
                    <div key={room.id} className="floor-room-item">
                      {getRoomIcon(room.type)} {room.name}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>

        <button className="floornav-add-btn" onClick={onAddFloor}>
          <Plus className="icon-sm" />
          Add Floor
        </button>
      </div>

      {/* Quick Stats */}
      <div className="floornav-stats">
        <h3 className="stats-title">Overview</h3>
        <div className="stats-grid">
          <div className="stat-item">
            <div className="stat-icon stat-icon-green">
              <MapPin className="icon-md" />
            </div>
            <div className="stat-content">
              <div className="stat-value">{floors.length}</div>
              <div className="stat-label">Total Floors</div>
            </div>
          </div>
          <div className="stat-item">
            <div className="stat-icon stat-icon-blue">
              <Home className="icon-md" />
            </div>
            <div className="stat-content">
              <div className="stat-value">{totalRooms}</div>
              <div className="stat-label">Total Rooms</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}