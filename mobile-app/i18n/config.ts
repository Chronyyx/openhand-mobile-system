import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import * as Localization from 'expo-localization';
import AsyncStorage from '@react-native-async-storage/async-storage';

// Import translation files
import en from '../locales/en/translation.json';
import fr from '../locales/fr/translation.json';
import es from '../locales/es/translation.json';

// Storage key for persisting language preference
const LANGUAGE_STORAGE_KEY = '@app_language';

// Supported languages
export const SUPPORTED_LANGUAGES = ['en', 'fr', 'es'] as const;
export type SupportedLanguage = typeof SUPPORTED_LANGUAGES[number];

// Helper to detect SSR (web server) vs browser/native runtime
const isServer = typeof window === 'undefined';

// Language detector plugin
const languageDetector = {
  type: 'languageDetector' as const,
  async: true,
  detect: async (callback: (lng: string) => void) => {
    try {
      // On server-side rendering, avoid using AsyncStorage/window; defer to fallback
      if (!isServer) {
        // Try to get saved language from AsyncStorage
        const savedLanguage = await AsyncStorage.getItem(LANGUAGE_STORAGE_KEY);
        if (savedLanguage && SUPPORTED_LANGUAGES.includes(savedLanguage as SupportedLanguage)) {
          callback(savedLanguage);
          return;
        }
      }

      // If no saved language, try to detect from device locale
      let deviceLanguage = 'en';
      try {
        const deviceLocale = Localization.getLocales()[0];
        deviceLanguage = deviceLocale?.languageCode || 'en';
      } catch {
        // In SSR or unsupported environments, keep 'en'
      }
      
      // Check if device language is supported
      if (SUPPORTED_LANGUAGES.includes(deviceLanguage as SupportedLanguage)) {
        callback(deviceLanguage);
      } else {
        // Fallback to English
        callback('en');
      }
    } catch (error) {
      console.error('Error detecting language:', error);
      callback('en');
    }
  },
  init: () => {},
  cacheUserLanguage: async (language: string) => {
    try {
      if (!isServer) {
        await AsyncStorage.setItem(LANGUAGE_STORAGE_KEY, language);
      }
    } catch (error) {
      console.error('Error saving language:', error);
    }
  },
};

// Initialize i18next
i18n
  .use(languageDetector)
  .use(initReactI18next)
  .init({
    // Set to false in production for better performance
    debug: __DEV__,
    
    // Translation resources
    resources: {
      en: { translation: en },
      fr: { translation: fr },
      es: { translation: es },
    },
    
    // Fallback language if translation is missing
    fallbackLng: 'en',
    
    // Namespace for translations (we use 'translation' as default)
    defaultNS: 'translation',
    
    // Interpolation options
    interpolation: {
      escapeValue: false, // React already escapes values
    },
    
    // React options
    react: {
      useSuspense: false, // Disable suspense for React Native
    },
    
    // Compatibility options
    compatibilityJSON: 'v4', // Use i18next v4 JSON format
  });

export default i18n;
