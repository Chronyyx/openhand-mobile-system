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
    const palette = isDark
        ? {
            primary: '#6AA9FF',
            progress: {
                green: '#51CF66',
                orange: '#FFD43B',
                red: '#FF6B6B',
            },
        }
        : {
            primary: '#0057B8',
            progress: {
                green: '#4CAF50',
                orange: '#F57C00',
                red: '#D32F2F',
            },
        };
    const styles = getStyles(isDark);

    if (loading) {
        return (
            <View style={styles.container}>
                <View style={styles.loadingContainer}>
                    <ActivityIndicator size="small" color={palette.primary} />
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
                                                ? palette.progress.red
                                                : percentageFull >= 80
                                                    ? palette.progress.orange
                                                    : palette.progress.green,
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
                                {attendee.participants && attendee.participants.length > 0 && (
                                    <View style={styles.participantList}>
                                        {attendee.participants.map((participant, index) => (
                                            <ThemedText
                                                key={`${participant.registrationId}-${index}`}
                                                style={styles.participantItem}
                                            >
                                                • {participant.fullName || t('registrations.participantUnknown')}
                                                {participant.primaryRegistrant ? ` ${t('registrations.participantPrimary')}` : ''}
                                                {participant.age != null ? ` (${participant.age})` : ''}
                                            </ThemedText>
                                        ))}
                                    </View>
                                )}
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
    const colors = isDark
        ? {
            text: '#ECEDEE',
            textSecondary: '#A0A7B1',
            border: '#3A3F47',
            divider: '#2A2F36',
            statBg: '#1A3A52',
            statBorder: '#4A7BA7',
            statValue: '#6AA9FF',
            progressBg: '#3A3F47',
            messageBg: '#1A2633',
            messageText: '#FFD43B',
            messageBorder: '#4A5F3F',
            emptyBg: '#1F2328',
            emptyText: '#A0A7B1',
            errorBg: '#3A2626',
            errorText: '#FF6B6B',
            errorBorder: '#8B4545',
            statusActiveBg: '#1A4620',
            statusInactiveBg: '#4A2626',
            statusActiveText: '#51CF66',
            statusInactiveText: '#FF6B6B',
            statusConfirmedBg: '#1A4620',
            statusWaitlistedBg: '#4A4620',
            statusConfirmedText: '#51CF66',
            statusWaitlistedText: '#FFD43B',
            noSpots: '#FF6B6B',
            limitedSpots: '#FFD43B',
        }
        : {
            text: '#333333',
            textSecondary: '#666666',
            border: '#E0E0E0',
            divider: '#F0F0F0',
            statBg: '#E8F4FD',
            statBorder: '#0057B8',
            statValue: '#0057B8',
            progressBg: '#E0E0E0',
            messageBg: '#FFF3CD',
            messageText: '#856404',
            messageBorder: '#FFC107',
            emptyBg: '#F5F5F5',
            emptyText: '#999999',
            errorBg: '#FFEBEE',
            errorText: '#D32F2F',
            errorBorder: '#D32F2F',
            statusActiveBg: '#E8F5E9',
            statusInactiveBg: '#FFEBEE',
            statusActiveText: '#2E7D32',
            statusInactiveText: '#C62828',
            statusConfirmedBg: '#C8E6C9',
            statusWaitlistedBg: '#FFF9C4',
            statusConfirmedText: '#1B5E20',
            statusWaitlistedText: '#F57F17',
            noSpots: '#D32F2F',
            limitedSpots: '#F57C00',
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
        borderBottomColor: colors.divider,
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
        color: colors.textSecondary,
    },
    attendeeStatusContainer: {
        flexDirection: 'row',
        gap: 6,
        alignItems: 'center',
    },
    participantList: {
        marginTop: 6,
        gap: 2,
    },
    participantItem: {
        fontSize: 11,
        color: colors.textSecondary,
    },
    memberStatusBadge: {
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 12,
    },
    activeStatus: {
        backgroundColor: colors.statusActiveBg,
    },
    inactiveStatus: {
        backgroundColor: colors.statusInactiveBg,
    },
    memberStatusText: {
        fontSize: 11,
        fontWeight: '600',
    },
    activeStatusText: {
        color: colors.statusActiveText,
    },
    inactiveStatusText: {
        color: colors.statusInactiveText,
    },
    registrationStatusBadge: {
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 12,
    },
    confirmedStatus: {
        backgroundColor: colors.statusConfirmedBg,
    },
    waitlistedStatus: {
        backgroundColor: colors.statusWaitlistedBg,
    },
    registrationStatusText: {
        fontSize: 11,
        fontWeight: '600',
    },
    confirmedStatusText: {
        color: colors.statusConfirmedText,
    },
    waitlistedStatusText: {
        color: colors.statusWaitlistedText,
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
        borderLeftColor: colors.statBorder,
    },
    statLabel: {
        fontSize: 12,
        color: colors.textSecondary,
        marginBottom: 4,
        fontWeight: '600',
    },
    statValue: {
        fontSize: 24,
        fontWeight: '700',
        color: colors.statValue,
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
        color: colors.textSecondary,
        fontSize: 14,
    },
    capacityValue: {
        fontSize: 16,
        fontWeight: '700',
        color: colors.text,
    },
    noSpotsText: {
        color: colors.noSpots,
    },
    limitedSpotsText: {
        color: colors.limitedSpots,
    },
    progressContainer: {
        marginVertical: 12,
        gap: 8,
    },
    progressBar: {
        height: 20,
        backgroundColor: colors.progressBg,
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
        color: colors.textSecondary,
        textAlign: 'center',
    },
    messageBox: {
        backgroundColor: colors.messageBg,
        borderRadius: 8,
        padding: 10,
        marginTop: 12,
        borderLeftWidth: 4,
        borderLeftColor: colors.messageBorder,
    },
    messageText: {
        fontSize: 13,
        color: colors.messageText,
        lineHeight: 18,
    },
    emptyStateBox: {
        backgroundColor: colors.emptyBg,
        borderRadius: 8,
        padding: 16,
        marginTop: 12,
        alignItems: 'center',
    },
    emptyStateText: {
        color: colors.emptyText,
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
        color: colors.textSecondary,
    },
    errorContainer: {
        backgroundColor: colors.errorBg,
        borderRadius: 8,
        padding: 12,
        gap: 8,
        borderLeftWidth: 4,
        borderLeftColor: colors.errorBorder,
    },
    errorText: {
        color: colors.errorText,
        fontWeight: '600',
        fontSize: 13,
    },
    errorHint: {
        color: colors.textSecondary,
        fontSize: 12,
    },
    retryButton: {
        backgroundColor: colors.errorText,
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

