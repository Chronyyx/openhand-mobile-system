import React, { useRef } from 'react';
import { Animated, Image, Pressable, StyleSheet, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

type AppHeaderProps = {
    onMenuPress: () => void;
};

const BLUE = '#0056A8';

export function AppHeader({ onMenuPress }: AppHeaderProps) {
    const menuScale = useRef(new Animated.Value(1)).current;

    const handlePressIn = () => {
        Animated.spring(menuScale, {
            toValue: 0.92,
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
                    <Ionicons name="menu" size={28} color={BLUE} />
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
