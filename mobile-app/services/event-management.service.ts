import axios from 'axios';
import { Platform } from 'react-native';
import AuthService from './auth.service';
import { API_BASE } from '../utils/api';

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
    status: 'OPEN' | 'NEARLY_FULL' | 'FULL' | 'COMPLETED';
    maxCapacity: number | null;
    currentRegistrations: number | null;
    category?: string | null;
    imageUrl?: string | null;
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

export const updateEvent = async (
    id: number,
    payload: CreateEventPayload,
): Promise<ManagedEventResponse> => {
    const headers = await getAuthHeaders();
    const response = await axios.put(`${API_BASE}/admin/events/${id}`, payload, { headers });
    return response.data;
};

export const cancelEvent = async (id: number): Promise<ManagedEventResponse> => {
    const headers = await getAuthHeaders();
    const response = await axios.post(`${API_BASE}/admin/events/${id}/cancel`, {}, { headers });
    return response.data;
};

export const getManagedEvents = async (): Promise<ManagedEventResponse[]> => {
    const headers = await getAuthHeaders();
    const response = await axios.get(`${API_BASE}/employee/events`, { headers });
    return response.data;
};

export const markEventCompleted = async (id: number): Promise<ManagedEventResponse> => {
    const headers = await getAuthHeaders();
    const response = await axios.put(`${API_BASE}/employee/events/${id}/complete`, null, { headers });
    return response.data;
};

export const deleteArchivedEvent = async (id: number): Promise<void> => {
    const headers = await getAuthHeaders();
    await axios.delete(`${API_BASE}/employee/events/${id}`, { headers });
};

export const uploadEventImage = async (
    id: number,
    uri: string,
    fileName?: string,
    mimeType?: string,
): Promise<{ url: string | null }> => {
    const headers = await getAuthHeaders();
    const formData = new FormData();
    const name = fileName || `event-${Date.now()}.jpg`;
    const type = mimeType || 'image/jpeg';

    if (Platform.OS === 'web') {
        const response = await fetch(uri);
        const blob = await response.blob();
        formData.append('file', blob, name);
    } else {
        formData.append('file', {
            uri,
            name,
            type,
        } as any);
    }

    const response = await axios.post(`${API_BASE}/admin/events/${id}/image`, formData, {
        headers: {
            ...headers,
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};
