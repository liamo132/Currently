/*
 * File: myappliances.jsx
 * Description: Private page implementing the "My Appliances" feature by
 *              combining the base appliances catalogue (appliances.json)
 *              with user-specific selections stored in the backend.
 * Author: Liam Connell
 */


import React, { useEffect, useState, useCallback } from "react";
import HeaderUser from "../../public/components/header-user";
import ApplianceCard from "./appliancecard";
import "./css/myappliances.css";

export default function MyAppliances() {
  const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

  const [catalogue, setCatalogue] = useState([]);
  const [userAppliances, setUserAppliances] = useState([]);
  const [rooms, setRooms] = useState([]);

  const [selectedBaseName, setSelectedBaseName] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedRoomFilter, setSelectedRoomFilter] = useState(""); // roomId as string or "" for all

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  // Authenticated fetch helper
  const fetchWithAuth = useCallback(async (url, options = {}) => {
    const token = localStorage.getItem("token");
    if (!token) {
      throw new Error("No authentication token found. Please log in again.");
    }

    const headers = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    };

    const response = await fetch(url, { ...options, headers });
    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || `Request failed with status ${response.status}`);
    }
    return response;
  }, []);

  const findBaseAppliance = (applianceName) =>
    catalogue.find((a) => a.name === applianceName);

  // Load catalogue + user appliances + rooms
  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        setError("");

        // 1) Base catalogue (public)
        const catRes = await fetch(`${API_BASE}/api/appliances`);
        if (!catRes.ok) {
          throw new Error("Failed to load base appliances catalogue.");
        }
        const catData = await catRes.json();
        setCatalogue(catData);

        if (catData.length > 0) {
          setSelectedBaseName(catData[0].name);
        }

        // 2) User appliances (auth)
        const userRes = await fetchWithAuth(
          `${API_BASE}/api/users/me/appliances`
        );
        const userData = await userRes.json();
        setUserAppliances(userData);

        // 3) Rooms from Map My House
        const roomsRes = await fetchWithAuth(
          `${API_BASE}/api/users/me/rooms`
        );
        const roomsData = await roomsRes.json();
        setRooms(roomsData);
      } catch (err) {
        setError(err.message || "An unexpected error occurred while loading.");
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [API_BASE, fetchWithAuth]);

  // Unique room options from rooms API
  const roomOptions = rooms.map((r) => ({
    id: r.id,
    name: r.name,
    floorLabel: r.floorLabel,
  }));

  // Filter by search + room
  const filteredAppliances = userAppliances.filter((ua) => {
    const label = (ua.customName || ua.applianceName || "").toLowerCase();
    const matchesSearch = label.includes(searchTerm.toLowerCase());

    const roomId = ua.roomId || null;
    const filterRoomId = selectedRoomFilter ? Number(selectedRoomFilter) : null;

    const matchesRoom =
      !filterRoomId || (roomId !== null && roomId === filterRoomId);

    return matchesSearch && matchesRoom;
  });

  // Add new appliance from catalogue (initially unassigned to any room)
  const handleAddAppliance = async () => {
    if (!selectedBaseName) {
      alert("Please select an appliance from the list.");
      return;
    }

    const base = findBaseAppliance(selectedBaseName);
    if (!base) {
      alert("Selected appliance not found in catalogue.");
      return;
    }

    try {
      setError("");

      const payload = {
        applianceName: base.name,
        customName: base.name,
        usageType: base.usageType, // "continuous" or "perUse"
        hoursPerDay:
          base.usageType === "continuous" ? base.defaultHoursPerDay || 1 : null,
        usesPerDay:
          base.usageType === "perUse" ? base.defaultUsesPerDay || 1 : null,
        roomId: null, // user will assign room later
      };

      const res = await fetchWithAuth(
        `${API_BASE}/api/users/me/appliances`,
        {
          method: "POST",
          body: JSON.stringify(payload),
        }
      );

      const created = await res.json();
      setUserAppliances((current) => [...current, created]);
    } catch (err) {
      setError(err.message || "Failed to add appliance.");
    }
  };

  const handleConfirmAdd = async () => {
    await handleAddAppliance();
    setIsAddModalOpen(false);
  };

  // Update appliance (usage, name, room)
  const handleUpdateAppliance = async (id, updatedFields) => {
    const existing = userAppliances.find((ua) => ua.id === id);
    if (!existing) return;

    try {
      setError("");

      const payload = {
        applianceName: existing.applianceName,
        customName:
          updatedFields.customName !== undefined
            ? updatedFields.customName
            : existing.customName,
        usageType: existing.usageType,
        hoursPerDay:
          updatedFields.hoursPerDay !== undefined
            ? updatedFields.hoursPerDay
            : existing.hoursPerDay,
        usesPerDay:
          updatedFields.usesPerDay !== undefined
            ? updatedFields.usesPerDay
            : existing.usesPerDay,
        roomId:
          updatedFields.roomId !== undefined
            ? updatedFields.roomId
            : existing.roomId ?? null,
      };

      const res = await fetchWithAuth(
        `${API_BASE}/api/users/me/appliances/${id}`,
        {
          method: "PUT",
          body: JSON.stringify(payload),
        }
      );

      const updated = await res.json();
      setUserAppliances((current) =>
        current.map((ua) => (ua.id === id ? updated : ua))
      );
    } catch (err) {
      setError(err.message || "Failed to update appliance.");
    }
  };

  // Delete appliance
  const handleRemoveAppliance = async (id) => {
    if (!window.confirm("Remove this appliance?")) return;

    try {
      setError("");

      await fetchWithAuth(
        `${API_BASE}/api/users/me/appliances/${id}`,
        {
          method: "DELETE",
        }
      );

      setUserAppliances((current) => current.filter((ua) => ua.id !== id));
    } catch (err) {
      setError(err.message || "Failed to remove appliance.");
    }
  };

  if (loading) {
    return (
      <div className="myappliances-container">
        <HeaderUser activePage="myappliances" />
        <div className="myappliances-content">
          <h1 className="myappliances-title">My Appliances</h1>
          <p>Loading your appliances...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="myappliances-container">
      <HeaderUser activePage="myappliances" />

      <div className="myappliances-content">
        <h1 className="myappliances-title">My Appliances</h1>

        {error && (
          <div className="myappliances-error">
            {error}
          </div>
        )}

        {/* Search + room filter */}
        <div className="filter-row">
          <div className="search-bar">
            <input
              type="text"
              placeholder="Search your appliances"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
          </div>

          <div className="room-filter">
            <select
              className="room-select"
              value={selectedRoomFilter}
              onChange={(e) => setSelectedRoomFilter(e.target.value)}
            >
              <option value="">All rooms</option>
              <option value="none">Unassigned</option>
              {roomOptions.map((room) => (
                <option key={room.id} value={room.id}>
                  {room.name} {room.floorLabel ? `(${room.floorLabel})` : ""}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Scrollable box with cards */}
        <div className="appliances-container">
          <div className="appliances-grid">
            {filteredAppliances.map((appliance) => (
              <ApplianceCard
                key={appliance.id}
                appliance={appliance}
                baseAppliance={findBaseAppliance(appliance.applianceName)}
                rooms={roomOptions}
                onUpdate={handleUpdateAppliance}
                onRemove={handleRemoveAppliance}
              />
            ))}

            {filteredAppliances.length === 0 && (
              <p className="empty-state">
                No appliances found. Adjust your search/room filter or click
                “+ Add Appliance”.
              </p>
            )}
          </div>
        </div>

        {/* Action button under the grey box */}
        <div className="actions">
          <button
            className="add-btn"
            onClick={() => setIsAddModalOpen(true)}
          >
            + Add Appliance
          </button>
        </div>
      </div>

      {/* Add-appliance modal */}
      {isAddModalOpen && (
        <div className="modal-backdrop">
          <div className="modal">
            <h2 className="modal-title">Add Appliance</h2>

            <label className="modal-label">Select from catalogue</label>
            <select
              className="modal-select"
              value={selectedBaseName}
              onChange={(e) => setSelectedBaseName(e.target.value)}
            >
              {catalogue.map((appliance) => (
                <option key={appliance.name} value={appliance.name}>
                  {appliance.name} ({appliance.category})
                </option>
              ))}
            </select>

            {selectedBaseName && (
              <div className="modal-preview">
                {(() => {
                  const base = findBaseAppliance(selectedBaseName);
                  if (!base) return null;
                  return (
                    <>
                      <div><strong>Category:</strong> {base.category}</div>
                      <div>
                        <strong>Usage type:</strong>{" "}
                        {base.usageType === "continuous"
                          ? "Continuous (hours per day)"
                          : "Per use (uses per day)"}
                      </div>
                    </>
                  );
                })()}
              </div>
            )}

            <div className="modal-actions">
              <button
                className="modal-btn primary"
                onClick={handleConfirmAdd}
              >
                Add
              </button>
              <button
                className="modal-btn"
                onClick={() => setIsAddModalOpen(false)}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}