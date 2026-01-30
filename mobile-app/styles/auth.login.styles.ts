import { StyleSheet } from "react-native";

export const getStyles = (colorScheme: 'light' | 'dark') => {
    const isDark = colorScheme === 'dark';
    
    return StyleSheet.create({
        container: {
            flex: 1,
            backgroundColor: isDark ? '#0F1419' : '#F5F7FB',
        },
        contentContainer: {
            flex: 1,
            justifyContent: "center",
            paddingHorizontal: 24,
            paddingBottom: 40,
        },
        logoContainer: {
            alignItems: "center",
            marginBottom: 40,
        },
        logo: {
            width: 200,
            height: 60,
        },
        title: {
            fontSize: 28,
            fontWeight: "700",
            color: isDark ? '#9FC3FF' : '#0056A8',
            marginBottom: 30,
            textAlign: "center",
        },
        inputContainer: {
            marginBottom: 20,
        },
        inputWrapper: {
            flexDirection: "row",
            alignItems: "center",
            backgroundColor: isDark ? '#1F2328' : '#FFFFFF',
            borderRadius: 12,
            borderWidth: 1,
            borderColor: isDark ? '#2F3A4A' : '#E0E4EC',
            marginBottom: 16,
            paddingHorizontal: 14,
            height: 56,
            shadowColor: "#000",
            shadowOpacity: 0.05,
            shadowRadius: 4,
            shadowOffset: { width: 0, height: 2 },
            elevation: 2,
        },
        inputIcon: {
            marginRight: 10,
        },
        input: {
            flex: 1,
            fontSize: 16,
            color: isDark ? '#ECEDEE' : '#333',
            height: "100%",
        },
        errorContainer: {
            flexDirection: "row",
            alignItems: "center",
            backgroundColor: isDark ? '#3A1F1F' : '#FFEBEE',
            padding: 10,
            borderRadius: 8,
            marginBottom: 20,
        },
        errorText: {
            color: isDark ? '#FFB4AB' : '#D32F2F',
            marginLeft: 8,
            fontSize: 14,
        },
        buttonContainer: {
            marginTop: 10,
            marginBottom: 30,
            height: 56,
        },
        loginButton: {
            backgroundColor: isDark ? '#6AA9FF' : '#0056A8',
            height: "100%",
            borderRadius: 12,
            alignItems: "center",
            justifyContent: "center",
            shadowColor: isDark ? '#6AA9FF' : '#0056A8',
            shadowOpacity: 0.3,
            shadowRadius: 8,
            shadowOffset: { width: 0, height: 4 },
            elevation: 4,
        },
        loginButtonText: {
            color: "#FFFFFF",
            fontSize: 16,
            fontWeight: "700",
            letterSpacing: 0.5,
        },
        footer: {
            flexDirection: "row",
            justifyContent: "center",
            alignItems: "center",
        },
        footerText: {
            fontSize: 14,
            color: isDark ? '#A0A7B1' : '#666',
        },
        link: {
            fontSize: 14,
            color: isDark ? '#9FC3FF' : '#0056A8',
            fontWeight: "700",
        },
    });
};

// Default export for backward compatibility
export const styles = getStyles('light');
