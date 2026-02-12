import React, { useMemo, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    Pressable,
    StyleSheet,
    View,
} from 'react-native';
import { Redirect } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';

import { DateTimePickerModal } from '../../components/date-time-picker-modal';
import { MenuLayout } from '../../components/menu-layout';
import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import { getAttendanceReports, type AttendanceReport } from '../../services/attendance.service';
import { formatIsoDate, formatIsoTime } from '../../utils/date-time';

function pad2(value: number) {
    return value.toString().padStart(2, '0');
}

function formatDateForApi(date: Date) {
    return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
}

function formatDateForDisplay(date: Date) {
    return formatDateForApi(date);
}

export default function AttendanceReportsScreen() {
    const { t } = useTranslation();
    const { hasRole } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const isDark = colorScheme === 'dark';
    const styles = getStyles(isDark);

    const isAdmin = hasRole(['ROLE_ADMIN']);
    const [startDate, setStartDate] = useState(() => {
        const now = new Date();
        const date = new Date(now);
        date.setDate(now.getDate() - 30);
        return date;
    });
    const [endDate, setEndDate] = useState(() => new Date());
    const [pickerVisible, setPickerVisible] = useState(false);
    const [activePicker, setActivePicker] = useState<'start' | 'end' | null>(null);
    const [reports, setReports] = useState<AttendanceReport[]>([]);
    const [loading, setLoading] = useState(false);
    const [hasGenerated, setHasGenerated] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const pickerInitialValue = useMemo(() => {
        if (activePicker === 'start') return startDate;
        if (activePicker === 'end') return endDate;
        return new Date();
    }, [activePicker, endDate, startDate]);

    if (!isAdmin) {
        return <Redirect href="/admin" />;
    }

    const openPicker = (target: 'start' | 'end') => {
        setActivePicker(target);
        setPickerVisible(true);
    };

    const onConfirmPicker = (value: Date) => {
        const normalized = new Date(value);
        normalized.setHours(0, 0, 0, 0);
        if (activePicker === 'start') setStartDate(normalized);
        if (activePicker === 'end') setEndDate(normalized);
        setPickerVisible(false);
        setActivePicker(null);
    };

    const onGenerate = async () => {
        if (startDate.getTime() > endDate.getTime()) {
            setError(t('admin.attendanceReports.invalidDateRange'));
            setReports([]);
            setHasGenerated(true);
            return;
        }

        try {
            setLoading(true);
            setError(null);
            const data = await getAttendanceReports(
                formatDateForApi(startDate),
                formatDateForApi(endDate),
            );
            setReports(data);
        } catch (err) {
            console.error('Failed to load attendance reports', err);
            setError(t('admin.attendanceReports.errorLoading'));
            setReports([]);
        } finally {
            setLoading(false);
            setHasGenerated(true);
        }
    };

    return (
        <MenuLayout>
            <ThemedView style={styles.container}>
                <ThemedText style={styles.title}>{t('admin.attendanceReports.title')}</ThemedText>

                <View style={styles.controls}>
                    <Pressable
                        onPress={() => openPicker('start')}
                        style={styles.dateField}
                        accessibilityRole="button"
                        accessibilityLabel={t('admin.attendanceReports.startDate')}
                    >
                        <Ionicons name="calendar-outline" size={18} color={isDark ? '#ECEDEE' : '#0F2848'} />
                        <View style={styles.dateTextWrap}>
                            <ThemedText style={styles.dateLabel}>{t('admin.attendanceReports.startDate')}</ThemedText>
                            <ThemedText style={styles.dateValue}>{formatDateForDisplay(startDate)}</ThemedText>
                        </View>
                    </Pressable>

                    <Pressable
                        onPress={() => openPicker('end')}
                        style={styles.dateField}
                        accessibilityRole="button"
                        accessibilityLabel={t('admin.attendanceReports.endDate')}
                    >
                        <Ionicons name="calendar-outline" size={18} color={isDark ? '#ECEDEE' : '#0F2848'} />
                        <View style={styles.dateTextWrap}>
                            <ThemedText style={styles.dateLabel}>{t('admin.attendanceReports.endDate')}</ThemedText>
                            <ThemedText style={styles.dateValue}>{formatDateForDisplay(endDate)}</ThemedText>
                        </View>
                    </Pressable>

                    <Pressable
                        onPress={onGenerate}
                        style={[styles.generateButton, loading && styles.generateButtonDisabled]}
                        disabled={loading}
                        accessibilityRole="button"
                        accessibilityLabel={t('admin.attendanceReports.generate')}
                    >
                        <ThemedText style={styles.generateButtonText}>
                            {t('admin.attendanceReports.generate')}
                        </ThemedText>
                    </Pressable>
                </View>

                {loading && (
                    <View style={styles.centered}>
                        <ActivityIndicator size="large" color={isDark ? '#9FC3FF' : '#0056A8'} />
                    </View>
                )}

                {!loading && error && (
                    <ThemedText style={styles.errorText}>{error}</ThemedText>
                )}

                {!loading && !error && hasGenerated && reports.length === 0 && (
                    <ThemedText style={styles.emptyText}>{t('admin.attendanceReports.noResults')}</ThemedText>
                )}

                {!loading && !error && reports.length > 0 && (
                    <FlatList
                        data={reports}
                        keyExtractor={(item) => item.eventId.toString()}
                        contentContainerStyle={styles.listContent}
                        renderItem={({ item }) => {
                            const rate = Math.round((item.attendanceRate ?? 0) * 100);
                            return (
                                <View style={styles.card}>
                                    <ThemedText style={styles.cardTitle}>{item.eventTitle}</ThemedText>
                                    <ThemedText style={styles.cardDate}>
                                        {formatIsoDate(item.eventDate)} {formatIsoTime(item.eventDate)}
                                    </ThemedText>
                                    <View style={styles.metricRow}>
                                        <ThemedText style={styles.metricLabel}>
                                            {t('admin.attendanceReports.totalAttended')}
                                        </ThemedText>
                                        <ThemedText style={styles.metricValue}>
                                            {item.totalAttended}
                                        </ThemedText>
                                    </View>
                                    <View style={styles.metricRow}>
                                        <ThemedText style={styles.metricLabel}>
                                            {t('admin.attendanceReports.attendanceRate')}
                                        </ThemedText>
                                        <ThemedText style={styles.metricValue}>
                                            {rate}%
                                        </ThemedText>
                                    </View>
                                </View>
                            );
                        }}
                    />
                )}
            </ThemedView>

            <DateTimePickerModal
                visible={pickerVisible}
                title={
                    activePicker === 'start'
                        ? t('admin.attendanceReports.startDate')
                        : t('admin.attendanceReports.endDate')
                }
                initialValue={pickerInitialValue}
                cancelLabel={t('common.cancel')}
                confirmLabel={t('common.confirm')}
                timeLabel={t('events.fields.time')}
                onCancel={() => {
                    setPickerVisible(false);
                    setActivePicker(null);
                }}
                onConfirm={onConfirmPicker}
            />
        </MenuLayout>
    );
}

const getStyles = (isDark: boolean) => {
    const background = isDark ? '#0F1419' : '#F5F7FB';
    const cardBackground = isDark ? '#1F2328' : '#FFFFFF';
    const border = isDark ? '#333A45' : '#D7E1F0';
    const textPrimary = isDark ? '#ECEDEE' : '#0F2848';
    const textMuted = isDark ? '#A0A7B1' : '#5C6A80';
    const accent = isDark ? '#9FC3FF' : '#0056A8';

    return StyleSheet.create({
        container: {
            flex: 1,
            backgroundColor: background,
            paddingHorizontal: 16,
            paddingTop: 16,
        },
        title: {
            fontSize: 22,
            fontWeight: '700',
            color: textPrimary,
            marginBottom: 14,
        },
        controls: {
            gap: 10,
            marginBottom: 14,
        },
        dateField: {
            borderWidth: 1,
            borderColor: border,
            borderRadius: 12,
            backgroundColor: cardBackground,
            paddingHorizontal: 12,
            paddingVertical: 12,
            flexDirection: 'row',
            alignItems: 'center',
            gap: 10,
        },
        dateTextWrap: {
            flex: 1,
        },
        dateLabel: {
            fontSize: 12,
            color: textMuted,
            marginBottom: 2,
        },
        dateValue: {
            fontSize: 15,
            color: textPrimary,
            fontWeight: '700',
        },
        generateButton: {
            backgroundColor: accent,
            borderRadius: 12,
            alignItems: 'center',
            justifyContent: 'center',
            paddingVertical: 12,
        },
        generateButtonDisabled: {
            opacity: 0.7,
        },
        generateButtonText: {
            color: '#FFFFFF',
            fontSize: 15,
            fontWeight: '700',
        },
        centered: {
            alignItems: 'center',
            justifyContent: 'center',
            marginTop: 28,
        },
        errorText: {
            color: '#C0392B',
            fontSize: 14,
            marginTop: 6,
        },
        emptyText: {
            fontSize: 14,
            color: textMuted,
            marginTop: 6,
        },
        listContent: {
            paddingBottom: 20,
            gap: 10,
        },
        card: {
            backgroundColor: cardBackground,
            borderWidth: 1,
            borderColor: border,
            borderRadius: 14,
            padding: 14,
            gap: 8,
        },
        cardTitle: {
            fontSize: 16,
            fontWeight: '700',
            color: textPrimary,
        },
        cardDate: {
            fontSize: 13,
            color: textMuted,
        },
        metricRow: {
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center',
        },
        metricLabel: {
            color: textMuted,
            fontSize: 13,
            fontWeight: '600',
        },
        metricValue: {
            color: textPrimary,
            fontSize: 14,
            fontWeight: '700',
        },
    });
};
