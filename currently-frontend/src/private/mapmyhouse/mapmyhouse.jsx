// pages/mapmyhouse/mapmyhouse.jsx
/*
 * File: mapmyhouse.jsx
 * Description: "Map My House" page. Uses existing UI components (FloorNav,
 *              FloorCanvas, RoomForm) but persists rooms to the backend
 *              via /api/users/me/rooms. Floors are a frontend grouping
 *              over Room.floorLabel.
 */

import React, { useState, useEffect, useCallback } from 'react';
// NOTE: path depends on your structure; this matches your latest tree:
import HeaderUser from '../../public/components/header-user';
import FloorNav from './floornav';
import FloorCanvas from './floorcanvas';
import RoomForm from './roomform';
import { getInitialHouseData } from './roomUtils';
import './css/mapmyhouse.css';

export default function MapMyHouse() {
  const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

  // Existing house structure in state
  const [house, setHouse] = useState(getInitialHouseData());

  const [selectedFloor, setSelectedFloor] = useState('floor-1');
  const [expandedFloors, setExpandedFloors] = useState(['floor-1']);
  const [showRoomForm, setShowRoomForm] = useState(false);
  const [editingRoom, setEditingRoom] = useState(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  /**
   * Helper: authenticated fetch
   */
  const fetchWithAuth = useCallback(async (url, options = {}) => {
    const token = localStorage.getItem('token');
    if (!token) {
      throw new Error('No authentication token found. Please log in again.');
    }

    const headers = {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    };

    const res = await fetch(url, { ...options, headers });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || `Request failed with status ${res.status}`);
    }
    return res;
  }, []);

  /**
   * Helper: build house state (floors + rooms) from backend rooms.
   * Backend RoomResponse shape: { id, name, floorLabel, type }
   */
  const buildHouseFromRooms = (rooms) => {
    if (!rooms || rooms.length === 0) {
      return getInitialHouseData();
    }

    const floorMap = new Map();

    rooms.forEach((room) => {
      const floorLabel = room.floorLabel || 'Floor';
      if (!floorMap.has(floorLabel)) {
        floorMap.set(floorLabel, {
          id: floorLabel,      // use floorLabel as stable id
          name: floorLabel,
          order: floorMap.size,
          rooms: [],
        });
      }
      const floor = floorMap.get(floorLabel);
      floor.rooms.push({
        id: room.id,          // backend room id
        name: room.name,
        type: room.type,
        appliances: [],       // will be linked later from MyAppliances
      });
    });

    return {
      houseName: 'My Home',
      floors: Array.from(floorMap.values()),
    };
  };

  /**
   * Load rooms from backend on mount.
   */
  useEffect(() => {
    const loadRooms = async () => {
      try {
        setLoading(true);
        setError('');

        const res = await fetchWithAuth(`${API_BASE}/api/users/me/rooms`);
        const data = await res.json(); // List<RoomResponse>

        const newHouse = buildHouseFromRooms(data);
        setHouse(newHouse);

        if (newHouse.floors.length > 0) {
          const firstFloorId = newHouse.floors[0].id;
          setSelectedFloor((prev) =>
            newHouse.floors.some((f) => f.id === prev) ? prev : firstFloorId
          );
          setExpandedFloors([firstFloorId]);
        }
      } catch (err) {
        console.error(err);
        setError(err.message || 'Failed to load rooms.');
      } finally {
        setLoading(false);
      }
    };

    loadRooms();
  }, [API_BASE, fetchWithAuth]);

  /**
   * Floor operations (pure frontend; floors are derived from Room.floorLabel).
   * Note: empty floors will only exist in frontend until a room is added.
   */

  const addFloor = () => {
    const newFloorNumber = house.floors.length + 1;
    const newFloorName = `Floor ${newFloorNumber}`;

    const newFloor = {
      id: newFloorName,      // keep id == name (used as floorLabel)
      name: newFloorName,
      order: house.floors.length,
      rooms: [],
    };

    const updatedFloors = [...house.floors, newFloor];
    setHouse({ ...house, floors: updatedFloors });
    setSelectedFloor(newFloor.id);
    setExpandedFloors([...expandedFloors, newFloor.id]);
    // Backend will persist this floor label once first room is created on it.
  };

  const deleteFloor = (floorId) => {
    const currentFloor = house.floors.find((f) => f.id === floorId);
    if (!currentFloor) return;

    if (house.floors.length === 1) {
      alert('You must have at least one floor!');
      return;
    }

    if (currentFloor.rooms.length > 0) {
      alert('To keep backend consistent, delete rooms on this floor first.');
      return;
    }

    if (window.confirm('Delete this floor?')) {
      const updatedFloors = house.floors.filter((f) => f.id !== floorId);
      setHouse({ ...house, floors: updatedFloors });
      if (selectedFloor === floorId && updatedFloors.length > 0) {
        setSelectedFloor(updatedFloors[0].id);
      }
    }
  };

  const toggleFloor = (floorId) => {
    setExpandedFloors((prev) =>
      prev.includes(floorId)
        ? prev.filter((id) => id !== floorId)
        : [...prev, floorId]
    );
  };

  /**
   * Room operations wired to backend
   */

  const addRoom = async (roomData) => {
    const floor = house.floors.find((f) => f.id === selectedFloor);
    if (!floor) {
      alert('Please select a floor first.');
      return;
    }

    try {
      setError('');

      const payload = {
        name: roomData.name,
        type: roomData.type,
        floorLabel: floor.name, // use floor name as label in backend
      };

      const res = await fetchWithAuth(`${API_BASE}/api/users/me/rooms`, {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      const created = await res.json(); // RoomResponse

      const newRoom = {
        id: created.id,
        name: created.name,
        type: created.type,
        appliances: [],
      };

      const updatedFloors = house.floors.map((f) => {
        if (f.id === selectedFloor) {
          return { ...f, rooms: [...f.rooms, newRoom] };
        }
        return f;
      });

      setHouse({ ...house, floors: updatedFloors });
    } catch (err) {
      console.error(err);
      setError(err.message || 'Failed to add room.');
    }
  };

  const updateRoom = async (roomData) => {
    if (!editingRoom) return;

    const floor = house.floors.find((f) => f.id === selectedFloor);
    if (!floor) return;

    try {
      setError('');

      const payload = {
        name: roomData.name,
        type: roomData.type,
        floorLabel: floor.name,
      };

      const res = await fetchWithAuth(
        `${API_BASE}/api/users/me/rooms/${editingRoom.id}`,
        {
          method: 'PUT',
          body: JSON.stringify(payload),
        }
      );

      const updated = await res.json(); // RoomResponse

      const updatedFloors = house.floors.map((f) => {
        if (f.id === selectedFloor) {
          return {
            ...f,
            rooms: f.rooms.map((room) =>
              room.id === editingRoom.id
                ? { ...room, name: updated.name, type: updated.type }
                : room
            ),
          };
        }
        return f;
      });

      setHouse({ ...house, floors: updatedFloors });
    } catch (err) {
      console.error(err);
      setError(err.message || 'Failed to update room.');
    }
  };

  const deleteRoom = async (roomId) => {
    if (!window.confirm('Delete this room?')) return;

    try {
      setError('');

      await fetchWithAuth(`${API_BASE}/api/users/me/rooms/${roomId}`, {
        method: 'DELETE',
      });

      const updatedFloors = house.floors.map((floor) => {
        if (floor.id === selectedFloor) {
          return {
            ...floor,
            rooms: floor.rooms.filter((r) => r.id !== roomId),
          };
        }
        return floor;
      });

      setHouse({ ...house, floors: updatedFloors });
    } catch (err) {
      console.error(err);
      setError(err.message || 'Failed to delete room.');
    }
  };

  const startEditRoom = (room) => {
    setEditingRoom(room);
    setShowRoomForm(true);
  };

  const startAddRoom = () => {
    setEditingRoom(null);
    setShowRoomForm(true);
  };

  const closeRoomForm = () => {
    setShowRoomForm(false);
    setEditingRoom(null);
  };

  const currentFloor = house.floors.find((f) => f.id === selectedFloor);
  const totalRooms = house.floors.reduce(
    (sum, floor) => sum + floor.rooms.length,
    0
  );

  if (loading) {
    return (
      <div className="mapmyhouse-container">
        <HeaderUser activePage="mapmyhouse" />
        <div className="mapmyhouse-content">
          <h1 className="mapmyhouse-title">Map My House</h1>
          <p>Loading your house layout...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mapmyhouse-container">
      <HeaderUser activePage="mapmyhouse" />

      <div className="mapmyhouse-content">
        <h1 className="mapmyhouse-title">Map My House</h1>

        {error && (
          <div className="myappliances-error" style={{ marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        <div className="mapmyhouse-grid">
          <FloorNav
            floors={house.floors}
            selectedFloor={selectedFloor}
            expandedFloors={expandedFloors}
            totalRooms={totalRooms}
            onSelectFloor={setSelectedFloor}
            onToggleFloor={toggleFloor}
            onAddFloor={addFloor}
            onDeleteFloor={deleteFloor}
          />

          <FloorCanvas
            floor={currentFloor}
            onAddRoom={startAddRoom}
            onEditRoom={startEditRoom}
            onDeleteRoom={deleteRoom}
          />
        </div>
      </div>

      {showRoomForm && (
        <RoomForm
          room={editingRoom}
          onSave={editingRoom ? updateRoom : addRoom}
          onClose={closeRoomForm}
        />
      )}
    </div>
  );
}
