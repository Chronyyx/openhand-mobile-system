import { expect, test } from '@playwright/test';

type DonationRow = {
    id: number;
    userId: number | null;
    donorName: string | null;
    donorEmail: string | null;
    amount: number;
    currency: string;
    frequency: string;
    status: string;
    createdAt: string;
};

test.describe('Admin manual donation entry', () => {
    const adminUser = {
        token: 'admin-token-manual-donations',
        refreshToken: 'refresh-token',
        type: 'Bearer',
        id: 1,
        email: 'admin@mana.org',
        roles: ['ROLE_ADMIN'],
        name: 'Admin User',
    };

    const events = [
        {
            id: 1,
            title: 'MANA Recognition Gala',
            startDateTime: '2026-03-15T18:00:00',
            endDateTime: '2026-03-15T22:00:00',
            locationName: 'Centre MANA',
        },
    ];

    const users = [
        {
            id: 10,
            email: 'ada@mana.org',
            roles: ['ROLE_MEMBER'],
            name: 'Ada Lovelace',
            phoneNumber: '',
            gender: 'FEMALE',
            age: 37,
            memberStatus: 'ACTIVE',
            statusChangedAt: null,
        },
        {
            id: 11,
            email: 'grace@mana.org',
            roles: ['ROLE_MEMBER'],
            name: 'Grace Hopper',
            phoneNumber: '',
            gender: 'FEMALE',
            age: 42,
            memberStatus: 'ACTIVE',
            statusChangedAt: null,
        },
    ];

    let donations: DonationRow[] = [
        {
            id: 1,
            userId: 10,
            donorName: 'Ada Lovelace',
            donorEmail: 'ada@mana.org',
            amount: 50,
            currency: 'CAD',
            frequency: 'ONE_TIME',
            status: 'RECEIVED',
            createdAt: '2026-01-15T10:00:00',
        },
    ];

    test.beforeEach(async ({ page }) => {
        donations = [
            {
                id: 1,
                userId: 10,
                donorName: 'Ada Lovelace',
                donorEmail: 'ada@mana.org',
                amount: 50,
                currency: 'CAD',
                frequency: 'ONE_TIME',
                status: 'RECEIVED',
                createdAt: '2026-01-15T10:00:00',
            },
        ];

        await page.route(/\/employee\/donations(?:\?.*)?$/, async (route) => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify(donations),
                });
                return;
            }
            await route.continue();
        });

        await page.route(/\/admin\/events\/all(?:\?.*)?$/, async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(events),
            });
        });

        await page.route(/\/admin\/users\/?(?:\?.*)?$/, async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(users),
            });
        });

        await page.route('**/profile', async (route) => {
            await route.fulfill({
                status: 200,
                headers: { 'content-type': 'application/json' },
                body: JSON.stringify(adminUser),
            });
        });

        await page.route('**/users/me/security-settings', async (route) => {
            await route.fulfill({
                status: 200,
                headers: { 'content-type': 'application/json' },
                body: JSON.stringify({ biometricsEnabled: false }),
            });
        });

        await page.route('**/notifications', async (route) => {
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

        await page.route('**/notifications/unread-count', async (route) => {
            await route.fulfill({
                status: 200,
                headers: { 'content-type': 'application/json' },
                body: JSON.stringify({ count: 0 }),
            });
        });

        await page.addInitScript((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);
    });

    test('Admin can open manual donation as a dedicated screen from donations list', async ({ page }) => {
        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });

        await expect(page.getByText('Donations', { exact: true }).first()).toBeVisible();
        await expect(page.getByText('Ada Lovelace')).toBeVisible();

        await page.getByTestId('open-manual-donation-screen').click();

        await expect(page.getByTestId('manual-donation-title')).toBeVisible();
        await expect(page.getByTestId('donor-type-existing-button')).toBeVisible();
        await expect(page.getByTestId('donor-type-guest-button')).toBeVisible();
        await expect(page.getByTestId('manual-donation-select-user')).toBeVisible();
    });

    test('Admin can submit manual donation for a guest donor', async ({ page }) => {
        let submittedPayload: any = null;

        await page.route(/\/employee\/donations\/manual\/?(?:\?.*)?$/, async (route) => {
            if (route.request().method() === 'POST') {
                submittedPayload = route.request().postDataJSON();

                const createdDonation = {
                    id: 2,
                    userId: null,
                    donorName: submittedPayload.donorName,
                    donorEmail: submittedPayload.donorEmail,
                    amount: submittedPayload.amount,
                    currency: submittedPayload.currency,
                    frequency: 'ONE_TIME',
                    status: 'RECEIVED',
                    createdAt: submittedPayload.donationDate,
                };

                donations = [createdDonation, ...donations];

                await route.fulfill({
                    status: 201,
                    contentType: 'application/json',
                    body: JSON.stringify(createdDonation),
                });
                return;
            }

            await route.continue();
        });

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });
        await page.getByTestId('open-manual-donation-screen').click();

        await page.getByTestId('donor-type-guest-button').click();
        await page.getByPlaceholder('0.00').fill('120');
        await page.getByPlaceholder('YYYY-MM-DD HH:MM').fill('2026-02-01 14:30');
        await page.getByTestId('manual-donation-guest-name').fill('Guest Supporter');
        await page.getByTestId('manual-donation-guest-email').fill('guest.supporter@example.com');

        page.once('dialog', (dialog) => dialog.accept().catch(() => {}));
        await page.getByTestId('manual-donation-submit').click();

        await expect.poll(() => submittedPayload).not.toBeNull();
        expect(submittedPayload.amount).toBe(120);
        expect(submittedPayload.donorName).toBe('Guest Supporter');
        expect(submittedPayload.donorEmail).toBe('guest.supporter@example.com');
        expect(submittedPayload.donorUserId ?? null).toBeNull();

        await expect(page).toHaveURL(/\/admin\/donations/);
        await expect(page.getByText('Guest Supporter')).toBeVisible();
    });

    test('Admin can search and select an existing user for manual donation', async ({ page }) => {
        let submittedPayload: any = null;

        await page.route(/\/employee\/donations\/manual\/?(?:\?.*)?$/, async (route) => {
            if (route.request().method() === 'POST') {
                submittedPayload = route.request().postDataJSON();
                const selectedUser = users.find((user) => user.id === submittedPayload.donorUserId);

                const createdDonation = {
                    id: 3,
                    userId: selectedUser?.id ?? null,
                    donorName: selectedUser?.name ?? 'Unknown donor',
                    donorEmail: selectedUser?.email ?? null,
                    amount: submittedPayload.amount,
                    currency: submittedPayload.currency,
                    frequency: 'ONE_TIME',
                    status: 'RECEIVED',
                    createdAt: submittedPayload.donationDate,
                };

                donations = [createdDonation, ...donations];

                await route.fulfill({
                    status: 201,
                    contentType: 'application/json',
                    body: JSON.stringify(createdDonation),
                });
                return;
            }

            await route.continue();
        });

        await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });
        await page.getByTestId('open-manual-donation-screen').click();

        await page.getByTestId('manual-donation-select-user').click();
        await page.getByTestId('manual-donation-user-search').fill('grace');
        await page.getByTestId('manual-donation-user-option-11').click();

        await page.getByPlaceholder('0.00').fill('75');
        await page.getByPlaceholder('YYYY-MM-DD HH:MM').fill('2026-02-02 09:15');

        page.once('dialog', (dialog) => dialog.accept().catch(() => {}));
        await page.getByTestId('manual-donation-submit').click();

        await expect.poll(() => submittedPayload).not.toBeNull();
        expect(submittedPayload.donorUserId).toBe(11);
        expect(submittedPayload.donorName ?? null).toBeNull();
        expect(submittedPayload.donorEmail ?? null).toBeNull();

        await expect(page).toHaveURL(/\/admin\/donations/);
        await expect(page.getByText('Grace Hopper')).toBeVisible();
    });
});
