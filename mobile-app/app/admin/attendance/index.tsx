import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    Pressable,
    RefreshControl,
    StyleSheet,
    TextInput,
    View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useFocusEffect } from '@react-navigation/native';
import { useTranslation } from 'react-i18next';

import { MenuLayout } from '../../../components/menu-layout';
import { ThemedText } from '../../../components/themed-text';
import { ThemedView } from '../../../components/themed-view';
import { useAuth } from '../../../context/AuthContext';
import { useColorScheme } from '../../../hooks/use-color-scheme';
import {
    getAttendanceEvents,
    type AttendanceEventSummary,
    type AttendanceUpdate,
} from '../../../services/attendance.service';
import { getStyles as getEventStyles } from '../../../styles/events.styles';
import { formatIsoDate, formatIsoTimeRange } from '../../../utils/date-time';
import { getTranslatedEventTitle } from '../../../utils/event-translations';
import { webSocketService } from '../../../utils/websocket';

export default function AttendanceDashboardScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { user, hasRole, isLoading } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const eventStyles = getEventStyles(colorScheme);
    const isDark = colorScheme === 'dark';
    const styles = getStyles(isDark);
    const iconColor = isDark ? '#A0A7B1' : '#666';
    const placeholderColor = isDark ? '#8B93A1' : '#999';
    const indicatorColor = isDark ? '#6AA9FF' : '#0056A8';

    const [events, setEvents] = useState<AttendanceEventSummary[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const canView = user && hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']);

    const loadEvents = useCallback(async () => {
        if (!user?.token || isLoading) {
            setError(t('common.notAuthenticated'));
            setLoading(false);
            setRefreshing(false);
            return;
        }

        try {
            setError(null);
            if (!refreshing) {
                setLoading(true);
            }
            const data = await getAttendanceEvents(user.token);
            setEvents(data);
        } catch (err) {
            console.error('Failed to load attendance events', err);
            setError(t('attendance.loadError'));
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [refreshing, t, user?.token, isLoading]);

    useFocusEffect(
        useCallback(() => {
            if (canView && !isLoading) {
                loadEvents();
            }
        }, [canView, loadEvents, isLoading]),
    );

    useEffect(() => {
        if (!user?.token) return;
        webSocketService.connect(user.token);
        const unsubscribe = webSocketService.subscribe('/topic/attendance/events', (message: AttendanceUpdate) => {
            setEvents((prev) =>
                prev.map((event) =>
                    event.eventId === message.eventId
                        ? {
                            ...event,
                            checkedInCount: message.checkedInCount,
                            registeredCount: message.registeredCount,
                            occupancyPercent: message.occupancyPercent ?? event.occupancyPercent,
                        }
                        : event,
                ),
            );
        });

        return unsubscribe;
    }, [user?.token]);

    const filteredEvents = useMemo(() => {
        if (!searchQuery.trim()) return events;
        const query = searchQuery.toLowerCase().trim();
        return events.filter((event) => {
            const title = getTranslatedEventTitle({ title: event.title }, t).toLowerCase();
            const location = event.locationName?.toLowerCase() ?? '';
            const address = event.address?.toLowerCase() ?? '';
            return title.includes(query) || location.includes(query) || address.includes(query);
        });
    }, [events, searchQuery, t]);

    const handleRefresh = useCallback(() => {
        setRefreshing(true);
        loadEvents();
    }, [loadEvents]);

    const formatOccupancy = (event: AttendanceEventSummary) => {
        if (event.maxCapacity == null) {
            return t('attendance.occupancy.unlimited', {
                count: event.checkedInCount,
            });
        }
        const percent = event.occupancyPercent ?? (event.maxCapacity > 0
            ? (event.checkedInCount * 100.0) / event.maxCapacity
            : 0);
        return t('attendance.occupancy.value', {
            checkedIn: event.checkedInCount,
            capacity: event.maxCapacity,
            percent: Math.round(percent),
        });
    };

    const renderEvent = ({ item }: { item: AttendanceEventSummary }) => (
        <View style={eventStyles.card}>
            <Pressable
                onPress={() => router.push(`/admin/attendance/${item.eventId}`)}
                style={({ pressed }) => [pressed && { opacity: 0.7 }]}
            >
                <View style={eventStyles.cardHeader}>
                    <ThemedText type="subtitle" style={eventStyles.eventTitle}>
                        {getTranslatedEventTitle({ title: item.title }, t)}
                    </ThemedText>
                </View>

                <View style={eventStyles.row}>
                    <ThemedText style={eventStyles.label}>{t('events.fields.date')}</ThemedText>
                    <ThemedText style={eventStyles.value}>{formatIsoDate(item.startDateTime)}</ThemedText>
                </View>

                <View style={eventStyles.row}>
                    <ThemedText style={eventStyles.label}>{t('events.fields.time')}</ThemedText>
                    <ThemedText style={eventStyles.value}>
                        {formatIsoTimeRange(item.startDateTime, item.endDateTime)}
                    </ThemedText>
                </View>

                <View style={eventStyles.row}>
                    <ThemedText style={eventStyles.label}>{t('events.fields.place')}</ThemedText>
                    <ThemedText style={eventStyles.value}>{item.address}</ThemedText>
                </View>

                <View style={styles.metricsRow}>
                    <View style={styles.metricCard}>
                        <ThemedText style={styles.metricLabel}>{t('attendance.fields.registered')}</ThemedText>
                        <ThemedText style={styles.metricValue}>{item.registeredCount}</ThemedText>
                    </View>
                    <View style={styles.metricCard}>
                        <ThemedText style={styles.metricLabel}>{t('attendance.fields.checkedIn')}</ThemedText>
                        <ThemedText style={styles.metricValue}>{item.checkedInCount}</ThemedText>
                    </View>
                </View>

                <View style={styles.occupancyRow}>
                    <ThemedText style={styles.occupancyLabel}>{t('attendance.fields.occupancy')}</ThemedText>
                    <ThemedText style={styles.occupancyValue}>{formatOccupancy(item)}</ThemedText>
                </View>

                {item.maxCapacity != null && (
                    <View style={styles.progressTrack}>
                        <View
                            style={[
                                styles.progressFill,
                                {
                                    width: `${Math.min(item.occupancyPercent ?? 0, 100)}%`,
                                },
                            ]}
                        />
                    </View>
                )}

                <View style={eventStyles.footerButton}>
                    <ThemedText style={eventStyles.footerButtonText}>
                        {t('attendance.actions.viewAttendees')}
                    </ThemedText>
                </View>
            </Pressable>
        </View>
    );

    let content = null;

    if (!canView) {
        content = (
            <ThemedView style={eventStyles.centered}>
                <ThemedText>{t('attendance.accessDenied')}</ThemedText>
            </ThemedView>
        );
    } else if (loading) {
        content = (
            <ThemedView style={eventStyles.centered}>
                <ActivityIndicator size="large" color={indicatorColor} />
                <ThemedText style={eventStyles.loadingText}>{t('attendance.loading')}</ThemedText>
            </ThemedView>
        );
    } else if (error) {
        content = (
            <ThemedView style={eventStyles.centered}>
                <ThemedText style={eventStyles.errorText}>{error}</ThemedText>
                <Pressable onPress={loadEvents} style={styles.retryButton}>
                    <ThemedText style={styles.retryText}>{t('attendance.retry')}</ThemedText>
                </Pressable>
            </ThemedView>
        );
    } else {
        content = (
            <FlatList
                data={filteredEvents}
                keyExtractor={(item) => item.eventId.toString()}
                renderItem={renderEvent}
                contentContainerStyle={eventStyles.listContent}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={handleRefresh}
                        colors={[indicatorColor]}
                    />
                }
                ListEmptyComponent={
                    <ThemedText style={eventStyles.emptyText}>{t('attendance.empty')}</ThemedText>
                }
            />
        );
    }

    return (
        <MenuLayout>
            <ThemedView style={eventStyles.container}>
                <ThemedText style={eventStyles.screenTitle}>{t('attendance.title')}</ThemedText>
                <ThemedText style={styles.subtitle}>{t('attendance.subtitle')}</ThemedText>

                <View style={eventStyles.searchContainer}>
                    <Ionicons name="search" size={20} color={iconColor} style={eventStyles.searchIcon} />
                    <TextInput
                        style={[eventStyles.searchInput, { color: isDark ? '#ECEDEE' : '#333' }]}
                        placeholder={t('attendance.searchPlaceholder')}
                        placeholderTextColor={placeholderColor}
                        value={searchQuery}
                        onChangeText={setSearchQuery}
                    />
                    {searchQuery.length > 0 && (
                        <Pressable onPress={() => setSearchQuery('')} hitSlop={10}>
                            <Ionicons name="close-circle" size={20} color={iconColor} style={{ marginLeft: 8 }} />
                        </Pressable>
                    )}
                </View>

                {content}
            </ThemedView>
        </MenuLayout>
    );
}

const getStyles = (isDark: boolean) => {
    const colors = {
        text: isDark ? '#ECEDEE' : '#0F2848',
        textMuted: isDark ? '#A0A7B1' : '#5A6A85',
        textSubtle: isDark ? '#8B93A1' : '#6F7B91',
        surface: isDark ? '#F5F8FF' : '#F5F8FF',
        background: isDark ? '#0F1419' : '#F5F7FB',
        border: isDark ? '#3A3F47' : '#D6E2F5',
        borderLight: isDark ? '#2A2F37' : '#E2E8F2',
        bgLight: isDark ? '#1F2328' : '#F5F8FF',
    };

    return StyleSheet.create({
        subtitle: {
            fontSize: 13,
            color: colors.textSubtle,
            marginBottom: 12,
            marginTop: -8,
        },
        metricsRow: {
            flexDirection: 'row',
            gap: 12,
            marginTop: 8,
        },
        metricCard: {
            flex: 1,
            backgroundColor: isDark ? '#1F2328' : '#F5F8FF',
            borderRadius: 10,
            paddingVertical: 10,
            paddingHorizontal: 12,
            borderWidth: StyleSheet.hairlineWidth,
            borderColor: isDark ? '#3A3F47' : '#D6E2F5',
        },
        metricLabel: {
            fontSize: 12,
            color: colors.textMuted,
            fontWeight: '600',
        },
        metricValue: {
            marginTop: 4,
            fontSize: 20,
            fontWeight: '700',
            color: colors.text,
        },
        occupancyRow: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: 10,
        },
        occupancyLabel: {
            fontSize: 12,
            color: colors.textMuted,
            fontWeight: '600',
        },
        occupancyValue: {
            fontSize: 12,
            fontWeight: '700',
            color: colors.text,
        },
        progressTrack: {
            marginTop: 8,
            height: 8,
            borderRadius: 8,
            backgroundColor: isDark ? '#2A2F37' : '#E2E8F2',
            overflow: 'hidden',
        },
        progressFill: {
            height: '100%',
            backgroundColor: '#0056A8',
            borderRadius: 8,
        },
        retryButton: {
            marginTop: 12,
            paddingHorizontal: 16,
            paddingVertical: 8,
            borderRadius: 8,
            borderWidth: 1,
            borderColor: '#0056A8',
        },
        retryText: {
            color: '#0056A8',
            fontWeight: '600',
        },
    });
};


