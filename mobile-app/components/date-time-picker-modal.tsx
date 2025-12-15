import React, { useEffect, useMemo, useState } from 'react';
import {
    Pressable,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

type Props = {
    visible: boolean;
    title: string;
    initialValue: Date;
    accentColor?: string;
    cancelLabel: string;
    confirmLabel: string;
    timeLabel: string;
    onCancel: () => void;
    onConfirm: (value: Date) => void;
};

type DayCell = {
    key: string;
    date: Date;
    inMonth: boolean;
};

const SURFACE = '#F5F7FB';
const DEFAULT_ACCENT = '#0056A8';
const MINUTE_STEP = 5;
const QUICK_MINUTES = [0, 15, 30, 45];

function pad2(value: number) {
    return value.toString().padStart(2, '0');
}

function startOfMonth(value: Date) {
    return new Date(value.getFullYear(), value.getMonth(), 1);
}

function addMonths(value: Date, months: number) {
    return new Date(value.getFullYear(), value.getMonth() + months, 1);
}

function sameDay(a: Date, b: Date) {
    return (
        a.getFullYear() === b.getFullYear() &&
        a.getMonth() === b.getMonth() &&
        a.getDate() === b.getDate()
    );
}

function clampSeconds(value: Date) {
    const next = new Date(value);
    next.setSeconds(0, 0);
    return next;
}

function roundUpToMinuteStep(value: Date, stepMinutes: number) {
    const next = clampSeconds(value);
    const minutes = next.getMinutes();
    const remainder = minutes % stepMinutes;

    if (remainder === 0) return next;

    const delta = stepMinutes - remainder;
    next.setMinutes(minutes + delta, 0, 0);
    return next;
}

function buildCalendarGrid(month: Date): DayCell[] {
    const first = startOfMonth(month);
    const firstDowSunday0 = first.getDay(); // 0..6, Sunday=0
    const mondayIndex = (firstDowSunday0 + 6) % 7; // 0..6, Monday=0

    const gridStart = new Date(first);
    gridStart.setDate(first.getDate() - mondayIndex);

    const cells: DayCell[] = [];
    for (let i = 0; i < 42; i += 1) {
        const d = new Date(gridStart);
        d.setDate(gridStart.getDate() + i);
        cells.push({
            key: `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`,
            date: d,
            inMonth: d.getMonth() === month.getMonth(),
        });
    }
    return cells;
}

function applyDay(base: Date, day: Date) {
    const next = new Date(base);
    next.setFullYear(day.getFullYear(), day.getMonth(), day.getDate());
    return next;
}

function formatMonthTitle(value: Date) {
    return value.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
}

export function DateTimePickerModal({
    visible,
    title,
    initialValue,
    accentColor = DEFAULT_ACCENT,
    cancelLabel,
    confirmLabel,
    timeLabel,
    onCancel,
    onConfirm,
}: Props) {
    const [selected, setSelected] = useState<Date>(() => roundUpToMinuteStep(initialValue, MINUTE_STEP));
    const [activeMonth, setActiveMonth] = useState<Date>(() => startOfMonth(initialValue));

    useEffect(() => {
        if (!visible) return;
        const normalized = roundUpToMinuteStep(initialValue, MINUTE_STEP);
        setSelected(normalized);
        setActiveMonth(startOfMonth(normalized));
    }, [visible, initialValue]);

    const dayCells = useMemo(() => buildCalendarGrid(activeMonth), [activeMonth]);

    const adjustHours = (delta: number) => {
        setSelected((current) => {
            const next = new Date(current);
            const hour = (current.getHours() + delta + 24) % 24;
            next.setHours(hour, current.getMinutes(), 0, 0);
            return next;
        });
    };

    const adjustMinutes = (delta: number) => {
        setSelected((current) => {
            const totalMinutes = current.getHours() * 60 + current.getMinutes() + delta;
            const normalized = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60);
            const nextHour = Math.floor(normalized / 60);
            const nextMinute = normalized % 60;
            const next = new Date(current);
            next.setHours(nextHour, nextMinute, 0, 0);
            return next;
        });
    };

    const setMinutes = (minute: number) => {
        setSelected((current) => {
            const next = new Date(current);
            next.setMinutes(minute, 0, 0);
            return next;
        });
    };

    const handleConfirm = () => onConfirm(clampSeconds(selected));
    const formattedTime = `${pad2(selected.getHours())}:${pad2(selected.getMinutes())}`;

    if (!visible) return null;

    return (
        <View style={styles.overlay}>
            <Pressable style={styles.backdrop} onPress={onCancel} />
            <View style={styles.card}>
                <View style={styles.header}>
                    <Text style={styles.title}>{title}</Text>
                    <Pressable onPress={onCancel} hitSlop={10}>
                        <Ionicons name="close" size={20} color="#5C6A80" />
                    </Pressable>
                </View>

                <View style={styles.monthRow}>
                    <Pressable
                        style={styles.monthNav}
                        onPress={() => setActiveMonth((m) => addMonths(m, -1))}
                        hitSlop={10}
                    >
                        <Ionicons name="chevron-back" size={18} color={accentColor} />
                    </Pressable>
                    <Text style={styles.monthTitle}>{formatMonthTitle(activeMonth)}</Text>
                    <Pressable
                        style={styles.monthNav}
                        onPress={() => setActiveMonth((m) => addMonths(m, 1))}
                        hitSlop={10}
                    >
                        <Ionicons name="chevron-forward" size={18} color={accentColor} />
                    </Pressable>
                </View>

                <View style={styles.weekHeader}>
                    {['L', 'M', 'M', 'J', 'V', 'S', 'D'].map((label, index) => (
                        <Text key={`${label}-${index}`} style={styles.weekLabel}>
                            {label}
                        </Text>
                    ))}
                </View>

                <View style={styles.calendarGrid}>
                    {dayCells.map((cell) => {
                        const isSelected = sameDay(cell.date, selected);
                        return (
                            <Pressable
                                key={cell.key}
                                style={[
                                    styles.dayCell,
                                    isSelected && { backgroundColor: accentColor },
                                ]}
                                onPress={() => {
                                    const next = applyDay(selected, cell.date);
                                    setSelected(next);
                                    if (!cell.inMonth) {
                                        setActiveMonth(startOfMonth(cell.date));
                                    }
                                }}
                            >
                                <Text
                                    style={[
                                        styles.dayText,
                                        !cell.inMonth && styles.dayTextMuted,
                                        isSelected && styles.dayTextSelected,
                                    ]}
                                >
                                    {cell.date.getDate()}
                                </Text>
                            </Pressable>
                        );
                    })}
                </View>

                <Text style={styles.sectionTitle}>{timeLabel}</Text>
                <View style={styles.timeSummary}>
                    <Text style={styles.timeSummaryLabel}>HH:MM</Text>
                    <Text style={[styles.timeSummaryValue, { color: accentColor }]}>
                        {formattedTime}
                    </Text>
                </View>

                <View style={styles.timeRow}>
                    <View style={styles.timeColumn}>
                        <Text style={styles.timeLabel}>Hours</Text>
                        <View style={styles.stepper}>
                            <Pressable
                                style={styles.stepperButton}
                                onPress={() => adjustHours(-1)}
                                hitSlop={8}
                            >
                                <Ionicons name="remove" size={16} color="#0F2848" />
                            </Pressable>
                            <Text style={styles.stepperValue}>{pad2(selected.getHours())}</Text>
                            <Pressable
                                style={styles.stepperButton}
                                onPress={() => adjustHours(1)}
                                hitSlop={8}
                            >
                                <Ionicons name="add" size={16} color="#0F2848" />
                            </Pressable>
                        </View>
                    </View>
                    <View style={styles.timeColumn}>
                        <Text style={styles.timeLabel}>Minutes</Text>
                        <View style={styles.stepper}>
                            <Pressable
                                style={styles.stepperButton}
                                onPress={() => adjustMinutes(-MINUTE_STEP)}
                                hitSlop={8}
                            >
                                <Ionicons name="remove" size={16} color="#0F2848" />
                            </Pressable>
                            <Text style={styles.stepperValue}>{pad2(selected.getMinutes())}</Text>
                            <Pressable
                                style={styles.stepperButton}
                                onPress={() => adjustMinutes(MINUTE_STEP)}
                                hitSlop={8}
                            >
                                <Ionicons name="add" size={16} color="#0F2848" />
                            </Pressable>
                        </View>
                    </View>
                </View>

                <Text style={styles.quickLabel}>Quick minutes</Text>
                <View style={styles.quickRow}>
                    {QUICK_MINUTES.map((minute) => {
                        const selectedChip = selected.getMinutes() === minute;
                        return (
                            <Pressable
                                key={minute}
                                style={[
                                    styles.quickChip,
                                    selectedChip && { backgroundColor: accentColor, borderColor: accentColor },
                                ]}
                                onPress={() => setMinutes(minute)}
                            >
                                <Text
                                    style={[
                                        styles.quickChipText,
                                        selectedChip && styles.quickChipTextSelected,
                                    ]}
                                >
                                    {pad2(minute)}
                                </Text>
                            </Pressable>
                        );
                    })}
                </View>

                <View style={styles.actions}>
                    <Pressable style={styles.secondaryButton} onPress={onCancel}>
                        <Text style={styles.secondaryText}>{cancelLabel}</Text>
                    </Pressable>
                    <Pressable style={[styles.primaryButton, { backgroundColor: accentColor }]} onPress={handleConfirm}>
                        <Text style={styles.primaryText}>{confirmLabel}</Text>
                    </Pressable>
                </View>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    overlay: {
        ...StyleSheet.absoluteFillObject,
        justifyContent: 'center',
        padding: 18,
        zIndex: 1000,
    },
    backdrop: {
        ...StyleSheet.absoluteFillObject,
        backgroundColor: 'rgba(15, 28, 48, 0.5)',
        zIndex: 0,
    },
    card: {
        backgroundColor: '#FFFFFF',
        borderRadius: 18,
        padding: 14,
        maxHeight: '92%',
        zIndex: 1,
        shadowColor: '#000',
        shadowOpacity: 0.08,
        shadowRadius: 18,
        shadowOffset: { width: 0, height: 8 },
        elevation: 8,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E7F3',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
    },
    title: {
        flex: 1,
        fontSize: 16,
        fontWeight: '800',
        color: '#0F2848',
    },
    monthRow: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginTop: 12,
        paddingVertical: 6,
        borderRadius: 12,
        backgroundColor: SURFACE,
        paddingHorizontal: 10,
    },
    monthNav: {
        width: 34,
        height: 34,
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: 10,
        backgroundColor: '#FFFFFF',
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#DCE4F2',
    },
    monthTitle: {
        fontSize: 13,
        fontWeight: '800',
        color: '#0F2848',
        textTransform: 'capitalize',
    },
    weekHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginTop: 10,
        paddingHorizontal: 2,
    },
    weekLabel: {
        width: `${100 / 7}%`,
        textAlign: 'center',
        fontSize: 12,
        fontWeight: '800',
        color: '#5C6A80',
    },
    calendarGrid: {
        marginTop: 8,
        flexDirection: 'row',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
    },
    dayCell: {
        width: `${100 / 7}%`,
        aspectRatio: 1,
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: 12,
        marginVertical: 2,
    },
    dayText: {
        color: '#0F2848',
        fontSize: 13,
        fontWeight: '800',
    },
    dayTextMuted: {
        color: '#9BA5B7',
        fontWeight: '700',
    },
    dayTextSelected: {
        color: '#FFFFFF',
    },
    sectionTitle: {
        marginTop: 10,
        fontSize: 12,
        fontWeight: '800',
        color: '#1B2F4A',
    },
    timeSummary: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: SURFACE,
        paddingHorizontal: 12,
        paddingVertical: 10,
        borderRadius: 12,
        marginTop: 8,
    },
    timeSummaryLabel: {
        color: '#5C6A80',
        fontSize: 12,
        fontWeight: '700',
    },
    timeSummaryValue: {
        fontSize: 16,
        fontWeight: '800',
        color: '#0F2848',
    },
    timeRow: {
        flexDirection: 'row',
        gap: 12,
        marginTop: 10,
    },
    timeColumn: {
        flex: 1,
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#DCE4F2',
        padding: 10,
        shadowColor: '#000',
        shadowOpacity: 0.03,
        shadowRadius: 6,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    timeLabel: {
        fontSize: 12,
        fontWeight: '800',
        color: '#1B2F4A',
        marginBottom: 8,
    },
    stepper: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: SURFACE,
        borderRadius: 10,
        paddingHorizontal: 8,
        paddingVertical: 6,
    },
    stepperButton: {
        width: 34,
        height: 34,
        borderRadius: 10,
        backgroundColor: '#FFFFFF',
        borderWidth: 1,
        borderColor: '#DCE4F2',
        alignItems: 'center',
        justifyContent: 'center',
    },
    stepperValue: {
        fontSize: 16,
        fontWeight: '800',
        color: '#0F2848',
    },
    quickLabel: {
        marginTop: 12,
        fontSize: 12,
        fontWeight: '800',
        color: '#1B2F4A',
    },
    quickRow: {
        flexDirection: 'row',
        gap: 8,
        marginTop: 8,
    },
    quickChip: {
        flex: 1,
        paddingVertical: 10,
        borderRadius: 12,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#FFFFFF',
        borderWidth: 1,
        borderColor: '#DCE4F2',
    },
    quickChipText: {
        fontSize: 13,
        fontWeight: '800',
        color: '#0F2848',
    },
    quickChipTextSelected: {
        color: '#FFFFFF',
    },
    actions: {
        flexDirection: 'row',
        gap: 12,
        marginTop: 14,
    },
    secondaryButton: {
        flex: 1,
        borderRadius: 12,
        paddingVertical: 12,
        borderWidth: 1,
        borderColor: '#DCE4F2',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#FFFFFF',
    },
    secondaryText: {
        color: '#1B2F4A',
        fontWeight: '800',
        fontSize: 14,
    },
    primaryButton: {
        flex: 1,
        borderRadius: 12,
        paddingVertical: 12,
        alignItems: 'center',
        justifyContent: 'center',
    },
    primaryText: {
        color: '#FFFFFF',
        fontWeight: '800',
        fontSize: 14,
    },
});
