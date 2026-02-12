import apiClient from './api.client';

export type DonationFrequency = 'ONE_TIME' | 'MONTHLY';
export type DonationStatus = 'RECEIVED' | 'FAILED';

export type DonationOptions = {
    currency: string;
    minimumAmount: number;
    presetAmounts: number[];
    frequencies: DonationFrequency[];
};

export type DonationRequest = {
    amount: number;
    currency: string;
    frequency: DonationFrequency;
};

export type DonationResponse = {
    id: number;
    amount: number;
    currency: string;
    frequency: DonationFrequency;
    status: DonationStatus;
    createdAt: string | null;
    message: string | null;
};

export async function getDonationOptions(): Promise<DonationOptions> {
    const response = await apiClient.get<DonationOptions>('/donations/options');
    return response.data;
}

export async function submitDonation(payload: DonationRequest): Promise<DonationResponse> {
    const response = await apiClient.post<DonationResponse>('/donations', payload);
    return response.data;
}
