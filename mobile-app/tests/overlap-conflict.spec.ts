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
    await page.route('**/api/registrations/me**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            registrationId: 1,
            status: 'CONFIRMED',
            createdAt: '2025-01-01T09:00:00',
            timeCategory: 'ACTIVE',
            event: {
              eventId: 101,
              title: 'Event A',
              startDateTime: '2025-01-01T10:00:00',
              endDateTime: '2025-01-01T11:00:00',
              location: 'Main Hall',
            },
          },
          {
            registrationId: 2,
            status: 'CONFIRMED',
            createdAt: '2025-01-01T09:30:00',
            timeCategory: 'ACTIVE',
            event: {
              eventId: 102,
              title: 'Event B',
              startDateTime: '2025-01-01T10:30:00',
              endDateTime: '2025-01-01T12:00:00',
              location: 'Main Hall',
            },
          },
        ]),
      });
    });

    // Seed auth token before load
    await page.addInitScript((storedUser) => {
      localStorage.setItem('userToken', JSON.stringify(storedUser));
    }, user);

    await page.goto('/registrations', { waitUntil: 'domcontentloaded' });

    // Wait for at least one conflict badge to appear
    const conflictBadges = page.getByText(/time conflict|conflit horaire|conflicto de horario/i);
    await expect(conflictBadges.first()).toBeVisible({ timeout: 10000 });
  });
});
