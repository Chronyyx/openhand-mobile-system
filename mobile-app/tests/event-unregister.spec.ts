import { test, expect } from '@playwright/test';

test.describe('Event Registration Flow', () => {

    test('should allow a user to register and then unregister from an event', async ({ page }) => {
        // 1. Login (Recorded Steps)
        await page.goto('http://localhost:8081/');
        await page.getByText('Log In / Register').click();

        const emailInput = page.getByRole('textbox', { name: 'Email or Phone Number' });
        await emailInput.fill('admin@mana.org');
        await page.getByRole('textbox', { name: 'Password' }).fill('admin123');
        await page.getByText('Log In', { exact: true }).click();

        // 2. Navigate to Event (Recorded Steps)
        // Wait for list to load
        await page.getByText('Browse Events').nth(1).click();

        // click on second event as first might be full
        await page.getByRole('button', { name: /View details/ }).nth(1).click();

        // 3. Logic: Check Status and Toggle
        // We look for text indicating we can Register or Unregister/Undo

        // "Undo registration" comes from events.actions.undo (English)
        // "Se désinscrire" comes from the fallback in code for events.actions.unregister
        // "Register for this event" comes from events.actions.register

        const registerButtonProxy = page.getByText(/Register for this event/i);
        // Matching both "Undo registration" (Success modal) and "Se désinscrire" (Details modal fallback)
        const unregisterButtonProxy = page.getByText(/Undo registration|Se désinscrire/i);

        // Wait for modal to appear by checking for the action button directly
        // React Native Web Modals might not have role="dialog" easily accessible

        // Wait for one of them to be visible
        // Increasing timeout slightly to account for animations
        await expect(registerButtonProxy.or(unregisterButtonProxy).first()).toBeVisible({ timeout: 10000 });

        if (await unregisterButtonProxy.isVisible()) {
            console.log('User is already registered. Unregistering...');
            await unregisterButtonProxy.click();
            // Verify we are back to "Register" state
            await expect(registerButtonProxy).toBeVisible();
        } else {
            console.log('User is NOT registered. Registering...');
            await registerButtonProxy.click();

            // 4. Verify Success
            // Should see Success message OR "Undo registration"
            await expect(page.getByText(/Registration Confirmed/i)).toBeVisible();

            // Now Unregister (Undo)
            const undoButton = page.getByText('Undo registration');
            await undoButton.click();

            // Verify we are back to "Register" state
            await expect(registerButtonProxy).toBeVisible();
        }
    });

});
