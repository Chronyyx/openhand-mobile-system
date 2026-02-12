import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
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
import { useTranslation } from 'react-i18next';
import { Redirect, useRouter } from 'expo-router';

import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import {
    getDonationDetail,
    getManagedDonations,
    submitManualDonation,
    type DonationDetail,
    type DonationSummary,
    type ManualDonationFormData,
} from '../../services/donation-management.service';
import { getUpcomingEvents, type EventSummary } from '../../services/events.service';

const formatAmount = (amount: number, currency: string) => `${currency} ${amount.toFixed(2)}`;

const formatTimestamp = (value: string | null) => {
    if (!value) return '';
    return value.replace('T', ' ').slice(0, 16);
};

export default function AdminDonationsScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { user, hasRole } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const styles = getStyles(colorScheme === 'dark');
    const isAdmin = hasRole(['ROLE_ADMIN']);
    const canView = hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']);

    const [menuVisible, setMenuVisible] = useState(false);
    const [donations, setDonations] = useState<DonationSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');

    const [detailOpen, setDetailOpen] = useState(false);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState<string | null>(null);
    const [detail, setDetail] = useState<DonationDetail | null>(null);

    // Manual donation form state
    const [manualFormOpen, setManualFormOpen] = useState(false);
    const [manualFormLoading, setManualFormLoading] = useState(false);
    const [manualFormError, setManualFormError] = useState<string | null>(null);
    const [events, setEvents] = useState<EventSummary[]>([]);
    const [eventsLoading, setEventsLoading] = useState(false);
    const [formData, setFormData] = useState<ManualDonationFormData>({
        amount: 0,
        currency: 'CAD',
        eventId: null,
        donationDate: new Date().toISOString(),
        comments: '',
    });

    const loadDonations = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getManagedDonations();
            setDonations(data);
        } catch (err) {
            console.error('Failed to load donations for staff', err);
            setError(t('admin.donations.loadError'));
        } finally {
            setLoading(false);
        }
    }, [t]);

    useEffect(() => {
        if (canView) {
            loadDonations();
        }
    }, [canView, loadDonations]);

    const filteredDonations = useMemo(() => {
        const query = searchQuery.trim().toLowerCase();
        if (!query) return donations;
        return donations.filter((donation) => {
            const name = donation.donorName?.toLowerCase() ?? '';
            const email = donation.donorEmail?.toLowerCase() ?? '';
            return name.includes(query) || email.includes(query);
        });
    }, [donations, searchQuery]);

    const handleOpenDetail = async (donationId: number) => {
        if (!isAdmin) return;
        setDetailOpen(true);
        setDetailLoading(true);
        setDetailError(null);
        try {
            const data = await getDonationDetail(donationId);
            setDetail(data);
        } catch (err) {
            console.error('Failed to load donation detail', err);
            setDetailError(t('admin.donations.detailError'));
        } finally {
            setDetailLoading(false);
        }
    };

    const handleCloseDetail = () => {
        setDetailOpen(false);
        setDetail(null);
        setDetailError(null);
    };

    const handleOpenManualForm = useCallback(async () => {
        setManualFormOpen(true);
        setManualFormError(null);
        setEventsLoading(true);
        try {
            const loadedEvents = await getUpcomingEvents();
            setEvents(loadedEvents);
        } catch (err) {
            console.error('Failed to load events', err);
            setManualFormError(t('admin.donations.loadEventsError'));
        } finally {
            setEventsLoading(false);
        }
    }, [t]);

    const handleCloseManualForm = () => {
        setManualFormOpen(false);
        setManualFormError(null);
        setFormData({
            amount: 0,
            currency: 'CAD',
            eventId: null,
            donationDate: new Date().toISOString(),
            comments: '',
        });
    };

    const validateForm = (): boolean => {
        if (!formData.amount || formData.amount <= 0) {
            setManualFormError(t('admin.donations.amountRequired'));
            return false;
        }
        if (!user?.id) {
            setManualFormError(t('admin.donations.employeeNotFound'));
            return false;
        }
        return true;
    };

    const handleSubmitManualDonation = async () => {
        if (!validateForm() || !user?.id) return;

        setManualFormLoading(true);
        setManualFormError(null);
        try {
            await submitManualDonation(user.id, formData);
            // Reload donations list
            await loadDonations();
            handleCloseManualForm();
        } catch (err) {
            console.error('Failed to create manual donation', err);
            setManualFormError(t('admin.donations.submitError'));
        } finally {
            setManualFormLoading(false);
        }
    };

    if (!canView) {
        return <Redirect href="/" />;
    }

    return (
        <View style={styles.container}>
            <AppHeader onMenuPress={() => setMenuVisible(true)} />

            <ScrollView contentContainerStyle={styles.content}>
                <Text style={styles.title}>{t('admin.donations.title')}</Text>
                <Text style={styles.subtitle}>{t('admin.donations.subtitle')}</Text>

                <View style={styles.searchWrapper}>
                    <Ionicons name="search" size={18} color={styles.icon.color} />
                    <TextInput
                        style={styles.searchInput}
                        placeholder={t('admin.donations.searchPlaceholder')}
                        placeholderTextColor={styles.placeholder.color}
                        value={searchQuery}
                        onChangeText={setSearchQuery}
                    />
                </View>

                {isAdmin && (
                    <Pressable
                        style={({ pressed }) => [styles.addButton, pressed && styles.addButtonPressed]}
                        onPress={handleOpenManualForm}
                        accessibilityRole="button"
                        accessibilityLabel={t('admin.donations.addButton')}
                    >
                        <Ionicons name="add-circle" size={20} color="#FFFFFF" />
                        <Text style={styles.addButtonText}>{t('admin.donations.addButton')}</Text>
                    </Pressable>
                )}

                {loading ? (
                    <View style={styles.centered}>
                        <ActivityIndicator />
                        <Text style={styles.loadingText}>{t('common.loading')}</Text>
                    </View>
                ) : (
                    <>
                        {error && <Text style={styles.errorText}>{error}</Text>}
                        {filteredDonations.length === 0 ? (
                            <View style={styles.emptyState}>
                                <Text style={styles.emptyText}>{t('admin.donations.empty')}</Text>
                            </View>
                        ) : (
                            <View style={styles.list}>
                                {filteredDonations.map((donation) => {
                                    const donorLabel = donation.donorName?.trim()
                                        ? donation.donorName
                                        : donation.donorEmail ?? t('admin.donations.unknownDonor');
                                    return (
                                        <View key={donation.id} style={styles.card}>
                                            <View style={styles.cardHeader}>
                                                <View>
                                                    <Text style={styles.cardTitle}>{donorLabel}</Text>
                                                    {donation.donorEmail && (
                                                        <Text style={styles.cardSubtitle}>{donation.donorEmail}</Text>
                                                    )}
                                                </View>
                                                <View style={styles.amountBadge}>
                                                    <Text style={styles.amountText}>
                                                        {formatAmount(donation.amount, donation.currency)}
                                                    </Text>
                                                </View>
                                            </View>

                                            <View style={styles.metaRow}>
                                                <Text style={styles.metaLabel}>{t('admin.donations.fields.status')}:</Text>
                                                <Text style={styles.metaValue}>
                                                    {t(`admin.donations.status.${donation.status}`, { defaultValue: donation.status })}
                                                </Text>
                                            </View>
                                            <View style={styles.metaRow}>
                                                <Text style={styles.metaLabel}>{t('admin.donations.fields.frequency')}:</Text>
                                                <Text style={styles.metaValue}>
                                                    {t(`admin.donations.frequency.${donation.frequency}`, { defaultValue: donation.frequency })}
                                                </Text>
                                            </View>
                                            <View style={styles.metaRow}>
                                                <Text style={styles.metaLabel}>{t('admin.donations.fields.createdAt')}:</Text>
                                                <Text style={styles.metaValue}>{formatTimestamp(donation.createdAt)}</Text>
                                            </View>

                                            {isAdmin && (
                                                <Pressable
                                                    style={({ pressed }) => [
                                                        styles.detailButton,
                                                        pressed && styles.detailButtonPressed,
                                                    ]}
                                                    onPress={() => handleOpenDetail(donation.id)}
                                                    accessibilityRole="button"
                                                    accessibilityLabel={t('admin.donations.viewDetails')}
                                                >
                                                    <Text style={styles.detailButtonText}>
                                                        {t('admin.donations.viewDetails')}
                                                    </Text>
                                                    <Ionicons name="chevron-forward" size={16} color={styles.detailButtonText.color} />
                                                </Pressable>
                                            )}
                                        </View>
                                    );
                                })}
                            </View>
                        )}
                    </>
                )}
            </ScrollView>

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
                onNavigateAdminDonations={() => {
                    setMenuVisible(false);
                    router.push('/admin/donations');
                }}
                showAdminDonations={canView}
                t={t}
            />

            <Modal visible={detailOpen} transparent animationType="fade" onRequestClose={handleCloseDetail}>
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>{t('admin.donations.detailTitle')}</Text>
                            <Pressable
                                onPress={handleCloseDetail}
                                accessibilityRole="button"
                                accessibilityLabel={t('common.close')}
                            >
                                <Ionicons name="close" size={20} color={styles.modalTitle.color} />
                            </Pressable>
                        </View>

                        {detailLoading ? (
                            <View style={styles.centered}>
                                <ActivityIndicator />
                                <Text style={styles.loadingText}>{t('common.loading')}</Text>
                            </View>
                        ) : detailError ? (
                            <Text style={styles.errorText}>{detailError}</Text>
                        ) : detail ? (
                            <View>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.donor')}: {detail.donorName ?? t('admin.donations.unknownDonor')}
                                </Text>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.email')}: {detail.donorEmail ?? '-'}
                                </Text>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.phone')}: {detail.donorPhone ?? '-'}
                                </Text>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.amount')}: {formatAmount(detail.amount, detail.currency)}
                                </Text>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.frequency')}: {t(`admin.donations.frequency.${detail.frequency}`, { defaultValue: detail.frequency })}
                                </Text>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.status')}: {t(`admin.donations.status.${detail.status}`, { defaultValue: detail.status })}
                                </Text>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.createdAt')}: {formatTimestamp(detail.createdAt)}
                                </Text>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.paymentProvider')}: {detail.paymentProvider ?? '-'}
                                </Text>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.paymentReference')}: {detail.paymentReference ?? '-'}
                                </Text>
                            </View>
                        ) : null}
                    </View>
                </View>
            </Modal>

            <Modal visible={manualFormOpen} transparent animationType="slide" onRequestClose={handleCloseManualForm}>
                <View style={styles.modalOverlay}>
                    <View style={[styles.modalContent, styles.largeModalContent]}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>{t('admin.donations.addDonationTitle')}</Text>
                            <Pressable
                                onPress={handleCloseManualForm}
                                accessibilityRole="button"
                                accessibilityLabel={t('common.close')}
                            >
                                <Ionicons name="close" size={20} color={styles.modalTitle.color} />
                            </Pressable>
                        </View>

                        {manualFormError && <Text style={styles.errorText}>{manualFormError}</Text>}

                        <ScrollView style={styles.formScrollView}>
                            <View style={styles.formGroup}>
                                <Text style={styles.formLabel}>{t('admin.donations.form.amount')} *</Text>
                                <TextInput
                                    style={styles.formInput}
                                    placeholder="0.00"
                                    placeholderTextColor={styles.placeholder.color}
                                    value={formData.amount ? formData.amount.toString() : ''}
                                    onChangeText={(text) => setFormData({ ...formData, amount: parseFloat(text) || 0 })}
                                    keyboardType="decimal-pad"
                                />
                            </View>

                            <View style={styles.formGroup}>
                                <Text style={styles.formLabel}>{t('admin.donations.form.currency')} *</Text>
                                <View style={styles.pickerWrapper}>
                                    <Picker
                                        selectedValue={formData.currency}
                                        onValueChange={(value: string) => setFormData({ ...formData, currency: value })}
                                        style={styles.picker}
                                    >
                                        <Picker.Item label="CAD" value="CAD" />
                                        <Picker.Item label="USD" value="USD" />
                                        <Picker.Item label="EUR" value="EUR" />
                                    </Picker>
                                </View>
                            </View>

                            <View style={styles.formGroup}>
                                <Text style={styles.formLabel}>{t('admin.donations.form.event')}</Text>
                                <View style={styles.pickerWrapper}>
                                    {eventsLoading ? (
                                        <Text style={styles.loadingText}>{t('common.loading')}</Text>
                                    ) : (
                                        <Picker
                                            selectedValue={formData.eventId || null}
                                            onValueChange={(value: number | null) => setFormData({ ...formData, eventId: value })}
                                            style={styles.picker}
                                        >
                                            <Picker.Item label={t('admin.donations.form.noEvent')} value={null} />
                                            {events.map((event) => (
                                                <Picker.Item key={event.id} label={event.title} value={event.id} />
                                            ))}
                                        </Picker>
                                    )}
                                </View>
                            </View>

                            <View style={styles.formGroup}>
                                <Text style={styles.formLabel}>{t('admin.donations.form.date')} *</Text>
                                <TextInput
                                    style={styles.formInput}
                                    placeholder="YYYY-MM-DD HH:MM"
                                    placeholderTextColor={styles.placeholder.color}
                                    value={formData.donationDate ? formData.donationDate.slice(0, 16).replace('T', ' ') : ''}
                                    onChangeText={(text) => {
                                        const dateStr = text.replace(' ', 'T');
                                        setFormData({ ...formData, donationDate: dateStr + ':00' });
                                    }}
                                />
                            </View>

                            <View style={styles.formGroup}>
                                <Text style={styles.formLabel}>{t('admin.donations.form.employeeId')}</Text>
                                <Text style={styles.readOnlyField}>{user?.id || t('admin.donations.notAvailable')}</Text>
                            </View>

                            <View style={styles.formGroup}>
                                <Text style={styles.formLabel}>{t('admin.donations.form.comments')}</Text>
                                <TextInput
                                    style={[styles.formInput, styles.textAreaInput]}
                                    placeholder={t('admin.donations.form.commentsPlaceholder')}
                                    placeholderTextColor={styles.placeholder.color}
                                    value={formData.comments}
                                    onChangeText={(text) => setFormData({ ...formData, comments: text })}
                                    multiline
                                    numberOfLines={4}
                                />
                            </View>
                        </ScrollView>

                        <View style={styles.formActions}>
                            <Pressable
                                style={({ pressed }) => [styles.cancelButton, pressed && styles.buttonPressed]}
                                onPress={handleCloseManualForm}
                                disabled={manualFormLoading}
                            >
                                <Text style={styles.cancelButtonText}>{t('common.cancel')}</Text>
                            </Pressable>
                            <Pressable
                                style={({ pressed }) => [styles.submitButton, pressed && styles.buttonPressed, manualFormLoading && styles.buttonDisabled]}
                                onPress={handleSubmitManualDonation}
                                disabled={manualFormLoading}
                            >
                                {manualFormLoading ? (
                                    <ActivityIndicator color="#FFFFFF" />
                                ) : (
                                    <Text style={styles.submitButtonText}>{t('admin.donations.form.submit')}</Text>
                                )}
                            </Pressable>
                        </View>
                    </View>
                </View>
            </Modal>
        </View>
    );
}

const getStyles = (isDark: boolean) => {
    const BG = isDark ? '#0F1419' : '#FFFFFF';
    const SURFACE = isDark ? '#141A21' : '#F5F7FB';
    const TEXT = isDark ? '#ECEDEE' : '#1E2A3B';
    const MUTED = isDark ? '#A0A7B1' : '#5C6A80';
    const BORDER = isDark ? '#2F3A4A' : '#E0E7F3';
    const ACCENT = isDark ? '#9FC3FF' : '#0056A8';
    const CARD = isDark ? '#1B222C' : '#FFFFFF';

    return StyleSheet.create({
        container: {
            flex: 1,
            backgroundColor: SURFACE,
        },
        content: {
            padding: 18,
            paddingBottom: 40,
        },
        title: {
            fontSize: 22,
            fontWeight: '700',
            color: TEXT,
        },
        subtitle: {
            color: MUTED,
            marginTop: 4,
            marginBottom: 16,
        },
        searchWrapper: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 8,
            borderWidth: 1,
            borderColor: BORDER,
            backgroundColor: BG,
            borderRadius: 12,
            paddingHorizontal: 12,
            paddingVertical: 10,
            marginBottom: 16,
        },
        searchInput: {
            flex: 1,
            color: TEXT,
        },
        placeholder: {
            color: MUTED,
        },
        icon: {
            color: MUTED,
        },
        centered: {
            alignItems: 'center',
            justifyContent: 'center',
            paddingVertical: 20,
        },
        loadingText: {
            marginTop: 8,
            color: MUTED,
        },
        errorText: {
            color: '#D93025',
            marginBottom: 12,
        },
        emptyState: {
            paddingVertical: 24,
            alignItems: 'center',
        },
        emptyText: {
            color: MUTED,
        },
        list: {
            gap: 12,
        },
        card: {
            backgroundColor: CARD,
            borderRadius: 14,
            padding: 14,
            borderWidth: 1,
            borderColor: BORDER,
            shadowColor: '#000',
            shadowOpacity: 0.06,
            shadowRadius: 8,
            shadowOffset: { width: 0, height: 3 },
            elevation: 3,
        },
        cardHeader: {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: 10,
        },
        cardTitle: {
            fontSize: 16,
            fontWeight: '700',
            color: TEXT,
        },
        cardSubtitle: {
            color: MUTED,
            marginTop: 2,
        },
        amountBadge: {
            backgroundColor: isDark ? '#1D2A3A' : '#EAF1FF',
            paddingHorizontal: 10,
            paddingVertical: 6,
            borderRadius: 12,
        },
        amountText: {
            color: ACCENT,
            fontWeight: '700',
        },
        metaRow: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            marginTop: 6,
        },
        metaLabel: {
            color: MUTED,
        },
        metaValue: {
            color: TEXT,
            fontWeight: '600',
        },
        detailButton: {
            marginTop: 12,
            paddingVertical: 10,
            paddingHorizontal: 12,
            borderRadius: 10,
            backgroundColor: isDark ? '#1D2A3A' : '#EAF1FF',
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
        },
        detailButtonPressed: {
            transform: [{ scale: 0.99 }],
        },
        detailButtonText: {
            color: ACCENT,
            fontWeight: '700',
        },
        modalOverlay: {
            flex: 1,
            backgroundColor: 'rgba(0,0,0,0.5)',
            justifyContent: 'center',
            padding: 20,
        },
        modalContent: {
            backgroundColor: BG,
            borderRadius: 16,
            padding: 16,
        },
        largeModalContent: {
            maxHeight: '90%',
        },
        modalHeader: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 12,
        },
        modalTitle: {
            color: TEXT,
            fontSize: 18,
            fontWeight: '700',
        },
        detailLine: {
            color: TEXT,
            marginBottom: 6,
        },
        addButton: {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 8,
            backgroundColor: ACCENT,
            paddingVertical: 12,
            paddingHorizontal: 16,
            borderRadius: 10,
            marginBottom: 16,
        },
        addButtonPressed: {
            transform: [{ scale: 0.98 }],
        },
        addButtonText: {
            color: '#FFFFFF',
            fontWeight: '700',
            fontSize: 16,
        },
        formScrollView: {
            maxHeight: '60%',
            marginBottom: 16,
        },
        formGroup: {
            marginBottom: 16,
        },
        formLabel: {
            color: TEXT,
            fontWeight: '600',
            marginBottom: 6,
            fontSize: 14,
        },
        formInput: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 8,
            padding: 10,
            color: TEXT,
            backgroundColor: BG,
        },
        textAreaInput: {
            minHeight: 100,
            textAlignVertical: 'top',
        },
        pickerWrapper: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 8,
            backgroundColor: BG,
            overflow: 'hidden',
        },
        picker: {
            color: TEXT,
        },
        readOnlyField: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 8,
            padding: 10,
            color: MUTED,
            backgroundColor: '#f0f0f0',
            fontSize: 14,
        },
        formActions: {
            flexDirection: 'row',
            gap: 12,
            justifyContent: 'flex-end',
        },
        cancelButton: {
            paddingVertical: 10,
            paddingHorizontal: 20,
            borderRadius: 8,
            borderWidth: 1,
            borderColor: BORDER,
            backgroundColor: 'transparent',
        },
        cancelButtonText: {
            color: TEXT,
            fontWeight: '600',
        },
        submitButton: {
            paddingVertical: 10,
            paddingHorizontal: 20,
            borderRadius: 8,
            backgroundColor: ACCENT,
        },
        submitButtonText: {
            color: '#FFFFFF',
            fontWeight: '600',
        },
        buttonPressed: {
            transform: [{ scale: 0.98 }],
        },
        buttonDisabled: {
            opacity: 0.6,        },
    });
};