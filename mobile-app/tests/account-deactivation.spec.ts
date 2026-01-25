import { test, expect } from '@playwright/test';

test.describe('Account Deactivation Flow', () => {
    // Helper function to set up authenticated user
    const setupAuthenticatedUser = async (page: any, role: string = 'ROLE_MEMBER') => {
        await page.route('**/auth/login', async (route: any) => {
            if (route.request().method() === 'POST') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        token: 'test-token',
                        refreshToken: 'refresh-token',
                        type: 'Bearer',
                        id: 100,
                        email: 'member@test.com',
                        roles: [role],
                        name: 'Test Member',
                        phoneNumber: '555-1234',
                        gender: 'MALE',
                        age: 30,
                    }),
                });
            } else {
                await route.continue();
            }
        });

        await page.goto('/auth/login', { waitUntil: 'domcontentloaded' });

        // Set token in localStorage to simulate authenticated session
        await page.evaluate(() => {
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
            };
            localStorage.setItem('userToken', JSON.stringify(user));
        });
    };

    test('Complete deactivation flow - successful deactivation redirects to home and logs out user', async ({ page }) => {
        await setupAuthenticatedUser(page);

        // Mock profile picture endpoint
        await page.route('**/account/profile-picture', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ url: null }),
            });
        });

        // Mock successful deactivation
        let deactivationCalled = false;
        await page.route('**/auth/deactivate', async (route) => {
            if (route.request().method() === 'POST') {
                deactivationCalled = true;
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        id: 100,
                        email: 'member@test.com',
                        memberStatus: 'INACTIVE',
                    }),
                });
            } else {
                await route.continue();
            }
        });

        // Navigate to profile page
        await page.goto('/profile', { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);

        // Find and click the "Deactivate account" link
        const deactivateLink = page.getByText(/deactivate account/i);
        await expect(deactivateLink).toBeVisible({ timeout: 5000 });
        await deactivateLink.click();

        // Should navigate to deactivate page
        await page.waitForURL(/\/settings\/deactivate/, { timeout: 5000 });

        // Verify warning message is displayed
        await expect(page.getByText(/this action is permanent/i)).toBeVisible();
        await expect(page.getByText(/your account will be deactivated immediately/i)).toBeVisible();

        // Verify confirmation input is present
        const confirmationInput = page.getByPlaceholder(/deactivate/i);
        await expect(confirmationInput).toBeVisible();

        // Try to submit without typing confirmation - should show error
        const deactivateButton = page.getByText(/deactivate my account/i);
        await deactivateButton.click();
        await page.waitForTimeout(500);
        await expect(page.getByText(/please type.*deactivate.*to continue/i)).toBeVisible();

        // Type correct confirmation text
        await confirmationInput.fill('deactivate');

        // Click deactivate button
        await deactivateButton.click();

        // Wait for deactivation to complete and redirect to home
        await page.waitForURL('/', { timeout: 10000 }).catch(() => {
            console.log('Did not redirect to home, current URL:', page.url());
        });

        // Verify deactivation API was called
        expect(deactivationCalled).toBeTruthy();

        // Verify user is logged out (token should be removed from localStorage)
        const tokenAfterDeactivation = await page.evaluate(() => {
            return localStorage.getItem('userToken');
        });
        expect(tokenAfterDeactivation).toBeNull();

        // Verify alert was shown (we can't interact with native alerts in web mode, but we can check the page state)
        // User should be on home page without authentication
        const currentUrl = page.url();
        expect(currentUrl).toMatch(/\/$|\/index/);
    });

    test('Deactivation validation - prevents deactivation with incorrect confirmation text', async ({ page }) => {
        await setupAuthenticatedUser(page);

        await page.route('**/account/profile-picture', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ url: null }),
            });
        });

        // Navigate directly to deactivate page
        await page.goto('/settings/deactivate', { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);

        // Verify page title and warning
        await expect(page.getByText(/deactivate account/i).first()).toBeVisible();
        await expect(page.getByText(/this action is permanent/i)).toBeVisible();

        const confirmationInput = page.getByPlaceholder(/deactivate/i);
        const deactivateButton = page.getByText(/deactivate my account/i);

        // Test 1: Empty input
        await deactivateButton.click();
        await page.waitForTimeout(500);
        await expect(page.getByText(/please type.*deactivate.*to continue/i)).toBeVisible();
        
        // Test 2: Wrong text (case matters in some implementations)
        await confirmationInput.fill('DEACTIVATE');
        await deactivateButton.click();
        await page.waitForTimeout(500);
        // Should still be on the same page, not redirected
        expect(page.url()).toContain('/settings/deactivate');

        // Test 3: Partial text
        await confirmationInput.clear();
        await confirmationInput.fill('deact');
        await deactivateButton.click();
        await page.waitForTimeout(500);
        await expect(page.getByText(/please type.*deactivate.*to continue/i)).toBeVisible();

        // Test 4: Extra spaces
        await confirmationInput.clear();
        await confirmationInput.fill('  deactivate  ');
        await deactivateButton.click();
        // This should work because the code trims the input
        await page.waitForTimeout(1000);
        
        // Verify cancel button works
        const cancelButton = page.getByText(/cancel/i);
        await expect(cancelButton).toBeVisible();
        await cancelButton.click();
        
        // Should navigate back
        await page.waitForTimeout(500);
        const urlAfterCancel = page.url();
        expect(urlAfterCancel).not.toContain('/settings/deactivate');
    });

    test('Deactivation error handling - displays error when API fails', async ({ page }) => {
        await setupAuthenticatedUser(page);

        await page.route('**/account/profile-picture', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ url: null }),
            });
        });

        // Mock failed deactivation - API error
        await page.route('**/auth/deactivate', async (route) => {
            if (route.request().method() === 'POST') {
                await route.fulfill({
                    status: 500,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        message: 'Unable to deactivate account at this time',
                    }),
                });
            } else {
                await route.continue();
            }
        });

        // Navigate to deactivate page
        await page.goto('/settings/deactivate', { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);

        // Fill in correct confirmation
        const confirmationInput = page.getByPlaceholder(/deactivate/i);
        await confirmationInput.fill('deactivate');

        // Click deactivate button
        const deactivateButton = page.getByText(/deactivate my account/i);
        await deactivateButton.click();

        // Wait for error message to appear
        await page.waitForTimeout(1500);

        // Verify error message is displayed
        const errorMessage = page.getByText(/unable to deactivate/i);
        await expect(errorMessage).toBeVisible({ timeout: 5000 });

        // Verify user is still on deactivate page (not redirected)
        expect(page.url()).toContain('/settings/deactivate');

        // Verify user is still logged in (token still in localStorage)
        const tokenAfterError = await page.evaluate(() => {
            return localStorage.getItem('userToken');
        });
        expect(tokenAfterError).not.toBeNull();

        // Test fallback endpoints: if /auth/deactivate fails with 404, try /account/deactivate
        await page.route('**/auth/deactivate', async (route) => {
            if (route.request().method() === 'POST') {
                await route.fulfill({
                    status: 404,
                    contentType: 'application/json',
                    body: JSON.stringify({ message: 'Not found' }),
                });
            } else {
                await route.continue();
            }
        });

        let accountDeactivateCalled = false;
        await page.route('**/account/deactivate', async (route) => {
            if (route.request().method() === 'POST') {
                accountDeactivateCalled = true;
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        id: 100,
                        email: 'member@test.com',
                        memberStatus: 'INACTIVE',
                    }),
                });
            } else {
                await route.continue();
            }
        });

        // Clear the confirmation input and retry
        await confirmationInput.clear();
        await confirmationInput.fill('deactivate');
        await deactivateButton.click();

        // Wait for redirect
        await page.waitForTimeout(2000);

        // Verify fallback endpoint was called
        expect(accountDeactivateCalled).toBeTruthy();
    });
});
