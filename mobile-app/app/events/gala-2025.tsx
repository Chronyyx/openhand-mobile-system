import { View, Text, StyleSheet } from "react-native";
import { MenuLayout } from "../../components/menu-layout";

export default function Gala2025Screen() {
    return (
        <MenuLayout>
            <View style={styles.container}>
                <Text style={styles.title}>Gala MANA 2025</Text>
                <Text>TODO: details + reservation flow.</Text>
            </View>
        </MenuLayout>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, justifyContent: "center", alignItems: "center" },
    title: { fontSize: 22, fontWeight: "700", marginBottom: 8 },
});
