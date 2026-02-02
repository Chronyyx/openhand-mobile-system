import { test, expect } from '@playwright/test';

test.describe('Events Screen', () => {
    test.beforeEach(async ({ page }) => {
        const mockUser = {
            token: 'test-token',
            refreshToken: 'refresh-token',
            type: 'Bearer',
            id: 1,
            email: 'test@mana.org',
            roles: ['ROLE_USER'],
        };

        // Mock upcoming events endpoint
        await page.route('**/events/upcoming', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([
                    {
                        id: 1,
                        title: 'gala_2025',
                        description: 'A grand gala event',
                        startDateTime: '2025-12-24T18:00:00',
                        endDateTime: '2025-12-25T02:00:00',
                        locationName: 'Grand Hall',
                        address: '123 Main St',
                        status: 'OPEN',
                        maxCapacity: 100,
                        currentRegistrations: 45,
                    },
                ]),
            });
        });

        // Set auth token before navigating
        await page.addInitScript((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, mockUser);

        await page.goto('/events', { waitUntil: 'domcontentloaded' });
    });

    test('Loads and displays list of events', async ({ page }) => {
        // Check for at least one "View details" button which indicates an event is loaded
        const viewDetails = page.locator('text=/view details|voir détails|details/i').first();
        await expect(viewDetails).toBeVisible({ timeout: 15000 });
    });

    test('Search input exposes an accessible label', async ({ page }) => {
        await expect(page.getByLabel(/search events/i)).toBeVisible();
    });

    test('Opens event details modal when an event is pressed', async ({ page }) => {
        await page.locator('text=/view details|voir détails|details/i').first().click();

        await expect(page.locator('text=/description/i')).toBeVisible();
    });

    test('Closes event modal', async ({ page }) => {
        await page.locator('text=/view details|voir détails|details/i').first().click();
        await expect(page.locator('text=/description/i')).toBeVisible();

        // Close button can be "Close" or "Fermer"
        await page.getByLabel(/close|fermer/i).first().click();

        await expect(page.locator('text=/description/i')).toHaveCount(0);
    });
});
