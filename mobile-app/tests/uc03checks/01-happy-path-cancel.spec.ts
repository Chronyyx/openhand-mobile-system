
import { test, expect } from '@playwright/test';
import {
    ACTIVE_MEMBER,
    buildEvent,
    setupScenarioRoutes,
    loginAs,
    openFirstEventFromHome,
    clearAuthState,
    type MockEvent,
    type MockRegistration,
} from './utils';

test.describe('UC03 Cancel Registration - Happy Path', () => {

    test('User can cancel an existing CONFIRMED registration', async ({ page }) => {
        test.setTimeout(90000);
        // 0. Ensure clean state
        await clearAuthState(page);

        // 1. Setup: User has a single confirmed registration
        const event = buildEvent({ id: 301, status: 'OPEN', currentRegistrations: 20 });
        const registration: MockRegistration = {
            id: 8001,
            userId: ACTIVE_MEMBER.id,
            eventId: event.id,
            eventTitle: event.title,
            status: 'CONFIRMED',
            requestedAt: new Date().toISOString(),
            confirmedAt: new Date().toISOString(),
            cancelledAt: null,
            waitlistedPosition: null,
            eventStartDateTime: event.startDateTime,
            eventEndDateTime: event.endDateTime,
        };

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [event],
            initialRegistrations: [registration],
            cancelHandler: (eventId) => {
                expect(eventId).toBe(event.id);
                const cancelledReg: MockRegistration = {
                    ...registration,
                    status: 'CANCELLED',
                    cancelledAt: new Date().toISOString()
                };
                return {
                    status: 200,
                    body: cancelledReg,
                    nextRegistrations: [cancelledReg],
                };
            }
        });

        // 2. Login
        await loginAs(page, ACTIVE_MEMBER);

        // 3. Go to event
        await openFirstEventFromHome(page);

        // 4. Verify Registration State
        const unregisterButton = page.getByRole('button', { name: /undo|unregister|annuler/i });
        await expect(unregisterButton).toBeVisible();

        // 5. Click "Unregister" / "Cancel"
        // Setup listener first
        page.once('dialog', async dialog => {
            console.log(`Dialog message: ${dialog.message()}`);
            await dialog.accept();
        });

        await unregisterButton.click();

        // 6. Handle Confirmation Modal (if any)
        const confirmButton = page.getByRole('button', { name: /confirm|yes|oui|si/i });
        if (await confirmButton.isVisible().catch(() => false)) {
            await confirmButton.click();
        }

        // 7. Verify Success State
        // Wait for state transition to "Register" button
        const registerButton = page.getByRole('button', { name: /register|s'inscrire|join/i });
        await expect(registerButton).toBeVisible();
    });

    test('User can register and then immediately cancel', async ({ page }) => {
        test.setTimeout(90000);
        // 0. Ensure clean state
        await clearAuthState(page);

        // 1. Setup: User starts with NO registration
        const event = buildEvent({ id: 302, status: 'OPEN', currentRegistrations: 20 });

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [event],
            initialRegistrations: [],
            registerHandler: (payload) => {
                const newReg: MockRegistration = {
                    id: 8002,
                    userId: ACTIVE_MEMBER.id,
                    eventId: event.id,
                    eventTitle: event.title,
                    status: 'CONFIRMED',
                    requestedAt: new Date().toISOString(),
                    confirmedAt: new Date().toISOString(),
                    cancelledAt: null,
                    waitlistedPosition: null,
                    eventStartDateTime: event.startDateTime,
                    eventEndDateTime: event.endDateTime,
                };
                return {
                    status: 201,
                    body: {
                        eventId: event.id,
                        primaryRegistrant: { registrationId: newReg.id, status: 'CONFIRMED' }
                    },
                    nextRegistrations: [newReg],
                };
            },
            cancelHandler: (eventId) => {
                const cancelledReg: MockRegistration = {
                    id: 8002,
                    userId: ACTIVE_MEMBER.id,
                    eventId: event.id,
                    eventTitle: event.title,
                    status: 'CANCELLED', // key change
                    requestedAt: new Date().toISOString(),
                    confirmedAt: new Date().toISOString(),
                    cancelledAt: new Date().toISOString(),
                    waitlistedPosition: null,
                    eventStartDateTime: event.startDateTime,
                    eventEndDateTime: event.endDateTime,
                };
                return {
                    status: 200,
                    body: cancelledReg,
                    nextRegistrations: [cancelledReg],
                };
            }
        });

        // 2. Login
        await loginAs(page, ACTIVE_MEMBER);

        // 3. Go to event
        await openFirstEventFromHome(page);

        // 4. Register
        await page.getByRole('button', { name: /register/i }).click();
        // Check for success view or direct transition?
        // UI often shows a success screen or updates the button.
        // Assuming success view might appear or just button changes.
        // For robustness, wait for 'Unregister'
        const unregisterButton = page.getByRole('button', { name: /undo|unregister|annuler/i });
        await expect(unregisterButton).toBeVisible();

        // 5. Cancel immediately
        page.once('dialog', async dialog => {
            console.log(`Dialog message: ${dialog.message()}`);
            await dialog.accept();
        });

        await unregisterButton.click();

        // 6. Handle Confirmation
        const confirmButton = page.getByRole('button', { name: /confirm|yes|oui|si/i });
        if (await confirmButton.isVisible().catch(() => false)) {
            await confirmButton.click();
        }

        // 7. Verify back to start
        const registerButton = page.getByRole('button', { name: /register|s'inscrire|join/i });
        await expect(registerButton).toBeVisible();
    });
});
