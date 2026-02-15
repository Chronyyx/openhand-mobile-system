import { test, expect, type Page } from '@playwright/test';

type SessionUser = {
    token: string;
    refreshToken: string;
    type: 'Bearer';
    id: number;
    email: string;
    roles: string[];
    name: string;
    phoneNumber: string;
    preferredLanguage: string;
    gender: string;
    age: number;
    memberStatus: 'ACTIVE' | 'INACTIVE';
};

const adminUser: SessionUser = {
    token: 'admin-token-metrics',
    refreshToken: 'refresh-token',
    type: 'Bearer',
    id: 1,
    email: 'admin@mana.org',
    roles: ['ROLE_ADMIN'],
    name: 'Admin User',
    phoneNumber: '555-1000',
    preferredLanguage: 'en',
    gender: 'MALE',
    age: 35,
    memberStatus: 'ACTIVE',
};

const employeeUser: SessionUser = {
    token: 'employee-token-metrics',
    refreshToken: 'refresh-token',
    type: 'Bearer',
    id: 2,
    email: 'employee@mana.org',
    roles: ['ROLE_EMPLOYEE'],
    name: 'Employee User',
    phoneNumber: '555-2000',
    preferredLanguage: 'en',
    gender: 'FEMALE',
    age: 29,
    memberStatus: 'ACTIVE',
};

const metricsResponse = {
    currency: 'CAD',
    totalDonations: 43,
    totalAmount: 1234.56,
    averageAmount: 28.71,
    uniqueDonors: 17,
    repeatDonors: 5,
    firstTimeDonors: 12,
    frequencyBreakdown: [
        { key: 'ONE_TIME', count: 31, amount: 914.56 },
        { key: 'MONTHLY', count: 12, amount: 320.0 },
    ],
    statusBreakdown: [
        { key: 'RECEIVED', count: 40, amount: 1180.56 },
        { key: 'FAILED', count: 3, amount: 54.0 },
    ],
    monthlyTrend: [
        { period: '2025-11', count: 7, amount: 220.0 },
        { period: '2025-12', count: 8, amount: 275.0 },
        { period: '2026-01', count: 9, amount: 332.0 },
    ],
    topDonorsByAmount: [
        {
            userId: 101,
            donorName: 'Amount Queen',
            donorEmail: 'amount-queen@mana.org',
            donationCount: 4,
            totalAmount: 900.0,
        },
    ],
    topDonorsByCount: [
        {
            userId: 202,
            donorName: 'Count King',
            donorEmail: 'count-king@mana.org',
            donationCount: 9,
            totalAmount: 180.0,
        },
    ],
    manualDonationsCount: 8,
    manualDonationsAmount: 290.0,
    externalDonationsCount: 35,
    externalDonationsAmount: 944.56,
    commentsCount: 11,
    commentsUsageRate: 25.58,
    donationNotificationsCreated: 13,
    donationNotificationsRead: 9,
    donationNotificationsUnread: 4,
};

async function mockAuthenticatedSession(page: Page, user: SessionUser) {
    await page.route('**/api/users/profile', async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(user),
        });
    });

    await page.route('**/api/users/me/security-settings', async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ biometricsEnabled: false }),
        });
    });

    await page.route('**/api/notifications', async (route) => {
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

    await page.route('**/api/notifications/unread-count', async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ count: 0 }),
        });
    });

    await page.addInitScript((sessionUser) => {
        window.localStorage.setItem('userToken', JSON.stringify(sessionUser));
    }, user);
}

test.describe('Admin donations metrics dashboard', () => {
    test('Admin dashboard card opens metrics and menu no longer includes Donations Management', async ({ page }) => {
        let metricsCalls = 0;

        await mockAuthenticatedSession(page, adminUser);
        await page.route('**/api/admin/donations/metrics', async (route) => {
            metricsCalls += 1;
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(metricsResponse),
            });
        });

        await page.goto('/admin', { waitUntil: 'domcontentloaded' });

        const metricsEntry = page.getByRole('button', { name: 'Donations Metrics' });
        await expect(metricsEntry).toBeVisible();

        await metricsEntry.click();
        await expect(page).toHaveURL(/\/admin\/donations-metrics(?:\?|$)/);
        await expect(page.getByText('Total amount')).toBeVisible();
        await expect(page.getByText('CAD 1234.56')).toBeVisible();

        await page.locator('button[aria-label="Open menu"]:visible').first().click();
        await expect(page.getByText('Navigation')).toBeVisible();
        await expect(page.getByText('Donations Management')).toHaveCount(0);
        await expect.poll(() => metricsCalls).toBe(1);
    });

    test('Metrics page renders data, toggles top donors mode, and refreshes data', async ({ page }) => {
        let metricsCalls = 0;

        await mockAuthenticatedSession(page, adminUser);
        await page.route('**/api/admin/donations/metrics', async (route) => {
            metricsCalls += 1;
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(metricsResponse),
            });
        });

        await page.goto('/admin', { waitUntil: 'domcontentloaded' });
        await page.getByRole('button', { name: 'Donations Metrics' }).click();
        await expect(page).toHaveURL(/\/admin\/donations-metrics(?:\?|$)/);

        await expect(page.getByText('Monitor donation performance and donor behavior.')).toBeVisible();
        await expect(page.getByText('Amount Queen')).toBeVisible();
        await expect(page.getByText('Count King')).toHaveCount(0);
        await expect(page.getByText('CAD 1234.56')).toBeVisible();
        await expect(page.getByText('25.58%')).toBeVisible();

        await page.getByText('By count').click();
        await expect(page.getByText('Count King')).toBeVisible();
        await expect(page.getByText('Amount Queen')).toHaveCount(0);

        await page.getByRole('button', { name: 'Refresh metrics' }).click();
        await expect.poll(() => metricsCalls).toBeGreaterThan(1);
    });

    test('Employee cannot access donations metrics screen', async ({ page }) => {
        await mockAuthenticatedSession(page, employeeUser);
        await page.goto('/admin', { waitUntil: 'domcontentloaded' });
        await expect(page.getByRole('button', { name: 'Donations Metrics' })).toHaveCount(0);
        await expect(page.getByText('Administrator Dashboard')).toBeVisible();
    });
});
