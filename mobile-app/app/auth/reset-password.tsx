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
import { useRouter, useLocalSearchParams } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import AuthService from "../../services/auth.service";

export default function ResetPasswordScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const params = useLocalSearchParams();

    // Ensure params.email is treated as a string, handling array case if necessary
    const initialEmail = Array.isArray(params.email) ? params.email[0] : (params.email as string) || "";

    const [email, setEmail] = useState(initialEmail);
    const [code, setCode] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isPasswordVisible, setIsPasswordVisible] = useState(false);

    const handleSubmit = async () => {
        if (!email || !code || !newPassword) {
            setError(t('auth.error.fill_all_fields_reset'));
            return;
        }

        setLoading(true);
        setError(null);
        setMessage(null);

        try {
            const res = await AuthService.resetPassword(email, code, newPassword);
            setMessage(res.data.message);
            setTimeout(() => {
                router.replace("/auth/login");
            }, 2000);
        } catch (e: any) {
            setError(e.response?.data?.message || t('auth.error.generic_reset_error'));
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

                    <Text style={styles.title}>{t('auth.reset_password_title')}</Text>

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

                        <View style={styles.inputWrapper}>
                            <Ionicons name="key-outline" size={20} color="#666" style={styles.inputIcon} />
                            <TextInput
                                style={styles.input}
                                placeholder={t('auth.six_digit_code_placeholder')}
                                placeholderTextColor="#999"
                                value={code}
                                onChangeText={setCode}
                                keyboardType="number-pad"
                                maxLength={6}
                            />
                        </View>

                        <View style={styles.inputWrapper}>
                            <Ionicons name="lock-closed-outline" size={20} color="#666" style={styles.inputIcon} />
                            <TextInput
                                style={styles.input}
                                placeholder={t('auth.new_password_placeholder')}
                                placeholderTextColor="#999"
                                value={newPassword}
                                onChangeText={setNewPassword}
                                secureTextEntry={!isPasswordVisible}
                            />
                            <TouchableOpacity onPress={() => setIsPasswordVisible(!isPasswordVisible)}>
                                <Ionicons
                                    name={isPasswordVisible ? "eye-off-outline" : "eye-outline"}
                                    size={20}
                                    color="#666"
                                />
                            </TouchableOpacity>
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
                                <Text style={styles.loginButtonText}>{t('auth.reset_password_button')}</Text>
                            </TouchableOpacity>
                        )}
                    </View>

                    <TouchableOpacity onPress={() => router.back()} style={{ marginTop: 20 }}>
                        <Text style={{ color: '#0056A8', textAlign: 'center' }}>{t('auth.cancel_link')}</Text>
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
        </Wrapper>
    );
}
