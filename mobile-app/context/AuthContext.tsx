import React, { createContext, useState, useEffect, useContext, useMemo, useCallback } from 'react';
import AuthService from '../services/auth.service';

export interface User {
    token: string;
    refreshToken: string;
    type: string;
    id: number;
    email: string;
    roles: string[];
    name: string;
    phoneNumber: string;
    gender: string;
    age: number;
    memberStatus?: 'ACTIVE' | 'INACTIVE';
    statusChangedAt?: string | null;
    profilePictureUrl?: string | null;
    preferredLanguage?: string;
}

interface AuthContextProps {
    user: User | null;
    isLoading: boolean;
    signIn: (email: string, password: string) => Promise<void>;
    signOut: () => Promise<void>;
    signUp: (email: string, password: string, roles: string[], name: string, phoneNumber: string, gender: string, age: string) => Promise<void>;
    hasRole: (roles: string[]) => boolean;
    updateUser: (updates: Partial<User>) => Promise<void>;
}

const AuthContext = createContext<AuthContextProps>({
    user: null,
    isLoading: true,
    signIn: async () => { },
    signOut: async () => { },
    signUp: async () => { },
    hasRole: () => false,
    updateUser: async () => { },
});

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const loadUser = async () => {
            try {
                // 1. Load from storage first for speed
                const storedUser = await AuthService.getCurrentUser();
                if (storedUser) {
                    setUser(storedUser);
                    // 2. Fetch fresh data from API to ensure sync (e.g. invalid token, changed pic)
                    // 2. Fetch fresh data from API to ensure sync (e.g. invalid token, changed pic)
                    // Copilot: Removed redundant auto-refresh on every load. 
                    // Any explicit refresh of profile data should be triggered elsewhere in the app.
                }
            } catch (error) {
                console.error("Failed to load user", error);
                setUser(null);
            } finally {
                setIsLoading(false);
            }
        };
        loadUser();
    }, []);

    const signIn = useCallback(async (email: string, password: string) => {
        try {
            const data = await AuthService.login(email, password);
            setUser(data);
        } catch (error) {
            throw error;
        }
    }, []);

    const signOut = useCallback(async () => {
        await AuthService.logout();
        setUser(null);
    }, []);

    const signUp = useCallback(async (email: string, password: string, roles: string[], name: string, phoneNumber: string, gender: string, age: string) => {
        const numericAge = parseInt(age, 10);
        await AuthService.register(email, password, roles, name, phoneNumber, gender, numericAge);
    }, []);

    const hasRole = useCallback((allowedRoles: string[]) => {
        if (!user || !user.roles) {
            return false;
        }

        const normalize = (role: string) => role.trim().toUpperCase().replace(/^ROLE_/, '');
        const allowed = allowedRoles.map(normalize);

        const result = user.roles.some((role: string) => {
            const normalized = normalize(role);
            return allowed.includes(normalized);
        });

        // Log only if allowedRoles are specific management roles to avoid spam on standard checks
        if (allowedRoles.includes('ROLE_ADMIN') || allowedRoles.includes('ROLE_EMPLOYEE')) {
            console.log('[AuthContext] hasRole check:', { allowed, result });
        }

        return result;
    }, [user]);

    const updateUser = useCallback(async (updates: Partial<User>) => {
        if (!user) {
            return;
        }
        const nextUser = { ...user, ...updates };
        setUser(nextUser);
        await AuthService.storeUser(nextUser);
    }, [user]);

    const value = useMemo(() => ({
        user,
        isLoading,
        signIn,
        signOut,
        signUp,
        hasRole,
        updateUser,
    }), [user, isLoading, signIn, signOut, signUp, hasRole, updateUser]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};
