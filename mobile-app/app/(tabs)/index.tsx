import React from "react";
import {
  View,
  Text,
  StyleSheet,
  Image,
  ScrollView,
  Pressable,
  Linking, Platform,
} from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../context/AuthContext";

export default function HomeScreen() {
  const router = useRouter();
  const { user, signOut } = useAuth();
  const { t } = useTranslation();

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
      if (Platform.OS === "web") {
        await Linking.openURL(webUrl);
      } else {
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
            <Ionicons name="menu" size={28} color="#0056A8" />
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
                  <Text style={styles.secondaryButtonText}>{t("home.loginRegister")}</Text>
                </Pressable>
            ) : (
                <Pressable
                    style={[styles.secondaryButton, styles.actionButton]}
                    onPress={signOut}
                >
                  <Text style={styles.secondaryButtonText}>Logout ({user.email})</Text>
                </Pressable>
            )}

            <Pressable
                style={[styles.secondaryButton, styles.actionButton]}
                onPress={() => router.push("/settings/language")}
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
        <Text style={styles.footerText}>
          {t("home.footer.copyright")}
        </Text>
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
});