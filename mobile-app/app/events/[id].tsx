import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    ListRenderItemInfo,
    RefreshControl,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { useLocalSearchParams } from 'expo-router';

import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import { EventCard } from '../../components/EventCard';
import { EventDetailModal } from '../../components/EventDetailModal';
import { MenuLayout } from '../../components/menu-layout';

import {
    getUpcomingEvents,
    getEventById,
    getRegistrationSummary,
    type EventSummary,
    type EventDetail,
    type RegistrationSummary,
} from '../../services/events.service';
import { registerForEvent, getMyRegistrations, cancelRegistration, type Registration } from '../../services/registration.service';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import { getStyles } from '../../styles/events.styles';
import { useCountdownTimer } from '../../hooks/useCountdownTimer';

export default function EventsDetailScreen() {
    const { id } = useLocalSearchParams();
    const { t } = useTranslation();
    const { user, hasRole, isLoading } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const styles = getStyles(colorScheme);

    // Data State
    const [events, setEvents] = useState<EventSummary[]>([]);
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
    const [myRegistrations, setMyRegistrations] = useState<Registration[]>([]);
    const [userRegistration, setUserRegistration] = useState<Registration | null>(null);

    // Registration Request State - CRITICAL for preventing race conditions
    const [isRegistering, setIsRegistering] = useState(false);
    const [registrationError, setRegistrationError] = useState<string | null>(null);

    // Admin/Employee Stats
    const [registrationSummary, setRegistrationSummary] = useState<RegistrationSummary | null>(null);
    const [summaryLoading, setSummaryLoading] = useState(false);
    const [summaryError, setSummaryError] = useState<string | null>(null);

    // Success View State
    const [showSuccessView, setShowSuccessView] = useState(false);

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

    const loadEvents = async () => {
        try {
            setError(null);
            if (!refreshing) setLoading(true);
            const data = await getUpcomingEvents();
            setEvents(data.filter((event) => event.status !== 'COMPLETED'));
        } catch (err) {
            console.error('Failed to load events', err);
            setError(t('events.errors.loadFailed'));
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    const loadMyRegistrations = async () => {
        if (!user) return;
        try {
            const regs = await getMyRegistrations(user.token);
            setMyRegistrations(regs);
        } catch (err) {
            console.error('Failed to load registrations', err);
        }
    };

    function onRefresh() {
        setRefreshing(true);
        loadEvents();
        if (user) loadMyRegistrations();
    }

    function checkUserRegistration(eventId: number) {
        const reg = myRegistrations.find((r: Registration) => r.eventId === eventId && r.status !== 'CANCELLED');
        setUserRegistration(reg || null);
    }

    async function openEventModal(event: EventSummary) {
        setSelectedEvent(event);
        setModalVisible(true);
        setDetailsLoading(true);
        setDetailsError(null);
        setShowSuccessView(false);
        resetCountdown();
        checkUserRegistration(event.id);

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
    }

    function closeEventModal() {
        setModalVisible(false);
        setSelectedEvent(null);
        setEventDetail(null);
        setRegistrationSummary(null);
        setUserRegistration(null);
        setShowSuccessView(false);
        setIsRegistering(false);
        setRegistrationError(null);
    }

    useEffect(() => {
        loadEvents();
        if (user) {
            loadMyRegistrations();
        }
    }, [user]);

    // Handle Deep Link or Initial ID
    useEffect(() => {
        if (id && events.length > 0) {
            const eventId = typeof id === 'string' ? parseInt(id, 10) : parseInt(id[0], 10);
            const found = events.find((e: EventSummary) => e.id === eventId);
            if (found) {
                openEventModal(found);
            }
        }
    }, [id, events]);

    // Check overlaps when selectedEvent changes
    useEffect(() => {
        if (selectedEvent && myRegistrations.length > 0) {
            checkUserRegistration(selectedEvent.id);
        }
    }, [selectedEvent, myRegistrations]);


    const loadRegistrationSummary = async (eventId: number) => {
        setSummaryLoading(true);
        setSummaryError(null);
        try {
            const summary = await getRegistrationSummary(eventId);
            setRegistrationSummary(summary);
        } catch (err) {
            setSummaryError(t('events.errors.summaryFailed')); // Use correct key or generic
        } finally {
            setSummaryLoading(false);
        }
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
        if (!selectedEvent || !user || isRegistering || isLoading) return;
        if (selectedEvent.status === 'COMPLETED') {
            setRegistrationError(t('events.errors.eventCompleted'));
            return;
        }

        try {
            setIsRegistering(true);
            setRegistrationError(null);

            const newReg = await registerForEvent(selectedEvent.id, user.token);

            // Success - registration confirmed or added to waitlist
            const updatedRegs = await getMyRegistrations(user.token);
            setMyRegistrations(updatedRegs);
            setUserRegistration(newReg);
            setShowSuccessView(true);
            startCountdown();

            if (hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])) {
                loadRegistrationSummary(selectedEvent.id);
            }
        } catch (err: any) {
            setIsRegistering(false);

            // Handle different error scenarios
            if (err.status === 409) {
                // HTTP 409 Conflict - either already registered or capacity exceeded
                const errorMessage = err.errorData?.message || err.message;

                if (errorMessage.includes('capacity')) {
                    // Event reached capacity - user placed on waitlist
                    setRegistrationError(
                        t('events.errors.eventFull',
                            'L\'événement est complet. Vous avez été ajouté(e) à la liste d\'attente.')
                    );
                } else if (errorMessage.toLowerCase().includes('completed')) {
                    setRegistrationError(t('events.errors.eventCompleted'));
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
                const updatedRegs = await getMyRegistrations(user.token);
                setMyRegistrations(updatedRegs);
                checkUserRegistration(selectedEvent.id);
            } else {
                // Other errors (network, server, etc.)
                const errorMessage = err.errorData?.message || err.message;
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

            // Manually update selectedEvent state locally
            const updatedEvent = {
                ...selectedEvent,
                currentRegistrations: (selectedEvent.currentRegistrations || 0) - 1
            };
            // Simple client-side status update
            if (updatedEvent.maxCapacity) {
                if (updatedEvent.currentRegistrations < updatedEvent.maxCapacity) {
                    updatedEvent.status = updatedEvent.currentRegistrations >= updatedEvent.maxCapacity * 0.8
                        ? 'NEARLY_FULL'
                        : 'OPEN';
                }
            }
            setSelectedEvent(updatedEvent);

            const updatedRegs = await getMyRegistrations(user.token);
            setMyRegistrations(updatedRegs);
            setUserRegistration(null);
            setShowSuccessView(false);
            resetCountdown();

            if (hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])) {
                loadRegistrationSummary(selectedEvent.id);
            }
            alert(t('events.success.unregistered'));
        } catch (err) {
            console.error(err);
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

    let content = (
        <ThemedView style={styles.container}>
            <FlatList
                data={events}
                renderItem={renderEventItem}
                keyExtractor={(item: EventSummary) => item.id.toString()}
                contentContainerStyle={styles.listContent}
                refreshControl={
                    <RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={[colorScheme === 'dark' ? '#6AA9FF' : '#0056A8']} />
                }
                ListEmptyComponent={
                    <ThemedView style={styles.centered}>
                        <ThemedText style={styles.emptyText}>{t('events.empty')}</ThemedText>
                    </ThemedView>
                }
            />

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
                onRetrySummary={() => {
                    if (!selectedEvent) return;
                    // Refresh both details and summary to reflect capacity updates immediately
                    getEventById(selectedEvent.id)
                        .then(setEventDetail)
                        .catch(() => {});
                    loadRegistrationSummary(selectedEvent.id);
                }}
                isRegistering={isRegistering}
                registrationError={registrationError}
                onCapacityRefresh={() => {
                    if (!selectedEvent) return;
                    getEventById(selectedEvent.id)
                        .then(setEventDetail)
                        .catch(() => {});
                }}
            />
        </ThemedView>
    );

    if (loading) {
        content = (
            <ThemedView style={styles.centered}>
                <ActivityIndicator size="large" color={colorScheme === 'dark' ? '#6AA9FF' : '#0056A8'} />
                <ThemedText style={styles.loadingText}>{t('events.loading')}</ThemedText>
            </ThemedView>
        );
    }

    return (
        <MenuLayout>
            {content}
        </MenuLayout>
    );
}
