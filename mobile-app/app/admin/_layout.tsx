import React from 'react';
import { ActivityIndicator, View, StyleSheet } from 'react-native';
import { Redirect, Stack } from 'expo-router';

import { useAuth } from '../../context/AuthContext';

export default function AdminLayout() {
    const { isLoading, hasRole } = useAuth();

    if (isLoading) {
        return (
            <View style={styles.centered}>
                <ActivityIndicator />
            </View>
        );
    }

    if (!hasRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])) {
        return <Redirect href="/" />;
    }

    return <Stack screenOptions={{ headerShown: false }} />;
}

const styles = StyleSheet.create({
    centered: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
});
