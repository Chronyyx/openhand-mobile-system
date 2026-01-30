// mobile-app/services/notification.service.ts
import apiClient from './api.client';

export type NotificationType = 'REGISTRATION_CONFIRMATION' | 'REMINDER' | 'CANCELLATION' | 'EMPLOYEE_REGISTERED_PARTICIPANT';

export type Notification = {
    id: number;
    eventId: number;
    eventTitle: string;
    notificationType: NotificationType;
    textContent: string;
    isRead: boolean;
    createdAt: string;
    readAt: string | null;
    eventStartDateTime: string | null;
    participantName: string | null;
};

export async function getNotifications(): Promise<Notification[]> {
    const response = await apiClient.get<Notification[]>('/notifications');
    return response.data;
}

export async function getUnreadCount(): Promise<number> {
    const response = await apiClient.get<{ count: number }>('/notifications/unread-count');
    return response.data.count;
}

export async function markAsRead(notificationId: number): Promise<Notification> {
    const response = await apiClient.put<Notification>(`/notifications/${notificationId}/read`);
    return response.data;
}

export async function markAllAsRead(): Promise<void> {
    await apiClient.put('/notifications/read-all');
}

export async function deleteNotification(notificationId: number): Promise<void> {
    await apiClient.delete(`/notifications/${notificationId}`);
}

