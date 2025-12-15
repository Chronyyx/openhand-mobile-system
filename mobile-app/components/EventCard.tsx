import React from 'react';
import { View, Pressable } from 'react-native';
import { ThemedText } from './themed-text';
import { styles } from '../app/events/events.styles';
import { formatIsoDate, formatIsoTimeRange } from '../utils/date-time';
import { getTranslatedEventTitle } from '../utils/event-translations';
import { type EventSummary } from '../services/events.service';
import { getStatusLabel, getStatusColor, getStatusTextColor } from '../utils/event-status';

type EventCardProps = {
    event: EventSummary;
    onPress: (event: EventSummary) => void;
    t: (key: string, options?: any) => string;
};

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
