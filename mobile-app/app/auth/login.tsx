import React, { useEffect, useState } from "react";
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    ActivityIndicator,
    Image,
    KeyboardAvoidingView,
    Platform,
    ScrollView,
    SafeAreaView,
} from "react-native";
import { getStyles } from "../../styles/auth.login.styles";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../context/AuthContext";
import { useColorScheme } from "../../hooks/use-color-scheme";
import { Link, useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { canLoginWithBiometrics } from "../../services/biometric-auth.service";

export default function LoginScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { signIn, signInWithBiometrics } = useAuth();
    const colorScheme = useColorScheme() ?? 'light';
    const styles = getStyles(colorScheme);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [isPasswordVisible, setIsPasswordVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [biometricLoginAvailable, setBiometricLoginAvailable] = useState(false);

    const iconColor = colorScheme === 'dark' ? '#A0A7B1' : '#666';
    const placeholderColor = colorScheme === 'dark' ? '#8B93A1' : '#999';
    const forgotPasswordColor = colorScheme === 'dark' ? '#9FC3FF' : '#0056A8';

    useEffect(() => {
        let mounted = true;
        const checkBiometricAvailability = async () => {
            const available = await canLoginWithBiometrics();
            if (mounted) {
                setBiometricLoginAvailable(available);
            }
        };
        checkBiometricAvailability();
        return () => {
            mounted = false;
        };
    }, []);

    const handleLogin = async () => {
        // Basic email format validation (optional, skipped if input might be phone)
        // We will just check if not empty for now, or ensure at least simple length

        if (!email || !password) {
            setError(t("auth.error.fill_all_fields_login"));
            return;
        }

        // Removed strict email regex check to allow phone numbers
        setLoading(true);
        setError(null);
        try {
            await signIn(email, password);
            router.replace("/");
        } catch (e: any) {
            if (e?.response?.status === 403) {
                setError(t("auth.error.account_inactive"));
            } else if (e?.response?.status === 401) {
                setError(t("auth.error.invalid_credentials"));
            } else if (e?.response) {
                setError(t("auth.error.login_failed"));
            } else {
                setError(t("auth.error.network_error"));
            }
        } finally {
            setLoading(false);
        }
    };

    const handleBiometricLogin = async () => {
        setLoading(true);
        setError(null);
        try {
            await signInWithBiometrics(t("auth.biometrics.promptLogin"));
            router.replace("/");
        } catch (e) {
            setError(t("auth.biometrics.errors.failed"));
        } finally {
            setLoading(false);
        }
    };

    return (
        <SafeAreaView style={styles.container}>
            <KeyboardAvoidingView
                behavior={Platform.OS === "ios" ? "padding" : "height"}
                style={styles.keyboardContainer}
            >
                <ScrollView
                    contentContainerStyle={styles.scrollContent}
                    keyboardShouldPersistTaps="handled"
                    showsVerticalScrollIndicator={false}
                >
                    <View style={styles.contentContainer}>
                        <View style={styles.logoContainer}>
                            <Image
                                source={require("../../assets/mana/manaLogo.png")}
                                style={styles.logo}
                                resizeMode="contain"
                            />
                        </View>

                        <Text style={styles.title}>{t("auth.title")}</Text>

                        <View style={styles.inputContainer}>
                            <View style={styles.inputWrapper}>
                                <Ionicons name="mail-outline" size={20} color={iconColor} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder={t("auth.email_placeholder")}
                                    placeholderTextColor={placeholderColor}
                                    value={email}
                                    onChangeText={setEmail}
                                    autoCapitalize="none"
                                    keyboardType="email-address"
                                    accessibilityLabel={t("auth.email_label", "Email")}
                                />
                            </View>

                            <View style={styles.inputWrapper}>
                                <Ionicons name="lock-closed-outline" size={20} color={iconColor} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder={t("auth.password_placeholder")}
                                    placeholderTextColor={placeholderColor}
                                    value={password}
                                    onChangeText={setPassword}
                                    secureTextEntry={!isPasswordVisible}
                                    accessibilityLabel={t("auth.password_label", "Password")}
                                />
                                <TouchableOpacity
                                    onPress={() => setIsPasswordVisible(!isPasswordVisible)}
                                    accessibilityRole="button"
                                    accessibilityLabel={t("auth.toggle_password_visibility")}
                                    accessibilityHint={t("auth.toggle_password_visibility_hint", "Shows or hides your password")}
                                >
                                    <Ionicons
                                        name={isPasswordVisible ? "eye-off-outline" : "eye-outline"}
                                        size={20}
                                        color={iconColor}
                                    />
                                </TouchableOpacity>
                            </View>
                        </View>

                        <View style={styles.forgotPasswordContainer}>
                            <Link href="/auth/forgot-password" asChild>
                                <TouchableOpacity accessibilityRole="button" accessibilityLabel={t('auth.forgot_password_link_login')}>
                                    <Text style={{ color: forgotPasswordColor, fontSize: 14 }}>{t('auth.forgot_password_link_login')}</Text>
                                </TouchableOpacity>
                            </Link>
                        </View>

                        {error && (
                            <View style={styles.errorContainer}>
                                <Ionicons name="alert-circle" size={18} color={colorScheme === 'dark' ? '#FFB4AB' : '#D32F2F'} />
                                <Text style={styles.errorText}>{error}</Text>
                            </View>
                        )}

                        <View style={styles.buttonContainer}>
                            {loading ? (
                                <ActivityIndicator size="large" color={colorScheme === 'dark' ? '#6AA9FF' : '#0056A8'} />
                            ) : (
                                <>
                                    <TouchableOpacity
                                        style={styles.loginButton}
                                        onPress={handleLogin}
                                        activeOpacity={0.8}
                                        accessibilityRole="button"
                                        accessibilityLabel={t("auth.login_button")}
                                        testID="login-button"
                                    >
                                        <Text style={styles.loginButtonText}>{t("auth.login_button")}</Text>
                                    </TouchableOpacity>

                                    {biometricLoginAvailable && (
                                        <TouchableOpacity
                                            style={styles.biometricButton}
                                            onPress={handleBiometricLogin}
                                            activeOpacity={0.8}
                                            accessibilityRole="button"
                                            accessibilityLabel={t("auth.biometrics.loginButton")}
                                            testID="biometric-login-button"
                                        >
                                            <Ionicons
                                                name={Platform.OS === "ios" ? "scan-outline" : "finger-print-outline"}
                                                size={18}
                                                color={colorScheme === 'dark' ? '#9FC3FF' : '#0056A8'}
                                            />
                                            <Text style={styles.biometricButtonText}>{t("auth.biometrics.loginButton")}</Text>
                                        </TouchableOpacity>
                                    )}
                                </>
                            )}
                        </View>

                        <View style={styles.footer}>
                            <Text style={styles.footerText}>{t("auth.register_prompt")} </Text>
                            <Link href="/auth/register" asChild>
                                <TouchableOpacity accessibilityRole="button" accessibilityLabel={t("auth.register_link")}>
                                    <Text style={styles.link}>{t("auth.register_link")}</Text>
                                </TouchableOpacity>
                            </Link>
                        </View>
                    </View>
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}
