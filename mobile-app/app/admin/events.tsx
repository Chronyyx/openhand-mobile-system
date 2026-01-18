import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Keyboard,
    KeyboardAvoidingView,
    SectionList,
    Modal,
    Platform,
    Pressable,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableWithoutFeedback,
    View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTranslation } from 'react-i18next';
import { useRouter } from 'expo-router';

import { AppHeader } from '../../components/app-header';
import { DateTimePickerModal } from '../../components/date-time-picker-modal';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { type EventSummary } from '../../services/events.service';
import { createEvent, updateEvent, cancelEvent, getManagedEvents, markEventCompleted, type CreateEventPayload } from '../../services/event-management.service';
import { getTranslatedEventTitle } from '../../utils/event-translations';
import { getStatusColor, getStatusLabel, getStatusTextColor } from '../../utils/event-status';

const ACCENT = '#0056A8';
const SURFACE = '#F5F7FB';

type FieldErrors = Partial<Record<
    | 'title'
    | 'description'
    | 'startDateTime'
    | 'endDateTime'
    | 'locationName'
    | 'address'
    | 'maxCapacity',
    string
>>;

type EventSection = {
    key: 'upcoming' | 'archived';
    title: string;
    emptyText: string;
    hint?: string;
    data: EventSummary[];
};

function pad2(value: number) {
    return value.toString().padStart(2, '0');
}

function formatLocalDateTimeForDisplay(date: Date) {
    return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(
        date.getHours(),
    )}:${pad2(date.getMinutes())}`;
}

function formatLocalDateTimeForApi(date: Date) {
    return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}T${pad2(
        date.getHours(),
    )}:${pad2(date.getMinutes())}`;
}

function parseLocalDateTime(value: string): Date {
    const normalized = value.includes('T') ? value : value.replace(' ', 'T');
    const parsed = new Date(normalized);
    if (Number.isNaN(parsed.getTime())) {
        return new Date();
    }
    return parsed;
}

function isCapacityTooLowError(error: unknown): boolean {
    const serverMessage =
        (error as any)?.response?.data?.message ??
        (typeof (error as any)?.response?.data === 'string'
            ? (error as any).response.data
            : null);

    return (
        typeof serverMessage === 'string' &&
        serverMessage
            .toLowerCase()
            .includes('maxcapacity cannot be less than current registrations')
    );
}

export default function AdminEventsScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { hasRole } = useAuth();
    const isAdmin = hasRole(['ROLE_ADMIN']);
    const canCompleteEvents = hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE']);

    const [menuVisible, setMenuVisible] = useState(false);
    const [loading, setLoading] = useState(true);
    const [events, setEvents] = useState<EventSummary[]>([]);
    const [error, setError] = useState<string | null>(null);

    const [formOpen, setFormOpen] = useState(false);
    const [editingEvent, setEditingEvent] = useState<EventSummary | null>(null);
    const [actionMenuFor, setActionMenuFor] = useState<number | null>(null);
    const [saving, setSaving] = useState(false);
    const [completingId, setCompletingId] = useState<number | null>(null);
    const [duplicatingId, setDuplicatingId] = useState<number | null>(null);
    const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [category, setCategory] = useState('');
    const [startDateTime, setStartDateTime] = useState<Date | null>(null);
    const [endDateTime, setEndDateTime] = useState<Date | null>(null);
    const [locationName, setLocationName] = useState('');
    const [address, setAddress] = useState('');
    const [maxCapacity, setMaxCapacity] = useState('');

    const [activePickerField, setActivePickerField] = useState<'start' | 'end' | null>(null);
    const [pickerVisible, setPickerVisible] = useState(false);
    const [pickerInitialValue, setPickerInitialValue] = useState<Date>(new Date());

    const loadEvents = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getManagedEvents();
            setEvents(data);
        } catch (e) {
            console.error('Failed to load events for admin', e);
            setError(t('admin.events.loadError'));
        } finally {
            setLoading(false);
        }
    }, [t]);

    useEffect(() => {
        loadEvents();
    }, [loadEvents]);

    const upcomingEvents = useMemo(() => {
        return events
            .filter((event) => event.status !== 'COMPLETED')
            .sort((a, b) => Date.parse(a.startDateTime) - Date.parse(b.startDateTime));
    }, [events]);

    const archivedEvents = useMemo(() => {
        return events
            .filter((event) => event.status === 'COMPLETED')
            .sort((a, b) => {
                const aTime = Date.parse(a.endDateTime ?? a.startDateTime);
                const bTime = Date.parse(b.endDateTime ?? b.startDateTime);
                return bTime - aTime;
            });
    }, [events]);

    const sections = useMemo<EventSection[]>(
        () => [
            {
                key: 'upcoming',
                title: t('admin.events.upcomingSection'),
                emptyText: t('admin.events.empty'),
                data: upcomingEvents,
            },
            {
                key: 'archived',
                title: t('admin.events.archivedSection'),
                emptyText: t('admin.events.archivedEmpty'),
                hint: t('admin.events.archivedHint'),
                data: archivedEvents,
            },
        ],
        [archivedEvents, upcomingEvents, t],
    );

    const resetForm = () => {
        setTitle('');
        setDescription('');
        setCategory('');
        setStartDateTime(null);
        setEndDateTime(null);
        setLocationName('');
        setAddress('');
        setMaxCapacity('');
        setFieldErrors({});
    };

    const openCreate = () => {
        if (!isAdmin) return;
        resetForm();
        setEditingEvent(null);
        setActionMenuFor(null);
        setFormOpen(true);
    };

    const openEdit = (event: EventSummary) => {
        setEditingEvent(event);
        setActionMenuFor(null);
        setFieldErrors({});
        setTitle(event.title);
        setDescription(event.description);
        setCategory(event.category ?? '');
        setStartDateTime(parseLocalDateTime(event.startDateTime));
        setEndDateTime(event.endDateTime ? parseLocalDateTime(event.endDateTime) : null);
        setLocationName(event.locationName);
        setAddress(event.address ?? '');
        setMaxCapacity(event.maxCapacity != null ? String(event.maxCapacity) : '');
        setFormOpen(true);
    };

    const closeForm = () => {
        setFormOpen(false);
        setSaving(false);
        setCompletingId(null);
        setFieldErrors({});
        setEditingEvent(null);
        closePicker();
    };

    const buildPayloadFromEvent = (event: EventSummary): CreateEventPayload => ({
        title: event.title.trim(),
        description: event.description.trim(),
        startDateTime: formatLocalDateTimeForApi(parseLocalDateTime(event.startDateTime)),
        endDateTime: event.endDateTime ? formatLocalDateTimeForApi(parseLocalDateTime(event.endDateTime)) : null,
        locationName: event.locationName.trim(),
        address: (event.address ?? '').trim(),
        maxCapacity: event.maxCapacity ?? null,
        category: event.category?.trim() ?? null,
    });

    const validate = (): boolean => {
        const nextErrors: FieldErrors = {};

        if (!title.trim()) nextErrors.title = t('admin.events.fields.titleRequired');
        if (!description.trim()) nextErrors.description = t('admin.events.fields.descriptionRequired');
        if (!locationName.trim()) nextErrors.locationName = t('admin.events.fields.locationNameRequired');
        if (!address.trim()) nextErrors.address = t('admin.events.fields.addressRequired');

        if (!startDateTime) {
            nextErrors.startDateTime = t('admin.events.fields.startDateTimeInvalid');
        } else {
            const now = new Date();
            if (startDateTime.getTime() <= now.getTime()) {
                nextErrors.startDateTime = t('admin.events.fields.startDateTimeFuture');
            }
        }
        if (startDateTime && endDateTime && endDateTime.getTime() <= startDateTime.getTime()) {
            nextErrors.endDateTime = t('admin.events.fields.endDateTimeInvalid');
        }

        if (maxCapacity.trim()) {
            const parsed = Number(maxCapacity);
            if (!Number.isFinite(parsed) || parsed <= 0 || !Number.isInteger(parsed)) {
                nextErrors.maxCapacity = t('admin.events.fields.maxCapacityInvalid');
            }
        }

        setFieldErrors(nextErrors);
        return Object.keys(nextErrors).length === 0;
    };

    const handleSubmit = async () => {
        if (!validate()) return;

        setSaving(true);
        setError(null);
        try {
            const payload = {
                title: title.trim(),
                description: description.trim(),
                startDateTime: formatLocalDateTimeForApi(startDateTime!),
                endDateTime: endDateTime ? formatLocalDateTimeForApi(endDateTime) : null,
                locationName: locationName.trim(),
                address: address.trim(),
                maxCapacity: maxCapacity.trim() ? Number(maxCapacity) : null,
                category: category.trim() ? category.trim() : null,
            };

            if (editingEvent) {
                await updateEvent(editingEvent.id, payload);
                Alert.alert(t('admin.events.updateSuccessTitle'), t('admin.events.updateSuccessMessage'));
            } else {
                await createEvent(payload);
                Alert.alert(t('admin.events.createSuccessTitle'), t('admin.events.createSuccessMessage'));
            }

            closeForm();
            await loadEvents();
        } catch (e) {
            console.error('Failed to save event', e);
            if (isCapacityTooLowError(e)) {
                setError(t('admin.events.capacityTooLow'));
            } else {
                setError(
                    t(editingEvent ? 'admin.events.updateError' : 'admin.events.createError'),
                );
            }
        } finally {
            setSaving(false);
        }
    };

    const handleDuplicate = async (event: EventSummary) => {
        if (duplicatingId) return;

        setDuplicatingId(event.id);
        setError(null);

        try {
            const payload = buildPayloadFromEvent(event);
            await createEvent(payload);
            Alert.alert(
                t('admin.events.duplicateSuccessTitle'),
                t('admin.events.duplicateSuccessMessage'),
            );
            await loadEvents();
        } catch (e) {
            console.error('Failed to duplicate event', e);
            if (isCapacityTooLowError(e)) {
                setError(t('admin.events.capacityTooLow'));
            } else {
                setError(t('admin.events.duplicateError'));
            }
        } finally {
            setDuplicatingId(null);
            setActionMenuFor(null);
        }
    };

    const confirmMarkCompleted = async (event: EventSummary) => {
        if (completingId) return;

        setCompletingId(event.id);
        setError(null);
        try {
            await markEventCompleted(event.id);
            Alert.alert(
                t('admin.events.completeSuccessTitle'),
                t('admin.events.completeSuccessMessage'),
            );
            closeForm();
            await loadEvents();
        } catch (e) {
            console.error('Failed to mark event completed', e);
            setError(t('admin.events.completeError'));
        } finally {
            setCompletingId(null);
            setActionMenuFor(null);
        }
    };

    const handleMarkCompleted = (event: EventSummary) => {
        if (Platform.OS === 'web') {
            const confirmed = window.confirm(t('admin.events.completeConfirmMessage'));
            if (confirmed) {
                confirmMarkCompleted(event);
            }
        } else {
            Alert.alert(
                t('admin.events.completeConfirmTitle'),
                t('admin.events.completeConfirmMessage'),
                [
                    { text: t('common.cancel'), style: 'cancel' },
                    {
                        text: t('admin.events.completeConfirmAction'),
                        style: 'destructive',
                        onPress: () => confirmMarkCompleted(event),
                    },
                ],
            );
        }
    };

    const openDateTimePicker = (field: 'start' | 'end') => {
        Keyboard.dismiss();

        setFieldErrors((current) => {
            const next = { ...current };
            delete next.startDateTime;
            delete next.endDateTime;
            return next;
        });

        setActivePickerField(field);

        const currentValue = field === 'start' ? startDateTime : endDateTime;
        const initialValue = currentValue ?? new Date();

        setPickerInitialValue(initialValue);
        setPickerVisible(true);
    };

    const closePicker = () => {
        setPickerVisible(false);
        setActivePickerField(null);
    };

    const confirmPicker = (value: Date) => {
        const normalized = new Date(value);
        normalized.setSeconds(0, 0);
        if (activePickerField === 'start') setStartDateTime(normalized);
        if (activePickerField === 'end') setEndDateTime(normalized);
        closePicker();
    };

    const handleNavigateHome = () => {
        setMenuVisible(false);
        router.replace('/');
    };

    const handleNavigateEvents = () => {
        setMenuVisible(false);
        router.push('/events');
    };

    const handleNavigateDashboard = () => {
        setMenuVisible(false);
        router.push('/admin');
    };

    const handleNavigateProfile = () => {
        setMenuVisible(false);
        router.push('/profile');
    };

    const selectedActionEvent = actionMenuFor
        ? events.find((evt) => evt.id === actionMenuFor) ?? null
        : null;

    const isArchived = editingEvent?.status === 'COMPLETED';
    const canEditFields = isAdmin && !isArchived;
    const secondaryLabel = canEditFields ? t('common.cancel') : t('common.close');

    const renderEventItem = ({ item, section }: { item: EventSummary; section: EventSection }) => {
        const translatedTitle = getTranslatedEventTitle(item, t);
        const statusLabel = getStatusLabel(item.status, t);
        const statusBackground = getStatusColor(item.status);
        const statusTextColor = getStatusTextColor(item.status);
        const isItemArchived = item.status === 'COMPLETED' || section.key === 'archived';

        return (
            <View style={[styles.eventCard, isItemArchived && styles.eventCardArchived]}>
                <View style={styles.eventHeader}>
                    <View style={styles.eventIcon}>
                        <Ionicons name="calendar-outline" size={18} color={ACCENT} />
                    </View>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.eventTitle}>{translatedTitle}</Text>
                        <Text style={styles.eventMeta}>
                            {item.startDateTime.replace('T', ' ').slice(0, 16)} • {item.address}
                        </Text>
                    </View>
                    <View style={styles.eventActions}>
                        <View style={[styles.statusPill, { backgroundColor: statusBackground }]}>
                            <Text style={[styles.statusPillText, { color: statusTextColor }]}>
                                {statusLabel || item.status}
                            </Text>
                        </View>
                        {!isItemArchived && (
                            <Pressable
                                onPress={() =>
                                    setActionMenuFor((current) => (current === item.id ? null : item.id))
                                }
                                style={({ pressed }) => [
                                    styles.menuTrigger,
                                    pressed && styles.menuTriggerPressed,
                                ]}
                                hitSlop={8}
                                accessibilityLabel={t('admin.events.actionsMenuLabel')}
                            >
                                <Ionicons name="ellipsis-vertical" size={18} color="#5C6A80" />
                            </Pressable>
                        )}
                    </View>
                </View>
            </View>
        );
    };

    return (
        <View style={styles.container}>
            <AppHeader onMenuPress={() => setMenuVisible(true)} />

            <View style={styles.content}>
                <View style={styles.hero}>
                    <View style={styles.heroHeader}>
                        <View style={styles.heroIcon}>
                            <Ionicons name="calendar" size={22} color={ACCENT} />
                        </View>
                        <View style={{ flex: 1 }}>
                            <Text style={styles.title}>{t('admin.events.title')}</Text>
                            <Text style={styles.subtitle}>{t('admin.events.subtitle')}</Text>
                        </View>
                    </View>
                    {isAdmin && (
                        <Pressable
                            style={({ pressed }) => [
                                styles.createButton,
                                pressed && styles.createButtonPressed,
                            ]}
                            onPress={openCreate}
                        >
                            <Ionicons name="add" size={18} color="#FFFFFF" />
                            <Text style={styles.createButtonText}>{t('admin.events.createButton')}</Text>
                        </Pressable>
                    )}
                </View>

                {error ? (
                    <View style={styles.errorBox}>
                        <Ionicons name="alert-circle" size={18} color="#C62828" />
                        <Text style={styles.errorText}>{error}</Text>
                    </View>
                ) : null}

                {loading ? (
                    <View style={styles.centered}>
                        <ActivityIndicator />
                    </View>
                ) : (
                    <SectionList
                        sections={sections}
                        keyExtractor={(item) => item.id.toString()}
                        renderItem={renderEventItem}
                        renderSectionHeader={({ section }) => (
                            <View style={styles.sectionHeader}>
                                <Text style={styles.sectionTitle}>{section.title}</Text>
                                {section.hint ? (
                                    <Text style={styles.sectionHint}>{section.hint}</Text>
                                ) : null}
                            </View>
                        )}
                        renderSectionFooter={({ section }) =>
                            section.data.length === 0 ? (
                                <View style={styles.sectionEmpty}>
                                    <Text style={styles.emptyText}>{section.emptyText}</Text>
                                </View>
                            ) : null
                        }
                        contentContainerStyle={styles.listContent}
                        onScrollBeginDrag={() => setActionMenuFor(null)}
                        stickySectionHeadersEnabled={false}
                    />
                )}

            </View>

            <Modal
                transparent
                visible={actionMenuFor !== null}
                animationType="fade"
                onRequestClose={() => setActionMenuFor(null)}
            >
                <View style={styles.contextOverlay}>
                    <Pressable
                        style={StyleSheet.absoluteFill}
                        onPress={() => setActionMenuFor(null)}
                    />
                    <View style={styles.contextCard}>
                        <View style={styles.contextHeader}>
                            <Ionicons name="calendar-outline" size={16} color={ACCENT} />
                            <Text style={styles.contextTitle} numberOfLines={1}>
                                {selectedActionEvent
                                    ? getTranslatedEventTitle(selectedActionEvent, t)
                                    : t('admin.events.title')}
                            </Text>
                        </View>
                        {selectedActionEvent && isAdmin ? (
                            <Pressable
                                style={({ pressed }) => [
                                    styles.menuItem,
                                    styles.contextItem,
                                    pressed && styles.menuItemPressed,
                                ]}
                                onPress={() => handleDuplicate(selectedActionEvent)}
                                disabled={duplicatingId !== null}
                            >
                                <Ionicons name="copy-outline" size={16} color={ACCENT} />
                                <Text style={styles.menuItemText}>
                                    {t('admin.events.duplicateAction')}
                                </Text>
                                {duplicatingId === selectedActionEvent.id ? (
                                    <ActivityIndicator size="small" color={ACCENT} />
                                ) : null}
                            </Pressable>
                        ) : null}
                        <Pressable
                            style={({ pressed }) => [
                                styles.menuItem,
                                styles.contextItem,
                                pressed && styles.menuItemPressed,
                            ]}
                            onPress={() => {
                                if (selectedActionEvent) openEdit(selectedActionEvent);
                                else setActionMenuFor(null);
                            }}
                        >
                            <Ionicons name={isAdmin ? 'create-outline' : 'eye-outline'} size={16} color={ACCENT} />
                            <Text style={styles.menuItemText}>
                                {t(isAdmin ? 'admin.events.editAction' : 'admin.events.viewAction')}
                            </Text>
                        </Pressable>
                    </View>
                </View>
            </Modal>

            <Modal
                transparent
                visible={formOpen}
                animationType="slide"
                onRequestClose={closeForm}
            >
                <TouchableWithoutFeedback onPress={Keyboard.dismiss} accessible={false}>
                    <View style={styles.modalOverlay}>
                        <KeyboardAvoidingView
                            behavior={Platform.OS === 'ios' ? 'padding' : undefined}
                            keyboardVerticalOffset={Platform.OS === 'ios' ? 24 : 0}
                        >
                            <TouchableWithoutFeedback accessible={false}>
                                <View style={styles.modalCard}>
                                    <View style={styles.modalHeader}>
                                        <Ionicons
                                            name={
                                                editingEvent
                                                    ? (canEditFields ? 'create-outline' : 'eye-outline')
                                                    : 'add-circle-outline'
                                            }
                                            size={18}
                                            color={ACCENT}
                                        />
                                        <Text style={styles.modalTitle}>
                                            {editingEvent
                                                ? t(canEditFields ? 'admin.events.editTitle' : 'admin.events.viewTitle')
                                                : t('admin.events.createTitle')}
                                        </Text>
                                        <Pressable onPress={Keyboard.dismiss} hitSlop={10}>
                                            <Ionicons name="chevron-down" size={20} color="#5C6A80" />
                                        </Pressable>
                                        <Pressable onPress={closeForm} hitSlop={10}>
                                            <Ionicons name="close" size={20} color="#5C6A80" />
                                        </Pressable>
                                    </View>

                                    <ScrollView
                                        style={{ marginTop: 12 }}
                                        contentContainerStyle={styles.form}
                                        keyboardDismissMode="on-drag"
                                        keyboardShouldPersistTaps="handled"
                                        onScrollBeginDrag={Keyboard.dismiss}
                                    >
                                        <Text style={styles.fieldLabel}>{t('admin.events.fields.title')}</Text>
                                        <TextInput
                                            style={[
                                                styles.input,
                                                !canEditFields && styles.inputDisabled,
                                                fieldErrors.title && styles.inputError,
                                            ]}
                                            value={title}
                                            onChangeText={setTitle}
                                            placeholder={t('admin.events.fields.titlePlaceholder')}
                                            placeholderTextColor="#9BA5B7"
                                            editable={canEditFields}
                                        />
                                        {fieldErrors.title ? (
                                            <Text style={styles.fieldError}>{fieldErrors.title}</Text>
                                        ) : null}

                                        <Text style={styles.fieldLabel}>{t('admin.events.fields.description')}</Text>
                                        <TextInput
                                            style={[
                                                styles.textarea,
                                                !canEditFields && styles.inputDisabled,
                                                fieldErrors.description && styles.inputError,
                                            ]}
                                            value={description}
                                            onChangeText={setDescription}
                                            placeholder={t('admin.events.fields.descriptionPlaceholder')}
                                            placeholderTextColor="#9BA5B7"
                                            multiline
                                            editable={canEditFields}
                                        />
                                        {fieldErrors.description ? (
                                            <Text style={styles.fieldError}>{fieldErrors.description}</Text>
                                        ) : null}

                                        <View style={styles.row}>
                                            <View style={{ flex: 1 }}>
                                                <Text style={styles.fieldLabel}>{t('admin.events.fields.category')}</Text>
                                                <TextInput
                                                    style={[styles.input, !canEditFields && styles.inputDisabled]}
                                                    value={category}
                                                    onChangeText={setCategory}
                                                    placeholder={t('admin.events.fields.categoryPlaceholder')}
                                                    placeholderTextColor="#9BA5B7"
                                                    editable={canEditFields}
                                                />
                                            </View>
                                            <View style={{ width: 12 }} />
                                            <View style={{ flex: 1 }}>
                                                <Text style={styles.fieldLabel}>{t('admin.events.fields.maxCapacity')}</Text>
                                                <TextInput
                                                    style={[
                                                        styles.input,
                                                        !canEditFields && styles.inputDisabled,
                                                        fieldErrors.maxCapacity && styles.inputError,
                                                    ]}
                                                    value={maxCapacity}
                                                    onChangeText={setMaxCapacity}
                                                    placeholder={t('admin.events.fields.maxCapacityPlaceholder')}
                                                    placeholderTextColor="#9BA5B7"
                                                    keyboardType="numeric"
                                                    editable={canEditFields}
                                                />
                                                {fieldErrors.maxCapacity ? (
                                                    <Text style={styles.fieldError}>{fieldErrors.maxCapacity}</Text>
                                                ) : null}
                                            </View>
                                        </View>

                                        <Text style={styles.fieldLabel}>{t('admin.events.fields.locationName')}</Text>
                                        <TextInput
                                            style={[
                                                styles.input,
                                                !canEditFields && styles.inputDisabled,
                                                fieldErrors.locationName && styles.inputError,
                                            ]}
                                            value={locationName}
                                            onChangeText={setLocationName}
                                            placeholder={t('admin.events.fields.locationNamePlaceholder')}
                                            placeholderTextColor="#9BA5B7"
                                            editable={canEditFields}
                                        />
                                        {fieldErrors.locationName ? (
                                            <Text style={styles.fieldError}>{fieldErrors.locationName}</Text>
                                        ) : null}

                                        <Text style={styles.fieldLabel}>{t('admin.events.fields.address')}</Text>
                                        <TextInput
                                            style={[
                                                styles.input,
                                                !canEditFields && styles.inputDisabled,
                                                fieldErrors.address && styles.inputError,
                                            ]}
                                            value={address}
                                            onChangeText={setAddress}
                                            placeholder={t('admin.events.fields.addressPlaceholder')}
                                            placeholderTextColor="#9BA5B7"
                                            editable={canEditFields}
                                        />
                                        {fieldErrors.address ? (
                                            <Text style={styles.fieldError}>{fieldErrors.address}</Text>
                                        ) : null}

                                        <View style={styles.row}>
                                            <View style={{ flex: 1 }}>
                                                <Text style={styles.fieldLabel}>{t('admin.events.fields.startDateTime')}</Text>
                                                <Pressable
                                                    style={({ pressed }) => [
                                                        styles.pickerField,
                                                        !canEditFields && styles.pickerFieldDisabled,
                                                        fieldErrors.startDateTime && styles.inputError,
                                                        pressed && canEditFields && styles.pickerFieldPressed,
                                                    ]}
                                                    onPress={() => {
                                                        if (canEditFields) openDateTimePicker('start');
                                                    }}
                                                    disabled={!canEditFields}
                                                >
                                                    <Text
                                                        style={[
                                                            styles.pickerFieldText,
                                                            !startDateTime && styles.pickerFieldPlaceholder,
                                                        ]}
                                                    >
                                                        {startDateTime
                                                            ? formatLocalDateTimeForDisplay(startDateTime)
                                                            : t('admin.events.fields.dateTimePlaceholder')}
                                                    </Text>
                                                    <Ionicons name="calendar-outline" size={18} color={ACCENT} />
                                                </Pressable>
                                                {fieldErrors.startDateTime ? (
                                                    <Text style={styles.fieldError}>{fieldErrors.startDateTime}</Text>
                                                ) : null}
                                            </View>
                                            <View style={{ width: 12 }} />
                                            <View style={{ flex: 1 }}>
                                                <Text style={styles.fieldLabel}>{t('admin.events.fields.endDateTime')}</Text>
                                                <Pressable
                                                    style={({ pressed }) => [
                                                        styles.pickerField,
                                                        !canEditFields && styles.pickerFieldDisabled,
                                                        fieldErrors.endDateTime && styles.inputError,
                                                        pressed && canEditFields && styles.pickerFieldPressed,
                                                    ]}
                                                    onPress={() => {
                                                        if (canEditFields) openDateTimePicker('end');
                                                    }}
                                                    disabled={!canEditFields}
                                                >
                                                    <Text
                                                        style={[
                                                            styles.pickerFieldText,
                                                            !endDateTime && styles.pickerFieldPlaceholder,
                                                        ]}
                                                    >
                                                        {endDateTime
                                                            ? formatLocalDateTimeForDisplay(endDateTime)
                                                            : t('admin.events.fields.endDateTimePlaceholder')}
                                                    </Text>
                                                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                                                        {canEditFields && endDateTime ? (
                                                            <Pressable
                                                                onPress={() => setEndDateTime(null)}
                                                                hitSlop={10}
                                                            >
                                                                <Ionicons name="close-circle" size={18} color="#9BA5B7" />
                                                            </Pressable>
                                                        ) : null}
                                                        <Ionicons name="time-outline" size={18} color={ACCENT} />
                                                    </View>
                                                </Pressable>
                                                {fieldErrors.endDateTime ? (
                                                    <Text style={styles.fieldError}>{fieldErrors.endDateTime}</Text>
                                                ) : null}
                                            </View>
                                        </View>

                                        {editingEvent && !saving && (
                                            <Pressable
                                                style={({ pressed }) => [
                                                    styles.cancelEventButton,
                                                    pressed && styles.cancelEventButtonPressed,
                                                ]}
                                                onPress={() => {
                                                    const performCancel = async () => {
                                                        try {
                                                            setSaving(true);
                                                            await cancelEvent(editingEvent.id);
                                                            await loadEvents();
                                                            closeForm();
                                                            if (Platform.OS === 'web') {
                                                                window.alert(t('admin.events.cancelSuccess'));
                                                            } else {
                                                                Alert.alert(t('common.success'), t('admin.events.cancelSuccess'));
                                                            }
                                                        } catch (e) {
                                                            console.error("Failed to cancel event", e);
                                                            if (Platform.OS === 'web') {
                                                                window.alert(t('admin.events.cancelError'));
                                                            } else {
                                                                Alert.alert(t('common.error'), t('admin.events.cancelError'));
                                                            }
                                                            setSaving(false);
                                                        }
                                                    };

                                                    if (Platform.OS === 'web') {
                                                        const confirmed = window.confirm(t('admin.events.cancelConfirmMessage'));
                                                        if (confirmed) {
                                                            performCancel();
                                                        }
                                                    } else {
                                                        Alert.alert(
                                                            t('admin.events.cancelConfirmTitle'),
                                                            t('admin.events.cancelConfirmMessage'),
                                                            [
                                                                { text: t('common.no'), style: 'cancel' },
                                                                {
                                                                    text: t('common.yes'),
                                                                    style: 'destructive',
                                                                    onPress: performCancel
                                                                }
                                                            ]
                                                        );
                                                    }
                                                }}
                                            >
                                                <Ionicons name="trash-outline" size={18} color="#D32F2F" />
                                                <Text style={styles.cancelEventButtonText}>{t('admin.events.cancelButton')}</Text>
                                            </Pressable>
                                        )}
                                    </ScrollView>

                                    <View style={styles.modalActions}>
                                        <Pressable style={styles.secondaryButton} onPress={closeForm} disabled={saving}>
                                            <Text style={styles.secondaryButtonText}>{secondaryLabel}</Text>
                                        </Pressable>
                                        {editingEvent && !isArchived && canCompleteEvents ? (
                                            <Pressable
                                                style={({ pressed }) => [
                                                    styles.dangerButton,
                                                    pressed && styles.dangerButtonPressed,
                                                    completingId !== null && styles.primaryButtonDisabled,
                                                ]}
                                                onPress={() => handleMarkCompleted(editingEvent)}
                                                disabled={completingId !== null || saving}
                                            >
                                                {completingId === editingEvent.id ? (
                                                    <ActivityIndicator color="#FFFFFF" />
                                                ) : (
                                                    <Text style={styles.dangerButtonText}>
                                                        {t('admin.events.completeAction')}
                                                    </Text>
                                                )}
                                            </Pressable>
                                        ) : null}
                                        {canEditFields && (
                                            <Pressable
                                                style={({ pressed }) => [
                                                    styles.primaryButton,
                                                    pressed && styles.primaryButtonPressed,
                                                    saving && styles.primaryButtonDisabled,
                                                ]}
                                                onPress={handleSubmit}
                                                disabled={saving}
                                            >
                                                {saving ? (
                                                    <ActivityIndicator color="#FFFFFF" />
                                                ) : (
                                                    <Text style={styles.primaryButtonText}>
                                                        {editingEvent
                                                            ? t('admin.events.updateSubmit')
                                                            : t('admin.events.createSubmit')}
                                                    </Text>
                                                )}
                                            </Pressable>
                                        )}
                                    </View>
                                </View>
                            </TouchableWithoutFeedback>
                        </KeyboardAvoidingView>
                        <DateTimePickerModal
                            visible={pickerVisible}
                            title={
                                activePickerField === 'start'
                                    ? t('admin.events.fields.startDateTime')
                                    : t('admin.events.fields.endDateTime')
                            }
                            initialValue={pickerInitialValue}
                            accentColor={ACCENT}
                            cancelLabel={t('common.cancel')}
                            confirmLabel={t('common.confirm')}
                            timeLabel={t('events.fields.time')}
                            onCancel={closePicker}
                            onConfirm={confirmPicker}
                        />
                    </View>
                </TouchableWithoutFeedback>
            </Modal>

            <NavigationMenu
                visible={menuVisible}
                onClose={() => setMenuVisible(false)}
                onNavigateHome={handleNavigateHome}
                onNavigateEvents={handleNavigateEvents}
                onNavigateProfile={handleNavigateProfile}
                onNavigateDashboard={handleNavigateDashboard}
                showDashboard={hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])}
                t={t}
            />
        </View >
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: SURFACE,
    },
    content: {
        flex: 1,
        paddingHorizontal: 18,
        paddingVertical: 18,
    },
    hero: {
        backgroundColor: '#FFFFFF',
        padding: 16,
        borderRadius: 14,
        alignItems: 'flex-start',
        gap: 12,
        shadowColor: '#000',
        shadowOpacity: 0.06,
        shadowRadius: 10,
        shadowOffset: { width: 0, height: 4 },
        elevation: 4,
    },
    heroHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        width: '100%',
    },
    heroIcon: {
        width: 46,
        height: 46,
        borderRadius: 14,
        backgroundColor: '#EAF1FF',
        alignItems: 'center',
        justifyContent: 'center',
    },
    title: {
        fontSize: 20,
        fontWeight: '700',
        color: '#0F2848',
    },
    subtitle: {
        color: '#5C6A80',
        marginTop: 4,
        fontSize: 14,
        flexShrink: 1,
    },
    createButton: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: ACCENT,
        paddingHorizontal: 12,
        paddingVertical: 10,
        borderRadius: 12,
        gap: 6,
    },
    createButtonPressed: {
        opacity: 0.92,
    },
    createButtonText: {
        color: '#FFFFFF',
        fontWeight: '700',
        fontSize: 13,
    },
    errorBox: {
        marginTop: 12,
        backgroundColor: '#FDECEA',
        borderColor: '#F5C6C6',
        borderWidth: StyleSheet.hairlineWidth,
        borderRadius: 12,
        padding: 12,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
    },
    errorText: {
        color: '#8A1F1F',
        flex: 1,
        fontSize: 13,
        fontWeight: '600',
    },
    centered: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    listContent: {
        paddingTop: 14,
        paddingBottom: 18,
        gap: 12,
    },
    sectionHeader: {
        marginTop: 8,
        marginBottom: 4,
        gap: 4,
    },
    sectionTitle: {
        fontSize: 14,
        fontWeight: '700',
        color: '#1B2F4A',
    },
    sectionHint: {
        fontSize: 12,
        color: '#6F7B91',
    },
    sectionEmpty: {
        paddingVertical: 8,
    },
    emptyState: {
        alignItems: 'center',
        paddingVertical: 36,
        gap: 8,
    },
    emptyText: {
        color: '#5C6A80',
        fontSize: 14,
        fontWeight: '600',
    },
    eventCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: 14,
        padding: 14,
        position: 'relative',
        shadowColor: '#000',
        shadowOpacity: 0.05,
        shadowRadius: 8,
        shadowOffset: { width: 0, height: 3 },
        elevation: 3,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E7F3',
    },
    eventCardArchived: {
        backgroundColor: '#F7F9FC',
        borderColor: '#E3E8F2',
    },
    eventHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    eventIcon: {
        width: 38,
        height: 38,
        borderRadius: 12,
        backgroundColor: '#F3F7FF',
        alignItems: 'center',
        justifyContent: 'center',
    },
    eventTitle: {
        fontSize: 15,
        fontWeight: '700',
        color: '#0F2848',
    },
    eventMeta: {
        marginTop: 3,
        fontSize: 12,
        color: '#5C6A80',
    },
    eventActions: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
    },
    statusPill: {
        paddingHorizontal: 10,
        paddingVertical: 6,
        backgroundColor: '#EAF1FF',
        borderRadius: 999,
    },
    statusPillText: {
        color: ACCENT,
        fontSize: 12,
        fontWeight: '800',
    },
    menuTrigger: {
        padding: 6,
        borderRadius: 10,
    },
    menuTriggerPressed: {
        backgroundColor: '#EEF3FF',
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
        paddingHorizontal: 12,
        paddingVertical: 10,
    },
    menuItemPressed: {
        backgroundColor: '#F2F6FF',
    },
    menuItemText: {
        color: '#0F2848',
        fontSize: 14,
        fontWeight: '700',
        flex: 1,
    },
    contextOverlay: {
        flex: 1,
        backgroundColor: 'rgba(15, 28, 48, 0.2)',
        justifyContent: 'flex-end',
        padding: 12,
    },
    contextCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: 14,
        paddingVertical: 10,
        paddingHorizontal: 12,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E7F3',
        shadowColor: '#000',
        shadowOpacity: 0.08,
        shadowRadius: 12,
        shadowOffset: { width: 0, height: 5 },
        elevation: 8,
        gap: 6,
    },
    contextHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
        paddingHorizontal: 4,
        paddingBottom: 4,
    },
    contextTitle: {
        flex: 1,
        fontSize: 13,
        fontWeight: '700',
        color: '#0F2848',
    },
    contextItem: {
        borderRadius: 10,
    },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(15, 28, 48, 0.5)',
        justifyContent: 'flex-end',
        position: 'relative',
    },
    modalCard: {
        backgroundColor: '#FFFFFF',
        borderTopLeftRadius: 18,
        borderTopRightRadius: 18,
        padding: 16,
        maxHeight: '92%',
    },
    modalHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
    },
    modalTitle: {
        flex: 1,
        fontSize: 16,
        fontWeight: '800',
        color: '#0F2848',
    },
    form: {
        marginTop: 12,
        gap: 10,
    },
    fieldLabel: {
        fontSize: 12,
        fontWeight: '800',
        color: '#1B2F4A',
        marginTop: 6,
    },
    input: {
        backgroundColor: '#F7F9FD',
        borderColor: '#DCE4F2',
        borderWidth: 1,
        borderRadius: 12,
        paddingHorizontal: 12,
        paddingVertical: 10,
        color: '#0F2848',
        fontSize: 14,
    },
    inputDisabled: {
        backgroundColor: '#EEF2F7',
        color: '#7B8798',
    },
    textarea: {
        backgroundColor: '#F7F9FD',
        borderColor: '#DCE4F2',
        borderWidth: 1,
        borderRadius: 12,
        paddingHorizontal: 12,
        paddingVertical: 10,
        color: '#0F2848',
        fontSize: 14,
        minHeight: 90,
        textAlignVertical: 'top',
    },
    inputError: {
        borderColor: '#C62828',
    },
    fieldError: {
        color: '#8A1F1F',
        fontSize: 12,
        fontWeight: '600',
        marginTop: 4,
    },
    pickerField: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: '#F7F9FD',
        borderColor: '#DCE4F2',
        borderWidth: 1,
        borderRadius: 12,
        paddingHorizontal: 12,
        paddingVertical: 12,
        gap: 10,
    },
    pickerFieldDisabled: {
        backgroundColor: '#EEF2F7',
        borderColor: '#E1E6EF',
    },
    pickerFieldPressed: {
        opacity: 0.92,
    },
    pickerFieldText: {
        flex: 1,
        color: '#0F2848',
        fontSize: 14,
        fontWeight: '700',
    },
    pickerFieldPlaceholder: {
        color: '#9BA5B7',
        fontWeight: '600',
    },
    row: {
        flexDirection: 'row',
        alignItems: 'flex-start',
    },
    modalActions: {
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
    secondaryButtonText: {
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
        backgroundColor: ACCENT,
    },
    primaryButtonPressed: {
        opacity: 0.92,
    },
    primaryButtonDisabled: {
        opacity: 0.7,
    },
    primaryButtonText: {
        color: '#FFFFFF',
        fontWeight: '800',
        fontSize: 14,
    },
    cancelEventButton: {
        marginTop: 24,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 12,
        paddingHorizontal: 16,
        backgroundColor: '#FFEBEE',
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#FFCDD2',
        gap: 8,
    },
    cancelEventButtonPressed: {
        backgroundColor: '#FFCDD2',
    },
    cancelEventButtonText: {
        color: '#D32F2F',
        fontWeight: '700',
        fontSize: 14,
    },
    dangerButton: {
        flex: 1,
        borderRadius: 12,
        paddingVertical: 12,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#C62828',
    },
    dangerButtonPressed: {
        opacity: 0.92,
    },
    dangerButtonText: {
        color: '#FFFFFF',
        fontWeight: '800',
        fontSize: 14,
        textAlign: 'center',
        width: '100%',
    },
});
