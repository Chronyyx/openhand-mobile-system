import React, { useEffect, useState, useCallback } from 'react';
import {
    ActivityIndicator,
    FlatList,
    RefreshControl,
    StyleSheet,
    View,
    Pressable,
    Modal,
    Image,
    Alert,
    ScrollView,
    TextInput,
    type ImageSourcePropType,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';

import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import { RegistrationSummaryComponent } from '../../components/registration-summary';
import {
    getUpcomingEvents,
    getRegistrationSummary,
    type EventSummary,
    type RegistrationSummary,
} from '../../services/events.service';
import { registerForEvent, cancelRegistration } from '../../services/registration.service';
import { useAuth } from '../../context/AuthContext';

// ---- Static image map for events ----
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

// ---- Map backend title ➜ translation key slug ----
const eventTranslationKeys: Record<string, string> = {
    'Gala de reconnaissance MANA': 'gala',
    'Distribution Alimentaire - Mardi': 'distribution_mardi',
    'Formation MANA – Médiateur interculturel': 'formation_mediateur',
};

function getTranslatedTitle(
    event: EventSummary,
    t: (key: string, options?: any) => string,
) {
    const slug = eventTranslationKeys[event.title];
    if (!slug) return event.title;
    return t(`events.names.${slug}`, { defaultValue: event.title });
}

function getTranslatedDescription(
    event: EventSummary,
    t: (key: string, options?: any) => string,
) {
    const slug = eventTranslationKeys[event.title];
    if (!slug) return event.description;
    return t(`events.descriptions.${slug}`, { defaultValue: event.description });
}

function formatDate(iso: string) {
    const date = new Date(iso);
    return date.toLocaleDateString('fr-CA', {
        day: '2-digit',
        month: 'long',
        year: 'numeric',
    });
}

function formatTimeRange(startIso: string, endIso: string | null) {
    const start = new Date(startIso);
    const end = endIso ? new Date(endIso) : null;

    const startStr = start.toLocaleTimeString('fr-CA', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
    });

    const endStr = end
        ? end.toLocaleTimeString('fr-CA', {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false,
        })
        : '';

    return endStr ? `${startStr} - ${endStr}` : startStr;
}

function getStatusLabel(status: EventSummary['status'], t: (k: string) => string) {
    switch (status) {
        case 'OPEN':
            return t('events.status.OPEN');
        case 'NEARLY_FULL':
            return t('events.status.NEARLY_FULL');
        case 'FULL':
            return t('events.status.FULL');
        default:
            return status;
    }
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
    // super simple typing for t
    const { t } = useTranslation() as { t: (key: string, options?: any) => string };
    const { user, hasRole } = useAuth();

    const [events, setEvents] = useState<EventSummary[]>([]);
    const [filteredEvents, setFilteredEvents] = useState<EventSummary[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // modal state
    const [selectedEvent, setSelectedEvent] = useState<EventSummary | null>(null);
    const [modalVisible, setModalVisible] = useState(false);

        // Registration summary state
        const [registrationSummary, setRegistrationSummary] = useState<RegistrationSummary | null>(null);
        const [summaryLoading, setSummaryLoading] = useState(false);
        const [summaryError, setSummaryError] = useState<string | null>(null);

            const loadEvents = useCallback(async () => {
        try {
            setError(null);
            const data = await getUpcomingEvents();
            setEvents(data);
            setFilteredEvents(data);
        } catch (e) {
            console.error('Failed to load events', e);
            // store already-translated error text
            setError(t('events.loadError'));
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
            }, [t]);

    useEffect(() => {
        loadEvents();
        }, [loadEvents]);

    useEffect(() => {
        if (searchQuery.trim() === '') {
            setFilteredEvents(events);
        } else {
            const lowerQuery = searchQuery.toLowerCase().trim();
            const filtered = events.filter(event => {
                // Search against TRANSLATED title/description
                const translatedTitle = getTranslatedTitle(event, t).toLowerCase();
                const translatedDesc = getTranslatedDescription(event, t).toLowerCase();
                const category = (event.category || '').toLowerCase();

                return translatedTitle.includes(lowerQuery) ||
                    translatedDesc.includes(lowerQuery) ||
                    category.includes(lowerQuery);
            });
            setFilteredEvents(filtered);
        }
    }, [searchQuery, events, t]); // Add 't' to dependencies

    const onRefresh = async () => {
        setRefreshing(true);
        await loadEvents();
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
                    : t('events.registrationSummary.loadingError'),
            );
        } finally {
            setSummaryLoading(false);
        }
    };

    const handleEventPress = (event: EventSummary) => {
        setSelectedEvent(event);
        setModalVisible(true);
        // Load registration summary
        loadRegistrationSummary(event.id);
    };

    const closeModal = () => {
        setModalVisible(false);
        setSelectedEvent(null);
        setRegistrationSummary(null);
        setSummaryError(null);
    };

    const handleRegister = async () => {
        if (!selectedEvent || !user?.token) return;
        try {
            const registration = await registerForEvent(selectedEvent.id, user.token);
            
            // Show different alerts based on registration status
            if (registration.status === 'CONFIRMED') {
                Alert.alert(
                    t('alerts.registerSuccess'),
                    t('alerts.registerSuccessMessage')
                );
            } else if (registration.status === 'WAITLISTED') {
                Alert.alert(
                    t('alerts.registerWaitlistSuccess'),
                    t('alerts.registerWaitlistMessage', { position: registration.waitlistedPosition })
                );
            }
            
            closeModal();
            onRefresh();
        } catch (e) {
            console.error(e);
            Alert.alert(t('alerts.registerError'));
        }
    };

    const handleCancel = async () => {
        if (!selectedEvent || !user?.token) return;
        try {
            await cancelRegistration(selectedEvent.id, user.token);
            Alert.alert(t('alerts.cancelSuccess'));
            closeModal();
            onRefresh();
        } catch (e) {
            console.error(e);
            Alert.alert(t('alerts.cancelError'));
        }
    };

    if (loading) {
        return (
            <ThemedView style={styles.centered}>
                <ActivityIndicator />
                <ThemedText style={styles.loadingText}>{t('events.loading')}</ThemedText>
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

    return (
        <ThemedView style={styles.container}>
            <View style={styles.searchContainer}>
                <Ionicons name="search" size={20} color="#666" style={styles.searchIcon} />
                <TextInput
                    style={styles.searchInput}
                    placeholder={t('events.searchPlaceholder') || "Rechercher..."}
                    value={searchQuery}
                    onChangeText={setSearchQuery}
                    clearButtonMode="while-editing"
                />
                {searchQuery.length > 0 && (
                    <Pressable onPress={() => setSearchQuery('')} hitSlop={10}>
                        <Ionicons name="close-circle" size={20} color="#666" style={{ marginLeft: 8 }} />
                    </Pressable>
                )}
            </View>

            {events.length === 0 ? (
                <ThemedView style={styles.centered}>
                    <ThemedText style={styles.emptyText}>{t('events.empty')}</ThemedText>
                </ThemedView>
            ) : (
                <FlatList<EventSummary>
                    data={filteredEvents}
                    keyExtractor={(item) => item.id.toString()}
                    renderItem={({ item }) => {
                        const translatedTitle = getTranslatedTitle(item, t);

                        return (
                            <Pressable
                                style={styles.card}
                                onPress={() => handleEventPress(item)}
                                accessibilityRole="button"
                                accessibilityLabel={t(
                                    'events.accessibility.viewDetails',
                                    { title: translatedTitle },
                                )}
                            >
                                <View style={styles.cardHeader}>
                                    <ThemedText type="subtitle" style={styles.eventTitle}>
                                        {translatedTitle}
                                    </ThemedText>
                                    <View style={[styles.statusBadge, { backgroundColor: getStatusColor(item.status) }]}>
                                        <ThemedText style={styles.statusText}>
                                            {getStatusLabel(item.status, t)}
                                        </ThemedText>
                                    </View>
                                </View>

                                <View style={styles.row}>
                                    <ThemedText style={styles.label}>
                                        {t('events.fields.date')}
                                    </ThemedText>
                                    <ThemedText style={styles.value}>
                                        {formatDate(item.startDateTime)}
                                    </ThemedText>
                                </View>

                                <View style={styles.row}>
                                    <ThemedText style={styles.label}>
                                        {t('events.fields.time')}
                                    </ThemedText>
                                    <ThemedText style={styles.value}>
                                        {formatTimeRange(
                                            item.startDateTime,
                                            item.endDateTime,
                                        )}
                                    </ThemedText>
                                </View>

                                <View style={styles.row}>
                                    <ThemedText style={styles.label}>
                                        {t('events.fields.place')}
                                    </ThemedText>
                                    <ThemedText style={styles.value}>{item.address}</ThemedText>
                                </View>

                                {item.maxCapacity != null &&
                                    item.currentRegistrations != null && (
                                        <View style={styles.row}>
                                            <ThemedText style={styles.label}>
                                                {t('events.fields.capacity')}
                                            </ThemedText>
                                            <ThemedText style={styles.value}>
                                                {item.currentRegistrations}/{item.maxCapacity}
                                            </ThemedText>
                                        </View>
                                    )}

                                <View style={styles.footerButton}>
                                    <ThemedText style={styles.footerButtonText}>
                                        {t('events.actions.viewDetails')}
                                    </ThemedText>
                                </View>
                            </Pressable>
                        );
                    }}
                    contentContainerStyle={styles.listContent}
                    refreshControl={
                        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
                    }
                    ListEmptyComponent={
                        <ThemedView style={styles.centered}>
                            <ThemedText style={styles.emptyText}>
                                {searchQuery ? t('events.noResults') || "Aucun résultat trouvé." : t('events.empty')}
                            </ThemedText>
                        </ThemedView>
                    }
                />
            )}

            <Modal
                visible={modalVisible && !!selectedEvent}
                animationType="slide"
                transparent
                onRequestClose={closeModal}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalCard}>
                        {/* Header with bigger image + title */}
                        <View style={styles.modalHeader}>
                            {selectedEvent && getEventImage(selectedEvent) && (
                                <Image
                                    source={getEventImage(selectedEvent)!}
                                    style={styles.modalImage}
                                    resizeMode="cover"
                                />
                            )}
                            <ThemedText type="title" style={styles.modalTitle}>
                                {selectedEvent &&
                                    getTranslatedTitle(selectedEvent, t)}
                            </ThemedText>
                        </View>

                        <ScrollView style={styles.modalBody} contentContainerStyle={{ paddingBottom: 16 }}>
                            <ThemedText style={styles.sectionTitle}>
                                {t('events.fields.description')}
                            </ThemedText>
                            <ThemedText>
                                {selectedEvent &&
                                    getTranslatedDescription(selectedEvent, t)}
                            </ThemedText>

                            <View style={styles.modalRow}>
                                <ThemedText style={styles.sectionTitle}>
                                    {t('events.fields.date')}
                                </ThemedText>
                                <ThemedText>
                                    {selectedEvent &&
                                        formatDate(selectedEvent.startDateTime)}
                                </ThemedText>
                            </View>

                            <View style={styles.modalRow}>
                                <ThemedText style={styles.sectionTitle}>
                                    {t('events.fields.time')}
                                </ThemedText>
                                <ThemedText>
                                    {selectedEvent &&
                                        formatTimeRange(
                                            selectedEvent.startDateTime,
                                            selectedEvent.endDateTime,
                                        )}
                                </ThemedText>
                            </View>

                            <View style={styles.modalRow}>
                                <ThemedText style={styles.sectionTitle}>
                                    {t('events.fields.place')}
                                </ThemedText>
                                <ThemedText>{selectedEvent?.address}</ThemedText>
                            </View>

                            <View style={styles.modalRow}>
                                <ThemedText style={styles.sectionTitle}>{t('events.fields.status')}</ThemedText>
                                <View style={[styles.inlineStatusBadge, { backgroundColor: selectedEvent ? getStatusColor(selectedEvent.status) : '#0057B8' }]}>
                                    <ThemedText style={styles.statusText}>
                                        {selectedEvent && getStatusLabel(selectedEvent.status, t)}
                                    </ThemedText>
                                </View>
                            </View>

                            {selectedEvent?.maxCapacity != null &&
                                selectedEvent.currentRegistrations != null && (
                                    <View style={styles.modalRow}>
                                        <ThemedText style={styles.sectionTitle}>
                                            {t('events.fields.capacity')}
                                        </ThemedText>
                                        <ThemedText>
                                            {selectedEvent.currentRegistrations}/
                                            {selectedEvent.maxCapacity}
                                        </ThemedText>
                                    </View>
                                )}

                                {/* Registration Summary - only visible for ROLE_ADMIN and ROLE_EMPLOYEE */}
                                {selectedEvent && user && hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']) && (
                                    <RegistrationSummaryComponent
                                        loading={summaryLoading}
                                        error={summaryError}
                                        summary={registrationSummary}
                                        onRetry={() => loadRegistrationSummary(selectedEvent.id)}
                                    />
                                )}

                            {/* Register button - visible for ROLE_MEMBER and ROLE_EMPLOYEE */}
                            {user && hasRole(['ROLE_MEMBER', 'ROLE_EMPLOYEE']) && (
                                <Pressable
                                    style={styles.registerButton}
                                    onPress={handleRegister}
                                >
                                    <ThemedText style={styles.registerButtonText}>
                                        {selectedEvent?.status === 'FULL'
                                            ? t('events.actions.joinWaitlist')
                                            : t('events.actions.register')}
                                    </ThemedText>
                                </Pressable>
                            )}
                        </ScrollView>

                        <Pressable
                            style={styles.modalCloseButton}
                            onPress={closeModal}
                        >
                            <ThemedText style={styles.modalCloseButtonText}>
                                {t('events.actions.close')}
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
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.45)',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 16,
        paddingVertical: 40,
    },
    modalCard: {
        width: '100%',
        maxWidth: 420,
        maxHeight: '80%',
        borderRadius: 16,
        backgroundColor: '#ffffff',
        overflow: 'hidden',
        flexDirection: 'column',
    },
    modalHeader: {
        backgroundColor: '#0057B8',
        paddingHorizontal: 16,
        paddingTop: 16,
        paddingBottom: 16,
        flexDirection: 'row',
        alignItems: 'center',
    },
    modalImage: {
        width: 72,
        height: 72,
        marginRight: 16,
        borderRadius: 8,
        backgroundColor: '#ffffff',
    },
    modalTitle: {
        flex: 1,
        color: '#ffffff',
        fontWeight: '700',
        fontSize: 20,
        lineHeight: 24,
    },
    modalBody: {
        paddingHorizontal: 16,
        paddingVertical: 16,
        gap: 8,
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
    registerButton: {
        marginTop: 16,
        backgroundColor: '#22c55e',
        paddingVertical: 14,
        paddingHorizontal: 20,
        borderRadius: 8,
        alignItems: 'center',
    },
    registerButtonText: {
        color: '#ffffff',
        fontSize: 16,
        fontWeight: '600',
    },
    searchContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#fff',
        marginBottom: 16,
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: '#ddd',
    },
    searchIcon: {
        marginRight: 8,
    },
    searchInput: {
        flex: 1,
        fontSize: 16,
        color: '#333',
    },
});
