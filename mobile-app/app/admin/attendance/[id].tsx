import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    Modal,
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
import { useColorScheme } from '../../../hooks/use-color-scheme';
import {
    checkInAttendee,
    getAttendanceEventAttendees,
    undoCheckInAttendee,
    type AttendanceAttendee,
    type AttendanceEventAttendeesResponse,
    type AttendanceUpdate,
} from '../../../services/attendance.service';
import { getEventById, type EventDetail } from '../../../services/events.service';
import { getStyles as getEventStyles } from '../../../styles/events.styles';
import { formatIsoDate, formatIsoTimeRange, formatIsoDateTime } from '../../../utils/date-time';
import { getTranslatedEventTitle } from '../../../utils/event-translations';
import { webSocketService } from '../../../utils/websocket';

type AttendeeSortOption = 'nameAsc' | 'nameDesc' | 'emailAsc' | 'emailDesc';

export default function AttendanceEventDetailScreen() {
    const { id } = useLocalSearchParams();
    const { t } = useTranslation();
    const { user, hasRole, isLoading } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const eventStyles = getEventStyles(colorScheme);
    const isDark = colorScheme === 'dark';
    const styles = getStyles(isDark);
    const indicatorColor = isDark ? '#6AA9FF' : '#0056A8';
    const iconColor = isDark ? '#A0A7B1' : '#666';
    const placeholderColor = isDark ? '#8B93A1' : '#999';

    const eventId = useMemo(() => {
        if (typeof id === 'string') return parseInt(id, 10);
        if (Array.isArray(id) && id.length > 0) return parseInt(id[0], 10);
        return NaN;
    }, [id]);

    const [attendance, setAttendance] = useState<AttendanceEventAttendeesResponse | null>(null);
    const [eventDetail, setEventDetail] = useState<EventDetail | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [filterVisible, setFilterVisible] = useState(false);
    const [sortOption, setSortOption] = useState<AttendeeSortOption>('nameAsc');
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
    }, [eventId, refreshing, t, user?.token, isLoading]);

    useFocusEffect(
        useCallback(() => {
            if (canView && !isLoading) {
                loadAttendance();
            }
        }, [canView, loadAttendance, isLoading]),
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
        const query = searchQuery.toLowerCase().trim();
        const searchedAttendees = query
            ? attendance.attendees.filter((attendee) => {
                const name = attendee.fullName?.toLowerCase() ?? '';
                const email = attendee.email?.toLowerCase() ?? '';
                return name.includes(query) || email.includes(query);
            })
            : attendance.attendees;

        const sortKey = (attendee: AttendanceAttendee, key: 'name' | 'email') => {
            if (key === 'name') {
                return (attendee.fullName || attendee.email || '').toLowerCase();
            }
            return (attendee.email || attendee.fullName || '').toLowerCase();
        };

        const sortedAttendees = [...searchedAttendees].sort((a, b) => {
            switch (sortOption) {
                case 'nameDesc':
                    return sortKey(b, 'name').localeCompare(sortKey(a, 'name'));
                case 'emailAsc':
                    return sortKey(a, 'email').localeCompare(sortKey(b, 'email'));
                case 'emailDesc':
                    return sortKey(b, 'email').localeCompare(sortKey(a, 'email'));
                case 'nameAsc':
                default:
                    return sortKey(a, 'name').localeCompare(sortKey(b, 'name'));
            }
        });

        return sortedAttendees;
    }, [attendance, searchQuery, sortOption]);

    const renderAttendee = ({ item }: { item: AttendanceAttendee }) => {
        const isPending = Boolean(pendingIds[item.userId]);
        return (
            <View style={styles.attendeeCard}>
                <View style={styles.attendeeInfo}>
                    <ThemedText style={styles.attendeeName} testID="attendance-attendee-name">
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
                    accessibilityRole="button"
                    accessibilityLabel={item.checkedIn ? t('attendance.actions.undoCheckIn') : t('attendance.actions.checkIn')}
                    accessibilityHint={t('attendance.actions.checkInHint', 'Updates attendance for this attendee')}
                    accessibilityState={{ disabled: isPending }}
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
                <ActivityIndicator size="large" color={indicatorColor} />
                <ThemedText style={eventStyles.loadingText}>{t('attendance.attendees.loading')}</ThemedText>
            </ThemedView>
        );
    } else if (error) {
        content = (
            <ThemedView style={eventStyles.centered}>
                <ThemedText style={eventStyles.errorText}>{error}</ThemedText>
                <Pressable
                    onPress={loadAttendance}
                    style={styles.retryButton}
                    accessibilityRole="button"
                    accessibilityLabel={t('attendance.retry')}
                >
                    <ThemedText style={styles.retryText}>{t('attendance.retry')}</ThemedText>
                </Pressable>
            </ThemedView>
        );
    } else {
        content = (
            <>
                <FlatList
                    data={filteredAttendees}
                    keyExtractor={(item) => item.userId.toString()}
                    renderItem={renderAttendee}
                    contentContainerStyle={eventStyles.listContent}
                    refreshControl={
                        <RefreshControl
                            refreshing={refreshing}
                            onRefresh={onRefresh}
                            colors={[indicatorColor]}
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
                            <View style={styles.searchRow}>
                                <View style={[eventStyles.searchContainer, styles.searchContainer]}>
                                    <Ionicons name="search" size={20} color={iconColor} style={eventStyles.searchIcon} />
                                    <TextInput
                                        style={[eventStyles.searchInput, { color: isDark ? '#ECEDEE' : '#333' }]}
                                        placeholder={t('attendance.attendees.searchPlaceholder')}
                                        placeholderTextColor={placeholderColor}
                                        value={searchQuery}
                                        onChangeText={setSearchQuery}
                                        accessibilityLabel={t('attendance.attendees.searchLabel', 'Search attendees')}
                                    />
                                    {searchQuery.length > 0 && (
                                        <Pressable
                                            onPress={() => setSearchQuery('')}
                                            hitSlop={10}
                                            accessibilityRole="button"
                                            accessibilityLabel={t('common.clearSearch', 'Clear search')}
                                        >
                                            <Ionicons name="close-circle" size={20} color={iconColor} style={{ marginLeft: 8 }} />
                                        </Pressable>
                                    )}
                                </View>
                                <Pressable
                                    onPress={() => setFilterVisible(true)}
                                    style={styles.filterButton}
                                    testID="attendance-attendees-filter-button"
                                    accessibilityRole="button"
                                    accessibilityLabel={t('attendance.attendees.filters.button')}
                                    accessibilityHint={t('attendance.attendees.filters.hint', 'Opens sorting options')}
                                >
                                    <Ionicons name="funnel-outline" size={18} color={iconColor} />
                                    <ThemedText style={styles.filterButtonText}>
                                        {t('attendance.attendees.filters.button')}
                                    </ThemedText>
                                </Pressable>
                            </View>
                        </View>
                    }
                    ListEmptyComponent={
                        <ThemedText style={eventStyles.emptyText}>{t('attendance.attendees.empty')}</ThemedText>
                    }
                />
                <Modal
                    transparent
                    visible={filterVisible}
                    animationType="fade"
                    onRequestClose={() => setFilterVisible(false)}
                >
                    <View style={eventStyles.modalOverlay}>
                        <Pressable
                            style={StyleSheet.absoluteFillObject}
                            onPress={() => setFilterVisible(false)}
                            accessible={false}
                        />
                        <View style={eventStyles.modalCard}>
                            <View style={styles.filterHeader}>
                                <ThemedText style={styles.filterTitle}>
                                    {t('attendance.attendees.filters.title')}
                                </ThemedText>
                                <Pressable
                                    onPress={() => setFilterVisible(false)}
                                    hitSlop={10}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('common.close', 'Close')}
                                >
                                    <Ionicons name="close" size={20} color={iconColor} />
                                </Pressable>
                            </View>
                            <View style={styles.filterOptions}>
                                {(
                                    [
                                        { key: 'nameAsc', label: t('attendance.attendees.filters.nameAsc') },
                                        { key: 'nameDesc', label: t('attendance.attendees.filters.nameDesc') },
                                        { key: 'emailAsc', label: t('attendance.attendees.filters.emailAsc') },
                                        { key: 'emailDesc', label: t('attendance.attendees.filters.emailDesc') },
                                    ] as { key: AttendeeSortOption; label: string }[]
                                ).map((option) => {
                                    const isSelected = sortOption === option.key;
                                    return (
                                        <Pressable
                                            key={option.key}
                                            onPress={() => {
                                                setSortOption(option.key);
                                                setFilterVisible(false);
                                            }}
                                            style={[
                                                styles.filterOption,
                                                isSelected && styles.filterOptionSelected,
                                            ]}
                                            testID={`attendance-attendees-filter-option-${option.key}`}
                                            accessibilityRole="button"
                                            accessibilityLabel={option.label}
                                            accessibilityState={{ selected: isSelected }}
                                        >
                                            <ThemedText
                                                style={[
                                                    styles.filterOptionText,
                                                    isSelected && styles.filterOptionTextSelected,
                                                ]}
                                            >
                                                {option.label}
                                            </ThemedText>
                                            {isSelected && (
                                                <Ionicons name="checkmark" size={18} color={indicatorColor} />
                                            )}
                                        </Pressable>
                                    );
                                })}
                            </View>
                        </View>
                    </View>
                </Modal>
            </>
        );
    }

    return (
        <MenuLayout>
            <ThemedView style={eventStyles.container}>{content}</ThemedView>
        </MenuLayout>
    );
}

const getStyles = (isDark: boolean) => {
    const colors = {
        text: isDark ? '#ECEDEE' : '#1A2D4A',
        textMuted: isDark ? '#A0A7B1' : '#3D4D65',
        textSubtle: isDark ? '#8B93A1' : '#6F7B91',
        surface: isDark ? '#1F2328' : '#FFFFFF',
        background: isDark ? '#0F1419' : '#F5F7FB',
        border: isDark ? '#3A3F47' : '#E0E4EC',
        borderLight: isDark ? '#2A2F37' : '#E8ECEF',
        successText: isDark ? '#66BB6A' : '#1E7C44',
        warningText: isDark ? '#FFB74D' : '#4A5568',
        bgLight: isDark ? '#1F2328' : '#F5F7FB',
    };

    return StyleSheet.create({
        header: {
            marginBottom: 16,
        },
        eventTitle: {
            fontSize: 16,
            fontWeight: '700',
            color: colors.text,
        },
        eventMeta: {
            marginTop: 4,
            fontSize: 12,
            color: colors.textSubtle,
        },
        summaryRow: {
            marginTop: 8,
            flexDirection: 'row',
            gap: 12,
        },
        summaryItem: {
            fontSize: 12,
            color: colors.textMuted,
            fontWeight: '600',
        },
        attendeeCard: {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: 14,
            borderRadius: 12,
            backgroundColor: colors.surface,
            borderWidth: 1,
            borderColor: colors.border,
            marginBottom: 12,
            gap: 12,
        },
        attendeeInfo: {
            flex: 1,
        },
        attendeeName: {
            fontSize: 15,
            fontWeight: '700',
            color: colors.text,
        },
        attendeeEmail: {
            marginTop: 2,
            fontSize: 12,
            color: colors.textSubtle,
        },
        timestampText: {
            fontSize: 12,
            color: colors.successText,
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
            backgroundColor: isDark ? '#1B3A1B' : '#E7F5EC',
        },
        statusPending: {
            backgroundColor: isDark ? '#2A2F37' : '#F2F4F8',
        },
        statusText: {
            fontSize: 11,
            fontWeight: '600',
        },
        statusCheckedText: {
            color: colors.successText,
        },
        statusPendingText: {
            color: colors.textSubtle,
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
            backgroundColor: isDark ? '#2A2F37' : '#F5F7FB',
            borderWidth: 1,
            borderColor: isDark ? '#3A3F47' : '#C7D2E5',
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
        searchRow: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 10,
            marginBottom: 16,
        },
        searchContainer: {
            flex: 1,
            marginBottom: 0,
        },
        filterButton: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 6,
            paddingHorizontal: 12,
            paddingVertical: 12,
            borderRadius: 12,
            borderWidth: 1,
            borderColor: colors.border,
            backgroundColor: colors.bgLight,
        },
        filterButtonText: {
            fontSize: 13,
            fontWeight: '600',
            color: colors.text,
        },
        filterHeader: {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingHorizontal: 18,
            paddingTop: 16,
            paddingBottom: 12,
            borderBottomWidth: StyleSheet.hairlineWidth,
            borderBottomColor: colors.border,
        },
        filterTitle: {
            fontSize: 16,
            fontWeight: '700',
            color: colors.text,
        },
        filterOptions: {
            paddingHorizontal: 16,
            paddingVertical: 12,
            gap: 6,
        },
        filterOption: {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingHorizontal: 10,
            paddingVertical: 12,
            borderRadius: 10,
        },
        filterOptionSelected: {
            backgroundColor: colors.bgLight,
        },
        filterOptionText: {
            fontSize: 14,
            fontWeight: '600',
            color: colors.text,
        },
        filterOptionTextSelected: {
            color: colors.text,
        },
    });
};
