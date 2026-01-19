import { API_BASE } from '../utils/api';

export type ProfileResponse = {
    profileImageUrl?: string | null;
};

export const uploadProfilePicture = async (
    uri: string,
    mimeType: string | undefined,
    fileName: string | undefined,
    token: string,
    tokenType?: string,
): Promise<ProfileResponse> => {
    const formData = new FormData();
    const name = fileName ?? 'profile.jpg';
    const type = mimeType ?? 'image/jpeg';

    formData.append('file', {
        uri,
        name,
        type,
    } as any);

    const response = await fetch(`${API_BASE}/users/me/profile-picture`, {
        method: 'POST',
        headers: {
            Authorization: `${tokenType ?? 'Bearer'} ${token}`,
        },
        body: formData,
    });

    const payload = await response.json();
    if (!response.ok) {
        const message = payload?.message ?? 'Upload failed';
        throw new Error(message);
    }

    return payload as ProfileResponse;
};
