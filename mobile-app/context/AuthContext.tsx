import React, { createContext, useState, useEffect, useContext, useMemo, useCallback } from 'react';
import AuthService from '../services/auth.service';
import { cacheBiometricsEnabled } from '../services/biometric-auth.service';

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
    signInWithBiometrics: (promptMessage: string) => Promise<void>;
    signOut: () => Promise<void>;
    signUp: (email: string, password: string, roles: string[], name: string, phoneNumber: string, gender: string, age: string) => Promise<void>;
    hasRole: (roles: string[]) => boolean;
    updateUser: (updates: Partial<User>) => Promise<void>;
}

const AuthContext = createContext<AuthContextProps>({
    user: null,
    isLoading: true,
    signIn: async () => { },
    signInWithBiometrics: async () => { },
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
            console.log('[AuthContext] loadUser - START');
            try {
                // 1. Load from storage first for speed
                const storedUser = await AuthService.getCurrentUser();
                console.log('[AuthContext] storedUser from storage:', storedUser ? 'Found' : 'NOT FOUND');
                if (storedUser) {
                    console.log('[AuthContext] Setting user and isLoading=false IMMEDIATELY');
                    setUser(storedUser);
                    setIsLoading(false); // ← Set loading false IMMEDIATELY after setting user from storage
                    // 2. Fetch fresh data from API in background to ensure sync
                    try {
                        const freshUser = await AuthService.getProfile();
                        console.log('[AuthContext] Fresh user from API:', freshUser ? 'Found' : 'NOT FOUND');
                        // CRITICAL: Merge fresh profile data with stored tokens
                        const mergedUser = {
                            ...freshUser,
                            token: storedUser.token,
                            refreshToken: storedUser.refreshToken,
                            type: storedUser.type,
                        };
                        setUser(mergedUser);
                        await AuthService.storeUser(mergedUser);
                        const settings = await AuthService.syncBiometricStateFromBackend();
                        if (settings) {
                            await cacheBiometricsEnabled(settings.biometricsEnabled);
                        }
                    } catch (refreshError) {
                        console.warn('[Auth] Failed to refresh profile on load', refreshError);
                        // If token is invalid, apiClient interceptor will handle cleanup
                    }
                } else {
                    console.log('[AuthContext] No stored user, setting isLoading=false');
                    setIsLoading(false);
                }
            } catch (error) {
                console.error("Failed to load user", error);
                setUser(null);
                setIsLoading(false);
            }
        };
        loadUser();
    }, []);

    const signIn = useCallback(async (email: string, password: string) => {
        try {
            const data = await AuthService.login(email, password);
            setUser(data);
            const settings = await AuthService.syncBiometricStateFromBackend();
            if (settings) {
                await cacheBiometricsEnabled(settings.biometricsEnabled);
            }
        } catch (error) {
            throw error;
        }
    }, []);

    const signInWithBiometrics = useCallback(async (promptMessage: string) => {
        const data = await AuthService.loginWithBiometrics(promptMessage);
        setUser(data);
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
        signInWithBiometrics,
        signOut,
        signUp,
        hasRole,
        updateUser,
    }), [user, isLoading, signIn, signInWithBiometrics, signOut, signUp, hasRole, updateUser]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};
