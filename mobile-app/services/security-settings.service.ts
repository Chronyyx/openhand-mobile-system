import apiClient from './api.client';

export type SecuritySettings = {
  biometricsEnabled: boolean;
};

export async function getSecuritySettings(): Promise<SecuritySettings> {
  const response = await apiClient.get('/users/me/security-settings');
  return response.data as SecuritySettings;
}

export async function updateSecuritySettings(biometricsEnabled: boolean): Promise<SecuritySettings> {
  const response = await apiClient.put('/users/me/security-settings', { biometricsEnabled });
  return response.data as SecuritySettings;
}
