import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Alert, StyleSheet, Switch, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import {
  getNotificationPreferences,
  updateNotificationPreferences,
  type NotificationPreference,
  type NotificationPreferenceCategory,
} from '../../services/notification-preferences.service';

const BLUE = '#0056A8';
const LIGHT_BG = '#F5F7FB';

const CATEGORY_ORDER: NotificationPreferenceCategory[] = [
  'CONFIRMATION',
  'REMINDER',
  'CANCELLATION',
];

export default function NotificationPreferencesScreen() {
  const { t } = useTranslation();
  const { user, hasRole } = useAuth();
  const [preferences, setPreferences] = useState<NotificationPreference[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isMember = hasRole(['ROLE_MEMBER']);

  const labels = useMemo(() => ({
    CONFIRMATION: t('settings.notifications.categories.confirmation'),
    REMINDER: t('settings.notifications.categories.reminder'),
    CANCELLATION: t('settings.notifications.categories.cancellation'),
  }), [t]);

  const sortedPreferences = useMemo(() => {
    return [...preferences].sort((a, b) => {
      return CATEGORY_ORDER.indexOf(a.category) - CATEGORY_ORDER.indexOf(b.category);
    });
  }, [preferences]);

  const loadPreferences = useCallback(async () => {
    if (!user?.token) {
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const data = await getNotificationPreferences(user.token);
      setPreferences(data.preferences);
    } catch (err) {
      setError(t('settings.notifications.errors.loadFailed'));
    } finally {
      setIsLoading(false);
    }
  }, [user?.token, t]);

  useEffect(() => {
    if (!user?.token || !isMember) {
      setIsLoading(false);
      return;
    }
    loadPreferences();
  }, [user?.token, isMember, loadPreferences]);

  const handleToggle = async (category: NotificationPreferenceCategory, enabled: boolean) => {
    if (!user?.token || isSaving) {
      return;
    }
    const previous = preferences;
    const next = preferences.map((pref) =>
      pref.category === category ? { ...pref, enabled } : pref
    );
    setPreferences(next);
    setIsSaving(true);
    try {
      const updates = next.map((pref) => ({
        category: pref.category,
        enabled: pref.enabled,
      }));
      const response = await updateNotificationPreferences(user.token, updates);
      setPreferences(response.preferences);
    } catch (err) {
      setPreferences(previous);
      Alert.alert(t('common.error'), t('settings.notifications.errors.updateFailed'));
    } finally {
      setIsSaving(false);
    }
  };

  if (!user) {
    return (
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>{t('settings.notifications.title')}</Text>
          <Text style={styles.subtitle}>{t('settings.notifications.subtitle')}</Text>
        </View>
        <View style={styles.emptyState}>
          <Ionicons name="lock-closed-outline" size={28} color={BLUE} />
          <Text style={styles.emptyText}>{t('common.notAuthenticated')}</Text>
        </View>
      </View>
    );
  }

  if (!isMember) {
    return (
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>{t('settings.notifications.title')}</Text>
          <Text style={styles.subtitle}>{t('settings.notifications.subtitle')}</Text>
        </View>
        <View style={styles.emptyState}>
          <Ionicons name="shield-checkmark-outline" size={28} color={BLUE} />
          <Text style={styles.emptyText}>{t('settings.notifications.memberOnly')}</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>{t('settings.notifications.title')}</Text>
        <Text style={styles.subtitle}>{t('settings.notifications.subtitle')}</Text>
      </View>

      {isLoading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={BLUE} />
          <Text style={styles.loadingText}>{t('common.loading')}</Text>
        </View>
      ) : (
        <View style={styles.list}>
          {error && <Text style={styles.errorText}>{error}</Text>}
          {sortedPreferences.map((pref) => (
            <View key={pref.category} style={styles.preferenceCard}>
              <View style={styles.preferenceRow}>
                <View>
                  <Text style={styles.preferenceTitle}>{labels[pref.category]}</Text>
                  {pref.isCritical && (
                    <Text style={styles.preferenceHint}>
                      {t('settings.notifications.mandatory')}
                    </Text>
                  )}
                </View>
                <Switch
                  value={pref.enabled || pref.isCritical}
                  onValueChange={(value) => handleToggle(pref.category, value)}
                  disabled={pref.isCritical || isSaving}
                  trackColor={{ false: '#D4DBE7', true: '#8CC3FF' }}
                  thumbColor={pref.enabled ? BLUE : '#F5F7FB'}
                  testID={`notification-toggle-${pref.category.toLowerCase()}`}
                />
              </View>
              {pref.isCritical && (
                <Text style={styles.helperText}>{t('settings.notifications.mandatoryHint')}</Text>
              )}
            </View>
          ))}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: LIGHT_BG,
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
    color: '#4F5D73',
  },
  list: {
    gap: 14,
  },
  preferenceCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 14,
    padding: 16,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E4E9F2',
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  preferenceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  preferenceTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#13233B',
  },
  preferenceHint: {
    fontSize: 12,
    color: '#687690',
    marginTop: 4,
  },
  helperText: {
    marginTop: 10,
    fontSize: 12,
    color: '#7C8AA5',
  },
  loadingContainer: {
    alignItems: 'center',
    marginTop: 40,
    gap: 12,
  },
  loadingText: {
    color: '#5C6A80',
  },
  emptyState: {
    marginTop: 40,
    alignItems: 'center',
    gap: 12,
  },
  emptyText: {
    color: '#5C6A80',
    textAlign: 'center',
  },
  errorText: {
    color: '#C62828',
    marginBottom: 6,
  },
});
