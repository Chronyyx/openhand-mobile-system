import { API_BASE } from '../utils/api';
import type { EventStatus } from './events.service';
import apiClient from './api.client';

export type AttendanceEventSummary = {
    eventId: number;
    title: string;
    startDateTime: string;
    endDateTime: string | null;
    locationName: string;
    address: string;
    status: EventStatus;
    maxCapacity: number | null;
    registeredCount: number;
    checkedInCount: number;
    occupancyPercent: number | null;
};

export type AttendanceAttendee = {
    userId: number;
    fullName: string | null;
    email: string;
    registrationStatus: 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED';
    checkedIn: boolean;
    checkedInAt: string | null;
};

export type AttendanceEventAttendeesResponse = {
    eventId: number;
    registeredCount: number;
    checkedInCount: number;
    attendees: AttendanceAttendee[];
};

export type AttendanceUpdate = {
    eventId: number;
    userId: number;
    checkedIn: boolean;
    checkedInAt: string | null;
    registeredCount: number;
    checkedInCount: number;
    occupancyPercent: number | null;
};

export type AttendanceReport = {
    eventId: number;
    eventTitle: string;
    eventDate: string;
    totalAttended: number;
    totalRegistered: number;
    attendanceRate: number;
};

async function handleResponse<T>(res: Response, context: string): Promise<T> {
    if (!res.ok) {
        const text = await res.text();
        let errorMsg = `HTTP ${res.status} during ${context}`;
        try {
            const json = JSON.parse(text);
            if (json.message) {
                errorMsg = json.message;
            } else if (json.error) {
                errorMsg = json.error;
            }
        } catch {
            errorMsg = text || errorMsg;
        }
        throw new Error(errorMsg);
    }
    return res.json() as Promise<T>;
}

export async function getAttendanceEvents(token: string): Promise<AttendanceEventSummary[]> {
    const url = `${API_BASE}/employee/attendance/events`;
    const res = await fetch(url, {
        method: 'GET',
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    return handleResponse<AttendanceEventSummary[]>(res, 'attendance events');
}

export async function getAttendanceEventAttendees(
    eventId: number,
    token: string,
): Promise<AttendanceEventAttendeesResponse> {
    const url = `${API_BASE}/employee/attendance/events/${eventId}/attendees`;
    const res = await fetch(url, {
        method: 'GET',
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    return handleResponse<AttendanceEventAttendeesResponse>(res, 'attendance attendees');
}

export async function checkInAttendee(
    eventId: number,
    userId: number,
    token: string,
): Promise<AttendanceUpdate> {
    const url = `${API_BASE}/employee/attendance/events/${eventId}/attendees/${userId}/check-in`;
    const res = await fetch(url, {
        method: 'PUT',
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    return handleResponse<AttendanceUpdate>(res, 'attendance check-in');
}

export async function undoCheckInAttendee(
    eventId: number,
    userId: number,
    token: string,
): Promise<AttendanceUpdate> {
    const url = `${API_BASE}/employee/attendance/events/${eventId}/attendees/${userId}/check-in`;
    const res = await fetch(url, {
        method: 'DELETE',
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    return handleResponse<AttendanceUpdate>(res, 'attendance undo check-in');
}

export async function getAttendanceReports(
    startDate: string,
    endDate: string,
    eventId?: number,
): Promise<AttendanceReport[]> {
    const response = await apiClient.get<AttendanceReport[]>('/admin/attendance-reports', {
        params: {
            startDate,
            endDate,
            ...(eventId != null ? { eventId } : {}),
        },
    });
    return response.data;
}
