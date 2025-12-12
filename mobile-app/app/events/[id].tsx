import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    ListRenderItemInfo,
    RefreshControl,
    StyleSheet,
    View,
    Pressable,
    Modal,
    Image,
    Alert,
    ScrollView,
    type ImageSourcePropType,
} from 'react-native';
import { useTranslation } from 'react-i18next';

import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import { RegistrationSummaryComponent } from '../../components/registration-summary';
import {
    getUpcomingEvents,
    getEventById,
    getRegistrationSummary,
    type EventSummary,
    type EventDetail,
    type RegistrationSummary,
} from '../../services/events.service';
import { registerForEvent } from '../../services/registration.service';
import { useAuth } from '../../context/AuthContext';

const eventImages: Record<string, ImageSourcePropType> = {
    'Gala de reconnaissance MANA': require('../../assets/mana/Gala_image_Mana.png'),
    'Distribution Alimentaire - Mardi':
        require('../../assets/mana/boutiqueSolidaire_Mana.png'),
    'Formation MANA – Médiateur interculturel':
        require('../../assets/mana/Interculturelle_Mana.png'),
};

function getEventImage(event: EventSummary | null): ImageSourcePropType | undefined {
    if (!event) return undefined;
    return eventImages[event.title];
}

function formatDate(iso: string) {
    const match = iso.match(/^(\d{4}-\d{2}-\d{2})/);
    if (match) return match[1];
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return iso;
    return date.toISOString().slice(0, 10);
}

function formatTimeRange(startIso: string, endIso: string | null) {
    const startMatch = startIso.match(/T(\d{2}:\d{2})/);
    const endMatch = endIso?.match(/T(\d{2}:\d{2})/) ?? null;

    const startStr = startMatch?.[1] ?? startIso;
    const endStr = endMatch?.[1] ?? '';

    return endStr ? `${startStr} - ${endStr}` : startStr;
}

function getStatusLabel(status: EventSummary['status'], t: (key: string, defaultValue: string) => string) {
    return t(`events.status.${status}`, status);
}

function getStatusColor(status: EventSummary['status']): string {
    switch (status) {
        case 'OPEN':
            return '#4CAF50'; // Green
        case 'NEARLY_FULL':
            return '#F57C00'; // Orange
        case 'FULL':
            return '#D32F2F'; // Red
        default:
            return '#0057B8'; // Default blue
    }
}

export default function EventsScreen() {
    const { t } = useTranslation();
    const [events, setEvents] = useState<EventSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // modal state
    const [selectedEvent, setSelectedEvent] = useState<EventSummary | null>(null);
    const [modalVisible, setModalVisible] = useState(false);
    const [eventDetail, setEventDetail] = useState<EventDetail | null>(null);
    const [modalLoading, setModalLoading] = useState(false);
    const [modalError, setModalError] = useState<string | null>(null);

    // Registration summary state
    const [registrationSummary, setRegistrationSummary] = useState<RegistrationSummary | null>(null);
    const [summaryLoading, setSummaryLoading] = useState(false);
    const [summaryError, setSummaryError] = useState<string | null>(null);

    // Auth context for checking if user can register
    const { user, hasRole } = useAuth();

    // Debug: Log user info when component loads
    useEffect(() => {
        console.log('[EventDetail] User:', user);
        console.log('[EventDetail] Has ROLE_MEMBER:', hasRole(['ROLE_MEMBER']));
        console.log('[EventDetail] eventDetail:', eventDetail);
        console.log('[EventDetail] eventDetail.status:', eventDetail?.status);
        console.log('[EventDetail] Show register button?', user && hasRole(['ROLE_MEMBER']) && eventDetail?.status !== 'FULL');
    }, [user, hasRole, eventDetail]);

    const loadEvents = async () => {
        try {
            setError(null);
            const data = await getUpcomingEvents();
            setEvents(data);
        } catch (e) {
            console.error('Failed to load events', e);
            setError(
                e instanceof Error
                    ? e.message
                    : 'Impossible de charger les événements.',
            );
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    useEffect(() => {
        loadEvents();
    }, []);

    const onRefresh = () => {
        setRefreshing(true);
        loadEvents();
    };

    const loadRegistrationSummary = async (eventId: number) => {
        try {
            setSummaryError(null);
            setSummaryLoading(true);
            const summary = await getRegistrationSummary(eventId);
            setRegistrationSummary(summary);
        } catch (e) {
            console.error('Failed to load registration summary', e);
            setSummaryError(
                e instanceof Error
                    ? e.message
                    : 'Impossible de charger le résumé des inscriptions',
            );
        } finally {
            setSummaryLoading(false);
        }
    };

    const openEventModal = async (event: EventSummary) => {
        console.log('[EventDetail] Opening modal for event:', event);
        setSelectedEvent(event);
        setModalVisible(true);
        setModalLoading(true);
        setModalError(null);
        setEventDetail(null);

        // Reset registration summary state
        setRegistrationSummary(null);
        setSummaryLoading(false);
        setSummaryError(null);

        try {
            // Fetch full event details from backend instead of using list data
            const detail = await getEventById(event.id);
            console.log('[EventDetail] Loaded event details:', detail);
            setEventDetail(detail);
        } catch (e) {
            console.error('Failed to load event details', e);
            setModalError(
                e instanceof Error
                    ? e.message
                    : "Impossible de charger les détails de l'événement",
            );
        } finally {
            setModalLoading(false);
        }

        // Load registration summary in parallel
        loadRegistrationSummary(event.id);
    };

    const closeEventModal = () => {
        setModalVisible(false);
        setSelectedEvent(null);
        setEventDetail(null);
        setModalError(null);
        setRegistrationSummary(null);
        setSummaryError(null);
    };

    const handleRegister = async () => {
        if (!user || !eventDetail) return;

        try {
            const registration = await registerForEvent(eventDetail.id, user.token);

            // Show success message based on status
            if (registration.status === 'CONFIRMED') {
                Alert.alert(
                    'Inscription confirmée! ✅',
                    `Vous êtes inscrit(e) à l'événement "${eventDetail.title}".`,
                    [{ text: 'OK' }]
                );
            } else if (registration.status === 'WAITLISTED') {
                Alert.alert(
                    'Ajouté à la liste d\'attente',
                    `L'événement est complet. Vous êtes en position ${registration.waitlistedPosition} sur la liste d'attente.`,
                    [{ text: 'OK' }]
                );
            }

            // Refresh event details to update capacity
            const updatedEvent = await getEventById(eventDetail.id);
            setEventDetail(updatedEvent);

            // Refresh registration summary
            loadRegistrationSummary(eventDetail.id);

        } catch (error) {
            console.error('Registration failed', error);
            const errorMessage = error instanceof Error ? error.message : 'Inscription échouée';

            // Handle specific error cases
            if (errorMessage.includes('409')) {
                Alert.alert(
                    'Déjà inscrit',
                    'Vous êtes déjà inscrit(e) à cet événement.',
                    [{ text: 'OK' }]
                );
            } else if (errorMessage.includes('400')) {
                Alert.alert(
                    'Événement complet',
                    'Cet événement est complet et n\'accepte plus d\'inscriptions.',
                    [{ text: 'OK' }]
                );
            } else {
                Alert.alert(
                    'Erreur',
                    'Impossible de compléter l\'inscription. Veuillez réessayer.',
                    [{ text: 'OK' }]
                );
            }
        }
    };

    // ---- Loading / error / empty states ----
    if (loading) {
        return (
            <ThemedView style={styles.centered}>
                <ActivityIndicator />
                <ThemedText style={styles.loadingText}>
                    Chargement des événements...
                </ThemedText>
            </ThemedView>
        );
    }

    if (error) {
        return (
            <ThemedView style={styles.centered}>
                <ThemedText style={styles.errorText}>{error}</ThemedText>
            </ThemedView>
        );
    }

    if (events.length === 0) {
        return (
            <ThemedView style={styles.centered}>
                <ThemedText style={styles.emptyText}>
                    Aucun événement à venir pour le moment.
                </ThemedText>
            </ThemedView>
        );
    }

    // ---- Main list + modal ----
    return (
        <ThemedView style={styles.container}>
            <ThemedText type="title" style={styles.screenTitle}>
                ÉVÉNEMENTS DISPONIBLES
            </ThemedText>

            <FlatList<EventSummary>
                data={events}
                keyExtractor={(item) => item.id.toString()}
                renderItem={({ item }: ListRenderItemInfo<EventSummary>) => (
                    <Pressable
                        style={styles.card}
                        onPress={() => openEventModal(item)}
                        accessibilityRole="button"
                        accessibilityLabel={`Voir détails, ${item.title}`}
                    >
                        <View style={styles.cardHeader}>
                            <ThemedText type="subtitle" style={styles.eventTitle}>
                                {item.title}
                            </ThemedText>
                            <View style={[styles.statusBadge, { backgroundColor: getStatusColor(item.status) }]}>
                                <ThemedText style={styles.statusText}>
                                    {getStatusLabel(item.status, t)}
                                </ThemedText>
                            </View>
                        </View>

                        <View style={styles.row}>
                            <ThemedText style={styles.label}>Date</ThemedText>
                            <ThemedText style={styles.value}>
                                {formatDate(item.startDateTime)}
                            </ThemedText>
                        </View>

                        <View style={styles.row}>
                            <ThemedText style={styles.label}>Heure</ThemedText>
                            <ThemedText style={styles.value}>
                                {formatTimeRange(item.startDateTime, item.endDateTime)}
                            </ThemedText>
                        </View>

                        <View style={styles.row}>
                            <ThemedText style={styles.label}>Lieu</ThemedText>
                            <ThemedText style={styles.value}>{item.address}</ThemedText>
                        </View>

                        {item.maxCapacity != null &&
                            item.currentRegistrations != null && (
                                <View style={styles.row}>
                                    <ThemedText style={styles.label}>Capacité</ThemedText>
                                    <ThemedText style={styles.value}>
                                        {item.currentRegistrations}/{item.maxCapacity}
                                    </ThemedText>
                                </View>
                            )}

                        <View style={styles.footerButton}>
                            <ThemedText style={styles.footerButtonText}>
                                Voir détails
                            </ThemedText>
                        </View>
                    </Pressable>
                )}
                contentContainerStyle={styles.listContent}
                refreshControl={
                    <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
                }
            />

            {/* ---- Event Detail Modal ---- */}
            <Modal
                visible={modalVisible && !!selectedEvent}
                animationType="slide"
                transparent
                onRequestClose={closeEventModal}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalCard}>
                        {/* Header with image + title */}
                        <View style={styles.modalHeader}>
                            {selectedEvent && getEventImage(selectedEvent) && (
                                <Image
                                    source={getEventImage(selectedEvent)!}
                                    style={styles.modalImage}
                                    resizeMode="contain"
                                />
                            )}
                            <ThemedText type="title" style={styles.modalTitle}>
                                {selectedEvent?.title}
                            </ThemedText>
                        </View>

                        <ScrollView style={styles.modalBody} contentContainerStyle={{ paddingBottom: 16 }}>
                            {/* Loading state while fetching event details */}
                            {modalLoading && (
                                <View style={styles.modalLoadingContainer}>
                                    <ActivityIndicator size="large" color="#0057B8" />
                                    <ThemedText style={styles.modalLoadingText}>
                                        Chargement des détails...
                                    </ThemedText>
                                </View>
                            )}

                            {/* Error state if event cannot be loaded */}
                            {modalError && !modalLoading && (
                                <View style={styles.modalErrorContainer}>
                                    <ThemedText style={styles.modalErrorText}>
                                        ❌ {modalError}
                                    </ThemedText>
                                    <ThemedText style={styles.modalErrorHint}>
                                        Veuillez réessayer plus tard.
                                    </ThemedText>
                                </View>
                            )}

                            {/* Event details - only show when loaded successfully */}
                            {!modalLoading && !modalError && eventDetail && (
                                <>
                                    <ThemedText style={styles.sectionTitle}>Description</ThemedText>
                                    <ThemedText>{eventDetail.description}</ThemedText>

                                    <View style={styles.modalRow}>
                                        <ThemedText style={styles.sectionTitle}>Date</ThemedText>
                                        <ThemedText>
                                            {formatDate(eventDetail.startDateTime)}
                                        </ThemedText>
                                    </View>

                                    <View style={styles.modalRow}>
                                        <ThemedText style={styles.sectionTitle}>Heure</ThemedText>
                                        <ThemedText>
                                            {formatTimeRange(
                                                eventDetail.startDateTime,
                                                eventDetail.endDateTime,
                                            )}
                                        </ThemedText>
                                    </View>

                                    <View style={styles.modalRow}>
                                        <ThemedText style={styles.sectionTitle}>Lieu</ThemedText>
                                        <ThemedText>{eventDetail.address}</ThemedText>
                                    </View>

                                    <View style={styles.modalRow}>
                                        <ThemedText style={styles.sectionTitle}>Statut</ThemedText>
                                        <View style={[styles.inlineStatusBadge, { backgroundColor: getStatusColor(eventDetail.status) }]}>
                                            <ThemedText style={styles.statusText}>
                                                {getStatusLabel(eventDetail.status, t)}
                                            </ThemedText>
                                        </View>
                                    </View>

                                    {eventDetail.maxCapacity != null &&
                                        eventDetail.currentRegistrations != null && (
                                            <View style={styles.modalRow}>
                                                <ThemedText style={styles.sectionTitle}>
                                                    Capacité restante
                                                </ThemedText>
                                                <ThemedText>
                                                    {eventDetail.currentRegistrations}/
                                                    {eventDetail.maxCapacity}
                                                </ThemedText>
                                            </View>
                                        )}

                                    {/* Registration Summary Component */}
                                    <RegistrationSummaryComponent
                                        summary={registrationSummary}
                                        loading={summaryLoading}
                                        error={summaryError}
                                        onRetry={() => loadRegistrationSummary(eventDetail.id)}
                                    />

                                    {/* Register button - visible for ROLE_MEMBER and ROLE_EMPLOYEE */}
                                    {user && hasRole(['ROLE_MEMBER', 'ROLE_EMPLOYEE']) && (
                                        <View style={{ gap: 8, marginTop: 16 }}>
                                            {eventDetail?.status === 'FULL' && (
                                                <ThemedText style={styles.infoText}>
                                                    L&apos;événement est complet : vous serez placé(e) sur liste d&apos;attente.
                                                </ThemedText>
                                            )}
                                            <Pressable
                                                style={styles.registerButton}
                                                onPress={handleRegister}
                                            >
                                                <ThemedText style={styles.registerButtonText}>
                                                    {eventDetail?.status === 'FULL'
                                                        ? '📝 Rejoindre la liste d\'attente'
                                                        : '📝 S\'inscrire à cet événement'}
                                                </ThemedText>
                                            </Pressable>
                                        </View>
                                    )}

                                    {/* Message for non-members/employees */}
                                    {!user && (
                                        <View style={styles.infoBox}>
                                            <ThemedText style={styles.infoText}>
                                                ℹ️ Connectez-vous pour vous inscrire
                                            </ThemedText>
                                        </View>
                                    )}
                                </>
                            )}
                        </ScrollView>

                        <Pressable
                            style={styles.modalCloseButton}
                            onPress={closeEventModal}
                        >
                            <ThemedText style={styles.modalCloseButtonText}>
                                Fermer
                            </ThemedText>
                        </Pressable>
                    </View>
                </View>
            </Modal>
        </ThemedView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        paddingHorizontal: 16,
        paddingTop: 16,
        paddingBottom: 24,
    },
    screenTitle: {
        marginBottom: 16,
        fontWeight: '700',
        textTransform: 'uppercase',
    },
    listContent: {
        paddingBottom: 16,
    },
    card: {
        backgroundColor: '#ffffff',
        borderRadius: 12,
        padding: 16,
        marginBottom: 16,
        shadowColor: '#000',
        shadowOpacity: 0.08,
        shadowRadius: 6,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    cardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: 12,
    },
    eventTitle: {
        flex: 1,
        marginRight: 8,
        fontWeight: '700',
    },
    statusBadge: {
        backgroundColor: '#0057B8',
        borderRadius: 999,
        paddingHorizontal: 10,
        paddingVertical: 4,
        alignSelf: 'flex-start',
    },
    statusText: {
        color: '#ffffff',
        fontSize: 12,
        fontWeight: '600',
    },
    inlineStatusBadge: {
        borderRadius: 6,
        paddingHorizontal: 8,
        paddingVertical: 4,
        alignSelf: 'flex-start',
        marginTop: 4,
    },
    row: {
        flexDirection: 'row',
        marginBottom: 4,
    },
    label: {
        width: 70,
        fontWeight: '600',
        fontSize: 12,
    },
    value: {
        flex: 1,
        fontSize: 12,
    },
    footerButton: {
        marginTop: 12,
        alignSelf: 'stretch',
        backgroundColor: '#0057B8',
        borderRadius: 8,
        paddingVertical: 10,
        alignItems: 'center',
    },
    footerButtonText: {
        color: '#ffffff',
        fontWeight: '600',
        fontSize: 14,
    },
    debugBox: {
        marginTop: 12,
        padding: 10,
        borderWidth: 1,
        borderColor: '#d0d7de',
        borderRadius: 8,
        backgroundColor: '#f6f8fa',
    },
    debugTitle: {
        fontWeight: '700',
        marginBottom: 4,
    },
    debugItem: {
        fontSize: 13,
        color: '#4a5568',
    },
    centered: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 24,
    },
    loadingText: {
        marginTop: 12,
        fontSize: 14,
    },
    errorText: {
        textAlign: 'center',
        color: 'red',
    },
    emptyText: {
        textAlign: 'center',
    },

    // Modal styles
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.45)',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 16,
    },
    modalCard: {
        width: '100%',
        maxWidth: 420,
        maxHeight: '80%',
        borderRadius: 16,
        backgroundColor: '#ffffff',
        overflow: 'hidden',
    },
    modalHeader: {
        backgroundColor: '#0057B8',
        paddingHorizontal: 16,
        paddingTop: 16,
        paddingBottom: 12,
        flexDirection: 'row',
        alignItems: 'center',
    },
    modalImage: {
        width: 48,
        height: 48,
        marginRight: 12,
    },
    modalTitle: {
        flex: 1,
        color: '#ffffff',
        fontWeight: '700',
    },
    modalBody: {
        flex: 1,
        paddingHorizontal: 16,
        paddingVertical: 16,
    },
    sectionTitle: {
        fontWeight: '700',
        marginBottom: 4,
    },
    modalRow: {
        marginTop: 8,
    },
    modalCloseButton: {
        backgroundColor: '#E53935',
        paddingVertical: 12,
        alignItems: 'center',
    },
    modalCloseButtonText: {
        color: '#ffffff',
        fontWeight: '600',
    },
    modalLoadingContainer: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 32,
    },
    modalLoadingText: {
        marginTop: 12,
        fontSize: 14,
        color: '#666',
    },
    modalErrorContainer: {
        backgroundColor: '#FFEBEE',
        borderRadius: 8,
        padding: 16,
        marginVertical: 8,
    },
    modalErrorText: {
        color: '#D32F2F',
        fontSize: 14,
        fontWeight: '600',
        marginBottom: 4,
    },
    modalErrorHint: {
        color: '#666',
        fontSize: 12,
    },
    registerButton: {
        backgroundColor: '#4CAF50',
        borderRadius: 8,
        paddingVertical: 14,
        paddingHorizontal: 20,
        marginTop: 16,
        alignItems: 'center',
        shadowColor: '#000',
        shadowOpacity: 0.1,
        shadowRadius: 4,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    registerButtonText: {
        color: '#ffffff',
        fontSize: 16,
        fontWeight: '700',
    },
    infoBox: {
        backgroundColor: '#E3F2FD',
        borderRadius: 8,
        padding: 12,
        marginTop: 16,
        borderLeftWidth: 4,
        borderLeftColor: '#2196F3',
    },
    infoText: {
        color: '#1565C0',
        fontSize: 13,
    },
});
