// pages/mapmyhouse/roomUtils.js
// Shared constants and utilities for Map My House feature

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

export const getRoomColor = (type) => {
  return ROOM_TEMPLATES.find(t => t.type === type)?.color || 'room-gray';
};

export const getRoomIcon = (type) => {
  return ROOM_TEMPLATES.find(t => t.type === type)?.icon || '📦';
};

// Initial house structure - useful for resetting or demo data
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

// Helper to generate unique IDs (temporary until backend provides them)
export const generateFloorId = () => `floor-${Date.now()}`;
export const generateRoomId = () => `room-${Date.now()}`;

// Validation helpers (ready for backend integration)
export const validateRoomData = (roomData) => {
  if (!roomData.name || !roomData.name.trim()) {
    return { valid: false, error: 'Room name is required' };
  }
  if (!roomData.type) {
    return { valid: false, error: 'Room type is required' };
  }
  return { valid: true };
};

export const validateFloorData = (floorData) => {
  if (!floorData.name || !floorData.name.trim()) {
    return { valid: false, error: 'Floor name is required' };
  }
  return { valid: true };
};