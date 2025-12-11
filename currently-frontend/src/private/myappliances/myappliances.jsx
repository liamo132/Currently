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
  const [selectedBaseName, setSelectedBaseName] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedRoomFilter, setSelectedRoomFilter] = useState("");
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

  // Load catalogue + user appliances
  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        setError("");

        const catRes = await fetch(`${API_BASE}/api/appliances`);
        if (!catRes.ok) {
          throw new Error("Failed to load base appliances catalogue.");
        }
        const catData = await catRes.json();
        setCatalogue(catData);

        if (catData.length > 0) {
          setSelectedBaseName(catData[0].name);
        }

        const userRes = await fetchWithAuth(
          `${API_BASE}/api/users/me/appliances`
        );
        const userData = await userRes.json();
        setUserAppliances(userData);
      } catch (err) {
        setError(err.message || "An unexpected error occurred while loading.");
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [API_BASE, fetchWithAuth]);

  const findBaseAppliance = (applianceName) =>
    catalogue.find((a) => a.name === applianceName);

  // Derive room options from catalogue categories (for "Search by room")
  const roomOptions = Array.from(
    new Set(catalogue.map((a) => a.category))
  ).sort();

  // Apply search + room filter
  const filteredAppliances = userAppliances.filter((ua) => {
    const label = (ua.customName || ua.applianceName || "").toLowerCase();
    const matchesSearch = label.includes(searchTerm.toLowerCase());

    const base = findBaseAppliance(ua.applianceName);
    const baseCategory = base ? base.category : "";

    const matchesRoom =
      !selectedRoomFilter || baseCategory === selectedRoomFilter;

    return matchesSearch && matchesRoom;
  });

  // Add appliance from catalogue
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
        usageType: base.usageType,
        hoursPerDay:
          base.usageType === "continuous" ? base.defaultHoursPerDay || 1 : null,
        usesPerDay:
          base.usageType === "perUse" ? base.defaultUsesPerDay || 1 : null,
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

  const handleRemoveAppliance = async (id) => {
    if (!window.confirm("Remove this appliance?")) return;

    try {
      setError("");

      await fetchWithAuth(`${API_BASE}/api/users/me/appliances/${id}`, {
        method: "DELETE",
      });

      setUserAppliances((current) => current.filter((ua) => ua.id !== id));
    } catch (err) {
      setError(err.message || "Failed to remove appliance.");
    }
  };

  const handleSaveAll = async () => {
    try {
      setError("");
      const res = await fetchWithAuth(
        `${API_BASE}/api/users/me/appliances`
      );
      const userData = await res.json();
      setUserAppliances(userData);
      alert("Appliances refreshed from server.");
    } catch (err) {
      setError(err.message || "Failed to refresh appliances.");
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

        {error && <div className="myappliances-error">{error}</div>}

        {/* Search + room filter row */}
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
              {roomOptions.map((room) => (
                <option key={room} value={room}>
                  {room}
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
                onUpdate={handleUpdateAppliance}
                onRemove={handleRemoveAppliance}
              />
            ))}

            {filteredAppliances.length === 0 && (
              <p className="empty-state">
                No appliances found. Adjust your search or room filter, or click
                “+ Add Appliance”.
              </p>
            )}
          </div>
        </div>

        {/* Single action button under the grey box */}
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
                      <div>
                        <strong>Category:</strong> {base.category}
                      </div>
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
