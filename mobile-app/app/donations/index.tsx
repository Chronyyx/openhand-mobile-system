import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    Modal,
    Pressable,
    ScrollView,
    StyleSheet,
    TextInput,
    View,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { useRouter } from 'expo-router';

import { MenuLayout } from '../../components/menu-layout';
import { ThemedText } from '../../components/themed-text';
import { ThemedView } from '../../components/themed-view';
import { useAuth } from '../../context/AuthContext';
import {
    getDonationOptions,
    submitDonation,
    type DonationOptions,
    type DonationFrequency,
    type DonationResponse,
} from '../../services/donations.service';
import { useColorScheme } from '../../hooks/use-color-scheme';

const formatCurrency = (amount: number, currency: string) => {
    if (Number.isNaN(amount)) return '';
    return `${currency} ${amount.toFixed(2)}`;
};

const normalizeAmountInput = (value: string) => value.replace(',', '.').trim();

export default function DonationsScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const { user, hasRole } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const styles = getStyles(colorScheme);

    const isMember = hasRole(['ROLE_MEMBER']);
    const [options, setOptions] = useState<DonationOptions | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [amountInput, setAmountInput] = useState('');
    const [selectedPreset, setSelectedPreset] = useState<number | null>(null);
    const [frequency, setFrequency] = useState<DonationFrequency>('ONE_TIME');
    const [submitting, setSubmitting] = useState(false);
    const [confirmation, setConfirmation] = useState<DonationResponse | null>(null);
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [modalError, setModalError] = useState<string | null>(null);

    const [billingEmail, setBillingEmail] = useState('');
    const [billingPhone, setBillingPhone] = useState('');
    const [billingCountry, setBillingCountry] = useState('');
    const [billingAddress, setBillingAddress] = useState('');
    const [billingCity, setBillingCity] = useState('');
    const [billingProvince, setBillingProvince] = useState('');
    const [billingPostal, setBillingPostal] = useState('');
    const [billingComment, setBillingComment] = useState('');

    const [cardName, setCardName] = useState('');
    const [cardNumber, setCardNumber] = useState('');
    const [cardExpiry, setCardExpiry] = useState('');
    const [cardCsc, setCardCsc] = useState('');

    const loadOptions = useCallback(async () => {
        if (!user || !isMember) {
            setLoading(false);
            return;
        }

        try {
            setError(null);
            setLoading(true);
            const data = await getDonationOptions();
            setOptions(data);
            if (data.frequencies.length > 0) {
                setFrequency(data.frequencies[0]);
            }
        } catch (err) {
            console.error('Failed to load donation options', err);
            setError(t('donations.errors.loadFailed'));
        } finally {
            setLoading(false);
        }
    }, [isMember, t, user]);

    useEffect(() => {
        loadOptions();
    }, [loadOptions]);

    useEffect(() => {
        if (!user) return;
        if (!billingEmail) {
            setBillingEmail(user.email || '');
        }
        if (!billingPhone) {
            setBillingPhone(user.phoneNumber || '');
        }
        if (!cardName) {
            setCardName(user.name || '');
        }
    }, [billingEmail, billingPhone, cardName, user]);

    const presetLabel = useMemo(() => {
        if (!options || selectedPreset == null) return '';
        return formatCurrency(selectedPreset, options.currency);
    }, [options, selectedPreset]);

    const handleSelectPreset = (amount: number) => {
        setSelectedPreset(amount);
        setAmountInput(amount.toString());
    };

    const handleAmountChange = (value: string) => {
        setAmountInput(value);
        if (selectedPreset != null && value !== selectedPreset.toString()) {
            setSelectedPreset(null);
        }
    };

    const validateBilling = () => {
        if (!billingEmail.trim()) return t('donations.validation.emailRequired');
        if (!billingPhone.trim()) return t('donations.validation.phoneRequired');
        if (!billingCountry.trim()) return t('donations.validation.countryRequired');
        if (!billingAddress.trim()) return t('donations.validation.addressRequired');
        if (!billingCity.trim()) return t('donations.validation.cityRequired');
        if (!billingProvince.trim()) return t('donations.validation.provinceRequired');
        if (!billingPostal.trim()) return t('donations.validation.postalRequired');
        return null;
    };

    const validatePayment = () => {
        if (!cardName.trim()) return t('donations.validation.cardNameRequired');
        if (!cardNumber.trim()) return t('donations.validation.cardNumberRequired');
        if (!cardExpiry.trim()) return t('donations.validation.cardExpiryRequired');
        if (!cardCsc.trim()) return t('donations.validation.cardCscRequired');
        return null;
    };

    const handleSubmit = async () => {
        if (!options) {
            return;
        }

        const normalized = normalizeAmountInput(amountInput);
        const amount = Number.parseFloat(normalized);
        if (!normalized || Number.isNaN(amount)) {
            setError(t('donations.validation.amountRequired'));
            return;
        }
        if (amount < options.minimumAmount) {
            setError(t('donations.validation.amountMin', { min: options.minimumAmount.toFixed(2) }));
            return;
        }

        const billingError = validateBilling();
        if (billingError) {
            setError(billingError);
            return;
        }

        setError(null);
        setModalError(null);
        setShowPaymentModal(true);
    };

    const handleConfirmPayment = async () => {
        if (!options) return;

        const paymentError = validatePayment();
        if (paymentError) {
            setModalError(paymentError);
            return;
        }

        try {
            setModalError(null);
            setSubmitting(true);
            const normalized = normalizeAmountInput(amountInput);
            const amount = Number.parseFloat(normalized);
            const response = await submitDonation({
                amount,
                currency: options.currency,
                frequency,
            });
            setConfirmation(response);
            setAmountInput('');
            setSelectedPreset(null);
            setShowPaymentModal(false);
        } catch (err) {
            console.error('Failed to submit donation', err);
            setModalError(t('donations.errors.submitFailed'));
        } finally {
            setSubmitting(false);
        }
    };

    const summaryLine = `${billingEmail} ${billingAddress} ${billingCity} ${billingProvince} ${billingPostal}`.trim();

    if (!user) {
        return (
            <MenuLayout>
                <ThemedView style={styles.centered}>
                    <ThemedText style={styles.messageText}>{t('common.notAuthenticated')}</ThemedText>
                    <Pressable
                        style={styles.primaryButton}
                        onPress={() => router.push('/auth/login')}
                        accessibilityRole="button"
                        accessibilityLabel={t('home.loginRegister')}
                    >
                        <ThemedText style={styles.primaryButtonText}>{t('home.loginRegister')}</ThemedText>
                    </Pressable>
                </ThemedView>
            </MenuLayout>
        );
    }

    if (!isMember) {
        return (
            <MenuLayout>
                <ThemedView style={styles.centered}>
                    <ThemedText style={styles.messageText}>{t('donations.memberOnly')}</ThemedText>
                </ThemedView>
            </MenuLayout>
        );
    }

    return (
        <MenuLayout>
            <ScrollView contentContainerStyle={styles.container}>
                <ThemedText type="title" style={styles.title} accessibilityRole="header">
                    {t('donations.title')}
                </ThemedText>
                <ThemedText style={styles.subtitle}>{t('donations.subtitle')}</ThemedText>

                {loading ? (
                    <View style={styles.centered}>
                        <ActivityIndicator />
                        <ThemedText style={styles.loadingText}>{t('common.loading')}</ThemedText>
                    </View>
                ) : (
                    <>
                        {error && <ThemedText style={styles.errorText}>{error}</ThemedText>}

                        {confirmation && (
                            <View style={styles.successCard}>
                                <ThemedText type="subtitle" style={styles.successTitle}>
                                    {t('donations.successTitle')}
                                </ThemedText>
                                <ThemedText style={styles.successMessage}>
                                    {confirmation.message || t('donations.successMessage')}
                                </ThemedText>
                                <ThemedText style={styles.successMeta}>
                                    {formatCurrency(confirmation.amount, confirmation.currency)}
                                </ThemedText>
                            </View>
                        )}

                        {options && (
                            <View style={styles.formCard}>
                                <ThemedText type="subtitle" style={styles.sectionTitle}>
                                    {t('donations.amountLabel')}
                                </ThemedText>
                                <View style={styles.presetRow}>
                                    {options.presetAmounts.map((amount) => (
                                        <Pressable
                                            key={amount}
                                            style={({ pressed }) => [
                                                styles.presetButton,
                                                selectedPreset === amount && styles.presetButtonActive,
                                                pressed && styles.presetButtonPressed,
                                            ]}
                                            onPress={() => handleSelectPreset(amount)}
                                            accessibilityRole="button"
                                            accessibilityLabel={formatCurrency(amount, options.currency)}
                                        >
                                            <ThemedText
                                                style={
                                                    selectedPreset === amount
                                                        ? styles.presetButtonTextActive
                                                        : styles.presetButtonText
                                                }
                                            >
                                                {formatCurrency(amount, options.currency)}
                                            </ThemedText>
                                        </Pressable>
                                    ))}
                                </View>

                                <View style={styles.inputGroup}>
                                    <ThemedText style={styles.inputLabel}>
                                        {t('donations.customAmountLabel')}
                                    </ThemedText>
                                    <TextInput
                                        value={amountInput}
                                        onChangeText={handleAmountChange}
                                        keyboardType="decimal-pad"
                                        placeholder={t('donations.amountPlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                        accessibilityLabel={t('donations.amountLabel')}
                                    />
                                    {presetLabel ? (
                                        <ThemedText style={styles.presetHint}>
                                            {t('donations.selectedPreset', { amount: presetLabel })}
                                        </ThemedText>
                                    ) : null}
                                </View>

                                <ThemedText type="subtitle" style={styles.sectionTitle}>
                                    {t('donations.billingTitle')}
                                </ThemedText>
                                <View style={styles.inputGroup}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.emailLabel')}</ThemedText>
                                    <TextInput
                                        value={billingEmail}
                                        onChangeText={setBillingEmail}
                                        keyboardType="email-address"
                                        autoCapitalize="none"
                                        placeholder={t('donations.emailPlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                    />
                                </View>
                                <View style={styles.inputGroup}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.phoneLabel')}</ThemedText>
                                    <TextInput
                                        value={billingPhone}
                                        onChangeText={setBillingPhone}
                                        keyboardType="phone-pad"
                                        placeholder={t('donations.phonePlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                    />
                                </View>
                                <View style={styles.inputGroup}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.countryLabel')}</ThemedText>
                                    <TextInput
                                        value={billingCountry}
                                        onChangeText={setBillingCountry}
                                        placeholder={t('donations.countryPlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                    />
                                </View>
                                <View style={styles.inputGroup}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.addressLabel')}</ThemedText>
                                    <TextInput
                                        value={billingAddress}
                                        onChangeText={setBillingAddress}
                                        placeholder={t('donations.addressPlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                    />
                                </View>
                                <View style={styles.inputGroup}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.cityLabel')}</ThemedText>
                                    <TextInput
                                        value={billingCity}
                                        onChangeText={setBillingCity}
                                        placeholder={t('donations.cityPlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                    />
                                </View>
                                <View style={styles.splitRow}>
                                    <View style={[styles.inputGroup, styles.splitColumn]}>
                                        <ThemedText style={styles.inputLabel}>{t('donations.provinceLabel')}</ThemedText>
                                        <TextInput
                                            value={billingProvince}
                                            onChangeText={setBillingProvince}
                                            placeholder={t('donations.provincePlaceholder')}
                                            placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                            style={styles.amountInput}
                                        />
                                    </View>
                                    <View style={[styles.inputGroup, styles.splitColumn]}>
                                        <ThemedText style={styles.inputLabel}>{t('donations.postalLabel')}</ThemedText>
                                        <TextInput
                                            value={billingPostal}
                                            onChangeText={setBillingPostal}
                                            placeholder={t('donations.postalPlaceholder')}
                                            placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                            style={styles.amountInput}
                                        />
                                    </View>
                                </View>
                                <View style={styles.inputGroup}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.commentLabel')}</ThemedText>
                                    <TextInput
                                        value={billingComment}
                                        onChangeText={setBillingComment}
                                        placeholder={t('donations.commentPlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={[styles.amountInput, styles.commentInput]}
                                        multiline
                                    />
                                </View>

                                <ThemedText type="subtitle" style={styles.sectionTitle}>
                                    {t('donations.frequencyLabel')}
                                </ThemedText>
                                <View style={styles.frequencyRow}>
                                    {options.frequencies.map((value) => (
                                        <Pressable
                                            key={value}
                                            style={({ pressed }) => [
                                                styles.frequencyButton,
                                                frequency === value && styles.frequencyButtonActive,
                                                pressed && styles.frequencyButtonPressed,
                                            ]}
                                            onPress={() => setFrequency(value)}
                                            accessibilityRole="button"
                                            accessibilityLabel={t(`donations.frequency.${value === 'ONE_TIME' ? 'oneTime' : 'monthly'}`)}
                                        >
                                            <ThemedText
                                                style={
                                                    frequency === value
                                                        ? styles.frequencyButtonTextActive
                                                        : styles.frequencyButtonText
                                                }
                                            >
                                                {t(`donations.frequency.${value === 'ONE_TIME' ? 'oneTime' : 'monthly'}`)}
                                            </ThemedText>
                                        </Pressable>
                                    ))}
                                </View>

                                <Pressable
                                    style={({ pressed }) => [
                                        styles.primaryButton,
                                        pressed && styles.primaryButtonPressed,
                                        submitting && styles.primaryButtonDisabled,
                                    ]}
                                    onPress={handleSubmit}
                                    disabled={submitting}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('donations.submit')}
                                >
                                    {submitting ? (
                                        <ActivityIndicator color="#FFFFFF" />
                                    ) : (
                                        <ThemedText style={styles.primaryButtonText}>
                                            {t('donations.submit')}
                                        </ThemedText>
                                    )}
                                </Pressable>
                            </View>
                        )}
                    </>
                )}
            </ScrollView>

            <Modal visible={showPaymentModal} transparent animationType="slide" onRequestClose={() => setShowPaymentModal(false)}>
                <View style={styles.modalOverlay}>
                    <View style={styles.modalCard}>
                        <ScrollView contentContainerStyle={styles.modalContent}>
                            <ThemedText type="subtitle" style={styles.modalTitle}>
                                {t('donations.paymentTitle')}
                            </ThemedText>
                            {options ? (
                                <ThemedText style={styles.modalAmount}>
                                    {formatCurrency(Number.parseFloat(normalizeAmountInput(amountInput)) || 0, options.currency)}
                                </ThemedText>
                            ) : null}

                            <View style={styles.inputGroup}>
                            <ThemedText style={styles.inputLabel}>{t('donations.emailLabel')}</ThemedText>
                            <TextInput
                                value={billingEmail}
                                onChangeText={setBillingEmail}
                                keyboardType="email-address"
                                autoCapitalize="none"
                                placeholder={t('donations.emailPlaceholder')}
                                placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                style={styles.amountInput}
                            />
                        </View>

                            <View style={styles.inputGroup}>
                            <ThemedText style={styles.inputLabel}>{t('donations.countryLabel')}</ThemedText>
                            <TextInput
                                value={billingCountry}
                                onChangeText={setBillingCountry}
                                placeholder={t('donations.countryPlaceholder')}
                                placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                style={styles.amountInput}
                            />
                        </View>

                            <View style={styles.inputGroup}>
                            <ThemedText style={styles.inputLabel}>{t('donations.addressLabel')}</ThemedText>
                            <TextInput
                                value={billingAddress}
                                onChangeText={setBillingAddress}
                                placeholder={t('donations.addressPlaceholder')}
                                placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                style={styles.amountInput}
                            />
                        </View>

                            <View style={styles.splitRow}>
                                <View style={[styles.inputGroup, styles.splitColumn]}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.cityLabel')}</ThemedText>
                                    <TextInput
                                        value={billingCity}
                                        onChangeText={setBillingCity}
                                        placeholder={t('donations.cityPlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                    />
                                </View>
                                <View style={[styles.inputGroup, styles.splitColumn]}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.provinceLabel')}</ThemedText>
                                    <TextInput
                                        value={billingProvince}
                                        onChangeText={setBillingProvince}
                                        placeholder={t('donations.provincePlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                    />
                                </View>
                            </View>

                            <View style={styles.inputGroup}>
                                <ThemedText style={styles.inputLabel}>{t('donations.postalLabel')}</ThemedText>
                                <TextInput
                                    value={billingPostal}
                                    onChangeText={setBillingPostal}
                                    placeholder={t('donations.postalPlaceholder')}
                                    placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                    style={styles.amountInput}
                                />
                            </View>

                            <ThemedText style={styles.summaryLine}>{summaryLine}</ThemedText>

                            <View style={styles.inputGroup}>
                                <ThemedText style={styles.inputLabel}>{t('donations.cardNameLabel')}</ThemedText>
                                <TextInput
                                    value={cardName}
                                    onChangeText={setCardName}
                                    placeholder={t('donations.cardNamePlaceholder')}
                                    placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                    style={styles.amountInput}
                                />
                            </View>
                            <View style={styles.inputGroup}>
                                <ThemedText style={styles.inputLabel}>{t('donations.cardNumberLabel')}</ThemedText>
                                <TextInput
                                    value={cardNumber}
                                    onChangeText={setCardNumber}
                                    keyboardType="number-pad"
                                    placeholder={t('donations.cardNumberPlaceholder')}
                                    placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                    style={styles.amountInput}
                                />
                            </View>
                            <View style={styles.splitRow}>
                                <View style={[styles.inputGroup, styles.splitColumn]}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.cardExpiryLabel')}</ThemedText>
                                    <TextInput
                                        value={cardExpiry}
                                        onChangeText={setCardExpiry}
                                        placeholder={t('donations.cardExpiryPlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                    />
                                </View>
                                <View style={[styles.inputGroup, styles.splitColumn]}>
                                    <ThemedText style={styles.inputLabel}>{t('donations.cardCscLabel')}</ThemedText>
                                    <TextInput
                                        value={cardCsc}
                                        onChangeText={setCardCsc}
                                        keyboardType="number-pad"
                                        placeholder={t('donations.cardCscPlaceholder')}
                                        placeholderTextColor={colorScheme === 'dark' ? '#8B93A1' : '#999'}
                                        style={styles.amountInput}
                                    />
                                </View>
                            </View>

                            {modalError && <ThemedText style={styles.errorText}>{modalError}</ThemedText>}

                            <View style={styles.modalActions}>
                                <Pressable
                                    style={[styles.secondaryButton, styles.modalButton]}
                                    onPress={() => setShowPaymentModal(false)}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('common.cancel')}
                                >
                                    <ThemedText style={styles.secondaryButtonText}>{t('common.cancel')}</ThemedText>
                                </Pressable>
                                <Pressable
                                    style={[styles.primaryButton, styles.modalButton, submitting && styles.primaryButtonDisabled]}
                                    onPress={handleConfirmPayment}
                                    disabled={submitting}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('donations.completePayment')}
                                >
                                    {submitting ? (
                                        <ActivityIndicator color="#FFFFFF" />
                                    ) : (
                                        <ThemedText style={styles.primaryButtonText}>{t('donations.completePayment')}</ThemedText>
                                    )}
                                </Pressable>
                            </View>
                        </ScrollView>
                    </View>
                </View>
            </Modal>
        </MenuLayout>
    );
}

const getStyles = (scheme: 'light' | 'dark') => {
    const isDark = scheme === 'dark';
    const colors = {
        background: isDark ? '#0F1419' : '#F5F7FB',
        surface: isDark ? '#1F2328' : '#FFFFFF',
        border: isDark ? '#2F3A4A' : '#E0E4EC',
        text: isDark ? '#ECEDEE' : '#333333',
        textMuted: isDark ? '#A0A7B1' : '#666666',
        primary: isDark ? '#6AA9FF' : '#0056A8',
        danger: isDark ? '#FFB4AB' : '#D32F2F',
        success: isDark ? '#9FC3FF' : '#0056A8',
    };

    return StyleSheet.create({
        container: {
            padding: 20,
            backgroundColor: colors.background,
        },
        title: {
            color: colors.primary,
            marginBottom: 8,
            fontSize: 26,
        },
        subtitle: {
            color: colors.textMuted,
            marginBottom: 20,
        },
        formCard: {
            backgroundColor: colors.surface,
            borderRadius: 16,
            padding: 16,
            borderWidth: 1,
            borderColor: colors.border,
            gap: 12,
        },
        sectionTitle: {
            color: colors.primary,
        },
        presetRow: {
            flexDirection: 'row',
            flexWrap: 'wrap',
            gap: 12,
        },
        presetButton: {
            paddingVertical: 8,
            paddingHorizontal: 12,
            borderRadius: 10,
            borderWidth: 1,
            borderColor: colors.border,
            backgroundColor: colors.surface,
        },
        presetButtonActive: {
            borderColor: colors.primary,
            backgroundColor: isDark ? '#142033' : '#E7F0FF',
        },
        presetButtonPressed: {
            opacity: 0.8,
        },
        presetButtonText: {
            color: colors.text,
            fontWeight: '600',
        },
        presetButtonTextActive: {
            color: colors.primary,
            fontWeight: '700',
        },
        inputGroup: {
            gap: 8,
        },
        splitRow: {
            flexDirection: 'row',
            gap: 12,
        },
        splitColumn: {
            flex: 1,
        },
        inputLabel: {
            color: colors.textMuted,
            fontSize: 13,
            fontWeight: '600',
        },
        amountInput: {
            borderWidth: 1,
            borderColor: colors.border,
            borderRadius: 10,
            paddingHorizontal: 12,
            paddingVertical: 10,
            fontSize: 16,
            color: colors.text,
            backgroundColor: colors.surface,
        },
        commentInput: {
            minHeight: 80,
            textAlignVertical: 'top',
        },
        presetHint: {
            color: colors.textMuted,
            fontSize: 12,
        },
        frequencyRow: {
            flexDirection: 'row',
            gap: 12,
        },
        frequencyButton: {
            flex: 1,
            paddingVertical: 10,
            borderRadius: 10,
            borderWidth: 1,
            borderColor: colors.border,
            alignItems: 'center',
            backgroundColor: colors.surface,
        },
        frequencyButtonActive: {
            borderColor: colors.primary,
            backgroundColor: isDark ? '#142033' : '#E7F0FF',
        },
        frequencyButtonPressed: {
            opacity: 0.85,
        },
        frequencyButtonText: {
            color: colors.text,
            fontWeight: '600',
        },
        frequencyButtonTextActive: {
            color: colors.primary,
            fontWeight: '700',
        },
        primaryButton: {
            marginTop: 12,
            backgroundColor: colors.primary,
            borderRadius: 12,
            paddingVertical: 14,
            alignItems: 'center',
        },
        primaryButtonPressed: {
            opacity: 0.9,
        },
        primaryButtonDisabled: {
            opacity: 0.6,
        },
        primaryButtonText: {
            color: '#FFFFFF',
            fontWeight: '700',
        },
        secondaryButton: {
            backgroundColor: colors.surface,
            borderRadius: 12,
            paddingVertical: 14,
            alignItems: 'center',
            borderWidth: 1,
            borderColor: colors.border,
        },
        secondaryButtonText: {
            color: colors.text,
            fontWeight: '600',
        },
        centered: {
            flex: 1,
            alignItems: 'center',
            justifyContent: 'center',
            padding: 24,
            backgroundColor: colors.background,
        },
        loadingText: {
            marginTop: 10,
            color: colors.textMuted,
        },
        errorText: {
            color: colors.danger,
            marginBottom: 12,
        },
        messageText: {
            color: colors.textMuted,
            textAlign: 'center',
            marginBottom: 16,
        },
        successCard: {
            backgroundColor: isDark ? '#142033' : '#E7F0FF',
            borderRadius: 14,
            padding: 16,
            borderWidth: 1,
            borderColor: colors.primary,
            marginBottom: 16,
        },
        successTitle: {
            color: colors.primary,
            marginBottom: 6,
        },
        successMessage: {
            color: colors.text,
            marginBottom: 8,
        },
        successMeta: {
            color: colors.textMuted,
            fontWeight: '600',
        },
        modalOverlay: {
            flex: 1,
            backgroundColor: 'rgba(0,0,0,0.6)',
            justifyContent: 'center',
            padding: 20,
        },
        modalCard: {
            backgroundColor: colors.surface,
            borderRadius: 16,
            padding: 16,
            borderWidth: 1,
            borderColor: colors.border,
            gap: 12,
            maxHeight: '90%',
        },
        modalContent: {
            gap: 12,
            paddingBottom: 4,
        },
        modalTitle: {
            color: colors.primary,
        },
        modalAmount: {
            color: colors.text,
            fontSize: 18,
            fontWeight: '700',
        },
        summaryLine: {
            color: colors.textMuted,
            fontSize: 12,
        },
        modalActions: {
            flexDirection: 'row',
            gap: 12,
            marginTop: 8,
        },
        modalButton: {
            flex: 1,
        },
    });
};
