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
    type DonationDetail,
    type DonationSummary,
} from '../../services/donation-management.service';

const formatAmount = (amount: number, currency: string) => `${currency} ${amount.toFixed(2)}`;

const formatTimestamp = (value: string | null) => {
    if (!value) return '';
    return value.replace('T', ' ').slice(0, 16);
};

export default function AdminDonationsScreen() {
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
    });
};
