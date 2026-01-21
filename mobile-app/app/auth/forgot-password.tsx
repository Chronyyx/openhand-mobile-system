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
} from "react-native";
import { styles } from "../../styles/auth.login.styles";
import { useTranslation } from "react-i18next";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import AuthService from "../../services/auth.service";

export default function ForgotPasswordScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const [email, setEmail] = useState("");
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

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
                    <Text style={{ textAlign: 'center', color: '#666', marginBottom: 20 }}>
                        {t('auth.forgot_password_prompt_title')}
                    </Text>

                    <View style={styles.inputContainer}>
                        <View style={styles.inputWrapper}>
                            <Ionicons name="mail-outline" size={20} color="#666" style={styles.inputIcon} />
                            <TextInput
                                style={styles.input}
                                placeholder={t('auth.email_placeholder')}
                                placeholderTextColor="#999"
                                value={email}
                                onChangeText={setEmail}
                                autoCapitalize="none"
                                keyboardType="email-address"
                            />
                        </View>
                    </View>

                    {error && (
                        <View style={styles.errorContainer}>
                            <Ionicons name="alert-circle" size={18} color="#D32F2F" />
                            <Text style={styles.errorText}>{error}</Text>
                        </View>
                    )}

                    {message && (
                        <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 15, padding: 10, backgroundColor: '#E8F5E9', borderRadius: 8 }}>
                            <Ionicons name="checkmark-circle" size={18} color="#2E7D32" />
                            <Text style={{ color: '#2E7D32', marginLeft: 8, flex: 1 }}>{message}</Text>
                        </View>
                    )}

                    <View style={styles.buttonContainer}>
                        {loading ? (
                            <ActivityIndicator size="large" color="#0056A8" />
                        ) : (
                            <TouchableOpacity style={styles.loginButton} onPress={handleSubmit} activeOpacity={0.8}>
                                <Text style={styles.loginButtonText}>{t('auth.send_reset_code_button')}</Text>
                            </TouchableOpacity>
                        )}
                    </View>

                    <TouchableOpacity onPress={() => router.back()} style={{ marginTop: 20 }}>
                        <Text style={{ color: '#0056A8', textAlign: 'center' }}>{t('auth.back_to_login_link')}</Text>
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
        </Wrapper>
    );
}
