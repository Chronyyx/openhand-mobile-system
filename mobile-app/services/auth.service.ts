import apiClient from './api.client';
import { setItem, getItem, deleteItem } from '../utils/storage';

const register = (email: string, password: string, roles: string[], name: string, phoneNumber: string, gender: string, age: number) => {
    return apiClient.post('/auth/register', {
        email,
        password,
        roles,
        name,
        phoneNumber,
        gender,
        age
    });
};

const login = (email: string, password: string) => {
    console.log(`[AuthService] Attempting login`);
    return apiClient
        .post('/auth/login', {
            email,
            password,
        })
        .then(async (response) => {
            console.log(`[AuthService] Login success.`);
            if (response.data.token) {
                const userToStore = {
                    token: response.data.token,
                    refreshToken: response.data.refreshToken,
                    type: response.data.type,
                    id: response.data.id,
                    email: response.data.email,
                    roles: response.data.roles,
                    name: response.data.name,
                    phoneNumber: response.data.phoneNumber,
                    gender: response.data.gender,
                    age: response.data.age,
                    memberStatus: response.data.memberStatus,
                    statusChangedAt: response.data.statusChangedAt,
                    profilePictureUrl: response.data.profilePictureUrl ?? null,
                };
                console.log(`[AuthService] Storing user.`);
                await setItem('userToken', JSON.stringify(userToStore));
            }
            return response.data;
        });
};

const logout = async () => {
    try {
        await apiClient.post('/auth/logout');
    } catch (e) {
        console.error('[AuthService] Server logout failed', e);
    } finally {
        await deleteItem('userToken');
    }
};

const forgotPassword = (email: string) => {
    return apiClient.post('/auth/forgot-password', { email });
};

const resetPassword = (email: string, code: string, newPassword: string) => {
    return apiClient.post('/auth/reset-password', { email, code, newPassword });
};

const getCurrentUser = async () => {
    const userStr = await getItem('userToken');
    if (userStr) return JSON.parse(userStr);
    return null;
};

const storeUser = async (user: unknown) => {
    await setItem('userToken', JSON.stringify(user));
};

const AuthService = {
    register,
    login,
    logout,
    getCurrentUser,
    forgotPassword,
    resetPassword,
    storeUser,
};

export default AuthService;
