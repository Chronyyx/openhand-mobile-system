import React from "react";
import {
  View,
  Text,
  StyleSheet,
  Image,
  ScrollView,
  Pressable,
  Linking,
} from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../context/AuthContext";
import { AppHeader } from "../../components/app-header";
import { NavigationMenu } from "../../components/navigation-menu";
import { useColorScheme } from "../../hooks/use-color-scheme";

export default function HomeScreen() {
  const router = useRouter();
  const { user, signOut, hasRole } = useAuth();
  const { t } = useTranslation();
  const colorScheme = useColorScheme() ?? "light";
  const styles = getStyles(colorScheme);
  const palette = getPalette(colorScheme);
  const [menuVisible, setMenuVisible] = React.useState(false);

  const WHATSAPP_NUMBER = "14388379223"; // +1 438 837 9223

  const handleWhatsAppContact = async () => {
    const message =
      "Bonjour amis de * MANA | Maison d’accueil *. Nous devons connaître les détails de certains services. J’écris à partir du site Web * MANA *.";

    const appUrl = `whatsapp://send?phone=${WHATSAPP_NUMBER}&text=${encodeURIComponent(
      message
    )}`;
    const webUrl = `https://api.whatsapp.com/send?phone=${WHATSAPP_NUMBER}&text=${encodeURIComponent(
      message
    )}`;

    try {
      const canOpen = await Linking.canOpenURL(appUrl);
      if (canOpen) {
        await Linking.openURL(appUrl);
      } else {
        await Linking.openURL(webUrl);
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

  const handleNavigateAttendance = () => {
    setMenuVisible(false);
    router.push("/admin/attendance");
  };

  const handleNavigateMyRegistrations = () => {
    setMenuVisible(false);
    router.push("/registrations");
  };

  const handleNavigateDonations = () => {
    setMenuVisible(false);
    router.push("/donations");
  };

  const handleNavigateDashboard = () => {
    setMenuVisible(false);
    router.push("/admin");
  };

  const handleNavigateProfile = () => {
    setMenuVisible(false);
    router.push("/profile");
  };

  return (
    <View style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* HEADER */}
        <AppHeader onMenuPress={() => setMenuVisible(true)} />

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
            testID="browse-events-button"
            accessibilityRole="button"
            accessibilityLabel={t("home.browseEvents")}
          >
            <Text style={styles.primaryButtonText}>{t("home.browseEvents")}</Text>
          </Pressable>

          {!user ? (
            <Pressable
              style={[styles.secondaryButton, styles.actionButton]}
              onPress={() => router.push("/auth/login")}
              accessibilityRole="button"
              accessibilityLabel={t("home.loginRegister")}
            >
              <Text style={styles.secondaryButtonText}>{t("home.loginRegister")}</Text>
            </Pressable>
          ) : (
            <Pressable
              style={[styles.secondaryButton, styles.actionButton]}
              onPress={signOut}
              accessibilityRole="button"
              accessibilityLabel={t("home.logout")}
            >
              <Text style={styles.secondaryButtonText}>{t("home.logout")} ({user.email})</Text>
            </Pressable>
          )}

          <Pressable
            style={[styles.secondaryButton, styles.actionButton]}
            onPress={() => router.push("/settings/language")}
            accessibilityRole="button"
            accessibilityLabel={t("home.changeLanguage")}
          >
            <Text style={styles.secondaryButtonText}>{t("home.changeLanguage")}</Text>
          </Pressable>
        </View>

        {/* “VIVEZ L’ÉMOTION” + RÉSERVER BUTTON */}
        <View style={styles.emotionSection}>
          <View style={{ flex: 1 }}>
            <Text style={styles.emotionTitle}>{t("home.liveEmotion")}</Text>
            <Text style={styles.emotionSubtitle}>{t("home.celebrateDiversity")}</Text>
          </View>
          <Pressable
            style={styles.reserveButton}
            onPress={() => router.push("/events/gala-2025")}
            accessibilityRole="button"
            accessibilityLabel={t("home.reserve")}
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
              palette={palette}
            />
            <ServiceCard
              icon="restaurant"
              title={t("home.services.foodAssistance.title")}
              description={t("home.services.foodAssistance.description")}
              palette={palette}
            />
            <ServiceCard
              icon="school"
              title={t("home.services.schoolSupport.title")}
              description={t("home.services.schoolSupport.description")}
              palette={palette}
            />
          </View>
        </View>

        {/* FOOTER */}
        <Footer t={t} palette={palette} />
      </ScrollView>

      {/* WHATSAPP FLOATING BUTTON */}
      <Pressable
        style={styles.whatsappButton}
        onPress={handleWhatsAppContact}
        accessibilityRole="button"
        accessibilityLabel={t("home.contactWhatsapp", "Contact MANA on WhatsApp")}
      >
        <Ionicons name="logo-whatsapp" size={26} color="#FFFFFF" />
      </Pressable>

      {/* HAMBURGER MENU */}
      <NavigationMenu
        visible={menuVisible}
        onClose={() => setMenuVisible(false)}
        onNavigateHome={handleNavigateHome}
        onNavigateEvents={handleNavigateEvents}
        onNavigateAttendance={handleNavigateAttendance}
        onNavigateProfile={handleNavigateProfile}
        onNavigateMyRegistrations={handleNavigateMyRegistrations}
        showMyRegistrations={!!user}
        showAttendance={hasRole(["ROLE_ADMIN", "ROLE_EMPLOYEE"])}
        showDashboard={hasRole(["ROLE_ADMIN", "ROLE_EMPLOYEE"])}
        onNavigateDashboard={handleNavigateDashboard}
        onNavigateDonations={handleNavigateDonations}
        showDonations={hasRole(["ROLE_MEMBER"])}
        t={t}
      />
    </View>
  );
}

type Palette = {
  primary: string;
  onPrimary: string;
  background: string;
  surface: string;
  border: string;
  text: string;
  textMuted: string;
  accent: string;
  success: string;
};

type ServiceCardProps = {
  icon: keyof typeof Ionicons.glyphMap;
  title: string;
  description: string;
  palette: Palette;
};

function ServiceCard({ icon, title, description, palette }: ServiceCardProps) {
  const colorScheme = useColorScheme() ?? "light";
  const styles = getStyles(colorScheme);
  return (
    <View style={styles.serviceCard}>
      <Ionicons name={icon} size={24} color={palette.primary} />
      <Text style={styles.serviceTitle}>{title}</Text>
      <Text style={styles.serviceDescription}>{description}</Text>
    </View>
  );
}

function Footer({ t, palette }: { t: (key: string) => string; palette: Palette }) {
  const colorScheme = useColorScheme() ?? "light";
  const styles = getStyles(colorScheme);
  return (
    <View style={[styles.footerContainer, { backgroundColor: palette.primary }]}
    >
      <Text style={styles.footerText}>
        {t("home.footer.copyright")}
      </Text>
      <Text style={styles.footerText}>{t("home.footer.phone")}</Text>
    </View>
  );
}

const getPalette = (scheme: "light" | "dark"): Palette => ({
  primary: scheme === "dark" ? "#9FC3FF" : "#0056A8",
  onPrimary: "#FFFFFF",
  background: scheme === "dark" ? "#111418" : "#F5F7FB",
  surface: scheme === "dark" ? "#151A20" : "#FFFFFF",
  border: scheme === "dark" ? "#2A313B" : "#E0E4EC",
  text: scheme === "dark" ? "#ECEDEE" : "#333333",
  textMuted: scheme === "dark" ? "#A0A7B1" : "#555555",
  accent: "#F6B800",
  success: "#25D366",
});

const getStyles = (scheme: "light" | "dark") => {
  const palette = getPalette(scheme);
  return StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: palette.background,
    },
    scrollContent: {
      paddingBottom: 0,
    },
    heroImage: {
      width: "100%",
      height: 190,
    },
    actionsContainer: {
      backgroundColor: palette.surface,
      paddingHorizontal: 16,
      paddingVertical: 14,
      borderBottomWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
    },
    actionButton: {
      marginBottom: 10,
    },
    primaryButton: {
      backgroundColor: palette.primary,
      paddingVertical: 12,
      borderRadius: 8,
      alignItems: "center",
    },
    primaryButtonText: {
      color: palette.onPrimary,
      fontWeight: "600",
      fontSize: 16,
    },
    secondaryButton: {
      backgroundColor: palette.surface,
      borderWidth: 1,
      borderColor: palette.primary,
      paddingVertical: 10,
      borderRadius: 8,
      alignItems: "center",
    },
    secondaryButtonText: {
      color: palette.primary,
      fontWeight: "500",
      fontSize: 15,
    },
    emotionSection: {
      flexDirection: "row",
      alignItems: "center",
      paddingHorizontal: 16,
      paddingVertical: 16,
      backgroundColor: palette.surface,
      borderTopWidth: StyleSheet.hairlineWidth,
      borderBottomWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
    },
    emotionTitle: {
      color: palette.primary,
      fontWeight: "700",
      fontSize: 14,
    },
    emotionSubtitle: {
      color: palette.text,
      fontSize: 12,
      marginTop: 2,
    },
    reserveButton: {
      backgroundColor: palette.accent,
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
      color: palette.primary,
      marginBottom: 6,
    },
    sectionSubtitle: {
      fontSize: 13,
      color: palette.textMuted,
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
      backgroundColor: palette.surface,
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
      color: palette.primary,
      fontSize: 15,
    },
    serviceDescription: {
      marginTop: 4,
      fontSize: 12,
      color: palette.textMuted,
    },
    whatsappButton: {
      position: "absolute",
      right: 18,
      bottom: 26,
      width: 54,
      height: 54,
      borderRadius: 27,
      backgroundColor: palette.success,
      alignItems: "center",
      justifyContent: "center",
      elevation: 4,
      shadowColor: "#000",
      shadowOpacity: 0.25,
      shadowRadius: 4,
      shadowOffset: { width: 0, height: 2 },
    },
    footerContainer: {
      backgroundColor: palette.primary,
      paddingVertical: 18,
      paddingHorizontal: 16,
      marginTop: 20,
    },
    footerText: {
      color: palette.onPrimary,
      fontSize: 12,
      textAlign: "center",
      marginBottom: 4,
    },
  });
};
