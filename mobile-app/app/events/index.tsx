import { View, Text, StyleSheet } from "react-native";

export default function EventsScreen() {
    return (
        <View style={styles.container}>
            <Text style={styles.title}>Browse Events</Text>
            <Text>TODO: list events from the backend here.</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, justifyContent: "center", alignItems: "center" },
    title: { fontSize: 22, fontWeight: "700", marginBottom: 8 },
});
