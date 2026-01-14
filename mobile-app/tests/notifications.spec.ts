import { test, expect, Page, Route } from '@playwright/test';

type Notification = {
    id: number;
    eventId: number;
    eventTitle: string;
    notificationType: 'REGISTRATION_CONFIRMATION' | 'REMINDER' | 'CANCELLATION';
    textContent: string;
    isRead: boolean;
    createdAt: string;
    readAt: string | null;
};

const userFixture = {
    token: 'test-token',
    refreshToken: 'refresh-token',
    type: 'Bearer',
    id: 1,
    email: 'notifications@test.local',
    roles: ['ROLE_MEMBER'],
};

async function seedAuth(page: Page) {
    await page.addInitScript((storedUser) => {
        localStorage.setItem('userToken', JSON.stringify(storedUser));
    }, userFixture);
}

function buildRoutes(page: Page, initialNotifications: Notification[]) {
    let state = initialNotifications.map((n) => ({ ...n }));

    const fulfillJson = async (route: Route, body: unknown, status = 200) => {
        await route.fulfill({
            status,
            headers: { 'content-type': 'application/json' },
            body: JSON.stringify(body),
        });
    };

    page.route('**/api/notifications', async (route) => {
        if (route.request().method() === 'GET') {
            return fulfillJson(route, state);
        }
        return route.continue();
    });

    page.route('**/api/notifications/unread-count', async (route) => {
        const unread = state.filter((n) => !n.isRead).length;
        return fulfillJson(route, { count: unread });
    });

    page.route(/.*\/api\/notifications\/(\d+)\/read$/, async (route) => {
        if (route.request().method() !== 'PUT') return route.continue();
        const match = route.request().url().match(/\/(\d+)\/read$/);
        const id = match ? Number(match[1]) : 0;
        state = state.map((n) => (n.id === id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n));
        const updated = state.find((n) => n.id === id);
        return fulfillJson(route, updated ?? null);
    });

    page.route('**/api/notifications/read-all', async (route) => {
        if (route.request().method() !== 'PUT') return route.continue();
        state = state.map((n) => ({ ...n, isRead: true, readAt: new Date().toISOString() }));
        return fulfillJson(route, {});
    });

    page.route(/.*\/api\/notifications\/(\d+)$/, async (route) => {
        if (route.request().method() !== 'DELETE') return route.continue();
        const match = route.request().url().match(/\/(\d+)$/);
        const id = match ? Number(match[1]) : 0;
        state = state.filter((n) => n.id !== id);
        await route.fulfill({ status: 204 });
    });
}

test.describe('Notifications (UI)', () => {
    test.beforeEach(async ({ page }) => {
        await seedAuth(page);
    });

    test('shows notifications list with unread badge and marks all as read', async ({ page }) => {
        buildRoutes(page, [
            {
                id: 1,
                eventId: 11,
                eventTitle: 'Gala Confirmation',
                notificationType: 'REGISTRATION_CONFIRMATION',
                textContent: 'You are confirmed for Gala Night',
                isRead: false,
                createdAt: new Date().toISOString(),
                readAt: null,
            },
            {
                id: 2,
                eventId: 22,
                eventTitle: 'Workshop Reminder',
                notificationType: 'REMINDER',
                textContent: 'Workshop starts soon',
                isRead: true,
                createdAt: new Date().toISOString(),
                readAt: new Date().toISOString(),
            },
        ]);

        await page.goto('/notifications');
        await expect(page.getByText(/Please log in to view notifications/i)).toHaveCount(0);
        await expect(page.getByText('Gala Confirmation').first()).toBeVisible();
        await expect(page.getByText('Workshop Reminder').first()).toBeVisible();
        await expect(page.getByText(/Mark all read/i)).toBeVisible();

        const markAllRequest = page.waitForRequest('**/api/notifications/read-all');
        await page.getByText(/Mark all read/i).click();
        await markAllRequest;

        await expect(page.getByText(/Mark all read/i)).toHaveCount(0);
    });

    test('marks a single notification as read when tapped', async ({ page }) => {
        buildRoutes(page, [
            {
                id: 10,
                eventId: 101,
                eventTitle: 'Unread Ticket',
                notificationType: 'REGISTRATION_CONFIRMATION',
                textContent: 'Your ticket is confirmed',
                isRead: false,
                createdAt: new Date().toISOString(),
                readAt: null,
            },
        ]);

        await page.goto('/notifications');
        const markRequest = page.waitForRequest('**/api/notifications/10/read');
        await page.getByText('Unread Ticket').first().click();
        await markRequest;

        await expect(page.getByText(/Mark all read/i)).toHaveCount(0);
    });

    test('shows empty state when there are no notifications', async ({ page }) => {
        buildRoutes(page, []);

        await page.goto('/notifications');
        await expect(page.getByText(/No notifications yet/i)).toBeVisible();
        await expect(page.getByText(/You'll receive notifications/i)).toBeVisible();
    });
});
