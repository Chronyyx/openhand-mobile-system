import { test, expect, Page, Route } from '@playwright/test';

type PreferenceCategory = 'CONFIRMATION' | 'REMINDER' | 'CANCELLATION';

type NotificationPreference = {
    category: PreferenceCategory;
    enabled: boolean;
    isCritical: boolean;
};

const userFixture = {
    token: 'test-token',
    refreshToken: 'refresh-token',
    type: 'Bearer',
    id: 1,
    email: 'preferences@test.local',
    roles: ['ROLE_MEMBER'],
};

async function seedAuth(page: Page) {
    await page.addInitScript((storedUser) => {
        localStorage.setItem('userToken', JSON.stringify(storedUser));
    }, userFixture);
}

function buildPreferencesRoute(page: Page, initial: NotificationPreference[]) {
    let state = initial.map((item) => ({ ...item }));

    const fulfillJson = async (route: Route, body: unknown, status = 200) => {
        await route.fulfill({
            status,
            headers: { 'content-type': 'application/json' },
            body: JSON.stringify(body),
        });
    };

    page.route('**/api/notifications/preferences', async (route) => {
        const method = route.request().method();
        if (method === 'GET') {
            return fulfillJson(route, { memberId: 1, preferences: state });
        }
        if (method === 'PUT') {
            const payload = route.request().postDataJSON() as { preferences: NotificationPreference[] };
            if (payload?.preferences) {
                state = payload.preferences.map((item) => ({ ...item }));
            }
            return fulfillJson(route, { memberId: 1, preferences: state });
        }
        return route.continue();
    });
}

test.describe('Notification Preferences (UI)', () => {
    test.beforeEach(async ({ page }) => {
        await seedAuth(page);
    });

    test('loads preferences and updates a non-critical toggle', async ({ page }) => {
        buildPreferencesRoute(page, [
            { category: 'CONFIRMATION', enabled: true, isCritical: false },
            { category: 'REMINDER', enabled: true, isCritical: false },
            { category: 'CANCELLATION', enabled: true, isCritical: true },
        ]);

        await page.goto('/settings/notifications');
        await expect(page.getByText('Notification Preferences')).toBeVisible();
        await expect(page.getByText('Required', { exact: true })).toBeVisible();

        const reminderToggle = page.locator('[data-testid="notification-toggle-reminder"]');
        await expect(reminderToggle).toBeVisible();

        const updateRequest = page.waitForRequest((request) => {
            return request.url().includes('/api/notifications/preferences') && request.method() === 'PUT';
        });
        await reminderToggle.click();
        const request = await updateRequest;
        const body = request.postDataJSON() as { preferences: NotificationPreference[] };

        const reminder = body.preferences.find((pref) => pref.category === 'REMINDER');
        expect(reminder?.enabled).toBe(false);
    });

    test('critical category toggle stays disabled', async ({ page }) => {
        buildPreferencesRoute(page, [
            { category: 'CONFIRMATION', enabled: true, isCritical: false },
            { category: 'REMINDER', enabled: true, isCritical: false },
            { category: 'CANCELLATION', enabled: true, isCritical: true },
        ]);

        await page.goto('/settings/notifications');
        const cancellationToggle = page.locator('[data-testid="notification-toggle-cancellation"]');
        await expect(cancellationToggle).toBeVisible();

        let putCount = 0;
        page.on('request', (request) => {
            if (request.url().includes('/api/notifications/preferences') && request.method() === 'PUT') {
                putCount += 1;
            }
        });

        await cancellationToggle.click();
        await page.waitForTimeout(500);
        expect(putCount).toBe(0);
    });
});
