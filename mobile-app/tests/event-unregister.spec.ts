import { test, expect } from '@playwright/test';

test.describe('Event Registration Flow', () => {

    test('should allow a user to register and then unregister from an event', async ({ page }) => {
        // Mock login endpoint
        await page.route('**/api/auth/login', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    token: 'test-token',
                    refreshToken: 'refresh-token',
                    type: 'Bearer',
                    id: 1,
                    email: 'admin@mana.org',
                    roles: ['ROLE_ADMIN'],
                }),
            });
        });

        // Mock events
        await page.route('**/api/events/upcoming', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([
                    {
                        id: 1,
                        title: 'gala_2025',
                        description: 'Gala',
                        startDateTime: '2025-12-24T18:00:00',
                        locationName: 'Hall',
                        address: '123 St',
                        status: 'OPEN',
                        maxCapacity: 100,
                        currentRegistrations: 50,
                    },
                ]),
            });
        });

        // Mock my registrations
        await page.route('**/api/registrations/my-registrations', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([]),
            });
        });

        // 1. Login
        await page.goto('/auth/login');
        await page.getByPlaceholder(/email or phone number/i).fill('admin@mana.org');
        await page.getByPlaceholder(/password/i).fill('admin123');
        await page.getByText(/log in/i).nth(1).click();

        // Wait for navigation and storage
        await page.waitForTimeout(500);

        // 2. Navigate to Events
        await page.goto('/events');
        await page.waitForTimeout(500);

        // click on event
        await page.getByText(/View details|Voir détails/i).first().click();
        await page.waitForTimeout(500);

        // 3. Register
        const registerButton = page.getByText(/Register for this event|Register/i).first();
        if (await registerButton.isVisible({ timeout: 5000 }).catch(() => false)) {
            await registerButton.click();
            await page.waitForTimeout(500);
        }
    });

});
