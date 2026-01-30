import React from 'react';
import {
    Modal,
    Pressable,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useColorScheme } from '../hooks/use-color-scheme';

type NavigationMenuProps = {
    visible: boolean;
    onClose: () => void;
    onNavigateHome: () => void;
    onNavigateEvents: () => void;
    onNavigateAttendance?: () => void;
    onNavigateProfile?: () => void;
    onNavigateMyRegistrations?: () => void;
    showAttendance?: boolean;
    showDashboard?: boolean;
    onNavigateDashboard?: () => void;
    showMyRegistrations?: boolean;
    t: (key: string) => string;
};

import { useNotifications } from '@/hooks/useNotifications';

export function NavigationMenu({
    visible,
    onClose,
    onNavigateHome,
    onNavigateEvents,
    onNavigateAttendance,
    onNavigateProfile,
    onNavigateMyRegistrations,
    showAttendance = false,
    showDashboard = false,
    onNavigateDashboard,
    showMyRegistrations = false,
    t,
}: NavigationMenuProps) {
    const { unreadCount } = useNotifications();
    const colorScheme = useColorScheme() ?? 'light';
    const styles = getStyles(colorScheme);
    const palette = {
        primary: colorScheme === 'dark' ? '#9FC3FF' : '#0056A8',
        text: colorScheme === 'dark' ? '#ECEDEE' : '#2D3B57',
        danger: colorScheme === 'dark' ? '#FFB4AB' : '#FF3B30',
    };

    return (
        <Modal
            visible={visible}
            transparent
            animationType="fade"
            onRequestClose={onClose}
        >
            <View style={styles.menuOverlay}>
                <Pressable style={StyleSheet.absoluteFill} onPress={onClose} />
                <View style={styles.menuContainer}>
                    <View style={styles.menuHeader}>
                        <View style={styles.menuBadge}>
                            <Ionicons name="sparkles" size={16} color={palette.primary} />
                        </View>
                        <View style={{ flex: 1, marginLeft: 10 }}>
                            <Text style={styles.menuTitle}>{t('menu.navigation')}</Text>
                            <Text style={styles.menuSubtitle}>{t('menu.quickAccess')}</Text>
                        </View>
                        <Pressable hitSlop={12} onPress={onClose}>
                            <Ionicons name="close" size={20} color={palette.text} />
                        </Pressable>
                    </View>

                    <View style={styles.menuDivider} />

                    <Pressable
                        style={({ pressed }) => [
                            styles.menuItem,
                            pressed && styles.menuItemPressed,
                        ]}
                        onPress={onNavigateHome}
                    >
                        <View style={styles.menuItemLeft}>
                            <Ionicons name="home" size={20} color={palette.primary} />
                            <Text style={styles.menuItemText}>{t('menu.home')}</Text>
                        </View>
                        <Ionicons name="chevron-forward" size={18} color={palette.primary} />
                    </Pressable>

                    <Pressable
                        style={({ pressed }) => [
                            styles.menuItem,
                            styles.menuItemElevated,
                            pressed && styles.menuItemPressed,
                        ]}
                        onPress={onNavigateEvents}
                    >
                        <View style={styles.menuItemLeft}>
                            <Ionicons name="calendar" size={20} color={palette.primary} />
                            <Text style={styles.menuItemText}>{t('menu.events')}</Text>
                        </View>
                        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                            {unreadCount > 0 && (
                                <View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: palette.danger }} />
                            )}
                            <Ionicons name="chevron-forward" size={18} color={palette.primary} />
                        </View>
                    </Pressable>

                    {showAttendance && onNavigateAttendance && (
                        <Pressable
                            style={({ pressed }) => [
                                styles.menuItem,
                                styles.menuItemElevated,
                                pressed && styles.menuItemPressed,
                            ]}
                            onPress={onNavigateAttendance}
                        >
                            <View style={styles.menuItemLeft}>
                                <Ionicons name="checkbox" size={20} color={palette.primary} />
                                <Text style={styles.menuItemText}>{t('menu.attendance')}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={palette.primary} />
                        </Pressable>
                    )}

                    {showMyRegistrations && onNavigateMyRegistrations && (
                        <Pressable
                            style={({ pressed }) => [
                                styles.menuItem,
                                styles.menuItemElevated,
                                pressed && styles.menuItemPressed,
                            ]}
                            onPress={onNavigateMyRegistrations}
                        >
                            <View style={styles.menuItemLeft}>
                                <Ionicons name="list" size={20} color={palette.primary} />
                                <Text style={styles.menuItemText}>{t('menu.myRegistrations')}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={palette.primary} />
                        </Pressable>
                    )}

                    {showDashboard && onNavigateDashboard && (
                        <Pressable
                            style={({ pressed }) => [
                                styles.menuItem,
                                styles.menuItemElevated,
                                pressed && styles.menuItemPressed,
                            ]}
                            onPress={onNavigateDashboard}
                        >
                            <View style={styles.menuItemLeft}>
                                <Ionicons name="speedometer" size={20} color={palette.primary} />
                                <Text style={styles.menuItemText}>{t('menu.dashboard')}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={palette.primary} />
                        </Pressable>
                    )}

                    {onNavigateProfile && (
                        <Pressable
                            style={({ pressed }) => [
                                styles.menuItem,
                                styles.menuItemElevated,
                                pressed && styles.menuItemPressed,
                            ]}
                            onPress={onNavigateProfile}
                        >
                            <View style={styles.menuItemLeft}>
                                <Ionicons name="person-circle" size={20} color={palette.primary} />
                                <Text style={styles.menuItemText}>{t('menu.profile')}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={palette.primary} />
                        </Pressable>
                    )}
                </View>
            </View>
        </Modal>
    );
}

const getStyles = (scheme: 'light' | 'dark') => {
    const isDark = scheme === 'dark';
    const primary = isDark ? '#9FC3FF' : '#0056A8';
    return StyleSheet.create({
        menuOverlay: {
            flex: 1,
            backgroundColor: isDark ? 'rgba(0,0,0,0.6)' : 'rgba(4,15,34,0.3)',
            paddingTop: 60,
            paddingHorizontal: 12,
        },
        menuContainer: {
            marginLeft: 'auto',
            width: 232,
            backgroundColor: isDark ? '#151A20' : '#FFFFFF',
            borderRadius: 16,
            paddingVertical: 14,
            paddingHorizontal: 14,
            borderWidth: StyleSheet.hairlineWidth,
            borderColor: isDark ? '#2A313B' : '#E5ECF8',
            shadowColor: '#000',
            shadowOpacity: 0.14,
            shadowRadius: 10,
            shadowOffset: { width: 0, height: 6 },
            elevation: 8,
            gap: 10,
        },
        menuHeader: {
            flexDirection: 'row',
            alignItems: 'center',
        },
        menuBadge: {
            width: 34,
            height: 34,
            borderRadius: 17,
            backgroundColor: isDark ? '#1D2A3A' : '#EAF1FF',
            alignItems: 'center',
            justifyContent: 'center',
        },
        menuTitle: {
            fontSize: 15,
            fontWeight: '700',
            color: isDark ? '#ECEDEE' : '#1A2D4A',
            letterSpacing: 0.2,
        },
        menuSubtitle: {
            fontSize: 12,
            color: isDark ? '#A0A7B1' : '#6F7B91',
            marginTop: 2,
        },
        menuDivider: {
            height: StyleSheet.hairlineWidth,
            backgroundColor: isDark ? '#2A313B' : '#E2E8F2',
        },
        menuItem: {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingVertical: 10,
            paddingHorizontal: 10,
            borderRadius: 12,
            backgroundColor: isDark ? '#1A2129' : '#F8FAFE',
        },
        menuItemPressed: {
            opacity: 0.8,
        },
        menuItemElevated: {
            borderWidth: StyleSheet.hairlineWidth,
            borderColor: isDark ? '#2E3945' : '#D9E5FF',
            shadowColor: isDark ? '#000' : '#2F64C0',
            shadowOpacity: 0.08,
            shadowRadius: 8,
            shadowOffset: { width: 0, height: 3 },
            elevation: 4,
        },
        menuItemLeft: {
            flexDirection: 'row',
            alignItems: 'center',
            gap: 10,
        },
        menuItemText: {
            fontSize: 15,
            fontWeight: '600',
            color: isDark ? '#ECEDEE' : '#1A2D4A',
        },
        menuItemDisabled: {
            backgroundColor: isDark ? '#1F2328' : '#F1F3F7',
            borderWidth: StyleSheet.hairlineWidth,
            borderColor: isDark ? '#2A313B' : '#E2E7EF',
        },
        menuItemTextDisabled: {
            color: isDark ? '#6B7280' : '#9BA5B7',
            fontWeight: '500',
        },
        menuPill: {
            backgroundColor: isDark ? '#1D2A3A' : '#EAF1FF',
            color: primary,
            fontSize: 11,
            fontWeight: '700',
            paddingHorizontal: 8,
            paddingVertical: 4,
            borderRadius: 12,
        },
    });
};
