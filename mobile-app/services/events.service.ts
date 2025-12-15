// mobile-app/services/events.service.ts
import { API_BASE } from '../utils/api';

export type EventStatus = 'OPEN' | 'NEARLY_FULL' | 'FULL';

export type EventSummary = {
    id: number;
    title: string;
    description: string;
    startDateTime: string;
    endDateTime: string | null;
    locationName: string;
    address: string;
    status: EventStatus;
    maxCapacity: number | null;
    currentRegistrations: number | null;
    category?: string;
};

// If later you have more fields for the detail, you can extend this.
export type EventDetail = EventSummary;

export type RegistrationSummary = {
    eventId: number;
    totalRegistrations: number;
    waitlistedCount: number;
    maxCapacity: number | null;
    remainingSpots: number | null;
    percentageFull: number | null;
};

async function handleResponse<T>(res: Response, context: string): Promise<T> {
    if (!res.ok) {
        throw new Error(`HTTP ${res.status} en chargeant ${context}`);
    }
    return res.json() as Promise<T>;
}

export async function getUpcomingEvents(): Promise<EventSummary[]> {
    const url = `${API_BASE}/events/upcoming`;
    const res = await fetch(url, { method: 'GET' });
    return handleResponse<EventSummary[]>(res, "les événements");
}

export async function getEventById(id: number): Promise<EventDetail> {
    const url = `${API_BASE}/events/${id}`;
    const res = await fetch(url, { method: 'GET' });
    return handleResponse<EventDetail>(res, "l'événement");
}

export async function getRegistrationSummary(eventId: number): Promise<RegistrationSummary> {
    const url = `${API_BASE}/events/${eventId}/registration-summary`;
    const res = await fetch(url, { method: 'GET' });
    return handleResponse<RegistrationSummary>(res, "le résumé des inscriptions");
}
