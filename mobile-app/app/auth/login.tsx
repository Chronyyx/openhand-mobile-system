import { View, Text, StyleSheet } from "react-native";
import { useTranslation } from "react-i18next";

export default function LoginScreen() {
        const { t } = useTranslation();

    return (
        <View style={styles.container}>
            <Text style={styles.title}>{t("auth.title")}</Text>
            <Text>{t("auth.placeholder")}</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, justifyContent: "center", alignItems: "center" },
    title: { fontSize: 22, fontWeight: "700", marginBottom: 8 },
});
