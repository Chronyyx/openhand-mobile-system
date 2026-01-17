import React, { type ReactNode } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { AppHeader } from './app-header';
import { NavigationMenu } from './navigation-menu';
import { useNavigationMenu, type NavigationMenuOptions } from '../hooks/use-navigation-menu';

type MenuLayoutProps = {
    children: ReactNode;
    containerStyle?: StyleProp<ViewStyle>;
    menuOptions?: NavigationMenuOptions;
};

export function MenuLayout({ children, containerStyle, menuOptions }: MenuLayoutProps) {
    const { openMenu, menuProps } = useNavigationMenu(menuOptions);

    return (
        <View style={[styles.container, containerStyle]}>
            <AppHeader onMenuPress={openMenu} />
            <View style={styles.content}>{children}</View>
            <NavigationMenu {...menuProps} />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    content: {
        flex: 1,
    },
});
