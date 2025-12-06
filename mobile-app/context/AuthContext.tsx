import React, { createContext, useState, useEffect, useContext } from 'react';
import AuthService from '../services/auth.service';
import { useRouter, useSegments } from 'expo-router';

interface AuthContextProps {
    user: any;
    isLoading: boolean;
    signIn: (email, password) => Promise<void>;
    signOut: () => Promise<void>;
    signUp: (email, password, roles) => Promise<void>;
    hasRole: (roles: string[]) => boolean;
}

const AuthContext = createContext<AuthContextProps>({
    user: null,
    isLoading: true,
    signIn: async () => { },
    signOut: async () => { },
    signUp: async () => { },
    hasRole: () => false,
});

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const rootSegment = useSegments()[0];
    const router = useRouter();

    useEffect(() => {
        const loadUser = async () => {
            try {
                const currentUser = await AuthService.getCurrentUser();
                setUser(currentUser);
            } catch (error) {
                console.error("Failed to load user", error);
                setUser(null);
            } finally {
                setIsLoading(false);
            }
        };
        loadUser();
    }, []);

    // Removed automatic redirection to allow access to home screen without login.
    // Navigation is now handled by explicit user actions (e.g. clicking Login button).

    const signIn = async (email, password) => {
        try {
            const data = await AuthService.login(email, password);
            setUser(data);
        } catch (error) {
            throw error;
        }
    };

    const signOut = async () => {
        await AuthService.logout();
        setUser(null);
    };

    const signUp = async (email, password, roles) => {
        return AuthService.register(email, password, roles);
    };

    const hasRole = (allowedRoles: string[]) => {
        if (!user || !user.roles) return false;
        return user.roles.some(role => allowedRoles.includes(role));
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                isLoading,
                signIn,
                signOut,
                signUp,
                hasRole,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};