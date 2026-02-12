import { Platform } from 'react-native';

type LocalAuthMockState = {
  isAvailable?: boolean;
  isEnrolled?: boolean;
  authenticateSuccess?: boolean;
  error?: string;
};

function getMockState(): LocalAuthMockState | undefined {
  return (globalThis as { __LOCAL_AUTH_MOCK__?: LocalAuthMockState }).__LOCAL_AUTH_MOCK__;
}

type LocalAuthenticationModule = {
  hasHardwareAsync: () => Promise<boolean>;
  isEnrolledAsync: () => Promise<boolean>;
  authenticateAsync: (options: {
    promptMessage: string;
    cancelLabel: string;
    disableDeviceFallback: boolean;
  }) => Promise<{ success: boolean; error?: string }>;
};

function getLocalAuthenticationModule(): LocalAuthenticationModule | null {
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('expo-local-authentication') as LocalAuthenticationModule;
  } catch {
    return null;
  }
}

export async function isBiometricAvailable(): Promise<boolean> {
  if (Platform.OS === 'web') {
    return Boolean(getMockState()?.isAvailable);
  }
  const localAuthentication = getLocalAuthenticationModule();
  if (!localAuthentication) {
    return false;
  }
  return localAuthentication.hasHardwareAsync();
}

export async function isBiometricEnrolled(): Promise<boolean> {
  if (Platform.OS === 'web') {
    return Boolean(getMockState()?.isEnrolled);
  }
  const localAuthentication = getLocalAuthenticationModule();
  if (!localAuthentication) {
    return false;
  }
  return localAuthentication.isEnrolledAsync();
}

export async function promptBiometricAuth(promptMessage: string): Promise<{ success: boolean; error?: string }> {
  if (Platform.OS === 'web') {
    const mock = getMockState();
    if (!mock?.isAvailable || !mock?.isEnrolled) {
      return { success: false, error: 'not_available' };
    }
    return {
      success: Boolean(mock.authenticateSuccess),
      error: mock.authenticateSuccess ? undefined : (mock.error ?? 'authentication_failed'),
    };
  }

  const localAuthentication = getLocalAuthenticationModule();
  if (!localAuthentication) {
    return { success: false, error: 'not_available' };
  }

  const result = await localAuthentication.authenticateAsync({
    promptMessage,
    cancelLabel: 'Cancel',
    disableDeviceFallback: false,
  });

  return {
    success: result.success,
    error: result.success ? undefined : result.error,
  };
}
