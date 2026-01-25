import React, { useState } from 'react';
import { View, Text, StyleSheet, Pressable, ActivityIndicator, TextInput, Alert } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useRouter } from 'expo-router';
import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { deactivateAccount } from '../../services/account.service';

const ACCENT = '#0056A8';
const DANGER = '#C62828';
const SURFACE = '#F5F7FB';

export default function DeactivateAccountScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const { signOut } = useAuth();
    const [menuVisible, setMenuVisible] = useState(false);
    const [confirmText, setConfirmText] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleDeactivate = async () => {
        if (confirmText.trim().toLowerCase() !== 'deactivate') {
            setError(t('settings.account.confirmationError'));
            return;
        }
        setError(null);
        setLoading(true);
        try {
            await deactivateAccount();
            Alert.alert(
                t('settings.account.successTitle'),
                t('settings.account.successMessage'),
                [
                    {
                        text: t('common.confirm'),
                        onPress: async () => {
                            await signOut();
                            router.replace('/');
                        }
                    }
                ]
            );
        } catch (e: any) {
            const message = e?.response?.data?.message || t('settings.account.error');
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <View style={styles.container}>
            <AppHeader onMenuPress={() => setMenuVisible(true)} />
            <View style={styles.content}>
                <Text style={styles.title}>{t('settings.account.title')}</Text>
                <Text style={styles.subtitle}>{t('settings.account.subtitle')}</Text>

                <View style={styles.warningBox}>
                    <Text style={styles.warningTitle}>{t('settings.account.warningTitle')}</Text>
                    <Text style={styles.warningText}>{t('settings.account.warningBody')}</Text>
                </View>

                <Text style={styles.label}>{t('settings.account.confirmationLabel')}</Text>
                <TextInput
                    style={styles.input}
                    placeholder={t('settings.account.confirmationPlaceholder')}
                    value={confirmText}
                    onChangeText={setConfirmText}
                    autoCapitalize="none"
                />

                {error && (
                    <View style={styles.errorBox}>
                        <Text style={styles.errorText}>{error}</Text>
                    </View>
                )}

                <Pressable
                    style={[styles.deactivateButton, loading && { opacity: 0.7 }]}
                    onPress={handleDeactivate}
                    disabled={loading}
                >
                    {loading ? (
                        <ActivityIndicator color="#FFFFFF" />
                    ) : (
                        <Text style={styles.deactivateButtonText}>{t('settings.account.deactivateButton')}</Text>
                    )}
                </Pressable>

                <Pressable style={styles.cancelButton} onPress={() => router.back()}>
                    <Text style={styles.cancelButtonText}>{t('common.cancel')}</Text>
                </Pressable>
            </View>

            <NavigationMenu
                visible={menuVisible}
                onClose={() => setMenuVisible(false)}
                onNavigateHome={() => { setMenuVisible(false); router.replace('/'); }}
                onNavigateEvents={() => { setMenuVisible(false); router.push('/events'); }}
                onNavigateAttendance={() => { setMenuVisible(false); router.push('/admin/attendance'); }}
                onNavigateProfile={() => setMenuVisible(false)}
                onNavigateMyRegistrations={() => { setMenuVisible(false); router.push('/registrations'); }}
                showMyRegistrations
                showAttendance={false}
                showDashboard={false}
                onNavigateDashboard={() => { setMenuVisible(false); router.push('/admin'); }}
                t={t}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: SURFACE,
    },
    content: {
        flex: 1,
        padding: 16,
        gap: 12,
    },
    title: {
        fontSize: 22,
        fontWeight: '700',
        color: '#0F172A',
    },
    subtitle: {
        fontSize: 14,
        color: '#334155',
    },
    warningBox: {
        backgroundColor: '#FEF2F2',
        borderLeftColor: DANGER,
        borderLeftWidth: 4,
        padding: 12,
        borderRadius: 8,
    },
    warningTitle: {
        fontWeight: '700',
        color: DANGER,
        marginBottom: 4,
    },
    warningText: {
        color: '#7F1D1D',
    },
    label: {
        fontWeight: '600',
        color: '#0F172A',
    },
    input: {
        borderWidth: 1,
        borderColor: '#E2E8F0',
        borderRadius: 8,
        padding: 12,
        backgroundColor: '#FFFFFF',
    },
    errorBox: {
        backgroundColor: '#FEF2F2',
        borderLeftColor: DANGER,
        borderLeftWidth: 4,
        padding: 10,
        borderRadius: 8,
    },
    errorText: {
        color: DANGER,
    },
    deactivateButton: {
        backgroundColor: DANGER,
        padding: 14,
        borderRadius: 10,
        alignItems: 'center',
        justifyContent: 'center',
    },
    deactivateButtonText: {
        color: '#FFFFFF',
        fontWeight: '700',
    },
    cancelButton: {
        padding: 12,
        alignItems: 'center',
    },
    cancelButtonText: {
        color: ACCENT,
        fontWeight: '600',
    },
});
