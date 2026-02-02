import { test, expect, Page, Route } from '@playwright/test';

const userFixture = {
    token: 'test-token',
    refreshToken: 'refresh-token',
    type: 'Bearer',
    id: 1,
    email: 'registrations@test.local',
    roles: ['ROLE_MEMBER'],
};

async function seedAuth(page: Page) {
    await page.addInitScript((storedUser) => {
        localStorage.setItem('userToken', JSON.stringify(storedUser));
    }, userFixture);
}

function stubNotifications(page: Page) {
    const fulfillJson = async (route: Route, body: unknown, status = 200) => {
        await route.fulfill({
            status,
            headers: { 'content-type': 'application/json' },
            body: JSON.stringify(body),
        });
    };

    page.route('**/api/notifications', async (route) => {
        if (route.request().method() === 'GET') {
            return fulfillJson(route, []);
        }
        return route.continue();
    });

    page.route('**/api/notifications/unread-count', async (route) => {
        return fulfillJson(route, { count: 0 });
    });
}

function stubRegistrations(page: Page) {
    const fulfillJson = async (route: Route, body: unknown, status = 200) => {
        await route.fulfill({
            status,
            headers: { 'content-type': 'application/json' },
            body: JSON.stringify(body),
        });
    };

    page.route('**/api/registrations/me?filter=ALL', async (route) => {
        return fulfillJson(route, [
            {
                registrationId: 1,
                status: 'CONFIRMED',
                createdAt: new Date().toISOString(),
                timeCategory: 'ACTIVE',
                event: {
                    eventId: 99,
                    title: 'gala_2025',
                    startDateTime: '2025-12-24T18:00:00',
                    endDateTime: '2025-12-25T02:00:00',
                    location: 'Grand Hall',
                },
                participants: [],
            },
        ]);
    });
}

test.describe('Registrations accessibility', () => {
    test('renders a titled registrations screen', async ({ page }) => {
        await seedAuth(page);
        stubNotifications(page);
        stubRegistrations(page);

        await page.goto('/registrations');
        await expect(page.getByRole('heading', { name: /registrations/i })).toBeVisible();
    });
});
