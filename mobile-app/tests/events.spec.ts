import { test, expect } from '@playwright/test';

test.describe('Events Screen', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/events', { waitUntil: 'commit' });
    });

    test('Loads and displays list of events', async ({ page }) => {
        // Title can be French or English
        await expect(
            page.locator('text=/ÉVÉNEMENTS DISPONIBLES|AVAILABLE EVENTS/i')
        ).toBeVisible();

        // At least one "View details" / "Voir détails"
        await expect(
            page.locator('text=/view details|voir détails|details/i').first()
        ).toBeVisible();
    });

    test('Opens event details modal when an event is pressed', async ({ page }) => {
        await page.locator('text=/view details|voir détails|details/i').first().click();

        await expect(page.locator('text=/description/i')).toBeVisible();
    });

    test('Closes event modal', async ({ page }) => {
        await page.locator('text=/view details|voir détails|details/i').first().click();
        await expect(page.locator('text=/description/i')).toBeVisible();

        // Close button can be "Close" or "Fermer"
        await page.locator('text=/close|fermer/i').first().click();

        await expect(page.locator('text=/description/i')).toHaveCount(0);
    });
});
