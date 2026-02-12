import { test, expect } from '@playwright/test';

test.describe('Admin donations management', () => {
    const adminUser = {
        token: 'admin-token',
        refreshToken: 'refresh-token',
        type: 'Bearer',
        id: 1,
        email: 'admin@mana.org',
        roles: ['ROLE_ADMIN'],
        name: 'Admin User',
    };

    const employeeUser = {
        token: 'employee-token',
        refreshToken: 'refresh-token',
        type: 'Bearer',
        id: 2,
        email: 'employee@mana.org',
        roles: ['ROLE_EMPLOYEE'],
        name: 'Employee User',
    };

    const donations = [
        {
            id: 1,
            userId: 10,
            donorName: 'Ada Lovelace',
            donorEmail: 'ada@mana.org',
            amount: 25,
            currency: 'CAD',
            frequency: 'ONE_TIME',
            status: 'RECEIVED',
            createdAt: '2025-01-01T10:00:00',
        },
    ];

    test.beforeEach(async ({ page }) => {
        await page.route('**/api/employee/donations', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(donations),
            });
        });

        await page.route('**/api/notifications', async (route) => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    headers: { 'content-type': 'application/json' },
                    body: JSON.stringify([]),
                });
                return;
            }
            await route.continue();
        });

        await page.route('**/api/notifications/unread-count', async (route) => {
            await route.fulfill({
                status: 200,
                headers: { 'content-type': 'application/json' },
                body: JSON.stringify({ count: 0 }),
            });
        });
    });

    test('Admin can view donation details', async ({ page }) => {
        await page.route('**/api/admin/donations/1', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    ...donations[0],
                    donorPhone: '+15141234567',
                    paymentProvider: 'Zeffy',
                    paymentReference: 'PAY-123',
                }),
            });
        });

        await page.addInitScript((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });

        await expect(page.getByText('Ada Lovelace')).toBeVisible();
        await page.getByText(/view details/i).click();

        await expect(page.getByText(/donation details/i)).toBeVisible();
        await expect(page.getByText(/Zeffy/i)).toBeVisible();
    });

    test('Employee can view list but not details', async ({ page }) => {
        await page.addInitScript((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, employeeUser);

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });

        await expect(page.getByText('Ada Lovelace')).toBeVisible();
        await expect(page.getByText(/view details/i)).toHaveCount(0);
    });

    test('Member is redirected away from admin donations', async ({ page }) => {
        const memberUser = {
            token: 'member-token',
            refreshToken: 'refresh-token',
            type: 'Bearer',
            id: 3,
            email: 'member@mana.org',
            roles: ['ROLE_MEMBER'],
        };

        await page.addInitScript((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, memberUser);

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });

        await expect(page.getByText(/browse events/i)).toBeVisible();
    });
});
