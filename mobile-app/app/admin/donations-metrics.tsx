import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    Pressable,
    RefreshControl,
    ScrollView,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Redirect, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import {
    getDonationMetrics,
    type DonationMetricBreakdown,
    type DonationMetrics,
    type DonationTopDonor,
} from '../../services/donation-metrics.service';

type TopListMode = 'amount' | 'count';

const formatCurrency = (amount: number, currency: string) => `${currency} ${Number(amount || 0).toFixed(2)}`;

const formatPercent = (value: number) => `${Number.isFinite(value) ? value.toFixed(2) : '0.00'}%`;

const formatMonthLabel = (period: string) => {
    const parts = period.split('-');
    if (parts.length < 2) return period;
    const year = Number(parts[0]);
    const monthIndex = Number(parts[1]) - 1;
    if (Number.isNaN(year) || Number.isNaN(monthIndex)) return period;

    try {
        const date = new Date(year, monthIndex, 1);
        return date.toLocaleString(undefined, { month: 'short', year: 'numeric' });
    } catch {
        return period;
    }
};

const BreakdownCard = ({
    title,
    entries,
    currency,
    maxAmount,
    resolveLabel,
    styles,
}: {
    title: string;
    entries: DonationMetricBreakdown[];
    currency: string;
    maxAmount: number;
    resolveLabel: (key: string) => string;
    styles: ReturnType<typeof getStyles>;
}) => (
    <View style={styles.sectionCard}>
        <Text style={styles.sectionTitle}>{title}</Text>
        {entries.length === 0 ? (
            <Text style={styles.mutedText}>-</Text>
        ) : (
            entries.map((item) => {
                const widthPct = maxAmount > 0 ? Math.max(8, (item.amount / maxAmount) * 100) : 0;
                return (
                    <View key={item.key} style={styles.breakdownRow}>
                        <View style={styles.breakdownHeader}>
                            <Text style={styles.breakdownKey}>{resolveLabel(item.key)}</Text>
                            <Text style={styles.breakdownMeta}>
                                {item.count} - {formatCurrency(item.amount, currency)}
                            </Text>
                        </View>
                        <View style={styles.barTrack}>
                            <View style={[styles.barFill, { width: `${Math.min(100, widthPct)}%` }]} />
                        </View>
                    </View>
                );
            })
        )}
    </View>
);

export default function DonationMetricsScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { hasRole } = useAuth();
    const isDark = (useColorScheme() ?? 'light') === 'dark';
    const styles = getStyles(isDark);

    const canView = hasRole(['ROLE_ADMIN']);
    const [menuVisible, setMenuVisible] = useState(false);
    const [metrics, setMetrics] = useState<DonationMetrics | null>(null);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [topListMode, setTopListMode] = useState<TopListMode>('amount');

    const loadMetrics = useCallback(
        async (asRefresh = false) => {
            if (asRefresh) {
                setRefreshing(true);
            } else {
                setLoading(true);
            }
            setError(null);
            try {
                const data = await getDonationMetrics();
                setMetrics(data);
            } catch (err) {
                console.error('Failed to load donation metrics', err);
                setError(t('admin.donationMetrics.loadError'));
            } finally {
                setLoading(false);
                setRefreshing(false);
            }
        },
        [t],
    );

    useEffect(() => {
        loadMetrics();
    }, [loadMetrics]);

    const topDonors: DonationTopDonor[] = useMemo(() => {
        if (!metrics) return [];
        return topListMode === 'amount' ? metrics.topDonorsByAmount : metrics.topDonorsByCount;
    }, [metrics, topListMode]);

    const trendMaxAmount = useMemo(() => {
        if (!metrics?.monthlyTrend?.length) return 0;
        return Math.max(...metrics.monthlyTrend.map((item) => Number(item.amount || 0)));
    }, [metrics]);

    const frequencyMaxAmount = useMemo(() => {
        if (!metrics?.frequencyBreakdown?.length) return 0;
        return Math.max(...metrics.frequencyBreakdown.map((item) => Number(item.amount || 0)));
    }, [metrics]);

    const statusMaxAmount = useMemo(() => {
        if (!metrics?.statusBreakdown?.length) return 0;
        return Math.max(...metrics.statusBreakdown.map((item) => Number(item.amount || 0)));
    }, [metrics]);

    if (!canView) {
        return <Redirect href="/admin" />;
    }

    return (
        <View style={styles.container}>
            <AppHeader onMenuPress={() => setMenuVisible(true)} />
            <ScrollView
                contentContainerStyle={styles.content}
                refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => loadMetrics(true)} />}
            >
                <View style={styles.headerRow}>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.title}>{t('admin.donationMetrics.title')}</Text>
                        <Text style={styles.subtitle}>{t('admin.donationMetrics.subtitle')}</Text>
                    </View>
                    <Pressable
                        style={({ pressed }) => [styles.refreshButton, pressed && styles.refreshButtonPressed]}
                        onPress={() => loadMetrics(true)}
                        accessibilityRole="button"
                        accessibilityLabel={t('admin.donationMetrics.refresh')}
                    >
                        <Ionicons name="refresh" size={18} color={styles.refreshIcon.color} />
                    </Pressable>
                </View>

                {loading ? (
                    <View style={styles.centered}>
                        <ActivityIndicator />
                        <Text style={styles.loadingText}>{t('common.loading')}</Text>
                    </View>
                ) : error ? (
                    <View style={styles.errorCard}>
                        <Ionicons name="alert-circle-outline" size={20} color={styles.errorText.color} />
                        <Text style={styles.errorText}>{error}</Text>
                    </View>
                ) : metrics ? (
                    <>
                        <View style={styles.kpiGrid}>
                            <View style={styles.kpiCard}>
                                <Text style={styles.kpiLabel}>{t('admin.donationMetrics.cards.totalAmount')}</Text>
                                <Text style={styles.kpiValue}>{formatCurrency(metrics.totalAmount, metrics.currency)}</Text>
                            </View>
                            <View style={styles.kpiCard}>
                                <Text style={styles.kpiLabel}>{t('admin.donationMetrics.cards.totalDonations')}</Text>
                                <Text style={styles.kpiValue}>{metrics.totalDonations}</Text>
                            </View>
                            <View style={styles.kpiCard}>
                                <Text style={styles.kpiLabel}>{t('admin.donationMetrics.cards.averageAmount')}</Text>
                                <Text style={styles.kpiValue}>{formatCurrency(metrics.averageAmount, metrics.currency)}</Text>
                            </View>
                            <View style={styles.kpiCard}>
                                <Text style={styles.kpiLabel}>{t('admin.donationMetrics.cards.uniqueDonors')}</Text>
                                <Text style={styles.kpiValue}>{metrics.uniqueDonors}</Text>
                            </View>
                            <View style={styles.kpiCard}>
                                <Text style={styles.kpiLabel}>{t('admin.donationMetrics.cards.repeatDonors')}</Text>
                                <Text style={styles.kpiValue}>{metrics.repeatDonors}</Text>
                            </View>
                            <View style={styles.kpiCard}>
                                <Text style={styles.kpiLabel}>{t('admin.donationMetrics.cards.firstTimeDonors')}</Text>
                                <Text style={styles.kpiValue}>{metrics.firstTimeDonors}</Text>
                            </View>
                        </View>

                        <BreakdownCard
                            title={t('admin.donationMetrics.breakdown.frequency')}
                            entries={metrics.frequencyBreakdown}
                            currency={metrics.currency}
                            maxAmount={frequencyMaxAmount}
                            resolveLabel={(key) => t(`admin.donations.frequency.${key}`, { defaultValue: key })}
                            styles={styles}
                        />

                        <BreakdownCard
                            title={t('admin.donationMetrics.breakdown.status')}
                            entries={metrics.statusBreakdown}
                            currency={metrics.currency}
                            maxAmount={statusMaxAmount}
                            resolveLabel={(key) => t(`admin.donations.status.${key}`, { defaultValue: key })}
                            styles={styles}
                        />

                        <View style={styles.sectionCard}>
                            <Text style={styles.sectionTitle}>{t('admin.donationMetrics.channels.title')}</Text>
                            <View style={styles.channelRow}>
                                <View style={styles.channelCard}>
                                    <Text style={styles.channelLabel}>{t('admin.donationMetrics.channels.manual')}</Text>
                                    <Text style={styles.channelValue}>{metrics.manualDonationsCount}</Text>
                                    <Text style={styles.channelMeta}>
                                        {formatCurrency(metrics.manualDonationsAmount, metrics.currency)}
                                    </Text>
                                </View>
                                <View style={styles.channelCard}>
                                    <Text style={styles.channelLabel}>{t('admin.donationMetrics.channels.external')}</Text>
                                    <Text style={styles.channelValue}>{metrics.externalDonationsCount}</Text>
                                    <Text style={styles.channelMeta}>
                                        {formatCurrency(metrics.externalDonationsAmount, metrics.currency)}
                                    </Text>
                                </View>
                            </View>
                        </View>

                        <View style={styles.sectionCard}>
                            <Text style={styles.sectionTitle}>{t('admin.donationMetrics.comments.title')}</Text>
                            <View style={styles.rowBetween}>
                                <Text style={styles.rowLabel}>{t('admin.donationMetrics.comments.count')}</Text>
                                <Text style={styles.rowValue}>{metrics.commentsCount}</Text>
                            </View>
                            <View style={styles.rowBetween}>
                                <Text style={styles.rowLabel}>{t('admin.donationMetrics.comments.usageRate')}</Text>
                                <Text style={styles.rowValue}>{formatPercent(metrics.commentsUsageRate)}</Text>
                            </View>
                        </View>

                        <View style={styles.sectionCard}>
                            <Text style={styles.sectionTitle}>{t('admin.donationMetrics.notifications.title')}</Text>
                            <View style={styles.rowBetween}>
                                <Text style={styles.rowLabel}>{t('admin.donationMetrics.notifications.created')}</Text>
                                <Text style={styles.rowValue}>{metrics.donationNotificationsCreated}</Text>
                            </View>
                            <View style={styles.rowBetween}>
                                <Text style={styles.rowLabel}>{t('admin.donationMetrics.notifications.read')}</Text>
                                <Text style={styles.rowValue}>{metrics.donationNotificationsRead}</Text>
                            </View>
                            <View style={styles.rowBetween}>
                                <Text style={styles.rowLabel}>{t('admin.donationMetrics.notifications.unread')}</Text>
                                <Text style={styles.rowValue}>{metrics.donationNotificationsUnread}</Text>
                            </View>
                        </View>

                        <View style={styles.sectionCard}>
                            <Text style={styles.sectionTitle}>{t('admin.donationMetrics.trend.title')}</Text>
                            {metrics.monthlyTrend.length === 0 ? (
                                <Text style={styles.mutedText}>{t('admin.donationMetrics.trend.empty')}</Text>
                            ) : (
                                metrics.monthlyTrend.map((item) => {
                                    const widthPct = trendMaxAmount > 0
                                        ? Math.max(8, (item.amount / trendMaxAmount) * 100)
                                        : 0;
                                    return (
                                        <View key={item.period} style={styles.breakdownRow}>
                                            <View style={styles.breakdownHeader}>
                                                <Text style={styles.breakdownKey}>{formatMonthLabel(item.period)}</Text>
                                                <Text style={styles.breakdownMeta}>
                                                    {item.count} - {formatCurrency(item.amount, metrics.currency)}
                                                </Text>
                                            </View>
                                            <View style={styles.barTrack}>
                                                <View style={[styles.barFill, { width: `${Math.min(100, widthPct)}%` }]} />
                                            </View>
                                        </View>
                                    );
                                })
                            )}
                        </View>

                        <View style={styles.sectionCard}>
                            <View style={styles.rowBetween}>
                                <Text style={styles.sectionTitle}>{t('admin.donationMetrics.topDonors.title')}</Text>
                                <View style={styles.toggleContainer}>
                                    <Pressable
                                        style={[
                                            styles.toggleButton,
                                            topListMode === 'amount' && styles.toggleButtonActive,
                                        ]}
                                        onPress={() => setTopListMode('amount')}
                                    >
                                        <Text
                                            style={[
                                                styles.toggleText,
                                                topListMode === 'amount' && styles.toggleTextActive,
                                            ]}
                                        >
                                            {t('admin.donationMetrics.topDonors.byAmount')}
                                        </Text>
                                    </Pressable>
                                    <Pressable
                                        style={[
                                            styles.toggleButton,
                                            topListMode === 'count' && styles.toggleButtonActive,
                                        ]}
                                        onPress={() => setTopListMode('count')}
                                    >
                                        <Text
                                            style={[
                                                styles.toggleText,
                                                topListMode === 'count' && styles.toggleTextActive,
                                            ]}
                                        >
                                            {t('admin.donationMetrics.topDonors.byCount')}
                                        </Text>
                                    </Pressable>
                                </View>
                            </View>

                            {topDonors.length === 0 ? (
                                <Text style={styles.mutedText}>{t('admin.donationMetrics.topDonors.empty')}</Text>
                            ) : (
                                topDonors.map((donor, index) => (
                                    <View key={`${donor.userId ?? donor.donorEmail ?? index}`} style={styles.donorRow}>
                                        <View style={styles.donorRank}>
                                            <Text style={styles.donorRankText}>{index + 1}</Text>
                                        </View>
                                        <View style={{ flex: 1 }}>
                                            <Text style={styles.donorName}>
                                                {donor.donorName || donor.donorEmail || t('admin.donations.unknownDonor')}
                                            </Text>
                                            {donor.donorEmail ? (
                                                <Text style={styles.donorEmail}>{donor.donorEmail}</Text>
                                            ) : null}
                                        </View>
                                        <View style={{ alignItems: 'flex-end' }}>
                                            <Text style={styles.donorValue}>
                                                {topListMode === 'amount'
                                                    ? formatCurrency(donor.totalAmount, metrics.currency)
                                                    : donor.donationCount}
                                            </Text>
                                            <Text style={styles.donorMeta}>
                                                {t('admin.donationMetrics.topDonors.donationsCount', { count: donor.donationCount })}
                                            </Text>
                                        </View>
                                    </View>
                                ))
                            )}
                        </View>
                    </>
                ) : null}
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
                showAttendance={hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])}
                onNavigateDashboard={() => {
                    setMenuVisible(false);
                    router.push('/admin');
                }}
                showDashboard={hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])}
                t={t}
            />
        </View>
    );
}

const getStyles = (isDark: boolean) => {
    const BG = isDark ? '#0F1419' : '#F5F7FB';
    const CARD = isDark ? '#1B222C' : '#FFFFFF';
    const BORDER = isDark ? '#2F3A4A' : '#E0E7F3';
    const TEXT = isDark ? '#ECEDEE' : '#1E2A3B';
    const MUTED = isDark ? '#A0A7B1' : '#5C6A80';
    const ACCENT = isDark ? '#9FC3FF' : '#0056A8';
    const ACCENT_SOFT = isDark ? '#1D2A3A' : '#EAF1FF';
    const ERROR = isDark ? '#FFB4AB' : '#D93025';

    return StyleSheet.create({
        container: {
            flex: 1,
            backgroundColor: BG,
        },
        content: {
            padding: 18,
            paddingBottom: 40,
            gap: 12,
        },
        headerRow: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 10,
        },
        title: {
            color: TEXT,
            fontSize: 24,
            fontWeight: '700',
        },
        subtitle: {
            color: MUTED,
            marginTop: 4,
            fontSize: 14,
        },
        refreshButton: {
            width: 38,
            height: 38,
            borderRadius: 10,
            backgroundColor: CARD,
            borderWidth: 1,
            borderColor: BORDER,
            alignItems: 'center',
            justifyContent: 'center',
        },
        refreshButtonPressed: {
            transform: [{ scale: 0.97 }],
        },
        refreshIcon: {
            color: ACCENT,
        },
        centered: {
            paddingVertical: 40,
            alignItems: 'center',
        },
        loadingText: {
            marginTop: 8,
            color: MUTED,
        },
        errorCard: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 8,
            backgroundColor: CARD,
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 12,
            padding: 12,
        },
        errorText: {
            color: ERROR,
            flex: 1,
        },
        kpiGrid: {
            flexDirection: 'row',
            flexWrap: 'wrap',
            gap: 10,
        },
        kpiCard: {
            width: '48.5%',
            backgroundColor: CARD,
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 12,
            padding: 12,
            minHeight: 92,
            justifyContent: 'center',
        },
        kpiLabel: {
            color: MUTED,
            fontSize: 12,
            fontWeight: '600',
        },
        kpiValue: {
            color: TEXT,
            fontSize: 20,
            fontWeight: '800',
            marginTop: 4,
        },
        sectionCard: {
            backgroundColor: CARD,
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 12,
            padding: 12,
            gap: 10,
        },
        sectionTitle: {
            color: TEXT,
            fontSize: 16,
            fontWeight: '700',
        },
        mutedText: {
            color: MUTED,
        },
        breakdownRow: {
            gap: 6,
        },
        breakdownHeader: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 8,
        },
        breakdownKey: {
            color: TEXT,
            fontWeight: '700',
            flex: 1,
        },
        breakdownMeta: {
            color: MUTED,
            fontSize: 12,
        },
        barTrack: {
            width: '100%',
            height: 8,
            borderRadius: 999,
            backgroundColor: ACCENT_SOFT,
            overflow: 'hidden',
        },
        barFill: {
            height: '100%',
            borderRadius: 999,
            backgroundColor: ACCENT,
        },
        channelRow: {
            flexDirection: 'row',
            gap: 10,
        },
        channelCard: {
            flex: 1,
            backgroundColor: ACCENT_SOFT,
            borderRadius: 10,
            padding: 10,
            gap: 4,
        },
        channelLabel: {
            color: MUTED,
            fontSize: 12,
        },
        channelValue: {
            color: TEXT,
            fontSize: 20,
            fontWeight: '800',
        },
        channelMeta: {
            color: ACCENT,
            fontWeight: '700',
            fontSize: 12,
        },
        rowBetween: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 10,
        },
        rowLabel: {
            color: MUTED,
            fontSize: 13,
        },
        rowValue: {
            color: TEXT,
            fontSize: 15,
            fontWeight: '700',
        },
        toggleContainer: {
            flexDirection: 'row',
            backgroundColor: ACCENT_SOFT,
            borderRadius: 10,
            padding: 2,
            gap: 4,
        },
        toggleButton: {
            paddingVertical: 6,
            paddingHorizontal: 8,
            borderRadius: 8,
        },
        toggleButtonActive: {
            backgroundColor: ACCENT,
        },
        toggleText: {
            fontSize: 12,
            color: ACCENT,
            fontWeight: '700',
        },
        toggleTextActive: {
            color: '#FFFFFF',
        },
        donorRow: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 10,
            paddingVertical: 6,
            borderBottomWidth: StyleSheet.hairlineWidth,
            borderBottomColor: BORDER,
        },
        donorRank: {
            width: 26,
            height: 26,
            borderRadius: 13,
            backgroundColor: ACCENT_SOFT,
            alignItems: 'center',
            justifyContent: 'center',
        },
        donorRankText: {
            color: ACCENT,
            fontWeight: '700',
            fontSize: 12,
        },
        donorName: {
            color: TEXT,
            fontWeight: '700',
        },
        donorEmail: {
            color: MUTED,
            fontSize: 12,
            marginTop: 2,
        },
        donorValue: {
            color: TEXT,
            fontWeight: '800',
        },
        donorMeta: {
            color: MUTED,
            fontSize: 11,
            marginTop: 2,
        },
    });
};
