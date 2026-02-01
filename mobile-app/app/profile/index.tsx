import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, Pressable, Alert, ActivityIndicator, TextInput } from 'react-native';
import { Picker } from '@react-native-picker/picker'; // Still used for Gender/Language
import { CountryPicker, countryCodes } from 'react-native-country-codes-picker'; // New library
import { AsYouType, parsePhoneNumber, CountryCode as LibCountryCode } from 'libphonenumber-js';
import { Image } from 'expo-image';
import * as ImagePicker from 'expo-image-picker';
import { useRouter, Href } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';
import { AppHeader } from '../../components/app-header';
import { NavigationMenu } from '../../components/navigation-menu';
import { useAuth } from '../../context/AuthContext';
import { ImageUploader } from '../../components/ImageUploader';
import { getProfilePicture, uploadProfilePicture } from '../../services/profile-picture.service';
import { updateProfile } from '../../services/profile.service';
import { API_BASE } from '../../utils/api';
import { useColorScheme } from '../../hooks/use-color-scheme';

const formatGender = (gender: string | undefined) => {
    if (!gender) return '-';
    switch (gender) {
        case 'MALE': return 'Male';
        case 'FEMALE': return 'Female';
        case 'OTHER': return 'Other';
        case 'PREFER_NOT_TO_SAY': return 'Prefer not to say';
        default: return gender.charAt(0).toUpperCase() + gender.slice(1).toLowerCase();
    }
};

const formatLanguage = (lang: string | undefined, t: any) => {
    switch (lang) {
        case 'en': return t('settings.language.languages.english');
        case 'fr': return t('settings.language.languages.french');
        case 'es': return t('settings.language.languages.spanish');
        default: return lang || '-';
    }
};

const formatPhoneDisplay = (phone: string | undefined) => {
    if (!phone) return '-';
    try {
        const phoneNumber = parsePhoneNumber(phone);
        return phoneNumber ? phoneNumber.formatInternational() : phone;
    } catch (e) {
        return phone;
    }
};

const findCountryByPhone = (phone: string | undefined) => {
    if (!phone) return { code: 'CA', dial_code: '+1' }; // Default
    try {
        const phoneNumber = parsePhoneNumber(phone);
        if (phoneNumber && phoneNumber.country) {
            const found = countryCodes.find(c => c.code === phoneNumber.country);
            if (found) return { code: found.code, dial_code: found.dial_code };
            return { code: phoneNumber.country as string, dial_code: '+' + phoneNumber.countryCallingCode };
        }
    } catch (e) {
        // Fallback to searching codes if parse fails (e.g. incomplete number)
    }

    // Sort by length desc so we match +1242 before +1
    const sortedCodes = [...countryCodes].sort((a, b) => b.dial_code.length - a.dial_code.length);
    const found = sortedCodes.find(c => phone.startsWith(c.dial_code));
    if (found) {
        return { code: found.code, dial_code: found.dial_code };
    }
    return { code: 'CA', dial_code: '+1' };
};

const resolveProfileUrl = (url: string | null | undefined) => {
    if (!url) return undefined;
    if (url.startsWith('http')) return url;
    // API_BASE includes /api, we need the root host
    const host = API_BASE.replace(/\/api$/, '');
    return `${host}${url.startsWith('/') ? '' : '/'}${url}`;
};

export default function ProfileScreen() {
    const router = useRouter();
    const { t, i18n } = useTranslation();
    const { user, signOut, hasRole, updateUser, isLoading } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const [menuVisible, setMenuVisible] = useState(false);
    const [profilePictureUrl, setProfilePictureUrl] = useState<string | null>(
        user?.profilePictureUrl ?? null
    );
    const [isUploading, setIsUploading] = useState(false);

    const [isEditing, setIsEditing] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [editName, setEditName] = useState(user?.name || '');
    // Phone state
    const [countryCode, setCountryCode] = useState('CA'); // Code like CA, US
    const [callingCode, setCallingCode] = useState('+1'); // Dial code like +1
    const [editPhoneBody, setEditPhoneBody] = useState('');
    const [showCountryPicker, setShowCountryPicker] = useState(false);

    const [editLanguage, setEditLanguage] = useState(user?.preferredLanguage || 'en');
    const [editGender, setEditGender] = useState(user?.gender || 'PREFER_NOT_TO_SAY');
    const [editAge, setEditAge] = useState(user?.age?.toString() || '');

    const styles = getStyles(colorScheme);
    const ACCENT = colorScheme === 'dark' ? '#9FC3FF' : '#0056A8';
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

    const handleNavigateAttendance = () => {
        setMenuVisible(false);
        router.push('/admin/attendance');
    };

    useEffect(() => {
        setProfilePictureUrl(user?.profilePictureUrl ?? null);
        if (user) {
            setEditName(user.name || '');

            // Initialize Phone
            if (user.phoneNumber) {
                const { code, dial_code } = findCountryByPhone(user.phoneNumber);
                setCountryCode(code);
                setCallingCode(dial_code);
                // Remove the dial code from the body and format it
                const rawBody = user.phoneNumber.substring(dial_code.length).trim();
                const formattedBody = new AsYouType(code as LibCountryCode).input(rawBody);
                setEditPhoneBody(formattedBody);
            } else {
                setCountryCode('CA');
                setCallingCode('+1');
                setEditPhoneBody('');
            }

            if ((user as any).preferredLanguage) {
                setEditLanguage((user as any).preferredLanguage);
            }
            setEditGender(user.gender || 'PREFER_NOT_TO_SAY');
            setEditAge(user.age?.toString() || '');
        }
    }, [user]);


    const handleSaveProfile = async () => {
        if (!user || isLoading) return;
        setIsSaving(true);
        try {
            // Clean payload: +15141231234 (unformatted)
            const cleanBody = editPhoneBody.replace(/\D/g, '');
            const finalPhone = cleanBody ? `${callingCode}${cleanBody}` : undefined; // e.g. +15141231234, +33612345678

            const updatedData = {
                name: editName,
                phoneNumber: finalPhone ? finalPhone.trim() : undefined,
                preferredLanguage: editLanguage,
                gender: editGender,
                age: editAge ? parseInt(editAge, 10) : undefined
            };
            const response = await updateProfile(updatedData);

            // Update language immediately
            if (editLanguage !== user.preferredLanguage) {
                await i18n.changeLanguage(editLanguage);
            }

            // Update local context
            await updateUser({
                name: response.name,
                phoneNumber: response.phoneNumber,
                preferredLanguage: response.preferredLanguage,
                gender: response.gender,
                age: response.age
            });

            setIsEditing(false);
            Alert.alert(t('common.success'), t('profile.updateSuccess'));
        } catch (error) {
            console.error('[Profile] Update failed', error);
            Alert.alert(t('common.error'), t('profile.updateError'));
        } finally {
            setIsSaving(false);
        }
    };

    const handleCancelEdit = () => {
        setIsEditing(false);
        if (user) {
            setEditName(user.name || '');
            // Reset phone
            if (user.phoneNumber) {
                const { code, dial_code } = findCountryByPhone(user.phoneNumber);
                setCountryCode(code);
                setCallingCode(dial_code);
                const rawBody = user.phoneNumber.substring(dial_code.length).trim();
                const formattedBody = new AsYouType(code as LibCountryCode).input(rawBody);
                setEditPhoneBody(formattedBody);
            } else {
                setCountryCode('CA');
                setCallingCode('+1');
                setEditPhoneBody('');
            }

            if ((user as any).preferredLanguage) {
                setEditLanguage((user as any).preferredLanguage);
            }
            setEditGender(user.gender || 'PREFER_NOT_TO_SAY');
            setEditAge(user.age?.toString() || '');
        }
    };

    // handleChangeProfilePicture removed (handled by ImageUploader)
    const handleProfileUploadSuccess = async (url: string) => {
        setProfilePictureUrl(url);
        await updateUser({ profilePictureUrl: url });
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
                    onNavigateAttendance={handleNavigateAttendance}
                    onNavigateProfile={() => setMenuVisible(false)}
                    onNavigateMyRegistrations={handleNavigateMyRegistrations}
                    showMyRegistrations={false}
                    showAttendance={false}
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
                    <View style={{ flex: 1 }} />
                    {!isEditing ? (
                        <Pressable style={styles.editButton} onPress={() => setIsEditing(true)}>
                            <Ionicons name="create-outline" size={20} color={ACCENT} />
                            <Text style={styles.editButtonText}>{t('common.edit')}</Text>
                        </Pressable>
                    ) : (
                        <View style={{ flexDirection: 'row', gap: 8 }}>
                            <Pressable disabled={isSaving} onPress={\CancelEdit}>
                                <Text style={[styles.editButtonText, { color: colorScheme === 'dark' ? '#A0A7B1' : '#666' }]}>{t('common.cancel')}</Text>
                            </Pressable>
                            {isSaving ? (
                                <ActivityIndicator size="small" color={ACCENT} />
                            ) : (
                                <Pressable onPress={handleSaveProfile}>
                                    <Text style={[styles.editButtonText, { fontWeight: 'bold' }]}>{t('common.save')}</Text>
                                </Pressable>
                            )}
                        </View>
                    )}
                </View>

                <View style={styles.card}>
                    <View style={styles.avatarContainer}>
                        <ImageUploader
                            imageUrl={profilePictureUrl}
                            onUploadSuccess={handleProfileUploadSuccess}
                            uploadFunction={uploadProfilePicture}
                            editable={!isUploading && !isEditing}
                            // Styling passed to match existing
                            containerStyle={{ marginBottom: 16 }}
                            imageStyle={styles.avatar} // Reusing avatar style (80x80 circle)
                        />

                        {isEditing ? (
                            <TextInput
                                style={styles.inputName}
                                value={editName}
                                onChangeText={setEditName}
                                placeholder={t('profile.namePlaceholder')}
                            />
                        ) : (
                            <Text style={styles.userName}>{user.name || user.email}</Text>
                        )}
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
                        {isEditing ? (
                            <View style={styles.infoItem}>
                                <View style={styles.iconContainer}>
                                    <Ionicons name="call-outline" size={20} color={ACCENT} />
                                </View>
                                <View style={{ flex: 1 }}>
                                    <Text style={styles.infoLabel}>{t('profile.phone')}</Text>
                                    <View style={{ flexDirection: 'row', gap: 10, alignItems: 'center' }}>
                                        <Pressable
                                            onPress={() => setShowCountryPicker(true)}
                                            style={styles.countryPickerButton}
                                        >
                                            <Text style={styles.callingCodeText}>{callingCode}</Text>
                                        </Pressable>

                                        <CountryPicker
                                            show={showCountryPicker}
                                            pickerButtonOnPress={(item) => {
                                                setCallingCode(item.dial_code);
                                                setCountryCode(item.code);
                                                setShowCountryPicker(false);
                                            }}
                                            lang={editLanguage}
                                            onBackdropPress={() => setShowCountryPicker(false)}
                                            style={{
                                                modal: {
                                                    height: 500,
                                                },
                                            }}
                                        />

                                        <TextInput
                                            style={[styles.input, { flex: 1 }]}
                                            value={editPhoneBody}
                                            onChangeText={(text) => {
                                                const asYouType = new AsYouType(countryCode as LibCountryCode);
                                                const formatted = asYouType.input(text);
                                                setEditPhoneBody(formatted);
                                            }}
                                            placeholder="(123) 456-7890"
                                            keyboardType="phone-pad"
                                            maxLength={20}
                                        />
                                    </View>
                                </View>
                            </View>
                        ) : (
                            <InfoItem icon="call-outline" label={t('profile.phone')} value={formatPhoneDisplay(user.phoneNumber)} colorScheme={colorScheme} />
                        )}

                        {isEditing ? (
                            <View style={styles.infoItem}>
                                <View style={styles.iconContainer}>
                                    <Ionicons name="male-female-outline" size={20} color={ACCENT} />
                                </View>
                                <View style={{ flex: 1 }}>
                                    <Text style={styles.infoLabel}>{t('profile.gender')}</Text>
                                    <View style={styles.pickerContainer}>
                                        <Picker
                                            selectedValue={editGender}
                                            onValueChange={(itemValue) => setEditGender(itemValue)}
                                        >
                                            <Picker.Item label="Prefer not to say" value="PREFER_NOT_TO_SAY" />
                                            <Picker.Item label="Male" value="MALE" />
                                            <Picker.Item label="Female" value="FEMALE" />
                                            <Picker.Item label="Other" value="OTHER" />
                                        </Picker>
                                    </View>
                                </View>
                            </View>
                        ) : (
                            <InfoItem icon="male-female-outline" label={t('profile.gender')} value={formatGender(user.gender)} colorScheme={colorScheme} />
                        )}

                        {isEditing ? (
                            <View style={styles.infoItem}>
                                <View style={styles.iconContainer}>
                                    <Ionicons name="calendar-outline" size={20} color={ACCENT} />
                                </View>
                                <View style={{ flex: 1 }}>
                                    <Text style={styles.infoLabel}>{t('profile.age')}</Text>
                                    <TextInput
                                        style={styles.input}
                                        value={editAge}
                                        onChangeText={(text) => {
                                            const numericValue = parseInt(text.replace(/[^0-9]/g, ''), 10);
                                            if (!text) {
                                                setEditAge('');
                                            } else if (!isNaN(numericValue) && numericValue >= 1 && numericValue <= 120) {
                                                setEditAge(numericValue.toString());
                                            }
                                        }}
                                        placeholder="13-120"
                                        keyboardType="numeric"
                                        maxLength={3}
                                    />
                                </View>
                            </View>
                        ) : (
                            <InfoItem icon="calendar-outline" label={t('profile.age')} value={user.age ? user.age.toString() : '-'} colorScheme={colorScheme} />
                        )}

                        {isEditing ? (
                            <View style={styles.infoItem}>
                                <View style={styles.iconContainer}>
                                    <Ionicons name="language-outline" size={20} color={ACCENT} />
                                </View>
                                <View style={{ flex: 1 }}>
                                    <Text style={styles.infoLabel}>{t('profile.preferredLanguage')}</Text>
                                    <View style={styles.pickerContainer}>
                                        <Picker
                                            selectedValue={editLanguage}
                                            onValueChange={(itemValue) => setEditLanguage(itemValue)}
                                        >
                                            <Picker.Item label="English" value="en" />
                                            <Picker.Item label="Français" value="fr" />
                                            <Picker.Item label="Español" value="es" />
                                        </Picker>
                                    </View>
                                </View>
                            </View>
                        ) : (
                            <InfoItem icon="language-outline" label={t('profile.preferredLanguage')} value={formatLanguage((user as any).preferredLanguage, t)} colorScheme={colorScheme} />
                        )}


                    </View>
                </View>

                {hasRole(['ROLE_MEMBER']) && (
                    <View style={styles.settingsCard}>
                        <Pressable
                            style={styles.settingsItem}
                            onPress={() => router.push('/settings/notifications' as Href)}
                        >
                            <View style={styles.settingsItemLeft}>
                                <Ionicons name="notifications-outline" size={20} color={ACCENT} />
                                <Text style={styles.settingsItemText}>{t('settings.notifications.title')}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={ACCENT} />
                        </Pressable>

                        <Pressable
                            style={[styles.settingsItem, styles.settingsDangerItem]}
                            onPress={() => router.push('/settings/deactivate' as Href)}
                        >
                            <View style={styles.settingsItemLeft}>
                                <Ionicons name="alert-circle-outline" size={20} color={colorScheme === 'dark' ? '#FFB4AB' : '#C62828'} />
                                <Text style={[styles.settingsItemText, { color: colorScheme === 'dark' ? '#FFB4AB' : '#C62828' }]}>
                                    {t('settings.account.deactivateLink')}
                                </Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={colorScheme === 'dark' ? '#FFB4AB' : '#C62828'} />
                        </Pressable>
                    </View>
                )}

                <Pressable style={styles.logoutButton} onPress={signOut}>
                    <Ionicons name="log-out-outline" size={20} color={colorScheme === 'dark' ? '#FFB4AB' : '#C62828'} />
                    <Text style={styles.logoutButtonText}>{t('profile.logout')}</Text>
                </Pressable>

            </ScrollView>

            <NavigationMenu
                visible={menuVisible}
                onClose={() => setMenuVisible(false)}
                onNavigateHome={handleNavigateHome}
                onNavigateEvents={handleNavigateEvents}
                onNavigateAttendance={handleNavigateAttendance}
                onNavigateProfile={() => setMenuVisible(false)}
                onNavigateMyRegistrations={handleNavigateMyRegistrations}
                showMyRegistrations={!!user}
                showAttendance={hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])}
                showDashboard={hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])}
                onNavigateDashboard={handleNavigateDashboard}
                t={t}
            />
        </View>
    );
}

function InfoItem({ icon, label, value, colorScheme }: { icon: keyof typeof Ionicons.glyphMap; label: string; value: string; colorScheme: 'light' | 'dark' }) {
    const styles = getStyles(colorScheme);
    const ACCENT = colorScheme === 'dark' ? '#9FC3FF' : '#0056A8';
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

const getStyles = (scheme: 'light' | 'dark') => {
    const isDark = scheme === 'dark';
    const ACCENT = isDark ? '#9FC3FF' : '#0056A8';
    const SURFACE = isDark ? '#111418' : '#F5F7FB';
    
    return StyleSheet.create({
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
        color: isDark ? '#ECEDEE' : '#0F2848',
    },
    editButton: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 8,
        gap: 4,
    },
    editButtonText: {
        color: ACCENT,
        fontSize: 14,
        fontWeight: '600',
    },
    card: {
        backgroundColor: isDark ? '#151A20' : '#FFFFFF',
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
        borderColor: isDark ? '#2A313B' : '#E0E6F0',
        paddingBottom: 24,
    },
    avatar: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: isDark ? '#1F2A3A' : '#EAF1FF',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 16,
        overflow: 'hidden',
    },
    avatarImage: {
        width: '100%',
        height: '100%',
    },
    pictureButton: {
        backgroundColor: ACCENT,
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 16,
        marginBottom: 16,
        minWidth: 160,
        alignItems: 'center',
    },
    pictureButtonText: {
        color: '#FFFFFF',
        fontWeight: '600',
        fontSize: 14,
    },
    userName: {
        fontSize: 20,
        fontWeight: '700',
        color: isDark ? '#ECEDEE' : '#0F2848',
        marginBottom: 4,
    },
    userEmail: {
        fontSize: 14,
        color: isDark ? '#A0A7B1' : '#5C6A80',
        marginBottom: 12,
    },
    rolesRow: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8,
    },
    rolePill: {
        backgroundColor: isDark ? '#1F2A3A' : '#F0F6FF',
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
        backgroundColor: isDark ? '#1F2A3A' : '#F5F9FF',
        alignItems: 'center',
        justifyContent: 'center',
    },
    infoLabel: {
        fontSize: 12,
        color: isDark ? '#A0A7B1' : '#5C6A80',
        marginBottom: 2,
    },
    infoValue: {
        fontSize: 16,
        color: isDark ? '#ECEDEE' : '#0F2848',
        fontWeight: '500',
    },
    logoutButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: isDark ? '#3A1F1F' : '#FFF0F0',
        padding: 16,
        borderRadius: 12,
        gap: 8,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: isDark ? '#6B2A2A' : '#FFDBDB',
    },
    logoutButtonText: {
        color: isDark ? '#FFB4AB' : '#C62828',
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
        backgroundColor: isDark ? '#151A20' : '#FFFFFF',
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
        backgroundColor: isDark ? '#1F2A3A' : '#F8FAFE',
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: isDark ? '#2F3A4A' : '#D9E5FF',
    },
    settingsDangerItem: {
        marginTop: 8,
        backgroundColor: isDark ? '#3A1F1F' : '#FEF2F2',
        borderColor: isDark ? '#6B2A2A' : '#F8BBD0',
    },
    settingsItemLeft: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
    },
    settingsItemText: {
        fontSize: 15,
        fontWeight: '600',
        color: isDark ? '#ECEDEE' : '#1A2D4A',
    },
    input: {
        borderWidth: 1,
        borderColor: isDark ? '#2F3A4A' : '#D9E5FF',
        borderRadius: 8,
        padding: 8,
        fontSize: 16,
        color: isDark ? '#ECEDEE' : '#0F2848',
        backgroundColor: isDark ? '#1F2A3A' : '#F8FAFE',
    },
    inputName: {
        borderWidth: 1,
        borderColor: isDark ? '#2F3A4A' : '#D9E5FF',
        borderRadius: 8,
        padding: 8,
        fontSize: 20,
        fontWeight: '700',
        color: isDark ? '#ECEDEE' : '#0F2848',
        marginBottom: 4,
        textAlign: 'center',
        backgroundColor: isDark ? '#1F2A3A' : '#F8FAFE',
        minWidth: 200,
    },
    pickerContainer: {
        borderWidth: 1,
        borderColor: isDark ? '#2F3A4A' : '#D9E5FF',
        borderRadius: 8,
        backgroundColor: isDark ? '#1F2A3A' : '#F8FAFE',
        overflow: 'hidden',
    },
    countryPickerContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        borderWidth: 1,
        borderColor: isDark ? '#2F3A4A' : '#D9E5FF',
        borderRadius: 8,
        backgroundColor: isDark ? '#1F2A3A' : '#F8FAFE',
        paddingHorizontal: 8,
        height: 50, // Match input height roughly
    },
    countryPickerButton: {
        justifyContent: 'center',
        alignItems: 'center',
        borderWidth: 1,
        borderColor: isDark ? '#2F3A4A' : '#D9E5FF',
        borderRadius: 8,
        backgroundColor: isDark ? '#1F2A3A' : '#F8FAFE',
        paddingHorizontal: 12,
        height: 50,
        minWidth: 80,
    },
    callingCodeText: {
        fontSize: 16,
        color: isDark ? '#ECEDEE' : '#0F2848',
        fontWeight: '500',
    },
    });
};
