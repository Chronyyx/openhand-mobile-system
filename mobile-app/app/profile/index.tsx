import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView, Pressable } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';
import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';

const ACCENT = '#0056A8';
const SURFACE = '#F5F7FB';

export default function ProfileScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { user, signOut, hasRole } = useAuth();
    const [menuVisible, setMenuVisible] = useState(false);

    const handleNavigateHome = () => {
        setMenuVisible(false);
        router.replace('/');
    };

    const handleNavigateEvents = () => {
        setMenuVisible(false);
        router.push('/events');
    };

    const handleNavigateMyRegistrations = () => {
        setMenuVisible(false);
        router.push('/registrations');
    };

    const handleNavigateDashboard = () => {
        setMenuVisible(false);
        router.push('/admin');
    };

    if (!user) {
        return (
            <View style={styles.container}>
                <AppHeader onMenuPress={() => setMenuVisible(true)} />
                <View style={styles.centered}>
                    <Text>{t('common.notAuthenticated')}</Text>
                    <Pressable style={styles.loginButton} onPress={() => router.push('/auth/login')}>
                        <Text style={styles.loginButtonText}>{t('home.loginRegister')}</Text>
                    </Pressable>
                </View>
                <NavigationMenu
                    visible={menuVisible}
                    onClose={() => setMenuVisible(false)}
                    onNavigateHome={handleNavigateHome}
                    onNavigateEvents={handleNavigateEvents}
                    onNavigateProfile={() => setMenuVisible(false)}
                    onNavigateMyRegistrations={handleNavigateMyRegistrations}
                    showMyRegistrations={false}
                    showDashboard={false}
                    onNavigateDashboard={handleNavigateDashboard}
                    t={t}
                />
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <AppHeader onMenuPress={() => setMenuVisible(true)} />

            <ScrollView contentContainerStyle={styles.scrollContent}>
                <View style={styles.header}>
                    <Pressable style={styles.backButton} onPress={() => router.back()}>
                        <Ionicons name="chevron-back" size={24} color={ACCENT} />
                    </Pressable>
                    <Text style={styles.title}>{t('profile.title')}</Text>
                </View>

                <View style={styles.card}>
                    <View style={styles.avatarContainer}>
                        <View style={styles.avatar}>
                            <Ionicons name="person" size={40} color={ACCENT} />
                        </View>
                        <Text style={styles.userName}>{user.name || user.email}</Text>
                        <Text style={styles.userEmail}>{user.email}</Text>
                        <View style={styles.rolesRow}>
                            {user.roles.map((role) => (
                                <View key={role} style={styles.rolePill}>
                                    <Text style={styles.rolePillText}>{role.replace('ROLE_', '')}</Text>
                                </View>
                            ))}
                        </View>
                    </View>

                    <View style={styles.infoSection}>
                        <InfoItem icon="call-outline" label={t('profile.phone')} value={user.phoneNumber || '-'} />
                        <InfoItem icon="male-female-outline" label={t('profile.gender')} value={user.gender || '-'} />
                        <InfoItem icon="calendar-outline" label={t('profile.age')} value={user.age ? user.age.toString() : '-'} />
                    </View>
                </View>

                {hasRole(['ROLE_MEMBER']) && (
                    <View style={styles.settingsCard}>
                        <Pressable
                            style={styles.settingsItem}
                            onPress={() => router.push('/settings/notifications')}
                        >
                            <View style={styles.settingsItemLeft}>
                                <Ionicons name="notifications-outline" size={20} color={ACCENT} />
                                <Text style={styles.settingsItemText}>{t('settings.notifications.title')}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={ACCENT} />
                        </Pressable>
                    </View>
                )}

                <Pressable style={styles.logoutButton} onPress={signOut}>
                    <Ionicons name="log-out-outline" size={20} color="#C62828" />
                    <Text style={styles.logoutButtonText}>{t('profile.logout')}</Text>
                </Pressable>

            </ScrollView>

            <NavigationMenu
                visible={menuVisible}
                onClose={() => setMenuVisible(false)}
                onNavigateHome={handleNavigateHome}
                onNavigateEvents={handleNavigateEvents}
                onNavigateProfile={() => setMenuVisible(false)}
                onNavigateMyRegistrations={handleNavigateMyRegistrations}
                showMyRegistrations={!!user}
                showDashboard={hasRole(['ROLE_ADMIN'])}
                onNavigateDashboard={handleNavigateDashboard}
                t={t}
            />
        </View>
    );
}

function InfoItem({ icon, label, value }: { icon: keyof typeof Ionicons.glyphMap; label: string; value: string }) {
    return (
        <View style={styles.infoItem}>
            <View style={styles.iconContainer}>
                <Ionicons name={icon} size={20} color={ACCENT} />
            </View>
            <View>
                <Text style={styles.infoLabel}>{label}</Text>
                <Text style={styles.infoValue}>{value}</Text>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: SURFACE,
    },
    scrollContent: {
        padding: 16,
    },
    centered: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        gap: 20
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 20,
    },
    backButton: {
        padding: 8,
        marginRight: 8,
    },
    title: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#0F2848',
    },
    card: {
        backgroundColor: '#FFFFFF',
        borderRadius: 16,
        padding: 24,
        shadowColor: '#000',
        shadowOpacity: 0.05,
        shadowRadius: 10,
        shadowOffset: { width: 0, height: 4 },
        elevation: 3,
        marginBottom: 20,
    },
    avatarContainer: {
        alignItems: 'center',
        marginBottom: 24,
        borderBottomWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E6F0',
        paddingBottom: 24,
    },
    avatar: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: '#EAF1FF',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 16,
    },
    userName: {
        fontSize: 20,
        fontWeight: '700',
        color: '#0F2848',
        marginBottom: 4,
    },
    userEmail: {
        fontSize: 14,
        color: '#5C6A80',
        marginBottom: 12,
    },
    rolesRow: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8,
    },
    rolePill: {
        backgroundColor: '#F0F6FF',
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 20,
    },
    rolePillText: {
        fontSize: 12,
        fontWeight: '600',
        color: ACCENT,
    },
    infoSection: {
        gap: 20,
    },
    infoItem: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 16,
    },
    iconContainer: {
        width: 40,
        height: 40,
        borderRadius: 12,
        backgroundColor: '#F5F9FF',
        alignItems: 'center',
        justifyContent: 'center',
    },
    infoLabel: {
        fontSize: 12,
        color: '#5C6A80',
        marginBottom: 2,
    },
    infoValue: {
        fontSize: 16,
        color: '#0F2848',
        fontWeight: '500',
    },
    logoutButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#FFF0F0',
        padding: 16,
        borderRadius: 12,
        gap: 8,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#FFDBDB',
    },
    logoutButtonText: {
        color: '#C62828',
        fontWeight: '600',
        fontSize: 16,
    },
    loginButton: {
        backgroundColor: ACCENT,
        paddingHorizontal: 24,
        paddingVertical: 12,
        borderRadius: 8,
    },
    loginButtonText: {
        color: '#FFFFFF',
        fontWeight: '600',
    },
    settingsCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: 16,
        paddingVertical: 6,
        paddingHorizontal: 8,
        shadowColor: '#000',
        shadowOpacity: 0.05,
        shadowRadius: 10,
        shadowOffset: { width: 0, height: 4 },
        elevation: 3,
        marginBottom: 20,
    },
    settingsItem: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: 14,
        paddingHorizontal: 10,
        borderRadius: 12,
        backgroundColor: '#F8FAFE',
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#D9E5FF',
    },
    settingsItemLeft: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
    },
    settingsItemText: {
        fontSize: 15,
        fontWeight: '600',
        color: '#1A2D4A',
    },
});
