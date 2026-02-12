import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Platform, StyleSheet, Switch, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';
import { MenuLayout } from '../../components/menu-layout';
import { useAuth } from '../../context/AuthContext';
import { useColorScheme } from '../../hooks/use-color-scheme';
import { getSecuritySettings, updateSecuritySettings } from '../../services/security-settings.service';
import {
  cacheBiometricsEnabled,
  checkBiometricCapability,
  clearBiometricSession,
  runBiometricPrompt,
  storeBiometricRefreshToken,
} from '../../services/biometric-auth.service';

export default function BiometricsSettingsScreen() {
  const { t } = useTranslation();
  const { user, hasRole, isLoading: authLoading } = useAuth();
  const colorScheme = useColorScheme() ?? 'light';
  const styles = getStyles(colorScheme);
  const BLUE = colorScheme === 'dark' ? '#9FC3FF' : '#0056A8';

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [enabled, setEnabled] = useState(false);

  const notify = (title: string, message: string) => {
    if (Platform.OS === 'web') {
      return;
    }
    Alert.alert(title, message);
  };

  const isAllowedRole = hasRole(['ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_EMPLOYEE']);

  const loadSecuritySettings = useCallback(async () => {
    if (!user || authLoading || !isAllowedRole) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    try {
      const settings = await getSecuritySettings();
      setEnabled(settings.biometricsEnabled);
      await cacheBiometricsEnabled(settings.biometricsEnabled);
    } catch (error) {
      notify(t('common.error'), t('settings.biometrics.errors.loadFailed'));
    } finally {
      setIsLoading(false);
    }
  }, [authLoading, isAllowedRole, t, user]);

  useEffect(() => {
    loadSecuritySettings();
  }, [loadSecuritySettings]);

  const disableBiometrics = async () => {
    await clearBiometricSession();
    const response = await updateSecuritySettings(false);
    setEnabled(response.biometricsEnabled);
    await cacheBiometricsEnabled(false);
  };

  const enableBiometrics = async () => {
    if (!user?.refreshToken || !user.id) {
      throw new Error('missing_refresh_token');
    }

    const capability = await checkBiometricCapability();
    if (!capability.available || !capability.enrolled) {
      throw new Error('biometrics_unavailable');
    }

    const result = await runBiometricPrompt(t('settings.biometrics.promptEnable'));
    if (!result.success) {
      throw new Error('biometric_auth_failed');
    }

    await storeBiometricRefreshToken(user.refreshToken, user.id);
    const response = await updateSecuritySettings(true);
    setEnabled(response.biometricsEnabled);
    await cacheBiometricsEnabled(true);
  };

  const handleToggle = async (nextValue: boolean) => {
    if (isSaving || !user) {
      return;
    }
    setIsSaving(true);
    try {
      if (nextValue) {
        await enableBiometrics();
        notify(t('common.success'), t('settings.biometrics.messages.enabled'));
      } else {
        await disableBiometrics();
        notify(t('common.success'), t('settings.biometrics.messages.disabled'));
      }
    } catch (error) {
      if ((error as Error).message === 'biometrics_unavailable') {
        notify(t('common.error'), t('settings.biometrics.errors.unavailable'));
      } else if ((error as Error).message === 'biometric_auth_failed') {
        notify(t('common.error'), t('settings.biometrics.errors.authFailed'));
      } else {
        notify(t('common.error'), t('settings.biometrics.errors.updateFailed'));
      }
      setEnabled(false);
    } finally {
      setIsSaving(false);
    }
  };

  if (!user) {
    return (
      <MenuLayout>
        <View style={styles.container}>
          <View style={styles.header}>
            <Text style={styles.title}>{t('settings.biometrics.title')}</Text>
            <Text style={styles.subtitle}>{t('settings.biometrics.subtitle')}</Text>
          </View>
          <View style={styles.emptyState}>
            <Ionicons name="lock-closed-outline" size={28} color={BLUE} />
            <Text style={styles.emptyText}>{t('common.notAuthenticated')}</Text>
          </View>
        </View>
      </MenuLayout>
    );
  }

  if (!isAllowedRole) {
    return (
      <MenuLayout>
        <View style={styles.container}>
          <View style={styles.header}>
            <Text style={styles.title}>{t('settings.biometrics.title')}</Text>
            <Text style={styles.subtitle}>{t('settings.biometrics.subtitle')}</Text>
          </View>
          <View style={styles.emptyState}>
            <Ionicons name="shield-checkmark-outline" size={28} color={BLUE} />
            <Text style={styles.emptyText}>{t('settings.biometrics.memberOnly')}</Text>
          </View>
        </View>
      </MenuLayout>
    );
  }

  return (
    <MenuLayout>
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>{t('settings.biometrics.title')}</Text>
          <Text style={styles.subtitle}>{t('settings.biometrics.subtitle')}</Text>
        </View>

        {isLoading ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color={BLUE} />
            <Text style={styles.loadingText}>{t('common.loading')}</Text>
          </View>
        ) : (
          <View style={styles.card}>
            <View style={styles.row}>
              <View style={{ flex: 1 }}>
                <Text style={styles.cardTitle}>{t('settings.biometrics.enableLabel')}</Text>
                <Text style={styles.cardSubtitle}>{t('settings.biometrics.enableHint')}</Text>
              </View>
              <Switch
                value={enabled}
                onValueChange={handleToggle}
                disabled={isSaving}
                testID="biometrics-toggle"
                accessibilityLabel={t('settings.biometrics.enableLabel')}
                accessibilityHint={t('settings.biometrics.enableHint')}
                trackColor={{ false: colorScheme === 'dark' ? '#4A5568' : '#D4DBE7', true: colorScheme === 'dark' ? '#6AA9FF' : '#8CC3FF' }}
                thumbColor={enabled ? BLUE : (colorScheme === 'dark' ? '#2A313B' : '#F5F7FB')}
              />
            </View>
          </View>
        )}
      </View>
    </MenuLayout>
  );
}

const getStyles = (scheme: 'light' | 'dark') => {
  const isDark = scheme === 'dark';
  const BLUE = isDark ? '#9FC3FF' : '#0056A8';
  return StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: isDark ? '#111418' : '#F5F7FB',
      padding: 20,
    },
    header: {
      marginBottom: 20,
      marginTop: 12,
    },
    title: {
      fontSize: 26,
      fontWeight: '700',
      color: BLUE,
      marginBottom: 6,
    },
    subtitle: {
      fontSize: 15,
      color: isDark ? '#A0A7B1' : '#4F5D73',
    },
    card: {
      backgroundColor: isDark ? '#151A20' : '#FFFFFF',
      borderRadius: 14,
      padding: 16,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: isDark ? '#2A313B' : '#E4E9F2',
      shadowColor: '#000',
      shadowOpacity: 0.05,
      shadowRadius: 8,
      shadowOffset: { width: 0, height: 2 },
      elevation: 2,
    },
    row: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 12,
    },
    cardTitle: {
      fontSize: 16,
      fontWeight: '600',
      color: isDark ? '#ECEDEE' : '#13233B',
    },
    cardSubtitle: {
      marginTop: 4,
      fontSize: 13,
      color: isDark ? '#A0A7B1' : '#687690',
    },
    loadingContainer: {
      alignItems: 'center',
      marginTop: 40,
      gap: 12,
    },
    loadingText: {
      color: isDark ? '#A0A7B1' : '#5C6A80',
    },
    emptyState: {
      marginTop: 40,
      alignItems: 'center',
      gap: 12,
    },
    emptyText: {
      color: isDark ? '#A0A7B1' : '#5C6A80',
      textAlign: 'center',
    },
  });
};
