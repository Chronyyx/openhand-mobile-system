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
import { styles } from "./login.styles";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../context/AuthContext";
import { Link, useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

export default function LoginScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { signIn } = useAuth();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [isPasswordVisible, setIsPasswordVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

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
            if (e?.response?.status === 401) {
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

    const Wrapper = (Platform.OS === 'web' ? View : TouchableWithoutFeedback) as any;
    const wrapperProps = Platform.OS === 'web' ? { style: styles.container } : { onPress: Keyboard.dismiss };

    return (
        <Wrapper {...wrapperProps}>
            <KeyboardAvoidingView
                behavior={Platform.OS === "ios" ? "padding" : "height"}
                style={styles.container}
            >
                <View style={styles.contentContainer}>
                    {/* LOGO */}
                    <View style={styles.logoContainer}>
                        <Image
                            source={require("../../assets/mana/manaLogo.png")}
                            style={styles.logo}
                            resizeMode="contain"
                        />
                    </View>

                    <Text style={styles.title}>{t("auth.title")}</Text>

                    {/* INPUTS */}
                    <View style={styles.inputContainer}>
                        <View style={styles.inputWrapper}>
                            <Ionicons name="mail-outline" size={20} color="#666" style={styles.inputIcon} />
                            <TextInput
                                style={styles.input}
                                placeholder={t("auth.email_placeholder")}
                                placeholderTextColor="#999"
                                value={email}
                                onChangeText={setEmail}
                                autoCapitalize="none"
                                keyboardType="email-address"
                            />
                        </View>

                        <View style={styles.inputWrapper}>
                            <Ionicons name="lock-closed-outline" size={20} color="#666" style={styles.inputIcon} />
                            <TextInput
                                style={styles.input}
                                placeholder={t("auth.password_placeholder")}
                                placeholderTextColor="#999"
                                value={password}
                                onChangeText={setPassword}
                                secureTextEntry={!isPasswordVisible}
                            />
                            <TouchableOpacity onPress={() => setIsPasswordVisible(!isPasswordVisible)} accessibilityLabel={t("auth.toggle_password_visibility")}>
                                <Ionicons
                                    name={isPasswordVisible ? "eye-off-outline" : "eye-outline"}
                                    size={20}
                                    color="#666"
                                />
                            </TouchableOpacity>
                        </View>
                    </View>

                    {/* ERROR MESSAGE */}
                    {error && (
                        <View style={styles.errorContainer}>
                            <Ionicons name="alert-circle" size={18} color="#D32F2F" />
                            <Text style={styles.errorText}>{error}</Text>
                        </View>
                    )}

                    {/* LOGIN BUTTON */}
                    <View style={styles.buttonContainer}>
                        {loading ? (
                            <ActivityIndicator size="large" color="#0056A8" />
                        ) : (
                            <TouchableOpacity style={styles.loginButton} onPress={handleLogin} activeOpacity={0.8}>
                                <Text style={styles.loginButtonText}>{t("auth.login_button")}</Text>
                            </TouchableOpacity>
                        )}
                    </View>

                    {/* REGISTER LINK */}
                    <View style={styles.footer}>
                        <Text style={styles.footerText}>{t("auth.register_prompt")} </Text>
                        <Link href="/auth/register" asChild>
                            <TouchableOpacity>
                                <Text style={styles.link}>{t("auth.register_link")}</Text>
                            </TouchableOpacity>
                        </Link>
                    </View>
                </View>
            </KeyboardAvoidingView>
        </Wrapper>
    );
}

