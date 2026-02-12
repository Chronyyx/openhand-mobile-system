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

export const getManagedDonations = async (): Promise<DonationSummary[]> => {
    const headers = await getAuthHeaders();
    const response = await axios.get(`${API_BASE}/employee/donations`, { headers });
    return response.data;
};

export const getDonationDetail = async (donationId: number): Promise<DonationDetail> => {
    const headers = await getAuthHeaders();
    const response = await axios.get(`${API_BASE}/admin/donations/${donationId}`, { headers });
    return response.data;
};
