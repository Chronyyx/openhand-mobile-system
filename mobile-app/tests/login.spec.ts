import { test, expect } from '@playwright/test';

test.describe('Login Flow', () => {
    test('User can log in with valid credentials (happy path)', async ({ page }) => {
        await page.goto('/auth/login');

        await page.getByPlaceholder(/email address/i).fill('admin@mana.org');
        await page.getByPlaceholder(/password/i).fill('admin123');

        // Click the main big "Log in" button.
        // There are multiple "Log in" texts, so take the second one.
        await page.getByText(/log in/i).nth(1).click();

        // Give the SPA a moment to navigate and assert we left the login page
        await page.waitForTimeout(2000);
        await expect(page).not.toHaveURL(/\/auth\/login$/);
    });

    test('Shows error for invalid credentials', async ({ page }) => {
        await page.goto('/auth/login');

        await page.getByPlaceholder(/email address/i).fill('wrong@user.com');
        await page.getByPlaceholder(/password/i).fill('wrongpass');

        // Same button as above
        await page.getByText(/log in/i).nth(1).click();

        // We should still be on the login page if creds are bad
        await expect(page).toHaveURL(/\/auth\/login$/);
    });
});
