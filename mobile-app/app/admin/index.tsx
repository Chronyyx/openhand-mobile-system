import React from 'react';
import { View, Text, StyleSheet, Pressable } from 'react-native';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';

export default function AdminDashboardScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { hasRole } = useAuth();
    const isAdmin = hasRole(['ROLE_ADMIN']);
    const [menuVisible, setMenuVisible] = React.useState(false);
    const colorScheme = useColorScheme() ?? 'light';
    const ACCENT = colorScheme === 'dark' ? '#6AA9FF' : '#0056A8';
    const SURFACE = colorScheme === 'dark' ? '#0F1419' : '#F5F7FB';
    const styles = getStyles(colorScheme);

    const handleNavigateHome = () => {
        setMenuVisible(false);
        router.replace('/');
    };

    const handleNavigateEvents = () => {
        setMenuVisible(false);
        router.push('/events');
    };

    const handleNavigateAttendance = () => {
        setMenuVisible(false);
        router.push('/admin/attendance');
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
                        <Text style={styles.subtitle}>
                            {t(isAdmin ? 'admin.dashboard.subtitle' : 'admin.dashboard.subtitleStaff')}
                        </Text>
                    </View>
                </View>

                <View style={styles.sectionHeader}>
                    <Text style={styles.sectionLabel}>{t('admin.dashboard.management')}</Text>
                    {isAdmin && (
                        <View style={styles.badge}>
                            <Text style={styles.badgeText}>{t('admin.dashboard.adminOnly')}</Text>
                        </View>
                    )}
                </View>

                {isAdmin && (
                    <Pressable
                        style={({ pressed }) => [
                            styles.card,
                            pressed && styles.cardPressed,
                        ]}
                        onPress={() => router.push('/admin/users')}
                        accessibilityRole="button"
                        accessibilityLabel={t('admin.dashboard.users')}
                        accessibilityHint={t('admin.dashboard.usersDescription')}
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
                )}

                {isAdmin && (
                    <Pressable
                        style={({ pressed }) => [
                            styles.card,
                            pressed && styles.cardPressed,
                        ]}
                        onPress={() => router.push('/admin/attendance-reports')}
                        accessibilityRole="button"
                        accessibilityLabel={t('admin.dashboard.attendanceReports')}
                        accessibilityHint={t('admin.dashboard.attendanceReportsDescription')}
                    >
                        <View style={styles.cardIcon}>
                            <Ionicons name="stats-chart" size={24} color={ACCENT} />
                        </View>
                        <View style={{ flex: 1 }}>
                            <Text style={styles.cardTitle}>{t('admin.dashboard.attendanceReports')}</Text>
                            <Text style={styles.cardDescription}>{t('admin.dashboard.attendanceReportsDescription')}</Text>
                        </View>
                        <Ionicons name="chevron-forward" size={18} color={ACCENT} />
                    </Pressable>
                )}

                <Pressable
                    style={({ pressed }) => [
                        styles.card,
                        pressed && styles.cardPressed,
                    ]}
                    onPress={() => router.push('/admin/events')}
                    accessibilityRole="button"
                    accessibilityLabel={t('admin.dashboard.events')}
                    accessibilityHint={t('admin.dashboard.eventsDescription')}
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
                onNavigateProfile={() => { setMenuVisible(false); router.push('/profile'); }}
                onNavigateAttendance={handleNavigateAttendance}
                showAttendance={hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])}
                onNavigateDashboard={handleNavigateDashboard}
                showDashboard={hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])}
                t={t}
            />
        </View>
    );
}

const getStyles = (scheme: 'light' | 'dark') => {
    const isDark = scheme === 'dark';
    const ACCENT = isDark ? '#6AA9FF' : '#0056A8';
    const SURFACE = isDark ? '#0F1419' : '#F5F7FB';
    const BG = isDark ? '#1F2328' : '#FFFFFF';
    const BORDER = isDark ? '#2F3A4A' : '#E0E7F3';
    const TEXT = isDark ? '#ECEDEE' : '#0F2848';
    const TEXT_MUTED = isDark ? '#A0A7B1' : '#5C6A80';
    const INFO_BG = isDark ? '#1D2A3A' : '#EAF1FF';

    return StyleSheet.create({
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
        backgroundColor: BG,
        padding: 16,
        borderRadius: 14,
        alignItems: 'center',
        gap: 12,
        shadowColor: '#000',
        shadowOpacity: 0.06,
        shadowRadius: 10,
        shadowOffset: { width: 0, height: 4 },
        elevation: 4,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
    },
    heroIcon: {
        width: 46,
        height: 46,
        borderRadius: 14,
        backgroundColor: INFO_BG,
        alignItems: 'center',
        justifyContent: 'center',
    },
    title: {
        fontSize: 20,
        fontWeight: '700',
        color: TEXT,
    },
    subtitle: {
        color: TEXT_MUTED,
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
        color: TEXT,
    },
    badge: {
        backgroundColor: INFO_BG,
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
        backgroundColor: BG,
        borderRadius: 14,
        padding: 14,
        gap: 12,
        shadowColor: '#000',
        shadowOpacity: 0.06,
        shadowRadius: 8,
        shadowOffset: { width: 0, height: 3 },
        elevation: 3,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
    },
    cardPressed: {
        transform: [{ scale: 0.99 }],
    },
    cardIcon: {
        width: 42,
        height: 42,
        borderRadius: 12,
        backgroundColor: INFO_BG,
        alignItems: 'center',
        justifyContent: 'center',
    },
    cardTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: TEXT,
    },
    cardDescription: {
        marginTop: 2,
        color: TEXT_MUTED,
        fontSize: 13,
    },
    });
};
