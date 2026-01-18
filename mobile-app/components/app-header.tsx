import React, { useRef } from 'react';
import { Animated, Image, Pressable, StyleSheet, View, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

type AppHeaderProps = {
    onMenuPress: () => void;
};

const BLUE = '#0056A8';

import { useNotifications } from '@/hooks/useNotifications';



export function AppHeader({ onMenuPress }: AppHeaderProps) {
    const menuScale = useRef(new Animated.Value(1)).current;
    const { unreadCount } = useNotifications();

    const handlePressIn = () => {
        Animated.spring(menuScale, {
            toValue: 0.9,
            useNativeDriver: true,
        }).start();
    };

    const handlePressOut = () => {
        Animated.spring(menuScale, {
            toValue: 1,
            useNativeDriver: true,
        }).start();
    };

    // ... (rest of hook body requires reconstructing or careful replacement)
    // Easier to just replace the Pressable content or the whole component body if small.
    // I will target the Pressable part.

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
                    <Ionicons name="menu" size={28} color={BLUE} />
                    {unreadCount > 0 && (
                        <View
                            style={{
                                position: 'absolute',
                                top: -2,
                                right: -2,
                                backgroundColor: '#FF3B30',
                                borderRadius: 8,
                                minWidth: 16,
                                height: 16,
                                alignItems: 'center',
                                justifyContent: 'center',
                                paddingHorizontal: 4,
                                borderWidth: 1.5,
                                borderColor: '#FFFFFF',
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

const styles = StyleSheet.create({
    header: {
        paddingHorizontal: 16,
        paddingTop: 40,
        paddingBottom: 12,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: '#FFFFFF',
    },
    logo: {
        width: 170,
        height: 40,
    },
    menuButton: {
        padding: 6,
        borderRadius: 18,
        backgroundColor: '#FFFFFF',
    },
    menuButtonPressed: {
        backgroundColor: 'rgba(0,86,168,0.08)',
    },
});
