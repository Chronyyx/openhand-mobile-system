import React, { useState } from "react";
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    ActivityIndicator,
    Image,
    KeyboardAvoidingView,
    Platform,
    TouchableWithoutFeedback,
    Keyboard,
    useColorScheme,
} from "react-native";
import { getStyles } from "../../styles/auth.login.styles";
import { useTranslation } from "react-i18next";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import AuthService from "../../services/auth.service";

export default function ForgotPasswordScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const colorScheme = useColorScheme() || 'light';
    const styles = getStyles(colorScheme);
    
    const [email, setEmail] = useState("");
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    
    const isDark = colorScheme === 'dark';
    const accentColor = isDark ? '#6AA9FF' : '#0056A8';
    const successColor = isDark ? '#81C784' : '#2E7D32';
    const successBG = isDark ? '#1B3A1B' : '#E8F5E9';
    const textMuted = isDark ? '#A0A7B1' : '#666';

    const handleSubmit = async () => {
        if (!email) {
            setError(t('auth.error.enter_email'));
            return;
        }

        setLoading(true);
        setError(null);
        setMessage(null);

        try {
            const res = await AuthService.forgotPassword(email);
            setMessage(res.data.message);
            // After short delay, navigate to reset screen
            setTimeout(() => {
                router.push({ pathname: "/auth/reset-password", params: { email } });
            }, 2000);
        } catch (e: any) {
            setError(t('auth.error.generic_reset_error'));
        } finally {
            setLoading(false);
        }
    };

    const Wrapper = (Platform.OS === 'web' ? View : TouchableWithoutFeedback) as any;
    const wrapperProps = Platform.OS === 'web' ? { style: styles.container } : { onPress: Keyboard.dismiss };

    return (
        <Wrapper {...wrapperProps}>
            <KeyboardAvoidingView
                behavior={Platform.OS === "ios" ? "padding" : "height"}
                style={styles.container}
            >
                <View style={styles.contentContainer}>
                    <View style={styles.logoContainer}>
                        <Image
                            source={require("../../assets/mana/manaLogo.png")}
                            style={styles.logo}
                            resizeMode="contain"
                        />
                    </View>

                    <Text style={styles.title}>{t('auth.forgot_password_title')}</Text>
                    <Text style={{ textAlign: 'center', color: textMuted, marginBottom: 20 }}>
                        {t('auth.forgot_password_prompt_title')}
                    </Text>

                    <View style={styles.inputContainer}>
                        <View style={styles.inputWrapper}>
                            <Ionicons name="mail-outline" size={20} color={textMuted} style={styles.inputIcon} />
                            <TextInput
                                style={styles.input}
                                placeholder={t('auth.email_placeholder')}
                                placeholderTextColor={textMuted}
                                value={email}
                                onChangeText={setEmail}
                                autoCapitalize="none"
                                keyboardType="email-address"
                                accessibilityLabel={t('auth.email_label', 'Email')}
                            />
                        </View>
                    </View>

                    {error && (
                        <View style={styles.errorContainer}>
                            <Ionicons name="alert-circle" size={18} color={isDark ? '#FFB4AB' : '#D32F2F'} />
                            <Text style={styles.errorText}>{error}</Text>
                        </View>
                    )}

                    {message && (
                        <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 15, padding: 10, backgroundColor: successBG, borderRadius: 8 }}>
                            <Ionicons name="checkmark-circle" size={18} color={successColor} />
                            <Text style={{ color: successColor, marginLeft: 8, flex: 1 }}>{message}</Text>
                        </View>
                    )}

                    <View style={styles.buttonContainer}>
                        {loading ? (
                            <ActivityIndicator size="large" color={accentColor} />
                        ) : (
                            <TouchableOpacity
                                style={styles.loginButton}
                                onPress={handleSubmit}
                                activeOpacity={0.8}
                                accessibilityRole="button"
                                accessibilityLabel={t('auth.send_reset_code_button')}
                            >
                                <Text style={styles.loginButtonText}>{t('auth.send_reset_code_button')}</Text>
                            </TouchableOpacity>
                        )}
                    </View>

                    <TouchableOpacity
                        onPress={() => router.back()}
                        style={{ marginTop: 20 }}
                        accessibilityRole="button"
                        accessibilityLabel={t('auth.back_to_login_link')}
                    >
                        <Text style={{ color: accentColor, textAlign: 'center' }}>{t('auth.back_to_login_link')}</Text>
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
        </Wrapper>
    );
}
