
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

test.describe('UC03 Cancel Registration - Waitlist', () => {

    test('User can cancel a WAITLISTED registration', async ({ page }) => {
        test.setTimeout(90000);

        // 0. Ensure clean state
        await clearAuthState(page);

        // 1. Setup: User is on waitlist
        const event = buildEvent({ id: 305, status: 'FULL', currentRegistrations: 100, maxCapacity: 100 });
        const waitlistedRegistration: MockRegistration = {
            id: 8030,
            userId: ACTIVE_MEMBER.id,
            eventId: event.id,
            eventTitle: event.title,
            status: 'WAITLISTED',
            waitlistedPosition: 5,
            requestedAt: new Date().toISOString(),
            confirmedAt: null,
            cancelledAt: null,
            eventStartDateTime: event.startDateTime,
            eventEndDateTime: event.endDateTime,
        };

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [event],
            initialRegistrations: [waitlistedRegistration],
            cancelHandler: (eventId) => {
                expect(eventId).toBe(event.id);
                // Return cancelled state
                const cancelledReg: MockRegistration = { // Ensure explicit type
                    ...waitlistedRegistration,
                    status: 'CANCELLED',
                    cancelledAt: new Date().toISOString(),
                    waitlistedPosition: null
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

        // 3. Go to Event
        // Navigate directly to ensure we land on the correct event regardless of list sorting/filtering
        await page.goto(`/events/${event.id}`);

        // 4. Verify Waitlist State
        // UI currently shows "Unregister" even for waitlisted users (impl detail)
        // Check for the "Unregister" button to confirm we are in a registered state
        await expect(page.getByRole('button', { name: /unregister|undo|annuler/i })).toBeVisible({ timeout: 10000 });

        // 5. Cancel
        // Setup dialog handler BEFORE clicking
        page.once('dialog', async dialog => {
            console.log(`Dialog message: ${dialog.message()}`);
            await dialog.accept();
        });

        const unregisterButton = page.getByRole('button', { name: /unregister|undo|annuler/i });
        await unregisterButton.click();

        // 6. Verify Success
        // Wait for removal of unregister button and appearance of "Join Waitlist"
        const joinWaitlistButton = page.getByRole('button', { name: /join waitlist|rejoindre|waiting list/i });
        await expect(joinWaitlistButton).toBeVisible();
        await expect(page.getByRole('button', { name: /leave waitlist|quitter/i })).not.toBeVisible();
    });
});
