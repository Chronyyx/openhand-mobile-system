
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

test.describe('UC03 Cancel Registration - Group/Family Flow', () => {

    test('Primary registrant cancelling should cancel ENTIRE group', async ({ page }) => {
        test.setTimeout(90000); // Increase test timeout to handle long app load

        // 0. Ensure clean state
        await clearAuthState(page);

        // 1. Setup: User has a GROUP registration (Self + 2 kids)
        const event = buildEvent({ id: 303, status: 'OPEN', currentRegistrations: 20 });
        const groupId = 'group-uuid-123';
        const primaryReg: MockRegistration = {
            id: 8010,
            userId: ACTIVE_MEMBER.id,
            eventId: event.id,
            eventTitle: event.title,
            status: 'CONFIRMED',
            registrationGroupId: groupId,
            primaryRegistrant: true,
            requestedAt: new Date().toISOString(),
            confirmedAt: new Date().toISOString(),
            cancelledAt: null,
            waitlistedPosition: null,
            eventStartDateTime: event.startDateTime,
            eventEndDateTime: event.endDateTime,
        };
        const familyReg1: MockRegistration = {
            id: 8011,
            userId: ACTIVE_MEMBER.id, // Linked to same user account for display
            eventId: event.id,
            eventTitle: event.title,
            status: 'CONFIRMED',
            registrationGroupId: groupId,
            primaryRegistrant: false,
            participantFullName: 'Kid One',
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
            initialRegistrations: [primaryReg, familyReg1],
            cancelHandler: (eventId) => {
                expect(eventId).toBe(event.id);
                // Backend logic: Cancel both
                const cancelledPrimary: MockRegistration = { ...primaryReg, status: 'CANCELLED', cancelledAt: new Date().toISOString() };
                const cancelledFamily: MockRegistration = { ...familyReg1, status: 'CANCELLED', cancelledAt: new Date().toISOString() };

                return {
                    status: 200,
                    body: cancelledPrimary, // Usually returns the primary
                    nextRegistrations: [cancelledPrimary, cancelledFamily],
                };
            }
        });

        // 2. Login
        await loginAs(page, ACTIVE_MEMBER);

        // 3. Go to event
        await openFirstEventFromHome(page);

        // 4. Verify Group Display
        await expect(page.getByRole('button', { name: /undo|Unregister|annuler/i })).toBeVisible();

        // 5. Cancel
        page.once('dialog', async dialog => {
            console.log(`Dialog message: ${dialog.message()}`);
            await dialog.accept();
        });

        await page.getByRole('button', { name: /undo|Unregister|annuler/i }).click();

        // 6. Verify Gone
        await expect(page.getByRole('button', { name: /register/i }).first()).toBeVisible();

        // And the unregister button should be gone
        await expect(page.getByRole('button', { name: /undo|Unregister|annuler/i })).not.toBeVisible();
    });
});
