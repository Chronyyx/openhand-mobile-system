import React, { useCallback, useEffect, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    RefreshControl,
    StyleSheet,
    View,
} from 'react-native';
import { useTranslation } from 'react-i18next';

import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import { useAuth } from '../../context/AuthContext';
import {
    getMyRegistrations,
    type Registration,
} from '../../services/registration.service';
import { formatIsoDate, formatIsoTimeRange } from '../../utils/date-time';

type RegistrationWithConflict = Registration & { hasConflict: boolean };

function computeConflictIds(registrations: Registration[]): Set<number> {
    const conflictIds = new Set<number>();
    const parsed = registrations
        .filter((r) => r.status !== 'CANCELLED')
        .map((r) => ({
            id: r.id,
            start: r.eventStartDateTime ? Date.parse(r.eventStartDateTime) : NaN,
            end: r.eventEndDateTime ? Date.parse(r.eventEndDateTime) : NaN,
        }));

    for (let i = 0; i < parsed.length; i++) {
        const a = parsed[i];
        if (Number.isNaN(a.start) || Number.isNaN(a.end)) continue;

        for (let j = i + 1; j < parsed.length; j++) {
            const b = parsed[j];
            if (Number.isNaN(b.start) || Number.isNaN(b.end)) continue;

            if (a.start < b.end && b.start < a.end) {
                conflictIds.add(a.id);
                conflictIds.add(b.id);
            }
        }
    }

    return conflictIds;
}

export default function MyRegistrationsScreen() {
    const { t } = useTranslation();
    const { user } = useAuth();

    const [registrations, setRegistrations] = useState<RegistrationWithConflict[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const loadRegistrations = useCallback(async () => {
        if (!user?.token) {
            setRegistrations([]);
            setLoading(false);
            setRefreshing(false);
            return;
        }

        try {
            setError(null);
            const data = await getMyRegistrations(user.token);
            const conflictIds = computeConflictIds(data);
            setRegistrations(
                data.map((item) => ({
                    ...item,
                    hasConflict: conflictIds.has(item.id),
                })),
            );
        } catch (e) {
            console.error('Failed to load registrations', e);
            setError(t('registrations.loadError'));
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [t, user?.token]);

    useEffect(() => {
        loadRegistrations();
    }, [loadRegistrations]);

    const onRefresh = useCallback(() => {
        setRefreshing(true);
        loadRegistrations();
    }, [loadRegistrations]);

    const renderItem = useCallback(
        ({ item }: { item: RegistrationWithConflict }) => (
            <View style={styles.card}>
                <View style={styles.cardHeader}>
                    <ThemedText type="subtitle" style={styles.eventTitle}>
                        {item.eventTitle}
                    </ThemedText>
                    <View style={[styles.statusBadge, getStatusStyle(item.status)]}>
                        <ThemedText style={styles.statusText}>
                            {t(`registrations.status.${item.status}`, { defaultValue: item.status })}
                        </ThemedText>
                    </View>
                </View>

                <View style={styles.row}>
                    <ThemedText style={styles.label}>{t('registrations.dateLabel')}</ThemedText>
                    <ThemedText style={styles.value}>
                        {item.eventStartDateTime ? formatIsoDate(item.eventStartDateTime) : 'N/A'}
                    </ThemedText>
                </View>
                <View style={styles.row}>
                    <ThemedText style={styles.label}>{t('registrations.timeLabel')}</ThemedText>
                    <ThemedText style={styles.value}>
                        {item.eventStartDateTime
                            ? formatIsoTimeRange(item.eventStartDateTime, item.eventEndDateTime)
                            : 'N/A'}
                    </ThemedText>
                </View>

                <View style={styles.row}>
                    <ThemedText style={styles.label}>{t('registrations.statusLabel')}</ThemedText>
                    <ThemedText style={styles.value}>
                        {t(`registrations.status.${item.status}`, { defaultValue: item.status })}
                    </ThemedText>
                </View>

                {item.hasConflict && (
                    <View style={styles.conflictBox}>
                        <View style={styles.conflictBadge}>
                            <ThemedText style={styles.conflictText}>
                                {t('registrations.conflict')}
                            </ThemedText>
                        </View>
                        <ThemedText style={styles.conflictHint}>
                            {t('registrations.conflictHint')}
                        </ThemedText>
                    </View>
                )}
            </View>
        ),
        [t],
    );

    if (!user) {
        return (
            <ThemedView style={styles.centered}>
                <ThemedText>{t('registrations.loginRequired')}</ThemedText>
            </ThemedView>
        );
    }

    if (loading) {
        return (
            <ThemedView style={styles.centered}>
                <ActivityIndicator />
                <ThemedText style={styles.loadingText}>{t('common.loading')}</ThemedText>
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
            <ThemedText type="title" style={styles.screenTitle}>
                {t('registrations.title')}
            </ThemedText>
            <FlatList
                data={registrations}
                keyExtractor={(item) => item.id.toString()}
                renderItem={renderItem}
                contentContainerStyle={registrations.length === 0 && styles.emptyContainer}
                ListEmptyComponent={
                    <ThemedText>{t('registrations.noRegistrations')}</ThemedText>
                }
                refreshControl={
                    <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
                }
            />
        </ThemedView>
    );
}

function getStatusStyle(status: Registration['status']) {
    switch (status) {
        case 'CONFIRMED':
            return styles.statusConfirmed;
        case 'WAITLISTED':
            return styles.statusWaitlisted;
        case 'CANCELLED':
            return styles.statusCancelled;
        default:
            return styles.statusDefault;
    }
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
    },
    centered: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        padding: 16,
    },
    loadingText: {
        marginTop: 12,
    },
    errorText: {
        color: 'red',
        textAlign: 'center',
    },
    card: {
        backgroundColor: '#ffffff',
        borderRadius: 12,
        padding: 16,
        marginBottom: 12,
        shadowColor: '#000',
        shadowOpacity: 0.06,
        shadowRadius: 6,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    cardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 10,
    },
    eventTitle: {
        flex: 1,
        marginRight: 10,
        fontWeight: '700',
    },
    row: {
        flexDirection: 'row',
        marginBottom: 6,
    },
    label: {
        width: 80,
        fontWeight: '600',
    },
    value: {
        flex: 1,
    },
    statusBadge: {
        borderRadius: 999,
        paddingHorizontal: 10,
        paddingVertical: 4,
    },
    statusText: {
        color: '#ffffff',
        fontWeight: '600',
        fontSize: 12,
    },
    statusConfirmed: {
        backgroundColor: '#16a34a',
    },
    statusWaitlisted: {
        backgroundColor: '#f59e0b',
    },
    statusCancelled: {
        backgroundColor: '#9ca3af',
    },
    statusDefault: {
        backgroundColor: '#2563eb',
    },
    conflictBox: {
        marginTop: 10,
        padding: 10,
        borderRadius: 10,
        backgroundColor: '#fff7ed',
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#f59e0b',
        gap: 6,
    },
    conflictBadge: {
        alignSelf: 'flex-start',
        backgroundColor: '#dc2626',
        borderRadius: 8,
        paddingHorizontal: 8,
        paddingVertical: 4,
    },
    conflictText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 12,
    },
    conflictHint: {
        color: '#9a3412',
        fontSize: 12,
    },
    emptyContainer: {
        flexGrow: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
});
