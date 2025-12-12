import axios from 'axios';
import AuthService from './auth.service';

const API_BASE = process.env.EXPO_PUBLIC_API_URL ?? 'http://192.168.0.16:8080/api';

export type CreateEventPayload = {
    title: string;
    description: string;
    startDateTime: string;
    endDateTime?: string | null;
    locationName: string;
    address: string;
    maxCapacity?: number | null;
    category?: string | null;
};

type ManagedEventResponse = {
    id: number;
    title: string;
    description: string;
    startDateTime: string;
    endDateTime: string | null;
    locationName: string;
    address: string;
    status: 'OPEN' | 'NEARLY_FULL' | 'FULL';
    maxCapacity: number | null;
    currentRegistrations: number | null;
    category?: string | null;
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

export const createEvent = async (payload: CreateEventPayload): Promise<ManagedEventResponse> => {
    const headers = await getAuthHeaders();
    const response = await axios.post(`${API_BASE}/admin/events`, payload, { headers });
    return response.data;
};

