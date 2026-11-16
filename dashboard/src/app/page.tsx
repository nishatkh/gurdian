"use client";

import React, { useCallback, useEffect, useState } from "react";
import {
  Activity,
  BookOpen,
  CheckCircle2,
  Cpu,
  FileWarning,
  Lock,
  LogOut,
  RefreshCw,
  Shield,
  Users,
} from "lucide-react";
import {
  AdminIncident,
  AdminUser,
  AIMonitoring,
  apiFetch,
  AuditLogEntry,
  DashboardResponse,
  KnowledgeDoc,
} from "@/lib/api";

export const dynamic = "force-dynamic";

type TabId = "overview" | "incidents" | "users" | "knowledge" | "monitoring" | "audit";

export default function AdminDashboardPage() {
  const [token, setToken] = useState<string>("");
  const [email, setEmail] = useState<string>("admin@guardianai.app");
  const [password, setPassword] = useState<string>("");
  const [authError, setAuthError] = useState<string>("");
  const [tab, setTab] = useState<TabId>("overview");
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>("");

  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [incidents, setIncidents] = useState<AdminIncident[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [knowledge, setKnowledge] = useState<KnowledgeDoc[]>([]);
  const [monitoring, setMonitoring] = useState<AIMonitoring | null>(null);
  const [auditLogs, setAuditLogs] = useState<AuditLogEntry[]>([]);

  const [selectedIncident, setSelectedIncident] = useState<AdminIncident | null>(null);
  const [reviewDecision, setReviewDecision] = useState<string>("under_review");
  const [reviewNotes, setReviewNotes] = useState<string>("");

  const [newDocId, setNewDocId] = useState("");
  const [newDocTitle, setNewDocTitle] = useState("");
  const [newDocPublisher, setNewDocPublisher] = useState("");
  const [newDocUrl, setNewDocUrl] = useState("");
  const [newDocContent, setNewDocContent] = useState("");

  useEffect(() => {
    const saved = window.sessionStorage.getItem("guardian_admin_token");
    if (saved) setToken(saved);
  }, []);

  const loadAllData = useCallback(async (authToken: string) => {
    setLoading(true);
    setError("");
    try {
      const [d, inc, kb, mon, logs] = await Promise.all([
        apiFetch<DashboardResponse>("/admin/dashboard", {}, authToken),
        apiFetch<AdminIncident[]>("/admin/incidents", {}, authToken),
        apiFetch<KnowledgeDoc[]>("/admin/knowledge", {}, authToken),
        apiFetch<AIMonitoring>("/admin/ai-monitoring", {}, authToken),
        apiFetch<AuditLogEntry[]>("/admin/audit-logs", {}, authToken),
      ]);
      setDashboard(d);
      setIncidents(inc);
      setKnowledge(kb);
      setMonitoring(mon);
      setAuditLogs(logs);
      try {
        const u = await apiFetch<AdminUser[]>("/admin/users", {}, authToken);
        setUsers(u);
      } catch {
        // Reviewers do not have admin-only /admin/users access
        setUsers([]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load backend data");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (token) {
      loadAllData(token);
    }
  }, [token, loadAllData]);

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setAuthError("");
    try {
      const res = await apiFetch<{ access_token: string; role: string }>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      if (res.role !== "admin" && res.role !== "reviewer") {
        setAuthError("Access denied: Reviewer or Admin role is required.");
        return;
      }
      window.sessionStorage.setItem("guardian_admin_token", res.access_token);
      setToken(res.access_token);
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : "Authentication failed");
    }
  }

  function handleLogout() {
    window.sessionStorage.removeItem("guardian_admin_token");
    setToken("");
    setDashboard(null);
  }

  async function submitReview(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedIncident || !reviewNotes.trim()) return;
    await apiFetch(
      `/admin/incidents/${selectedIncident.id}/review`,
      {
        method: "POST",
        body: JSON.stringify({ decision: reviewDecision, notes: reviewNotes }),
      },
      token
    );
    setReviewNotes("");
    setSelectedIncident(null);
    await loadAllData(token);
  }

  async function toggleDoc(id: string) {
    await apiFetch(`/admin/knowledge/${id}/toggle`, { method: "POST" }, token);
    await loadAllData(token);
  }

  async function reindexDoc(id: string) {
    await apiFetch(`/admin/knowledge/${id}/reindex`, { method: "POST" }, token);
    await loadAllData(token);
  }

  async function addKnowledgeDoc(e: React.FormEvent) {
    e.preventDefault();
    await apiFetch(
      "/admin/knowledge",
      {
        method: "POST",
        body: JSON.stringify({
          id: newDocId,
          title: newDocTitle,
          publisher: newDocPublisher,
          source_url: newDocUrl,
          last_verified: new Date().toISOString().slice(0, 10),
          language: "bilingual",
          jurisdiction: "BD",
          content: newDocContent,
        }),
      },
      token
    );
    setNewDocId("");
    setNewDocTitle("");
    setNewDocPublisher("");
    setNewDocUrl("");
    setNewDocContent("");
    await loadAllData(token);
  }

  async function updateUserRole(userId: string, role: "user" | "reviewer" | "admin") {
    await apiFetch(
      `/admin/users/${userId}/role`,
      { method: "PATCH", body: JSON.stringify({ role }) },
      token
    );
    await loadAllData(token);
  }

  if (!token) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#050505] p-6">
        <div className="w-full max-w-md rounded-2xl border border-[#262626] bg-[#171717] p-8 shadow-2xl">
          <div className="mb-6 flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#B6FF55]/15 text-[#B6FF55]">
              <Shield className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-xl font-semibold text-[#F5F7F2]">Guardian AI Console</h1>
              <p className="text-xs text-[#9CA3AF]">Reviewer & Administration Portal</p>
            </div>
          </div>

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="mb-1 block text-xs text-[#9CA3AF]">Reviewer / Admin Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full rounded-xl border border-[#262626] bg-[#111111] px-4 py-3 text-sm text-[#F5F7F2] focus:border-[#B6FF55] focus:outline-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-[#9CA3AF]">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="Enter reviewer password"
                className="w-full rounded-xl border border-[#262626] bg-[#111111] px-4 py-3 text-sm text-[#F5F7F2] focus:border-[#B6FF55] focus:outline-none"
              />
            </div>
            {authError && (
              <p className="rounded-lg bg-red-500/10 p-3 text-xs text-red-400">{authError}</p>
            )}
            <button
              type="submit"
              className="w-full rounded-xl bg-[#B6FF55] py-3 text-sm font-semibold text-[#050505] transition hover:brightness-105"
            >
              Sign In to Admin Console
            </button>
          </form>
        </div>
      </main>
    );
  }

  const navItems: Array<{ id: TabId; label: string; icon: React.ReactNode }> = [
    { id: "overview", label: "Overview", icon: <Activity className="h-4 w-4" /> },
    { id: "incidents", label: "Incident Review", icon: <FileWarning className="h-4 w-4" /> },
    { id: "users", label: "Users & Roles", icon: <Users className="h-4 w-4" /> },
    { id: "knowledge", label: "Knowledge Base", icon: <BookOpen className="h-4 w-4" /> },
    { id: "monitoring", label: "AI & ML Monitor", icon: <Cpu className="h-4 w-4" /> },
    { id: "audit", label: "Audit Logs", icon: <Lock className="h-4 w-4" /> },
  ];

  return (
    <div className="flex min-h-screen bg-[#050505] text-[#F5F7F2]">
      <aside className="hidden w-64 flex-col border-r border-[#262626] bg-[#111111] p-5 md:flex">
        <div className="mb-8 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#B6FF55] text-[#050505]">
            <Shield className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-bold tracking-tight">Guardian AI</p>
            <p className="text-[11px] text-[#9CA3AF]">Admin & Review</p>
          </div>
        </div>

        <nav className="flex-1 space-y-1.5">
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => setTab(item.id)}
              className={`flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm transition ${
                tab === item.id
                  ? "bg-[#B6FF55]/15 font-semibold text-[#B6FF55]"
                  : "text-[#9CA3AF] hover:bg-[#171717] hover:text-[#F5F7F2]"
              }`}
            >
              {item.icon}
              {item.label}
            </button>
          ))}
        </nav>

        <button
          onClick={handleLogout}
          className="mt-6 flex items-center gap-2 rounded-xl border border-[#262626] px-3.5 py-2.5 text-xs text-[#9CA3AF] hover:border-red-500/40 hover:text-red-400"
        >
          <LogOut className="h-4 w-4" /> Sign Out
        </button>
      </aside>

      <main className="flex-1 p-5 md:p-8">
        <header className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">
              {navItems.find((n) => n.id === tab)?.label}
            </h1>
            <p className="text-xs text-[#9CA3AF]">
              Live database records • Reviewer decisions never auto-submit police reports
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => loadAllData(token)}
              disabled={loading}
              className="flex items-center gap-2 rounded-xl border border-[#262626] bg-[#171717] px-4 py-2 text-xs font-medium hover:border-[#B6FF55]/50"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
              Refresh
            </button>
          </div>
        </header>

        <div className="mb-5 flex gap-2 overflow-x-auto md:hidden">
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => setTab(item.id)}
              className={`whitespace-nowrap rounded-lg px-3 py-1.5 text-xs ${
                tab === item.id ? "bg-[#B6FF55] font-semibold text-[#050505]" : "bg-[#171717] text-[#9CA3AF]"
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>

        {error && (
          <div className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
            {error}
          </div>
        )}

        {tab === "overview" && dashboard && (
          <div className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {[
                { label: "Total Incidents", val: dashboard.metrics.total_incidents },
                { label: "Active Incidents", val: dashboard.metrics.active_incidents },
                { label: "Awaiting Review", val: dashboard.metrics.awaiting_review },
                { label: "Closed / Approved", val: dashboard.metrics.closed_incidents },
              ].map((kpi) => (
                <div key={kpi.label} className="rounded-2xl border border-[#262626] bg-[#171717] p-5">
                  <p className="text-xs text-[#9CA3AF]">{kpi.label}</p>
                  <p className="mt-2 text-3xl font-bold text-[#B6FF55]">{kpi.val}</p>
                </div>
              ))}
            </div>

            <div className="grid gap-6 lg:grid-cols-2">
              <div className="rounded-2xl border border-[#262626] bg-[#171717] p-5">
                <h2 className="mb-4 text-sm font-semibold">Incident Status Breakdown</h2>
                {Object.keys(dashboard.status_breakdown).length === 0 ? (
                  <p className="text-xs text-[#9CA3AF]">No incidents recorded yet.</p>
                ) : (
                  <div className="space-y-3">
                    {Object.entries(dashboard.status_breakdown).map(([status, count]) => (
                      <div key={status}>
                        <div className="mb-1 flex justify-between text-xs">
                          <span className="capitalize text-[#F5F7F2]">{status.replace(/_/g, " ")}</span>
                          <span className="text-[#B6FF55]">{count}</span>
                        </div>
                        <div className="h-2 rounded-full bg-[#111111]">
                          <div
                            className="h-2 rounded-full bg-[#B6FF55]"
                            style={{
                              width: `${Math.min(
                                100,
                                (count / Math.max(1, dashboard.metrics.total_incidents)) * 100
                              )}%`,
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="rounded-2xl border border-[#262626] bg-[#171717] p-5">
                <h2 className="mb-4 text-sm font-semibold">System & Synchronization Health</h2>
                <div className="space-y-2 text-xs">
                  <div className="flex justify-between rounded-xl bg-[#111111] p-3">
                    <span className="text-[#9CA3AF]">Database Connection</span>
                    <span className="font-medium text-[#B6FF55]">{dashboard.system_health.database}</span>
                  </div>
                  <div className="flex justify-between rounded-xl bg-[#111111] p-3">
                    <span className="text-[#9CA3AF]">ML Model Version</span>
                    <span className="font-mono text-[#F5F7F2]">
                      {dashboard.system_health.ml_model_version}
                    </span>
                  </div>
                  <div className="flex justify-between rounded-xl bg-[#111111] p-3">
                    <span className="text-[#9CA3AF]">Outbox Events Synced</span>
                    <span className="font-medium text-[#F5F7F2]">
                      {dashboard.metrics.total_synced_outbox_events}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {tab === "incidents" && (
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="space-y-3 lg:col-span-2">
              {incidents.length === 0 ? (
                <div className="rounded-2xl border border-[#262626] bg-[#171717] p-8 text-center text-sm text-[#9CA3AF]">
                  No incidents in the database yet.
                </div>
              ) : (
                incidents.map((inc) => (
                  <button
                    key={inc.id}
                    onClick={() => {
                      setSelectedIncident(inc);
                      setReviewDecision(
                        inc.status === "submitted_for_review" ? "under_review" : "approved"
                      );
                    }}
                    className={`w-full rounded-2xl border p-4 text-left transition ${
                      selectedIncident?.id === inc.id
                        ? "border-[#B6FF55] bg-[#1E2D0F]/40"
                        : "border-[#262626] bg-[#171717] hover:border-[#3A3A3A]"
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-semibold text-[#F5F7F2]">{inc.title}</span>
                      <span className="rounded-full bg-[#B6FF55]/15 px-2.5 py-0.5 text-[11px] font-medium text-[#B6FF55]">
                        {inc.status}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-[#9CA3AF]">
                      {inc.category} • {inc.device_brand || "Device N/A"} {inc.device_model || ""} • IMEI:{" "}
                      {inc.imei_masked || "Not provided"}
                    </p>
                    {inc.missing_fields.length > 0 && (
                      <p className="mt-2 text-[11px] text-amber-400">
                        Missing: {inc.missing_fields.join(", ")}
                      </p>
                    )}
                  </button>
                ))
              )}
            </div>

            <div className="rounded-2xl border border-[#262626] bg-[#171717] p-5">
              {selectedIncident ? (
                <form onSubmit={submitReview} className="space-y-4">
                  <h2 className="text-base font-semibold">Internal Review Decision</h2>
                  <p className="text-xs text-[#9CA3AF]">{selectedIncident.title}</p>
                  {selectedIncident.ai_summary && (
                    <div className="rounded-xl bg-[#111111] p-3 text-xs text-[#D1D5DB]">
                      <span className="mb-1 block font-semibold text-[#B6FF55]">AI Summary</span>
                      {selectedIncident.ai_summary}
                    </div>
                  )}
                  <div>
                    <label className="mb-1 block text-xs text-[#9CA3AF]">Decision</label>
                    <select
                      value={reviewDecision}
                      onChange={(e) => setReviewDecision(e.target.value)}
                      className="w-full rounded-xl border border-[#262626] bg-[#111111] px-3 py-2.5 text-sm"
                    >
                      <option value="under_review">Mark Under Review</option>
                      <option value="additional_info_requested">Request Additional Info</option>
                      <option value="approved">Approve Internal Draft</option>
                      <option value="closed">Close Internal Review</option>
                    </select>
                  </div>
                  <div>
                    <label className="mb-1 block text-xs text-[#9CA3AF]">Reviewer Notes</label>
                    <textarea
                      value={reviewNotes}
                      onChange={(e) => setReviewNotes(e.target.value)}
                      required
                      rows={3}
                      placeholder="Guidance or correction requested..."
                      className="w-full rounded-xl border border-[#262626] bg-[#111111] p-3 text-sm"
                    />
                  </div>
                  <button
                    type="submit"
                    className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#B6FF55] py-2.5 text-sm font-semibold text-[#050505]"
                  >
                    <CheckCircle2 className="h-4 w-4" /> Save Review Decision
                  </button>
                </form>
              ) : (
                <p className="text-xs text-[#9CA3AF]">
                  Select an incident from the queue to inspect AI summary, missing fields, and record a review decision.
                </p>
              )}
            </div>
          </div>
        )}

        {tab === "users" && (
          <div className="rounded-2xl border border-[#262626] bg-[#171717] p-5">
            {users.length === 0 ? (
              <p className="text-sm text-[#9CA3AF]">Admin role required to view user directory.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="border-b border-[#262626] text-[#9CA3AF]">
                    <tr>
                      <th className="py-2">Name</th>
                      <th className="py-2">Email</th>
                      <th className="py-2">Role</th>
                      <th className="py-2">Language</th>
                      <th className="py-2">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#262626]">
                    {users.map((u) => (
                      <tr key={u.id}>
                        <td className="py-3 font-medium">{u.full_name}</td>
                        <td className="py-3 text-[#9CA3AF]">{u.email}</td>
                        <td className="py-3">
                          <span className="rounded bg-[#202020] px-2 py-0.5 text-[#B6FF55]">
                            {u.role}
                          </span>
                        </td>
                        <td className="py-3 uppercase">{u.preferred_language}</td>
                        <td className="py-3">
                          <select
                            value={u.role}
                            onChange={(e) =>
                              updateUserRole(u.id, e.target.value as "user" | "reviewer" | "admin")
                            }
                            className="rounded-lg border border-[#262626] bg-[#111111] px-2 py-1 text-xs"
                          >
                            <option value="user">user</option>
                            <option value="reviewer">reviewer</option>
                            <option value="admin">admin</option>
                          </select>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {tab === "knowledge" && (
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="space-y-3 lg:col-span-2">
              {knowledge.map((doc) => (
                <div
                  key={doc.id}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#262626] bg-[#171717] p-4"
                >
                  <div>
                    <p className="text-sm font-semibold">{doc.title}</p>
                    <p className="text-xs text-[#9CA3AF]">
                      {doc.publisher} • Verified {doc.last_verified} • {doc.jurisdiction}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => reindexDoc(doc.id)}
                      className="rounded-lg border border-[#262626] bg-[#202020] px-3 py-1.5 text-xs hover:border-[#B6FF55]"
                    >
                      Re-index
                    </button>
                    <button
                      onClick={() => toggleDoc(doc.id)}
                      className={`rounded-lg px-3 py-1.5 text-xs font-medium ${
                        doc.is_active
                          ? "bg-[#B6FF55]/15 text-[#B6FF55]"
                          : "bg-red-500/15 text-red-400"
                      }`}
                    >
                      {doc.is_active ? "Active" : "Disabled"}
                    </button>
                  </div>
                </div>
              ))}
            </div>
            <form
              onSubmit={addKnowledgeDoc}
              className="space-y-3 rounded-2xl border border-[#262626] bg-[#171717] p-5"
            >
              <h2 className="text-sm font-semibold">Add Verified Source</h2>
              <input
                value={newDocId}
                onChange={(e) => setNewDocId(e.target.value)}
                placeholder="Doc ID (e.g. btrc_notice_08)"
                required
                className="w-full rounded-xl border border-[#262626] bg-[#111111] px-3 py-2 text-xs"
              />
              <input
                value={newDocTitle}
                onChange={(e) => setNewDocTitle(e.target.value)}
                placeholder="Document Title"
                required
                className="w-full rounded-xl border border-[#262626] bg-[#111111] px-3 py-2 text-xs"
              />
              <input
                value={newDocPublisher}
                onChange={(e) => setNewDocPublisher(e.target.value)}
                placeholder="Official Publisher"
                required
                className="w-full rounded-xl border border-[#262626] bg-[#111111] px-3 py-2 text-xs"
              />
              <input
                value={newDocUrl}
                onChange={(e) => setNewDocUrl(e.target.value)}
                placeholder="https://..."
                required
                className="w-full rounded-xl border border-[#262626] bg-[#111111] px-3 py-2 text-xs"
              />
              <textarea
                value={newDocContent}
                onChange={(e) => setNewDocContent(e.target.value)}
                placeholder="Verified guidance text..."
                rows={4}
                required
                className="w-full rounded-xl border border-[#262626] bg-[#111111] p-3 text-xs"
              />
              <button
                type="submit"
                className="w-full rounded-xl bg-[#B6FF55] py-2.5 text-xs font-semibold text-[#050505]"
              >
                Ingest & Index Document
              </button>
            </form>
          </div>
        )}

        {tab === "monitoring" && monitoring && (
          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-2xl border border-[#262626] bg-[#171717] p-5">
              <h2 className="mb-3 text-sm font-semibold">LLM Provider Telemetry</h2>
              <div className="space-y-2 text-xs">
                <p>Primary Provider: <span className="text-[#B6FF55]">{monitoring.llm_config.primary_provider}</span> ({monitoring.llm_config.groq_model})</p>
                <p>Fallback Provider: <span className="text-[#F5F7F2]">{monitoring.llm_config.fallback_provider}</span> ({monitoring.llm_config.gemini_model})</p>
                <p>Total Requests: {monitoring.ai_stats.total_requests}</p>
                <p>Fallback Activations: {monitoring.ai_stats.fallback_requests}</p>
                <p>Avg Latency: {monitoring.ai_stats.avg_latency_ms} ms</p>
                <p>Total Tokens: {monitoring.ai_stats.total_tokens}</p>
              </div>
            </div>
            <div className="rounded-2xl border border-[#262626] bg-[#171717] p-5">
              <h2 className="mb-3 text-sm font-semibold">ML Model Inference Stats</h2>
              <div className="space-y-2 text-xs">
                <p>Model Version: <span className="font-mono text-[#B6FF55]">{monitoring.ml_stats.model_version}</span></p>
                <p>Activity Classifier: {monitoring.ml_stats.best_activity_classifier}</p>
                <p>Anomaly Detector: {monitoring.ml_stats.best_anomaly_detector}</p>
                <p>Models Loaded: {monitoring.ml_stats.models_loaded ? "Yes" : "No"}</p>
                <p>Logged Inferences: {monitoring.ml_stats.total_inferences}</p>
              </div>
            </div>
          </div>
        )}

        {tab === "audit" && (
          <div className="rounded-2xl border border-[#262626] bg-[#171717] p-5">
            <div className="space-y-2 text-xs">
              {auditLogs.map((log) => (
                <div
                  key={log.id}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-[#111111] px-4 py-3"
                >
                  <div>
                    <span className="font-mono font-semibold text-[#B6FF55]">{log.action}</span>
                    <span className="ml-2 text-[#9CA3AF]">
                      by {log.actor_role} ({log.resource_type})
                    </span>
                  </div>
                  <span className="font-mono text-[11px] text-[#9CA3AF]">{log.created_at}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
