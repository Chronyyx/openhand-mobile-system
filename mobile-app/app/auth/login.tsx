import { View, Text, StyleSheet } from "react-native";

export default function LoginScreen() {
    return (
        <View style={styles.container}>
            <Text style={styles.title}>Log In / Register</Text>
            <Text>TODO: build authentication form.</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, justifyContent: "center", alignItems: "center" },
    title: { fontSize: 22, fontWeight: "700", marginBottom: 8 },
});
