// mobile-app/services/employee.service.ts
import { API_BASE } from '../utils/api';

export type EmployeeSearchResult = {
    id: number;
    email: string;
    roles: string[];
};

async function handleResponse<T>(res: Response, context: string): Promise<T> {
    if (!res.ok) {
        const text = await res.text();
        let errorMsg = `HTTP ${res.status} during ${context}`;
        try {
            const json = JSON.parse(text);
            // Extract the message field if available, otherwise use error field
            if (json.message) {
                errorMsg = json.message;
            } else if (json.error) {
                errorMsg = json.error;
            }
        } catch {
            // If not JSON, use the raw text
            errorMsg = text || errorMsg;
        }
        throw new Error(errorMsg);
    }
    return res.json() as Promise<T>;
}

export async function searchUsers(query: string, token: string): Promise<EmployeeSearchResult[]> {
    const url = `${API_BASE}/employee/users/search?query=${encodeURIComponent(query)}`;
    const res = await fetch(url, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
        },
    });
    return handleResponse<EmployeeSearchResult[]>(res, 'employee user search');
}

export async function registerParticipantForEvent(eventId: number, userId: number, token: string) {
    const url = `${API_BASE}/employee/registrations`;
    const res = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({ eventId, userId }),
    });
    return handleResponse(res, 'employee register participant');
}
