import { API_ROOT_URL } from '../api/client';
import { clearAuthToken, getAuthToken } from './auth';

export type MessageRole = 'USER' | 'ASSISTANT';

export interface ConsultationSession {
  id: string;
  personaType: string;
  readingRecordId: string | null;
  createdAt: string;
}

export interface ConsultationMessage {
  id: string;
  role: MessageRole;
  content: string;
  createdAt: string;
}

export class ConsultationApiError extends Error {
  status: number;

  constructor(status: number) {
    super(`consultation API failed: ${status}`);
    this.status = status;
  }
}

async function authedFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getAuthToken();
  if (!token) throw new ConsultationApiError(401);

  const res = await fetch(`${API_ROOT_URL}${path}`, {
    ...init,
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      Authorization: `Bearer ${token}`,
      ...init?.headers,
    },
  });
  if (!res.ok) {
    if (res.status === 401) clearAuthToken();
    throw new ConsultationApiError(res.status);
  }
  return res.json() as Promise<T>;
}

export function fetchConsultationSessions(): Promise<ConsultationSession[]> {
  return authedFetch<ConsultationSession[]>('/api/consultation/sessions');
}

export function createConsultationSession(readingRecordId: string): Promise<ConsultationSession> {
  return authedFetch<ConsultationSession>('/api/consultation/sessions', {
    method: 'POST',
    body: JSON.stringify({ readingRecordId }),
  });
}

export function fetchConsultationMessages(sessionId: string): Promise<ConsultationMessage[]> {
  return authedFetch<ConsultationMessage[]>(
    `/api/consultation/sessions/${encodeURIComponent(sessionId)}/messages`,
  );
}

// Throws ConsultationApiError(402) when out of credit, (502) when the LLM call failed
// (the spent credit is auto-refunded server-side in that case).
export function sendConsultationMessage(
  sessionId: string,
  content: string,
): Promise<ConsultationMessage> {
  return authedFetch<ConsultationMessage>(
    `/api/consultation/sessions/${encodeURIComponent(sessionId)}/messages`,
    { method: 'POST', body: JSON.stringify({ content }) },
  );
}
