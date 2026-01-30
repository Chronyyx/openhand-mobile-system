import React from 'react';
import { View, Pressable, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { ThemedText } from './themed-text';
import { getStyles } from '../styles/events.styles';
import { formatIsoDate, formatIsoTimeRange } from '../utils/date-time';
import { getTranslatedEventTitle } from '../utils/event-translations';
import { type EventSummary } from '../services/events.service';
import { getStatusLabel, getStatusColor, getStatusTextColor } from '../utils/event-status';
import { useColorScheme } from '../hooks/use-color-scheme';

type EventCardProps = {
    event: EventSummary;
    onPress: (event: EventSummary) => void;
    t: (key: string, options?: any) => string;
    onClose?: () => void;
};

export function EventCard({ event, onPress, t, onClose }: EventCardProps) {
    const translatedTitle = getTranslatedEventTitle(event, t);
    const isCancelled = event.status === 'CANCELLED';
    const colorScheme = useColorScheme() ?? 'light';
    const globalStyles = getStyles(colorScheme);
    const closeIconColor = colorScheme === 'dark' ? '#A0A7B1' : '#666';
    const cancelledTextColor = colorScheme === 'dark' ? '#A0A7B1' : '#757575';

    return (
        <View style={[globalStyles.card, isCancelled && styles.cardCancelled, { position: 'relative' }]}>
            {isCancelled && onClose && (
                <Pressable
                    onPress={onClose}
                    style={styles.closeButton}
                    hitSlop={10}
                    accessibilityLabel="Hide cancelled event"
                >
                    <Ionicons name="close-circle" size={24} color={closeIconColor} />
                </Pressable>
            )}
            <Pressable
                onPress={() => !isCancelled && onPress(event)}
                style={({ pressed }) => [
                    pressed && !isCancelled && { opacity: 0.7 }
                ]}
                disabled={isCancelled}
            >
                <View style={[globalStyles.cardHeader, { paddingRight: isCancelled ? 24 : 0 }]}>
                    <ThemedText
                        type="subtitle"
                        style={[
                            globalStyles.eventTitle,
                            isCancelled && { textDecorationLine: 'line-through', color: cancelledTextColor }
                        ]}
                    >
                        {translatedTitle}
                    </ThemedText>
                    <View style={[globalStyles.statusBadge, { backgroundColor: getStatusColor(event.status) }]}>
                        <ThemedText style={[styles.statusText, { color: getStatusTextColor(event.status) }]}>
                            {getStatusLabel(event.status, t)}
                        </ThemedText>
                    </View>
                </View>

                <View style={globalStyles.row}>
                    <ThemedText style={globalStyles.label}>
                        {t('events.fields.date')}
                    </ThemedText>
                    <ThemedText style={globalStyles.value}>
                        {formatIsoDate(event.startDateTime)}
                    </ThemedText>
                </View>

                <View style={globalStyles.row}>
                    <ThemedText style={globalStyles.label}>
                        {t('events.fields.time')}
                    </ThemedText>
                    <ThemedText style={globalStyles.value}>
                        {formatIsoTimeRange(event.startDateTime, event.endDateTime)}
                    </ThemedText>
                </View>

                <View style={globalStyles.row}>
                    <ThemedText style={globalStyles.label}>
                        {t('events.fields.place')}
                    </ThemedText>
                    <ThemedText style={globalStyles.value}>{event.address}</ThemedText>
                </View>

                {event.maxCapacity != null && event.currentRegistrations != null && (
                    <View style={globalStyles.row}>
                        <ThemedText style={globalStyles.label}>
                            {t('events.fields.capacity')}
                        </ThemedText>
                        <ThemedText style={globalStyles.value}>
                            {event.currentRegistrations}/{event.maxCapacity}
                        </ThemedText>
                    </View>
                )}

                {!isCancelled && (
                    <View style={globalStyles.footerButton}>
                        <ThemedText style={globalStyles.footerButtonText}>
                            {t('events.actions.viewDetails')}
                        </ThemedText>
                    </View>
                )}
            </Pressable>
        </View>
    );
}

const styles = StyleSheet.create({
    statusText: {
        fontWeight: '700',
        fontSize: 10,
    },
    closeButton: {
        position: 'absolute',
        top: 8,
        right: 8,
        zIndex: 10,
        padding: 4
    },
    cardCancelled: {
        opacity: 0.8,
    }
});
