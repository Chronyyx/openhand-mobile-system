import axios from 'axios';
import AuthService from './auth.service';

const API_BASE = process.env.EXPO_PUBLIC_API_URL ?? 'http://192.168.2.12:8080/api';

export type ManagedUser = {
    id: number;
    email: string;
    roles: string[];
};

const getAuthHeaders = async () => {
    const currentUser = await AuthService.getCurrentUser();
    if (!currentUser || !currentUser.token) {
        throw new Error('Not authenticated');
    }

    const tokenType = currentUser.type ?? 'Bearer';
    return {
        Authorization: `${tokenType} ${currentUser.token}`,
    };
};

export const fetchAllUsers = async (): Promise<ManagedUser[]> => {
    const headers = await getAuthHeaders();
    const response = await axios.get(`${API_BASE}/admin/users`, { headers });
    return response.data;
};

export const fetchAvailableRoles = async (): Promise<string[]> => {
    const headers = await getAuthHeaders();
    const response = await axios.get(`${API_BASE}/admin/users/roles`, { headers });
    return response.data;
};

export const updateUserRoles = async (userId: number, roles: string[]): Promise<ManagedUser> => {
    const headers = await getAuthHeaders();
    const response = await axios.put(`${API_BASE}/admin/users/${userId}/roles`, { roles }, { headers });
    return response.data;
};
