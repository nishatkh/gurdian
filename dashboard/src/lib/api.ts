const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8000/api/v1";

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  token?: string
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string> | undefined),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  const resp = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    cache: "no-store",
  });
  if (!resp.ok) {
    const errBody = await resp.json().catch(() => ({ detail: resp.statusText }));
    throw new Error(errBody.detail || `Request failed (${resp.status})`);
  }
  return (await resp.json()) as T;
}

export interface DashboardResponse {
  metrics: {
    total_incidents: number;
    active_incidents: number;
    awaiting_review: number;
    closed_incidents: number;
    total_synced_outbox_events: number;
  };
  status_breakdown: Record<string, number>;
  category_breakdown: Record<string, number>;
  recent_activity: Array<{
    id: string;
    incident_id: string;
    event_type: string;
    note: string;
    actor_role: string;
    timestamp: string;
  }>;
  system_health: {
    database: string;
    ml_models_loaded: boolean;
    ml_model_version: string;
  };
}

export interface AdminIncident {
  id: string;
  user_id: string;
  title: string;
  category: string;
  status: string;
  review_status: string;
  device_brand?: string;
  device_model?: string;
  imei_masked?: string;
  ai_summary?: string;
  missing_fields: string[];
  reviewer_notes?: string;
  occurred_at: string;
  updated_at: string;
}

export interface AdminUser {
  id: string;
  email: string;
  role: "user" | "reviewer" | "admin";
  is_active: boolean;
  full_name: string;
  preferred_language: string;
  created_at: string;
}

export interface KnowledgeDoc {
  id: string;
  title: string;
  publisher: string;
  source_url: string;
  last_verified: string;
  language: string;
  jurisdiction: string;
  trust_level: string;
  is_active: boolean;
}

export interface AIMonitoring {
  llm_config: {
    primary_provider: string;
    fallback_provider: string;
    groq_model: string;
    gemini_model: string;
    groq_configured: boolean;
    gemini_configured: boolean;
  };
  ai_stats: {
    total_requests: number;
    fallback_requests: number;
    avg_latency_ms: number;
    total_tokens: number;
  };
  ml_stats: {
    models_loaded: boolean;
    model_version: string;
    best_activity_classifier: string;
    best_anomaly_detector: string;
    total_inferences: number;
  };
}

export interface AuditLogEntry {
  id: string;
  actor_user_id?: string;
  actor_role: string;
  action: string;
  resource_type: string;
  resource_id?: string;
  details: Record<string, unknown>;
  created_at: string;
}
