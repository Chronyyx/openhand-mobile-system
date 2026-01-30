import React, { useEffect, useState, useCallback, useRef } from 'react';
import {
    ActivityIndicator,
    FlatList,
    ListRenderItemInfo,
    RefreshControl,
    View,
    TextInput,
    Pressable,
    Alert,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';

import { ThemedView } from '../../components/themed-view';
import { ThemedText } from '../../components/themed-text';
import { EventCard } from '../../components/EventCard';
import { EventDetailModal, type FamilyMemberInput } from '../../components/EventDetailModal';
import { MenuLayout } from '../../components/menu-layout';
import { getTranslatedEventTitle, getTranslatedEventDescription } from '../../utils/event-translations';
import { useRouter, useFocusEffect } from 'expo-router';
import { useCountdownTimer } from '../../hooks/useCountdownTimer';
import { useNotifications } from '../../hooks/useNotifications';
import AsyncStorage from '@react-native-async-storage/async-storage';

import {
    getUpcomingEvents,
    getRegistrationSummary,
    getEventById,
    type EventSummary,
    type EventDetail,
    type RegistrationSummary,
} from '../../services/events.service';
import {
    registerForEventWithFamily,
    cancelRegistration,
    getMyRegistrations,
    type Registration,
    type FamilyMemberPayload,
    type GroupRegistrationResponse
} from '../../services/registration.service';
import { useAuth } from '../../context/AuthContext';

import { styles } from '../../styles/events.styles';

const HIDDEN_EVENTS_KEY = 'hiddenEventIds';

export default function EventsScreen() {
    const { t } = useTranslation() as { t: (key: string, options?: any) => string };
    const { user, hasRole } = useAuth();
    const router = useRouter();
    const { notifications, markAllAsRead, markAsRead } = useNotifications();

    // Data State
    const [events, setEvents] = useState<EventSummary[]>([]);
    const [filteredEvents, setFilteredEvents] = useState<EventSummary[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Modal State
    const [selectedEvent, setSelectedEvent] = useState<EventSummary | null>(null);
    const [eventDetail, setEventDetail] = useState<EventDetail | null>(null);
    const [modalVisible, setModalVisible] = useState(false);
    const [detailsLoading, setDetailsLoading] = useState(false);
    const [detailsError, setDetailsError] = useState<string | null>(null);

    // Registration State
    const [userRegistration, setUserRegistration] = useState<Registration | null>(null);
    const [myRegistrations, setMyRegistrations] = useState<Registration[]>([]);
    const [isRegistering, setIsRegistering] = useState(false);
    const [registrationError, setRegistrationError] = useState<string | null>(null);
    const [registrationParticipants, setRegistrationParticipants] = useState<GroupRegistrationResponse['participants'] | null>(null);

    // Admin/Employee Stats
    const [registrationSummary, setRegistrationSummary] = useState<RegistrationSummary | null>(null);
    const [summaryLoading, setSummaryLoading] = useState(false);
    const [summaryError, setSummaryError] = useState<string | null>(null);

    // Hidden Events Logic
    const [hiddenEventIds, setHiddenEventIds] = useState<number[]>([]);
    const prevEventStatuses = useRef<Record<number, string>>({});

    // Success View State
    const [showSuccessView, setShowSuccessView] = useState(false);

    // Countdown Hook
    const {
        countdownSeconds,
        countdownAnimation: countdown,
        startCountdown,
        resetCountdown
    } = useCountdownTimer({
        onComplete: () => {
            setModalVisible(false);
            setSelectedEvent(null);
        }
    });

    const isAdmin = hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']);

    // ========================================
    // HELPER FUNCTIONS
    // ========================================

    // ========================================
    // DATA LOADING
    // ========================================

    const loadEvents = useCallback(async () => {
        try {
            setError(null);
            const data = await getUpcomingEvents();
            const activeEvents = data.filter((event) => event.status !== 'COMPLETED');
            setEvents(activeEvents);
        } catch (e) {
            console.error('Failed to load events', e);
            setError(t('events.loadError'));
        } finally {
            setLoading(false);
        }
    }, [t]);

    const loadMyRegistrations = useCallback(async () => {
        if (!user?.token) {
            setMyRegistrations([]);
            return;
        }

        try {
            const regs = await getMyRegistrations(user.token);
            setMyRegistrations(regs);
        } catch (e) {
            console.error('Failed to load registrations', e);
        }
    }, [user?.token]);

    const loadRegistrationSummary = useCallback(async (eventId: number) => {
        if (!isAdmin) return;

        setSummaryLoading(true);
        setSummaryError(null);
        try {
            const summary = await getRegistrationSummary(eventId);
            setRegistrationSummary(summary);
        } catch (e) {
            console.error('Failed to load registration summary', e);
            setSummaryError(t('events.errors.summaryFailed'));
        } finally {
            setSummaryLoading(false);
        }
    }, [isAdmin, t]);

    const onRefresh = async () => {
        setRefreshing(true);
        await Promise.all([loadEvents(), loadMyRegistrations()]);
        setRefreshing(false);
    };

    // ========================================
    // HIDDEN EVENTS MANAGEMENT
    // ========================================

    const loadHiddenEvents = useCallback(async () => {
        try {
            const stored = await AsyncStorage.getItem(HIDDEN_EVENTS_KEY);
            if (stored) {
                setHiddenEventIds(JSON.parse(stored));
            }
        } catch (e) {
            console.error('Failed to load hidden events', e);
        }
    }, []);

    const saveHiddenEvents = useCallback(async (ids: number[]) => {
        try {
            await AsyncStorage.setItem(HIDDEN_EVENTS_KEY, JSON.stringify(ids));
        } catch (e) {
            console.error('Failed to save hidden events', e);
        }
    }, []);

    const hideEvent = useCallback((eventId: number) => {
        setHiddenEventIds(prev => {
            const newIds = [...prev, eventId];
            saveHiddenEvents(newIds);
            return newIds;
        });

        // Mark associated notification as read
        const notification = notifications.find(n =>
            n.eventId === eventId &&
            !n.read &&
            (n.type === 'CANCELLATION' || n.type === 'EVENT_UPDATE')
        );
        if (notification) {
            markAsRead(notification.id);
        }
    }, [notifications, markAsRead, saveHiddenEvents]);

    const unhideEvent = useCallback((eventId: number) => {
        setHiddenEventIds(prev => {
            const newIds = prev.filter(id => id !== eventId);
            saveHiddenEvents(newIds);
            return newIds;
        });
    }, [saveHiddenEvents]);

    // Auto-unhide events that change status
    useEffect(() => {
        if (events.length === 0) return;

        const idsToUnhide: number[] = [];
        const currentStatuses: Record<number, string> = {};

        events.forEach(event => {
            currentStatuses[event.id] = event.status;

            if (!hiddenEventIds.includes(event.id)) return;

            // Unhide if event becomes active
            if (event.status !== 'CANCELLED') {
                idsToUnhide.push(event.id);
                return;
            }

            // Unhide if event just transitioned to cancelled
            const prevStatus = prevEventStatuses.current[event.id];
            if (prevStatus && prevStatus !== 'CANCELLED') {
                idsToUnhide.push(event.id);
            }
        });

        prevEventStatuses.current = currentStatuses;

        if (idsToUnhide.length > 0) {
            const uniqueIds = [...new Set(idsToUnhide)];
            setHiddenEventIds(prev => {
                const newIds = prev.filter(id => !uniqueIds.includes(id));
                saveHiddenEvents(newIds);
                return newIds;
            });
        }
    }, [events, hiddenEventIds, saveHiddenEvents]);

    // ========================================
    // EVENT FILTERING
    // ========================================

    useEffect(() => {
        let filtered = events;

        // Filter hidden cancelled events (unless they have unread notifications)
        if (hiddenEventIds.length > 0) {
            filtered = filtered.filter(event => {
                if (!hiddenEventIds.includes(event.id)) return true;
                if (event.status !== 'CANCELLED') return true;

                // Show if there's an unread notification
                const hasUnread = notifications.some(n =>
                    n.eventId === event.id &&
                    !n.read &&
                    (n.type === 'CANCELLATION' || n.type === 'EVENT_UPDATE')
                );

                return hasUnread;
            });
        }

        // Filter cancelled events (only show to registered users)
        if (user) {
            filtered = filtered.filter(event => {
                if (event.status !== 'CANCELLED') return true;
                return myRegistrations.some(r => r.eventId === event.id);
            });
        } else {
            filtered = filtered.filter(event => event.status !== 'CANCELLED');
        }

        // Search filter
        if (searchQuery.trim()) {
            const lowerQuery = searchQuery.toLowerCase().trim();
            filtered = filtered.filter(event => {
                const title = getTranslatedEventTitle(event, t).toLowerCase();
                const desc = (getTranslatedEventDescription(event, t) || '').toLowerCase();
                const addr = (event.address || '').toLowerCase();
                return title.includes(lowerQuery) ||
                    desc.includes(lowerQuery) ||
                    addr.includes(lowerQuery);
            });
        }

        setFilteredEvents(filtered);
    }, [searchQuery, events, hiddenEventIds, myRegistrations, user, notifications, t]);

    // ========================================
    // MODAL MANAGEMENT
    // ========================================

    const openEventModal = useCallback(async (event: EventSummary) => {
        setSelectedEvent(event);
        setModalVisible(true);
        setDetailsLoading(true);
        setDetailsError(null);
        setShowSuccessView(false);
        setRegistrationError(null);
        setRegistrationParticipants(null);
        resetCountdown();

        // Check user registration
        const reg = myRegistrations.find(r =>
            r.eventId === event.id && r.status !== 'CANCELLED'
        );
        setUserRegistration(reg || null);

        // Load event details
        try {
            const detail = await getEventById(event.id);
            setEventDetail(detail);

            if (isAdmin) {
                await loadRegistrationSummary(event.id);
            }
        } catch (err) {
            console.error('Failed to load event details', err);
            setDetailsError(t('events.errors.detailsFailed'));
        } finally {
            setDetailsLoading(false);
        }
    }, [myRegistrations, isAdmin, loadRegistrationSummary, resetCountdown, t]);

    const closeEventModal = useCallback(() => {
        setModalVisible(false);
        setSelectedEvent(null);
        setEventDetail(null);
        setRegistrationSummary(null);
        setUserRegistration(null);
        setShowSuccessView(false);
        setIsRegistering(false);
        setRegistrationError(null);
        setRegistrationParticipants(null);
    }, []);

    // ========================================
    // REGISTRATION HANDLERS
    // ========================================

    const handleRegister = useCallback(async (familyMembers: FamilyMemberInput[]) => {
        if (!selectedEvent || !user || isRegistering) return;

        if (selectedEvent.status === 'COMPLETED') {
            setRegistrationError(t('events.errors.eventCompleted'));
            return;
        }

        const normalizedFamily: FamilyMemberPayload[] = [];
        for (const member of familyMembers) {
            const name = member.fullName?.trim() || '';
            const relation = member.relation?.trim() || undefined;
            const ageText = member.age?.trim() || '';
            const ageValue = ageText ? Number(ageText) : NaN;
            const isEmpty = !name && !ageText && !relation;

            if (isEmpty) {
                continue;
            }

            if (!name || !Number.isFinite(ageValue) || ageValue <= 0) {
                setRegistrationError(t('events.family.validationError', 'Please enter a name and age for each family member.'));
                return;
            }

            normalizedFamily.push({
                fullName: name,
                age: Math.floor(ageValue),
                relation
            });
        }

        setIsRegistering(true);
        setRegistrationError(null);

        try {
            const response = await registerForEventWithFamily(selectedEvent.id, normalizedFamily, user.token);
            setRegistrationParticipants(response.participants || []);

            const primary = response.primaryRegistrant;
            if (primary) {
                const newReg: Registration = {
                    id: primary.registrationId,
                    userId: user.id ?? 0,
                    eventId: selectedEvent.id,
                    eventTitle: selectedEvent.title,
                    status: primary.status,
                    requestedAt: new Date().toISOString(),
                    confirmedAt: primary.status === 'CONFIRMED' ? new Date().toISOString() : null,
                    cancelledAt: null,
                    waitlistedPosition: primary.waitlistedPosition ?? null,
                    eventStartDateTime: selectedEvent.startDateTime,
                    eventEndDateTime: selectedEvent.endDateTime
                };
                setUserRegistration(newReg);
            }

            unhideEvent(selectedEvent.id);

            if (primary?.status === 'CONFIRMED') {
                setShowSuccessView(true);
                startCountdown();
            } else if (primary?.status === 'WAITLISTED') {
                setRegistrationError(
                    t('alerts.registerWaitlistMessage', { position: primary.waitlistedPosition })
                );
            }

            await Promise.all([
                onRefresh(),
                isAdmin && selectedEvent ? loadRegistrationSummary(selectedEvent.id) : Promise.resolve()
            ]);

        } catch (e: any) {
            const errorMessage = e.errorData?.message || e.message;

            if (e.status === 403) {
                setRegistrationError(t('events.errors.accessDenied'));
            } else if (e.status === 400 && errorMessage.toLowerCase().includes('capacity')) {
                setRegistrationError(t('events.family.capacityError', 'Not enough capacity for your group.'));
            } else if (e.status === 409) {
                if (errorMessage.includes('capacity')) {
                    setRegistrationError(t('events.errors.eventFull'));
                } else if (errorMessage.toLowerCase().includes('completed')) {
                    setRegistrationError(t('events.errors.eventCompleted'));
                } else if (errorMessage.includes('Already Registered')) {
                    setRegistrationError(t('events.errors.alreadyRegistered'));
                } else {
                    setRegistrationError(errorMessage);
                }

                try {
                    const regs = await getMyRegistrations(user.token);
                    const reg = regs.find(r =>
                        r.eventId === selectedEvent.id && r.status !== 'CANCELLED'
                    );
                    setUserRegistration(reg || null);
                } catch (err) {
                    console.error('Failed to refresh registrations', err);
                }
            } else {
                setRegistrationError(t('events.errors.registrationFailed'));
                console.error('Registration error:', errorMessage);
            }
        } finally {
            setIsRegistering(false);
        }
    }, [selectedEvent, user, isRegistering, isAdmin, t, unhideEvent, startCountdown, onRefresh, loadRegistrationSummary]);

    const handleUnregister = useCallback(async () => {
        if (!selectedEvent || !userRegistration || !user) return;

        setIsRegistering(true);
        setRegistrationError(null);

        try {
            await cancelRegistration(selectedEvent.id, user.token);

            setUserRegistration(null);
            setShowSuccessView(false);
            resetCountdown();

            try {
                if (selectedEvent) {
                    const refreshed = await getEventById(selectedEvent.id);
                    setEventDetail(refreshed);
                    setSelectedEvent(refreshed as EventSummary);
                }
            } catch (err) {
                console.error('Failed to refresh event after unregister', err);
            }

            Alert.alert(t('events.success.unregistered'));

            // Refresh data
            await Promise.all([
                onRefresh(),
                isAdmin && selectedEvent ? loadRegistrationSummary(selectedEvent.id) : Promise.resolve()
            ]);

        } catch (e) {
            console.error('Unregister error:', e);
            setRegistrationError(t('events.errors.unregisterFailed'));
        } finally {
            setIsRegistering(false);
        }
    }, [selectedEvent, userRegistration, user, eventDetail, isAdmin, t, resetCountdown, onRefresh, loadRegistrationSummary]);

    // ========================================
    // LIFECYCLE HOOKS
    // ========================================

    useEffect(() => {
        loadEvents();
        loadHiddenEvents();
    }, [loadEvents, loadHiddenEvents]);

    useEffect(() => {
        loadMyRegistrations();
    }, [loadMyRegistrations, refreshing, showSuccessView]);

    useFocusEffect(
        useCallback(() => {
            if (user?.token) {
                markAllAsRead();
            }
        }, [user, markAllAsRead])
    );

    // ========================================
    // RENDER FUNCTIONS
    // ========================================

    const renderEventItem = ({ item }: ListRenderItemInfo<EventSummary>) => (
        <EventCard
            event={item}
            onPress={openEventModal}
            t={t}
            onClose={item.status === 'CANCELLED' ? () => hideEvent(item.id) : undefined}
        />
    );

    const handleRefreshCapacity = useCallback(() => {
        if (!selectedEvent) return;

        getEventById(selectedEvent.id)
            .then(setEventDetail)
            .catch(err => console.error('Failed to refresh capacity', err));
    }, [selectedEvent]);

    const handleRetrySummary = useCallback(() => {
        if (!selectedEvent) return;

        Promise.all([
            getEventById(selectedEvent.id).then(setEventDetail),
            loadRegistrationSummary(selectedEvent.id)
        ]).catch(err => console.error('Failed to retry summary', err));
    }, [selectedEvent, loadRegistrationSummary]);

    // ========================================
    // RENDER
    // ========================================

    return (
        <MenuLayout>
            <ThemedView style={styles.container}>
                <ThemedText style={styles.screenTitle}>
                    {t('events.title')}
                </ThemedText>

                {/* Search Bar */}
                <View style={styles.searchContainer}>
                    <Ionicons name="search" size={20} color="#666" style={styles.searchIcon} />
                    <TextInput
                        style={styles.searchInput}
                        placeholder={t('events.searchPlaceholder')}
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

                {/* List */}
                {loading ? (
                    <View style={styles.centered}>
                        <ActivityIndicator size="large" color="#0056A8" />
                        <ThemedText style={styles.loadingText}>{t('events.loading')}</ThemedText>
                    </View>
                ) : error ? (
                    <View style={styles.centered}>
                        <ThemedText style={styles.errorText}>{error}</ThemedText>
                    </View>
                ) : (
                    <FlatList
                        data={filteredEvents}
                        renderItem={renderEventItem}
                        keyExtractor={(item: EventSummary) => item.id.toString()}
                        contentContainerStyle={styles.listContent}
                        refreshControl={
                            <RefreshControl
                                refreshing={refreshing}
                                onRefresh={onRefresh}
                                colors={['#0056A8']}
                            />
                        }
                        ListEmptyComponent={
                            <ThemedText style={styles.emptyText}>
                                {t('events.noEvents')}
                            </ThemedText>
                        }
                    />
                )}

                {/* Event Detail Modal */}
                <EventDetailModal
                    visible={modalVisible}
                    onClose={closeEventModal}
                    selectedEvent={selectedEvent}
                    eventDetail={eventDetail}
                    loading={detailsLoading}
                    error={detailsError}
                    user={user}
                    hasRole={hasRole}
                    t={t}
                    userRegistration={userRegistration}
                    onRegister={handleRegister}
                    onUnregister={handleUnregister}
                    showSuccessView={showSuccessView}
                    countdownValue={countdown}
                    countdownSeconds={countdownSeconds}
                    registrationSummary={registrationSummary}
                    summaryLoading={summaryLoading}
                    summaryError={summaryError}
                    onRetrySummary={handleRetrySummary}
                    isRegistering={isRegistering}
                    registrationError={registrationError}
                    onCapacityRefresh={handleRefreshCapacity}
                    registrationParticipants={registrationParticipants}
                />
            </ThemedView>
        </MenuLayout>
    );
}
