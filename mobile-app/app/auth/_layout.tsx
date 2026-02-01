import { Stack } from 'expo-router';
import { useColorScheme } from 'react-native';

export default function AuthLayout() {
    const colorScheme = useColorScheme();
    const backgroundColor = colorScheme === 'dark' ? '#0F1419' : '#FFFFFF';
    
    return (
        <Stack
            screenOptions={{
                headerShown: false,
                contentStyle: { backgroundColor },
            }}
        />
    );
}
