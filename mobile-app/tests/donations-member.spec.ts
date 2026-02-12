import { test, expect } from '@playwright/test';

test.describe('Donations - Member Setup Page', () => {
    const setupMemberSession = async (page: any) => {
        await page.route('**/users/profile', async (route: any) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    id: 100,
                    email: 'member@test.com',
                    roles: ['ROLE_MEMBER'],
                    memberStatus: 'ACTIVE',
                    name: 'Test Member',
                    phoneNumber: '555-1234',
                    preferredLanguage: 'en',
                    gender: 'MALE',
                    age: 30,
                }),
            });
        });

        await page.route('**/notifications', async (route: any) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([]),
            });
        });

        await page.addInitScript(() => {
            const user = {
                token: 'test-token',
                refreshToken: 'refresh-token',
                type: 'Bearer',
                id: 100,
                email: 'member@test.com',
                roles: ['ROLE_MEMBER'],
                name: 'Test Member',
                phoneNumber: '555-1234',
                gender: 'MALE',
                age: 30,
                memberStatus: 'ACTIVE',
                preferredLanguage: 'en',
            };
            localStorage.setItem('userToken', JSON.stringify(user));
        });
    };

    test('loads donation setup with presets and Zeffy option', async ({ page }) => {
        await setupMemberSession(page);

        await page.route('**/donations/options', async (route: any) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    currency: 'CAD',
                    minimumAmount: 1.0,
                    presetAmounts: [10, 25, 50, 100],
                    frequencies: ['ONE_TIME', 'MONTHLY'],
                }),
            });
        });

        const optionsPromise = page.waitForResponse('**/donations/options');
        await page.goto('/donations', { waitUntil: 'domcontentloaded' });
        await optionsPromise;

        await expect(page.getByRole('heading', { name: 'Donations', exact: true })).toBeVisible();
        await expect(page.getByText('Donate with Zeffy')).toBeVisible();
        await expect(page.getByText('Donate on Zeffy')).toBeVisible();
        await expect(page.getByText('Choose an amount')).toBeVisible();
        await expect(page.getByText('CAD 10.00')).toBeVisible();
        await expect(page.getByText('One-time')).toBeVisible();
        await expect(page.getByText('Monthly')).toBeVisible();
    });

    test('submits a donation and shows confirmation', async ({ page }) => {
        await setupMemberSession(page);

        await page.route('**/donations/options', async (route: any) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    currency: 'CAD',
                    minimumAmount: 1.0,
                    presetAmounts: [10, 25, 50, 100],
                    frequencies: ['ONE_TIME', 'MONTHLY'],
                }),
            });
        });

        let submittedPayload: any = null;
        await page.route('**/donations', async (route: any) => {
            if (route.request().method() === 'POST') {
                submittedPayload = route.request().postDataJSON();
                await route.fulfill({
                    status: 201,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        id: 1,
                        amount: 10.0,
                        currency: 'CAD',
                        frequency: 'ONE_TIME',
                        status: 'RECEIVED',
                        createdAt: '2025-01-01T10:00:00',
                        message: 'Your donation was received.',
                    }),
                });
                return;
            }
            await route.continue();
        });

        const optionsPromise = page.waitForResponse('**/donations/options');
        await page.goto('/donations', { waitUntil: 'domcontentloaded' });
        await optionsPromise;

        await page.getByPlaceholder('Enter an amount').fill('10');
        await page.getByPlaceholder('name@example.com').fill('member@test.com');
        await page.getByPlaceholder('Enter your phone').fill('555-1234');
        await page.getByPlaceholder('Canada').fill('Canada');
        await page.getByPlaceholder('Street address').fill('123 Main St');
        await page.getByPlaceholder('City').fill('Montreal');
        await page.getByPlaceholder('Province').fill('QC');
        await page.getByPlaceholder('Postal code').fill('H2X 1Y4');

        await page.getByText('Submit donation').click();
        await expect(page.getByRole('dialog').getByText('Complete payment').first()).toBeVisible();

        await page.getByPlaceholder('Name on card').fill('Test Member');
        await page.getByPlaceholder('Card number').fill('4111111111111111');
        await page.getByPlaceholder('MM/YY').fill('12/30');
        await page.getByPlaceholder('CSC').fill('123');

        await page.getByRole('button', { name: 'Complete payment', exact: true }).click();

        const successCard = page
            .locator('div', { hasText: 'Thank you!' })
            .filter({ hasText: 'Your donation was received.' })
            .first();
        await expect(successCard).toBeVisible();
        await expect(successCard).toContainText('CAD 10.00');
        expect(submittedPayload).toMatchObject({
            amount: 10,
            currency: 'CAD',
            frequency: 'ONE_TIME',
        });
    });

    test('shows validation error when amount is below minimum', async ({ page }) => {
        await setupMemberSession(page);

        await page.route('**/donations/options', async (route: any) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    currency: 'CAD',
                    minimumAmount: 5.0,
                    presetAmounts: [10, 25, 50, 100],
                    frequencies: ['ONE_TIME', 'MONTHLY'],
                }),
            });
        });

        const optionsPromise = page.waitForResponse('**/donations/options');
        await page.goto('/donations', { waitUntil: 'domcontentloaded' });
        await optionsPromise;

        await page.getByPlaceholder('Enter an amount').fill('1');
        await page.getByText('Submit donation').click();

        await expect(page.getByText('Minimum donation is 5.00.')).toBeVisible();
    });
});
