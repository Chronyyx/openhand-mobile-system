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
    ScrollView,
} from "react-native";
import { styles } from "../../styles/auth.register.styles";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../context/AuthContext";
import { Link, useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

export default function RegisterScreen() {
    const router = useRouter();
    const { t } = useTranslation();
    const { signUp } = useAuth();

    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [phoneNumber, setPhoneNumber] = useState("");
    const [gender, setGender] = useState("PREFER_NOT_TO_SAY");
    const [age, setAge] = useState("");

    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    const [isPasswordVisible, setIsPasswordVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const isValidEmail = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

    const MIN_USER_AGE = 13;

    const handleRegister = async () => {
        if (!name || !email || !password || !age) {
            setError(t("auth.error.fill_all_fields_register"));
            return;
        }
        if (!isValidEmail(email)) {
            setError(t("auth.error.invalid_email"));
            return;
        }

        const isValidPhone = (phone: string) => /^\+?[0-9]{10,15}$/.test(phone);
        if (phoneNumber && !isValidPhone(phoneNumber)) {
            setError(t("auth.error.invalid_phone_number"));
            return;
        }

        if (password !== confirmPassword) {
            setError(t("auth.error.passwords_do_not_match"));
            return;
        }

        // Optional: Add simple age validation
        if (isNaN(Number(age)) || Number(age) < MIN_USER_AGE) {
            setError(t("auth.error.invalid_age"));
            return;
        }

        setLoading(true);
        setError(null);
        try {
            // No roles provided; backend will assign default member role (ROLE_MEMBER)
            await signUp(email, password, [], name, phoneNumber, gender, age);
            router.replace("/auth/login");
        } catch (e: any) {
            console.error(e);
            setError(e.response?.data?.message || t("auth.error.registration_failed"));
        } finally {
            setLoading(false);
        }
    };

    const Wrapper = (Platform.OS === 'web' ? View : TouchableWithoutFeedback) as any;
    const wrapperProps = Platform.OS === 'web' ? { style: styles.container } : { onPress: Keyboard.dismiss };

    const genderOptions = [
        { label: t("auth.gender_options.male"), value: "MALE" },
        { label: t("auth.gender_options.female"), value: "FEMALE" },
        { label: t("auth.gender_options.other"), value: "OTHER" },
        { label: t("auth.gender_options.prefer_not_to_say"), value: "PREFER_NOT_TO_SAY" },
    ];

    return (
        <Wrapper {...wrapperProps}>
            <KeyboardAvoidingView
                behavior={Platform.OS === "ios" ? "padding" : "height"}
                style={styles.container}
            >
                <ScrollView contentContainerStyle={{ flexGrow: 1 }} showsVerticalScrollIndicator={false}>
                    <View style={[styles.contentContainer, { paddingTop: 40 }]}>
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
                            {/* NAME */}
                            <View style={styles.inputWrapper}>
                                <Ionicons name="person-outline" size={20} color="#666" style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder={t("auth.name_placeholder")}
                                    placeholderTextColor="#999"
                                    value={name}
                                    onChangeText={setName}
                                    accessibilityLabel={t("auth.name_placeholder")}
                                />
                            </View>

                            {/* EMAIL */}
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
                                    accessibilityLabel={t("auth.email_placeholder")}
                                />
                            </View>

                            {/* PHONE */}
                            <View style={styles.inputWrapper}>
                                <Ionicons name="call-outline" size={20} color="#666" style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder={t("auth.phone_placeholder")}
                                    placeholderTextColor="#999"
                                    value={phoneNumber}
                                    onChangeText={setPhoneNumber}
                                    keyboardType="phone-pad"
                                    accessibilityLabel={t("auth.phone_placeholder")}
                                />
                            </View>

                            {/* AGE */}
                            <View style={styles.inputWrapper}>
                                <Ionicons name="calendar-outline" size={20} color="#666" style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder={t("auth.age_placeholder")}
                                    placeholderTextColor="#999"
                                    value={age}
                                    onChangeText={setAge}
                                    keyboardType="number-pad"
                                    accessibilityLabel={t("auth.age_placeholder")}
                                />
                            </View>

                            {/* GENDER */}
                            <View style={styles.genderContainer}>
                                <Text style={styles.genderLabel}>{t("auth.gender_label")}</Text>
                                <View style={styles.genderButtonContainer}>
                                    {genderOptions.map((option) => (
                                        <TouchableOpacity
                                            key={option.value}
                                            style={[
                                                styles.genderButton,
                                                gender === option.value && styles.genderButtonSelected,
                                            ]}
                                            onPress={() => setGender(option.value)}
                                            accessibilityRole="button"
                                            accessibilityLabel={`${option.label}${gender === option.value ? ", selected" : ""}`}
                                        >
                                            <Text
                                                style={[
                                                    styles.genderButtonText,
                                                    gender === option.value && styles.genderButtonTextSelected,
                                                ]}
                                            >
                                                {option.label}
                                            </Text>
                                        </TouchableOpacity>
                                    ))}
                                </View>
                            </View>

                            {/* PASSWORD */}
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

                            {/* CONFIRM PASSWORD */}
                            <View style={styles.inputWrapper}>
                                <Ionicons name="lock-closed-outline" size={20} color="#666" style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder={t("auth.confirm_password_placeholder")}
                                    placeholderTextColor="#999"
                                    value={confirmPassword}
                                    onChangeText={setConfirmPassword}
                                    secureTextEntry={!isPasswordVisible}
                                    accessibilityLabel={t("auth.confirm_password_placeholder")}
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
                </ScrollView>
            </KeyboardAvoidingView>
        </Wrapper>
    );
}

