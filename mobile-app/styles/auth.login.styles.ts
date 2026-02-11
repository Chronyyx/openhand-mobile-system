import { StyleSheet } from "react-native";

export const getStyles = (colorScheme: 'light' | 'dark') => {
    const isDark = colorScheme === 'dark';
    
    return StyleSheet.create({
        container: {
            flex: 1,
            backgroundColor: isDark ? '#0F1419' : '#F5F7FB',
        },
        keyboardContainer: {
            flex: 1,
        },
        scrollContent: {
            flexGrow: 1,
            paddingHorizontal: 24,
            paddingTop: 24,
            paddingBottom: 36,
            justifyContent: "center",
        },
        contentContainer: {
            width: "100%",
            maxWidth: 480,
            alignSelf: "center",
        },
        logoContainer: {
            alignItems: "center",
            marginBottom: 28,
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
            minHeight: 56,
            paddingVertical: 8,
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
            minHeight: 24,
        },
        forgotPasswordContainer: {
            alignItems: 'flex-end',
            marginBottom: 20,
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
            marginBottom: 20,
            gap: 12,
            minHeight: 56,
        },
        loginButton: {
            backgroundColor: isDark ? '#6AA9FF' : '#0056A8',
            minHeight: 52,
            paddingVertical: 14,
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
        biometricButton: {
            borderWidth: 1,
            borderColor: isDark ? '#6AA9FF' : '#0056A8',
            backgroundColor: isDark ? '#1B2430' : '#FFFFFF',
            height: 52,
            borderRadius: 12,
            alignItems: "center",
            justifyContent: "center",
            flexDirection: 'row',
            gap: 8,
        },
        biometricButtonText: {
            color: isDark ? '#9FC3FF' : '#0056A8',
            fontSize: 15,
            fontWeight: "700",
        },
        footer: {
            flexDirection: "row",
            justifyContent: "center",
            alignItems: "center",
            paddingBottom: 8,
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
