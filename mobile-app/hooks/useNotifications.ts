import axios from 'axios';
import { useState, useCallback, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { webSocketService } from '../utils/websocket';
import { type Notification, getNotifications as fetchNotificationsApi, markAsRead as markAsReadApi, markAllAsRead as markAllAsReadApi } from '../services/notification.service';

export function useNotifications() {
    const { user, isLoading } = useAuth();
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);

    const fetchNotifications = useCallback(async () => {
        if (!user?.token) return;

        try {
            // Use the service function instead of direct axios call to ensure type consistency
            const list = await fetchNotificationsApi();
            setNotifications(list);
            setUnreadCount(list.filter(n => !n.isRead).length);
        } catch (error) {
            console.error('Failed to fetch notifications', error);
        }
    }, [user]);

    const markAsRead = useCallback(async (id: number) => {
        if (!user?.token) return;
        try {
            await markAsReadApi(id);
            // Optimistic update
            setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch (e) {
            console.error(e);
        }
    }, [user]);

    const markAllAsRead = useCallback(async () => {
        if (!user?.token) return;
        const currentUnread = notifications.filter(n => !n.isRead).length;
        if (currentUnread === 0) return;

        // Optimistic update all
        setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
        setUnreadCount(0);

        try {
            await markAllAsReadApi();
        } catch (e) {
            console.error('Failed to mark all as read', e);
            // Revert on failure? Probably not worth the complexity for read status
        }
    }, [user, notifications]);


    useEffect(() => {
        let mounted = true;

        // Wait for auth to finish loading before making API calls
        if (!isLoading && user?.token) {
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
                    const nextState = exists
                        ? prev.map(n => n.id === incoming.id ? incoming : n)
                        : [incoming, ...prev];

                    // Update unread count based on NEW state
                    const count = nextState.filter(n => !n.isRead).length;
                    setUnreadCount(count);

                    return nextState;
                });
            });
        }

        return () => {
            mounted = false;
        };
        // Removed fetchNotifications from dependency array to avoid potential loops if the function identity is unstable.
        // We fundamentally depend on 'user' (specifically user.token and user.id).
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isLoading, user?.token, user?.id]);



    return {
        notifications,
        unreadCount,
        markAsRead,
        markAllAsRead,
        refresh: fetchNotifications
    };
}
