import { test, expect } from '@playwright/test';

test.describe('Register Flow', () => {
    test('User can register a new account (frontend happy path)', async ({ page }) => {
        await page.goto('/auth/register');

        const randomEmail = `user${Date.now()}@test.com`;

        await page.getByPlaceholder(/email address/i).fill(randomEmail);
        await page.getByPlaceholder(/password/i).fill('Password123!');

        // Click the big "Sign Up" button (text locator, not role)
        await page.getByText(/sign up/i).click();

        // After a successful register the app sends user back to login
        await expect(page).toHaveURL(/\/auth\/login/);
    });

    test('Shows error when fields are empty', async ({ page }) => {
        await page.goto('/auth/register');

        // Click Sign Up with empty fields
        await page.getByText(/sign up/i).click();

        // Frontend validation message from your RegisterScreen: "Please fill in all fields."
        await expect(page.getByText(/please fill in all fields/i)).toBeVisible();
    });
});
