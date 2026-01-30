import React, { useRef } from 'react';
import { Animated, Image, Pressable, StyleSheet, View, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useColorScheme } from '../hooks/use-color-scheme';

type AppHeaderProps = {
    onMenuPress: () => void;
};

import { useNotifications } from '@/hooks/useNotifications';



export function AppHeader({ onMenuPress }: AppHeaderProps) {
    const menuScale = useRef(new Animated.Value(1)).current;
    const { unreadCount } = useNotifications();
    const colorScheme = useColorScheme() ?? 'light';
    const styles = getStyles(colorScheme);
    const palette = {
        primary: colorScheme === 'dark' ? '#9FC3FF' : '#0056A8',
        surface: colorScheme === 'dark' ? '#151A20' : '#FFFFFF',
        danger: colorScheme === 'dark' ? '#FFB4AB' : '#FF3B30',
        surfaceBorder: colorScheme === 'dark' ? '#2A313B' : '#FFFFFF',
    };

    const handlePressIn = () => {
        Animated.spring(menuScale, {
            toValue: 0.9,
            useNativeDriver: true,
            speed: 20,
            bounciness: 6,
        }).start();
    };

    const handlePressOut = () => {
        Animated.spring(menuScale, {
            toValue: 1,
            useNativeDriver: true,
            speed: 20,
            bounciness: 8,
        }).start();
    };



    return (
        <View style={styles.header}>
            <Image
                source={require('../assets/mana/manaLogo.png')}
                style={styles.logo}
                resizeMode="contain"
            />
            <Pressable
                onPress={onMenuPress}
                onPressIn={handlePressIn}
                onPressOut={handlePressOut}
                hitSlop={12}
                style={({ pressed }) => [
                    styles.menuButton,
                    pressed && styles.menuButtonPressed,
                ]}
            >
                <Animated.View style={{ transform: [{ scale: menuScale }] }}>
                    <Ionicons name="menu" size={28} color={palette.primary} />
                    {unreadCount > 0 && (
                        <View
                            style={{
                                position: 'absolute',
                                top: -2,
                                right: -2,
                                backgroundColor: palette.danger,
                                borderRadius: 8,
                                minWidth: 16,
                                height: 16,
                                alignItems: 'center',
                                justifyContent: 'center',
                                paddingHorizontal: 4,
                                borderWidth: 1.5,
                                borderColor: palette.surfaceBorder,
                            }}
                        >
                            <Text style={{ color: 'white', fontSize: 10, fontWeight: 'bold' }}>
                                {unreadCount > 9 ? '9+' : unreadCount}
                            </Text>
                        </View>
                    )}
                </Animated.View>
            </Pressable>
        </View>
    );
}

const getStyles = (scheme: 'light' | 'dark') => {
    const isDark = scheme === 'dark';
    return StyleSheet.create({
        header: {
            paddingHorizontal: 16,
            paddingTop: 40,
            paddingBottom: 12,
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor: isDark ? '#151A20' : '#FFFFFF',
        },
        logo: {
            width: 170,
            height: 40,
        },
        menuButton: {
            padding: 6,
            borderRadius: 18,
            backgroundColor: isDark ? '#151A20' : '#FFFFFF',
        },
        menuButtonPressed: {
            backgroundColor: isDark ? 'rgba(159,195,255,0.15)' : 'rgba(0,86,168,0.08)',
        },
    });
};
