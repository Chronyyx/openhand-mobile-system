import { Platform } from 'react-native';
import apiClient from './api.client';

export type ProfilePictureResponse = {
    url: string | null;
};

export const getProfilePicture = async () => {
    const res = await apiClient.get<ProfilePictureResponse>('/users/profile-picture');
    return res.data;
};

export const uploadProfilePicture = async (uri: string, fileName?: string, mimeType?: string) => {
    const formData = new FormData();
    const name = fileName || `profile-${Date.now()}.jpg`;
    const type = mimeType || 'image/jpeg';

    if (Platform.OS === 'web') {
        // On Web, we must convert the URI to a Blob
        const response = await fetch(uri);
        const blob = await response.blob();
        formData.append('file', blob, name);
    } else {
        // On Native (iOS/Android), axios/RN expects this object format
        formData.append('file', {
            uri,
            name,
            type,
        } as any);
    }

    const res = await apiClient.post<ProfilePictureResponse>('/users/profile-picture', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });

    return res.data;
};
