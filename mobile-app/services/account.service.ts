import apiClient from './api.client';

export const deactivateAccount = async () => {
    // Prefer the new /auth/deactivate path; fall back to older paths for compatibility
    try {
        const authResp = await apiClient.post('/auth/deactivate');
        return authResp.data;
    } catch (err: any) {
        if (err?.response?.status === 404) {
            // Fallback to /account/deactivate
            try {
                const accountResp = await apiClient.post('/account/deactivate');
                return accountResp.data;
            } catch (secondErr: any) {
                if (secondErr?.response?.status === 404) {
                    // Final fallback to legacy /member/account/deactivate
                    const legacyResp = await apiClient.post('/member/account/deactivate');
                    return legacyResp.data;
                }
                throw secondErr;
            }
        }
        throw err;
    }
};
