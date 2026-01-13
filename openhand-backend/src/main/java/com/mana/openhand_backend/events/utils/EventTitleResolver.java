package com.mana.openhand_backend.events.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to resolve event title translation keys to display names.
 * This is used in notifications and other backend contexts where translation
 * keys stored in the database need to be converted to readable names.
 * 
 * The resolver provides fallback translations for core events in English,
 * French, and Spanish. For custom events, it falls back to humanizing
 * the translation key by replacing underscores with spaces and capitalizing.
 */
public class EventTitleResolver {

    private static final Map<String, Map<String, String>> TITLE_TRANSLATIONS = new HashMap<>();

    static {
        // Initialize English translations
        Map<String, String> enTitles = new HashMap<>();
        enTitles.put("gala", "MANA Recognition Gala");
        enTitles.put("panier_noel", "Christmas Basket");
        enTitles.put("distribution_mardi", "Food Distribution – Tuesday");
        enTitles.put("distribution_jeudi", "Food Distribution – Thursday");
        enTitles.put("formation_mediateur", "MANA Training – Intercultural Mediator");
        TITLE_TRANSLATIONS.put("en", enTitles);

        // Initialize French translations
        Map<String, String> frTitles = new HashMap<>();
        frTitles.put("gala", "Gala de reconnaissance MANA");
        frTitles.put("panier_noel", "Panier de Noël");
        frTitles.put("distribution_mardi", "Distribution Alimentaire - Mardi");
        frTitles.put("distribution_jeudi", "Distribution alimentaire – Jeudi");
        frTitles.put("formation_mediateur", "Formation MANA – Médiateur interculturel");
        TITLE_TRANSLATIONS.put("fr", frTitles);

        // Initialize Spanish translations
        Map<String, String> esTitles = new HashMap<>();
        esTitles.put("gala", "Gala de reconocimiento MANA");
        esTitles.put("panier_noel", "Cesta de Navidad");
        esTitles.put("distribution_mardi", "Distribución de alimentos – Martes");
        esTitles.put("distribution_jeudi", "Distribución de alimentos – Jueves");
        esTitles.put("formation_mediateur", "Capacitación MANA – Mediador intercultural");
        TITLE_TRANSLATIONS.put("es", esTitles);
    }

    /**
     * Resolve an event title key to a readable display name in the given language.
     * 
     * @param titleKey the translation key stored in the event (e.g., "distribution_mardi")
     * @param language the language code (e.g., "en", "fr", "es")
     * @return the translated title, or a humanized fallback if the key is not found
     */
    public static String resolve(String titleKey, String language) {
        if (titleKey == null || titleKey.isBlank()) {
            return "Event";
        }

        // Get translations for the specified language, default to English if not found
        Map<String, String> langTitles = TITLE_TRANSLATIONS.getOrDefault(language, TITLE_TRANSLATIONS.get("en"));

        // Return the translated title, or humanize the key as fallback
        return langTitles.getOrDefault(titleKey, humanizeKey(titleKey));
    }

    /**
     * Resolve an event title key with fallback to English translations.
     * Used when language is unknown or for consistency.
     * 
     * @param titleKey the translation key stored in the event
     * @return the translated title in English, or a humanized fallback
     */
    public static String resolveToEnglish(String titleKey) {
        return resolve(titleKey, "en");
    }

    /**
     * Convert a translation key to a human-readable format.
     * Example: "distribution_mardi" → "Distribution Mardi"
     * 
     * @param key the translation key
     * @return humanized display name
     */
    private static String humanizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "Event";
        }
        // Replace underscores with spaces and capitalize each word
        String spaced = key.replace('_', ' ');
        Pattern pattern = Pattern.compile("\\b([a-z])");
        Matcher matcher = pattern.matcher(spaced);
        return matcher.replaceAll(m -> m.group(1).toUpperCase());
    }
}
