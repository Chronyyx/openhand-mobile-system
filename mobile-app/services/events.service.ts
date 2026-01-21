// mobile-app/services/events.service.ts
import apiClient from './api.client';

export type EventStatus = 'OPEN' | 'NEARLY_FULL' | 'FULL' | 'CANCELLED' | 'COMPLETED';

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
    category?: string | null;
};

// If later you have more fields for the detail, you can extend this.
export type EventDetail = EventSummary;

export type AttendeeInfo = {
    userId: number;
    userName: string;
    userEmail: string;
    registrationStatus: 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED';
    memberStatus: 'ACTIVE' | 'INACTIVE';
    waitlistedPosition: number | null;
    requestedAt: string | null;
    confirmedAt: string | null;
};

export type RegistrationSummary = {
    eventId: number;
    totalRegistrations: number;
    waitlistedCount: number;
    maxCapacity: number | null;
    remainingSpots: number | null;
    percentageFull: number | null;
    attendees?: AttendeeInfo[];
};

export type EventAttendee = {
    attendeeId: number;
    fullName: string | null;
    age: number | null;
};

export type EventAttendeesResponse = {
    eventId: number;
    totalAttendees: number;
    attendees: EventAttendee[];
};

async function handleResponse<T>(res: any, context: string): Promise<T> {
    // Axios response data is already parsed
    return res.data;
}

export async function getUpcomingEvents(): Promise<EventSummary[]> {
    const res = await apiClient.get('/events/upcoming');
    return res.data;
}

export async function getEventById(id: number): Promise<EventDetail> {
    const res = await apiClient.get(`/events/${id}`);
    return res.data;
}

export async function getRegistrationSummary(eventId: number): Promise<RegistrationSummary> {
    const res = await apiClient.get(`/events/${eventId}/registration-summary`);
    return res.data;
}

export async function getEventAttendees(eventId: number): Promise<EventAttendeesResponse> {
    const res = await apiClient.get(`/events/${eventId}/attendees`);
    return res.data;
}
