// mobile-app/services/notification.service.ts
import { API_BASE } from '../utils/api';

export type NotificationType = 'REGISTRATION_CONFIRMATION' | 'REMINDER' | 'CANCELLATION';

export type Notification = {
    id: number;
    eventId: number;
    eventTitle: string;
    notificationType: NotificationType;
    textContent: string;
    isRead: boolean;
    createdAt: string;
    readAt: string | null;
};

async function handleResponse<T>(res: Response, context: string): Promise<T> {
    if (!res.ok) {
        throw new Error(`HTTP ${res.status} while ${context}`);
    }
    return res.json() as Promise<T>;
}

export async function getNotifications(token: string): Promise<Notification[]> {
    const url = `${API_BASE}/notifications`;
    const res = await fetch(url, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
        }
    });
    return handleResponse<Notification[]>(res, 'fetching notifications');
}

export async function getUnreadCount(token: string): Promise<number> {
    const url = `${API_BASE}/notifications/unread-count`;
    const res = await fetch(url, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
        }
    });
    const data = await handleResponse<{ count: number }>(res, 'fetching unread count');
    return data.count;
}

export async function markAsRead(notificationId: number, token: string): Promise<Notification> {
    const url = `${API_BASE}/notifications/${notificationId}/read`;
    const res = await fetch(url, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        }
    });
    return handleResponse<Notification>(res, 'marking notification as read');
}

export async function markAllAsRead(token: string): Promise<void> {
    const url = `${API_BASE}/notifications/read-all`;
    const res = await fetch(url, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        }
    });
    if (!res.ok) {
        throw new Error(`HTTP ${res.status} while marking all as read`);
    }
}

export async function deleteNotification(notificationId: number, token: string): Promise<void> {
    const url = `${API_BASE}/notifications/${notificationId}`;
    const res = await fetch(url, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`,
        }
    });
    if (!res.ok) {
        throw new Error(`HTTP ${res.status} while deleting notification`);
    }
}
