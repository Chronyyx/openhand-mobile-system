import React, { useEffect, useState, useCallback } from 'react';
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
import { EventDetailModal } from '../../components/EventDetailModal';
import { getTranslatedEventTitle, getTranslatedEventDescription } from '../../utils/event-translations';
import { useCountdownTimer } from '../../hooks/useCountdownTimer';

import {
    getUpcomingEvents,
    getRegistrationSummary,
    getEventById,
    type EventSummary,
    type EventDetail,
    type RegistrationSummary,
} from '../../services/events.service';
import { registerForEvent, cancelRegistration, getMyRegistrations, type Registration } from '../../services/registration.service';
import { useAuth } from '../../context/AuthContext';

import { styles } from './events.styles';

export default function EventsScreen() {
    // Cast t to avoid type errors
    const { t } = useTranslation() as { t: (key: string, options?: any) => string };
    const { user, hasRole } = useAuth();

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

    // Registration User State
    const [userRegistration, setUserRegistration] = useState<Registration | null>(null);

    // Admin/Employee Stats
    const [registrationSummary, setRegistrationSummary] = useState<RegistrationSummary | null>(null);
    const [summaryLoading, setSummaryLoading] = useState(false);
    const [summaryError, setSummaryError] = useState<string | null>(null);

    // Success View State
    const [showSuccessView, setShowSuccessView] = useState(false);

    // Registration Request State - CRITICAL for preventing race conditions
    const [isRegistering, setIsRegistering] = useState(false);
    const [registrationError, setRegistrationError] = useState<string | null>(null);

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


    const loadEvents = useCallback(async () => {
        try {
            setError(null);
            const data = await getUpcomingEvents();
            setEvents(data);
            setFilteredEvents(data);
        } catch (e) {
            console.error('Failed to load events', e);
            setError(t('events.loadError'));
        } finally {
            setLoading(false);
            // setRefreshing(false); // Removed redundant call
        }
    }, [t]);

    useEffect(() => {
        loadEvents();
    }, [loadEvents]);

    // Search Filtering
    useEffect(() => {
        if (searchQuery.trim() === '') {
            setFilteredEvents(events);
        } else {
            const lowerQuery = searchQuery.toLowerCase().trim();
            const filtered = events.filter((event: EventSummary) => {
                const title = getTranslatedEventTitle(event, t).toLowerCase();
                const desc = (getTranslatedEventDescription(event, t) || '').toLowerCase();
                const addr = (event.address || '').toLowerCase();
                return title.includes(lowerQuery) || desc.includes(lowerQuery) || addr.includes(lowerQuery);
            });
            setFilteredEvents(filtered);
        }
    }, [searchQuery, events, t]);

    const onRefresh = async () => {
        setRefreshing(true);
        await loadEvents();
        setRefreshing(false);
    };

    const loadRegistrationSummary = async (eventId: number) => {
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
    };

    const checkUserRegistration = async (eventId: number) => {
        if (!user) return;
        try {
            const myRegs = await getMyRegistrations(user.token);
            const reg = myRegs.find((r: Registration) => r.eventId === eventId && r.status !== 'CANCELLED');
            setUserRegistration(reg || null);
        } catch (e) {
            console.error('Failed to check user registration', e);
        }
    };

    // We need to define closeEventModal first or hoist it, or use it in openEventModal safely.
    // openEventModal uses checkUserRegistration and loadRegistrationSummary

    const resetCountdownState = () => {
        resetCountdown();
    };

    const openEventModal = async (event: EventSummary) => {
        setSelectedEvent(event);
        setModalVisible(true);
        setDetailsLoading(true);
        setDetailsError(null);
        setShowSuccessView(false);
        resetCountdownState();

        // Initial check
        checkUserRegistration(event.id);

        // Load details
        try {
            // 1. Get Details
            const detail = await getEventById(event.id);
            setEventDetail(detail);

            // 2. Get Summary (Admin only)
            if (user && hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])) {
                loadRegistrationSummary(event.id);
            }
        } catch (err) {
            console.error('Failed to load event details', err);
            setDetailsError(t('events.errors.detailsFailed'));
        } finally {
            setDetailsLoading(false);
        }
    };

    const closeEventModal = () => {
        setModalVisible(false);
        setSelectedEvent(null);
        setEventDetail(null);
        setRegistrationSummary(null);
        setUserRegistration(null);
        setShowSuccessView(false);
        setIsRegistering(false);
        setRegistrationError(null);
    };

    /**
     * RACE CONDITION PREVENTION: Request Debouncing
     * 
     * This handler implements client-side request debouncing to prevent race conditions.
     * Even though the backend has pessimistic locking, we prevent unnecessary concurrent
     * requests from the client side by:
     * 
     * 1. Setting isRegistering=true immediately, which disables the register button
     * 2. Only allowing one registration request at a time
     * 3. Properly handling HTTP 409 conflicts from concurrent attempts
     * 
     * Flow:
     * - User clicks register → isRegistering becomes true → button disabled
     * - Backend locks event and atomically checks capacity
     * - If successful: show success view
     * - If 409 conflict (event full): show waitlist message
     * - If other error: show error alert
     */
    const handleRegister = async () => {
        if (!selectedEvent || !user || isRegistering) return;

        try {
            setIsRegistering(true);
            setRegistrationError(null);

            const newReg = await registerForEvent(selectedEvent.id, user.token);
            
            if (newReg.status === 'CONFIRMED') {
                setUserRegistration(newReg);
                setShowSuccessView(true);
                startCountdown();
            } else if (newReg.status === 'WAITLISTED') {
                setRegistrationError(
                    t('events.errors.eventFull',
                        `L'événement est complet. Vous êtes en position ${newReg.waitlistedPosition} sur la liste d'attente.`)
                );
                setUserRegistration(newReg);
            }

            // Refresh list
            onRefresh();

            // Refresh summary if admin
            if (hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])) {
                loadRegistrationSummary(selectedEvent.id);
            }

        } catch (e: any) {
            setIsRegistering(false);

            // Handle different error scenarios
            if (e.status === 409) {
                // HTTP 409 Conflict - either already registered or capacity exceeded
                const errorMessage = e.errorData?.message || e.message;

                if (errorMessage.includes('capacity')) {
                    // Event reached capacity - user placed on waitlist
                    setRegistrationError(
                        t('events.errors.eventFull',
                            'L\'événement est complet. Vous avez été ajouté(e) à la liste d\'attente.')
                    );
                } else if (errorMessage.includes('Already Registered')) {
                    // User already has active registration
                    setRegistrationError(
                        t('events.errors.alreadyRegistered',
                            'Vous êtes déjà inscrit(e) à cet événement.')
                    );
                } else {
                    setRegistrationError(errorMessage);
                }

                // Refresh registrations to show current state
                const myRegs = await getMyRegistrations(user.token);
                const reg = myRegs.find((r: Registration) => r.eventId === selectedEvent.id && r.status !== 'CANCELLED');
                setUserRegistration(reg || null);
            } else {
                // Other errors (network, server, etc.)
                const errorMessage = e.errorData?.message || e.message;
                setRegistrationError(
                    t('events.errors.registrationFailed',
                        'Inscription échouée. Veuillez réessayer.')
                );
                console.error('Registration error:', errorMessage);
            }
        } finally {
            setIsRegistering(false);
        }
    };

    const handleUnregister = async () => {
        if (!selectedEvent || !userRegistration || !user) return;
        try {
            setIsRegistering(true);
            setRegistrationError(null);

            await cancelRegistration(selectedEvent.id, user.token);

            // Manually update selectedEvent state to reflect changes immediately
            const updatedEvent = {
                ...selectedEvent,
                currentRegistrations: (selectedEvent.currentRegistrations || 0) - 1
            };

            // Client-side status update
            if (updatedEvent.maxCapacity) {
                if (updatedEvent.currentRegistrations < updatedEvent.maxCapacity) {
                    // Check if it should be NEARLY_FULL or OPEN
                    // Logic: If >= 80%, NEARLY_FULL, else OPEN
                    // Also handle if it WAS Full, now it might be NEARLY_FULL or OPEN
                    updatedEvent.status = updatedEvent.currentRegistrations >= updatedEvent.maxCapacity * 0.8
                        ? 'NEARLY_FULL'
                        : 'OPEN';
                }
                // Implicitly, if it's still >= maxCapacity (impossible if we just decremented), it stays FULL?
                // But we decremented, so it MUST be < maxCapacity if it was FULL (unless capacity was 0?)
                // Just to be safe, if for some reason current >= max, it should be FULL
                if (updatedEvent.currentRegistrations >= updatedEvent.maxCapacity) {
                    updatedEvent.status = 'FULL';
                }
            }
            setSelectedEvent(updatedEvent);

            // Also update eventDetail if it exists
            if (eventDetail && eventDetail.id === selectedEvent.id) {
                const updatedDetail = {
                    ...eventDetail,
                    currentRegistrations: (eventDetail.currentRegistrations || 0) - 1
                };
                if (updatedDetail.maxCapacity) {
                    if (updatedDetail.currentRegistrations < updatedDetail.maxCapacity) {
                        updatedDetail.status = updatedDetail.currentRegistrations >= updatedDetail.maxCapacity * 0.8
                            ? 'NEARLY_FULL'
                            : 'OPEN';
                    } else {
                        updatedDetail.status = 'FULL';
                    }
                }
                setEventDetail(updatedDetail as any);
            }

            setUserRegistration(null);
            setShowSuccessView(false); // If undoing
            resetCountdown();

            Alert.alert(t('events.success.unregistered'));
            onRefresh();
            // Refresh summary if admin
            if (hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])) {
                loadRegistrationSummary(selectedEvent.id);
            }
        } catch (e) {
            console.error(e);
            setRegistrationError(
                t('events.errors.unregisterFailed',
                    'Annulation échouée. Veuillez réessayer.')
            );
        } finally {
            setIsRegistering(false);
        }
    };

    const renderEventItem = ({ item }: ListRenderItemInfo<EventSummary>) => {
        return (
            <EventCard
                event={item}
                onPress={openEventModal}
                t={t}
            />
        );
    };

    return (
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
                        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#0056A8']} />
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

                // Registration
                userRegistration={userRegistration}
                onRegister={handleRegister}
                onUnregister={handleUnregister}

                // Success
                showSuccessView={showSuccessView}
                countdownValue={countdown}
                countdownSeconds={countdownSeconds}

                // Stats
                registrationSummary={registrationSummary}
                summaryLoading={summaryLoading}
                summaryError={summaryError}
                onRetrySummary={() => {
                    if (!selectedEvent) return;
                    // Refresh event details to update capacity immediately
                    getEventById(selectedEvent.id)
                        .then(setEventDetail)
                        .catch(() => {});
                    // Refresh registration summary for admin/employee
                    loadRegistrationSummary(selectedEvent.id);
                }}

                // Race Condition Prevention Props
                isRegistering={isRegistering}
                registrationError={registrationError}
                onCapacityRefresh={() => {
                    if (!selectedEvent) return;
                    // Fetch latest event details to reflect capacity change right away
                    getEventById(selectedEvent.id)
                        .then(setEventDetail)
                        .catch(() => {});
                }}
            />
        </ThemedView>
    );
}
