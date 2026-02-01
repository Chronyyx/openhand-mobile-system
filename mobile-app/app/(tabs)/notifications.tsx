// mobile-app/app/(tabs)/notifications.tsx
import React, { useCallback, useEffect, useState, useRef } from 'react';
import {
    ActivityIndicator,
    FlatList,
    RefreshControl,
    View,
    StyleSheet,
    Pressable,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from '@react-navigation/native';

import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import { NotificationCard } from '../../components/NotificationCard';
import { MenuLayout } from '../../components/menu-layout';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import {
    getNotifications,
    getUnreadCount,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    type Notification,
} from '../../services/notification.service';

export default function NotificationsScreen() {
    const { t } = useTranslation();
    const { user, isLoading } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const styles = getStyles(colorScheme);
    const palette = {
        primary: colorScheme === 'dark' ? '#6AA9FF' : '#0056A8',
        primarySurface: colorScheme === 'dark' ? '#1D2A3A' : '#E6F4FE',
        danger: colorScheme === 'dark' ? '#FFB4AB' : '#DC3545',
        dangerSurface: colorScheme === 'dark' ? '#3A1F1F' : '#FFE6E6',
        muted: colorScheme === 'dark' ? '#A0A7B1' : '#999999',
        iconMuted: colorScheme === 'dark' ? '#8B93A1' : '#CCCCCC',
        text: colorScheme === 'dark' ? '#ECEDEE' : '#333333',
    };

    // Ref to track last action time to prevent race conditions with focus reload
    const lastActionTimeRef = useRef<number>(0);

    // Data state
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Load notifications
    const loadNotifications = useCallback(async (isRefresh = false) => {
        console.log('[NotificationsScreen] loadNotifications - isLoading:', isLoading, 'user:', user ? 'Found' : 'NOT FOUND');
        if (!user || isLoading) {
            console.log('[NotificationsScreen] loadNotifications - SKIPPED (no user or still loading)');
            return;
        }

        console.log('[NotificationsScreen] loadNotifications - proceeding');
        try {
            setError(null);
            if (!isRefresh) setLoading(true);

            const [notifs, unread] = await Promise.all([
                getNotifications(),
                getUnreadCount(),
            ]);

            setNotifications(notifs);
            setUnreadCount(unread);
        } catch (err) {
            console.error('Failed to load notifications', err);
            setError(t('notifications.errors.loadFailed', 'Failed to load notifications'));
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [user, t, isLoading]);

    // Reload when screen comes into focus (but skip if we just performed an action)
    useFocusEffect(
        useCallback(() => {
            console.log('[NotificationsScreen] useFocusEffect - isLoading:', isLoading, 'user:', user ? 'Found' : 'NOT FOUND');
            if (user && !isLoading) {
                const timeSinceLastAction = Date.now() - lastActionTimeRef.current;
                // Only auto-reload if it's been more than 2 seconds since last action
                // This prevents race conditions with optimistic updates
                if (timeSinceLastAction > 2000) {
                    console.log('[NotificationsScreen] useFocusEffect - calling loadNotifications');
                    loadNotifications(true);
                } else {
                    console.log('[NotificationsScreen] useFocusEffect - SKIPPED (too soon after last action)');
                }
            }
        }, [user, loadNotifications, isLoading])
    );

    // Initial load
    useEffect(() => {
        console.log('[NotificationsScreen] useEffect - isLoading:', isLoading);
        if (!isLoading) {
            console.log('[NotificationsScreen] useEffect - calling loadNotifications');
            loadNotifications();
        }
    }, [loadNotifications, isLoading]);

    const onRefresh = useCallback(() => {
        setRefreshing(true);
        loadNotifications(true);
    }, [loadNotifications]);

    const handleMarkAsRead = useCallback(async (notification: Notification) => {
        try {
            if (!user) return;

            // Track action time to prevent race with focus reload
            lastActionTimeRef.current = Date.now();

            // Optimistically update UI first
            setNotifications(notifs =>
                notifs.map(n => (n.id === notification.id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n))
            );
            setUnreadCount(count => Math.max(0, count - 1));

            // Then update on server
            await markAsRead(notification.id);
            // Don't reload immediately - trust the optimistic update to avoid race conditions
        } catch (err) {
            console.error('Failed to mark notification as read', err);
            setError(t('notifications.errors.markReadFailed', 'Failed to mark as read'));
            // Reload on error to revert optimistic update
            await loadNotifications(true);
        }
    }, [user, t, loadNotifications]);

    const handleDeleteNotification = useCallback(async (notification: Notification) => {
        try {
            if (!user) return;

            // Track action time to prevent race with focus reload
            lastActionTimeRef.current = Date.now();

            // Optimistically remove from UI
            setNotifications(notifs => notifs.filter(n => n.id !== notification.id));
            if (!notification.isRead) {
                setUnreadCount(count => Math.max(0, count - 1));
            }

            // Delete on server
            await deleteNotification(notification.id);
            // Don't reload immediately - trust the optimistic update to avoid race conditions
        } catch (err) {
            console.error('Failed to delete notification', err);
            setError(t('notifications.errors.deleteFailed', 'Failed to delete notification'));
            // Reload on error to revert optimistic update
            await loadNotifications(true);
        }
    }, [user, t, loadNotifications]);

    const handleMarkAllAsRead = useCallback(async () => {
        try {
            if (!user) return;

            // Track action time to prevent race with focus reload
            lastActionTimeRef.current = Date.now();

            // Optimistically update UI first
            setNotifications(notifs =>
                notifs.map(n => ({ ...n, isRead: true, readAt: new Date().toISOString() }))
            );
            setUnreadCount(0);

            // Update on server
            await markAllAsRead();
            // Don't reload immediately - trust the optimistic update to avoid race conditions
        } catch (err) {
            console.error('Failed to mark all as read', err);
            setError(t('notifications.errors.markAllReadFailed', 'Failed to mark all as read'));
            // Reload on error to revert optimistic update
            await loadNotifications(true);
        }
    }, [user, t, loadNotifications]);

    let content = null;

    if (!user) {
        content = (
            <ThemedView style={styles.container}>
                <ThemedText style={styles.emptyText}>
                    {t('notifications.notAuthenticated', 'Please log in to view notifications')}
                </ThemedText>
            </ThemedView>
        );
    } else {
        content = (
            <ThemedView style={styles.container}>
                {/* Header with unread count and mark all read button */}
                <View style={styles.header}>
                    <View style={styles.headerTitle}>
                        <ThemedText type="title" style={styles.title}>
                            {t('notifications.title', 'Notifications')}
                        </ThemedText>
                        {unreadCount > 0 && (
                            <View style={styles.badge}>
                                <ThemedText style={styles.badgeText}>{unreadCount}</ThemedText>
                            </View>
                        )}
                    </View>

                    {unreadCount > 0 && (
                        <Pressable
                            onPress={handleMarkAllAsRead}
                            style={styles.markAllButton}
                        >
                            <Ionicons name="checkmark" size={20} color={palette.primary} />
                            <ThemedText style={styles.markAllText}>
                                {t('notifications.markAllAsRead', 'Mark all read')}
                            </ThemedText>
                        </Pressable>
                    )}
                </View>

                {/* Error message */}
                {error && (
                    <View style={styles.errorContainer}>
                        <Ionicons name="alert-circle" size={20} color={palette.danger} />
                        <ThemedText style={styles.errorText}>{error}</ThemedText>
                        <Pressable onPress={() => setError(null)}>
                            <Ionicons name="close" size={20} color={palette.danger} />
                        </Pressable>
                    </View>
                )}

                {/* Loading state */}
                {loading && !refreshing ? (
                    <View style={styles.centerContainer}>
                        <ActivityIndicator size="large" color={palette.primary} />
                        <ThemedText style={styles.loadingText}>
                            {t('notifications.loading', 'Loading notifications...')}
                        </ThemedText>
                    </View>
                ) : notifications.length === 0 ? (
                    /* Empty state */
                    <View style={styles.centerContainer}>
                        <Ionicons name="notifications-off-outline" size={64} color={palette.iconMuted} />
                        <ThemedText style={styles.emptyText}>
                            {t('notifications.empty', 'No notifications yet')}
                        </ThemedText>
                        <ThemedText style={styles.emptySubtext}>
                            {t('notifications.emptySubtext', 'You\'ll receive notifications when you register for events')}
                        </ThemedText>
                    </View>
                ) : (
                    /* Notifications list */
                    <FlatList
                        data={notifications}
                        keyExtractor={(item) => `${item.id}-${item.isRead}`}
                        extraData={notifications}
                        renderItem={({ item }) => (
                            <NotificationCard
                                notification={item}
                                onPress={() => {
                                    if (!item.isRead) {
                                        handleMarkAsRead(item);
                                    }
                                }}
                                onMarkAsRead={handleMarkAsRead}
                                onDelete={handleDeleteNotification}
                            />
                        )}
                        refreshControl={
                            <RefreshControl
                                refreshing={refreshing}
                                onRefresh={onRefresh}
                                colors={[palette.primary]}
                                enabled={true}
                            />
                        }
                        contentContainerStyle={styles.listContent}
                    />
                )}
            </ThemedView>
        );
    }

    return (
        <MenuLayout>
            {content}
        </MenuLayout>
    );
}

const getStyles = (scheme: 'light' | 'dark') => {
    const isDark = scheme === 'dark';
    return StyleSheet.create({
        container: {
            flex: 1,
            backgroundColor: isDark ? '#111418' : '#F5F7FB',
        },
        header: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            paddingHorizontal: 16,
            paddingTop: 40,
            paddingBottom: 12,
            backgroundColor: isDark ? '#151A20' : '#fff',
            borderBottomWidth: 1,
            borderBottomColor: isDark ? '#2A313B' : '#E0E0E0',
        },
        headerTitle: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 12,
        },
        title: {
            fontSize: 28,
            fontWeight: '700',
            color: isDark ? '#ECEDEE' : '#333',
        },
        badge: {
            backgroundColor: isDark ? '#B3261E' : '#DC3545',
            borderRadius: 12,
            paddingHorizontal: 8,
            paddingVertical: 4,
            minWidth: 24,
            justifyContent: 'center',
            alignItems: 'center',
        },
        badgeText: {
            color: '#fff',
            fontSize: 12,
            fontWeight: '700',
        },
        markAllButton: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 6,
            paddingHorizontal: 12,
            paddingVertical: 8,
            backgroundColor: isDark ? '#1D2A3A' : '#E6F4FE',
            borderRadius: 6,
        },
        markAllText: {
            fontSize: 12,
            color: isDark ? '#9FC3FF' : '#0056A8',
            fontWeight: '600',
        },
        errorContainer: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 12,
            paddingHorizontal: 16,
            paddingVertical: 12,
            backgroundColor: isDark ? '#3A1F1F' : '#FFE6E6',
            marginHorizontal: 8,
            marginTop: 8,
            borderRadius: 6,
        },
        errorText: {
            flex: 1,
            fontSize: 13,
            color: isDark ? '#FFB4AB' : '#DC3545',
        },
        centerContainer: {
            flex: 1,
            justifyContent: 'center',
            alignItems: 'center',
            paddingHorizontal: 24,
        },
        loadingText: {
            marginTop: 12,
            fontSize: 16,
            color: isDark ? '#A0A7B1' : '#666',
        },
        emptyText: {
            marginTop: 16,
            fontSize: 18,
            fontWeight: '600',
            color: isDark ? '#ECEDEE' : '#333',
            textAlign: 'center',
        },
        emptySubtext: {
            marginTop: 8,
            fontSize: 14,
            color: isDark ? '#A0A7B1' : '#999',
            textAlign: 'center',
        },
        listContent: {
            paddingVertical: 8,
            paddingHorizontal: 0,
        },
    });
};
