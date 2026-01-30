import React from 'react';
import { View, StyleSheet, ActivityIndicator, Pressable } from 'react-native';
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

const styles = StyleSheet.create({
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
        borderTopColor: '#E0E0E0',
    },
    attendeesSectionTitle: {
        fontWeight: '700',
        fontSize: 14,
        marginBottom: 12,
        color: '#333',
    },
    attendeeItem: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingVertical: 10,
        paddingHorizontal: 8,
        borderBottomWidth: 1,
        borderBottomColor: '#F0F0F0',
    },
    attendeeInfo: {
        flex: 1,
        marginRight: 12,
    },
    attendeeName: {
        fontWeight: '600',
        fontSize: 13,
        color: '#333',
        marginBottom: 2,
    },
    attendeeEmail: {
        fontSize: 11,
        color: '#999',
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
        color: '#4b5563',
    },
    memberStatusBadge: {
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 12,
    },
    activeStatus: {
        backgroundColor: '#E8F5E9',
    },
    inactiveStatus: {
        backgroundColor: '#FFEBEE',
    },
    memberStatusText: {
        fontSize: 11,
        fontWeight: '600',
    },
    activeStatusText: {
        color: '#2E7D32',
    },
    inactiveStatusText: {
        color: '#C62828',
    },
    registrationStatusBadge: {
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 12,
    },
    confirmedStatus: {
        backgroundColor: '#C8E6C9',
    },
    waitlistedStatus: {
        backgroundColor: '#FFF9C4',
    },
    registrationStatusText: {
        fontSize: 11,
        fontWeight: '600',
    },
    confirmedStatusText: {
        color: '#1B5E20',
    },
    waitlistedStatusText: {
        color: '#F57F17',
    },
    statsRow: {
        flexDirection: 'row',
        gap: 12,
        marginBottom: 16,
    },
    statItem: {
        flex: 1,
        backgroundColor: '#E8F4FD',
        borderRadius: 8,
        padding: 12,
        borderLeftWidth: 4,
        borderLeftColor: '#0057B8',
    },
    statLabel: {
        fontSize: 12,
        color: '#666',
        marginBottom: 4,
        fontWeight: '600',
    },
    statValue: {
        fontSize: 24,
        fontWeight: '700',
        color: '#0057B8',
    },
    capacityRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingVertical: 8,
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    capacityLabel: {
        fontWeight: '600',
        color: '#666',
        fontSize: 14,
    },
    capacityValue: {
        fontSize: 16,
        fontWeight: '700',
        color: '#333',
    },
    noSpotsText: {
        color: '#D32F2F',
    },
    limitedSpotsText: {
        color: '#F57C00',
    },
    progressContainer: {
        marginVertical: 12,
        gap: 8,
    },
    progressBar: {
        height: 20,
        backgroundColor: '#E0E0E0',
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
        color: '#666',
        textAlign: 'center',
    },
    messageBox: {
        backgroundColor: '#FFF3CD',
        borderRadius: 8,
        padding: 10,
        marginTop: 12,
        borderLeftWidth: 4,
        borderLeftColor: '#FFC107',
    },
    messageText: {
        fontSize: 13,
        color: '#856404',
        lineHeight: 18,
    },
    emptyStateBox: {
        backgroundColor: '#F5F5F5',
        borderRadius: 8,
        padding: 16,
        marginTop: 12,
        alignItems: 'center',
    },
    emptyStateText: {
        color: '#999',
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
        color: '#666',
    },
    errorContainer: {
        backgroundColor: '#FFEBEE',
        borderRadius: 8,
        padding: 12,
        gap: 8,
        borderLeftWidth: 4,
        borderLeftColor: '#D32F2F',
    },
    errorText: {
        color: '#D32F2F',
        fontWeight: '600',
        fontSize: 13,
    },
    errorHint: {
        color: '#666',
        fontSize: 12,
    },
    retryButton: {
        backgroundColor: '#D32F2F',
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
