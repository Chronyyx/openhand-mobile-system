import apiClient from './api.client';

export interface ProfileData {
    name: string;
    phoneNumber?: string;
    preferredLanguage: string;
    gender?: string;
    age?: number;
}

export const getProfile = async () => {
    const response = await apiClient.get('/users/profile');
    return response.data;
};

export const updateProfile = async (data: ProfileData) => {
    const response = await apiClient.patch('/users/profile', data);
    return response.data;
};
