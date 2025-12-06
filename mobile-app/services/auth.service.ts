import axios from 'axios';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

// Updated to use your local LAN IP based on the logs you provided (10.0.0.171)
// This is required for physical devices to reach the backend on your computer.
const API_URL = 'http://10.0.0.171:8080/api/auth/';

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

const register = (email, password, roles) => {
    return axios.post(API_URL + 'register', {
        email,
        password,
        roles,
    });
};

const login = (email, password) => {
    console.log(`[AuthService] Attempting login to: ${API_URL}login`);
    console.log(`[AuthService] Payload:`, { email, password: '***' });

    return axios
        .post(API_URL + 'login', {
            email,
            password,
        })
        .then(async (response) => {
            console.log(`[AuthService] Login success. Status: ${response.status}`);
            if (response.data.token) {
                await setItem('userToken', JSON.stringify(response.data));
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

const AuthService = {
    register,
    login,
    logout,
    getCurrentUser,
};

export default AuthService;
