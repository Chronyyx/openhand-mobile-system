import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, ActivityIndicator, Pressable, ScrollView, Dimensions, Modal } from 'react-native';
import { useRouter, Redirect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useTranslation } from 'react-i18next';
import { PieChart, LineChart } from 'react-native-chart-kit';
import { MenuLayout } from '../../components/menu-layout';
import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import { getGlobalAnalytics, getCompareAnalytics, seedAnalyticsData, type GlobalAnalyticsResponseModel, type EventPerformanceSummary, type EventAnalyticsResponse } from '../../services/event-management.service';

const screenWidth = Dimensions.get("window").width - 32;

export default function AdminEventAnalyticsPickerScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const { hasRole } = useAuth();
    const isAdminOrEmployee = hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']);
    const colorScheme = useColorScheme() ?? 'light';
    const isDark = colorScheme === 'dark';
    const styles = getStyles(isDark);

    const [globalData, setGlobalData] = useState<GlobalAnalyticsResponseModel | null>(null);
    const [loading, setLoading] = useState(true);
    const [seeding, setSeeding] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isModalVisible, setModalVisible] = useState(false);

    const [selectedCompareIds, setSelectedCompareIds] = useState<number[]>([]);
    const [compareData, setCompareData] = useState<EventAnalyticsResponse[]>([]);
    const [loadingCompare, setLoadingCompare] = useState(false);

    useEffect(() => {
        if (isAdminOrEmployee) {
            loadAnalytics();
        }
    }, [isAdminOrEmployee]);

    useEffect(() => {
        const fetchCompare = async () => {
            if (selectedCompareIds.length === 0) {
                setCompareData([]);
                return;
            }
            try {
                setLoadingCompare(true);
                const data = await getCompareAnalytics(selectedCompareIds);
                setCompareData(data);
            } catch (err) {
                console.error("Failed to load compare data", err);
            } finally {
                setLoadingCompare(false);
            }
        };
        fetchCompare();
    }, [selectedCompareIds]);

    const loadAnalytics = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await getGlobalAnalytics();
            setGlobalData(data);
        } catch (err) {
            console.error('Failed to load global analytics', err);
            setError(t('admin.events.errors.loadFailed', 'Could not load global analytics.'));
        } finally {
            setLoading(false);
        }
    };

    const handleSeedData = async () => {
        try {
            setSeeding(true);
            await seedAnalyticsData();
            await loadAnalytics(); // reload after seed
        } catch (err) {
            console.error(err);
        } finally {
            setSeeding(false);
        }
    };

    const toggleCompareSelection = (eventId: number) => {
        setSelectedCompareIds(prev => {
            if (prev.includes(eventId)) {
                return prev.filter(id => id !== eventId);
            }
            if (prev.length >= 5) {
                return prev; // limit to 5
            }
            return [...prev, eventId];
        });
    };

    if (!isAdminOrEmployee) {
        return <Redirect href="/admin" />;
    }

    const renderItem = ({ item }: { item: EventPerformanceSummary }) => {
        return (
            <Pressable
                style={({ pressed }) => [styles.card, pressed && styles.cardPressed]}
                onPress={() => router.push(`/admin/events/${item.eventId}/analytics` as any)}
                accessibilityRole="button"
            >
                <View style={{ flex: 1 }}>
                    <ThemedText style={styles.cardTitle}>{item.title}</ThemedText>
                    <ThemedText style={styles.cardSubtitle}>
                        {item.currentRegistrations}/{item.maxCapacity || '∞'} {t('analytics.global.registrations')}
                    </ThemedText>
                </View>
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                    <View style={[styles.badge, { borderColor: item.trendingUp ? '#4CAF50' : '#F44336' }]}>
                        <Ionicons
                            name={item.trendingUp ? 'trending-up' : 'trending-down'}
                            size={14}
                            color={item.trendingUp ? '#4CAF50' : '#F44336'}
                        />
                        <Text style={[styles.badgeText, { color: item.trendingUp ? '#4CAF50' : '#F44336' }]}>
                            {Math.abs(item.attendanceDeltaVsNorm).toFixed(1)}% {t('analytics.global.vsNorm')}
                        </Text>
                    </View>
                    <Ionicons name="chevron-forward" size={20} color={isDark ? '#A0A7B1' : '#5C6A80'} />
                </View>
            </Pressable>
        )
    };

    const getCompareChartData = () => {
        if (compareData.length === 0) return null;

        const colors = ["#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336"];

        // 1. Find all unique midnight timestamps across all compared events
        const allTimestamps = new Set<number>();
        compareData.forEach(cd => {
            cd.eventTimeline.forEach(m => {
                if (m.date) {
                    allTimestamps.add(new Date(m.date).setHours(0, 0, 0, 0));
                }
            });
        });

        const sortedTimestamps = Array.from(allTimestamps).sort((a, b) => a - b);
        if (sortedTimestamps.length === 0) return null;

        // 2. Create continuous days between min and max date
        const continuousTimestamps: number[] = [];
        const minTime = sortedTimestamps[0];
        const maxTime = sortedTimestamps[sortedTimestamps.length - 1];
        for (let t = minTime; t <= maxTime; t += 86400000) {
            continuousTimestamps.push(t);
        }

        // 3. Map datasets to exact continuous timestamps
        const datasets = compareData.map((cd, index) => {
            const dataMap = new Map<number, number>();
            cd.eventTimeline.forEach(m => {
                if (m.date) {
                    dataMap.set(new Date(m.date).setHours(0, 0, 0, 0), m.confirmed + m.waitlisted);
                }
            });

            let lastVal = 0;
            const data = continuousTimestamps.map(t => {
                if (dataMap.has(t)) {
                    lastVal = dataMap.get(t)!;
                }
                return lastVal;
            });

            return {
                data,
                color: (opacity = 1) => colors[index % colors.length],
                strokeWidth: 2
            };
        });

        // 4. Format labels (thin out X-axis slightly so it doesn't overlap)
        const labels = continuousTimestamps.map(t => new Date(t).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));

        return {
            labels: labels.filter((_, i) => i % Math.max(1, Math.floor(labels.length / 5)) === 0),
            datasets: datasets,
            legend: compareData.map(cd => cd.title.length > 10 ? cd.title.substring(0, 10) + '...' : cd.title)
        };
    };

    const pieData = globalData ? [
        {
            name: "Confirmed",
            population: globalData.totalConfirmed,
            color: "#4CAF50",
            legendFontColor: isDark ? "#ECEDEE" : "#0F2848",
            legendFontSize: 13
        },
        {
            name: "Waitlisted",
            population: globalData.totalWaitlisted,
            color: "#FF9800",
            legendFontColor: isDark ? "#ECEDEE" : "#0F2848",
            legendFontSize: 13
        }
    ] : [];

    return (
        <MenuLayout>
            <ThemedView style={styles.container}>
                <View style={styles.headerRow}>
                    <ThemedText style={styles.title}>{t('analytics.global.title')}</ThemedText>
                    <Pressable style={styles.seedBtn} onPress={handleSeedData} disabled={seeding}>
                        {seeding ? <ActivityIndicator size="small" color="#fff" /> : <Text style={styles.seedBtnText}>{t('analytics.global.seedData')}</Text>}
                    </Pressable>
                </View>

                {loading || !globalData ? (
                    <View style={styles.centered}>
                        <ActivityIndicator size="large" color={isDark ? '#9FC3FF' : '#0056A8'} />
                        <ThemedText style={{ marginTop: 10, color: isDark ? '#A0A7B1' : '#5C6A80' }}>{t('analytics.global.loading')}</ThemedText>
                    </View>
                ) : error ? (
                    <View style={styles.centered}>
                        <ThemedText style={{ color: '#D32F2F' }}>{error}</ThemedText>
                    </View>
                ) : (
                    <FlatList
                        data={globalData.activeEventPerformances}
                        keyExtractor={item => item.eventId.toString()}
                        contentContainerStyle={styles.listContent}
                        renderItem={renderItem}
                        ListHeaderComponent={
                            <View>
                                {/* Global Stats Cards */}
                                <View style={styles.statsRow}>
                                    <View style={styles.statBox}>
                                        <Text style={styles.statBoxTitle}>{t('analytics.global.globalVelocity')}</Text>
                                        <Text style={styles.statBoxValue}>{globalData.currentGlobalVelocity.toFixed(1)} <Text style={{ fontSize: 12 }}>{t('analytics.global.perDay')}</Text></Text>
                                        <Text style={[styles.statBoxDelta, { color: globalData.performingBetterThanUsual ? '#4CAF50' : '#F44336' }]}>
                                            {globalData.performingBetterThanUsual ? '↑' : '↓'} {Math.abs(globalData.velocityDeltaPercentage).toFixed(1)}% {t('analytics.global.vsUsual')}
                                        </Text>
                                    </View>
                                </View>

                                {/* Pie Chart */}
                                <View style={styles.chartContainer}>
                                    <Text style={styles.chartTitle}>{t('analytics.global.pieTitle')}</Text>
                                    <PieChart
                                        data={pieData}
                                        width={screenWidth}
                                        height={180}
                                        chartConfig={{
                                            color: (opacity = 1) => `rgba(255, 255, 255, ${opacity})`,
                                            labelColor: (opacity = 1) => isDark ? '#ECEDEE' : '#0F2848'
                                        }}
                                        accessor={"population"}
                                        backgroundColor={"transparent"}
                                        paddingLeft={"15"}
                                        absolute
                                    />
                                </View>

                                {/* Compare Section */}
                                <View style={[styles.chartContainer, { marginTop: 16 }]}>
                                    <Text style={[styles.chartTitle, { marginBottom: 12 }]}>{t('analytics.global.compareTitle')}</Text>

                                    <View style={{ width: '100%', marginBottom: selectedCompareIds.length > 0 ? 12 : 0 }}>
                                        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingHorizontal: 16, gap: 8, flexDirection: 'row' }}>
                                            {selectedCompareIds.map((id, index) => {
                                                const eventInfo = globalData.activeEventPerformances.find(e => e.eventId === id);
                                                const colors = ["#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336"];
                                                return (
                                                    <View key={id} style={styles.chip}>
                                                        <View style={[styles.chipDot, { backgroundColor: colors[index % colors.length] }]} />
                                                        <Text style={styles.chipText}>{eventInfo?.title.length && eventInfo.title.length > 15 ? eventInfo.title.substring(0, 15) + '...' : eventInfo?.title}</Text>
                                                        <Pressable onPress={() => toggleCompareSelection(id)} hitSlop={8} style={{ marginLeft: 4 }}>
                                                            <Ionicons name="close" size={14} color={isDark ? '#A0A7B1' : '#5C6A80'} />
                                                        </Pressable>
                                                    </View>
                                                );
                                            })}
                                            {selectedCompareIds.length < 5 && (
                                                <Pressable style={styles.addChip} onPress={() => setModalVisible(true)}>
                                                    <Ionicons name="add" size={14} color={isDark ? '#A0A7B1' : '#5C6A80'} />
                                                    <Text style={styles.addChipText}>{t('analytics.global.addEvent')}</Text>
                                                </Pressable>
                                            )}
                                        </ScrollView>
                                    </View>

                                    {selectedCompareIds.length > 0 ? (
                                        <>
                                            {loadingCompare ? (
                                                <ActivityIndicator size="small" color={isDark ? '#9FC3FF' : '#0056A8'} style={{ marginVertical: 30 }} />
                                            ) : getCompareChartData() && (
                                                <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                                                    <LineChart
                                                        data={getCompareChartData()!}
                                                        width={Math.max(screenWidth, getCompareChartData()!.labels.length * 30)}
                                                        height={280}
                                                        withDots={false}
                                                        withInnerLines={false}
                                                        chartConfig={{
                                                            backgroundColor: isDark ? '#1F2328' : '#FAFBFD',
                                                            backgroundGradientFrom: isDark ? '#1F2328' : '#FAFBFD',
                                                            backgroundGradientTo: isDark ? '#1F2328' : '#FAFBFD',
                                                            decimalPlaces: 0,
                                                            color: (opacity = 1) => isDark ? `rgba(255, 255, 255, ${opacity})` : `rgba(15, 40, 72, ${opacity})`,
                                                            labelColor: (opacity = 1) => isDark ? '#A0A7B1' : '#5C6A80',
                                                            strokeWidth: 3,
                                                            propsForBackgroundLines: { strokeDasharray: "" },
                                                            useShadowColorFromDataset: false,
                                                        }}
                                                        bezier
                                                        fromZero
                                                        style={{ marginVertical: 8, borderRadius: 12 }}
                                                    />
                                                </ScrollView>
                                            )}
                                            <Text style={{ fontSize: 12, color: isDark ? '#A0A7B1' : '#5C6A80', alignSelf: 'center', marginBottom: 8 }}>
                                                {t('analytics.global.chartSubtitle')}
                                            </Text>
                                        </>
                                    ) : (
                                        <View style={{ paddingVertical: 30, alignItems: 'center' }}>
                                            <Ionicons name="analytics-outline" size={32} color={isDark ? '#333A45' : '#D7E1F0'} />
                                            <Text style={{ color: isDark ? '#A0A7B1' : '#5C6A80', marginTop: 8, textAlign: 'center', paddingHorizontal: 16 }}>{t('analytics.global.selectPrompt')}</Text>
                                        </View>
                                    )}
                                </View>

                                <ThemedText style={[styles.title, { fontSize: 18, marginTop: 20 }]}>{t('analytics.global.individualTitle')}</ThemedText>
                            </View>
                        }
                        ListEmptyComponent={
                            <ThemedText style={{ color: isDark ? '#A0A7B1' : '#5C6A80' }}>
                                {t('analytics.global.noEventsFound')}
                            </ThemedText>
                        }
                    />
                )}
            </ThemedView>

            {/* Modal for selecting events to compare */}
            <Modal visible={isModalVisible} animationType="slide" transparent={true}>
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>{t('analytics.global.selectEvent')}</Text>
                            <Pressable onPress={() => setModalVisible(false)} hitSlop={10}>
                                <Ionicons name="close" size={24} color={isDark ? '#ECEDEE' : '#0F2848'} />
                            </Pressable>
                        </View>
                        <FlatList
                            data={globalData?.activeEventPerformances.filter(e => !selectedCompareIds.includes(e.eventId))}
                            keyExtractor={item => item.eventId.toString()}
                            renderItem={({ item }) => (
                                <Pressable
                                    style={({ pressed }) => [styles.modalItem, pressed && styles.cardPressed]}
                                    onPress={() => {
                                        toggleCompareSelection(item.eventId);
                                        setModalVisible(false);
                                    }}
                                >
                                    <Text style={styles.modalItemTitle}>{item.title}</Text>
                                    <Ionicons name="add-circle-outline" size={20} color={isDark ? '#9FC3FF' : '#0056A8'} />
                                </Pressable>
                            )}
                            ListEmptyComponent={
                                <Text style={{ color: isDark ? '#A0A7B1' : '#5C6A80', textAlign: 'center', marginTop: 20 }}>
                                    {t('analytics.global.noMoreEvents')}
                                </Text>
                            }
                        />
                    </View>
                </View>
            </Modal>
        </MenuLayout>
    );
}

const getStyles = (isDark: boolean) => {
    const background = isDark ? '#0F1419' : '#F5F7FB';
    const textPrimary = isDark ? '#ECEDEE' : '#0F2848';
    const cardBackground = isDark ? '#1F2328' : '#FFFFFF';
    const border = isDark ? '#333A45' : '#D7E1F0';
    const textMuted = isDark ? '#A0A7B1' : '#5C6A80';

    return StyleSheet.create({
        container: { flex: 1, backgroundColor: background, padding: 16 },
        headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 },
        title: { fontSize: 22, fontWeight: '700', color: textPrimary },
        seedBtn: { backgroundColor: '#FF5722', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 8 },
        seedBtnText: { color: '#fff', fontWeight: 'bold', fontSize: 12 },
        centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
        listContent: { paddingBottom: 20, gap: 12 },

        statsRow: { flexDirection: 'row', gap: 12, marginBottom: 16 },
        statBox: {
            flex: 1, backgroundColor: cardBackground, borderWidth: 1, borderColor: border,
            borderRadius: 12, padding: 16, alignItems: 'center'
        },
        statBoxTitle: { fontSize: 12, color: textMuted, marginBottom: 4, fontWeight: '600' },
        statBoxValue: { fontSize: 24, fontWeight: '800', color: textPrimary },
        statBoxDelta: { fontSize: 11, fontWeight: '700', marginTop: 4 },

        chartContainer: {
            backgroundColor: cardBackground, borderWidth: 1, borderColor: border,
            borderRadius: 12, paddingVertical: 16, alignItems: 'center', marginBottom: 12
        },
        chartTitle: { fontSize: 14, fontWeight: '600', color: textPrimary, alignSelf: 'flex-start', marginLeft: 16, marginBottom: 4 },

        card: {
            backgroundColor: cardBackground, borderWidth: 1, borderColor: border, borderRadius: 12,
            padding: 16, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        },
        cardPressed: { opacity: 0.8 },
        cardTitle: { fontSize: 16, fontWeight: 'bold', color: textPrimary, marginBottom: 4 },
        cardSubtitle: { fontSize: 13, color: textMuted },

        badge: {
            flexDirection: 'row', alignItems: 'center', gap: 4,
            borderWidth: 1, borderRadius: 12, paddingHorizontal: 6, paddingVertical: 2
        },
        badgeText: { fontSize: 11, fontWeight: '700' },

        // Google Trends Chips Styles
        chip: {
            flexDirection: 'row', alignItems: 'center', backgroundColor: isDark ? '#333A45' : '#E8EEF8',
            paddingVertical: 6, paddingHorizontal: 12, borderRadius: 16, borderWidth: 1, borderColor: border
        },
        chipDot: { width: 8, height: 8, borderRadius: 4, marginRight: 6 },
        chipText: { fontSize: 13, color: textPrimary, fontWeight: '500' },
        addChip: {
            flexDirection: 'row', alignItems: 'center', backgroundColor: 'transparent',
            paddingVertical: 6, paddingHorizontal: 12, borderRadius: 16, borderWidth: 1, borderColor: border, borderStyle: 'dashed'
        },
        addChipText: { fontSize: 13, color: textMuted, fontWeight: '500', marginLeft: 4 },

        // Modal Styles
        modalOverlay: {
            flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end'
        },
        modalContent: {
            backgroundColor: background, borderTopLeftRadius: 20, borderTopRightRadius: 20,
            padding: 20, maxHeight: Dimensions.get('window').height * 0.7
        },
        modalHeader: {
            flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16
        },
        modalTitle: { fontSize: 18, fontWeight: 'bold', color: textPrimary },
        modalItem: {
            flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
            paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: border
        },
        modalItemTitle: { fontSize: 15, color: textPrimary }
    });
};
