import React from 'react';
import { View, Pressable } from 'react-native';
import { ThemedText } from './themed-text';
import { styles } from '../app/events/events.styles';
import { formatIsoDate, formatIsoTimeRange } from '../utils/date-time';
import { getTranslatedEventTitle } from '../utils/event-translations';
import { type EventSummary } from '../services/events.service';

type EventCardProps = {
    event: EventSummary;
    onPress: (event: EventSummary) => void;
    t: (key: string, options?: any) => string;
};

// --- Helper Functions duplicated from index (could be moved to a util) ---
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
            return '#E3F2FD'; // Light Blue
        case 'NEARLY_FULL':
            return '#F6B800'; // Yellow
        case 'FULL':
            return '#E0E0E0'; // Light Gray (Grayed out)
        default:
            return '#E3F2FD';
    }
}

function getStatusTextColor(status: EventSummary['status']): string {
    switch (status) {
        case 'OPEN':
            return '#0056A8'; // Blue Text
        case 'NEARLY_FULL':
            return '#333333'; // Dark Gray Text (Readable on Yellow)
        case 'FULL':
            return '#757575'; // Dark Gray Text (Disabled look)
        default:
            return '#0056A8';
    }
}

export function EventCard({ event, onPress, t }: EventCardProps) {
    const translatedTitle = getTranslatedEventTitle(event, t);

    return (
        <Pressable
            style={styles.card}
            onPress={() => onPress(event)}
            accessibilityRole="button"
            accessibilityLabel={t('events.accessibility.viewDetails', { title: translatedTitle })}
        >
            <View style={styles.cardHeader}>
                <ThemedText type="subtitle" style={styles.eventTitle}>
                    {translatedTitle}
                </ThemedText>
                <View style={[styles.statusBadge, { backgroundColor: getStatusColor(event.status) }]}>
                    <ThemedText style={[styles.statusText, { color: getStatusTextColor(event.status) }]}>
                        {getStatusLabel(event.status, t)}
                    </ThemedText>
                </View>
            </View>

            <View style={styles.row}>
                <ThemedText style={styles.label}>
                    {t('events.fields.date')}
                </ThemedText>
                <ThemedText style={styles.value}>
                    {formatIsoDate(event.startDateTime)}
                </ThemedText>
            </View>

            <View style={styles.row}>
                <ThemedText style={styles.label}>
                    {t('events.fields.time')}
                </ThemedText>
                <ThemedText style={styles.value}>
                    {formatIsoTimeRange(event.startDateTime, event.endDateTime)}
                </ThemedText>
            </View>

            <View style={styles.row}>
                <ThemedText style={styles.label}>
                    {t('events.fields.place')}
                </ThemedText>
                <ThemedText style={styles.value}>{event.address}</ThemedText>
            </View>

            {event.maxCapacity != null && event.currentRegistrations != null && (
                <View style={styles.row}>
                    <ThemedText style={styles.label}>
                        {t('events.fields.capacity')}
                    </ThemedText>
                    <ThemedText style={styles.value}>
                        {event.currentRegistrations}/{event.maxCapacity}
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
}
