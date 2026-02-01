import { View, Text, StyleSheet, Pressable, Alert } from "react-native";
import { useTranslation } from "react-i18next";
import { Ionicons } from "@expo/vector-icons";
import { useState } from "react";
import { MenuLayout } from "../../components/menu-layout";
import { useColorScheme } from "../../hooks/use-color-scheme";

export default function LanguageSettingsScreen() {
        const { t, i18n } = useTranslation();
        const [selectedLanguage, setSelectedLanguage] = useState(i18n.language);
        const colorScheme = useColorScheme() ?? 'light';
        const styles = getStyles(colorScheme);
        const BLUE = colorScheme === 'dark' ? '#9FC3FF' : '#0056A8';
        const LIGHT_BLUE = colorScheme === 'dark' ? '#1D2A3A' : '#E6F4FE';

        const languages = [
            { code: "en", name: t("settings.language.languages.english"), flag: "🇬🇧" },
            { code: "fr", name: t("settings.language.languages.french"), flag: "🇫🇷" },
            { code: "es", name: t("settings.language.languages.spanish"), flag: "🇪🇸" },
        ];

        const handleLanguageChange = async (languageCode: string) => {
            try {
                await i18n.changeLanguage(languageCode);
                setSelectedLanguage(languageCode);
            
                // Show success message
                Alert.alert(
                    t("settings.language.success"),
                    "",
                    [{ text: "OK" }]
                );
            } catch (error) {
                console.error("Error changing language:", error);
                Alert.alert(t("common.error"), "Failed to change language");
            }
        };

    return (
        <MenuLayout>
            <View style={styles.container}>
                <View style={styles.header}>
                    <Text style={styles.title}>{t("settings.language.title")}</Text>
                    <Text style={styles.subtitle}>{t("settings.language.subtitle")}</Text>
                </View>

                <View style={styles.currentLanguageContainer}>
                    <Ionicons name="globe-outline" size={20} color={BLUE} />
                    <Text style={styles.currentLanguageLabel}>
                        {t("settings.language.current")}:
                    </Text>
                    <Text style={styles.currentLanguageValue}>
                        {languages.find(lang => lang.code === selectedLanguage)?.name}
                    </Text>
                </View>

                <View style={styles.languageList}>
                    {languages.map((language) => (
                        <Pressable
                            key={language.code}
                            style={[
                                styles.languageItem,
                                selectedLanguage === language.code && styles.languageItemSelected,
                            ]}
                            onPress={() => handleLanguageChange(language.code)}
                        >
                            <View style={styles.languageInfo}>
                                <Text style={styles.flag}>{language.flag}</Text>
                                <Text
                                    style={[
                                        styles.languageName,
                                        selectedLanguage === language.code && styles.languageNameSelected,
                                    ]}
                                >
                                    {language.name}
                                </Text>
                            </View>
                            {selectedLanguage === language.code && (
                                <Ionicons name="checkmark-circle" size={24} color={BLUE} />
                            )}
                        </Pressable>
                    ))}
                </View>
            </View>
        </MenuLayout>
    );
}

const getStyles = (scheme: 'light' | 'dark') => {
    const isDark = scheme === 'dark';
    const BLUE = isDark ? '#9FC3FF' : '#0056A8';
    const LIGHT_BLUE = isDark ? '#1D2A3A' : '#E6F4FE';

    return StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: isDark ? '#111418' : '#F5F7FB',
        padding: 20,
    },
    header: {
        marginBottom: 24,
        marginTop: 20,
    },
    title: {
        fontSize: 28,
        fontWeight: "700",
        color: BLUE,
        marginBottom: 8,
    },
    subtitle: {
        fontSize: 16,
        color: isDark ? '#A0A7B1' : '#555',
    },
    currentLanguageContainer: {
        flexDirection: "row",
        alignItems: "center",
        backgroundColor: isDark ? '#151A20' : '#FFFFFF',
        padding: 16,
        borderRadius: 12,
        marginBottom: 20,
        shadowColor: "#000",
        shadowOpacity: 0.05,
        shadowRadius: 8,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    currentLanguageLabel: {
        fontSize: 16,
        color: isDark ? '#A0A7B1' : '#555',
        marginLeft: 10,
        fontWeight: "500",
    },
    currentLanguageValue: {
        fontSize: 16,
        color: BLUE,
        fontWeight: "700",
        marginLeft: 6,
    },
    languageList: {
        gap: 12,
    },
    languageItem: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        backgroundColor: isDark ? '#151A20' : '#FFFFFF',
        padding: 18,
        borderRadius: 12,
        borderWidth: 2,
        borderColor: "transparent",
        shadowColor: "#000",
        shadowOpacity: 0.05,
        shadowRadius: 8,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    languageItemSelected: {
        backgroundColor: LIGHT_BLUE,
        borderColor: BLUE,
    },
    languageInfo: {
        flexDirection: "row",
        alignItems: "center",
        gap: 12,
    },
    flag: {
        fontSize: 32,
    },
    languageName: {
        fontSize: 18,
        color: isDark ? '#ECEDEE' : '#333',
        fontWeight: "500",
    },
    languageNameSelected: {
        color: BLUE,
        fontWeight: "700",
    },
    });
};
