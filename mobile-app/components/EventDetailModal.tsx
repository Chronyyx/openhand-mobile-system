import React from 'react';
import { View, Modal, Image, ScrollView, Animated, Pressable, ActivityIndicator, TextInput, useColorScheme, Alert } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { ThemedText } from './themed-text';
import { RegistrationSummaryComponent } from './registration-summary';
import { getStyles } from '../styles/events.styles';
import { type EventSummary, type EventDetail, type RegistrationSummary } from '../services/events.service';
import { type Registration, type RegistrationParticipant } from '../services/registration.service';
import { searchUsers, registerParticipantForEvent, type EmployeeSearchResult } from '../services/employee.service';
import { formatIsoDate, formatIsoTimeRange } from '../utils/date-time';
import { getTranslatedEventTitle, getTranslatedEventDescription } from '../utils/event-translations';
import { getEventImage } from '../constants/event-images';
import { getStatusLabel, getStatusColor, getStatusTextColor } from '../utils/event-status';
import { resolveUrl } from '../utils/api';

// Props Definition
export type FamilyMemberInput = {
    id: string;
    fullName: string;
    age: string;
    relation?: string;
};

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
    onRegister: (familyMembers: FamilyMemberInput[]) => void;
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
    registrationParticipants?: RegistrationParticipant[] | null;
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
    registrationError = null,
    onCapacityRefresh,
    registrationParticipants = null
}: EventDetailModalProps) {
    const router = useRouter();

    // Fallback if no details yet
    const displayEvent = eventDetail || selectedEvent;
    const isCompleted = displayEvent?.status === 'COMPLETED';
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    const styles = getStyles(colorScheme);
    const infoPalette = isDark
        ? {
            errorBg: '#3A2626',
            errorBorder: '#8B4545',
            errorText: '#FFB4AB',
            successBg: '#1A4620',
            successBorder: '#2F6F3A',
            successText: '#8BE28B',
        }
        : {
            errorBg: '#FFEBEE',
            errorBorder: '#D32F2F',
            errorText: '#C62828',
            successBg: '#E8F5E9',
            successBorder: '#2E7D32',
            successText: '#2E7D32',
        };
    const inputPalette = isDark
        ? {
            bg: '#1F2328',
            border: '#3A3F47',
            text: '#ECEDEE',
            placeholder: '#8B93A1',
        }
        : {
            bg: '#FFFFFF',
            border: '#E0E7F3',
            text: '#333333',
            placeholder: '#999999',
        };

    // Employee Walk-in State
    const [walkinQuery, setWalkinQuery] = React.useState('');
    const [walkinResults, setWalkinResults] = React.useState<EmployeeSearchResult[]>([]);
    const [walkinSelected, setWalkinSelected] = React.useState<EmployeeSearchResult | null>(null);
    const [walkinSubmitting, setWalkinSubmitting] = React.useState(false);
    const [walkinError, setWalkinError] = React.useState<string | null>(null);
    const [walkinSuccess, setWalkinSuccess] = React.useState<string | null>(null);
    const [familyMembers, setFamilyMembers] = React.useState<FamilyMemberInput[]>([]);

    React.useEffect(() => {
        if (!visible) {
            setFamilyMembers([]);
        }
    }, [visible, selectedEvent?.id]);

    const handleAddFamilyMember = () => {
        setFamilyMembers(prev => [
            ...prev,
            {
                id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
                fullName: '',
                age: '',
                relation: ''
            }
        ]);
    };

    const handleRemoveFamilyMember = (id: string) => {
        setFamilyMembers(prev => prev.filter(member => member.id !== id));
    };

    const handleUpdateFamilyMember = (id: string, field: 'fullName' | 'age' | 'relation', value: string) => {
        setFamilyMembers(prev =>
            prev.map(member =>
                member.id === id ? { ...member, [field]: value } : member
            )
        );
    };

    const totalParticipants = 1 + familyMembers.length;

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

    const handleUnregisterPress = () => {
        Alert.alert(
            t('events.actions.confirmUnregisterTitle', 'Cancel Registration?'),
            t('events.actions.confirmUnregisterMessage', 'Are you sure you want to cancel your registration? This action cannot be undone.'),
            [
                {
                    text: t('events.actions.confirmUnregisterNo', 'No, Keep it'),
                    style: 'cancel'
                },
                {
                    text: t('events.actions.confirmUnregisterYes', 'Yes, Cancel'),
                    style: 'destructive',
                    onPress: onUnregister
                }
            ]
        );
    };

    const isInactiveMember = user?.memberStatus === 'INACTIVE';

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
                        {selectedEvent && (selectedEvent.imageUrl || getEventImage(selectedEvent)) && (
                            <Image
                                source={
                                    selectedEvent.imageUrl
                                        ? { uri: resolveUrl(selectedEvent.imageUrl) }
                                        : getEventImage(selectedEvent)!
                                }
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
                                <ActivityIndicator size="large" color={colorScheme === 'dark' ? '#6AA9FF' : '#0056A8'} />
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
                                <Ionicons name="checkbox" size={64} color={colorScheme === 'dark' ? '#6AA9FF' : '#0056A8'} style={{ marginBottom: 16 }} />
                                <ThemedText type="subtitle" style={styles.successTitle}>
                                    {t('alerts.registerSuccess', 'Inscription confirmée !')}
                                </ThemedText>
                                <ThemedText testID="success-message" style={styles.successMessage}>
                                    {t('alerts.registerSuccessMessage', "Vous êtes inscrit(e) à l'événement.")}
                                </ThemedText>
                                {registrationParticipants && registrationParticipants.length > 0 && (
                                    <View style={styles.participantsList}>
                                        <ThemedText style={styles.participantsTitle}>
                                            {t('events.family.participantsTitle', 'Participants')}
                                        </ThemedText>
                                        {registrationParticipants.map((participant) => (
                                            <ThemedText key={participant.registrationId} style={styles.participantItem}>
                                                {participant.fullName || t('events.family.unknownParticipant')}
                                            </ThemedText>
                                        ))}
                                    </View>
                                )}
                                <Pressable
                                    style={styles.undoButton}
                                    onPress={handleUnregisterPress}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('events.actions.undo', "Cancel registration")}
                                    accessibilityHint={t('events.actions.undoHint', 'Cancels your registration')}
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
                                    <Pressable
                                        style={styles.attendeesButton}
                                        onPress={handleOpenAttendees}
                                        accessibilityRole="button"
                                        accessibilityLabel={t('events.attendees.viewButton')}
                                        accessibilityHint={t('events.attendees.viewHint', 'Opens the attendee list')}
                                    >
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
                                                style={{ flex: 1, backgroundColor: inputPalette.bg, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 10, borderWidth: 1, borderColor: inputPalette.border, color: inputPalette.text }}
                                                placeholder={t('events.walkin.searchPlaceholder')}
                                                placeholderTextColor={inputPalette.placeholder}
                                                value={walkinQuery}
                                                onChangeText={setWalkinQuery}
                                                accessibilityLabel={t('events.walkin.searchLabel', 'Search attendee by email')}
                                                accessibilityHint={t('events.walkin.searchHint', 'Enter an email to search for a walk-in attendee')}
                                            />
                                            <Pressable
                                                style={styles.registerButton}
                                                onPress={handleWalkinSearch}
                                                accessibilityRole="button"
                                                accessibilityLabel={t('events.walkin.search')}
                                                accessibilityHint={t('events.walkin.searchHintButton', 'Searches for matching users')}
                                            >
                                                <ThemedText style={styles.registerButtonText}>{t('events.walkin.search')}</ThemedText>
                                            </Pressable>
                                        </View>
                                        {walkinError && (
                                            <View style={[styles.infoBox, { borderLeftColor: infoPalette.errorBorder, borderLeftWidth: 4, backgroundColor: infoPalette.errorBg }]}>
                                                <ThemedText style={[styles.infoText, { color: infoPalette.errorText }]}>{walkinError}</ThemedText>
                                            </View>
                                        )}
                                        {walkinSuccess && (
                                            <View style={[styles.infoBox, { borderLeftColor: infoPalette.successBorder, borderLeftWidth: 4, backgroundColor: infoPalette.successBg }]}>
                                                <ThemedText style={[styles.infoText, { color: infoPalette.successText }]}>{walkinSuccess}</ThemedText>
                                            </View>
                                        )}
                                        {walkinResults.length > 0 && (
                                            <View style={{ gap: 8 }}>
                                                {walkinResults.map(r => (
                                                    <Pressable
                                                        key={r.id}
                                                        onPress={() => setWalkinSelected(r)}
                                                        style={({ pressed }) => [styles.unregisterButton, pressed && { opacity: 0.9 }]}
                                                        accessibilityRole="button"
                                                        accessibilityLabel={t('events.walkin.selectUser', 'Select user')}
                                                        accessibilityHint={r.email}
                                                        accessibilityState={{ selected: walkinSelected?.id === r.id }}
                                                    >
                                                        <ThemedText style={styles.unregisterButtonText}>{r.email}</ThemedText>
                                                    </Pressable>
                                                ))}
                                            </View>
                                        )}
                                        <Pressable
                                            style={[styles.registerButton, (walkinSubmitting || !walkinSelected) && { opacity: 0.6 }]}
                                            onPress={handleWalkinRegister}
                                            disabled={walkinSubmitting || !walkinSelected}
                                            accessibilityRole="button"
                                            accessibilityLabel={t('events.walkin.register')}
                                            accessibilityHint={t('events.walkin.registerHint', 'Registers the selected attendee')}
                                            accessibilityState={{ disabled: walkinSubmitting || !walkinSelected }}
                                        >
                                            {walkinSubmitting ? (
                                                <ActivityIndicator color="#FFFFFF" />
                                            ) : (
                                                <ThemedText style={styles.registerButtonText}>{t('events.walkin.register')}</ThemedText>
                                            )}
                                        </Pressable>
                                    </View>
                                )}

                                {/* Family Registration */}
                                {selectedEvent && user && hasRole(['ROLE_MEMBER']) && !userRegistration && !isCompleted && (
                                    <View style={styles.familySection}>
                                        <View style={styles.familyHeaderRow}>
                                            <ThemedText style={styles.sectionTitle}>
                                                {t('events.family.title', 'Family Members')}
                                            </ThemedText>
                                            <Pressable
                                                style={styles.familyAddButton}
                                                onPress={handleAddFamilyMember}
                                                testID="add-family-member"
                                                accessibilityRole="button"
                                                accessibilityLabel={t('events.family.addButton', 'Add Family Member')}
                                                accessibilityHint={t('events.family.addHint', 'Adds another family member to this registration')}
                                            >
                                                <Ionicons name="add" size={16} color="#FFFFFF" />
                                                <ThemedText style={styles.familyAddButtonText}>
                                                    {t('events.family.addButton', 'Add Family Member')}
                                                </ThemedText>
                                            </Pressable>
                                        </View>
                                        {familyMembers.length === 0 ? (
                                            <ThemedText style={styles.familyHint}>
                                                {t('events.family.hint', 'Add family members to register together.')}
                                            </ThemedText>
                                        ) : (
                                            familyMembers.map(member => (
                                                <View key={member.id} style={styles.familyCard}>
                                                    <TextInput
                                                        style={styles.familyInput}
                                                        placeholder={t('events.family.fullNamePlaceholder', 'Full name')}
                                                        placeholderTextColor={inputPalette.placeholder}
                                                        value={member.fullName}
                                                        onChangeText={(text) => handleUpdateFamilyMember(member.id, 'fullName', text)}
                                                        accessibilityLabel={t('events.family.fullNameLabel', 'Family member full name')}
                                                    />
                                                    <TextInput
                                                        style={styles.familyInput}
                                                        placeholder={t('events.family.agePlaceholder', 'Age')}
                                                        placeholderTextColor={inputPalette.placeholder}
                                                        keyboardType="numeric"
                                                        value={member.age}
                                                        onChangeText={(text) => handleUpdateFamilyMember(member.id, 'age', text)}
                                                        accessibilityLabel={t('events.family.ageLabel', 'Family member age')}
                                                        accessibilityHint={t('events.family.ageHint', 'Enter age in years')}
                                                    />
                                                    <TextInput
                                                        style={styles.familyInput}
                                                        placeholder={t('events.family.relationPlaceholder', 'Relation (optional)')}
                                                        placeholderTextColor={inputPalette.placeholder}
                                                        value={member.relation || ''}
                                                        onChangeText={(text) => handleUpdateFamilyMember(member.id, 'relation', text)}
                                                        accessibilityLabel={t('events.family.relationLabel', 'Relationship')}
                                                        accessibilityHint={t('events.family.relationHint', 'Optional relationship')}
                                                    />
                                                    <Pressable
                                                        style={styles.familyRemoveButton}
                                                        onPress={() => handleRemoveFamilyMember(member.id)}
                                                        accessibilityRole="button"
                                                        accessibilityLabel={t('events.family.removeButton', 'Remove')}
                                                        accessibilityHint={t('events.family.removeHint', 'Removes this family member')}
                                                    >
                                                        <Ionicons name="trash-outline" size={16} color="#FFFFFF" />
                                                        <ThemedText style={styles.familyRemoveButtonText}>
                                                            {t('events.family.removeButton', 'Remove')}
                                                        </ThemedText>
                                                    </Pressable>
                                                </View>
                                            ))
                                        )}
                                    </View>
                                )}

                                {/* Buttons */}
                                {user ? (
                                    isInactiveMember ? (
                                        <View testID="error-message" style={[styles.infoBox, { borderLeftColor: infoPalette.errorBorder, borderLeftWidth: 4, backgroundColor: infoPalette.errorBg }]}>
                                            <ThemedText style={[styles.infoText, { color: infoPalette.errorText }]}>
                                                {t('events.inactiveMember')}
                                            </ThemedText>
                                        </View>
                                    ) : hasRole(['ROLE_MEMBER', 'ROLE_EMPLOYEE']) ? (
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
                                                    <View testID="error-message" style={[styles.infoBox, { borderLeftColor: infoPalette.errorBorder, borderLeftWidth: 4, backgroundColor: infoPalette.errorBg }]}>
                                                        <ThemedText style={[styles.infoText, { color: infoPalette.errorText }]}>
                                                            {registrationError}
                                                        </ThemedText>
                                                    </View>
                                                )}

                                                {userRegistration ? (
                                                    <Pressable
                                                        style={[styles.unregisterButton, isRegistering && { opacity: 0.6 }]}
                                                        onPress={handleUnregisterPress}
                                                        disabled={isRegistering}
                                                        accessibilityRole="button"
                                                        accessibilityLabel={t('events.actions.unregister', 'Unregister')}
                                                        accessibilityHint={t('events.actions.unregisterHint', 'Cancels your registration')}
                                                        accessibilityState={{ disabled: isRegistering }}
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
                                                    <View testID="register-button" style={{ gap: 10 }}>
                                                        <View style={styles.participantCountRow}>
                                                            <ThemedText style={styles.participantCountLabel}>
                                                                {t('events.family.totalParticipants', 'Total participants')}
                                                            </ThemedText>
                                                            <ThemedText style={styles.participantCountValue}>
                                                                {totalParticipants}
                                                            </ThemedText>
                                                        </View>
                                                        <Pressable
                                                            style={[styles.registerButton, isRegistering && { opacity: 0.6 }]}
                                                            onPress={() => onRegister(familyMembers)}
                                                            disabled={isRegistering}
                                                            testID="confirm-register"
                                                            accessibilityRole="button"
                                                            accessibilityLabel={displayEvent?.status === 'FULL'
                                                                ? t('events.actions.joinWaitlist')
                                                                : t('events.actions.register')}
                                                            accessibilityHint={t('events.actions.registerHint', 'Registers you for this event')}
                                                            accessibilityState={{ disabled: isRegistering }}
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
                                                    </View>
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
                        accessibilityRole="button"
                        accessibilityLabel={t('events.actions.close')}
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
