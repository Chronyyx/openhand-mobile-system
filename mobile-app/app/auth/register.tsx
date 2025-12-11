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
import { styles } from "./register.styles";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../context/AuthContext";
import { Link, useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

export default function RegisterScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { signUp } = useAuth();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [isPasswordVisible, setIsPasswordVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const isValidEmail = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

    const handleRegister = async () => {
        if (!email || !password) {
            setError("Please fill in all fields.");
            return;
        }
        if (!isValidEmail(email)) {
            setError("Please enter a valid email address.");
            return;
        }
        if (password !== confirmPassword) {
            setError("Passwords do not match.");
            return;
        }
        setLoading(true);
        setError(null);
        try {
            // No roles provided; backend will assign default member role (ROLE_MEMBER)
            await signUp(email, password, []);
            router.replace("/auth/login");
        } catch (e: any) {
            console.error(e);
            setError(e.response?.data?.message || "Registration failed. Please try again.");
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

                    <Text style={styles.title}>{t("auth.create_account")}</Text>

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
                            <TouchableOpacity onPress={() => setIsPasswordVisible(!isPasswordVisible)} accessibilityLabel={t("auth.toggle_password_visibility", "Toggle password visibility")}>
                                <Ionicons
                                    name={isPasswordVisible ? "eye-off-outline" : "eye-outline"}
                                    size={20}
                                    color="#666"
                                />
                            </TouchableOpacity>
                        </View>

                        <View style={styles.inputWrapper}>
                            <Ionicons name="lock-closed-outline" size={20} color="#666" style={styles.inputIcon} />
                            <TextInput
                                style={styles.input}
                                placeholder={t("auth.confirm_password_placeholder", "Confirm Password")}
                                placeholderTextColor="#999"
                                value={confirmPassword}
                                onChangeText={setConfirmPassword}
                                secureTextEntry={!isPasswordVisible}
                                accessibilityLabel={t("auth.confirm_password_placeholder", "Confirm Password")}
                                testID="confirm-password-input"
                            />
                        </View>
                    </View>

                    {/* ERROR MESSAGE */}
                    {error && (
                        <View style={styles.errorContainer}>
                            <Ionicons name="alert-circle" size={18} color="#D32F2F" />
                            <Text style={styles.errorText}>{error}</Text>
                        </View>
                    )}

                    {/* REGISTER BUTTON */}
                    <View style={styles.buttonContainer}>
                        {loading ? (
                            <ActivityIndicator size="large" color="#0056A8" />
                        ) : (
                            <TouchableOpacity style={styles.loginButton} onPress={handleRegister} activeOpacity={0.8}>
                                <Text style={styles.loginButtonText}>{t("auth.register_button")}</Text>
                            </TouchableOpacity>
                        )}
                    </View>

                    {/* LOGIN LINK */}
                    <View style={styles.footer}>
                        <Text style={styles.footerText}>{t("auth.login_prompt")} </Text>
                        <Link href="/auth/login" asChild>
                            <TouchableOpacity>
                                <Text style={styles.link}>{t("auth.login_link")}</Text>
                            </TouchableOpacity>
                        </Link>
                    </View>
                </View>
            </KeyboardAvoidingView>
        </Wrapper>
    );
}

