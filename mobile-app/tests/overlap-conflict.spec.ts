import { test, expect } from '@playwright/test';

test.describe('Overlapping Event Conflict Detection', () => {
  const user = {
    token: 'test-token',
    refreshToken: 'refresh-token',
    type: 'Bearer',
    id: 1,
    email: 'member@example.com',
    roles: ['ROLE_MEMBER'],
  };

  test('shows conflict badge when registrations overlap', async ({ page }) => {
    // Intercept my-registrations with overlapping events
    await page.route('**/api/registrations/my-registrations', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 1,
            userId: 1,
            eventId: 101,
            eventTitle: 'Event A',
            status: 'CONFIRMED',
            requestedAt: '2025-01-01T09:00:00',
            confirmedAt: '2025-01-01T09:05:00',
            cancelledAt: null,
            waitlistedPosition: null,
            eventStartDateTime: '2025-01-01T10:00:00',
            eventEndDateTime: '2025-01-01T11:00:00',
          },
          {
            id: 2,
            userId: 1,
            eventId: 102,
            eventTitle: 'Event B',
            status: 'CONFIRMED',
            requestedAt: '2025-01-01T09:30:00',
            confirmedAt: '2025-01-01T09:35:00',
            cancelledAt: null,
            waitlistedPosition: null,
            eventStartDateTime: '2025-01-01T10:30:00',
            eventEndDateTime: '2025-01-01T12:00:00',
          },
        ]),
      });
    });

    // Seed auth token before load
    await page.addInitScript((storedUser) => {
      localStorage.setItem('userToken', JSON.stringify(storedUser));
    }, user);

    await page.goto('/registrations', { waitUntil: 'domcontentloaded' });

    // Wait for conflict badge to appear
    const conflictBadge = page.getByText(/time conflict|conflit horaire|conflicto de horario/i);
    await expect(conflictBadge).toBeVisible({ timeout: 10000 });
  });
});
