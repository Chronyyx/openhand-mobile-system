import React, { useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    FlatList,
    Modal,
    Pressable,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Picker } from '@react-native-picker/picker';
import { useTranslation } from 'react-i18next';
import { Redirect, useRouter } from 'expo-router';
import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';

import {
    fetchAllUsers,
    fetchAvailableRoles,
    updateUserRoles,
    updateUserProfile,
    updateUserStatus,
    type AdminUserProfileUpdate,
    type ManagedUser,
} from '../../services/user-management.service';

export default function AdminUsersScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { hasRole } = useAuth();
    const isAdmin = hasRole(['ROLE_ADMIN']);
    const colorScheme = useColorScheme() ?? 'light';
    const isDark = colorScheme === 'dark';
    const ACCENT = isDark ? '#6AA9FF' : '#0056A8';

    const [users, setUsers] = useState<ManagedUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [availableRoles, setAvailableRoles] = useState<string[]>([]);
    const [activeTab, setActiveTab] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE');
    const [searchQuery, setSearchQuery] = useState('');

    const [selectedUser, setSelectedUser] = useState<ManagedUser | null>(null);
    const [selectedRole, setSelectedRole] = useState<string | null>(null);
    const [rolePickerOpen, setRolePickerOpen] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [menuVisible, setMenuVisible] = useState(false);
    const [modalError, setModalError] = useState<string | null>(null);

    const [editName, setEditName] = useState('');
    const [editEmail, setEditEmail] = useState('');
    const [editPhoneNumber, setEditPhoneNumber] = useState('');
    const [editGender, setEditGender] = useState('PREFER_NOT_TO_SAY');
    const [editAge, setEditAge] = useState('');

    const roleLabels = useMemo(
        () => ({
            ROLE_ADMIN: t('admin.users.roles.admin'),
            ROLE_MEMBER: t('admin.users.roles.member'),
            ROLE_EMPLOYEE: t('admin.users.roles.employee'),
        }),
        [t],
    );

    const activeUsers = useMemo(
        () => users.filter((user) => user.memberStatus !== 'INACTIVE'),
        [users],
    );

    const inactiveUsers = useMemo(
        () => users.filter((user) => user.memberStatus === 'INACTIVE'),
        [users],
    );

    const filteredUsers = useMemo(() => {
        const pool = activeTab === 'ACTIVE' ? activeUsers : inactiveUsers;
        const query = searchQuery.trim().toLowerCase();
        if (!query) {
            return pool;
        }
        return pool.filter((user) => {
            const name = (user.name ?? '').toLowerCase();
            const email = (user.email ?? '').toLowerCase();
            return name.includes(query) || email.includes(query);
        });
    }, [activeTab, activeUsers, inactiveUsers, searchQuery]);

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
        setEditName(user.name ?? '');
        setEditEmail(user.email ?? '');
        setEditPhoneNumber(user.phoneNumber ?? '');
        setEditGender(user.gender ?? 'PREFER_NOT_TO_SAY');
        setEditAge(user.age ? user.age.toString() : '');
        setModalError(null);
        setRolePickerOpen(false);
        setModalVisible(true);
    };

    const closeEditor = () => {
        setModalVisible(false);
        setSelectedUser(null);
        setSelectedRole(null);
        setRolePickerOpen(false);
        setModalError(null);
    };

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

    const handleNavigateProfile = () => {
        setMenuVisible(false);
        router.push('/profile');
    };

    const saveUserChanges = async () => {
        if (!selectedUser) return;
        const trimmedEmail = editEmail.trim();
        if (!trimmedEmail) {
            setModalError(t('admin.users.emailRequired'));
            return;
        }

        const profilePayload: AdminUserProfileUpdate = {};

        const trimmedName = editName.trim();
        if (trimmedName !== (selectedUser.name ?? '')) {
            profilePayload.name = trimmedName;
        }

        if (trimmedEmail !== selectedUser.email) {
            profilePayload.email = trimmedEmail;
        }

        const trimmedPhone = editPhoneNumber.trim();
        if (trimmedPhone !== (selectedUser.phoneNumber ?? '')) {
            profilePayload.phoneNumber = trimmedPhone;
        }

        if (editGender !== (selectedUser.gender ?? '')) {
            profilePayload.gender = editGender;
        }

        const nextAge = editAge.trim() ? parseInt(editAge, 10) : null;
        if (nextAge !== null && nextAge !== selectedUser.age) {
            profilePayload.age = nextAge;
        }

        const roleChanged =
            selectedRole &&
            (selectedUser.roles.length !== 1 || selectedUser.roles[0] !== selectedRole);

        const shouldUpdateProfile = Object.keys(profilePayload).length > 0;

        if (!shouldUpdateProfile && !roleChanged) {
            closeEditor();
            return;
        }

        setSaving(true);
        setError(null);
        setModalError(null);
        try {
            let updatedUser = selectedUser;
            if (shouldUpdateProfile) {
                updatedUser = await updateUserProfile(selectedUser.id, profilePayload);
            }
            if (roleChanged && selectedRole) {
                updatedUser = await updateUserRoles(selectedUser.id, [selectedRole]);
            }

            setUsers((current) =>
                current.map((user) => (user.id === updatedUser.id ? updatedUser : user)),
            );
            closeEditor();
        } catch (err) {
            console.error('Failed to update user', err);
            setModalError(t('admin.users.updateError'));
        } finally {
            setSaving(false);
        }
    };

    const updateStatus = async (status: 'ACTIVE' | 'INACTIVE') => {
        if (!selectedUser) return;
        setSaving(true);
        setError(null);
        setModalError(null);
        try {
            const updated = await updateUserStatus(selectedUser.id, status);
            setUsers((current) =>
                current.map((user) => (user.id === updated.id ? updated : user)),
            );
            closeEditor();
        } catch (err) {
            console.error('Failed to update user status', err);
            setModalError(t('admin.users.statusUpdateError'));
        } finally {
            setSaving(false);
        }
    };

    const confirmStatusChange = (status: 'ACTIVE' | 'INACTIVE') => {
        if (!selectedUser) return;
        const isDeactivating = status === 'INACTIVE';
        Alert.alert(
            isDeactivating
                ? t('admin.users.deactivateConfirmTitle')
                : t('admin.users.reactivateConfirmTitle'),
            isDeactivating
                ? t('admin.users.deactivateConfirmMessage')
                : t('admin.users.reactivateConfirmMessage'),
            [
                { text: t('common.cancel'), style: 'cancel' },
                {
                    text: isDeactivating
                        ? t('admin.users.deactivateConfirmAction')
                        : t('admin.users.reactivateConfirmAction'),
                    style: isDeactivating ? 'destructive' : 'default',
                    onPress: () => updateStatus(status),
                },
            ],
        );
    };

    const styles = getStyles(colorScheme);
    const activeCount = activeUsers.length;
    const inactiveCount = inactiveUsers.length;

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
                    <View style={styles.statusRow}>
                        <View
                            style={[
                                styles.statusPill,
                                item.memberStatus === 'INACTIVE'
                                    ? styles.statusPillInactive
                                    : styles.statusPillActive,
                            ]}
                        >
                            <Text
                                style={[
                                    styles.statusPillText,
                                    item.memberStatus === 'INACTIVE'
                                        ? styles.statusPillTextInactive
                                        : styles.statusPillTextActive,
                                ]}
                            >
                                {item.memberStatus === 'INACTIVE'
                                    ? t('admin.users.status.inactive')
                                    : t('admin.users.status.active')}
                            </Text>
                        </View>
                    </View>
                </View>
                <Pressable
                    style={({ pressed }) => [
                        styles.editButton,
                        pressed && styles.editButtonPressed,
                    ]}
                    onPress={() => openEditor(item)}
                    accessibilityRole="button"
                    accessibilityLabel={t('admin.users.edit')}
                    accessibilityHint={t('admin.users.editHint', 'Edits this user')}
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
                    <Pressable
                        style={styles.backButton}
                        onPress={() => router.back()}
                        accessibilityRole="button"
                        accessibilityLabel={t('common.back', 'Go back')}
                    >
                        <Ionicons name="chevron-back" size={18} color={ACCENT} />
                    </Pressable>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.title}>{t('admin.users.title')}</Text>
                        <Text style={styles.subtitle}>{t('admin.users.subtitle')}</Text>
                    </View>
                </View>

                <View style={styles.controls}>
                    <View style={styles.searchBox}>
                        <Ionicons name="search-outline" size={18} color={isDark ? '#8B93A1' : '#5C6A80'} />
                        <TextInput
                            style={styles.searchInput}
                            placeholder={t('admin.users.searchPlaceholder')}
                            placeholderTextColor={isDark ? '#8B93A1' : '#9BA5B7'}
                            value={searchQuery}
                            onChangeText={setSearchQuery}
                            autoCapitalize="none"
                            accessibilityLabel={t('admin.users.searchLabel', 'Search users')}
                        />
                    </View>
                    <View style={styles.tabs}>
                        <Pressable
                            style={[
                                styles.tabButton,
                                activeTab === 'ACTIVE' && styles.tabButtonActive,
                            ]}
                            onPress={() => setActiveTab('ACTIVE')}
                            accessibilityRole="tab"
                            accessibilityLabel={t('admin.users.tabs.active')}
                            accessibilityState={{ selected: activeTab === 'ACTIVE' }}
                        >
                            <Text
                                style={[
                                    styles.tabLabel,
                                    activeTab === 'ACTIVE' && styles.tabLabelActive,
                                ]}
                            >
                                {t('admin.users.tabs.active')}
                            </Text>
                            <View
                                style={[
                                    styles.tabCount,
                                    activeTab === 'ACTIVE' && styles.tabCountActive,
                                ]}
                            >
                                <Text
                                    style={[
                                        styles.tabCountText,
                                        activeTab === 'ACTIVE' && styles.tabCountTextActive,
                                    ]}
                                >
                                    {activeCount}
                                </Text>
                            </View>
                        </Pressable>
                        <Pressable
                            style={[
                                styles.tabButton,
                                activeTab === 'INACTIVE' && styles.tabButtonActive,
                            ]}
                            onPress={() => setActiveTab('INACTIVE')}
                            accessibilityRole="tab"
                            accessibilityLabel={t('admin.users.tabs.disabled')}
                            accessibilityState={{ selected: activeTab === 'INACTIVE' }}
                        >
                            <Text
                                style={[
                                    styles.tabLabel,
                                    activeTab === 'INACTIVE' && styles.tabLabelActive,
                                ]}
                            >
                                {t('admin.users.tabs.disabled')}
                            </Text>
                            <View
                                style={[
                                    styles.tabCount,
                                    activeTab === 'INACTIVE' && styles.tabCountActive,
                                ]}
                            >
                                <Text
                                    style={[
                                        styles.tabCountText,
                                        activeTab === 'INACTIVE' && styles.tabCountTextActive,
                                    ]}
                                >
                                    {inactiveCount}
                                </Text>
                            </View>
                        </Pressable>
                    </View>
                </View>

                {loading ? (
                    <View style={styles.centered}>
                        <ActivityIndicator />
                    </View>
                ) : error ? (
                    <View style={styles.errorBox}>
                        <Ionicons name="alert-circle" size={18} color={isDark ? '#FFB4AB' : '#C62828'} />
                        <Text style={styles.errorText}>{error}</Text>
                    </View>
                ) : (
                    <FlatList
                        data={filteredUsers}
                        keyExtractor={(item) => item.id.toString()}
                        renderItem={renderUserItem}
                        contentContainerStyle={styles.listContent}
                        ListEmptyComponent={
                            <View style={styles.emptyState}>
                                <Ionicons name="people-outline" size={26} color={isDark ? '#8B93A1' : '#9BA5B7'} />
                                <Text style={styles.emptyText}>
                                    {activeTab === 'ACTIVE'
                                        ? t('admin.users.empty')
                                        : t('admin.users.emptyDisabled')}
                                </Text>
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
                        <ScrollView contentContainerStyle={styles.modalContent} keyboardShouldPersistTaps="handled">
                            <View style={styles.modalHeader}>
                                <Ionicons name="person-circle-outline" size={24} color={ACCENT} />
                                <Text style={styles.modalTitle}>{t('admin.users.userDetails')}</Text>
                            </View>

                            <Text style={styles.sectionTitle}>{t('admin.users.personalInfo')}</Text>

                            <View style={styles.inputGroup}>
                                <Text style={styles.inputLabel}>{t('admin.users.fields.name')}</Text>
                                <TextInput
                                    style={styles.input}
                                    value={editName}
                                    onChangeText={setEditName}
                                    placeholder={t('admin.users.placeholders.name')}
                                    placeholderTextColor={isDark ? '#8B93A1' : '#9BA5B7'}
                                    accessibilityLabel={t('admin.users.fields.name')}
                                />
                            </View>

                            <View style={styles.inputGroup}>
                                <Text style={styles.inputLabel}>{t('admin.users.fields.email')}</Text>
                                <TextInput
                                    style={styles.input}
                                    value={editEmail}
                                    onChangeText={setEditEmail}
                                    autoCapitalize="none"
                                    keyboardType="email-address"
                                    placeholder={t('admin.users.placeholders.email')}
                                    placeholderTextColor={isDark ? '#8B93A1' : '#9BA5B7'}
                                    accessibilityLabel={t('admin.users.fields.email')}
                                />
                            </View>

                            <View style={styles.inputGroup}>
                                <Text style={styles.inputLabel}>{t('admin.users.fields.phone')}</Text>
                                <TextInput
                                    style={styles.input}
                                    value={editPhoneNumber}
                                    onChangeText={(text) => setEditPhoneNumber(text.replace(/[^0-9+]/g, ''))}
                                    keyboardType="phone-pad"
                                    placeholder={t('admin.users.placeholders.phone')}
                                    placeholderTextColor={isDark ? '#8B93A1' : '#9BA5B7'}
                                    accessibilityLabel={t('admin.users.fields.phone')}
                                />
                            </View>

                            <View style={styles.inputGroup}>
                                <Text style={styles.inputLabel}>{t('admin.users.fields.gender')}</Text>
                                <View style={styles.pickerContainer}>
                                    <Picker
                                        selectedValue={editGender}
                                        onValueChange={(itemValue) => setEditGender(itemValue)}
                                        accessibilityLabel={t('admin.users.fields.gender')}
                                    >
                                        <Picker.Item label={t('profile.genderOptions.preferNotToSay')} value="PREFER_NOT_TO_SAY" />
                                        <Picker.Item label={t('profile.genderOptions.male')} value="MALE" />
                                        <Picker.Item label={t('profile.genderOptions.female')} value="FEMALE" />
                                        <Picker.Item label={t('profile.genderOptions.other')} value="OTHER" />
                                    </Picker>
                                </View>
                            </View>

                            <View style={styles.inputGroup}>
                                <Text style={styles.inputLabel}>{t('admin.users.fields.age')}</Text>
                                <TextInput
                                    style={styles.input}
                                    value={editAge}
                                    onChangeText={(text) => {
                                        const numericValue = parseInt(text.replace(/[^0-9]/g, ''), 10);
                                        if (!text) {
                                            setEditAge('');
                                        } else if (!isNaN(numericValue)) {
                                            setEditAge(numericValue.toString());
                                        }
                                    }}
                                    keyboardType="numeric"
                                    placeholder={t('admin.users.placeholders.age')}
                                    placeholderTextColor={isDark ? '#8B93A1' : '#9BA5B7'}
                                    maxLength={3}
                                    accessibilityLabel={t('admin.users.fields.age')}
                                />
                            </View>

                            <Text style={styles.fieldLabel}>{t('admin.users.role')}</Text>
                            <Pressable
                                style={styles.dropdown}
                                onPress={() => setRolePickerOpen((open) => !open)}
                                accessibilityRole="button"
                                accessibilityLabel={t('admin.users.role')}
                                accessibilityHint={t('admin.users.selectRole')}
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
                                            accessibilityRole="button"
                                            accessibilityLabel={renderRoleLabel(role)}
                                            accessibilityState={{ selected: selectedRole === role }}
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

                            {modalError && (
                                <View style={styles.modalErrorBox}>
                                    <Ionicons name="alert-circle" size={16} color={isDark ? '#FFB4AB' : '#C62828'} />
                                    <Text style={styles.modalErrorText}>{modalError}</Text>
                                </View>
                            )}

                            <Pressable
                                style={[
                                    styles.statusButton,
                                    selectedUser?.memberStatus === 'INACTIVE'
                                        ? styles.statusButtonActive
                                        : styles.statusButtonDanger,
                                    saving && styles.statusButtonDisabled,
                                ]}
                                disabled={saving}
                                onPress={() =>
                                    confirmStatusChange(
                                        selectedUser?.memberStatus === 'INACTIVE' ? 'ACTIVE' : 'INACTIVE',
                                    )
                                }
                                accessibilityRole="button"
                                accessibilityLabel={
                                    selectedUser?.memberStatus === 'INACTIVE'
                                        ? t('admin.users.reactivate')
                                        : t('admin.users.deactivate')
                                }
                                accessibilityState={{ disabled: saving }}
                            >
                                <Text
                                    style={[
                                        styles.statusButtonText,
                                        selectedUser?.memberStatus === 'INACTIVE'
                                            ? styles.statusButtonTextActive
                                            : styles.statusButtonTextDanger,
                                    ]}
                                >
                                    {selectedUser?.memberStatus === 'INACTIVE'
                                        ? t('admin.users.reactivate')
                                        : t('admin.users.deactivate')}
                                </Text>
                            </Pressable>

                            <View style={styles.modalActions}>
                                <Pressable
                                    style={styles.secondaryButton}
                                    onPress={closeEditor}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('common.cancel')}
                                >
                                    <Text style={styles.secondaryButtonText}>{t('common.cancel')}</Text>
                                </Pressable>
                                <Pressable
                                    style={({ pressed }) => [
                                        styles.primaryButton,
                                        (!selectedRole || saving) && styles.primaryButtonDisabled,
                                        pressed && styles.primaryButtonPressed,
                                    ]}
                                    disabled={!selectedRole || saving}
                                    onPress={saveUserChanges}
                                    accessibilityRole="button"
                                    accessibilityLabel={t('admin.users.save')}
                                    accessibilityState={{ disabled: !selectedRole || saving }}
                                >
                                    {saving ? (
                                        <ActivityIndicator color="#FFFFFF" />
                                    ) : (
                                        <Text style={styles.primaryButtonText}>{t('admin.users.save')}</Text>
                                    )}
                                </Pressable>
                            </View>
                        </ScrollView>
                    </View>
                </View>
            </Modal>

            <NavigationMenu
                visible={menuVisible}
                onClose={() => setMenuVisible(false)}
                onNavigateHome={handleNavigateHome}
                onNavigateEvents={handleNavigateEvents}
                onNavigateAttendance={handleNavigateAttendance}
                onNavigateProfile={handleNavigateProfile}
                onNavigateDashboard={handleNavigateDashboard}
                showAttendance={hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])}
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
    const TEXT = isDark ? '#ECEDEE' : '#0F2848';
    const TEXT_MUTED = isDark ? '#A0A7B1' : '#5C6A80';
    const BG = isDark ? '#1F2328' : '#FFFFFF';
    const BORDER = isDark ? '#2F3A4A' : '#E0E6F0';
    const ERROR_BG = isDark ? '#3A1F1F' : '#FFF0F0';
    const ERROR_BORDER = isDark ? '#6B2A2A' : '#F5C6CB';
    const ERROR = isDark ? '#FFB4AB' : '#C62828';
    const INFO_BG = isDark ? '#1D2A3A' : '#F0F6FF';
    const INFO_BORDER = isDark ? '#2F4B7D' : '#D7E5FF';

    return StyleSheet.create({
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
        backgroundColor: BG,
        borderBottomWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
    },
    backButton: {
        width: 32,
        height: 32,
        borderRadius: 10,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 10,
    },
    title: {
        fontSize: 18,
        fontWeight: '700',
        color: TEXT,
    },
    subtitle: {
        fontSize: 13,
        color: TEXT_MUTED,
        marginTop: 2,
    },
    controls: {
        paddingHorizontal: 16,
        paddingVertical: 12,
        gap: 12,
        backgroundColor: SURFACE,
    },
    searchBox: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        backgroundColor: BG,
        paddingHorizontal: 12,
        paddingVertical: 10,
        borderRadius: 12,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
    },
    searchInput: {
        flex: 1,
        color: TEXT,
        fontSize: 14,
    },
    tabs: {
        flexDirection: 'row',
        backgroundColor: BG,
        borderRadius: 12,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
        overflow: 'hidden',
    },
    tabButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        paddingVertical: 10,
        borderBottomWidth: 2,
        borderBottomColor: 'transparent',
    },
    tabButtonActive: {
        backgroundColor: isDark ? '#1D2A3A' : '#F0F6FF',
        borderBottomWidth: 2,
        borderBottomColor: ACCENT,
    },
    tabLabel: {
        fontSize: 13,
        fontWeight: '600',
        color: TEXT_MUTED,
    },
    tabLabelActive: {
        color: ACCENT,
        fontWeight: '700',
    },
    tabCount: {
        minWidth: 24,
        paddingHorizontal: 6,
        paddingVertical: 2,
        borderRadius: 10,
        backgroundColor: isDark ? '#2F3A4A' : '#E5EAF3',
        alignItems: 'center',
    },
    tabCountActive: {
        backgroundColor: isDark ? '#2F4B7D' : '#D7E5FF',
    },
    tabCountText: {
        fontSize: 12,
        fontWeight: '700',
        color: TEXT_MUTED,
    },
    tabCountTextActive: {
        color: ACCENT,
    },
    centered: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
    errorBox: {
        margin: 16,
        padding: 12,
        backgroundColor: ERROR_BG,
        borderRadius: 10,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: ERROR_BORDER,
    },
    errorText: {
        color: ERROR,
        flex: 1,
        fontSize: 14,
    },
    listContent: {
        padding: 16,
        gap: 12,
    },
    userCard: {
        backgroundColor: BG,
        padding: 14,
        borderRadius: 14,
        shadowColor: '#000',
        shadowOpacity: 0.06,
        shadowRadius: 8,
        shadowOffset: { width: 0, height: 3 },
        elevation: 3,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
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
        backgroundColor: INFO_BG,
        alignItems: 'center',
        justifyContent: 'center',
    },
    userNameList: {
        fontSize: 15,
        fontWeight: '700',
        color: TEXT,
        marginBottom: 2,
    },
    userEmail: {
        fontSize: 13,
        color: TEXT_MUTED,
    },
    rolesRow: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 6,
        marginTop: 4,
    },
    rolePill: {
        backgroundColor: INFO_BG,
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 10,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: INFO_BORDER,
    },
    rolePillText: {
        fontSize: 12,
        fontWeight: '700',
        color: ACCENT,
    },
    statusRow: {
        marginTop: 6,
    },
    statusPill: {
        alignSelf: 'flex-start',
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 10,
        borderWidth: StyleSheet.hairlineWidth,
    },
    statusPillActive: {
        backgroundColor: INFO_BG,
        borderColor: INFO_BORDER,
    },
    statusPillInactive: {
        backgroundColor: ERROR_BG,
        borderColor: ERROR_BORDER,
    },
    statusPillText: {
        fontSize: 11,
        fontWeight: '700',
    },
    statusPillTextActive: {
        color: ACCENT,
    },
    statusPillTextInactive: {
        color: ERROR,
    },
    editButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        paddingHorizontal: 10,
        paddingVertical: 8,
        borderRadius: 10,
        backgroundColor: isDark ? '#1D2A3A' : '#F4F8FF',
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: isDark ? '#2F4B7D' : '#D7E5FF',
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
        color: isDark ? '#8B93A1' : '#6B7285',
        fontSize: 14,
    },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.35)',
        alignItems: 'center',
        justifyContent: 'flex-end',
    },
    modalCard: {
        backgroundColor: BG,
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
        color: TEXT,
    },
    modalContent: {
        gap: 12,
    },
    sectionTitle: {
        fontSize: 14,
        fontWeight: '700',
        color: TEXT,
        marginTop: 4,
    },
    inputGroup: {
        gap: 6,
    },
    inputLabel: {
        fontSize: 13,
        fontWeight: '600',
        color: TEXT,
    },
    input: {
        backgroundColor: isDark ? '#1D2A3A' : '#F7F9FC',
        borderRadius: 12,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
        paddingHorizontal: 12,
        paddingVertical: 10,
        color: TEXT,
    },
    pickerContainer: {
        borderRadius: 12,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
        overflow: 'hidden',
        backgroundColor: isDark ? '#1D2A3A' : '#F7F9FC',
    },
    fieldLabel: {
        fontSize: 13,
        fontWeight: '700',
        color: TEXT,
    },
    dropdown: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: isDark ? '#1D2A3A' : '#F7F9FC',
        borderRadius: 12,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
        paddingHorizontal: 12,
        paddingVertical: 12,
    },
    dropdownValue: {
        color: TEXT,
        fontWeight: '600',
    },
    dropdownList: {
        backgroundColor: BG,
        borderRadius: 12,
        marginTop: 10,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: BORDER,
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
        backgroundColor: isDark ? '#1D2A3A' : '#F6F8FB',
    },
    dropdownItemSelected: {
        backgroundColor: INFO_BG,
    },
    dropdownItemText: {
        color: TEXT,
        fontWeight: '600',
    },
    dropdownItemTextSelected: {
        color: ACCENT,
    },
    modalErrorBox: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        padding: 10,
        borderRadius: 10,
        backgroundColor: ERROR_BG,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: ERROR_BORDER,
    },
    modalErrorText: {
        color: ERROR,
        fontSize: 13,
        flex: 1,
    },
    statusButton: {
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: 10,
        borderWidth: StyleSheet.hairlineWidth,
        paddingVertical: 12,
    },
    statusButtonDanger: {
        backgroundColor: ERROR_BG,
        borderColor: ERROR_BORDER,
    },
    statusButtonActive: {
        backgroundColor: isDark ? '#1F2A22' : '#E7F6ED',
        borderColor: isDark ? '#2F4A3A' : '#C8E6C9',
    },
    statusButtonText: {
        fontSize: 13,
        fontWeight: '700',
    },
    statusButtonTextDanger: {
        color: ERROR,
    },
    statusButtonTextActive: {
        color: isDark ? '#8BE9B7' : '#2E7D32',
    },
    statusButtonDisabled: {
        opacity: 0.6,
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
        borderColor: BORDER,
        backgroundColor: BG,
    },
    secondaryButtonText: {
        color: TEXT,
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
    });
};
