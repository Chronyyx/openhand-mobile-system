import axios from 'axios';
import AuthService from './auth.service';
import { API_BASE } from '../utils/api';

// Centralized API_BASE keeps auth and admin calls pointed at the same backend.

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
