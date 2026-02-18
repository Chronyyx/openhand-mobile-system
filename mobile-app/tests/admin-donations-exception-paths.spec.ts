import { expect, test } from '@playwright/test';

const adminUser = {
    token: 'admin-token-donations-exceptions',
    refreshToken: 'refresh-token',
    type: 'Bearer',
    id: 1,
    email: 'admin@mana.org',
    roles: ['ROLE_ADMIN'],
    name: 'Admin User',
    phoneNumber: '',
    preferredLanguage: 'en',
    gender: 'MALE',
    age: 35,
    memberStatus: 'ACTIVE',
};

const baseDonation = {
    id: 1,
    userId: 10,
    donorName: 'Ada Lovelace',
    donorEmail: 'ada@mana.org',
    amount: 25,
    currency: 'CAD',
    frequency: 'ONE_TIME',
    status: 'RECEIVED',
    createdAt: '2026-01-01T10:00:00',
};

test.describe('Admin donations exception paths', () => {
    test.beforeEach(async ({ page }) => {
        await page.addInitScript((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);

        await page.route('**/users/profile', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(adminUser),
            });
        });

        await page.route('**/users/me/security-settings', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ biometricsEnabled: false }),
            });
        });

        await page.route('**/notifications', async (route) => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify([]),
                });
                return;
            }
            await route.continue();
        });

        await page.route('**/notifications/unread-count', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ count: 0 }),
            });
        });

        await page.route('**/admin/events/all', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([]),
            });
        });
    });

    test('C) Unauthorized access (403): blocked with error', async ({ page }) => {
        await page.route('**/employee/donations*', async (route) => {
            await route.fulfill({
                status: 403,
                contentType: 'application/json',
                body: JSON.stringify({ message: 'Access denied.' }),
            });
        });

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });

        await expect(page.getByText('Access denied.')).toBeVisible();
        await expect(page.locator('[data-testid="donation-card"]')).toHaveCount(0);
    });

    test('D) No donations found: empty state is shown', async ({ page }) => {
        await page.route('**/employee/donations*', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([]),
            });
        });

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });

        await expect(page.getByText('No donations found.')).toBeVisible();
    });

    test('E) Invalid filter parameters (400): shows error and keeps previous list', async ({ page }) => {
        await page.route('**/employee/donations*', async (route) => {
            const url = new URL(route.request().url());
            if (url.searchParams.get('month') === '99') {
                await route.fulfill({
                    status: 400,
                    contentType: 'application/json',
                    body: JSON.stringify({ message: 'Invalid filter parameters' }),
                });
                return;
            }

            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([baseDonation]),
            });
        });

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });
        await expect(page.getByText('Ada Lovelace')).toBeVisible();

        await page.getByTestId('date-filter-month').fill('99');

        await expect(page.getByText('Invalid filter parameters')).toBeVisible();
        await expect(page.getByText('Ada Lovelace')).toBeVisible();
    });

    test('F) Donation not found (404): shows not found error when opening details', async ({ page }) => {
        await page.route('**/employee/donations*', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([baseDonation]),
            });
        });

        await page.route('**/admin/donations/1', async (route) => {
            await route.fulfill({
                status: 404,
                contentType: 'application/json',
                body: JSON.stringify({ message: 'Donation not found' }),
            });
        });

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });
        await page.getByRole('button', { name: /view details/i }).click();

        await expect(page.getByText('Donation not found')).toBeVisible();
    });

    test('G) System failure (500): shows generic error', async ({ page }) => {
        await page.route('**/employee/donations*', async (route) => {
            await route.fulfill({
                status: 500,
                contentType: 'application/json',
                body: JSON.stringify({}),
            });
        });

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });

        await expect(page.getByText('Unable to load donations.')).toBeVisible();
    });
});
