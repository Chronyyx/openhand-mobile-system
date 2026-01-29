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
import { useFocusEffect } from '@react-navigation/native';
import { useLocalSearchParams } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { MenuLayout } from '../../../components/menu-layout';
import { ThemedText } from '../../../components/themed-text';
import { ThemedView } from '../../../components/themed-view';
import { useAuth } from '../../../context/AuthContext';
import {
    checkInAttendee,
    getAttendanceEventAttendees,
    undoCheckInAttendee,
    type AttendanceAttendee,
    type AttendanceEventAttendeesResponse,
    type AttendanceUpdate,
} from '../../../services/attendance.service';
import { getEventById, type EventDetail } from '../../../services/events.service';
import { styles as eventStyles } from '../../../styles/events.styles';
import { formatIsoDate, formatIsoTimeRange, formatIsoDateTime } from '../../../utils/date-time';
import { getTranslatedEventTitle } from '../../../utils/event-translations';
import { webSocketService } from '../../../utils/websocket';

export default function AttendanceEventDetailScreen() {
    const { id } = useLocalSearchParams();
    const { t } = useTranslation();
    const { user, hasRole } = useAuth();

    const eventId = useMemo(() => {
        if (typeof id === 'string') return parseInt(id, 10);
        if (Array.isArray(id) && id.length > 0) return parseInt(id[0], 10);
        return NaN;
    }, [id]);

    const [attendance, setAttendance] = useState<AttendanceEventAttendeesResponse | null>(null);
    const [eventDetail, setEventDetail] = useState<EventDetail | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [pendingIds, setPendingIds] = useState<Record<number, boolean>>({});

    const canView = user && hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']);

    const loadAttendance = useCallback(async () => {
        if (Number.isNaN(eventId)) {
            setError(t('attendance.attendees.loadError'));
            setLoading(false);
            setRefreshing(false);
            return;
        }
        if (!user?.token) {
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
            const [eventData, attendanceData] = await Promise.all([
                getEventById(eventId),
                getAttendanceEventAttendees(eventId, user.token),
            ]);
            setEventDetail(eventData);
            setAttendance(attendanceData);
        } catch (err) {
            console.error('Failed to load attendance attendees', err);
            setError(t('attendance.attendees.loadError'));
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [eventId, refreshing, t, user?.token]);

    useFocusEffect(
        useCallback(() => {
            if (canView) {
                loadAttendance();
            }
        }, [canView, loadAttendance]),
    );

    const applyUpdate = useCallback((update: AttendanceUpdate) => {
        setAttendance((prev) => {
            if (!prev || prev.eventId !== update.eventId) return prev;
            const attendees = prev.attendees.map((attendee) =>
                attendee.userId === update.userId
                    ? {
                        ...attendee,
                        checkedIn: update.checkedIn,
                        checkedInAt: update.checkedInAt,
                    }
                    : attendee,
            );
            return {
                ...prev,
                attendees,
                registeredCount: update.registeredCount,
                checkedInCount: update.checkedInCount,
            };
        });
    }, []);

    useEffect(() => {
        if (!user?.token || Number.isNaN(eventId)) return;
        webSocketService.connect(user.token);
        const unsubscribe = webSocketService.subscribe(`/topic/attendance/events/${eventId}`, (message: AttendanceUpdate) => {
            applyUpdate(message);
        });
        return unsubscribe;
    }, [applyUpdate, eventId, user?.token]);

    const onRefresh = useCallback(() => {
        setRefreshing(true);
        loadAttendance();
    }, [loadAttendance]);

    const handleToggleCheckIn = useCallback(
        async (attendee: AttendanceAttendee) => {
            if (!user?.token || Number.isNaN(eventId)) return;
            setPendingIds((prev) => ({ ...prev, [attendee.userId]: true }));
            try {
                const update = attendee.checkedIn
                    ? await undoCheckInAttendee(eventId, attendee.userId, user.token)
                    : await checkInAttendee(eventId, attendee.userId, user.token);
                applyUpdate(update);
            } catch (err) {
                console.error('Failed to toggle check-in', err);
            } finally {
                setPendingIds((prev) => ({ ...prev, [attendee.userId]: false }));
            }
        },
        [applyUpdate, eventId, user?.token],
    );

    const filteredAttendees = useMemo(() => {
        if (!attendance) return [];
        if (!searchQuery.trim()) return attendance.attendees;
        const query = searchQuery.toLowerCase().trim();
        return attendance.attendees.filter((attendee) => {
            const name = attendee.fullName?.toLowerCase() ?? '';
            const email = attendee.email?.toLowerCase() ?? '';
            return name.includes(query) || email.includes(query);
        });
    }, [attendance, searchQuery]);

    const renderAttendee = ({ item }: { item: AttendanceAttendee }) => {
        const isPending = Boolean(pendingIds[item.userId]);
        return (
            <View style={styles.attendeeCard}>
                <View style={styles.attendeeInfo}>
                    <ThemedText style={styles.attendeeName}>
                        {item.fullName || item.email || t('attendance.attendees.unknownName')}
                    </ThemedText>
                    <ThemedText style={styles.attendeeEmail}>{item.email}</ThemedText>
                    {item.checkedIn && item.checkedInAt && (
                        <ThemedText style={styles.timestampText}>
                            {t('attendance.attendees.checkedInAt', { timestamp: formatIsoDateTime(item.checkedInAt) })}
                        </ThemedText>
                    )}
                    <View style={[styles.statusPill, item.checkedIn ? styles.statusChecked : styles.statusPending]}>
                        <ThemedText
                            style={[
                                styles.statusText,
                                item.checkedIn ? styles.statusCheckedText : styles.statusPendingText,
                            ]}
                        >
                            {item.checkedIn
                                ? t('attendance.attendees.checkedIn')
                                : t('attendance.attendees.notCheckedIn')}
                        </ThemedText>
                    </View>
                </View>
                <Pressable
                    style={[
                        styles.checkInButton,
                        item.checkedIn ? styles.undoButton : styles.confirmButton,
                        isPending && styles.disabledButton,
                    ]}
                    disabled={isPending}
                    onPress={() => handleToggleCheckIn(item)}
                >
                    <ThemedText
                        style={[
                            styles.checkInButtonText,
                            item.checkedIn ? styles.undoButtonText : styles.confirmButtonText,
                        ]}
                    >
                        {item.checkedIn ? t('attendance.actions.undoCheckIn') : t('attendance.actions.checkIn')}
                    </ThemedText>
                </Pressable>
            </View>
        );
    };

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
                <ActivityIndicator size="large" color="#0056A8" />
                <ThemedText style={eventStyles.loadingText}>{t('attendance.attendees.loading')}</ThemedText>
            </ThemedView>
        );
    } else if (error) {
        content = (
            <ThemedView style={eventStyles.centered}>
                <ThemedText style={eventStyles.errorText}>{error}</ThemedText>
                <Pressable onPress={loadAttendance} style={styles.retryButton}>
                    <ThemedText style={styles.retryText}>{t('attendance.retry')}</ThemedText>
                </Pressable>
            </ThemedView>
        );
    } else {
        content = (
            <FlatList
                data={filteredAttendees}
                keyExtractor={(item) => item.userId.toString()}
                renderItem={renderAttendee}
                contentContainerStyle={eventStyles.listContent}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={onRefresh}
                        colors={['#0056A8']}
                    />
                }
                ListHeaderComponent={
                    <View style={styles.header}>
                        <ThemedText style={eventStyles.screenTitle}>
                            {t('attendance.attendees.title')}
                        </ThemedText>
                        {eventDetail && (
                            <>
                                <ThemedText style={styles.eventTitle}>
                                    {getTranslatedEventTitle(eventDetail, t)}
                                </ThemedText>
                                <ThemedText style={styles.eventMeta}>
                                    {formatIsoDate(eventDetail.startDateTime)} ·{' '}
                                    {formatIsoTimeRange(eventDetail.startDateTime, eventDetail.endDateTime)}
                                </ThemedText>
                            </>
                        )}
                        <View style={styles.summaryRow}>
                            <ThemedText style={styles.summaryItem}>
                                {t('attendance.attendees.registeredCount', {
                                    count: attendance?.registeredCount ?? 0,
                                })}
                            </ThemedText>
                            <ThemedText style={styles.summaryItem}>
                                {t('attendance.attendees.checkedInCount', {
                                    count: attendance?.checkedInCount ?? 0,
                                })}
                            </ThemedText>
                        </View>
                        <View style={eventStyles.searchContainer}>
                            <Ionicons name="search" size={20} color="#666" style={eventStyles.searchIcon} />
                            <TextInput
                                style={eventStyles.searchInput}
                                placeholder={t('attendance.attendees.searchPlaceholder')}
                                placeholderTextColor="#999"
                                value={searchQuery}
                                onChangeText={setSearchQuery}
                            />
                            {searchQuery.length > 0 && (
                                <Pressable onPress={() => setSearchQuery('')} hitSlop={10}>
                                    <Ionicons name="close-circle" size={20} color="#666" style={{ marginLeft: 8 }} />
                                </Pressable>
                            )}
                        </View>
                    </View>
                }
                ListEmptyComponent={
                    <ThemedText style={eventStyles.emptyText}>{t('attendance.attendees.empty')}</ThemedText>
                }
            />
        );
    }

    return (
        <MenuLayout>
            <ThemedView style={eventStyles.container}>{content}</ThemedView>
        </MenuLayout>
    );
}

const styles = StyleSheet.create({
    header: {
        marginBottom: 16,
    },
    eventTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: '#1A2D4A',
    },
    eventMeta: {
        marginTop: 4,
        fontSize: 12,
        color: '#6F7B91',
    },
    summaryRow: {
        marginTop: 8,
        flexDirection: 'row',
        gap: 12,
    },
    summaryItem: {
        fontSize: 12,
        color: '#3D4D65',
        fontWeight: '600',
    },
    attendeeCard: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: 14,
        borderRadius: 12,
        backgroundColor: '#FFFFFF',
        borderWidth: 1,
        borderColor: '#E0E4EC',
        marginBottom: 12,
        gap: 12,
    },
    attendeeInfo: {
        flex: 1,
    },
    attendeeName: {
        fontSize: 15,
        fontWeight: '700',
        color: '#1A2D4A',
    },
    attendeeEmail: {
        marginTop: 2,
        fontSize: 12,
        color: '#6F7B91',
    },
    timestampText: {
        fontSize: 12,
        color: '#1E7C44',
        marginTop: 4,
        fontWeight: '500',
    },
    statusPill: {
        marginTop: 8,
        alignSelf: 'flex-start',
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 12,
    },
    statusChecked: {
        backgroundColor: '#E7F5EC',
    },
    statusPending: {
        backgroundColor: '#F2F4F8',
    },
    statusText: {
        fontSize: 11,
        fontWeight: '600',
    },
    statusCheckedText: {
        color: '#1E7C44',
    },
    statusPendingText: {
        color: '#4A5568',
    },
    checkInButton: {
        paddingVertical: 8,
        paddingHorizontal: 12,
        borderRadius: 8,
        alignItems: 'center',
        justifyContent: 'center',
    },
    confirmButton: {
        backgroundColor: '#0056A8',
    },
    undoButton: {
        backgroundColor: '#F5F7FB',
        borderWidth: 1,
        borderColor: '#C7D2E5',
    },
    disabledButton: {
        opacity: 0.6,
    },
    checkInButtonText: {
        fontSize: 12,
        fontWeight: '700',
    },
    confirmButtonText: {
        color: '#FFFFFF',
    },
    undoButtonText: {
        color: '#0056A8',
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
