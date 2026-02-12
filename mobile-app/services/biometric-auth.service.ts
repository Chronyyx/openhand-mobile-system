import axios from 'axios';
import { API_BASE } from '../utils/api';
import { deleteItem, getItem, setItem } from '../utils/storage';
import { isBiometricAvailable, isBiometricEnrolled, promptBiometricAuth } from './local-auth.service';

export const BIOMETRIC_ENABLED_CACHE_KEY = 'biometricsEnabled';
export const BIOMETRIC_REFRESH_TOKEN_KEY = 'biometricRefreshToken';
export const BIOMETRIC_USER_ID_KEY = 'biometricUserId';

type StoredAuthUser = {
  token: string;
  refreshToken: string;
  type: string;
  id: number;
  email: string;
  roles: string[];
  name: string;
  phoneNumber: string;
  gender: string;
  age: number;
  memberStatus?: 'ACTIVE' | 'INACTIVE';
  statusChangedAt?: string | null;
  profilePictureUrl?: string | null;
  preferredLanguage?: string;
};

export async function cacheBiometricsEnabled(enabled: boolean): Promise<void> {
  await setItem(BIOMETRIC_ENABLED_CACHE_KEY, enabled ? 'true' : 'false');
}

export async function getCachedBiometricsEnabled(): Promise<boolean> {
  const value = await getItem(BIOMETRIC_ENABLED_CACHE_KEY);
  return value === 'true';
}

export async function storeBiometricRefreshToken(refreshToken: string, userId: number): Promise<void> {
  await setItem(BIOMETRIC_REFRESH_TOKEN_KEY, refreshToken);
  await setItem(BIOMETRIC_USER_ID_KEY, String(userId));
}

export async function clearBiometricSession(): Promise<void> {
  await deleteItem(BIOMETRIC_REFRESH_TOKEN_KEY);
  await deleteItem(BIOMETRIC_USER_ID_KEY);
}

export async function clearBiometricsCompletely(): Promise<void> {
  await clearBiometricSession();
  await cacheBiometricsEnabled(false);
}

export async function canLoginWithBiometrics(): Promise<boolean> {
  const enabled = await getCachedBiometricsEnabled();
  if (!enabled) {
    return false;
  }
  const refreshToken = await getItem(BIOMETRIC_REFRESH_TOKEN_KEY);
  return Boolean(refreshToken);
}

export async function checkBiometricCapability(): Promise<{ available: boolean; enrolled: boolean }> {
  const available = await isBiometricAvailable();
  const enrolled = available ? await isBiometricEnrolled() : false;
  return { available, enrolled };
}

export async function runBiometricPrompt(promptMessage: string): Promise<{ success: boolean; error?: string }> {
  return promptBiometricAuth(promptMessage);
}

export async function syncBiometricRefreshToken(user: { id: number; refreshToken?: string | null }): Promise<void> {
  const enabled = await getCachedBiometricsEnabled();
  if (!enabled || !user.refreshToken) {
    return;
  }

  const storedUserId = await getItem(BIOMETRIC_USER_ID_KEY);
  if (storedUserId && Number(storedUserId) !== user.id) {
    return;
  }

  await storeBiometricRefreshToken(user.refreshToken, user.id);
}

export async function signInWithBiometricRefresh(promptMessage: string): Promise<StoredAuthUser> {
  const promptResult = await runBiometricPrompt(promptMessage);
  if (!promptResult.success) {
    throw new Error(promptResult.error ?? 'authentication_failed');
  }

  const refreshToken = await getItem(BIOMETRIC_REFRESH_TOKEN_KEY);
  if (!refreshToken) {
    throw new Error('missing_biometric_token');
  }

  const refreshResponse = await axios.post(`${API_BASE}/auth/refreshtoken`, { refreshToken });
  const { accessToken, refreshToken: rotatedRefreshToken } = refreshResponse.data as {
    accessToken: string;
    refreshToken: string;
  };

  const profileResponse = await axios.get(`${API_BASE}/users/profile`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  const profile = profileResponse.data as Omit<StoredAuthUser, 'token' | 'refreshToken' | 'type'>;
  const authenticatedUser: StoredAuthUser = {
    ...profile,
    token: accessToken,
    refreshToken: rotatedRefreshToken,
    type: 'Bearer',
  };

  await setItem('userToken', JSON.stringify(authenticatedUser));
  if (authenticatedUser.id) {
    await storeBiometricRefreshToken(rotatedRefreshToken, authenticatedUser.id);
  }

  return authenticatedUser;
}
