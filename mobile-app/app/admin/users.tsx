import React, { useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    Modal,
    Pressable,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTranslation } from 'react-i18next';
import { Redirect, useRouter } from 'expo-router';
import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';

import {
    fetchAllUsers,
    fetchAvailableRoles,
    updateUserRoles,
    type ManagedUser,
} from '../../services/user-management.service';

const ACCENT = '#0056A8';
const SURFACE = '#F5F7FB';

export default function AdminUsersScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { hasRole } = useAuth();
    const isAdmin = hasRole(['ROLE_ADMIN']);

    const [users, setUsers] = useState<ManagedUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [availableRoles, setAvailableRoles] = useState<string[]>([]);

    const [selectedUser, setSelectedUser] = useState<ManagedUser | null>(null);
    const [selectedRole, setSelectedRole] = useState<string | null>(null);
    const [rolePickerOpen, setRolePickerOpen] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [menuVisible, setMenuVisible] = useState(false);

    const roleLabels = useMemo(
        () => ({
            ROLE_ADMIN: t('admin.users.roles.admin'),
            ROLE_MEMBER: t('admin.users.roles.member'),
            ROLE_EMPLOYEE: t('admin.users.roles.employee'),
        }),
        [t],
    );

    useEffect(() => {
        if (isAdmin) {
            loadUsers();
        }
    }, [isAdmin]);

    if (!isAdmin) {
        return <Redirect href="/admin/events" />;
    }

    const loadUsers = async () => {
        setLoading(true);
        setError(null);
        try {
            const [userData, roles] = await Promise.all([fetchAllUsers(), fetchAvailableRoles()]);
            setUsers(userData);
            setAvailableRoles(roles);
        } catch (err) {
            console.error('Failed to load users', err);
            setError(t('admin.users.loadError'));
        } finally {
            setLoading(false);
        }
    };

    const openEditor = (user: ManagedUser) => {
        setSelectedUser(user);
        const preferredRole =
            availableRoles.find((role) => user.roles.includes(role)) ?? user.roles[0] ?? null;
        setSelectedRole(preferredRole);
        setRolePickerOpen(false);
        setModalVisible(true);
    };

    const closeEditor = () => {
        setModalVisible(false);
        setSelectedUser(null);
        setSelectedRole(null);
        setRolePickerOpen(false);
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

    const saveRoleChange = async () => {
        if (!selectedUser || !selectedRole) return;
        setSaving(true);
        setError(null);
        try {
            const updated = await updateUserRoles(selectedUser.id, [selectedRole]);
            setUsers((current) =>
                current.map((user) => (user.id === updated.id ? updated : user)),
            );
            closeEditor();
        } catch (err) {
            console.error('Failed to update user role', err);
            setError(t('admin.users.updateError'));
        } finally {
            setSaving(false);
        }
    };

    const renderRoleLabel = (role: string) => roleLabels[role as keyof typeof roleLabels] ?? role;

    const renderUserItem = ({ item }: { item: ManagedUser }) => (
        <View style={styles.userCard}>
            <View style={styles.userHeader}>
                <View style={styles.userAvatar}>
                    <Ionicons name="person" size={18} color={ACCENT} />
                </View>
                <View style={{ flex: 1 }}>
                    {item.name ? <Text style={styles.userNameList}>{item.name}</Text> : null}
                    <Text style={styles.userEmail}>{item.email}</Text>
                    <View style={styles.rolesRow}>
                        {item.roles.map((role) => (
                            <View key={role} style={styles.rolePill}>
                                <Text style={styles.rolePillText}>{renderRoleLabel(role)}</Text>
                            </View>
                        ))}
                    </View>
                </View>
                <Pressable
                    style={({ pressed }) => [
                        styles.editButton,
                        pressed && styles.editButtonPressed,
                    ]}
                    onPress={() => openEditor(item)}
                >
                    <Ionicons name="create-outline" size={16} color={ACCENT} />
                    <Text style={styles.editButtonText}>{t('admin.users.edit')}</Text>
                </Pressable>
            </View>
        </View>
    );

    return (
        <View style={styles.container}>
            <AppHeader onMenuPress={() => setMenuVisible(true)} />

            <View style={styles.content}>
                <View style={styles.header}>
                    <Pressable style={styles.backButton} onPress={() => router.back()}>
                        <Ionicons name="chevron-back" size={18} color={ACCENT} />
                    </Pressable>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.title}>{t('admin.users.title')}</Text>
                        <Text style={styles.subtitle}>{t('admin.users.subtitle')}</Text>
                    </View>
                </View>

                {loading ? (
                    <View style={styles.centered}>
                        <ActivityIndicator />
                    </View>
                ) : error ? (
                    <View style={styles.errorBox}>
                        <Ionicons name="alert-circle" size={18} color="#C62828" />
                        <Text style={styles.errorText}>{error}</Text>
                    </View>
                ) : (
                    <FlatList
                        data={users}
                        keyExtractor={(item) => item.id.toString()}
                        renderItem={renderUserItem}
                        contentContainerStyle={styles.listContent}
                        ListEmptyComponent={
                            <View style={styles.emptyState}>
                                <Ionicons name="people-outline" size={26} color="#9BA5B7" />
                                <Text style={styles.emptyText}>{t('admin.users.empty')}</Text>
                            </View>
                        }
                    />
                )}
            </View>

            <Modal
                transparent
                visible={modalVisible}
                animationType="slide"
                onRequestClose={closeEditor}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalCard}>
                        <View style={styles.modalHeader}>
                            <Ionicons name="person-circle-outline" size={24} color={ACCENT} />
                            <Text style={styles.modalTitle}>{t('admin.users.userDetails')}</Text>
                        </View>

                        <View style={styles.userInfoBlock}>
                            <Text style={styles.userName}>{selectedUser?.name || '-'}</Text>
                            <Text style={styles.userEmail}>{selectedUser?.email}</Text>

                            <View style={styles.infoRow}>
                                <Ionicons name="call-outline" size={14} color="#5C6A80" />
                                <Text style={styles.infoText}>{selectedUser?.phoneNumber || '-'}</Text>
                            </View>
                            <View style={styles.infoRow}>
                                <Ionicons name="male-female-outline" size={14} color="#5C6A80" />
                                <Text style={styles.infoText}>{selectedUser?.gender || '-'}</Text>
                                <Text style={styles.separator}>•</Text>
                                <Ionicons name="calendar-outline" size={14} color="#5C6A80" />
                                <Text style={styles.infoText}>{selectedUser?.age ? selectedUser.age + ' y.o.' : '-'}</Text>
                            </View>
                        </View>

                        <Text style={styles.fieldLabel}>{t('admin.users.role')}</Text>
                        <Pressable
                            style={styles.dropdown}
                            onPress={() => setRolePickerOpen((open) => !open)}
                        >
                            <Text style={styles.dropdownValue}>
                                {selectedRole ? renderRoleLabel(selectedRole) : t('admin.users.selectRole')}
                            </Text>
                            <Ionicons
                                name={rolePickerOpen ? 'chevron-up' : 'chevron-down'}
                                size={18}
                                color={ACCENT}
                            />
                        </Pressable>

                        {rolePickerOpen && (
                            <View style={styles.dropdownList}>
                                {availableRoles.map((role) => (
                                    <Pressable
                                        key={role}
                                        style={({ pressed }) => [
                                            styles.dropdownItem,
                                            selectedRole === role && styles.dropdownItemSelected,
                                            pressed && styles.dropdownItemPressed,
                                        ]}
                                        onPress={() => {
                                            setSelectedRole(role);
                                            setRolePickerOpen(false);
                                        }}
                                    >
                                        <Text
                                            style={[
                                                styles.dropdownItemText,
                                                selectedRole === role && styles.dropdownItemTextSelected,
                                            ]}
                                        >
                                            {renderRoleLabel(role)}
                                        </Text>
                                        {selectedRole === role && (
                                            <Ionicons name="checkmark" size={16} color={ACCENT} />
                                        )}
                                    </Pressable>
                                ))}
                            </View>
                        )}

                        <View style={styles.modalActions}>
                            <Pressable style={styles.secondaryButton} onPress={closeEditor}>
                                <Text style={styles.secondaryButtonText}>{t('common.cancel')}</Text>
                            </Pressable>
                            <Pressable
                                style={({ pressed }) => [
                                    styles.primaryButton,
                                    (!selectedRole || saving) && styles.primaryButtonDisabled,
                                    pressed && styles.primaryButtonPressed,
                                ]}
                                disabled={!selectedRole || saving}
                                onPress={saveRoleChange}
                            >
                                {saving ? (
                                    <ActivityIndicator color="#FFFFFF" />
                                ) : (
                                    <Text style={styles.primaryButtonText}>{t('admin.users.save')}</Text>
                                )}
                            </Pressable>
                        </View>
                    </View>
                </View>
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
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 16,
        paddingTop: 18,
        paddingBottom: 12,
        backgroundColor: '#FFFFFF',
        borderBottomWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E6F0',
    },
    backButton: {
        width: 32,
        height: 32,
        borderRadius: 10,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E6F0',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 10,
    },
    title: {
        fontSize: 18,
        fontWeight: '700',
        color: '#0F2848',
    },
    subtitle: {
        fontSize: 13,
        color: '#5C6A80',
        marginTop: 2,
    },
    centered: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
    errorBox: {
        margin: 16,
        padding: 12,
        backgroundColor: '#FFF0F0',
        borderRadius: 10,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#F5C6CB',
    },
    errorText: {
        color: '#C62828',
        flex: 1,
        fontSize: 14,
    },
    listContent: {
        padding: 16,
        gap: 12,
    },
    userCard: {
        backgroundColor: '#FFFFFF',
        padding: 14,
        borderRadius: 14,
        shadowColor: '#000',
        shadowOpacity: 0.06,
        shadowRadius: 8,
        shadowOffset: { width: 0, height: 3 },
        elevation: 3,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E1E7F2',
    },
    userHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    userAvatar: {
        width: 40,
        height: 40,
        borderRadius: 12,
        backgroundColor: '#F0F6FF',
        alignItems: 'center',
        justifyContent: 'center',
    },
    userNameList: {
        fontSize: 15,
        fontWeight: '700',
        color: '#0F2848',
        marginBottom: 2,
    },
    userEmail: {
        fontSize: 13,
        color: '#5C6A80',
    },
    rolesRow: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 6,
        marginTop: 4,
    },
    rolePill: {
        backgroundColor: '#EAF1FF',
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 10,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#D7E5FF',
    },
    rolePillText: {
        fontSize: 12,
        fontWeight: '700',
        color: ACCENT,
    },
    editButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        paddingHorizontal: 10,
        paddingVertical: 8,
        borderRadius: 10,
        backgroundColor: '#F4F8FF',
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#D7E5FF',
    },
    editButtonPressed: {
        opacity: 0.85,
    },
    editButtonText: {
        fontSize: 13,
        fontWeight: '700',
        color: ACCENT,
    },
    emptyState: {
        alignItems: 'center',
        padding: 30,
        gap: 10,
    },
    emptyText: {
        color: '#6B7285',
        fontSize: 14,
    },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.35)',
        alignItems: 'center',
        justifyContent: 'flex-end',
    },
    modalCard: {
        backgroundColor: '#FFFFFF',
        width: '100%',
        borderTopLeftRadius: 18,
        borderTopRightRadius: 18,
        padding: 18,
        shadowColor: '#000',
        shadowOpacity: 0.2,
        shadowRadius: 10,
        shadowOffset: { width: 0, height: -3 },
        elevation: 10,
        gap: 12,
    },
    modalHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
    },
    modalTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: '#0F2848',
    },
    modalSubtitle: {
        color: '#5C6A80',
        fontSize: 13,
        marginBottom: 6,
    },
    fieldLabel: {
        fontSize: 13,
        fontWeight: '700',
        color: '#1B2F4A',
    },
    dropdown: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: '#F7F9FC',
        borderRadius: 12,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E6F0',
        paddingHorizontal: 12,
        paddingVertical: 12,
    },
    dropdownValue: {
        color: '#0F2848',
        fontWeight: '600',
    },
    dropdownList: {
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        marginTop: 10,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E6F0',
        overflow: 'hidden',
    },
    dropdownItem: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 12,
        paddingVertical: 12,
    },
    dropdownItemPressed: {
        backgroundColor: '#F6F8FB',
    },
    dropdownItemSelected: {
        backgroundColor: '#EAF1FF',
    },
    dropdownItemText: {
        color: '#1B2F4A',
        fontWeight: '600',
    },
    dropdownItemTextSelected: {
        color: ACCENT,
    },
    modalActions: {
        flexDirection: 'row',
        justifyContent: 'flex-end',
        gap: 10,
        marginTop: 10,
    },
    secondaryButton: {
        paddingVertical: 12,
        paddingHorizontal: 16,
        borderRadius: 10,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E6F0',
        backgroundColor: '#FFFFFF',
    },
    secondaryButtonText: {
        color: '#1B2F4A',
        fontWeight: '700',
    },
    primaryButton: {
        paddingVertical: 12,
        paddingHorizontal: 16,
        borderRadius: 10,
        backgroundColor: ACCENT,
    },
    primaryButtonDisabled: {
        opacity: 0.5,
    },
    primaryButtonPressed: {
        opacity: 0.85,
    },
    primaryButtonText: {
        color: '#FFFFFF',
        fontWeight: '700',
    },
    userInfoBlock: {
        backgroundColor: '#F7F9FC',
        padding: 12,
        borderRadius: 12,
        marginBottom: 12,
    },
    userName: {
        fontSize: 16,
        fontWeight: '700',
        color: '#0F2848',
        marginBottom: 2,
    },
    infoRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        marginTop: 4,
    },
    infoText: {
        fontSize: 13,
        color: '#5C6A80',
    },
    separator: {
        color: '#C5CDD9',
        marginHorizontal: 4,
    }
});
