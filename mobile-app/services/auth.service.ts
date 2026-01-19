import axios from 'axios';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { API_BASE } from '../utils/api';

const API_URL = API_BASE;

const setItem = async (key: string, value: string) => {
    if (Platform.OS === 'web') {
        try {
            localStorage.setItem(key, value);
        } catch (e) {
            console.error('Local storage is unavailable:', e);
        }
    } else {
        await SecureStore.setItemAsync(key, value);
    }
};

const getItem = async (key: string) => {
    if (Platform.OS === 'web') {
        try {
            return localStorage.getItem(key);
        } catch (e) {
            console.error('Local storage is unavailable:', e);
            return null;
        }
    } else {
        return await SecureStore.getItemAsync(key);
    }
};

const deleteItem = async (key: string) => {
    if (Platform.OS === 'web') {
        try {
            localStorage.removeItem(key);
        } catch (e) {
            console.error('Local storage is unavailable:', e);
        }
    } else {
        await SecureStore.deleteItemAsync(key);
    }
};

const register = (email: string, password: string, roles: string[], name: string, phoneNumber: string, gender: string, age: number) => {
    return axios.post(API_URL + '/auth/register', {
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
    console.log(`[AuthService] Attempting login to: ${API_URL}/auth/login`);
    console.log(`[AuthService] Payload:`, { email, password: '***' });

    return axios
        .post(API_URL + '/auth/login', {
            email,
            password,
        })
        .then(async (response) => {
            console.log(`[AuthService] Login success. Status: ${response.status}`);
            console.log(`[AuthService] Response data:`, response.data);
            if (response.data.token) {
                const userToStore = {
                    token: response.data.token,
                    refreshToken: response.data.refreshToken,
                    type: response.data.type,
                    id: response.data.id,
                    email: response.data.email,
                    roles: response.data.roles,
                    name: response.data.name,
                    profileImageUrl: response.data.profileImageUrl ?? null,
                    phoneNumber: response.data.phoneNumber,
                    gender: response.data.gender,
                    age: response.data.age,
                };
                console.log(`[AuthService] Storing user:`, userToStore);
                await setItem('userToken', JSON.stringify(userToStore));
            }
            return response.data;
        })
        .catch((error) => {
            console.error(`[AuthService] Login failed.`);
            if (error.response) {
                // The request was made and the server responded with a status code
                // that falls out of the range of 2xx
                console.error(`[AuthService] Server responded with error:`, error.response.status, error.response.data);
            } else if (error.request) {
                // The request was made but no response was received
                console.error(`[AuthService] No response received. Possible network issue or incorrect API_URL. Request details:`, error.request);
            } else {
                // Something happened in setting up the request that triggered an Error
                console.error(`[AuthService] Error setting up request:`, error.message);
            }
            throw error;
        });
};

const logout = async () => {
    await deleteItem('userToken');
};

const getCurrentUser = async () => {
    const userStr = await getItem('userToken');
    if (userStr) return JSON.parse(userStr);
    return null;
};

const setCurrentUser = async (user: unknown) => {
    await setItem('userToken', JSON.stringify(user));
};

const AuthService = {
    register,
    login,
    logout,
    getCurrentUser,
    setCurrentUser,
};

export default AuthService;
