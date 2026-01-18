import React from 'react';
import {
    Modal,
    Pressable,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

type NavigationMenuProps = {
    visible: boolean;
    onClose: () => void;
    onNavigateHome: () => void;
    onNavigateEvents: () => void;
    onNavigateProfile?: () => void;
    onNavigateMyRegistrations?: () => void;
    showDashboard?: boolean;
    onNavigateDashboard?: () => void;
    showMyRegistrations?: boolean;
    t: (key: string) => string;
};

const BLUE = '#0056A8';

import { useNotifications } from '@/hooks/useNotifications';

export function NavigationMenu({
    visible,
    onClose,
    onNavigateHome,
    onNavigateEvents,
    onNavigateProfile,
    onNavigateMyRegistrations,
    showDashboard = false,
    onNavigateDashboard,
    showMyRegistrations = false,
    t,
}: NavigationMenuProps) {
    const { unreadCount } = useNotifications();

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
                            <Ionicons name="sparkles" size={16} color={BLUE} />
                        </View>
                        <View style={{ flex: 1, marginLeft: 10 }}>
                            <Text style={styles.menuTitle}>{t('menu.navigation')}</Text>
                            <Text style={styles.menuSubtitle}>{t('menu.quickAccess')}</Text>
                        </View>
                        <Pressable hitSlop={12} onPress={onClose}>
                            <Ionicons name="close" size={20} color="#2D3B57" />
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
                            <Ionicons name="home" size={20} color={BLUE} />
                            <Text style={styles.menuItemText}>{t('menu.home')}</Text>
                        </View>
                        <Ionicons name="chevron-forward" size={18} color={BLUE} />
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
                            <Ionicons name="calendar" size={20} color={BLUE} />
                            <Text style={styles.menuItemText}>{t('menu.events')}</Text>
                        </View>
                        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                            {unreadCount > 0 && (
                                <View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: '#FF3B30' }} />
                            )}
                            <Ionicons name="chevron-forward" size={18} color={BLUE} />
                        </View>
                    </Pressable>

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
                                <Ionicons name="list" size={20} color={BLUE} />
                                <Text style={styles.menuItemText}>{t('menu.myRegistrations')}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={BLUE} />
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
                                <Ionicons name="speedometer" size={20} color={BLUE} />
                                <Text style={styles.menuItemText}>{t('menu.dashboard')}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={BLUE} />
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
                                <Ionicons name="person-circle" size={20} color={BLUE} />
                                <Text style={styles.menuItemText}>{t('menu.profile')}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={BLUE} />
                        </Pressable>
                    )}
                </View>
            </View>
        </Modal>
    );
}

const styles = StyleSheet.create({
    menuOverlay: {
        flex: 1,
        backgroundColor: 'rgba(4,15,34,0.3)',
        paddingTop: 60,
        paddingHorizontal: 12,
    },
    menuContainer: {
        marginLeft: 'auto',
        width: 232,
        backgroundColor: '#FFFFFF',
        borderRadius: 16,
        paddingVertical: 14,
        paddingHorizontal: 14,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E5ECF8',
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
        backgroundColor: '#EAF1FF',
        alignItems: 'center',
        justifyContent: 'center',
    },
    menuTitle: {
        fontSize: 15,
        fontWeight: '700',
        color: '#1A2D4A',
        letterSpacing: 0.2,
    },
    menuSubtitle: {
        fontSize: 12,
        color: '#6F7B91',
        marginTop: 2,
    },
    menuDivider: {
        height: StyleSheet.hairlineWidth,
        backgroundColor: '#E2E8F2',
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: 10,
        paddingHorizontal: 10,
        borderRadius: 12,
        backgroundColor: '#F8FAFE',
    },
    menuItemPressed: {
        opacity: 0.8,
    },
    menuItemElevated: {
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#D9E5FF',
        shadowColor: '#2F64C0',
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
        color: '#1A2D4A',
    },
    menuItemDisabled: {
        backgroundColor: '#F1F3F7',
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E2E7EF',
    },
    menuItemTextDisabled: {
        color: '#9BA5B7',
        fontWeight: '500',
    },
    menuPill: {
        backgroundColor: '#EAF1FF',
        color: BLUE,
        fontSize: 11,
        fontWeight: '700',
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 12,
    },
});
