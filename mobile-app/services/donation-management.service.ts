import axios from 'axios';
import AuthService from './auth.service';
import { API_BASE } from '../utils/api';

export type DonationSummary = {
    id: number;
    userId: number | null;
    donorName: string | null;
    donorEmail: string | null;
    amount: number;
    currency: string;
    frequency: 'ONE_TIME' | 'MONTHLY' | string;
    status: 'RECEIVED' | 'FAILED' | string;
    createdAt: string | null;
};

export type DonationDetail = DonationSummary & {
    donorPhone: string | null;
    paymentProvider: string | null;
    paymentReference: string | null;
    eventName?: string | null;
};

export type ManualDonationFormData = {
    amount: number;
    currency: string;
    eventId?: number | null;
    donationDate: string; // ISO datetime string
    comments?: string;
    donorUserId?: number | null;
    donorName?: string;
    donorEmail?: string;
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

export const getManagedDonations = async (filters?: { eventId?: number; year?: number; month?: number; day?: number }): Promise<DonationSummary[]> => {
    const headers = await getAuthHeaders();
    let query = '';
    if (filters) {
        const params = [];
        if (filters.eventId) params.push(`eventId=${filters.eventId}`);
        if (filters.year) params.push(`year=${filters.year}`);
        if (filters.month) params.push(`month=${filters.month}`);
        if (filters.day) params.push(`day=${filters.day}`);
        if (params.length > 0) query = '?' + params.join('&');
    }
    const response = await axios.get(`${API_BASE}/employee/donations${query}`, { headers });
    return response.data;
};

export const getDonationDetail = async (donationId: number): Promise<DonationDetail> => {
    const headers = await getAuthHeaders();
    const response = await axios.get(`${API_BASE}/admin/donations/${donationId}`, { headers });
    return response.data;
};

export const submitManualDonation = async (formData: ManualDonationFormData): Promise<DonationSummary> => {
    const headers = await getAuthHeaders();
    const response = await axios.post(
        `${API_BASE}/employee/donations/manual`,
        formData,
        { headers }
    );
    return response.data;
};
