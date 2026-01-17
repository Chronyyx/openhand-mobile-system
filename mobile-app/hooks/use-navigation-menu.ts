import { useCallback, useMemo, useState } from 'react';
import { usePathname, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { useAuth } from '../context/AuthContext';

export type NavigationMenuOptions = {
    showMyRegistrations?: boolean;
    showDashboard?: boolean;
    showProfile?: boolean;
};

type NavigateOptions = {
    replace?: boolean;
};

export function useNavigationMenu(options: NavigationMenuOptions = {}) {
    const router = useRouter();
    const pathname = usePathname();
    const { t } = useTranslation();
    const { user, hasRole } = useAuth();
    const [menuVisible, setMenuVisible] = useState(false);

    const closeMenu = useCallback(() => {
        setMenuVisible(false);
    }, []);

    const openMenu = useCallback(() => {
        setMenuVisible(true);
    }, []);

    const navigate = useCallback(
        (path: string, navigateOptions: NavigateOptions = {}) => {
            closeMenu();
            if (pathname === path) return;
            if (navigateOptions.replace) {
                router.replace(path);
            } else {
                router.push(path);
            }
        },
        [closeMenu, pathname, router],
    );

    const showMyRegistrations = options.showMyRegistrations ?? Boolean(user);
    const showDashboard = options.showDashboard ?? hasRole(['ROLE_ADMIN']);
    const showProfile = options.showProfile !== false;

    const menuProps = useMemo(
        () => ({
            visible: menuVisible,
            onClose: closeMenu,
            onNavigateHome: () => navigate('/', { replace: true }),
            onNavigateEvents: () => navigate('/events'),
            onNavigateProfile: showProfile ? () => navigate('/profile') : undefined,
            onNavigateMyRegistrations: showMyRegistrations ? () => navigate('/registrations') : undefined,
            showMyRegistrations,
            showDashboard,
            onNavigateDashboard: showDashboard ? () => navigate('/admin') : undefined,
            t,
        }),
        [
            closeMenu,
            menuVisible,
            navigate,
            showDashboard,
            showMyRegistrations,
            showProfile,
            t,
        ],
    );

    return { openMenu, closeMenu, menuProps };
}
