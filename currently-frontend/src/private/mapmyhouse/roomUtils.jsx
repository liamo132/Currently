// pages/mapmyhouse/roomUtils.js
// Shared constants and utilities for Map My House feature.

// Room templates: define display labels, icons, and color classes for common household Room types.
export const ROOM_TEMPLATES = [
  { type: 'Kitchen', icon: '🍳', color: 'room-orange' },
  { type: 'Bedroom', icon: '🛏️', color: 'room-blue' },
  { type: 'Living Room', icon: '🛋️', color: 'room-emerald' },
  { type: 'Bathroom', icon: '🚿', color: 'room-cyan' },
  { type: 'Office', icon: '💼', color: 'room-purple' },
  { type: 'Garage', icon: '🚗', color: 'room-gray' },
  { type: 'Laundry', icon: '🧺', color: 'room-pink' },
  { type: 'Custom', icon: '✏️', color: 'room-yellow' }
];

// Display helper: returns the CSS color class for a Room type.
export const getRoomColor = (type) => {
  return ROOM_TEMPLATES.find(t => t.type === type)?.color || 'room-gray';
};

// Display helper: returns the icon for a Room type.
export const getRoomIcon = (type) => {
  return ROOM_TEMPLATES.find(t => t.type === type)?.icon || '📦';
};

// Frontend State helper: initial house structure used before backend Rooms are loaded.
export const getInitialHouseData = () => ({
  houseName: 'My Home',
  floors: [
    {
      id: 'floor-1',
      name: 'Ground Floor',
      order: 0,
      rooms: []
    }
  ]
});

// Frontend State helper: temporary id for frontend-only floor groups.
export const generateFloorId = () => `floor-${Date.now()}`;

// Frontend State helper: temporary id for local-only Room drafts if needed.
export const generateRoomId = () => `room-${Date.now()}`;

// Validation helper: checks required Room fields before save.
export const validateRoomData = (roomData) => {
  if (!roomData.name || !roomData.name.trim()) {
    return { valid: false, error: 'Room name is required' };
  }
  if (!roomData.type) {
    return { valid: false, error: 'Room type is required' };
  }
  return { valid: true };
};

// Validation helper: checks required Floor fields for frontend floor grouping.
export const validateFloorData = (floorData) => {
  if (!floorData.name || !floorData.name.trim()) {
    return { valid: false, error: 'Floor name is required' };
  }
  return { valid: true };
};
