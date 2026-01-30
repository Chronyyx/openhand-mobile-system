import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    RefreshControl,
    View,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { useLocalSearchParams } from 'expo-router';

import { ThemedText } from '../../../components/themed-text';
import { ThemedView } from '../../../components/themed-view';
import { MenuLayout } from '../../../components/menu-layout';
import { useAuth } from '../../../context/AuthContext';
import {
    getEventAttendees,
    getEventById,
    type EventAttendee,
    type EventAttendeesResponse,
    type EventDetail,
} from '../../../services/events.service';
import { getTranslatedEventTitle } from '../../../utils/event-translations';
import { styles } from '../../../styles/events.styles';

export default function EventAttendeesScreen() {
    const { id } = useLocalSearchParams();
    const { t } = useTranslation();
    const { user, hasRole, isLoading } = useAuth();

    const eventId = useMemo(() => {
        if (typeof id === 'string') return parseInt(id, 10);
        if (Array.isArray(id) && id.length > 0) return parseInt(id[0], 10);
        return NaN;
    }, [id]);

    const [attendees, setAttendees] = useState<EventAttendeesResponse | null>(null);
    const [eventDetail, setEventDetail] = useState<EventDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const canView = user && hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']);

    const loadAttendees = useCallback(async () => {
        if (Number.isNaN(eventId)) {
            setError(t('events.attendees.error'));
            setLoading(false);
            setRefreshing(false);
            return;
        }
        if (!user?.token || isLoading) {
            setError(t('common.notAuthenticated'));
            setLoading(false);
            setRefreshing(false);
            return;
        }

        try {
            setError(null);
            if (!refreshing) setLoading(true);
            const [eventData, attendeeData] = await Promise.all([
                getEventById(eventId),
                getEventAttendees(eventId, user.token),
            ]);
            setEventDetail(eventData);
            setAttendees(attendeeData);
        } catch (err) {
            console.error('Failed to load attendees', err);
            setError(t('events.attendees.error'));
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [eventId, refreshing, t, user?.token, isLoading]);

    useEffect(() => {
        if (canView && !isLoading) {
            loadAttendees();
        }
    }, [canView, loadAttendees, isLoading]);

    const onRefresh = useCallback(() => {
        setRefreshing(true);
        loadAttendees();
    }, [loadAttendees]);

    let content = null;

    if (!canView) {
        content = (
            <ThemedView style={styles.centered}>
                <ThemedText>{t('events.attendees.accessDenied')}</ThemedText>
            </ThemedView>
        );
    } else if (loading) {
        content = (
            <ThemedView style={styles.centered}>
                <ActivityIndicator size="large" color="#0056A8" />
                <ThemedText style={styles.loadingText}>{t('events.attendees.loading')}</ThemedText>
            </ThemedView>
        );
    } else if (error) {
        content = (
            <ThemedView style={styles.centered}>
                <ThemedText style={styles.errorText}>{error}</ThemedText>
                <ThemedText style={styles.footerButtonText} onPress={loadAttendees}>
                    {t('events.attendees.retry')}
                </ThemedText>
            </ThemedView>
        );
    } else {
        const list = attendees?.attendees ?? [];
        const totalAttendees = attendees?.totalAttendees ?? 0;
        const eventTitle = eventDetail ? getTranslatedEventTitle(eventDetail, t) : '';

        content = (
            <ThemedView style={styles.container}>
                <FlatList
                    data={list}
                    keyExtractor={(item: EventAttendee) => item.attendeeId.toString()}
                    renderItem={({ item }) => (
                        <View style={styles.attendeeCard}>
                            <ThemedText style={styles.attendeeName}>
                                {item.fullName || 'Unknown'}
                            </ThemedText>
                            <ThemedText style={styles.attendeeAge}>
                                {item.age != null
                                    ? `${t('events.attendees.ageLabel')}: ${item.age}`
                                    : t('events.attendees.ageUnknown')}
                            </ThemedText>
                        </View>
                    )}
                    contentContainerStyle={styles.listContent}
                    refreshControl={
                        <RefreshControl
                            refreshing={refreshing}
                            onRefresh={onRefresh}
                            colors={['#0056A8']}
                        />
                    }
                    ListHeaderComponent={
                        <View style={styles.attendeesHeader}>
                            <ThemedText type="title" style={styles.screenTitle}>
                                {t('events.attendees.title')}
                            </ThemedText>
                            {eventTitle ? (
                                <ThemedText style={styles.attendeesEventTitle}>{eventTitle}</ThemedText>
                            ) : null}
                            <ThemedText style={styles.attendeesCount}>
                                {t('events.attendees.total', { count: totalAttendees })}
                            </ThemedText>
                        </View>
                    }
                    ListEmptyComponent={
                        <ThemedText style={styles.emptyText}>
                            {t('events.attendees.empty')}
                        </ThemedText>
                    }
                />
            </ThemedView>
        );
    }

    return (
        <MenuLayout>
            {content}
        </MenuLayout>
    );
}
