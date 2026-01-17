import { Image } from 'expo-image';
import { Platform, StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';

import { Collapsible } from '@/components/ui/collapsible';
import { ExternalLink } from '@/components/external-link';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { MenuLayout } from '@/components/menu-layout';
import { Fonts } from '@/constants/theme';

export default function TabTwoScreen() {
    const { t } = useTranslation();

  return (
    <MenuLayout>
      <ParallaxScrollView
        headerBackgroundColor={{ light: '#D0D0D0', dark: '#353636' }}
        headerImage={
          <IconSymbol
            size={310}
            color="#808080"
            name="chevron.left.forwardslash.chevron.right"
            style={styles.headerImage}
          />
        }>
        <ThemedView style={styles.titleContainer}>
          <ThemedText
            type="title"
            style={{
              fontFamily: Fonts.rounded,
            }}>
            {t('explore.title')}
          </ThemedText>
        </ThemedView>
        <ThemedText>{t('explore.description')}</ThemedText>
        <Collapsible title={t('explore.fileRouting.title')}>
          <ThemedText>
            {t('explore.fileRouting.description1')}{' '}
            <ThemedText type="defaultSemiBold">app/(tabs)/index.tsx</ThemedText> and{' '}
            <ThemedText type="defaultSemiBold">app/(tabs)/explore.tsx</ThemedText>
          </ThemedText>
          <ThemedText>
            {t('explore.fileRouting.description2')} <ThemedText type="defaultSemiBold">app/(tabs)/_layout.tsx</ThemedText>{' '}
            {t('explore.fileRouting.description3')}
          </ThemedText>
          <ExternalLink href="https://docs.expo.dev/router/introduction">
            <ThemedText type="link">{t('explore.fileRouting.learnMore')}</ThemedText>
          </ExternalLink>
        </Collapsible>
        <Collapsible title={t('explore.platformSupport.title')}>
          <ThemedText>
            {t('explore.platformSupport.description')}
          </ThemedText>
        </Collapsible>
        <Collapsible title={t('explore.images.title')}>
          <ThemedText>
            {t('explore.images.description')}
          </ThemedText>
          <Image
            source={require('@/assets/images/react-logo.png')}
            style={{ width: 100, height: 100, alignSelf: 'center' }}
          />
          <ExternalLink href="https://reactnative.dev/docs/images">
            <ThemedText type="link">{t('explore.fileRouting.learnMore')}</ThemedText>
          </ExternalLink>
        </Collapsible>
        <Collapsible title={t('explore.darkMode.title')}>
          <ThemedText>
            {t('explore.darkMode.description')}
          </ThemedText>
          <ExternalLink href="https://docs.expo.dev/develop/user-interface/color-themes/">
            <ThemedText type="link">{t('explore.fileRouting.learnMore')}</ThemedText>
          </ExternalLink>
        </Collapsible>
        <Collapsible title={t('explore.animations.title')}>
          <ThemedText>
            {t('explore.animations.description')}
          </ThemedText>
          {Platform.select({
            ios: (
              <ThemedText>
                The <ThemedText type="defaultSemiBold">components/ParallaxScrollView.tsx</ThemedText>{' '}
                component provides a parallax effect for the header image.
              </ThemedText>
            ),
          })}
        </Collapsible>
      </ParallaxScrollView>
    </MenuLayout>
  );
}

const styles = StyleSheet.create({
  headerImage: {
    color: '#808080',
    bottom: -90,
    left: -35,
    position: 'absolute',
  },
  titleContainer: {
    flexDirection: 'row',
    gap: 8,
  },
});
