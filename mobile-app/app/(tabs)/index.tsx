import React from "react";
import {
    View,
    Text,
    StyleSheet,
    Image,
    ScrollView,
    Pressable,
    Linking,
    Platform,
    Modal,
    Animated,
} from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../context/AuthContext";

export default function HomeScreen() {
    const router = useRouter();
    const { user, signOut } = useAuth();
    const { t } = useTranslation();

    const [menuVisible, setMenuVisible] = React.useState(false);
    const menuScale = React.useRef(new Animated.Value(1)).current;

    const WHATSAPP_NUMBER = "14388379223"; // +1 438 837 9223

    const handleWhatsAppContact = async () => {
        const message =
            "Bonjour amis de * MANA | Maison d’accueil *. Nous devons connaître les détails de certains services. J’écris à partir du site Web * MANA *.";

        const appUrl = `whatsapp://send?phone=${WHATSAPP_NUMBER}&text=${encodeURIComponent(
            message
        )}`;
        const webUrl = `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(
            message
        )}`;

        try {
            // Web: always open browser link
            if (Platform.OS === "web") {
                await Linking.openURL(webUrl);
            } else {
                // Native: try app first, fall back to web
                const canOpen = await Linking.canOpenURL(appUrl);
                if (canOpen) {
                    await Linking.openURL(appUrl);
                } else {
                    await Linking.openURL(webUrl);
                }
            }
        } catch (err) {
            console.warn("Unable to open WhatsApp", err);
        }
    };

    const handleNavigateHome = () => {
        setMenuVisible(false);
        router.replace("/");
    };

    const handleNavigateEvents = () => {
        setMenuVisible(false);
        router.push("/events");
    };

    const handleMenuPressIn = () => {
        Animated.spring(menuScale, {
            toValue: 0.92,
            useNativeDriver: true,
            speed: 20,
            bounciness: 6,
        }).start();
    };

    const handleMenuPressOut = () => {
        Animated.spring(menuScale, {
            toValue: 1,
            useNativeDriver: true,
            speed: 20,
            bounciness: 8,
        }).start();
    };

    return (
        <View style={styles.container}>
            <ScrollView contentContainerStyle={styles.scrollContent}>
                {/* HEADER */}
                <View style={styles.header}>
                    <Image
                        source={require("../../assets/mana/manaLogo.png")}
                        style={styles.logo}
                        resizeMode="contain"
                    />
                    <Pressable
                        onPress={() => setMenuVisible(true)}
                        onPressIn={handleMenuPressIn}
                        onPressOut={handleMenuPressOut}
                        hitSlop={12}
                        style={({ pressed }) => [
                            styles.menuButton,
                            pressed && styles.menuButtonPressed,
                        ]}
                    >
                        <Animated.View style={{ transform: [{ scale: menuScale }] }}>
                            <Ionicons name="menu" size={28} color="#0056A8" />
                        </Animated.View>
                    </Pressable>
                </View>

                {/* HERO / GALA BANNER */}
                <Image
                    source={require("../../assets/mana/Gala_image_Mana.png")}
                    style={styles.heroImage}
                    resizeMode="cover"
                />

                {/* MAIN ACTIONS (Browse events / Login / Language) */}
                <View style={styles.actionsContainer}>
                    <Pressable
                        style={[styles.primaryButton, styles.actionButton]}
                        onPress={() => router.push("/events")}
                    >
                        <Text style={styles.primaryButtonText}>{t("home.browseEvents")}</Text>
                    </Pressable>

                    {!user ? (
                        <Pressable
                            style={[styles.secondaryButton, styles.actionButton]}
                            onPress={() => router.push("/auth/login")}
                        >
                            <Text style={styles.secondaryButtonText}>
                                {t("home.loginRegister")}
                            </Text>
                        </Pressable>
                    ) : (
                        <Pressable
                            style={[styles.secondaryButton, styles.actionButton]}
                            onPress={signOut}
                        >
                            <Text style={styles.secondaryButtonText}>
                                Logout ({user.email})
                            </Text>
                        </Pressable>
                    )}

                    <Pressable
                        style={[styles.secondaryButton, styles.actionButton]}
                        onPress={() => router.push("/settings/language")}
                    >
                        <Text style={styles.secondaryButtonText}>
                            {t("home.changeLanguage")}
                        </Text>
                    </Pressable>
                </View>

                {/* “VIVEZ L’ÉMOTION” + RÉSERVER BUTTON */}
                <View style={styles.emotionSection}>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.emotionTitle}>{t("home.liveEmotion")}</Text>
                        <Text style={styles.emotionSubtitle}>
                            {t("home.celebrateDiversity")}
                        </Text>
                    </View>
                    <Pressable
                        style={styles.reserveButton}
                        onPress={() => router.push("/events/gala-2025")}
                    >
                        <Text style={styles.reserveButtonText}>{t("home.reserve")}</Text>
                    </Pressable>
                </View>

                {/* DERNIÈRES ACTUALITÉS */}
                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>{t("home.latestNews")}</Text>
                    <ScrollView
                        horizontal
                        showsHorizontalScrollIndicator={false}
                        contentContainerStyle={styles.newsRow}
                    >
                        <Image
                            source={require("../../assets/mana/examenTEF_Mana.png")}
                            style={styles.newsCard}
                        />
                        <Image
                            source={require("../../assets/mana/Interculturelle_Mana.png")}
                            style={styles.newsCard}
                        />
                        <Image
                            source={require("../../assets/mana/boutiqueSolidaire_Mana.png")}
                            style={styles.newsCard}
                        />
                    </ScrollView>
                </View>

                {/* SLOGAN IMAGE */}
                <Image
                    source={require("../../assets/mana/Slogan_Mana.png")}
                    style={styles.sloganImage}
                    resizeMode="contain"
                />

                {/* NOS SERVICES */}
                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>{t("home.ourServices")}</Text>
                    <Text style={styles.sectionSubtitle}>
                        {t("home.ourServicesSubtitle")}
                    </Text>

                    <View style={styles.servicesRow}>
                        <ServiceCard
                            icon="home"
                            title={t("home.services.welcomeCanada.title")}
                            description={t("home.services.welcomeCanada.description")}
                        />
                        <ServiceCard
                            icon="restaurant"
                            title={t("home.services.foodAssistance.title")}
                            description={t("home.services.foodAssistance.description")}
                        />
                        <ServiceCard
                            icon="school"
                            title={t("home.services.schoolSupport.title")}
                            description={t("home.services.schoolSupport.description")}
                        />
                    </View>
                </View>

                {/* FOOTER */}
                <Footer t={t} />
            </ScrollView>

            {/* WHATSAPP FLOATING BUTTON */}
            <Pressable style={styles.whatsappButton} onPress={handleWhatsAppContact}>
                <Ionicons name="logo-whatsapp" size={26} color="#FFFFFF" />
            </Pressable>

            {/* HAMBURGER MENU */}
            <Modal
                visible={menuVisible}
                transparent
                animationType="fade"
                onRequestClose={() => setMenuVisible(false)}
            >
                <View style={styles.menuOverlay}>
                    <Pressable
                        style={StyleSheet.absoluteFill}
                        onPress={() => setMenuVisible(false)}
                    />
                    <View style={styles.menuContainer}>
                        <View style={styles.menuHeader}>
                            <View style={styles.menuBadge}>
                                <Ionicons name="sparkles" size={16} color={BLUE} />
                            </View>
                            <View style={{ flex: 1, marginLeft: 10 }}>
                                <Text style={styles.menuTitle}>{t("menu.navigation")}</Text>
                                <Text style={styles.menuSubtitle}>{t("menu.quickAccess")}</Text>
                            </View>
                            <Pressable hitSlop={12} onPress={() => setMenuVisible(false)}>
                                <Ionicons name="close" size={20} color="#2D3B57" />
                            </Pressable>
                        </View>

                        <View style={styles.menuDivider} />

                        <Pressable
                            style={({ pressed }) => [
                                styles.menuItem,
                                pressed && styles.menuItemPressed,
                            ]}
                            onPress={handleNavigateHome}
                        >
                            <View style={styles.menuItemLeft}>
                                <Ionicons name="home" size={20} color={BLUE} />
                                <Text style={styles.menuItemText}>{t("menu.home")}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={BLUE} />
                        </Pressable>

                        <Pressable
                            style={({ pressed }) => [
                                styles.menuItem,
                                styles.menuItemElevated,
                                pressed && styles.menuItemPressed,
                            ]}
                            onPress={handleNavigateEvents}
                        >
                            <View style={styles.menuItemLeft}>
                                <Ionicons name="calendar" size={20} color={BLUE} />
                                <Text style={styles.menuItemText}>{t("menu.events")}</Text>
                            </View>
                            <Ionicons name="chevron-forward" size={18} color={BLUE} />
                        </Pressable>

                        <View style={[styles.menuItem, styles.menuItemDisabled]}>
                            <View style={styles.menuItemLeft}>
                                <Ionicons name="person-circle" size={20} color="#9BA5B7" />
                                <Text
                                    style={[styles.menuItemText, styles.menuItemTextDisabled]}
                                >
                                    {t("menu.profile")}
                                </Text>
                            </View>
                            <Text style={styles.menuPill}>{t("menu.soon")}</Text>
                        </View>
                    </View>
                </View>
            </Modal>
        </View>
    );
}

type ServiceCardProps = {
    icon: keyof typeof Ionicons.glyphMap;
    title: string;
    description: string;
};

function ServiceCard({ icon, title, description }: ServiceCardProps) {
    return (
        <View style={styles.serviceCard}>
            <Ionicons name={icon} size={24} color="#0056A8" />
            <Text style={styles.serviceTitle}>{title}</Text>
            <Text style={styles.serviceDescription}>{description}</Text>
        </View>
    );
}

function Footer({ t }: { t: (key: string) => string }) {
    return (
        <View style={styles.footerContainer}>
            <Text style={styles.footerText}>{t("home.footer.copyright")}</Text>
            <Text style={styles.footerText}>{t("home.footer.phone")}</Text>
        </View>
    );
}

const BLUE = "#0056A8";
const YELLOW = "#F6B800";
const LIGHT_BG = "#F5F7FB";

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: LIGHT_BG,
    },
    scrollContent: {
        paddingBottom: 120,
    },
    header: {
        paddingHorizontal: 16,
        paddingTop: 40,
        paddingBottom: 12,
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        backgroundColor: "#FFFFFF",
    },
    logo: {
        width: 170,
        height: 40,
    },
    heroImage: {
        width: "100%",
        height: 190,
    },
    actionsContainer: {
        backgroundColor: "#FFFFFF",
        paddingHorizontal: 16,
        paddingVertical: 14,
        borderBottomWidth: StyleSheet.hairlineWidth,
        borderColor: "#E0E4EC",
    },
    actionButton: {
        marginBottom: 10,
    },
    primaryButton: {
        backgroundColor: BLUE,
        paddingVertical: 12,
        borderRadius: 8,
        alignItems: "center",
    },
    primaryButtonText: {
        color: "#FFFFFF",
        fontWeight: "600",
        fontSize: 16,
    },
    secondaryButton: {
        backgroundColor: "#FFFFFF",
        borderWidth: 1,
        borderColor: BLUE,
        paddingVertical: 10,
        borderRadius: 8,
        alignItems: "center",
    },
    secondaryButtonText: {
        color: BLUE,
        fontWeight: "500",
        fontSize: 15,
    },
    emotionSection: {
        flexDirection: "row",
        alignItems: "center",
        paddingHorizontal: 16,
        paddingVertical: 16,
        backgroundColor: "#FFFFFF",
        borderTopWidth: StyleSheet.hairlineWidth,
        borderBottomWidth: StyleSheet.hairlineWidth,
        borderColor: "#E0E4EC",
    },
    emotionTitle: {
        color: BLUE,
        fontWeight: "700",
        fontSize: 14,
    },
    emotionSubtitle: {
        color: "#333333",
        fontSize: 12,
        marginTop: 2,
    },
    reserveButton: {
        backgroundColor: YELLOW,
        paddingHorizontal: 18,
        paddingVertical: 10,
        borderRadius: 6,
    },
    reserveButtonText: {
        color: "#000",
        fontWeight: "700",
        fontSize: 14,
    },
    section: {
        paddingHorizontal: 16,
        paddingTop: 18,
        paddingBottom: 8,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: "700",
        color: BLUE,
        marginBottom: 6,
    },
    sectionSubtitle: {
        fontSize: 13,
        color: "#555",
        marginBottom: 12,
    },
    newsRow: {
        flexDirection: "row",
        gap: 12,
        paddingVertical: 8,
    },
    newsCard: {
        width: 220,
        height: 120,
        borderRadius: 8,
    },
    sloganImage: {
        width: "100%",
        height: 70,
        marginTop: 4,
    },
    servicesRow: {
        marginTop: 10,
        rowGap: 12,
    },
    serviceCard: {
        backgroundColor: "#FFFFFF",
        borderRadius: 10,
        padding: 14,
        marginBottom: 10,
        shadowColor: "#000",
        shadowOpacity: 0.04,
        shadowRadius: 4,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    serviceTitle: {
        marginTop: 6,
        fontWeight: "600",
        color: BLUE,
        fontSize: 15,
    },
    serviceDescription: {
        marginTop: 4,
        fontSize: 12,
        color: "#555",
    },
    whatsappButton: {
        position: "absolute",
        right: 18,
        bottom: 26,
        width: 54,
        height: 54,
        borderRadius: 27,
        backgroundColor: "#25D366",
        alignItems: "center",
        justifyContent: "center",
        elevation: 4,
        shadowColor: "#000",
        shadowOpacity: 0.25,
        shadowRadius: 4,
        shadowOffset: { width: 0, height: 2 },
    },
    footerContainer: {
        backgroundColor: BLUE,
        paddingVertical: 18,
        paddingHorizontal: 16,
        marginTop: 20,
    },
    footerText: {
        color: "#FFFFFF",
        fontSize: 12,
        textAlign: "center",
        marginBottom: 4,
    },
    menuButton: {
        padding: 6,
        borderRadius: 18,
        backgroundColor: "#FFFFFF",
    },
    menuButtonPressed: {
        backgroundColor: "rgba(0,86,168,0.08)",
    },
    menuOverlay: {
        flex: 1,
        backgroundColor: "rgba(4,15,34,0.3)",
        paddingTop: 60,
        paddingHorizontal: 12,
    },
    menuContainer: {
        marginLeft: "auto",
        width: 232,
        backgroundColor: "#FFFFFF",
        borderRadius: 16,
        paddingVertical: 14,
        paddingHorizontal: 14,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: "#E5ECF8",
        shadowColor: "#000",
        shadowOpacity: 0.14,
        shadowRadius: 10,
        shadowOffset: { width: 0, height: 6 },
        elevation: 8,
        gap: 10,
    },
    menuHeader: {
        flexDirection: "row",
        alignItems: "center",
    },
    menuBadge: {
        width: 34,
        height: 34,
        borderRadius: 17,
        backgroundColor: "#EAF1FF",
        alignItems: "center",
        justifyContent: "center",
    },
    menuTitle: {
        fontSize: 15,
        fontWeight: "700",
        color: "#1A2D4A",
        letterSpacing: 0.2,
    },
    menuSubtitle: {
        fontSize: 12,
        color: "#6F7B91",
        marginTop: 2,
    },
    menuDivider: {
        height: StyleSheet.hairlineWidth,
        backgroundColor: "#E2E8F2",
    },
    menuItem: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        paddingVertical: 10,
        paddingHorizontal: 10,
        borderRadius: 12,
        backgroundColor: "#F8FAFE",
    },
    menuItemPressed: {
        opacity: 0.8,
    },
    menuItemElevated: {
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: "#D9E5FF",
        shadowColor: "#2F64C0",
        shadowOpacity: 0.08,
        shadowRadius: 8,
        shadowOffset: { width: 0, height: 3 },
        elevation: 4,
    },
    menuItemLeft: {
        flexDirection: "row",
        alignItems: "center",
        gap: 10,
    },
    menuItemText: {
        fontSize: 15,
        fontWeight: "600",
        color: "#1A2D4A",
    },
    menuItemDisabled: {
        backgroundColor: "#F1F3F7",
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: "#E2E7EF",
    },
    menuItemTextDisabled: {
        color: "#9BA5B7",
        fontWeight: "500",
    },
    menuPill: {
        backgroundColor: "#EAF1FF",
        color: BLUE,
        fontSize: 11,
        fontWeight: "700",
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 12,
    },
});
