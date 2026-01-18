import axios from 'axios';
import { useState, useCallback, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { webSocketService } from '../utils/websocket';
import { API_BASE } from '../utils/api';

export interface Notification {
    id: number;
    userId: number;
    eventId: number;
    type: 'REGISTRATION_CONFIRMATION' | 'REMINDER' | 'CANCELLATION' | 'EMPLOYEE_REGISTERED_PARTICIPANT' | 'EVENT_UPDATE';
    message: string;
    read: boolean;
    createdAt: string;
}

export function useNotifications() {
    const { user } = useAuth();
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);

    const fetchNotifications = useCallback(async () => {
        if (!user?.token) return;

        try {
            const response = await axios.get(`${API_BASE}/notifications`, {
                headers: {
                    'Authorization': `Bearer ${user.token}`
                }
            });

            // axios returns data directly in response.data
            const data = response.data;
            const list = Array.isArray(data) ? data : (data.content || []);

            setNotifications(list);
            setUnreadCount(list.filter((n: Notification) => !n.read).length);
        } catch (error) {
            console.error('Failed to fetch notifications', error);
        }
    }, [user]);

    const markAsRead = useCallback(async (id: number) => {
        if (!user?.token) return;
        try {
            await axios.put(`${API_BASE}/notifications/${id}/read`, {}, {
                headers: {
                    'Authorization': `Bearer ${user.token}`
                }
            });
            // Optimistic update
            setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch (e) {
            console.error(e);
        }
    }, [user]);

    const markAllAsRead = useCallback(async () => {
        if (!user?.token) return;
        const currentUnread = notifications.filter(n => !n.read).length;
        if (currentUnread === 0) return;

        // Optimistic update all
        setNotifications(prev => prev.map(n => ({ ...n, read: true })));
        setUnreadCount(0);

        try {
            await axios.put(`${API_BASE}/notifications/read-all`, {}, {
                headers: { 'Authorization': `Bearer ${user.token}` }
            });
        } catch (e) {
            console.error('Failed to mark all as read', e);
            // Revert on failure? Probably not worth the complexity for read status
        }
    }, [user, notifications]);

    useEffect(() => {
        let mounted = true;

        if (user?.token) {
            // Initial fetch
            fetchNotifications();

            // Connect WebSocket
            webSocketService.connect(user.token);

            // Subscribe to personal notifications
            const unsubscribe = webSocketService.subscribe(`/topic/notifications/${user.id}`, (message: any) => {
                if (!mounted) return;
                console.log('Received notification via WS', message);

                const incoming = message as Notification;

                setNotifications(prev => {
                    // Check if exists
                    const exists = prev.some(n => n.id === incoming.id);
                    if (exists) {
                        // Update existing (e.g. read status changed)
                        return prev.map(n => n.id === incoming.id ? incoming : n);
                    } else {
                        // Add new
                        return [incoming, ...prev];
                    }
                });

                // Recalculate unread count based on new state
                setNotifications(currentState => {
                    setUnreadCount(currentState.filter(n => !n.read).length);
                    return currentState;
                });
            });

            return () => {
                mounted = false;
                unsubscribe();
            };
        }

        return () => {
            mounted = false;
        };
    }, [user, fetchNotifications]);

    return {
        notifications,
        unreadCount,
        markAsRead,
        markAllAsRead,
        refresh: fetchNotifications
    };
}
