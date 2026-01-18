import React from 'react';
import { View, Modal, Image, ScrollView, Animated, Pressable, ActivityIndicator, TextInput } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { ThemedText } from './themed-text';
import { RegistrationSummaryComponent } from './registration-summary';
import { styles } from '../styles/events.styles';
import { type EventSummary, type EventDetail, type RegistrationSummary } from '../services/events.service';
import { type Registration } from '../services/registration.service';
import { searchUsers, registerParticipantForEvent, type EmployeeSearchResult } from '../services/employee.service';
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
    // Optional callback to refresh capacity/details from parent
    onCapacityRefresh?: () => void;
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
    ,
    onCapacityRefresh
}: EventDetailModalProps) {
    const router = useRouter();

    // Fallback if no details yet
    const displayEvent = eventDetail || selectedEvent;
    const isCompleted = displayEvent?.status === 'COMPLETED';

    // Employee Walk-in State
    const [walkinQuery, setWalkinQuery] = React.useState('');
    const [walkinResults, setWalkinResults] = React.useState<EmployeeSearchResult[]>([]);
    const [walkinSelected, setWalkinSelected] = React.useState<EmployeeSearchResult | null>(null);
    const [walkinSubmitting, setWalkinSubmitting] = React.useState(false);
    const [walkinError, setWalkinError] = React.useState<string | null>(null);
    const [walkinSuccess, setWalkinSuccess] = React.useState<string | null>(null);

    const handleWalkinSearch = async () => {
        setWalkinError(null);
        setWalkinSuccess(null);
        setWalkinSelected(null);
        try {
            // We don't have direct token here; parent passes user in props
            // For simplicity, we assume user has a token field
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const token = (user as any)?.token;
            if (!token) {
                setWalkinError(t('events.walkin.notAuthenticated'));
                return;
            }
            const results = await searchUsers(walkinQuery.trim(), token);
            setWalkinResults(results);
            if (results.length === 1) setWalkinSelected(results[0]);
        } catch (e: any) {
            setWalkinError(e.message || t('events.walkin.searchFailed'));
        }
    };

    const handleWalkinRegister = async () => {
        if (!displayEvent || !walkinSelected) return;
        setWalkinSubmitting(true);
        setWalkinError(null);
        setWalkinSuccess(null);
        try {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const token = (user as any)?.token;
            if (!token) {
                setWalkinError(t('events.walkin.notAuthenticated'));
                return;
            }
            const reg = await registerParticipantForEvent(displayEvent.id, walkinSelected.id, token);
            // Success message based on status
            const status = (reg as any)?.status as string | undefined;
            if (status === 'CONFIRMED') {
                setWalkinSuccess(t('events.walkin.successConfirmed'));
                // Capacity may have changed; ask parent to refresh details
                onCapacityRefresh?.();
            } else if (status === 'WAITLISTED') {
                setWalkinSuccess(t('events.walkin.successWaitlisted'));
            } else {
                setWalkinSuccess(t('events.walkin.successRegistered'));
            }
            // Refresh summary for admins/employees
            onRetrySummary();
        } catch (e: any) {
            // Parse the error message to provide a user-friendly version
            const rawMsg = e?.message || 'Registration failed';
            let displayMsg = rawMsg;
            
            // If the message mentions "already registered", provide a cleaner version
            if (rawMsg.toLowerCase().includes('already registered')) {
                displayMsg = t('events.walkin.alreadyRegistered', { email: walkinSelected?.email });
            }
            
            setWalkinError(displayMsg);
        } finally {
            setWalkinSubmitting(false);
        }
    };

    const handleOpenAttendees = () => {
        if (!displayEvent) return;
        onClose();
        router.push(`/events/${displayEvent.id}/attendees`);
    };

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

                                {selectedEvent && user && hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']) && (
                                    <Pressable style={styles.attendeesButton} onPress={handleOpenAttendees}>
                                        <ThemedText style={styles.attendeesButtonText}>
                                            {t('events.attendees.viewButton')}
                                        </ThemedText>
                                    </Pressable>
                                )}

                                {/* Employee Walk-In Registration */}
                                {selectedEvent && user && hasRole(['ROLE_EMPLOYEE', 'ROLE_ADMIN']) && !isCompleted && (
                                    <View style={{ marginTop: 18, gap: 10 }}>
                                        <ThemedText style={styles.sectionTitle}>{t('events.walkin.title')}</ThemedText>
                                        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                                            <TextInput
                                                style={{ flex: 1, backgroundColor: '#FFFFFF', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 10, borderWidth: 1, borderColor: '#E0E7F3' }}
                                                placeholder={t('events.walkin.searchPlaceholder')}
                                                value={walkinQuery}
                                                onChangeText={setWalkinQuery}
                                            />
                                            <Pressable style={styles.registerButton} onPress={handleWalkinSearch}>
                                                <ThemedText style={styles.registerButtonText}>{t('events.walkin.search')}</ThemedText>
                                            </Pressable>
                                        </View>
                                        {walkinError && (
                                            <View style={[styles.infoBox, { borderLeftColor: '#d32f2f', borderLeftWidth: 4, backgroundColor: '#ffebee' }]}> 
                                                <ThemedText style={[styles.infoText, { color: '#c62828' }]}>{walkinError}</ThemedText>
                                            </View>
                                        )}
                                        {walkinSuccess && (
                                            <View style={[styles.infoBox, { borderLeftColor: '#2e7d32', borderLeftWidth: 4, backgroundColor: '#e8f5e9' }]}> 
                                                <ThemedText style={[styles.infoText, { color: '#2e7d32' }]}>{walkinSuccess}</ThemedText>
                                            </View>
                                        )}
                                        {walkinResults.length > 0 && (
                                            <View style={{ gap: 8 }}>
                                                {walkinResults.map(r => (
                                                    <Pressable key={r.id} onPress={() => setWalkinSelected(r)} style={({ pressed }) => [styles.unregisterButton, pressed && { opacity: 0.9 }]}> 
                                                        <ThemedText style={styles.unregisterButtonText}>{r.email}</ThemedText>
                                                    </Pressable>
                                                ))}
                                            </View>
                                        )}
                                        <Pressable
                                            style={[styles.registerButton, (walkinSubmitting || !walkinSelected) && { opacity: 0.6 }]}
                                            onPress={handleWalkinRegister}
                                            disabled={walkinSubmitting || !walkinSelected}
                                        >
                                            {walkinSubmitting ? (
                                                <ActivityIndicator color="#FFFFFF" />
                                            ) : (
                                                <ThemedText style={styles.registerButtonText}>{t('events.walkin.register')}</ThemedText>
                                            )}
                                        </Pressable>
                                    </View>
                                )}

                                {/* Buttons */}
                                {user ? (
                                    hasRole(['ROLE_MEMBER', 'ROLE_EMPLOYEE']) ? (
                                        isCompleted ? (
                                            <View style={styles.infoBox}>
                                                <ThemedText style={styles.infoText}>
                                                    {t('events.completedNotice')}
                                                </ThemedText>
                                            </View>
                                        ) : (
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
                                        )
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
