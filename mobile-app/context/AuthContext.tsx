import React, { createContext, useState, useEffect, useContext } from 'react';
import AuthService from '../services/auth.service';

export interface User {
    token: string;
    type: string;
    id: number;
    email: string;
    roles: string[];
}

interface AuthContextProps {
    user: User | null;
    isLoading: boolean;
    signIn: (email: string, password: string) => Promise<void>;
    signOut: () => Promise<void>;
    signUp: (email: string, password: string, roles: string[]) => Promise<void>;
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

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

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

    const signIn = async (email: string, password: string) => {
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

    const signUp = async (email: string, password: string, roles: string[]) => {
        await AuthService.register(email, password, roles);
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