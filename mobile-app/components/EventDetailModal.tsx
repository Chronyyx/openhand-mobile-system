import React from 'react';
import { View, Modal, Image, ScrollView, Animated, Pressable, ActivityIndicator } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { ThemedText } from './themed-text';
import { RegistrationSummaryComponent } from './registration-summary';
import { styles } from '../styles/events.styles';
import { type EventSummary, type EventDetail, type RegistrationSummary } from '../services/events.service';
import { type Registration } from '../services/registration.service';
import { formatIsoDate, formatIsoTimeRange } from '../utils/date-time';
import { getTranslatedEventTitle, getTranslatedEventDescription } from '../utils/event-translations';
import { getEventImage } from '../constants/event-images';
import { getStatusLabel, getStatusColor, getStatusTextColor } from '../utils/event-status';

// Props Definition
type EventDetailModalProps = {
    visible: boolean;
    onClose: () => void;
    selectedEvent: EventSummary | null;
    eventDetail: EventDetail | null;
    loading: boolean;
    error: string | null;
    user: any; // Auth User
    hasRole: (roles: string[]) => boolean;
    t: (key: string, options?: any) => string;

    // Registration Props
    userRegistration: Registration | null;
    onRegister: () => void;
    onUnregister: () => void;

    // Success State
    showSuccessView: boolean;
    countdownValue: any; // Animated.Value or number
    countdownSeconds: number; // For text display

    // Admin/Employee Summary Props
    registrationSummary: RegistrationSummary | null;
    summaryLoading: boolean;
    summaryError: string | null;
    onRetrySummary: () => void;

    // Race Condition Prevention Props
    isRegistering?: boolean;
    registrationError?: string | null;
};

// Use explicit class for Animated View created in index if passed, but here we can just use View or re-create it.
// Since we are in a separate component, let's just make sure we handle the refactor correctly.
// The user previously had 'AnimatedView' created via 'createAnimatedComponent'.
// We can do the same here.
const AnimatedView = Animated.createAnimatedComponent(View) as any;

export function EventDetailModal({
    visible,
    onClose,
    selectedEvent,
    eventDetail,
    loading,
    error,
    user,
    hasRole,
    t,
    userRegistration,
    onRegister,
    onUnregister,
    showSuccessView,
    countdownValue, // Expecting Animated.Value for smooth bar, or number if simplistic
    countdownSeconds,
    registrationSummary,
    summaryLoading,
    summaryError,
    onRetrySummary,
    isRegistering = false,
    registrationError = null
}: EventDetailModalProps) {

    // Fallback if no details yet
    const displayEvent = eventDetail || selectedEvent;

    return (
        <Modal
            visible={visible && !!selectedEvent}
            animationType="slide"
            transparent
            onRequestClose={onClose}
        >
            <View style={styles.modalOverlay}>
                <View style={styles.modalCard}>
                    {/* Header */}
                    <View style={styles.modalHeader}>
                        {selectedEvent && getEventImage(selectedEvent) && (
                            <Image
                                source={getEventImage(selectedEvent)!}
                                style={styles.modalImage}
                                resizeMode="cover"
                            />
                        )}
                        <ThemedText type="title" style={styles.modalTitle}>
                            {selectedEvent && getTranslatedEventTitle(selectedEvent, t)}
                        </ThemedText>
                    </View>

                    <ScrollView style={styles.modalBody} contentContainerStyle={{ paddingBottom: 16 }}>
                        {loading ? (
                            <View style={styles.modalLoadingContainer}>
                                <ActivityIndicator size="large" color="#0056A8" />
                                <ThemedText style={styles.modalLoadingText}>
                                    {t('events.loading')}
                                </ThemedText>
                            </View>
                        ) : error ? (
                            <View style={styles.modalErrorContainer}>
                                <ThemedText style={styles.modalErrorText}>{error}</ThemedText>
                            </View>
                        ) : showSuccessView && selectedEvent ? (
                            /* Success View */
                            <View style={styles.successContainer}>
                                <Ionicons name="checkbox" size={64} color="#0056A8" style={{ marginBottom: 16 }} />
                                <ThemedText type="subtitle" style={styles.successTitle}>
                                    {t('alerts.registerSuccess', 'Inscription confirmée !')}
                                </ThemedText>
                                <ThemedText style={styles.successMessage}>
                                    {t('alerts.registerSuccessMessage', "Vous êtes inscrit(e) à l'événement.")}
                                </ThemedText>
                                <Pressable
                                    style={styles.undoButton}
                                    onPress={onUnregister}
                                >
                                    <ThemedText style={styles.undoButtonText}>
                                        {t('events.actions.undo', "Annuler l'inscription")}
                                    </ThemedText>
                                </Pressable>
                                <View style={styles.timerContainer}>
                                    {/* Handle both Animated.Value and simple numbers for portability */}
                                    {typeof countdownValue === 'number' ? (
                                        <View style={[styles.timerBar, { width: `${(countdownValue / 10) * 100}%` }]} />
                                    ) : (
                                        <AnimatedView
                                            style={[
                                                styles.timerBar,
                                                {
                                                    width: countdownValue.interpolate({
                                                        inputRange: [0, 1],
                                                        outputRange: ['0%', '100%']
                                                    })
                                                }
                                            ]}
                                        />
                                    )}
                                    <ThemedText style={styles.timerText}>
                                        {t('events.registrationSuccess.closingIn', { count: countdownSeconds })}
                                    </ThemedText>
                                </View>
                            </View>
                        ) : (
                            /* Detail View */
                            <>
                                <ThemedText style={styles.sectionTitle}>
                                    {t('events.fields.description')}
                                </ThemedText>
                                <ThemedText>
                                    {displayEvent && getTranslatedEventDescription(displayEvent, t)}
                                </ThemedText>

                                <View style={styles.modalRow}>
                                    <ThemedText style={styles.sectionTitle}>
                                        {t('events.fields.date')}
                                    </ThemedText>
                                    <ThemedText>
                                        {displayEvent && formatIsoDate(displayEvent.startDateTime)}
                                    </ThemedText>
                                </View>

                                <View style={styles.modalRow}>
                                    <ThemedText style={styles.sectionTitle}>
                                        {t('events.fields.time')}
                                    </ThemedText>
                                    <ThemedText>
                                        {displayEvent &&
                                            formatIsoTimeRange(
                                                displayEvent.startDateTime,
                                                displayEvent.endDateTime,
                                            )}
                                    </ThemedText>
                                </View>

                                <View style={styles.modalRow}>
                                    <ThemedText style={styles.sectionTitle}>
                                        {t('events.fields.place')}
                                    </ThemedText>
                                    <ThemedText>{displayEvent?.address}</ThemedText>
                                </View>

                                <View style={styles.modalRow}>
                                    <ThemedText style={styles.sectionTitle}>{t('events.fields.status')}</ThemedText>
                                    <View style={[styles.inlineStatusBadge, { backgroundColor: getStatusColor(displayEvent?.status) }]}>
                                        <ThemedText style={[styles.statusText, { color: getStatusTextColor(displayEvent?.status) }]}>
                                            {getStatusLabel(displayEvent?.status, t)}
                                        </ThemedText>
                                    </View>
                                </View>

                                {displayEvent?.maxCapacity != null &&
                                    displayEvent?.currentRegistrations != null && (
                                        <View style={styles.modalRow}>
                                            <ThemedText style={styles.sectionTitle}>
                                                {t('events.fields.capacity')}
                                            </ThemedText>
                                            <ThemedText>
                                                {displayEvent.currentRegistrations}/
                                                {displayEvent.maxCapacity}
                                            </ThemedText>
                                        </View>
                                    )}

                                {/* Registration Summary (Admin) */}
                                {selectedEvent && user && hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']) && (
                                    <RegistrationSummaryComponent
                                        loading={summaryLoading}
                                        error={summaryError}
                                        summary={registrationSummary}
                                        onRetry={onRetrySummary}
                                    />
                                )}

                                {/* Buttons */}
                                {user ? (
                                    hasRole(['ROLE_MEMBER', 'ROLE_EMPLOYEE']) ? (
                                        <View style={{ marginTop: 24, gap: 12 }}>
                                            {/* Error Message Display */}
                                            {registrationError && (
                                                <View style={[styles.infoBox, { borderLeftColor: '#d32f2f', borderLeftWidth: 4, backgroundColor: '#ffebee' }]}>
                                                    <ThemedText style={[styles.infoText, { color: '#c62828' }]}>
                                                        {registrationError}
                                                    </ThemedText>
                                                </View>
                                            )}

                                            {userRegistration ? (
                                                <Pressable
                                                    style={[styles.unregisterButton, isRegistering && { opacity: 0.6 }]}
                                                    onPress={onUnregister}
                                                    disabled={isRegistering}
                                                >
                                                    {isRegistering ? (
                                                        <ActivityIndicator color="#FFFFFF" />
                                                    ) : (
                                                        <ThemedText style={styles.unregisterButtonText}>
                                                            {t('events.actions.unregister', 'Se désinscrire')}
                                                        </ThemedText>
                                                    )}
                                                </Pressable>
                                            ) : (
                                                <Pressable
                                                    style={[styles.registerButton, isRegistering && { opacity: 0.6 }]}
                                                    onPress={onRegister}
                                                    disabled={isRegistering}
                                                >
                                                    {isRegistering ? (
                                                        <ActivityIndicator color="#FFFFFF" />
                                                    ) : (
                                                        <ThemedText style={styles.registerButtonText}>
                                                            {displayEvent?.status === 'FULL'
                                                                ? t('events.actions.joinWaitlist')
                                                                : t('events.actions.register')}
                                                        </ThemedText>
                                                    )}
                                                </Pressable>
                                            )}
                                        </View>
                                    ) : (
                                        /* Logged in but not Member/Employee? Use case unclear but safe fallback */
                                        null
                                    )
                                ) : (
                                    /* Not Logged In */
                                    <View style={styles.infoBox}>
                                        <ThemedText style={styles.infoText}>
                                            {t('events.loginToRegister', 'Connectez-vous pour vous inscrire')}
                                        </ThemedText>
                                    </View>
                                )}
                            </>
                        )}
                    </ScrollView>

                    <Pressable
                        style={styles.modalCloseButton}
                        onPress={onClose}
                    >
                        <ThemedText style={styles.modalCloseButtonText}>
                            {t('events.actions.close')}
                        </ThemedText>
                    </Pressable>
                </View>
            </View>
        </Modal>
    );
}
