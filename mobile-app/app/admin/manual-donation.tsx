import React, { useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Modal,
    Pressable,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    View,
} from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { Ionicons } from '@expo/vector-icons';
import { Redirect, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import {
    submitManualDonation,
    type ManualDonationFormData,
} from '../../services/donation-management.service';
import { getAllEvents, type EventSummary } from '../../services/events.service';
import { fetchAllUsers, type ManagedUser } from '../../services/user-management.service';

function pad2(value: number) {
    return value.toString().padStart(2, '0');
}

function getDefaultDonationDateInput() {
    const now = new Date();
    return `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())} ${pad2(now.getHours())}:${pad2(now.getMinutes())}`;
}

function normalizeDonationDate(value: string): string | null {
    const trimmed = value.trim();
    if (!trimmed) return null;

    const withSeparator = trimmed.includes('T') ? trimmed : trimmed.replace(' ', 'T');
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(withSeparator)) {
        return `${withSeparator}:00`;
    }
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(withSeparator)) {
        return withSeparator;
    }
    return null;
}

function isValidEmail(value: string) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

type DonorMode = 'existing' | 'guest';

export default function AdminManualDonationScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { hasRole } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const isDark = colorScheme === 'dark';
    const styles = getStyles(isDark);

    const isAdmin = hasRole(['ROLE_ADMIN']);
    const canView = hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']);

    const [menuVisible, setMenuVisible] = useState(false);
    const [events, setEvents] = useState<EventSummary[]>([]);
    const [eventsLoading, setEventsLoading] = useState(false);
    const [users, setUsers] = useState<ManagedUser[]>([]);
    const [usersLoading, setUsersLoading] = useState(false);

    const [donorMode, setDonorMode] = useState<DonorMode>('existing');
    const [selectedUser, setSelectedUser] = useState<ManagedUser | null>(null);
    const [userModalVisible, setUserModalVisible] = useState(false);
    const [userSearchQuery, setUserSearchQuery] = useState('');

    const [amountInput, setAmountInput] = useState('');
    const [currency, setCurrency] = useState('CAD');
    const [eventId, setEventId] = useState<string>('');
    const [donationDateInput, setDonationDateInput] = useState(getDefaultDonationDateInput());
    const [comments, setComments] = useState('');
    const [guestName, setGuestName] = useState('');
    const [guestEmail, setGuestEmail] = useState('');

    const [submitError, setSubmitError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (!isAdmin) {
            return;
        }

        setEventsLoading(true);
        void getAllEvents()
            .then(setEvents)
            .catch((err) => {
                console.error('Failed to load events for manual donation', err);
                setSubmitError(t('admin.donations.loadEventsError'));
            })
            .finally(() => setEventsLoading(false));

        setUsersLoading(true);
        void fetchAllUsers()
            .then(setUsers)
            .catch((err) => {
                console.error('Failed to load users for manual donation', err);
                setSubmitError(t('admin.donations.userPicker.loadError'));
            })
            .finally(() => setUsersLoading(false));
    }, [isAdmin, t]);

    const filteredUsers = useMemo(() => {
        const query = userSearchQuery.trim().toLowerCase();
        if (!query) {
            return users;
        }
        return users.filter((user) => {
            const name = (user.name ?? '').toLowerCase();
            const email = (user.email ?? '').toLowerCase();
            return name.includes(query) || email.includes(query);
        });
    }, [userSearchQuery, users]);

    const handleSubmit = async () => {
        const amount = Number.parseFloat(amountInput);
        if (!amount || amount <= 0) {
            setSubmitError(t('admin.donations.amountRequired'));
            return;
        }

        const normalizedDonationDate = normalizeDonationDate(donationDateInput);
        if (!normalizedDonationDate) {
            setSubmitError(t('admin.donations.validation.dateInvalid'));
            return;
        }

        const payload: ManualDonationFormData = {
            amount,
            currency,
            eventId: eventId ? Number(eventId) : null,
            donationDate: normalizedDonationDate,
            comments: comments.trim() ? comments.trim() : undefined,
        };

        if (donorMode === 'existing') {
            if (!selectedUser) {
                setSubmitError(t('admin.donations.validation.existingUserRequired'));
                return;
            }
            payload.donorUserId = selectedUser.id;
        } else {
            const trimmedName = guestName.trim();
            const trimmedEmail = guestEmail.trim();
            if (!trimmedName) {
                setSubmitError(t('admin.donations.validation.guestNameRequired'));
                return;
            }
            if (!trimmedEmail) {
                setSubmitError(t('admin.donations.validation.guestEmailRequired'));
                return;
            }
            if (!isValidEmail(trimmedEmail)) {
                setSubmitError(t('admin.donations.validation.guestEmailInvalid'));
                return;
            }

            payload.donorName = trimmedName;
            payload.donorEmail = trimmedEmail;
        }

        setSubmitting(true);
        setSubmitError(null);
        try {
            await submitManualDonation(payload);
            Alert.alert(t('common.success'), t('admin.donations.manualSuccess'));
            router.replace('/admin/donations');
        } catch (err: any) {
            console.error('Failed to submit manual donation', err);
            const backendMessage = err?.response?.data?.message;
            setSubmitError(backendMessage || t('admin.donations.submitError'));
        } finally {
            setSubmitting(false);
        }
    };

    if (!canView) {
        return <Redirect href="/" />;
    }

    if (!isAdmin) {
        return <Redirect href="/admin/donations" />;
    }

    return (
        <View style={styles.container}>
            <AppHeader onMenuPress={() => setMenuVisible(true)} />

            <ScrollView contentContainerStyle={styles.content}>
                <Pressable
                    style={({ pressed }) => [styles.backButton, pressed && styles.backButtonPressed]}
                    onPress={() => router.back()}
                    accessibilityRole="button"
                    accessibilityLabel={t('admin.donations.backToList')}
                >
                    <Ionicons name="arrow-back" size={17} color={styles.backButtonText.color} />
                    <Text style={styles.backButtonText}>{t('admin.donations.backToList')}</Text>
                </Pressable>

                <View style={styles.heroCard}>
                    <View style={styles.heroIconWrap}>
                        <Ionicons name="create-outline" size={22} color={styles.heroIcon.color} />
                    </View>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.title} testID="manual-donation-title">
                            {t('admin.donations.addDonationTitle')}
                        </Text>
                        <Text style={styles.subtitle}>{t('admin.donations.manualScreenSubtitle')}</Text>
                    </View>
                </View>

                <View style={styles.sectionCard}>
                    <Text style={styles.sectionTitle}>{t('admin.donations.form.donorType')}</Text>
                    <Text style={styles.sectionHint}>{t('admin.donations.form.donorTypeHint')}</Text>
                    <View style={styles.segmentRow}>
                        <Pressable
                            testID="donor-type-existing-button"
                            style={({ pressed }) => [
                                styles.segmentButton,
                                donorMode === 'existing' && styles.segmentButtonActive,
                                pressed && styles.segmentPressed,
                            ]}
                            onPress={() => setDonorMode('existing')}
                        >
                            <Text
                                style={[
                                    styles.segmentText,
                                    donorMode === 'existing' && styles.segmentTextActive,
                                ]}
                            >
                                {t('admin.donations.form.existingUser')}
                            </Text>
                        </Pressable>
                        <Pressable
                            testID="donor-type-guest-button"
                            style={({ pressed }) => [
                                styles.segmentButton,
                                donorMode === 'guest' && styles.segmentButtonActive,
                                pressed && styles.segmentPressed,
                            ]}
                            onPress={() => setDonorMode('guest')}
                        >
                            <Text
                                style={[
                                    styles.segmentText,
                                    donorMode === 'guest' && styles.segmentTextActive,
                                ]}
                            >
                                {t('admin.donations.form.guestDonor')}
                            </Text>
                        </Pressable>
                    </View>

                    {donorMode === 'existing' ? (
                        <View style={styles.existingDonorWrap}>
                            <Pressable
                                testID="manual-donation-select-user"
                                style={({ pressed }) => [styles.selectUserButton, pressed && styles.selectUserButtonPressed]}
                                onPress={() => setUserModalVisible(true)}
                            >
                                <Ionicons name="people-outline" size={16} color={styles.selectUserButtonText.color} />
                                <Text style={styles.selectUserButtonText}>
                                    {selectedUser
                                        ? t('admin.donations.form.changeUser')
                                        : t('admin.donations.form.selectUser')}
                                </Text>
                            </Pressable>

                            <View style={styles.selectedUserCard}>
                                {selectedUser ? (
                                    <>
                                        <Text style={styles.selectedUserName}>{selectedUser.name || t('admin.donations.unknownDonor')}</Text>
                                        <Text style={styles.selectedUserEmail}>{selectedUser.email}</Text>
                                    </>
                                ) : (
                                    <Text style={styles.selectedUserEmpty}>{t('admin.donations.form.noUserSelected')}</Text>
                                )}
                            </View>
                        </View>
                    ) : (
                        <View style={styles.guestFieldsWrap}>
                            <View style={styles.formGroup}>
                                <Text style={styles.formLabel}>{t('admin.donations.form.guestName')} *</Text>
                                <TextInput
                                    testID="manual-donation-guest-name"
                                    style={styles.formInput}
                                    value={guestName}
                                    onChangeText={setGuestName}
                                    placeholder={t('admin.donations.form.guestNamePlaceholder')}
                                    placeholderTextColor={styles.placeholder.color}
                                />
                            </View>
                            <View style={styles.formGroup}>
                                <Text style={styles.formLabel}>{t('admin.donations.form.guestEmail')} *</Text>
                                <TextInput
                                    testID="manual-donation-guest-email"
                                    style={styles.formInput}
                                    value={guestEmail}
                                    onChangeText={setGuestEmail}
                                    placeholder={t('admin.donations.form.guestEmailPlaceholder')}
                                    keyboardType="email-address"
                                    autoCapitalize="none"
                                    placeholderTextColor={styles.placeholder.color}
                                />
                            </View>
                        </View>
                    )}
                </View>

                <View style={styles.sectionCard}>
                    <View style={styles.formGroup}>
                        <Text style={styles.formLabel}>{t('admin.donations.form.amount')} *</Text>
                        <TextInput
                            style={styles.formInput}
                            placeholder={t('admin.donations.form.amountPlaceholder')}
                            placeholderTextColor={styles.placeholder.color}
                            value={amountInput}
                            onChangeText={setAmountInput}
                            keyboardType="decimal-pad"
                        />
                    </View>

                    <View style={styles.formGroup}>
                        <Text style={styles.formLabel}>{t('admin.donations.form.currency')} *</Text>
                        <View style={styles.pickerWrapper}>
                            <Picker selectedValue={currency} onValueChange={(value: string) => setCurrency(value)}>
                                <Picker.Item label={t('admin.donations.currency.CAD')} value="CAD" />
                                <Picker.Item label={t('admin.donations.currency.USD')} value="USD" />
                                <Picker.Item label={t('admin.donations.currency.EUR')} value="EUR" />
                            </Picker>
                        </View>
                    </View>

                    <View style={styles.formGroup}>
                        <Text style={styles.formLabel}>{t('admin.donations.form.event')}</Text>
                        <View style={styles.pickerWrapper}>
                            {eventsLoading ? (
                                <View style={styles.inlineLoadingWrap}>
                                    <ActivityIndicator size="small" />
                                </View>
                            ) : (
                                <Picker selectedValue={eventId} onValueChange={(value: string) => setEventId(value)}>
                                    <Picker.Item label={t('admin.donations.form.noEvent')} value="" />
                                    {events.map((event) => (
                                        <Picker.Item key={event.id} label={event.title} value={String(event.id)} />
                                    ))}
                                </Picker>
                            )}
                        </View>
                    </View>

                    <View style={styles.formGroup}>
                        <Text style={styles.formLabel}>{t('admin.donations.form.date')} *</Text>
                        <TextInput
                            style={styles.formInput}
                            placeholder={t('admin.donations.form.datePlaceholder')}
                            placeholderTextColor={styles.placeholder.color}
                            value={donationDateInput}
                            onChangeText={setDonationDateInput}
                        />
                    </View>

                    <View style={styles.formGroup}>
                        <Text style={styles.formLabel}>{t('admin.donations.form.comments')}</Text>
                        <TextInput
                            style={[styles.formInput, styles.commentsInput]}
                            placeholder={t('admin.donations.form.commentsPlaceholder')}
                            placeholderTextColor={styles.placeholder.color}
                            value={comments}
                            onChangeText={setComments}
                            multiline
                            numberOfLines={4}
                        />
                    </View>
                </View>

                {submitError && <Text style={styles.errorText}>{submitError}</Text>}

                <View style={styles.formActions}>
                    <Pressable
                        style={({ pressed }) => [styles.cancelButton, pressed && styles.cancelButtonPressed]}
                        onPress={() => router.back()}
                        disabled={submitting}
                    >
                        <Text style={styles.cancelButtonText}>{t('common.cancel')}</Text>
                    </Pressable>
                    <Pressable
                        testID="manual-donation-submit"
                        style={({ pressed }) => [
                            styles.submitButton,
                            pressed && styles.submitButtonPressed,
                            submitting && styles.submitButtonDisabled,
                        ]}
                        onPress={handleSubmit}
                        disabled={submitting}
                    >
                        {submitting ? (
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <Text style={styles.submitButtonText}>{t('admin.donations.form.submit')}</Text>
                        )}
                    </Pressable>
                </View>
            </ScrollView>

            <Modal visible={userModalVisible} animationType="slide" transparent onRequestClose={() => setUserModalVisible(false)}>
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>{t('admin.donations.userPicker.title')}</Text>
                            <Pressable onPress={() => setUserModalVisible(false)}>
                                <Ionicons name="close" size={20} color={styles.modalTitle.color} />
                            </Pressable>
                        </View>

                        <TextInput
                            testID="manual-donation-user-search"
                            style={styles.formInput}
                            placeholder={t('admin.donations.userPicker.searchPlaceholder')}
                            placeholderTextColor={styles.placeholder.color}
                            value={userSearchQuery}
                            onChangeText={setUserSearchQuery}
                        />

                        {usersLoading ? (
                            <View style={styles.inlineLoadingWrap}>
                                <ActivityIndicator />
                            </View>
                        ) : (
                            <ScrollView style={styles.usersList}>
                                {filteredUsers.length === 0 ? (
                                    <Text style={styles.emptyUsersText}>{t('admin.donations.userPicker.empty')}</Text>
                                ) : (
                                    filteredUsers.map((user) => (
                                        <Pressable
                                            key={user.id}
                                            testID={`manual-donation-user-option-${user.id}`}
                                            style={({ pressed }) => [
                                                styles.userOption,
                                                selectedUser?.id === user.id && styles.userOptionSelected,
                                                pressed && styles.userOptionPressed,
                                            ]}
                                            onPress={() => {
                                                setSelectedUser(user);
                                                setUserModalVisible(false);
                                                setUserSearchQuery('');
                                                setSubmitError(null);
                                            }}
                                        >
                                            <Text style={styles.userOptionName}>{user.name || t('admin.donations.unknownDonor')}</Text>
                                            <Text style={styles.userOptionEmail}>{user.email}</Text>
                                        </Pressable>
                                    ))
                                )}
                            </ScrollView>
                        )}
                    </View>
                </View>
            </Modal>

            <NavigationMenu
                visible={menuVisible}
                onClose={() => setMenuVisible(false)}
                onNavigateHome={() => {
                    setMenuVisible(false);
                    router.replace('/');
                }}
                onNavigateEvents={() => {
                    setMenuVisible(false);
                    router.push('/events');
                }}
                onNavigateProfile={() => {
                    setMenuVisible(false);
                    router.push('/profile');
                }}
                onNavigateAttendance={() => {
                    setMenuVisible(false);
                    router.push('/admin/attendance');
                }}
                showAttendance={canView}
                onNavigateDashboard={() => {
                    setMenuVisible(false);
                    router.push('/admin');
                }}
                showDashboard={canView}
                t={t}
            />
        </View>
    );
}

const getStyles = (isDark: boolean) => {
    const SURFACE = isDark ? '#101722' : '#F3F7FD';
    const CARD = isDark ? '#182130' : '#FFFFFF';
    const BORDER = isDark ? '#2F3E56' : '#D6E2F3';
    const TEXT = isDark ? '#F0F4FA' : '#1A2B43';
    const MUTED = isDark ? '#A6B4C8' : '#5D708C';
    const ACCENT = isDark ? '#8FC1FF' : '#0C63B8';
    const ACCENT_BG = isDark ? 'rgba(143,193,255,0.15)' : '#E8F2FF';

    return StyleSheet.create({
        container: {
            flex: 1,
            backgroundColor: SURFACE,
        },
        content: {
            padding: 16,
            paddingBottom: 44,
            gap: 12,
        },
        backButton: {
            alignSelf: 'flex-start',
            flexDirection: 'row',
            alignItems: 'center',
            gap: 6,
            paddingHorizontal: 10,
            paddingVertical: 7,
            borderRadius: 999,
            backgroundColor: ACCENT_BG,
        },
        backButtonPressed: {
            opacity: 0.85,
        },
        backButtonText: {
            color: ACCENT,
            fontWeight: '700',
            fontSize: 12,
        },
        heroCard: {
            backgroundColor: CARD,
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 16,
            padding: 14,
            flexDirection: 'row',
            gap: 12,
            alignItems: 'center',
        },
        heroIconWrap: {
            width: 42,
            height: 42,
            borderRadius: 12,
            backgroundColor: ACCENT_BG,
            alignItems: 'center',
            justifyContent: 'center',
        },
        heroIcon: {
            color: ACCENT,
        },
        title: {
            color: TEXT,
            fontSize: 21,
            fontWeight: '800',
        },
        subtitle: {
            color: MUTED,
            marginTop: 3,
            fontSize: 13,
            lineHeight: 18,
        },
        sectionCard: {
            backgroundColor: CARD,
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 16,
            padding: 14,
            gap: 12,
        },
        sectionTitle: {
            color: TEXT,
            fontSize: 15,
            fontWeight: '700',
        },
        sectionHint: {
            color: MUTED,
            fontSize: 12,
            marginTop: -6,
        },
        segmentRow: {
            flexDirection: 'row',
            gap: 8,
        },
        segmentButton: {
            flex: 1,
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 10,
            paddingVertical: 10,
            alignItems: 'center',
            backgroundColor: isDark ? '#202A3A' : '#F5F9FF',
        },
        segmentButtonActive: {
            borderColor: ACCENT,
            backgroundColor: ACCENT_BG,
        },
        segmentPressed: {
            opacity: 0.9,
        },
        segmentText: {
            color: MUTED,
            fontWeight: '700',
            fontSize: 12,
        },
        segmentTextActive: {
            color: ACCENT,
        },
        existingDonorWrap: {
            gap: 10,
        },
        selectUserButton: {
            borderWidth: 1,
            borderColor: ACCENT,
            borderRadius: 10,
            paddingVertical: 10,
            alignItems: 'center',
            justifyContent: 'center',
            flexDirection: 'row',
            gap: 6,
            backgroundColor: ACCENT_BG,
        },
        selectUserButtonPressed: {
            opacity: 0.9,
        },
        selectUserButtonText: {
            color: ACCENT,
            fontWeight: '700',
            fontSize: 12,
        },
        selectedUserCard: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 10,
            padding: 10,
            backgroundColor: isDark ? '#1E2837' : '#FAFCFF',
        },
        selectedUserName: {
            color: TEXT,
            fontWeight: '700',
            fontSize: 14,
        },
        selectedUserEmail: {
            color: MUTED,
            marginTop: 3,
            fontSize: 12,
        },
        selectedUserEmpty: {
            color: MUTED,
            fontSize: 12,
        },
        guestFieldsWrap: {
            gap: 10,
        },
        formGroup: {
            gap: 6,
        },
        formLabel: {
            color: TEXT,
            fontSize: 12,
            fontWeight: '700',
        },
        formInput: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 10,
            backgroundColor: isDark ? '#1F2A3B' : '#FFFFFF',
            color: TEXT,
            paddingHorizontal: 10,
            paddingVertical: 9,
            fontSize: 14,
        },
        placeholder: {
            color: MUTED,
        },
        pickerWrapper: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 10,
            overflow: 'hidden',
            backgroundColor: isDark ? '#1F2A3B' : '#FFFFFF',
        },
        commentsInput: {
            minHeight: 84,
            textAlignVertical: 'top',
        },
        inlineLoadingWrap: {
            paddingVertical: 10,
            alignItems: 'center',
            justifyContent: 'center',
        },
        errorText: {
            color: '#D14343',
            fontWeight: '600',
            fontSize: 13,
        },
        formActions: {
            flexDirection: 'row',
            gap: 8,
            marginTop: 2,
        },
        cancelButton: {
            flex: 1,
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 11,
            paddingVertical: 12,
            alignItems: 'center',
            backgroundColor: CARD,
        },
        cancelButtonPressed: {
            opacity: 0.9,
        },
        cancelButtonText: {
            color: TEXT,
            fontWeight: '700',
            fontSize: 13,
        },
        submitButton: {
            flex: 1,
            borderRadius: 11,
            paddingVertical: 12,
            alignItems: 'center',
            backgroundColor: ACCENT,
            justifyContent: 'center',
        },
        submitButtonPressed: {
            opacity: 0.9,
        },
        submitButtonDisabled: {
            opacity: 0.6,
        },
        submitButtonText: {
            color: '#FFFFFF',
            fontWeight: '800',
            fontSize: 13,
        },
        modalOverlay: {
            flex: 1,
            backgroundColor: 'rgba(0,0,0,0.45)',
            justifyContent: 'center',
            padding: 16,
        },
        modalContent: {
            backgroundColor: CARD,
            borderRadius: 14,
            borderWidth: 1,
            borderColor: BORDER,
            padding: 14,
            maxHeight: '78%',
        },
        modalHeader: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 10,
        },
        modalTitle: {
            color: TEXT,
            fontSize: 16,
            fontWeight: '800',
        },
        usersList: {
            marginTop: 10,
        },
        emptyUsersText: {
            color: MUTED,
            paddingVertical: 8,
            fontSize: 13,
        },
        userOption: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 10,
            padding: 10,
            marginBottom: 8,
            backgroundColor: isDark ? '#1F2A3B' : '#FFFFFF',
        },
        userOptionSelected: {
            borderColor: ACCENT,
            backgroundColor: ACCENT_BG,
        },
        userOptionPressed: {
            opacity: 0.9,
        },
        userOptionName: {
            color: TEXT,
            fontSize: 13,
            fontWeight: '700',
        },
        userOptionEmail: {
            color: MUTED,
            marginTop: 3,
            fontSize: 12,
        },
    });
};
