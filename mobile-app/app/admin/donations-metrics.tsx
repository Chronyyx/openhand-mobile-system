import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Pressable,
    RefreshControl,
    ScrollView,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import * as FileSystem from 'expo-file-system/legacy';
import { Ionicons } from '@expo/vector-icons';
import { Redirect, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { AppHeader } from '../../components/app-header';
import { DateTimePickerModal } from '../../components/date-time-picker-modal';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import {
    exportDonationReportCsv,
    getDonationReport,
    getDonationMetrics,
    type DonationMetricBreakdown,
    type DonationMetrics,
    type DonationReportRow,
    type DonationTopDonor,
} from '../../services/donation-metrics.service';

type TopListMode = 'amount' | 'count';

const formatCurrency = (amount: number, currency: string) => `${currency} ${Number(amount || 0).toFixed(2)}`;

const formatPercent = (value: number) => `${Number.isFinite(value) ? value.toFixed(2) : '0.00'}%`;

function pad2(value: number) {
    return value.toString().padStart(2, '0');
}

function formatDateForApi(date: Date) {
    return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
}

function formatDateForDisplay(date: Date) {
    return formatDateForApi(date);
}

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
    const [reportStartDate, setReportStartDate] = useState(() => {
        const now = new Date();
        const start = new Date(now);
        start.setDate(now.getDate() - 30);
        start.setHours(0, 0, 0, 0);
        return start;
    });
    const [reportEndDate, setReportEndDate] = useState(() => {
        const now = new Date();
        now.setHours(0, 0, 0, 0);
        return now;
    });
    const [pickerVisible, setPickerVisible] = useState(false);
    const [activePicker, setActivePicker] = useState<'start' | 'end' | null>(null);
    const [reportRows, setReportRows] = useState<DonationReportRow[]>([]);
    const [reportLoading, setReportLoading] = useState(false);
    const [reportExporting, setReportExporting] = useState(false);
    const [hasGeneratedReport, setHasGeneratedReport] = useState(false);
    const [reportError, setReportError] = useState<string | null>(null);

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

    const pickerInitialValue = useMemo(() => {
        if (activePicker === 'start') return reportStartDate;
        if (activePicker === 'end') return reportEndDate;
        return new Date();
    }, [activePicker, reportEndDate, reportStartDate]);

    const reportTotalAmount = useMemo(
        () => reportRows.reduce((sum, row) => sum + Number(row.amount || 0), 0),
        [reportRows],
    );

    const isReportBusy = reportLoading || reportExporting;
    const isExportDisabled = !hasGeneratedReport || isReportBusy;

    const openDatePicker = (target: 'start' | 'end') => {
        setActivePicker(target);
        setPickerVisible(true);
    };

    const onConfirmDatePicker = (value: Date) => {
        const normalized = new Date(value);
        normalized.setHours(0, 0, 0, 0);
        if (activePicker === 'start') {
            setReportStartDate(normalized);
        }
        if (activePicker === 'end') {
            setReportEndDate(normalized);
        }
        setPickerVisible(false);
        setActivePicker(null);
    };

    const onGenerateReport = async () => {
        if (reportStartDate.getTime() > reportEndDate.getTime()) {
            setReportError(t('admin.donationMetrics.reports.invalidDateRange'));
            setHasGeneratedReport(false);
            setReportRows([]);
            return;
        }

        try {
            setReportLoading(true);
            setReportError(null);
            const rows = await getDonationReport(
                formatDateForApi(reportStartDate),
                formatDateForApi(reportEndDate),
            );
            setReportRows(rows);
            setHasGeneratedReport(true);
        } catch (reportRequestError) {
            console.error('Failed to generate donation report', reportRequestError);
            setReportError(t('admin.donationMetrics.reports.generateError'));
            setReportRows([]);
            setHasGeneratedReport(false);
        } finally {
            setReportLoading(false);
        }
    };

    const onExportReportCsv = async () => {
        if (isExportDisabled) {
            return;
        }
        if (reportStartDate.getTime() > reportEndDate.getTime()) {
            setReportError(t('admin.donationMetrics.reports.invalidDateRange'));
            return;
        }

        const targetDirectory = FileSystem.cacheDirectory ?? FileSystem.documentDirectory;
        if (!targetDirectory) {
            Alert.alert(t('common.error'), t('admin.donationMetrics.reports.exportError'));
            return;
        }

        const fileName = `donation-report-${formatDateForApi(reportStartDate)}-to-${formatDateForApi(reportEndDate)}.csv`;
        const fileUri = `${targetDirectory}${fileName}`;

        try {
            setReportExporting(true);
            const csvContent = await exportDonationReportCsv(
                formatDateForApi(reportStartDate),
                formatDateForApi(reportEndDate),
            );
            await FileSystem.writeAsStringAsync(fileUri, csvContent, {
                encoding: FileSystem.EncodingType.UTF8,
            });

            const Sharing = await import('expo-sharing');
            const sharingAvailable = await Sharing.isAvailableAsync();
            if (!sharingAvailable) {
                Alert.alert(t('common.error'), t('admin.donationMetrics.reports.shareUnavailable'));
                return;
            }

            await Sharing.shareAsync(fileUri, {
                mimeType: 'text/csv',
                dialogTitle: fileName,
                UTI: 'public.comma-separated-values-text',
            });

            Alert.alert(t('common.success'), t('admin.donationMetrics.reports.exportSuccess'));
        } catch (reportExportError) {
            console.error('Failed to export donation report CSV', reportExportError);
            Alert.alert(t('common.error'), t('admin.donationMetrics.reports.exportError'));
        } finally {
            setReportExporting(false);
        }
    };

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
                        <View style={styles.sectionCard}>
                            <Text style={styles.sectionTitle}>{t('admin.donationMetrics.reports.title')}</Text>
                            <Text style={styles.sectionSubtitle}>{t('admin.donationMetrics.reports.subtitle')}</Text>

                            <View style={styles.reportControls}>
                                <Pressable
                                    onPress={() => openDatePicker('start')}
                                    style={styles.dateField}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('admin.donationMetrics.reports.startDate')}
                                >
                                    <Ionicons name="calendar-outline" size={18} color={styles.rowValue.color} />
                                    <View style={styles.dateTextWrap}>
                                        <Text style={styles.dateLabel}>{t('admin.donationMetrics.reports.startDate')}</Text>
                                        <Text style={styles.dateValue}>{formatDateForDisplay(reportStartDate)}</Text>
                                    </View>
                                </Pressable>

                                <Pressable
                                    onPress={() => openDatePicker('end')}
                                    style={styles.dateField}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('admin.donationMetrics.reports.endDate')}
                                >
                                    <Ionicons name="calendar-outline" size={18} color={styles.rowValue.color} />
                                    <View style={styles.dateTextWrap}>
                                        <Text style={styles.dateLabel}>{t('admin.donationMetrics.reports.endDate')}</Text>
                                        <Text style={styles.dateValue}>{formatDateForDisplay(reportEndDate)}</Text>
                                    </View>
                                </Pressable>
                            </View>

                            <View style={styles.actionRow}>
                                <Pressable
                                    style={({ pressed }) => [
                                        styles.actionButton,
                                        isReportBusy && styles.actionButtonDisabled,
                                        pressed && !isReportBusy && styles.actionButtonPressed,
                                    ]}
                                    onPress={onGenerateReport}
                                    disabled={isReportBusy}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('admin.donationMetrics.reports.generate')}
                                >
                                    <Text style={styles.actionButtonText}>
                                        {t('admin.donationMetrics.reports.generate')}
                                    </Text>
                                </Pressable>
                                <Pressable
                                    style={({ pressed }) => [
                                        styles.actionButton,
                                        isExportDisabled && styles.actionButtonDisabled,
                                        pressed && !isExportDisabled && styles.actionButtonPressed,
                                    ]}
                                    onPress={onExportReportCsv}
                                    disabled={isExportDisabled}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('admin.donationMetrics.reports.exportCsv')}
                                >
                                    <Text
                                        style={[
                                            styles.actionButtonText,
                                            isExportDisabled && styles.actionButtonTextDisabled,
                                        ]}
                                    >
                                        {t('admin.donationMetrics.reports.exportCsv')}
                                    </Text>
                                </Pressable>
                            </View>

                            {reportError ? (
                                <Text style={styles.errorText}>{reportError}</Text>
                            ) : null}

                            {hasGeneratedReport && !reportError ? (
                                <>
                                    <View style={styles.rowBetween}>
                                        <Text style={styles.rowLabel}>{t('admin.donationMetrics.reports.totalRows')}</Text>
                                        <Text style={styles.rowValue}>{reportRows.length}</Text>
                                    </View>
                                    <View style={styles.rowBetween}>
                                        <Text style={styles.rowLabel}>{t('admin.donationMetrics.reports.totalAmount')}</Text>
                                        <Text style={styles.rowValue}>
                                            {formatCurrency(reportTotalAmount, metrics.currency)}
                                        </Text>
                                    </View>
                                    <Text style={styles.reportStatusText}>
                                        {reportRows.length > 0
                                            ? t('admin.donationMetrics.reports.generatedReady')
                                            : t('admin.donationMetrics.reports.empty')}
                                    </Text>
                                </>
                            ) : null}
                        </View>

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

            <DateTimePickerModal
                visible={pickerVisible}
                title={
                    activePicker === 'start'
                        ? t('admin.donationMetrics.reports.startDate')
                        : t('admin.donationMetrics.reports.endDate')
                }
                initialValue={pickerInitialValue}
                cancelLabel={t('common.cancel')}
                confirmLabel={t('common.confirm')}
                timeLabel={t('events.fields.time')}
                onCancel={() => {
                    setPickerVisible(false);
                    setActivePicker(null);
                }}
                onConfirm={onConfirmDatePicker}
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
        sectionSubtitle: {
            color: MUTED,
            fontSize: 13,
            marginTop: -2,
        },
        reportControls: {
            gap: 8,
        },
        dateField: {
            borderWidth: 1,
            borderColor: BORDER,
            borderRadius: 12,
            backgroundColor: ACCENT_SOFT,
            paddingHorizontal: 12,
            paddingVertical: 10,
            flexDirection: 'row',
            alignItems: 'center',
            gap: 10,
        },
        dateTextWrap: {
            flex: 1,
        },
        dateLabel: {
            color: MUTED,
            fontSize: 12,
        },
        dateValue: {
            color: TEXT,
            fontSize: 14,
            fontWeight: '700',
            marginTop: 2,
        },
        actionRow: {
            flexDirection: 'row',
            gap: 10,
        },
        actionButton: {
            flex: 1,
            backgroundColor: ACCENT,
            borderRadius: 10,
            alignItems: 'center',
            justifyContent: 'center',
            paddingVertical: 11,
        },
        actionButtonDisabled: {
            backgroundColor: isDark ? '#3D4755' : '#E0E6F0',
        },
        actionButtonPressed: {
            transform: [{ scale: 0.98 }],
        },
        actionButtonText: {
            color: '#FFFFFF',
            fontSize: 13,
            fontWeight: '700',
        },
        actionButtonTextDisabled: {
            color: isDark ? '#C5CFDE' : '#7A8598',
        },
        reportStatusText: {
            color: MUTED,
            fontSize: 12,
            fontStyle: 'italic',
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
