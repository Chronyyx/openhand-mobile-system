import { Tabs } from 'expo-router';
import React from 'react';
import { StyleSheet, Text } from 'react-native';
import { useTranslation } from 'react-i18next';

import { HapticTab } from '@/components/haptic-tab';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

export default function TabLayout() {
  const colorScheme = useColorScheme();
  const { t } = useTranslation();
  const styles = getStyles();

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: Colors[colorScheme ?? 'light'].tint,
        headerShown: false,
        tabBarButton: HapticTab,
        tabBarLabelStyle: styles.tabLabel,
      }}>
      <Tabs.Screen
        name="index"
        options={{
          title: t('tabs.home'),
          tabBarIcon: ({ color }) => <IconSymbol size={28} name="house.fill" color={color} />,
          tabBarLabel: ({ focused, color }) => (
            <Text style={[styles.tabLabel, focused && styles.tabLabelFocused, { color }]}>
              {t('tabs.home')}
            </Text>
          ),
        }}
      />
      <Tabs.Screen
        name="explore"
        options={{
          title: t('tabs.explore'),
          tabBarIcon: ({ color }) => <IconSymbol size={28} name="paperplane.fill" color={color} />,
          tabBarLabel: ({ focused, color }) => (
            <Text style={[styles.tabLabel, focused && styles.tabLabelFocused, { color }]}>
              {t('tabs.explore')}
            </Text>
          ),
        }}
      />
      <Tabs.Screen
        name="notifications"
        options={{
          title: t('notifications.title'),
          tabBarIcon: ({ color }) => <IconSymbol size={28} name="bell.fill" color={color} />,
          tabBarLabel: ({ focused, color }) => (
            <Text style={[styles.tabLabel, focused && styles.tabLabelFocused, { color }]}>
              {t('notifications.title')}
            </Text>
          ),
        }}
      />
    </Tabs>
  );
}

const getStyles = () =>
  StyleSheet.create({
    tabLabel: {
      fontSize: 12,
      fontWeight: '600',
    },
    tabLabelFocused: {
      fontWeight: '800',
      textDecorationLine: 'underline',
    },
  });
