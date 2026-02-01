import { StyleSheet, useColorScheme } from "react-native";
import { MenuLayout } from "../../components/menu-layout";
import { ThemedText } from "../../components/themed-text";
import { ThemedView } from "../../components/themed-view";

export default function Gala2025Screen() {
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    return (
        <MenuLayout>
            <ThemedView style={[styles.container, { backgroundColor: isDark ? '#0F1419' : '#F5F7FB' }]}
            >
                <ThemedText style={styles.title}>Gala MANA 2025</ThemedText>
                <ThemedText>TODO: details + reservation flow.</ThemedText>
            </ThemedView>
        </MenuLayout>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, justifyContent: "center", alignItems: "center" },
    title: { fontSize: 22, fontWeight: "700", marginBottom: 8 },
});
