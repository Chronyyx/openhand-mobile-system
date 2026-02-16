
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

test.describe('UC03 Cancel Registration - Exception Paths', () => {

    test('User CANNOT cancel registration for a COMPLETED event', async ({ page }) => {
        test.setTimeout(90000);

        // 0. Ensure clean state
        await clearAuthState(page);

        // 1. Setup: Registration for a past/completed event
        const completedEvent = buildEvent({
            id: 304,
            status: 'COMPLETED',
            endDateTime: '2020-01-01T20:00:00', // Definitely in the past
            title: 'Past Gala'
        });

        const pastRegistration: MockRegistration = {
            id: 8020,
            userId: ACTIVE_MEMBER.id,
            eventId: completedEvent.id,
            eventTitle: completedEvent.title,
            status: 'CONFIRMED',
            requestedAt: '2019-12-01T12:00:00',
            confirmedAt: '2019-12-01T12:00:00',
            cancelledAt: null,
            waitlistedPosition: null,
            eventStartDateTime: completedEvent.startDateTime,
            eventEndDateTime: completedEvent.endDateTime,
        };

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [completedEvent],
            initialRegistrations: [pastRegistration],
            // No cancelHandler logic needed because UI should block
        });

        // 2. Login
        await loginAs(page, ACTIVE_MEMBER);

        // 3. Go to event
        // Navigate directly to the event details page since completed events are not listed in "Upcoming"
        await page.goto(`/events/${completedEvent.id}`);

        // 4. Verify Cancellation Block
        // Note: The app filters out completed events from the main list, so the modal might not even open.
        // Therefore, we simply verify that the Unregister button is NOT accessible.

        // Expect NO "Unregister" button
        const unregisterButton = page.getByRole('button', { name: /undo|unregister|annuler/i });
        await expect(unregisterButton).toHaveCount(0);

    });
});
