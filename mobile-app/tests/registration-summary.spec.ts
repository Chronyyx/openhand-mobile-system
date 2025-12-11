import { test, expect } from '@playwright/test';

test.describe('Registration Summary Feature', () => {
    test('Displays registration summary for admin users when event modal opens', async ({ page }) => {
        // Mock admin user with ROLE_ADMIN
        const adminUser = {
            token: 'test-token-admin',
            type: 'Bearer',
            id: 1,
            email: 'admin@mana.org',
            roles: ['ROLE_ADMIN'],
        };

        await page.goto('/', { waitUntil: 'domcontentloaded' });
        await page.evaluate((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);

        // Navigate to events and open first event modal
        await page.goto('/events', { waitUntil: 'domcontentloaded' });
        const viewDetails = page.getByText(/view details|voir détails|details/i).first();
        await expect(viewDetails).toBeVisible({ timeout: 15000 });
        await viewDetails.click();

        // Wait for modal and registration summary to load
        await expect(page.getByText(/description/i)).toBeVisible({ timeout: 10000 });
        
        // Registration summary should be visible for admin
        // Check for summary title (could be in French or English)
        const summaryTitle = page.locator('text=/résumé des inscriptions|registration summary/i');
        await expect(summaryTitle).toBeVisible({ timeout: 10000 });
    });

    test('Displays confirmed registrations and waitlisted count in summary', async ({ page }) => {
        // Mock admin user
        const adminUser = {
            token: 'test-token-admin',
            type: 'Bearer',
            id: 1,
            email: 'admin@mana.org',
            roles: ['ROLE_ADMIN'],
        };

        await page.goto('/', { waitUntil: 'domcontentloaded' });
        await page.evaluate((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);

        // Navigate to events
        await page.goto('/events', { waitUntil: 'domcontentloaded' });
        const viewDetails = page.getByText(/view details|voir détails|details/i).first();
        await expect(viewDetails).toBeVisible({ timeout: 15000 });
        await viewDetails.click();

        // Wait for modal and summary to load
        await expect(page.getByText(/description/i)).toBeVisible({ timeout: 10000 });
        await expect(page.locator('text=/résumé des inscriptions|registration summary/i')).toBeVisible({ timeout: 10000 });

        // Check for confirmed registrations label and value
        const confirmedLabel = page.locator('text=/inscriptions confirmées|confirmed registrations/i');
        await expect(confirmedLabel).toBeVisible();

        // Check for waitlisted label
        const waitlistedLabel = page.locator('text=/en attente|waitlisted/i');
        await expect(waitlistedLabel).toBeVisible();

        // Numeric values should be present (at least 0)
        const numberPattern = page.locator('text=/\\d+/');
        const numberCount = await numberPattern.count();
        expect(numberCount).toBeGreaterThan(0);
    });

    test('Shows capacity and remaining spots in summary with progress bar', async ({ page }) => {
        // Mock employee user with ROLE_EMPLOYEE
        const employeeUser = {
            token: 'test-token-employee',
            type: 'Bearer',
            id: 2,
            email: 'employee@mana.org',
            roles: ['ROLE_EMPLOYEE'],
        };

        await page.goto('/', { waitUntil: 'domcontentloaded' });
        await page.evaluate((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, employeeUser);

        // Navigate to events
        await page.goto('/events', { waitUntil: 'domcontentloaded' });
        const viewDetails = page.getByText(/view details|voir détails|details/i).first();
        await expect(viewDetails).toBeVisible({ timeout: 15000 });
        await viewDetails.click();

        // Wait for modal and summary to load
        await expect(page.getByText(/description/i)).toBeVisible({ timeout: 10000 });
        const summaryTitle = page.locator('text=/résumé des inscriptions|registration summary/i');
        await expect(summaryTitle).toBeVisible({ timeout: 10000 });

        // Get the modal container to target capacity/remaining spots within summary section
        const modalBody = page.locator('[style*="modalBody"]').or(page.locator('div').filter({ has: summaryTitle })).first();

        // Check for capacity and remaining spots labels within modal
        const capacityText = modalBody.locator('text=/capacité|capacity/i').last();
        await expect(capacityText).toBeVisible({ timeout: 5000 });

        const remainingText = modalBody.locator('text=/places restantes|remaining spots/i').last();
        await expect(remainingText).toBeVisible({ timeout: 5000 });

        // Progress bar should be present (look for percentage text)
        const percentageText = page.locator('text=/%/');
        await expect(percentageText).toBeVisible();
    });

    test('Registration summary is not visible for regular members', async ({ page }) => {
        // Mock member user (should NOT see summary)
        const memberUser = {
            token: 'test-token-member',
            type: 'Bearer',
            id: 3,
            email: 'member@mana.org',
            roles: ['ROLE_MEMBER'],
        };

        await page.goto('/', { waitUntil: 'domcontentloaded' });
        await page.evaluate((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, memberUser);

        // Navigate to events
        await page.goto('/events', { waitUntil: 'domcontentloaded' });
        const viewDetails = page.getByText(/view details|voir détails|details/i).first();
        await expect(viewDetails).toBeVisible({ timeout: 15000 });
        await viewDetails.click();

        // Wait for modal to load
        await expect(page.getByText(/description/i)).toBeVisible({ timeout: 10000 });

        // Registration summary should NOT be visible
        const summaryTitle = page.locator('text=/résumé des inscriptions|registration summary/i');
        const summaryCount = await summaryTitle.count();
        expect(summaryCount).toBe(0);

        // But register button should be visible for members
        const registerButton = page.locator('text=/rejoindre la liste d\'attente|s\'inscrire|register|join waitlist/i');
        const registerButtonCount = await registerButton.count();
        expect(registerButtonCount).toBeGreaterThan(0);
    });

    test('Registration summary updates after refreshing event modal', async ({ page }) => {
        // Mock admin user
        const adminUser = {
            token: 'test-token-admin',
            type: 'Bearer',
            id: 1,
            email: 'admin@mana.org',
            roles: ['ROLE_ADMIN'],
        };

        await page.goto('/', { waitUntil: 'domcontentloaded' });
        await page.evaluate((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);

        // Navigate to events
        await page.goto('/events', { waitUntil: 'domcontentloaded' });
        const viewDetails = page.getByText(/view details|voir détails|details/i).first();
        await expect(viewDetails).toBeVisible({ timeout: 15000 });
        await viewDetails.click();

        // Wait for summary to load
        await expect(page.locator('text=/résumé des inscriptions|registration summary/i')).toBeVisible({ timeout: 10000 });

        // Close modal
        await page.locator('text=/close|fermer/i').first().click();
        await expect(page.getByText(/description/i)).toHaveCount(0);

        // Reopen same event (or next event)
        const viewDetailsAgain = page.getByText(/view details|voir détails|details/i).first();
        await expect(viewDetailsAgain).toBeVisible({ timeout: 10000 });
        await viewDetailsAgain.click();

        // Summary should load again for the newly opened modal
        await expect(page.locator('text=/résumé des inscriptions|registration summary/i')).toBeVisible({ timeout: 10000 });

        // Verify summary content is present
        const confirmedLabel = page.locator('text=/inscriptions confirmées|confirmed registrations/i');
        await expect(confirmedLabel).toBeVisible();
    });
});
