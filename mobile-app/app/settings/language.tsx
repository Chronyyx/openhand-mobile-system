import { View, Text, StyleSheet } from "react-native";

export default function LanguageSettingsScreen() {
    return (
        <View style={styles.container}>
            <Text style={styles.title}>Change Language</Text>
            <Text>TODO: choose between FR / EN / ES, etc.</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, justifyContent: "center", alignItems: "center" },
    title: { fontSize: 22, fontWeight: "700", marginBottom: 8 },
});
