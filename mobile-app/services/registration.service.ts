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

async function handleResponse<T>(res: Response, context: string): Promise<T> {
    if (!res.ok) {
        // Preserve status so the UI can show friendly messages (e.g., 409 already registered)
        const errorText = await res.text();
        const error = new Error(
            errorText || `HTTP ${res.status} during ${context}`,
        ) as Error & { status?: number };
        error.status = res.status;
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
