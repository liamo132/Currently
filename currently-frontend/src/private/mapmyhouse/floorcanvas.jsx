// pages/mapmyhouse/floorcanvas.jsx
import React from 'react';
import { Plus, Edit2, Trash2, Home } from 'lucide-react';
import { getRoomColor, getRoomIcon } from './roomUtils';
import './css/floorcanvas.css';

export default function FloorCanvas({ floor, onAddRoom, onEditRoom, onDeleteRoom }) {
  return (
    <div className="floorcanvas-container">
      <div className="floorcanvas-header">
        <h2 className="floorcanvas-title">{floor?.name}</h2>
        <button className="floorcanvas-add-btn" onClick={onAddRoom}>
          <Plus className="icon-sm" />
          Add Room
        </button>
      </div>

      <div className="room-grid">
        {floor?.rooms.map((room) => (
          <div key={room.id} className={`room-card ${getRoomColor(room.type)}`}>
            <div className="room-card-header">
              <div className="room-icon">{getRoomIcon(room.type)}</div>
              <div className="room-actions">
                <button
                  className="room-edit-btn"
                  onClick={() => onEditRoom(room)}
                >
                  <Edit2 className="icon-sm" />
                </button>
                <button
                  className="room-delete-btn"
                  onClick={() => onDeleteRoom(room.id)}
                >
                  <Trash2 className="icon-sm" />
                </button>
              </div>
            </div>
            <h3 className="room-name">{room.name}</h3>
            <p className="room-type">{room.type}</p>
            <div className="room-footer">
              <span className="room-appliance-count">
                {room.appliances.length} appliance{room.appliances.length !== 1 ? 's' : ''}
              </span>
            </div>
          </div>
        ))}

        {floor?.rooms.length === 0 && (
          <div className="room-empty-state">
            <Home className="empty-icon" />
            <p className="empty-text">No rooms yet. Click "Add Room" to get started!</p>
          </div>
        )}
      </div>
    </div>
  );
}