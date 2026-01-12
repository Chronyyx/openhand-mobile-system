import { test, expect } from '@playwright/test';

test.describe('Employee Walk-In Registration', () => {
  const employeeUser = {
    token: 'test-token-employee',
    type: 'Bearer',
    id: 2,
    email: 'employee@mana.org',
    roles: ['ROLE_EMPLOYEE'],
  } as const;

  const mockEvent = {
    id: 1,
    title: 'Walk-in Friendly Event',
    description: 'Community gathering with on-site registration.',
    startDateTime: '2026-02-01T18:00:00Z',
    endDateTime: '2026-02-01T20:00:00Z',
    locationName: 'Centre MANA',
    address: '123 Main Street, Montreal',
    status: 'OPEN',
    maxCapacity: 30,
    currentRegistrations: 5,
    category: 'GALA',
  };

  test.beforeEach(async ({ page }) => {
    // Mock core event endpoints
    await page.route('**/api/events/upcoming', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([mockEvent]),
      });
    });

    await page.route('**/api/events/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockEvent),
      });
    });

    await page.route('**/api/events/1/registration-summary', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          eventId: mockEvent.id,
          totalRegistrations: 5,
          waitlistedCount: 0,
          maxCapacity: mockEvent.maxCapacity,
          remainingSpots: (mockEvent.maxCapacity ?? 0) - (mockEvent.currentRegistrations ?? 0),
          percentageFull: 17,
        }),
      });
    });

    // User registrations for the signed-in employee (not relevant but requested by UI)
    await page.route('**/api/registrations/my-registrations', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    // Employee user search endpoint – echo back the query as the found email
    await page.route('**/api/employee/users/search**', async (route) => {
      const url = new URL(route.request().url());
      const query = url.searchParams.get('query') || 'walkin@test.com';
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { id: 42, email: query, roles: ['ROLE_MEMBER'] },
        ]),
      });
    });

    // Seed auth token before navigating
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.evaluate((user) => {
      window.localStorage.setItem('userToken', JSON.stringify(user));
    }, employeeUser);
  });

  test('Employee can search and register a walk-in participant', async ({ page }) => {
    // Mock successful employee registration
    await page.route('**/api/employee/registrations', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 999,
          eventId: mockEvent.id,
          userId: 42,
          status: 'CONFIRMED',
        }),
      });
    });

    await page.goto('/events', { waitUntil: 'domcontentloaded' });

    // Open event modal
    const viewDetails = page.getByText(/view details|voir détails|details/i).first();
    await expect(viewDetails).toBeVisible({ timeout: 15000 });
    await viewDetails.click();

    // Ensure walk-in panel is visible
    await expect(page.getByText(/walk-in registration|inscription sur place|registro presencial/i)).toBeVisible();

    // Search for participant
    const searchInput = page.getByPlaceholder(/email|courriel|correo|phone|téléphone/i).first();
    await searchInput.fill('walkin@test.com');
    await page.getByText(/search|buscar|rechercher/i).first().click();

    // Result and register action
    await expect(page.getByText('walkin@test.com')).toBeVisible();
    await page.getByText(/register participant|enregistrer le participant|registrar participante/i).first().click();

    // Success message from employee registration
    await expect(
      page.getByText(/participant confirmed|confirmado para el evento|confirmé à l'événement/i)
    ).toBeVisible({ timeout: 5000 });
  });

  test('Shows a friendly message when participant is already registered', async ({ page }) => {
    // Force backend to respond with an already-registered error
    await page.route('**/api/employee/registrations', async (route) => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'User already registered' }),
      });
    });

    await page.goto('/events', { waitUntil: 'domcontentloaded' });

    // Open event modal
    const viewDetails = page.getByText(/view details|voir détails|details/i).first();
    await expect(viewDetails).toBeVisible({ timeout: 15000 });
    await viewDetails.click();

    await expect(page.getByText(/walk-in registration|inscription sur place|registro presencial/i)).toBeVisible();

    // Search and try to register
    const searchInput = page.getByPlaceholder(/email|courriel|correo|phone|téléphone/i).first();
    await searchInput.fill('already@member.com');
    await page.getByText(/search|buscar|rechercher/i).first().click();

    await expect(page.getByText('already@member.com')).toBeVisible();
    await page.getByText(/register participant|enregistrer le participant|registrar participante/i).first().click();

    // Error should be localized and include the participant email
    await expect(
      page.getByText(/already registered|ya está registrado|déjà inscrit/i)
    ).toContainText('already@member.com');
  });
});
