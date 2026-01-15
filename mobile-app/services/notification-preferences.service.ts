import { API_BASE } from '../utils/api';

export type NotificationPreferenceCategory = 'CONFIRMATION' | 'REMINDER' | 'CANCELLATION';

export type NotificationPreference = {
  category: NotificationPreferenceCategory;
  enabled: boolean;
  isCritical: boolean;
};

export type NotificationPreferenceResponse = {
  memberId: number;
  preferences: NotificationPreference[];
};

export type NotificationPreferenceUpdate = {
  category: NotificationPreferenceCategory;
  enabled: boolean;
};

async function handleResponse<T>(res: Response, context: string): Promise<T> {
  if (!res.ok) {
    throw new Error(`HTTP ${res.status} while ${context}`);
  }
  return res.json() as Promise<T>;
}

export async function getNotificationPreferences(token: string): Promise<NotificationPreferenceResponse> {
  const url = `${API_BASE}/notifications/preferences`;
  const res = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  return handleResponse<NotificationPreferenceResponse>(res, 'fetching notification preferences');
}

export async function updateNotificationPreferences(
  token: string,
  preferences: NotificationPreferenceUpdate[]
): Promise<NotificationPreferenceResponse> {
  const url = `${API_BASE}/notifications/preferences`;
  const res = await fetch(url, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ preferences }),
  });
  return handleResponse<NotificationPreferenceResponse>(res, 'updating notification preferences');
}
