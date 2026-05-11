// pages/mapmyhouse/roomform.jsx
import React, { useState, useEffect } from 'react';
import { ROOM_TEMPLATES } from './roomUtils';
import './css/roomform.css';

/*
 * Component: RoomForm
 * Purpose: Modal form used for both creating and editing Map My House Rooms.
 */
export default function RoomForm({ room, onSave, onClose }) {
  const [formData, setFormData] = useState({
    name: '',
    type: 'Kitchen'
  });

  // Hook: when editing, pre-fills the form with the selected Room's current name and type.
  useEffect(() => {
    if (room) {
      setFormData({
        name: room.name,
        type: room.type
      });
    }
  }, [room]);

  // Event handler: validates a Room name before sending the form data back to MapMyHouse.
  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (!formData.name.trim()) {
      alert('Please enter a room name');
      return;
    }

    onSave(formData);
    onClose();
  };

  return (
    <div className="roomform-overlay" onClick={onClose}>
      <div className="roomform-modal" onClick={(e) => e.stopPropagation()}>
        <h3 className="roomform-title">
          {room ? 'Edit Room' : 'Add New Room'}
        </h3>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Room Name</label>
            <input
              type="text"
              className="form-input"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="e.g., Liam's Room"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label className="form-label">Room Type</label>
            <select
              className="form-select"
              value={formData.type}
              onChange={(e) => setFormData({ ...formData, type: e.target.value })}
            >
              {ROOM_TEMPLATES.map((template) => (
                <option key={template.type} value={template.type}>
                  {template.icon} {template.type}
                </option>
              ))}
            </select>
          </div>

          <div className="form-actions">
            <button type="submit" className="form-btn form-btn-primary">
              {room ? 'Update Room' : 'Save Room'}
            </button>
            <button type="button" className="form-btn form-btn-secondary" onClick={onClose}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
