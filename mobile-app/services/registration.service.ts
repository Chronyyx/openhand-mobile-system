// mobile-app/services/registration.service.ts
import { API_BASE } from '../utils/api';

export type RegistrationStatus = 'REQUESTED' | 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED';

export type Registration = {
    id: number;
    userId: number;
    eventId: number;
    eventTitle: string;
    status: RegistrationStatus;
    requestedAt: string;
    confirmedAt: string | null;
    cancelledAt: string | null;
    waitlistedPosition: number | null;
    eventStartDateTime: string | null;
    eventEndDateTime: string | null;
};

export type RegistrationError = {
    status: number;
    error: string;
    message: string;
};

async function handleResponse<T>(res: Response, context: string): Promise<T> {
    if (!res.ok) {
        // Preserve status so the UI can show friendly messages based on HTTP status codes
        const errorText = await res.text();
        let errorData: RegistrationError | null = null;

        try {
            errorData = JSON.parse(errorText);
        } catch {
            // If response isn't JSON, create a simple error object
            errorData = {
                status: res.status,
                error: `HTTP ${res.status}`,
                message: errorText || `Failed during ${context}`,
            };
        }

        const error = new Error(errorData?.message || 'Unknown error') as Error & { 
            status?: number; 
            errorData?: RegistrationError;
        };
        error.status = res.status;
        error.errorData = errorData || undefined;
        throw error;
    }

    return res.json() as Promise<T>;
}

export async function registerForEvent(eventId: number, token: string): Promise<Registration> {
    const url = `${API_BASE}/registrations`;
    const res = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({ eventId }),
    });
    return handleResponse<Registration>(res, 'registration');
}

export async function getMyRegistrations(token: string): Promise<Registration[]> {
    const url = `${API_BASE}/registrations/my-registrations`;
    const res = await fetch(url, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
        },
    });
    return handleResponse<Registration[]>(res, "les inscriptions");
}

export async function cancelRegistration(eventId: number, token: string): Promise<Registration> {
    const url = `${API_BASE}/registrations/event/${eventId}`;
    const res = await fetch(url, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`,
        },
    });
    return handleResponse<Registration>(res, "l'annulation");
}
