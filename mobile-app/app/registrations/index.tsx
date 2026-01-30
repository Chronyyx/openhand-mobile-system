import React, { useCallback, useState } from 'react';
import {
    ActivityIndicator,
    RefreshControl,
    SectionList,
    StyleSheet,
    View,
    useColorScheme,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { useFocusEffect } from '@react-navigation/native';

import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import { MenuLayout } from '../../components/menu-layout';
import { useAuth } from '../../context/AuthContext';
import {
    getRegistrationHistory,
    type RegistrationHistoryItem,
    type RegistrationStatus,
} from '../../services/registration.service';
import { formatIsoDate, formatIsoTimeRange } from '../../utils/date-time';
import { getTranslatedEventTitle } from '../../utils/event-translations';

type RegistrationWithConflict = RegistrationHistoryItem & { hasConflict: boolean };
type RegistrationSection = {
    key: 'ACTIVE' | 'PAST';
    title: string;
    emptyText: string;
    data: RegistrationWithConflict[];
};

function computeConflictIds(registrations: RegistrationHistoryItem[]): Set<number> {
    const conflictIds = new Set<number>();
    const parsed = registrations
        .filter((r) => r.status !== 'CANCELLED')
        .map((r) => ({
            id: r.registrationId,
            start: r.event?.startDateTime ? Date.parse(r.event.startDateTime) : NaN,
            end: r.event?.endDateTime ? Date.parse(r.event.endDateTime) : NaN,
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
    const { user, isLoading } = useAuth();
    const colorScheme = useColorScheme();
    const styles = getStyles(colorScheme);

    const [registrations, setRegistrations] = useState<RegistrationWithConflict[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const loadRegistrations = useCallback(async () => {
        if (!user?.token || isLoading) {
            setRegistrations([]);
            setLoading(false);
            setRefreshing(false);
            return;
        }

        try {
            setError(null);
            const data = await getRegistrationHistory(user.token, 'ALL');
            const activeRegistrations = data.filter((item) => item.timeCategory === 'ACTIVE');
            const conflictIds = computeConflictIds(activeRegistrations);
            setRegistrations(
                data.map((item) => ({
                    ...item,
                    hasConflict: conflictIds.has(item.registrationId),
                })),
            );
        } catch (e) {
            console.error('Failed to load registrations', e);
            setError(t('registrations.loadError'));
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [t, user?.token, isLoading]);

    useFocusEffect(
        useCallback(() => {
            if (!isLoading) {
                loadRegistrations();
            }
        }, [loadRegistrations, isLoading]),
    );

    const onRefresh = useCallback(() => {
        setRefreshing(true);
        loadRegistrations();
    }, [loadRegistrations]);

    const activeRegistrations = registrations.filter(
        (registration) => registration.timeCategory === 'ACTIVE',
    );
    const pastRegistrations = registrations.filter(
        (registration) => registration.timeCategory === 'PAST',
    );

    const sections: RegistrationSection[] = [
        {
            key: 'ACTIVE',
            title: t('registrations.activeTitle'),
            emptyText: t('registrations.emptyActive'),
            data: activeRegistrations,
        },
        {
            key: 'PAST',
            title: t('registrations.pastTitle'),
            emptyText: t('registrations.emptyPast'),
            data: pastRegistrations,
        },
    ];

    const renderItem = useCallback(
        ({ item }: { item: RegistrationWithConflict }) => {
            // Translate event title from translation key
            const translatedTitle = getTranslatedEventTitle({ title: item.event.title }, t);
            return (
            <View style={styles.card}>
                <View style={styles.cardHeader}>
                    <ThemedText type="subtitle" style={styles.eventTitle}>
                        {translatedTitle}
                    </ThemedText>
                    <View style={[styles.statusBadge, getStatusStyle(item.status, styles)]}>
                        <ThemedText style={styles.statusText}>
                            {t(`registrations.status.${item.status}`, { defaultValue: item.status })}
                        </ThemedText>
                    </View>
                </View>

                <View style={styles.row}>
                    <ThemedText style={styles.label}>{t('registrations.dateLabel')}</ThemedText>
                    <ThemedText style={styles.value}>
                        {item.event.startDateTime ? formatIsoDate(item.event.startDateTime) : 'N/A'}
                    </ThemedText>
                </View>
                <View style={styles.row}>
                    <ThemedText style={styles.label}>{t('registrations.timeLabel')}</ThemedText>
                    <ThemedText style={styles.value}>
                        {item.event.startDateTime
                            ? formatIsoTimeRange(item.event.startDateTime, item.event.endDateTime)
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
            );
        },
        [t],
    );

    let content = null;

    if (!user) {
        content = (
            <ThemedView style={styles.centered}>
                <ThemedText>{t('registrations.loginRequired')}</ThemedText>
            </ThemedView>
        );
    } else if (loading) {
        content = (
            <ThemedView style={styles.centered}>
                <ActivityIndicator />
                <ThemedText style={styles.loadingText}>{t('common.loading')}</ThemedText>
            </ThemedView>
        );
    } else if (error) {
        content = (
            <ThemedView style={styles.centered}>
                <ThemedText style={styles.errorText}>{error}</ThemedText>
            </ThemedView>
        );
    } else {
        content = (
            <ThemedView style={styles.container}>
                <ThemedText type="title" style={styles.screenTitle}>
                    {t('registrations.title')}
                </ThemedText>
                <SectionList
                    sections={sections}
                    keyExtractor={(item) => item.registrationId.toString()}
                    renderItem={renderItem}
                    renderSectionHeader={({ section }) => (
                        <View style={styles.sectionHeader}>
                            <ThemedText type="subtitle" style={styles.sectionTitle}>
                                {section.title}
                            </ThemedText>
                            <ThemedText style={styles.sectionCount}>
                                {section.data.length}
                            </ThemedText>
                        </View>
                    )}
                    renderSectionFooter={({ section }) =>
                        section.data.length === 0 ? (
                            <ThemedText style={styles.sectionEmptyText}>
                                {section.emptyText}
                            </ThemedText>
                        ) : null
                    }
                    contentContainerStyle={styles.listContent}
                    refreshControl={
                        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
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

function getStatusStyle(status: RegistrationStatus, styles: any) {
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

const getStyles = (colorScheme: 'light' | 'dark' | null) => {
    const isDark = colorScheme === 'dark';
    
    const TEXT_MUTED = isDark ? '#A0A7B1' : '#6b7280';
    const BG = isDark ? '#1F2328' : '#ffffff';
    const SURFACE = isDark ? '#0F1419' : 'transparent';
    const BORDER = isDark ? '#2F3A4A' : 'transparent';
    const ERROR_TEXT = isDark ? '#FFB4AB' : '#DC2626';
    const CONFLICT_BG = isDark ? '#3A1F1F' : '#fff7ed';
    const CONFLICT_BORDER = isDark ? '#6B2A2A' : '#f59e0b';
    const CONFLICT_TEXT = isDark ? '#FFB4AB' : '#9a3412';
    const STATUS_GREEN = isDark ? '#66BB6A' : '#16a34a';
    const STATUS_YELLOW = isDark ? '#FFA726' : '#f59e0b';
    const STATUS_GRAY = isDark ? '#9E9E9E' : '#9ca3af';
    const STATUS_BLUE = isDark ? '#42A5F5' : '#2563eb';

    return StyleSheet.create({
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
            color: ERROR_TEXT,
            textAlign: 'center',
        },
        listContent: {
            paddingBottom: 24,
        },
        sectionHeader: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: 10,
            marginBottom: 6,
        },
        sectionTitle: {
            fontWeight: '700',
        },
        sectionCount: {
            color: TEXT_MUTED,
            fontSize: 12,
        },
        sectionEmptyText: {
            color: TEXT_MUTED,
            marginBottom: 12,
        },
        card: {
            backgroundColor: BG,
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
            backgroundColor: STATUS_GREEN,
        },
        statusWaitlisted: {
            backgroundColor: STATUS_YELLOW,
        },
        statusCancelled: {
            backgroundColor: STATUS_GRAY,
        },
        statusDefault: {
            backgroundColor: STATUS_BLUE,
        },
        conflictBox: {
            marginTop: 10,
            padding: 10,
            borderRadius: 10,
            backgroundColor: CONFLICT_BG,
            borderWidth: StyleSheet.hairlineWidth,
            borderColor: CONFLICT_BORDER,
            gap: 6,
        },
        conflictBadge: {
            alignSelf: 'flex-start',
            backgroundColor: ERROR_TEXT,
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
            color: CONFLICT_TEXT,
            fontSize: 12,
        },
    });
};

const styles = getStyles('light');
