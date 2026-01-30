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
import { EventDetailModal, type FamilyMemberInput } from '../../components/EventDetailModal';
import { MenuLayout } from '../../components/menu-layout';

import {
    getUpcomingEvents,
    getEventById,
    getRegistrationSummary,
    type EventSummary,
    type EventDetail,
    type RegistrationSummary,
} from '../../services/events.service';
import {
    registerForEventWithFamily,
    getMyRegistrations,
    cancelRegistration,
    type Registration,
    type FamilyMemberPayload,
    type GroupRegistrationResponse
} from '../../services/registration.service';
import { useAuth } from '../../context/AuthContext';
import { styles } from '../../styles/events.styles';
import { useCountdownTimer } from '../../hooks/useCountdownTimer';

export default function EventsDetailScreen() {
    const { id } = useLocalSearchParams();
    const { t } = useTranslation();
    const { user, hasRole } = useAuth();

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
    const [registrationParticipants, setRegistrationParticipants] = useState<GroupRegistrationResponse['participants'] | null>(null);

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
        setRegistrationParticipants(null);
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
    const handleRegister = async (familyMembers: FamilyMemberInput[]) => {
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

        try {
            setIsRegistering(true);
            setRegistrationError(null);

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

            const updatedRegs = await getMyRegistrations(user.token);
            setMyRegistrations(updatedRegs);

            if (primary?.status === 'CONFIRMED') {
                setShowSuccessView(true);
                startCountdown();
            } else if (primary?.status === 'WAITLISTED') {
                setRegistrationError(
                    t('alerts.registerWaitlistMessage', { position: primary.waitlistedPosition })
                );
            }

            if (hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])) {
                loadRegistrationSummary(selectedEvent.id);
            }
        } catch (err: any) {
            setIsRegistering(false);

            const errorMessage = err.errorData?.message || err.message;
            if (err.status === 403) {
                setRegistrationError(t('events.errors.accessDenied'));
            } else if (err.status === 400 && errorMessage.toLowerCase().includes('capacity')) {
                setRegistrationError(t('events.family.capacityError', 'Not enough capacity for your group.'));
            } else if (err.status === 409) {
                if (errorMessage.includes('capacity')) {
                    setRegistrationError(
                        t('events.errors.eventFull',
                            "L'?v?nement est complet. Vous avez ?t? ajout?(e) ? la liste d'attente.")
                    );
                } else if (errorMessage.toLowerCase().includes('completed')) {
                    setRegistrationError(t('events.errors.eventCompleted'));
                } else if (errorMessage.includes('Already Registered')) {
                    setRegistrationError(
                        t('events.errors.alreadyRegistered',
                            "Vous ?tes d?j? inscrit(e) ? cet ?v?nement.")
                    );
                } else {
                    setRegistrationError(errorMessage);
                }

                const updatedRegs = await getMyRegistrations(user.token);
                setMyRegistrations(updatedRegs);
            } else {
                setRegistrationError(t('events.errors.registrationFailed'));
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

            try {
                const refreshed = await getEventById(selectedEvent.id);
                setEventDetail(refreshed);
                setSelectedEvent(refreshed as EventSummary);
            } catch (err) {
                console.error('Failed to refresh event after unregister', err);
            }

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
                    <RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#0056A8']} />
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
                registrationParticipants={registrationParticipants}
            />
        </ThemedView>
    );

    if (loading) {
        content = (
            <ThemedView style={styles.centered}>
                <ActivityIndicator size="large" color="#0056A8" />
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
