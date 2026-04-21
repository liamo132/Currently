/*
 * File: myappliances.jsx
 * Description: Private page implementing the "My Appliances" feature by
 *              combining the base appliances catalogue (appliances.json)
 *              with user-specific selections stored in the backend.
 * Author: Liam Connell
 */


import React, { useEffect, useState, useCallback, useMemo, useRef } from "react";
import HeaderUser from "../../public/components/header-user";
import ApplianceCard from "./appliancecard";
import "../shared/private-layout.css";
import "./css/myappliances.css";

function useDebouncedValue(value, delayMs) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => setDebouncedValue(value), delayMs);
    return () => window.clearTimeout(timeoutId);
  }, [value, delayMs]);

  return debouncedValue;
}

const csvEscape = (value) => {
  const text = value === null || value === undefined ? "" : String(value);
  return `"${text.replace(/"/g, '""')}"`;
};

export default function MyAppliances() {
  const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

  const [catalogue, setCatalogue] = useState([]);
  const [userAppliances, setUserAppliances] = useState([]);
  const [rooms, setRooms] = useState([]);
  const userAppliancesRef = useRef([]);

  const [selectedBaseName, setSelectedBaseName] = useState("");
  const [catalogueSearch, setCatalogueSearch] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedRoomFilter, setSelectedRoomFilter] = useState(""); // roomId as string or "" for all
  const [sortMode, setSortMode] = useState("cost-desc");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [saveMessage, setSaveMessage] = useState("");
  const debouncedSearchTerm = useDebouncedValue(searchTerm, 180);

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

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const [catRes, userRes, roomsRes] = await Promise.all([
        fetch(`${API_BASE}/api/appliances`),
        fetchWithAuth(`${API_BASE}/api/users/me/appliances`),
        fetchWithAuth(`${API_BASE}/api/users/me/rooms`),
      ]);

      if (!catRes.ok) {
        throw new Error("Failed to load base appliances catalogue.");
      }

      const [catData, userData, roomsData] = await Promise.all([
        catRes.json(),
        userRes.json(),
        roomsRes.json(),
      ]);

      setCatalogue(catData);
      setUserAppliances(userData);
      setRooms(roomsData);

      if (catData.length > 0) {
        setSelectedBaseName((current) => current || catData[0].name);
      }
    } catch (err) {
      setError(err.message || "An unexpected error occurred while loading.");
    } finally {
      setLoading(false);
    }
  }, [API_BASE, fetchWithAuth]);

  // Load catalogue + user appliances + rooms
  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    userAppliancesRef.current = userAppliances;
  }, [userAppliances]);

  const baseAppliancesByName = useMemo(() => {
    return new Map(catalogue.map((appliance) => [appliance.name, appliance]));
  }, [catalogue]);

  const findBaseAppliance = useCallback(
    (applianceName) => baseAppliancesByName.get(applianceName),
    [baseAppliancesByName]
  );

  const roomOptions = useMemo(
    () =>
      rooms.map((r) => ({
        id: r.id,
        name: r.name,
        floorLabel: r.floorLabel,
      })),
    [rooms]
  );

  const assignedAppliancesCount = useMemo(
    () =>
      userAppliances.filter(
        (ua) => ua.roomId !== null && ua.roomId !== undefined
      ).length,
    [userAppliances]
  );

  const roomNameById = useMemo(() => {
    return new Map(roomOptions.map((room) => [room.id, room.name]));
  }, [roomOptions]);

  const filteredCatalogue = useMemo(() => {
    const search = catalogueSearch.trim().toLowerCase();
    return catalogue.filter((appliance) => {
      const name = (appliance.name || "").toLowerCase();
      const category = (appliance.category || "").toLowerCase();
      return search === "" || name.includes(search) || category.includes(search);
    });
  }, [catalogue, catalogueSearch]);

  const filteredCatalogueGroups = useMemo(() => {
    return filteredCatalogue.reduce((groups, appliance) => {
      const category = appliance.category || "Other";
      if (!groups.has(category)) {
        groups.set(category, []);
      }
      groups.get(category).push(appliance);
      return groups;
    }, new Map());
  }, [filteredCatalogue]);

  useEffect(() => {
    if (!isAddModalOpen) return;

    if (filteredCatalogue.length === 0) {
      setSelectedBaseName("");
      return;
    }

    const selectedStillVisible = filteredCatalogue.some(
      (appliance) => appliance.name === selectedBaseName
    );

    if (!selectedStillVisible) {
      setSelectedBaseName(filteredCatalogue[0].name);
    }
  }, [filteredCatalogue, isAddModalOpen, selectedBaseName]);

  const selectedBaseAppliance = useMemo(
    () => findBaseAppliance(selectedBaseName),
    [findBaseAppliance, selectedBaseName]
  );

  const formatUsageLabel = (appliance) => {
    if (!appliance) return "";
    return appliance.usageType === "continuous"
      ? "Continuous"
      : "Per use";
  };

  const formatDefaultUsage = (appliance) => {
    if (!appliance) return "";
    if (appliance.usageType === "continuous") {
      return `${appliance.averageWatts}W for ${appliance.defaultHoursPerDay}h/day`;
    }
    return `${appliance.averageWattsPerUse}Wh per use, ${appliance.defaultUsesPerDay}/day`;
  };

  const filteredAppliances = useMemo(() => {
    // Filtering and sorting can run often while typing, so keep the search
    // debounced and derive the visible list in one memoized pass.
    const search = debouncedSearchTerm.trim().toLowerCase();

    return userAppliances
      .filter((ua) => {
        const label = (ua.customName || ua.applianceName || "").toLowerCase();
        const roomLabel = (ua.roomName || roomNameById.get(ua.roomId) || "").toLowerCase();
        const matchesSearch =
          search === "" || label.includes(search) || roomLabel.includes(search);

        const roomId = ua.roomId || null;
        let matchesRoom = true;

        if (selectedRoomFilter === "none") {
          matchesRoom = roomId === null;
        } else if (selectedRoomFilter) {
          matchesRoom = roomId !== null && roomId === Number(selectedRoomFilter);
        }

        return matchesSearch && matchesRoom;
      })
      .sort((a, b) => {
        if (sortMode === "name-asc") {
          return (a.customName || a.applianceName || "").localeCompare(
            b.customName || b.applianceName || ""
          );
        }
        if (sortMode === "room-asc") {
          return (a.roomName || "Unassigned").localeCompare(b.roomName || "Unassigned");
        }
        if (sortMode === "kwh-desc") {
          return Number(b.dailyKWh || 0) - Number(a.dailyKWh || 0);
        }
        return Number(b.estimatedDailyCost || 0) - Number(a.estimatedDailyCost || 0);
      });
  }, [debouncedSearchTerm, roomNameById, selectedRoomFilter, sortMode, userAppliances]);

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
  const handleUpdateAppliance = useCallback(async (id, updatedFields) => {
    const existing = userAppliancesRef.current.find((ua) => ua.id === id);
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
  }, [API_BASE, fetchWithAuth]);

  // Delete appliance
  const handleRemoveAppliance = useCallback(async (id) => {
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
  }, [API_BASE, fetchWithAuth]);

  const handleSave = () => {
    setSaveMessage("All appliance changes are saved.");
    window.setTimeout(() => setSaveMessage(""), 2200);
  };

  const handleExportCsv = () => {
    const headers = [
      "Name",
      "Base appliance",
      "Usage type",
      "Room",
      "Hours per day",
      "Uses per day",
      "Daily kWh",
      "Estimated daily cost",
    ];

    const rows = userAppliances.map((appliance) => [
      appliance.customName || appliance.applianceName || "Appliance",
      appliance.applianceName || "",
      appliance.usageType || "",
      appliance.roomName || "Unassigned",
      appliance.hoursPerDay ?? "",
      appliance.usesPerDay ?? "",
      Number(appliance.dailyKWh || 0).toFixed(2),
      Number(appliance.estimatedDailyCost || 0).toFixed(2),
    ]);

    const csv = [headers, ...rows]
      .map((row) => row.map(csvEscape).join(","))
      .join("\n");

    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "currently-appliances.csv";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  if (loading) {
    return (
      <div className="myappliances-container private-page">
        <HeaderUser activePage="myappliances" />
        <div className="myappliances-content private-content">
          <h1 className="myappliances-title private-title">My Appliances</h1>
          <p className="myappliances-subtitle private-subtitle">manage appliances and define how they use energy.</p>
          <p>Loading your appliances...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="myappliances-container private-page">
      <HeaderUser activePage="myappliances" />

      <div className="myappliances-content private-content">
        <h1 className="myappliances-title private-title">My Appliances</h1>
        <p className="myappliances-subtitle private-subtitle">manage appliances and define how they use energy.</p>

        {error && (
          <div className="myappliances-error">
            {error}
          </div>
        )}

        <div className="myappliances-overview">
          <div className="overview-pill">
            <span className="overview-pill__value">{userAppliances.length}</span>
            <span className="overview-pill__label">appliances tracked</span>
          </div>
          <div className="overview-pill">
            <span className="overview-pill__value">{assignedAppliancesCount}</span>
            <span className="overview-pill__label">assigned to rooms</span>
          </div>
          <div className="overview-pill">
            <span className="overview-pill__value">{userAppliances.length - assignedAppliancesCount}</span>
            <span className="overview-pill__label">still unassigned</span>
          </div>
        </div>

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

          <div className="sort-filter">
            <select
              className="room-select"
              value={sortMode}
              onChange={(e) => setSortMode(e.target.value)}
              aria-label="Sort appliances"
            >
              <option value="cost-desc">Highest daily cost</option>
              <option value="kwh-desc">Highest daily kWh</option>
              <option value="name-asc">Name A to Z</option>
              <option value="room-asc">Room A to Z</option>
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
                {userAppliances.length === 0
                  ? 'No appliances added yet. Click "+ Add Appliance" to start tracking usage.'
                  : 'No appliances match these filters. Try another search, room, or sort option.'}
              </p>
            )}
          </div>
        </div>

        {/* Action button under the grey box */}
        <div className="actions">
          {saveMessage && <span className="save-message">{saveMessage}</span>}
          <button
            className="save-btn"
            onClick={handleSave}
          >
            Save
          </button>
          <button
            className="export-btn"
            onClick={handleExportCsv}
            disabled={userAppliances.length === 0}
          >
            Export CSV
          </button>
          <button
            className="add-btn"
            onClick={() => {
              setCatalogueSearch("");
              setIsAddModalOpen(true);
            }}
          >
            + Add Appliance
          </button>
        </div>
      </div>

      {/* Add-appliance modal */}
      {isAddModalOpen && (
        <div className="modal-backdrop">
          <div className="modal">
            <div className="modal-header">
              <div>
                <h2 className="modal-title">Add Appliance</h2>
                <p className="modal-subtitle">Pick a catalogue item and Currently will use its default energy profile.</p>
              </div>
              <button
                type="button"
                className="modal-close"
                onClick={() => setIsAddModalOpen(false)}
                aria-label="Close add appliance"
              >
                x
              </button>
            </div>

            <div className="modal-grid">
              <div className="modal-picker">
                <label className="modal-label" htmlFor="catalogue-search">Search catalogue</label>
                <input
                  id="catalogue-search"
                  className="modal-search"
                  type="search"
                  value={catalogueSearch}
                  onChange={(e) => setCatalogueSearch(e.target.value)}
                  placeholder="Search by appliance or room type"
                />

                <label className="modal-label" htmlFor="catalogue-select">Appliance</label>
                <select
                  id="catalogue-select"
                  className="modal-select modal-select--large"
                  value={selectedBaseName}
                  onChange={(e) => setSelectedBaseName(e.target.value)}
                  size={Math.min(10, Math.max(5, catalogue.length))}
                >
                  {[...filteredCatalogueGroups.entries()].map(([category, appliances]) => (
                    <optgroup key={category} label={category}>
                      {appliances.map((appliance) => (
                        <option key={appliance.name} value={appliance.name}>
                          {appliance.name}
                        </option>
                      ))}
                    </optgroup>
                  ))}
                </select>
                {filteredCatalogueGroups.size === 0 && (
                  <div className="modal-empty">No matching appliances</div>
                )}
              </div>

              <div className="modal-preview">
                {selectedBaseAppliance ? (
                  <>
                    <span className="modal-preview__eyebrow">{selectedBaseAppliance.category}</span>
                    <h3>{selectedBaseAppliance.name}</h3>
                    <div className="modal-preview__rows">
                      <div>
                        <span>Usage model</span>
                        <strong>{formatUsageLabel(selectedBaseAppliance)}</strong>
                      </div>
                      <div>
                        <span>Default assumption</span>
                        <strong>{formatDefaultUsage(selectedBaseAppliance)}</strong>
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="modal-empty">Select an appliance to preview its defaults.</div>
                )}
              </div>
            </div>

            <div className="modal-actions">
              <button
                type="button"
                className="modal-btn modal-btn--primary"
                onClick={handleConfirmAdd}
                disabled={!selectedBaseAppliance}
              >
                Add
              </button>
              <button
                type="button"
                className="modal-btn modal-btn--secondary"
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
