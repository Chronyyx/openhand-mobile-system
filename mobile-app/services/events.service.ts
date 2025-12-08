// mobile-app/services/events.service.ts

const API_BASE =
    process.env.EXPO_PUBLIC_API_URL ?? 'http://192.168.0.16:8080/api';

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
};

// If later you have more fields for the detail, you can extend this.
export type EventDetail = EventSummary;

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
