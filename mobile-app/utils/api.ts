import Constants from 'expo-constants';
import { NativeModules, Platform } from 'react-native';

const stripTrailingSlash = (url: string): string => url.replace(/\/+$/, '');

const isLocalHostname = (hostname: string): boolean =>
    hostname === 'localhost' ||
    hostname === '127.0.0.1' ||
    hostname.startsWith('10.') ||
    hostname.startsWith('192.168.') ||
    hostname.startsWith('172.');

const normalizeEnvApiUrl = (raw: string): string => {
    let input = raw.trim();
    if (!/^https?:\/\//i.test(input)) {
        input = `http://${input}`;
    }

    try {
        const url = new URL(input);
        if (!url.port && isLocalHostname(url.hostname)) {
            url.port = '8080';
            if (__DEV__) {
                console.warn(`[API] EXPO_PUBLIC_API_URL missing port; defaulting to 8080 => ${url.hostname}:8080`);
            }
        }

        const pathname = (url.pathname === '/' || url.pathname === '') ? '/api' : url.pathname;
        const normalized = `${url.protocol}//${url.hostname}${url.port ? `:${url.port}` : ''}${pathname}`;
        return stripTrailingSlash(normalized);
    } catch (e) {
        if (__DEV__) {
            console.warn('[API] Could not parse EXPO_PUBLIC_API_URL, using raw string', e);
        }
        return stripTrailingSlash(input);
    }
};

const resolveHostFromScriptUrl = (): string | null => {
    const scriptURL = (NativeModules as any)?.SourceCode?.scriptURL as string | undefined;
    if (!scriptURL) return null;

    const match = scriptURL.match(/https?:\/\/([^:/]+)/);
    return match?.[1] ?? null;
};

const resolveHostFromExpoConfig = (): string | null => {
    const hostUri = Constants.expoConfig?.hostUri;
    if (!hostUri) return null;

    return hostUri.split(':')[0] || null;
};

export const resolveApiBase = (): string => {
    const envUrl = process.env.EXPO_PUBLIC_API_URL;
    if (envUrl?.trim()) {
        return normalizeEnvApiUrl(envUrl);
    }

    const host = resolveHostFromScriptUrl() ?? resolveHostFromExpoConfig();
    if (host) {
        return `http://${host}:8080/api`;
    }

    if (Platform.OS === 'android') {
        return 'http://10.0.2.2:8080/api';
    }

    return 'http://localhost:8080/api';
};

export const API_BASE = resolveApiBase();

if (__DEV__) {
    console.log(`[API] Using base URL: ${API_BASE}`);
}

export const resolvePublicUrl = (path?: string | null): string | null => {
    if (!path) return null;
    if (/^https?:\/\//i.test(path)) return path;

    const base = API_BASE.replace(/\/api\/?$/, '');
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    return `${base}${normalizedPath}`;
};
