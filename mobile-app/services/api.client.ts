import axios from 'axios';
import { API_BASE } from '../utils/api';
import { getItem, setItem, deleteItem } from '../utils/storage';
import { clearBiometricSession, syncBiometricRefreshToken } from './biometric-auth.service';

const apiClient = axios.create({
    baseURL: API_BASE,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request Interceptor: Attach Token
apiClient.interceptors.request.use(
    async (config) => {
        const userStr = await getItem('userToken');
        console.log('[API] Request interceptor - userStr:', userStr ? 'Found' : 'NOT FOUND');
        if (userStr) {
            try {
                const user = JSON.parse(userStr);
                console.log('[API] Request interceptor - user.token:', user?.token ? `Bearer ${user.token.substring(0, 20)}...` : 'NO TOKEN');
                if (user?.token) {
                    config.headers.Authorization = `Bearer ${user.token}`;
                }
            } catch (e) {
                console.error('[API] Error parsing user token', e);
            }
        } else {
            console.warn('[API] No userToken found in storage for request:', config.url);
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Response Interceptor: Refresh Token
apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const userStr = await getItem('userToken');
                if (!userStr) {
                    return Promise.reject(error);
                }

                const user = JSON.parse(userStr);
                const refreshToken = user.refreshToken;

                if (!refreshToken) {
                    await deleteItem('userToken');
                    return Promise.reject(error);
                }

                console.log('[API] Converting 401 to Refresh Token Request');

                // Use a separate axios instance to avoid infinite loops
                const refreshResponse = await axios.post(`${API_BASE}/auth/refreshtoken`, {
                    refreshToken,
                });

                const { accessToken, refreshToken: newRefreshToken } = refreshResponse.data;

                // Update stored user
                const updatedUser = {
                    ...user,
                    token: accessToken,
                    refreshToken: newRefreshToken,
                };
                await setItem('userToken', JSON.stringify(updatedUser));
                await syncBiometricRefreshToken({
                    id: updatedUser.id,
                    refreshToken: updatedUser.refreshToken,
                });

                // Update header and retry
                apiClient.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
                originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;

                return apiClient(originalRequest);

            } catch (refreshError) {
                console.error('[API] Token refresh failed', refreshError);
                await deleteItem('userToken');
                await clearBiometricSession();
                // Ideally trigger a redirect to login here, but since we are in a service,
                // we reject and let the UI handle or AuthContext to pick up the state change eventually.
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);

export default apiClient;
