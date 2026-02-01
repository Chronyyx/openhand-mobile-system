import { test, expect } from '@playwright/test';

test.describe('Admin users management', () => {
    const adminUser = {
        token: 'admin-token',
        refreshToken: 'refresh-token',
        type: 'Bearer',
        id: 1,
        email: 'admin@mana.org',
        roles: ['ROLE_ADMIN'],
        name: 'Admin User',
    };

    test('Admin can search users and view disabled tab', async ({ page }) => {
        const users = [
            {
                id: 10,
                email: 'active@test.com',
                roles: ['ROLE_MEMBER'],
                name: 'Active Member',
                phoneNumber: '',
                gender: 'FEMALE',
                age: 25,
                memberStatus: 'ACTIVE',
                statusChangedAt: null,
            },
            {
                id: 11,
                email: 'inactive@test.com',
                roles: ['ROLE_MEMBER'],
                name: 'Inactive Member',
                phoneNumber: '',
                gender: 'MALE',
                age: 30,
                memberStatus: 'INACTIVE',
                statusChangedAt: '2025-01-01T10:00:00',
            },
        ];

        await page.route('**/api/admin/users', async (route) => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify(users),
                });
            } else {
                await route.continue();
            }
        });

        await page.route('**/api/admin/users/roles', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(['ROLE_ADMIN', 'ROLE_MEMBER', 'ROLE_EMPLOYEE']),
            });
        });

        await page.addInitScript((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);

        await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(500);

        await expect(page.getByText('Active Member')).toBeVisible();
        await expect(page.getByText('Inactive Member')).toHaveCount(0);

        const searchInput = page.getByPlaceholder(/search by name or email/i);
        await searchInput.fill('active@test.com');
        await expect(page.getByText('Active Member')).toBeVisible();

        await searchInput.fill('inactive@test.com');
        await expect(page.getByText('Active Member')).toHaveCount(0);

        await page.getByText(/disabled/i).click();
        await expect(page.getByText('Inactive Member')).toBeVisible();
    });

    test('Admin can update user details and deactivate an account', async ({ page }) => {
        const baseUser = {
            id: 20,
            email: 'member@test.com',
            roles: ['ROLE_MEMBER'],
            name: 'Member One',
            phoneNumber: '',
            gender: 'MALE',
            age: 32,
            memberStatus: 'ACTIVE',
            statusChangedAt: null,
        };

        await page.route('**/api/admin/users', async (route) => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify([baseUser]),
                });
            } else {
                await route.continue();
            }
        });

        await page.route('**/api/admin/users/roles', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(['ROLE_ADMIN', 'ROLE_MEMBER', 'ROLE_EMPLOYEE']),
            });
        });

        let profilePayload: any = null;
        await page.route('**/api/admin/users/20/profile', async (route) => {
            if (route.request().method() === 'PUT') {
                profilePayload = JSON.parse(route.request().postData() ?? '{}');
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        ...baseUser,
                        ...profilePayload,
                    }),
                });
            } else {
                await route.continue();
            }
        });

        await page.route('**/api/admin/users/20/status', async (route) => {
            if (route.request().method() === 'PUT') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        ...baseUser,
                        ...(profilePayload ?? {}),
                        memberStatus: 'INACTIVE',
                        statusChangedAt: '2025-01-02T10:00:00',
                    }),
                });
            } else {
                await route.continue();
            }
        });

        await page.addInitScript((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);

        await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(500);

        await page.getByText('Edit').first().click();

        const nameInput = page.getByPlaceholder(/full name/i);
        await nameInput.fill('Updated Member');
        await page.getByText(/save changes/i).click();

        await page.waitForTimeout(500);
        expect(profilePayload?.name).toBe('Updated Member');
        await expect(page.getByText('Updated Member')).toBeVisible();

        await page.getByText('Edit').first().click();

        page.once('dialog', (dialog) => dialog.accept().catch(() => {}));
        await page.getByText(/deactivate account/i).click();

        await page.waitForTimeout(500);
        await expect(page.getByText('Updated Member')).toHaveCount(0);

        await page.getByText(/disabled/i).click();
        await expect(page.getByText('Updated Member')).toBeVisible();
    });
});
