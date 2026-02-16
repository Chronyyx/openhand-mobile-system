import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    Modal,
    Platform,
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
import { Redirect, useFocusEffect, useRouter } from 'expo-router';

import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import {
    getDonationDetail,
    getManagedDonations,
    type DonationDetail,
    type DonationSummary,
} from '../../services/donation-management.service';
import { getAllEvents, type EventSummary } from '../../services/events.service';

const formatAmount = (amount: number, currency: string) => `${currency} ${amount.toFixed(2)}`;

const formatTimestamp = (value: string | null) => {
    if (!value) return '';
    return value.replace('T', ' ').slice(0, 16);
};

export default function AdminDonationsScreen() {
    const [eventId, setEventId] = useState<string>('');
    const [year, setYear] = useState('');
    const [month, setMonth] = useState('');
    const [day, setDay] = useState('');
    const [events, setEvents] = useState<EventSummary[]>([]);
    const [eventsLoading, setEventsLoading] = useState(false);

    const router = useRouter();
    const { t } = useTranslation();
    const { hasRole } = useAuth();
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

    const loadDonations = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const filters: { eventId?: number; year?: number; month?: number; day?: number } = {};
            if (eventId && !isNaN(Number(eventId)) && Number(eventId) > 0) {
                filters.eventId = parseInt(eventId, 10);
            }
            if (year) filters.year = parseInt(year, 10);
            if (month) filters.month = parseInt(month, 10);
            if (day) filters.day = parseInt(day, 10);
            const data = await getManagedDonations(filters);
            setDonations(data);
        } catch (err: any) {
            console.error('Failed to load donations for staff', err);
            setError(err?.response?.data?.message || t('admin.donations.loadError'));
        } finally {
            setLoading(false);
        }
    }, [day, eventId, month, t, year]);

    useFocusEffect(
        useCallback(() => {
            if (canView) {
                void loadDonations();
            }
        }, [canView, loadDonations]),
    );

    useEffect(() => {
        setEventsLoading(true);
        getAllEvents()
            .then(setEvents)
            .catch((err) => {
                console.error('Failed to load events', err);
            })
            .finally(() => setEventsLoading(false));
    }, []);

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

    if (!canView) {
        return <Redirect href="/" />;
    }

    return (
        <View style={styles.container}>
            <AppHeader onMenuPress={() => setMenuVisible(true)} />
            <ScrollView contentContainerStyle={styles.content}>
                <Text style={styles.title}>{t('admin.donations.title')}</Text>
                <Text style={styles.subtitle}>{t('admin.donations.subtitle')}</Text>

                <View style={styles.filterSection}>
                    <View style={styles.filterGroupFullWidth}>
                        <Text style={styles.filterLabel}>{t('admin.donations.filter.eventName')}</Text>
                        <View style={styles.pickerWrapper}>
                            {eventsLoading ? (
                                <ActivityIndicator size="small" />
                            ) : Platform.OS === 'web' ? (
                                <select
                                    data-testid="event-filter-dropdown"
                                    value={eventId ?? ''}
                                    onChange={(e) => setEventId(e.target.value)}
                                    style={{
                                        width: '100%',
                                        padding: 8,
                                        borderRadius: 6,
                                        borderColor: '#E0E7F3',
                                        fontSize: 15,
                                    }}
                                >
                                    <option value="">{t('admin.donations.filter.eventNamePlaceholder')}</option>
                                    {events.map((event) => (
                                        <option key={event.id} value={event.id}>
                                            {event.title}
                                        </option>
                                    ))}
                                </select>
                            ) : (
                                <Picker selectedValue={eventId} onValueChange={setEventId}>
                                    <Picker.Item label={t('admin.donations.filter.eventNamePlaceholder')} value="" />
                                    {events.map((event) => (
                                        <Picker.Item key={event.id} label={event.title} value={String(event.id)} />
                                    ))}
                                </Picker>
                            )}
                        </View>
                    </View>
                    <View style={styles.dateFiltersRow}>
                        <View style={styles.filterGroupSmall}>
                            <Text style={styles.filterLabel}>{t('admin.donations.filter.year')}</Text>
                            <TextInput
                                style={styles.filterInput}
                                placeholder={t('admin.donations.filter.yearPlaceholder')}
                                value={year}
                                onChangeText={setYear}
                                keyboardType="numeric"
                                maxLength={4}
                                testID="date-filter-year"
                            />
                        </View>
                        <View style={styles.filterGroupSmall}>
                            <Text style={styles.filterLabel}>{t('admin.donations.filter.month')}</Text>
                            <TextInput
                                style={styles.filterInput}
                                placeholder={t('admin.donations.filter.monthPlaceholder')}
                                value={month}
                                onChangeText={setMonth}
                                keyboardType="numeric"
                                maxLength={2}
                                testID="date-filter-month"
                            />
                        </View>
                        <View style={styles.filterGroupSmall}>
                            <Text style={styles.filterLabel}>{t('admin.donations.filter.day')}</Text>
                            <TextInput
                                style={styles.filterInput}
                                placeholder={t('admin.donations.filter.dayPlaceholder')}
                                value={day}
                                onChangeText={setDay}
                                keyboardType="numeric"
                                maxLength={2}
                                testID="date-filter-day"
                            />
                        </View>
                    </View>
                </View>

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
                        testID="open-manual-donation-screen"
                        style={({ pressed }) => [styles.addButton, pressed && styles.addButtonPressed]}
                        onPress={() => router.push('/admin/manual-donation')}
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
                                        <View key={donation.id} style={styles.card} data-testid="donation-card">
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
                                                    {t(`admin.donations.status.${donation.status}`, {
                                                        defaultValue: donation.status,
                                                    })}
                                                </Text>
                                            </View>
                                            <View style={styles.metaRow}>
                                                <Text style={styles.metaLabel}>{t('admin.donations.fields.frequency')}:</Text>
                                                <Text style={styles.metaValue}>
                                                    {t(`admin.donations.frequency.${donation.frequency}`, {
                                                        defaultValue: donation.frequency,
                                                    })}
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
                                                    <Text style={styles.detailButtonText}>{t('admin.donations.viewDetails')}</Text>
                                                    <Ionicons
                                                        name="chevron-forward"
                                                        size={16}
                                                        color={styles.detailButtonText.color}
                                                    />
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
                                    {t('admin.donations.fields.event')}: {detail.eventName ? detail.eventName : '-'}
                                </Text>
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
                                    {t('admin.donations.fields.frequency')}:{' '}
                                    {t(`admin.donations.frequency.${detail.frequency}`, {
                                        defaultValue: detail.frequency,
                                    })}
                                </Text>
                                <Text style={styles.detailLine}>
                                    {t('admin.donations.fields.status')}:{' '}
                                    {t(`admin.donations.status.${detail.status}`, {
                                        defaultValue: detail.status,
                                    })}
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
        </View>
    );
}

const getStyles = (isDark: boolean) => {
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
            fontSize: 24,
            fontWeight: '800',
            color: TEXT,
            marginBottom: 4,
        },
        subtitle: {
            fontSize: 14,
            color: MUTED,
            marginBottom: 18,
        },
        filterSection: {
            marginBottom: 16,
        },
        filterGroupFullWidth: {
            marginBottom: 10,
        },
        dateFiltersRow: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            gap: 8,
        },
        filterGroupSmall: {
            flex: 1,
        },
        filterLabel: {
            color: TEXT,
            fontSize: 12,
            marginBottom: 6,
            fontWeight: '600',
        },
        filterInput: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 10,
            backgroundColor: CARD,
            color: TEXT,
            paddingHorizontal: 10,
            paddingVertical: 8,
            fontSize: 14,
        },
        pickerWrapper: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 10,
            backgroundColor: CARD,
            overflow: 'hidden',
        },
        searchWrapper: {
            flexDirection: 'row',
            alignItems: 'center',
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 12,
            backgroundColor: CARD,
            paddingHorizontal: 12,
            marginBottom: 14,
            gap: 8,
        },
        searchInput: {
            flex: 1,
            color: TEXT,
            paddingVertical: 10,
            fontSize: 14,
        },
        icon: {
            color: MUTED,
        },
        placeholder: {
            color: MUTED,
        },
        addButton: {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 8,
            backgroundColor: ACCENT,
            paddingVertical: 12,
            borderRadius: 12,
            marginBottom: 16,
        },
        addButtonPressed: {
            opacity: 0.9,
        },
        addButtonText: {
            color: '#FFFFFF',
            fontWeight: '700',
            fontSize: 15,
        },
        centered: {
            alignItems: 'center',
            justifyContent: 'center',
            paddingVertical: 24,
        },
        loadingText: {
            color: MUTED,
            marginTop: 8,
        },
        errorText: {
            color: '#D14343',
            marginBottom: 10,
            fontWeight: '600',
        },
        emptyState: {
            alignItems: 'center',
            justifyContent: 'center',
            paddingVertical: 30,
        },
        emptyText: {
            color: MUTED,
            fontSize: 14,
        },
        list: {
            gap: 12,
        },
        card: {
            backgroundColor: CARD,
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 14,
            padding: 14,
            gap: 6,
            shadowColor: '#000',
            shadowOpacity: 0.04,
            shadowRadius: 8,
            shadowOffset: { width: 0, height: 3 },
            elevation: 2,
        },
        cardHeader: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            marginBottom: 6,
        },
        cardTitle: {
            color: TEXT,
            fontSize: 16,
            fontWeight: '700',
            marginBottom: 2,
        },
        cardSubtitle: {
            color: MUTED,
            fontSize: 12,
        },
        amountBadge: {
            backgroundColor: isDark ? '#1D2A3A' : '#EAF1FF',
            paddingHorizontal: 10,
            paddingVertical: 6,
            borderRadius: 999,
        },
        amountText: {
            color: ACCENT,
            fontWeight: '700',
            fontSize: 13,
        },
        metaRow: {
            flexDirection: 'row',
            gap: 6,
        },
        metaLabel: {
            color: MUTED,
            fontWeight: '600',
            fontSize: 12,
        },
        metaValue: {
            color: TEXT,
            fontSize: 12,
            flexShrink: 1,
        },
        detailButton: {
            marginTop: 8,
            alignSelf: 'flex-start',
            flexDirection: 'row',
            alignItems: 'center',
            gap: 4,
            paddingHorizontal: 10,
            paddingVertical: 7,
            borderRadius: 10,
            borderWidth: 1,
            borderColor: BORDER,
            backgroundColor: isDark ? '#202A36' : '#F6F9FF',
        },
        detailButtonPressed: {
            opacity: 0.9,
        },
        detailButtonText: {
            color: ACCENT,
            fontWeight: '700',
            fontSize: 12,
        },
        modalOverlay: {
            flex: 1,
            backgroundColor: 'rgba(0,0,0,0.45)',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 18,
        },
        modalContent: {
            width: '100%',
            borderRadius: 14,
            padding: 16,
            backgroundColor: CARD,
            borderWidth: 1,
            borderColor: BORDER,
        },
        modalHeader: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 12,
        },
        modalTitle: {
            color: TEXT,
            fontSize: 17,
            fontWeight: '800',
        },
        detailLine: {
            color: TEXT,
            marginBottom: 6,
            fontSize: 13,
        },
    });
};
