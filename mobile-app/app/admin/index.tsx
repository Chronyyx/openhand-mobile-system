import React from 'react';
import { View, Text, StyleSheet, Pressable } from 'react-native';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';

const ACCENT = '#0056A8';
const SURFACE = '#F5F7FB';

export default function AdminDashboardScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { hasRole } = useAuth();
    const [menuVisible, setMenuVisible] = React.useState(false);

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

    return (
        <View style={styles.container}>
            <AppHeader onMenuPress={() => setMenuVisible(true)} />

            <View style={styles.content}>
                <View style={styles.hero}>
                    <View style={styles.heroIcon}>
                        <Ionicons name="shield-checkmark" size={22} color={ACCENT} />
                    </View>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.title}>{t('admin.dashboard.title')}</Text>
                        <Text style={styles.subtitle}>{t('admin.dashboard.subtitle')}</Text>
                    </View>
                </View>

                <View style={styles.sectionHeader}>
                    <Text style={styles.sectionLabel}>{t('admin.dashboard.management')}</Text>
                    <View style={styles.badge}>
                        <Text style={styles.badgeText}>{t('admin.dashboard.adminOnly')}</Text>
                    </View>
                </View>

                <Pressable
                    style={({ pressed }) => [
                        styles.card,
                        pressed && styles.cardPressed,
                    ]}
                    onPress={() => router.push('/admin/users')}
                >
                    <View style={styles.cardIcon}>
                        <Ionicons name="people-circle" size={26} color={ACCENT} />
                    </View>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.cardTitle}>{t('admin.dashboard.users')}</Text>
                        <Text style={styles.cardDescription}>{t('admin.dashboard.usersDescription')}</Text>
                    </View>
                    <Ionicons name="chevron-forward" size={18} color={ACCENT} />
                </Pressable>

                <Pressable
                    style={({ pressed }) => [
                        styles.card,
                        pressed && styles.cardPressed,
                    ]}
                    onPress={() => router.push('/admin/events')}
                >
                    <View style={styles.cardIcon}>
                        <Ionicons name="calendar" size={24} color={ACCENT} />
                    </View>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.cardTitle}>{t('admin.dashboard.events')}</Text>
                        <Text style={styles.cardDescription}>{t('admin.dashboard.eventsDescription')}</Text>
                    </View>
                    <Ionicons name="chevron-forward" size={18} color={ACCENT} />
                </Pressable>
            </View>

            <NavigationMenu
                visible={menuVisible}
                onClose={() => setMenuVisible(false)}
                onNavigateHome={handleNavigateHome}
                onNavigateEvents={handleNavigateEvents}
                onNavigateDashboard={handleNavigateDashboard}
                showDashboard={hasRole(['ROLE_ADMIN'])}
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
        paddingHorizontal: 18,
        paddingVertical: 18,
    },
    hero: {
        flexDirection: 'row',
        backgroundColor: '#FFFFFF',
        padding: 16,
        borderRadius: 14,
        alignItems: 'center',
        gap: 12,
        shadowColor: '#000',
        shadowOpacity: 0.06,
        shadowRadius: 10,
        shadowOffset: { width: 0, height: 4 },
        elevation: 4,
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
    },
    sectionHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: 22,
        marginBottom: 10,
        gap: 8,
    },
    sectionLabel: {
        fontSize: 14,
        fontWeight: '700',
        color: '#1B2F4A',
    },
    badge: {
        backgroundColor: '#EAF1FF',
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 12,
    },
    badgeText: {
        color: ACCENT,
        fontSize: 12,
        fontWeight: '700',
    },
    card: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        borderRadius: 14,
        padding: 14,
        gap: 12,
        shadowColor: '#000',
        shadowOpacity: 0.06,
        shadowRadius: 8,
        shadowOffset: { width: 0, height: 3 },
        elevation: 3,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E7F3',
    },
    cardPressed: {
        transform: [{ scale: 0.99 }],
    },
    cardIcon: {
        width: 42,
        height: 42,
        borderRadius: 12,
        backgroundColor: '#F3F7FF',
        alignItems: 'center',
        justifyContent: 'center',
    },
    cardTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: '#0F2848',
    },
    cardDescription: {
        marginTop: 2,
        color: '#5C6A80',
        fontSize: 13,
    },
});
