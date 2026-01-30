import React from 'react';
import { View, StyleSheet, ActivityIndicator, Pressable, useColorScheme } from 'react-native';
import { useTranslation } from 'react-i18next';
import { ThemedText } from './themed-text';
import { RegistrationSummary } from '../services/events.service';

export type RegistrationSummaryComponentProps = {
    summary: RegistrationSummary | null;
    loading: boolean;
    error: string | null;
    onRetry?: () => void;
};

export function RegistrationSummaryComponent({
    summary,
    loading,
    error,
    onRetry,
}: RegistrationSummaryComponentProps) {
    const { t } = useTranslation();
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    const styles = getStyles(isDark);

    if (loading) {
        return (
            <View style={styles.container}>
                <View style={styles.loadingContainer}>
                    <ActivityIndicator size="small" color="#0057B8" />
                    <ThemedText style={styles.loadingText}>
                        {t('events.registrationSummary.loading', 'Loading summary...')}
                    </ThemedText>
                </View>
            </View>
        );
    }

    if (error) {
        return (
            <View style={styles.container}>
                <View style={styles.errorContainer}>
                    <ThemedText style={styles.errorText}>
                        ❌ {error}
                    </ThemedText>
                    <ThemedText style={styles.errorHint}>
                        {t('events.registrationSummary.loadingError')}
                    </ThemedText>
                    {onRetry && (
                        <Pressable style={styles.retryButton} onPress={onRetry}>
                            <ThemedText style={styles.retryButtonText}>
                                {t('events.registrationSummary.retry')}
                            </ThemedText>
                        </Pressable>
                    )}
                </View>
            </View>
        );
    }

    if (!summary) {
        return null;
    }

    // Calculate percentage full (0-100)
    const percentageFull = summary.percentageFull ?? 0;

    return (
        <View style={styles.container}>
            <ThemedText style={styles.sectionTitle}>
                {t('events.registrationSummary.title')}
            </ThemedText>

            {/* Total Registrations */}
            <View style={styles.statsRow}>
                <View style={styles.statItem}>
                    <ThemedText style={styles.statLabel}>
                        {t('events.registrationSummary.confirmedRegistrations')}
                    </ThemedText>
                    <ThemedText style={styles.statValue}>
                        {summary.totalRegistrations}
                    </ThemedText>
                </View>

                {/* Waitlisted */}
                <View style={styles.statItem}>
                    <ThemedText style={styles.statLabel}>
                        {t('events.registrationSummary.waitlisted')}
                    </ThemedText>
                    <ThemedText style={styles.statValue}>
                        {summary.waitlistedCount}
                    </ThemedText>
                </View>
            </View>

            {/* Capacity Info */}
            {summary.maxCapacity != null ? (
                <>
                    <View style={styles.capacityRow}>
                        <ThemedText style={styles.capacityLabel}>
                            {t('events.registrationSummary.capacity')}
                        </ThemedText>
                        <ThemedText style={styles.capacityValue}>
                            {summary.totalRegistrations} / {summary.maxCapacity}
                        </ThemedText>
                    </View>

                    {/* Remaining Spots */}
                    <View style={styles.capacityRow}>
                        <ThemedText style={styles.capacityLabel}>
                            {t('events.registrationSummary.remainingSpots')}
                        </ThemedText>
                        <ThemedText
                            style={[
                                styles.capacityValue,
                                summary.remainingSpots === 0 && styles.noSpotsText,
                                summary.remainingSpots !== null &&
                                    summary.remainingSpots <= 2 &&
                                    summary.remainingSpots > 0 &&
                                    styles.limitedSpotsText,
                            ]}
                        >
                            {summary.remainingSpots}
                        </ThemedText>
                    </View>

                    {/* Progress Bar */}
                    <View style={styles.progressContainer}>
                        <View style={styles.progressBar}>
                            <View
                                style={[
                                    styles.progressFill,
                                    {
                                        width: `${Math.min(percentageFull, 100)}%`,
                                        backgroundColor:
                                            percentageFull >= 100
                                                ? '#D32F2F'
                                                : percentageFull >= 80
                                                    ? '#F57C00'
                                                    : '#4CAF50',
                                    },
                                ]}
                            />
                        </View>
                        <ThemedText style={styles.percentageText}>
                            {Math.round(percentageFull)}%
                        </ThemedText>
                    </View>

                    {/* Full or Limited Spots Message */}
                    {summary.remainingSpots === 0 && (
                        <View style={styles.messageBox}>
                            <ThemedText style={styles.messageText}>
                                {t('events.registrationSummary.eventFull')}
                            </ThemedText>
                        </View>
                    )}

                    {summary.remainingSpots !== null &&
                        summary.remainingSpots > 0 &&
                        summary.remainingSpots <= 2 && (
                            <View style={styles.messageBox}>
                                <ThemedText style={styles.messageText}>
                                    {t('events.registrationSummary.limitedSpots', {
                                        count: summary.remainingSpots,
                                    })}
                                </ThemedText>
                            </View>
                        )}
                </>
            ) : (
                // No capacity limit
                <View style={styles.capacityRow}>
                    <ThemedText style={styles.capacityLabel}>
                        {t('events.registrationSummary.capacity')}
                    </ThemedText>
                    <ThemedText style={styles.capacityValue}>
                        {t('events.registrationSummary.unlimited')}
                    </ThemedText>
                </View>
            )}

            {/* Zero Registrations Message */}
            {summary.totalRegistrations === 0 && summary.waitlistedCount === 0 && (
                <View style={styles.emptyStateBox}>
                    <ThemedText style={styles.emptyStateText}>
                        {t('events.registrationSummary.noRegistrations')}
                    </ThemedText>
                </View>
            )}

            {/* Attendees List with Member Status */}
            {summary.attendees && summary.attendees.length > 0 && (
                <View style={styles.attendeesSection}>
                    <ThemedText style={styles.attendeesSectionTitle}>
                        {t('events.registrationSummary.attendees', 'Attendees')}
                    </ThemedText>
                    {summary.attendees.map((attendee) => (
                        <View key={attendee.userId} style={styles.attendeeItem}>
                            <View style={styles.attendeeInfo}>
                                <ThemedText style={styles.attendeeName}>
                                    {attendee.userName || 'Unknown'}
                                </ThemedText>
                                <ThemedText style={styles.attendeeEmail}>
                                    {attendee.userEmail}
                                </ThemedText>
                            </View>
                            <View style={styles.attendeeStatusContainer}>
                                <View 
                                    style={[
                                        styles.memberStatusBadge,
                                        attendee.memberStatus === 'ACTIVE' 
                                            ? styles.activeStatus 
                                            : styles.inactiveStatus
                                    ]}
                                >
                                    <ThemedText 
                                        style={[
                                            styles.memberStatusText,
                                            attendee.memberStatus === 'ACTIVE'
                                                ? styles.activeStatusText
                                                : styles.inactiveStatusText
                                        ]}
                                    >
                                        {attendee.memberStatus === 'ACTIVE' ? '●' : '○'} {t(`registrations.${attendee.memberStatus.toLowerCase()}`, attendee.memberStatus)}
                                    </ThemedText>
                                </View>
                                <View 
                                    style={[
                                        styles.registrationStatusBadge,
                                        attendee.registrationStatus === 'CONFIRMED' 
                                            ? styles.confirmedStatus 
                                            : styles.waitlistedStatus
                                    ]}
                                >
                                    <ThemedText 
                                        style={[
                                            styles.registrationStatusText,
                                            attendee.registrationStatus === 'CONFIRMED'
                                                ? styles.confirmedStatusText
                                                : styles.waitlistedStatusText
                                        ]}
                                    >
                                        {t(`events.registrationStatus.${attendee.registrationStatus.toLowerCase()}`, attendee.registrationStatus)}
                                        {attendee.waitlistedPosition && attendee.registrationStatus === 'WAITLISTED' 
                                            ? ` #${attendee.waitlistedPosition}` 
                                            : ''}
                                    </ThemedText>
                                </View>
                            </View>
                        </View>
                    ))}
                </View>
            )}
        </View>
    );
}

const getStyles = (isDark: boolean) => {
    const colors = {
        text: isDark ? '#ECEDEE' : '#333',
        textMuted: isDark ? '#A0A7B1' : '#666',
        textSubtle: isDark ? '#8A9199' : '#999',
        surface: isDark ? '#1F2328' : '#FFFFFF',
        background: isDark ? '#0F1419' : '#F5F5F5',
        border: isDark ? '#2F3A4A' : '#E0E0E0',
        borderLight: isDark ? '#2A313B' : '#F0F0F0',
        statBg: isDark ? '#1D2A3A' : '#E8F4FD',
        statAccent: isDark ? '#6AA9FF' : '#0057B8',
        successBg: isDark ? '#1B3A1B' : '#E8F5E9',
        successText: isDark ? '#81C784' : '#2E7D32',
        warningBg: isDark ? '#3A2A1A' : '#FFF3CD',
        warningText: isDark ? '#FFB74D' : '#856404',
        errorBg: isDark ? '#3A1F1F' : '#FFEBEE',
        errorText: isDark ? '#FFB4AB' : '#D32F2F',
        dangerText: isDark ? '#FF5252' : '#D32F2F',
        warningColor: isDark ? '#FFA726' : '#F57C00',
    };

    return StyleSheet.create({
        container: {
            marginTop: 16,
            paddingHorizontal: 0,
        },
        sectionTitle: {
            fontWeight: '700',
            fontSize: 16,
            marginBottom: 12,
        },
        attendeesSection: {
            marginTop: 20,
            paddingTop: 16,
            borderTopWidth: 1,
            borderTopColor: colors.border,
        },
        attendeesSectionTitle: {
            fontWeight: '700',
            fontSize: 14,
            marginBottom: 12,
            color: colors.text,
        },
        attendeeItem: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            paddingVertical: 10,
            paddingHorizontal: 8,
            borderBottomWidth: 1,
            borderBottomColor: colors.borderLight,
        },
        attendeeInfo: {
            flex: 1,
            marginRight: 12,
        },
        attendeeName: {
            fontWeight: '600',
            fontSize: 13,
            color: colors.text,
            marginBottom: 2,
        },
        attendeeEmail: {
            fontSize: 11,
            color: colors.textSubtle,
        },
        attendeeStatusContainer: {
            flexDirection: 'row',
            gap: 6,
            alignItems: 'center',
        },
        memberStatusBadge: {
            paddingHorizontal: 8,
            paddingVertical: 4,
            borderRadius: 12,
        },
        activeStatus: {
            backgroundColor: isDark ? '#1B3A1B' : '#E8F5E9',
        },
        inactiveStatus: {
            backgroundColor: isDark ? '#3A1F1F' : '#FFEBEE',
        },
        memberStatusText: {
            fontSize: 11,
            fontWeight: '600',
        },
        activeStatusText: {
            color: isDark ? '#81C784' : '#2E7D32',
        },
        inactiveStatusText: {
            color: isDark ? '#FFB4AB' : '#C62828',
        },
        registrationStatusBadge: {
            paddingHorizontal: 8,
            paddingVertical: 4,
            borderRadius: 12,
        },
        confirmedStatus: {
            backgroundColor: isDark ? '#1D3A1D' : '#C8E6C9',
        },
        waitlistedStatus: {
            backgroundColor: isDark ? '#3A3A1A' : '#FFF9C4',
        },
        registrationStatusText: {
            fontSize: 11,
            fontWeight: '600',
        },
        confirmedStatusText: {
            color: isDark ? '#81C784' : '#1B5E20',
        },
        waitlistedStatusText: {
            color: isDark ? '#FFB74D' : '#F57F17',
        },
        statsRow: {
            flexDirection: 'row',
            gap: 12,
            marginBottom: 16,
        },
        statItem: {
            flex: 1,
            backgroundColor: colors.statBg,
            borderRadius: 8,
            padding: 12,
            borderLeftWidth: 4,
            borderLeftColor: colors.statAccent,
        },
        statLabel: {
            fontSize: 12,
            color: colors.textMuted,
            marginBottom: 4,
            fontWeight: '600',
        },
        statValue: {
            fontSize: 24,
            fontWeight: '700',
            color: colors.statAccent,
        },
        capacityRow: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            paddingVertical: 8,
            borderBottomWidth: 1,
            borderBottomColor: colors.border,
        },
        capacityLabel: {
            fontWeight: '600',
            color: colors.textMuted,
            fontSize: 14,
        },
        capacityValue: {
            fontSize: 16,
            fontWeight: '700',
            color: colors.text,
        },
        noSpotsText: {
            color: colors.dangerText,
        },
        limitedSpotsText: {
            color: colors.warningColor,
        },
        progressContainer: {
            marginVertical: 12,
            gap: 8,
        },
        progressBar: {
            height: 20,
            backgroundColor: colors.border,
            borderRadius: 10,
            overflow: 'hidden',
            flexDirection: 'row',
        },
        progressFill: {
            height: '100%',
            borderRadius: 10,
        },
        percentageText: {
            fontSize: 12,
            fontWeight: '600',
            color: colors.text,
            textAlign: 'center',
        },
        messageBox: {
            backgroundColor: colors.warningBg,
            borderRadius: 8,
            padding: 10,
            marginTop: 12,
            borderLeftWidth: 4,
            borderLeftColor: colors.warningColor,
        },
        messageText: {
            fontSize: 13,
            color: colors.warningText,
            lineHeight: 18,
        },
        emptyStateBox: {
            backgroundColor: colors.background,
            borderRadius: 8,
            padding: 16,
            marginTop: 12,
            alignItems: 'center',
        },
        emptyStateText: {
            color: colors.textSubtle,
            fontSize: 14,
            fontStyle: 'italic',
        },
        loadingContainer: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 8,
            paddingVertical: 12,
        },
        loadingText: {
            fontSize: 14,
            color: colors.textMuted,
        },
        errorContainer: {
            backgroundColor: colors.errorBg,
            borderRadius: 8,
            padding: 12,
            gap: 8,
            borderLeftWidth: 4,
            borderLeftColor: colors.dangerText,
        },
        errorText: {
            color: colors.dangerText,
            fontWeight: '600',
            fontSize: 13,
        },
        errorHint: {
            color: colors.textMuted,
            fontSize: 12,
        },
        retryButton: {
            backgroundColor: colors.dangerText,
            borderRadius: 6,
            paddingVertical: 8,
            paddingHorizontal: 12,
            marginTop: 4,
            alignItems: 'center',
        },
        retryButtonText: {
            color: '#ffffff',
            fontWeight: '600',
            fontSize: 12,
        },
    });
};
