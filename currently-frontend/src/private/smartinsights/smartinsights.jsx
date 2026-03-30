import React, { useMemo, useState, useCallback } from 'react';
import '../watchyourwatts/css/smartinsights.css';

export default function SmartInsights({ appliances = [], pricePerKwh = 0.3 }) {
  const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

  const [insights, setInsights] = useState([]);
  const [runId, setRunId] = useState(null);
  const [hasMore, setHasMore] = useState(false);
  const [stopReason, setStopReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');

  const hasAppliances = useMemo(() => (appliances || []).length > 0, [appliances]);

  const fetchWithAuth = useCallback(async (url, options = {}) => {
    const token = localStorage.getItem('token');
    if (!token) throw new Error('No authentication token found. Please log in again.');

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

  const handleGenerate = async () => {
    if (!hasAppliances) return;

    try {
      setLoading(true);
      setError('');
      setStopReason('');

      // Send the latest tariff value so insight calculations stay aligned with the UI.
      const res = await fetchWithAuth(`${API_BASE}/api/insights/generate`, {
        method: 'POST',
        body: JSON.stringify({
          pricePerKwh: Number(pricePerKwh) || 0.3,
        }),
      });

      const data = await res.json();
      setInsights(Array.isArray(data?.insights) ? data.insights : []);
      setRunId(data?.runId || null);
      setHasMore(Boolean(data?.hasMore));
      setStopReason(data?.stopReason || '');
    } catch (e) {
      console.error(e);
      setError('Failed to generate insights.');
      setInsights([]);
      setRunId(null);
      setHasMore(false);
      setStopReason('');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateMore = async () => {
    if (!runId || !hasMore) return;

    try {
      setLoadingMore(true);
      setError('');

      const res = await fetchWithAuth(`${API_BASE}/api/insights/${runId}/more`, {
        method: 'POST',
      });

      const data = await res.json();
      const next = Array.isArray(data?.insights) ? data.insights : [];
      setInsights((prev) => [...prev, ...next]);
      setHasMore(Boolean(data?.hasMore));
      setStopReason(data?.stopReason || '');
    } catch (e) {
      console.error(e);
      setError('Failed to generate more insights.');
    } finally {
      setLoadingMore(false);
    }
  };

  if (!hasAppliances) {
    return (
      <div className="smartinsights-empty">
        <h3>No appliance data yet</h3>
        <p>Add appliances in My Appliances to generate personalised energy insights.</p>
      </div>
    );
  }

  return (
    <div className="smartinsights-wrap">
      <div className="smartinsights-controls">
        <p className="smartinsights-label">Generate recommendations from your full energy profile.</p>

        <div className="smartinsights-control-row">
          <button
            type="button"
            className="smartinsights-btn"
            onClick={handleGenerate}
            disabled={loading}
          >
            {loading ? 'Generating insights...' : 'Generate Insights'}
          </button>

          {insights.length > 0 && (
            <button
              type="button"
              className="smartinsights-btn"
              onClick={handleGenerateMore}
              disabled={!hasMore || loadingMore}
            >
              {loadingMore ? 'Generating more...' : 'Generate More'}
            </button>
          )}
        </div>
      </div>

      {error && <div className="smartinsights-error">{error}</div>}
      {!error && stopReason && insights.length > 0 && <div className="smartinsights-note">{stopReason}</div>}

      {!loading && !error && insights.length > 0 && (
        <div className="smartinsights-grid">
          {insights.map((insight, idx) => (
            <article key={`${insight.title}-${idx}`} className="smartinsights-card">
              <h3 className="smartinsights-title">{insight.title}</h3>
              <p className="smartinsights-reasoning">{insight.reasoning}</p>

              <div className="smartinsights-section">
                <span className="smartinsights-k">Recommended action</span>
                <p>{insight.action}</p>
              </div>

              <div className="smartinsights-meta">
                <span>Impact: EUR {Number(insight.impactWeekly || 0).toFixed(2)}/week</span>
                <span>EUR {Number(insight.impactMonthly || 0).toFixed(2)}/month</span>
              </div>

              <div className="smartinsights-meta">
                <span>Confidence: {insight.confidence}</span>
                <span>Category: {insight.category}</span>
              </div>

              {Array.isArray(insight.references) && insight.references.length > 0 && (
                <div className="smartinsights-section">
                  <span className="smartinsights-k">References</span>
                  <p>{insight.references.join(' | ')}</p>
                </div>
              )}
            </article>
          ))}
        </div>
      )}

      {!loading && !error && insights.length === 0 && (
        <div className="smartinsights-empty smartinsights-empty--subtle">
          <p>Click Generate Insights to get recommendations.</p>
        </div>
      )}
    </div>
  );
}
