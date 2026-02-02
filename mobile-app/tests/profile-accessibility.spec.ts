import { test, expect, Page, Route } from '@playwright/test';

const userFixture = {
    token: 'test-token',
    refreshToken: 'refresh-token',
    type: 'Bearer',
    id: 1,
    email: 'profile@test.local',
    roles: ['ROLE_MEMBER'],
    name: 'Test User',
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

test.describe('Profile accessibility', () => {
    test('exposes labeled actions on profile', async ({ page }) => {
        await seedAuth(page);
        stubNotifications(page);

        await page.goto('/profile');
        await expect(page.getByLabel(/edit/i)).toBeVisible();
        await expect(page.getByLabel(/notification preferences/i)).toBeVisible();
        await expect(page.getByLabel(/logout/i)).toBeVisible();
    });
});
