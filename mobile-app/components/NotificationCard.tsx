// mobile-app/components/NotificationCard.tsx
import React, { useEffect, useState } from 'react';
import { View, Pressable, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { ThemedText } from './themed-text';
import { ThemedView } from './themed-view';
import { useTranslation } from 'react-i18next';
import { type Notification } from '../services/notification.service';
import { getTranslatedEventTitle } from '../utils/event-translations';
import { getTranslatedNotificationText } from '../utils/notification-translations';

interface NotificationCardProps {
    notification: Notification;
    onPress: (notification: Notification) => void;
    onMarkAsRead?: (notification: Notification) => void;
    onDelete?: (notification: Notification) => void;
}

function getNotificationIcon(type: string): string {
    switch (type) {
        case 'REGISTRATION_CONFIRMATION':
            return 'checkmark-circle';
        case 'CANCELLATION':
            return 'close-circle';
        case 'REMINDER':
            return 'alarm';
        default:
            return 'notifications';
    }
}

function getNotificationColor(type: string): string {
    switch (type) {
        case 'REGISTRATION_CONFIRMATION':
            return '#28a745';
        case 'CANCELLATION':
            return '#dc3545';
        case 'REMINDER':
            return '#ffc107';
        default:
            return '#0056A8';
    }
}

export function NotificationCard({
    notification,
    onPress,
    onMarkAsRead,
    onDelete,
}: NotificationCardProps) {
    const { t, i18n } = useTranslation();

    // Try to translate event title if it's a translation key (fallback to displayed title)
    const displayedEventTitle = getTranslatedEventTitle(
        { title: notification.eventTitle },
        t as any
    );

    // Translate notification text dynamically based on current language
    const displayedNotificationText = getTranslatedNotificationText(
        notification.notificationType,
        displayedEventTitle,
        i18n.language,
        notification.eventStartDateTime,
        notification.participantName
    );

    // Trigger re-render to keep relative time labels fresh
    const [now, setNow] = useState(Date.now());
    useEffect(() => {
        const intervalId = setInterval(() => setNow(Date.now()), 60_000);
        return () => clearInterval(intervalId);
    }, []);

    // Format relative time
    const createdDate = new Date(notification.createdAt);
    const createdTime = createdDate.getTime();
    const diffMs = Number.isNaN(createdTime) ? 0 : now - createdTime;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    let timeLabel = '';
    if (Number.isNaN(createdTime)) {
        timeLabel = notification.createdAt;
    } else if (diffMins < 1) {
        timeLabel = t('notifications.time.justNow', 'Just now');
    } else if (diffMins < 60) {
        timeLabel = t('notifications.time.minutesAgo', '{{count}} minutes ago', { count: diffMins });
    } else if (diffHours < 24) {
        timeLabel = t('notifications.time.hoursAgo', '{{count}} hours ago', { count: diffHours });
    } else if (diffDays < 7) {
        timeLabel = t('notifications.time.daysAgo', '{{count}} days ago', { count: diffDays });
    } else {
        timeLabel = createdDate.toLocaleDateString();
    }

    return (
        <Pressable onPress={() => onPress(notification)}>
            <ThemedView style={[styles.container, !notification.isRead && styles.unreadContainer]}>
                {/* Unread indicator dot */}


                {/* Left icon */}
                <View style={[styles.iconContainer, { backgroundColor: getNotificationColor(notification.notificationType) }]}>
                    <Ionicons
                        name={getNotificationIcon(notification.notificationType) as any}
                        size={24}
                        color="white"
                    />
                </View>

                {/* Content */}
                <View style={styles.contentContainer}>
                    <ThemedText
                        type="subtitle"
                        numberOfLines={1}
                        style={[
                            styles.eventTitle,
                            !notification.isRead && styles.eventTitleBold
                        ]}
                    >
                        {displayedEventTitle}
                    </ThemedText>
                    <ThemedText
                        numberOfLines={2}
                        style={[
                            styles.notificationText,
                            notification.isRead && styles.notificationTextRead
                        ]}
                    >
                        {displayedNotificationText}
                    </ThemedText>
                    <ThemedText style={styles.timeText}>
                        {timeLabel}
                    </ThemedText>
                </View>

                {/* Right actions area */}
                <View style={styles.actionsContainer}>
                    {!notification.isRead && (
                        <Pressable
                            onPress={() => onMarkAsRead?.(notification)}
                            style={styles.actionButton}
                        >
                            <Ionicons name="checkmark" size={20} color="#0056A8" />
                        </Pressable>
                    )}
                    <Pressable
                        onPress={() => onDelete?.(notification)}
                        style={styles.actionButton}
                    >
                        <Ionicons name="trash-outline" size={20} color="#dc3545" />
                    </Pressable>
                </View>

                {/* Unread indicator dot - Moved to top right absolute position */}
                {!notification.isRead && <View style={styles.unreadDot} />}
            </ThemedView>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        paddingHorizontal: 12,
        paddingVertical: 12,
        marginHorizontal: 8,
        marginVertical: 6,
        borderRadius: 8,
        alignItems: 'center',
        backgroundColor: '#F5F7FB',
        borderLeftWidth: 4,
        borderLeftColor: 'transparent',
        // Ensure relative positioning for the absolute dot
        position: 'relative',
    },
    unreadContainer: {
        backgroundColor: '#E6F4FE',
        borderLeftColor: '#0056A8',
    },
    unreadDot: {
        position: 'absolute',
        top: 8,
        right: 8,
        width: 8,
        height: 8,
        borderRadius: 4,
        backgroundColor: '#dc3545', // Red color
    },
    iconContainer: {
        width: 48,
        height: 48,
        borderRadius: 24,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 12,
    },
    contentContainer: {
        flex: 1,
        justifyContent: 'center',
        paddingRight: 16, // Add padding to avoid overlap with dot if needed, though dot is top-right
    },
    eventTitle: {
        fontSize: 14,
        fontWeight: '400', // Normal weight for read items
        marginBottom: 4,
        color: '#333',
    },
    eventTitleBold: {
        fontWeight: '700', // Bold for unread
    },
    notificationText: {
        fontSize: 13,
        color: '#666',
        marginBottom: 4,
        lineHeight: 18,
    },
    notificationTextRead: {
        color: '#999', // Lighter color for read items
    },
    timeText: {
        fontSize: 11,
        color: '#999',
    },
    actionsContainer: {
        flexDirection: 'row',
        marginLeft: 8,
        gap: 8,
    },
    actionButton: {
        padding: 8,
        justifyContent: 'center',
        alignItems: 'center',
    },
});
