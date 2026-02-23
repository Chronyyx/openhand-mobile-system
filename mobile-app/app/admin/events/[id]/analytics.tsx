import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, ActivityIndicator, TouchableOpacity, Dimensions, useColorScheme } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { LineChart } from 'react-native-chart-kit';
import { useTranslation } from 'react-i18next';
import { getEventAnalytics, EventAnalyticsResponse } from '../../../../services/event-management.service';
import { MenuLayout } from '../../../../components/menu-layout';

const screenWidth = Dimensions.get("window").width - 32;

export default function EventAnalyticsScreen() {
    const { id } = useLocalSearchParams();
    const eventId = Number(id);
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    const { t } = useTranslation();

    const [analytics, setAnalytics] = useState<EventAnalyticsResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // YouTube Studio style toggles
    const [showConfirmed, setShowConfirmed] = useState(true);
    const [showUsualTrend, setShowUsualTrend] = useState(true);
    const [showWaitlist, setShowWaitlist] = useState(false);

    useEffect(() => {
        fetchAnalytics();
    }, [eventId]);

    const fetchAnalytics = async () => {
        try {
            setLoading(true);
            const data = await getEventAnalytics(eventId);
            setAnalytics(data);
            setError(null);
        } catch (err) {
            console.error("Failed to fetch analytics:", err);
            setError(t('analytics.single.errorLoad'));
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <MenuLayout>
                <View style={[styles.center, isDark ? styles.darkBg : styles.lightBg]}>
                    <ActivityIndicator size="large" color="#007bff" />
                    <Text style={isDark ? styles.darkText : styles.lightText}>{t('analytics.single.loading')}</Text>
                </View>
            </MenuLayout>
        );
    }

    if (error || !analytics) {
        return (
            <MenuLayout>
                <View style={[styles.center, isDark ? styles.darkBg : styles.lightBg]}>
                    <Text style={{ color: 'red' }}>{error}</Text>
                </View>
            </MenuLayout>
        );
    }

    const formatDelta = (delta: number) => {
        if (delta > 0) return t('analytics.single.moreThanUsual', { value: delta.toFixed(1) });
        if (delta < 0) return t('analytics.single.lessThanUsual', { value: Math.abs(delta).toFixed(1) });
        return t('analytics.single.sameAsUsual');
    };

    const isDoingWell = analytics.confirmedDeltaVsUsual > 0;

    // Build Chart Data
    const generateChartData = () => {
        const labels: string[] = [];
        const confirmedData: number[] = [];
        const waitlistData: number[] = [];
        const usualData: number[] = [];

        // Sort event timeline by daysBeforeEvent descending (furthest first)
        const sortedTimeline = [...analytics.eventTimeline].sort((a, b) => b.daysBeforeEvent - a.daysBeforeEvent);
        if (sortedTimeline.length === 0) {
            return { labels: ['No data'], datasets: [{ data: [0], color: () => 'transparent' }] };
        }

        // Sample ~8 evenly-spaced points from the timeline
        const maxPoints = 8;
        const step = Math.max(1, Math.floor(sortedTimeline.length / maxPoints));
        const sampled = sortedTimeline.filter((_, idx) => idx % step === 0);

        for (const entry of sampled) {
            // Use the date field if available, otherwise compute a short label
            const dateStr = entry.date
                ? new Date(entry.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
                : `T-${entry.daysBeforeEvent}`;
            labels.push(dateStr);

            confirmedData.push(entry.confirmed);
            waitlistData.push(entry.waitlisted);

            // Find the matching usual trend entry for this T-minus day
            const usr = analytics.usualTrendTimeline.find(m => m.daysBeforeEvent === entry.daysBeforeEvent);
            usualData.push(usr ? usr.confirmed : 0);
        }

        const datasets = [];
        if (showConfirmed) datasets.push({ data: confirmedData, color: () => '#007bff', strokeWidth: 3 });
        if (showWaitlist) datasets.push({ data: waitlistData, color: () => '#ffc107', strokeWidth: 2 });
        if (showUsualTrend) datasets.push({
            data: usualData,
            color: () => isDark ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.3)',
            strokeWidth: 2,
            withDots: false
        });

        // Ensure at least 1 dataset avoids a crash
        if (datasets.length === 0) datasets.push({ data: [0], color: () => 'transparent' });

        return { labels, datasets };
    };

    const chartConfig = {
        backgroundGradientFrom: isDark ? '#1e1e1e' : '#ffffff',
        backgroundGradientTo: isDark ? '#1e1e1e' : '#ffffff',
        color: (opacity = 1) => isDark ? `rgba(255, 255, 255, ${opacity})` : `rgba(0, 0, 0, ${opacity})`,
        strokeWidth: 2,
        useShadowColorFromDataset: false,
        propsForDots: { r: "3", strokeWidth: "1" }
    };

    return (
        <MenuLayout>
            <ScrollView style={[styles.container, isDark ? styles.darkBg : styles.lightBg]}>

                {/* YouTube Studio Dashboard Header */}
                <View style={styles.headerBlock}>
                    <Text style={[styles.title, isDark && styles.darkText]}>
                        {t('analytics.single.title', { name: analytics.title })}
                    </Text>
                    <View style={[styles.performanceBanner, isDark && styles.performanceBannerDark]}>
                        <Text style={[styles.performanceBannerText, isDark && styles.performanceBannerTextDark]}>
                            {isDoingWell ? `🎉 ${t('analytics.single.performingBetter')}` : `📊 ${t('analytics.single.trackingAverage')}`}
                        </Text>
                    </View>
                </View>

                {/* Stage 6: Overview Metric Cards (Toggles) */}
                <View style={styles.metricsRow}>
                    <TouchableOpacity
                        style={[styles.metricCard, isDark && styles.metricCardDark, showConfirmed && (isDark ? styles.metricCardActiveDark : styles.metricCardActive)]}
                        onPress={() => setShowConfirmed(!showConfirmed)}
                    >
                        <Text style={[styles.metricTitle, isDark && styles.metricTitleDark]}>{t('analytics.single.confirmed')}</Text>
                        <Text style={[styles.metricValue, isDark && styles.darkText]}>
                            {analytics.eventTimeline.length > 0 ? analytics.eventTimeline[analytics.eventTimeline.length - 1].confirmed : 0}
                        </Text>
                        <Text style={[styles.metricDelta, { color: isDoingWell ? (isDark ? '#66bb6a' : 'green') : (isDark ? '#aaa' : 'gray') }]}>
                            {formatDelta(analytics.confirmedDeltaVsUsual)}
                        </Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                        style={[styles.metricCard, isDark && styles.metricCardDark, showWaitlist && (isDark ? styles.metricCardActiveDark : styles.metricCardActive)]}
                        onPress={() => setShowWaitlist(!showWaitlist)}
                    >
                        <Text style={[styles.metricTitle, isDark && styles.metricTitleDark]}>{t('analytics.single.waitlist')}</Text>
                        <Text style={[styles.metricValue, isDark && styles.darkText]}>
                            {analytics.eventTimeline.length > 0 ? analytics.eventTimeline[analytics.eventTimeline.length - 1].waitlisted : 0}
                        </Text>
                        <Text style={[styles.metricDelta, { color: analytics.waitlistDeltaVsUsual > 0 ? (isDark ? '#ffb74d' : 'orange') : (isDark ? '#aaa' : 'gray') }]}>
                            {formatDelta(analytics.waitlistDeltaVsUsual)}
                        </Text>
                    </TouchableOpacity>
                </View>

                {/* Stages 1-3: The Multi-Line Chart */}
                <View style={[styles.chartContainer, isDark && styles.chartContainerDark]}>
                    <View style={styles.chartHeader}>
                        <Text style={[styles.chartTitle, isDark && styles.darkText]}>{t('analytics.single.timeline')}</Text>
                        <TouchableOpacity onPress={() => setShowUsualTrend(!showUsualTrend)} style={styles.usualToggle}>
                            <Text style={{ color: showUsualTrend ? '#007bff' : (isDark ? '#888' : 'gray') }}>
                                {showUsualTrend ? `☑ ${t('analytics.single.usualTrend')}` : `☐ ${t('analytics.single.usualTrend')}`}
                            </Text>
                        </TouchableOpacity>
                    </View>

                    <LineChart
                        data={generateChartData()}
                        width={screenWidth}
                        height={220}
                        chartConfig={chartConfig}
                        bezier
                        style={styles.chart}
                        fromZero
                    />
                </View>

                {/* Fill Rate Forecast */}
                <View style={[styles.gaugeCard, isDark && styles.chartContainerDark, { marginBottom: 20 }]}>
                    <Text style={[styles.metricTitle, isDark && styles.metricTitleDark]}>{t('analytics.single.fillRate')}</Text>
                    <Text style={[styles.metricValue, { fontSize: 28 }, isDark && styles.darkText]}>
                        {analytics.estimatedDaysToFill === 0
                            ? `✅ ${t('analytics.single.full')}`
                            : analytics.estimatedDaysToFill != null
                                ? `${analytics.estimatedDaysToFill}d`
                                : '—'}
                    </Text>
                    <Text style={[styles.chartSubTitle, isDark && styles.subtitleDark]}>
                        {analytics.estimatedDaysToFill === 0
                            ? t('analytics.single.atCapacity')
                            : analytics.estimatedDaysToFill != null
                                ? t('analytics.single.daysToFill')
                                : t('analytics.single.notEnoughData')}
                    </Text>
                    <Text style={[styles.velocityText, isDark && styles.velocityTextDark]}>
                        {t('analytics.single.regsPerDay', { value: analytics.currentVelocity.toFixed(1) })}
                    </Text>
                </View>

                <View style={{ height: 50 }} />

            </ScrollView>
        </MenuLayout>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, padding: 16 },
    lightBg: { backgroundColor: '#f8f9fa' },
    darkBg: { backgroundColor: '#121212' },
    lightText: { color: '#000' },
    darkText: { color: '#E8EAED' },
    center: { flex: 1, justifyContent: 'center', alignItems: 'center' },

    headerBlock: { marginBottom: 20 },
    title: { fontSize: 22, fontWeight: 'bold', marginBottom: 8, color: '#1a1a1a' },
    performanceBanner: { backgroundColor: '#e8f5e9', padding: 10, borderRadius: 8, borderLeftWidth: 4, borderLeftColor: '#4caf50' },
    performanceBannerDark: { backgroundColor: '#1b3a1b', borderLeftColor: '#66bb6a' },
    performanceBannerText: { color: '#2e7d32', fontWeight: 'bold' },
    performanceBannerTextDark: { color: '#a5d6a7' },

    metricsRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
    metricCard: { flex: 1, backgroundColor: '#fff', padding: 16, borderRadius: 12, marginRight: 8, borderWidth: 1, borderColor: '#eee', elevation: 2 },
    metricCardDark: { backgroundColor: '#1e1e1e', borderColor: '#333' },
    metricCardActive: { borderColor: '#007bff', borderWidth: 2, backgroundColor: '#f0f7ff' },
    metricCardActiveDark: { borderColor: '#4a9eff', borderWidth: 2, backgroundColor: '#1a2a3e' },
    metricTitle: { fontSize: 14, color: '#666', fontWeight: 'bold', marginBottom: 8 },
    metricTitleDark: { color: '#A0A7B1' },
    metricValue: { fontSize: 24, fontWeight: 'bold', marginBottom: 4, color: '#1a1a1a' },
    metricDelta: { fontSize: 12, fontWeight: 'bold' },

    chartContainer: { backgroundColor: '#fff', padding: 16, borderRadius: 12, elevation: 2, marginBottom: 20 },
    chartContainerDark: { backgroundColor: '#1e1e1e' },
    chartHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
    chartTitle: { fontSize: 16, fontWeight: 'bold', color: '#1a1a1a' },
    chartSubTitle: { fontSize: 12, color: '#666', marginTop: 4, textAlign: 'center' },
    subtitleDark: { color: '#A0A7B1' },
    usualToggle: { padding: 4 },
    chart: { marginVertical: 8, borderRadius: 16, alignSelf: 'center' },

    gaugeCard: { flex: 1, backgroundColor: '#fff', padding: 16, borderRadius: 12, alignItems: 'center', justifyContent: 'center', elevation: 2 },
    velocityText: { fontSize: 10, color: '#666', marginTop: 12, textAlign: 'center' },
    velocityTextDark: { color: '#A0A7B1' },
});
