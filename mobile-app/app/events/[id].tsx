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
    type ImageSourcePropType,
} from 'react-native';

import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import {
    getUpcomingEvents,
    type EventSummary,
} from '../../services/events.service';

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

function getStatusLabel(status: EventSummary['status']) {
    switch (status) {
        case 'OPEN':
            return 'OUVERT';
        case 'NEARLY_FULL':
            return 'PLACES LIMITÉES';
        case 'FULL':
            return 'COMPLET';
        default:
            return status;
    }
}

export default function EventsScreen() {
    const [events, setEvents] = useState<EventSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // modal state
    const [selectedEvent, setSelectedEvent] = useState<EventSummary | null>(null);
    const [modalVisible, setModalVisible] = useState(false);

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

    const openEventModal = (event: EventSummary) => {
        setSelectedEvent(event);
        setModalVisible(true);
    };

    const closeEventModal = () => {
        setModalVisible(false);
        setSelectedEvent(null);
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
                            <View style={styles.statusBadge}>
                                <ThemedText style={styles.statusText}>
                                    {getStatusLabel(item.status)}
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

                        <View style={styles.modalBody}>
                            <ThemedText style={styles.sectionTitle}>Description</ThemedText>
                            <ThemedText>{selectedEvent?.description}</ThemedText>

                            <View style={styles.modalRow}>
                                <ThemedText style={styles.sectionTitle}>Date</ThemedText>
                                <ThemedText>
                                    {selectedEvent && formatDate(selectedEvent.startDateTime)}
                                </ThemedText>
                            </View>

                            <View style={styles.modalRow}>
                                <ThemedText style={styles.sectionTitle}>Heure</ThemedText>
                                <ThemedText>
                                    {selectedEvent &&
                                        formatTimeRange(
                                            selectedEvent.startDateTime,
                                            selectedEvent.endDateTime,
                                        )}
                                </ThemedText>
                            </View>

                            <View style={styles.modalRow}>
                                <ThemedText style={styles.sectionTitle}>Lieu</ThemedText>
                                <ThemedText>{selectedEvent?.address}</ThemedText>
                            </View>

                            {selectedEvent?.maxCapacity != null &&
                                selectedEvent.currentRegistrations != null && (
                                    <View style={styles.modalRow}>
                                        <ThemedText style={styles.sectionTitle}>Capacité</ThemedText>
                                        <ThemedText>
                                            {selectedEvent.currentRegistrations}/
                                            {selectedEvent.maxCapacity}
                                        </ThemedText>
                                    </View>
                                )}
                        </View>

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
});
