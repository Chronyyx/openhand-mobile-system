import { test, expect } from '@playwright/test';

test.describe('Member Status Visibility (Admin / Employee)', () => {
  const adminUser = {
    token: 'test-token-admin',
    type: 'Bearer',
    id: 1,
    email: 'admin@mana.org',
    roles: ['ROLE_ADMIN'],
  } as const;

  const employeeUser = {
    token: 'test-token-employee',
    type: 'Bearer',
    id: 2,
    email: 'employee@mana.org',
    roles: ['ROLE_EMPLOYEE'],
  } as const;

  const mockEvent = {
    id: 123,
    title: 'Member Status Test Event',
    description: 'Event to test member status visibility',
    startDateTime: '2026-02-15T19:00:00Z',
    endDateTime: '2026-02-15T21:00:00Z',
    locationName: 'Test Venue',
    address: '456 Test Street',
    status: 'OPEN',
    maxCapacity: 25,
    currentRegistrations: 8,
    category: 'GENERAL',
  };

  const mockRegistrationSummaryWithMixedStatuses = {
    eventId: mockEvent.id,
    totalRegistrations: 8,
    waitlistedCount: 2,
    maxCapacity: mockEvent.maxCapacity,
    remainingSpots: 17,
    percentageFull: 32.0,
    attendees: [
      {
        userId: 101,
        userName: 'Active Member One',
        userEmail: 'active1@test.com',
        registrationStatus: 'CONFIRMED',
        memberStatus: 'ACTIVE',
        waitlistedPosition: null,
        requestedAt: '2026-01-10T10:00:00',
        confirmedAt: '2026-01-10T10:00:00',
      },
      {
        userId: 102,
        userName: 'Inactive Member One',
        userEmail: 'inactive1@test.com',
        registrationStatus: 'CONFIRMED',
        memberStatus: 'INACTIVE',
        waitlistedPosition: null,
        requestedAt: '2026-01-11T11:00:00',
        confirmedAt: '2026-01-11T11:00:00',
      },
      {
        userId: 103,
        userName: 'Active Member Two',
        userEmail: 'active2@test.com',
        registrationStatus: 'CONFIRMED',
        memberStatus: 'ACTIVE',
        waitlistedPosition: null,
        requestedAt: '2026-01-12T12:00:00',
        confirmedAt: '2026-01-12T12:00:00',
      },
      {
        userId: 104,
        userName: 'Waitlisted Active Member',
        userEmail: 'waitlisted-active@test.com',
        registrationStatus: 'WAITLISTED',
        memberStatus: 'ACTIVE',
        waitlistedPosition: 1,
        requestedAt: '2026-01-13T13:00:00',
        confirmedAt: null,
      },
      {
        userId: 105,
        userName: 'Waitlisted Inactive Member',
        userEmail: 'waitlisted-inactive@test.com',
        registrationStatus: 'WAITLISTED',
        memberStatus: 'INACTIVE',
        waitlistedPosition: 2,
        requestedAt: '2026-01-14T14:00:00',
        confirmedAt: null,
      },
    ],
  };

  test.beforeEach(async ({ page }) => {
    // Mock event endpoints
    await page.route('**/api/events/upcoming', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([mockEvent]),
      });
    });

    await page.route('**/api/events/123', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockEvent),
      });
    });

    await page.route('**/api/events/123/registration-summary', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockRegistrationSummaryWithMixedStatuses),
      });
    });

    await page.route('**/api/registrations/my-registrations', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });
  });

  test('Admin can view member status badges for all attendees with mixed active and inactive members', async ({ page }) => {
    // Arrange - Login as admin
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.evaluate((user) => {
      window.localStorage.setItem('userToken', JSON.stringify(user));
    }, adminUser);

    // Act - Navigate to events and open event modal
    await page.goto('/events', { waitUntil: 'domcontentloaded' });
    const viewDetails = page.getByText(/view details|voir détails|details/i).first();
    await expect(viewDetails).toBeVisible({ timeout: 15000 });
    await viewDetails.click();

    // Wait for modal and registration summary
    await expect(page.getByText(/description/i).first()).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=/résumé des inscriptions|registration summary/i').first()).toBeVisible({ timeout: 10000 });

    // Assert - Verify attendees list is visible
    const attendeesSection = page.locator('text=/attendees|participants/i').first();
    await expect(attendeesSection).toBeVisible({ timeout: 10000 });

    // Assert - Verify all 5 attendees are displayed
    await expect(page.getByText('Active Member One').first()).toBeVisible();
    await expect(page.getByText('Inactive Member One').first()).toBeVisible();
    await expect(page.getByText('Active Member Two').first()).toBeVisible();
    await expect(page.getByText('Waitlisted Active Member').first()).toBeVisible();
    await expect(page.getByText('Waitlisted Inactive Member').first()).toBeVisible();

    // Assert - Verify active member status badges are present (case-insensitive for translations)
    const activeStatusBadges = page.locator('text=/●.*active|actif/i');
    const activeCount = await activeStatusBadges.count();
    expect(activeCount).toBeGreaterThanOrEqual(3); // 3 active members

    // Assert - Verify inactive member status badges are present (case-insensitive for translations)
    const inactiveStatusBadges = page.locator('text=/○.*inactive|inactif/i');
    const inactiveCount = await inactiveStatusBadges.count();
    expect(inactiveCount).toBeGreaterThanOrEqual(2); // 2 inactive members

    // Assert - Verify registration status (CONFIRMED/WAITLISTED) is also shown
    await expect(page.locator('text=/confirmed|confirmé/i').first()).toBeVisible();
    await expect(page.locator('text=/waitlisted|en attente/i').first()).toBeVisible();

    // Assert - Verify waitlist positions are shown for waitlisted attendees
    await expect(page.getByText(/#1/)).toBeVisible(); // Waitlist position #1
    await expect(page.getByText(/#2/)).toBeVisible(); // Waitlist position #2
  });

  test('Employee can view member status for attendees and distinguish between active and inactive members', async ({ page }) => {
    // Arrange - Login as employee
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.evaluate((user) => {
      window.localStorage.setItem('userToken', JSON.stringify(user));
    }, employeeUser);

    // Act - Navigate to events and open event modal
    await page.goto('/events', { waitUntil: 'domcontentloaded' });
    const viewDetails = page.getByText(/view details|voir détails|details/i).first();
    await expect(viewDetails).toBeVisible({ timeout: 15000 });
    await viewDetails.click();

    // Wait for modal and attendees list
    await expect(page.getByText(/description/i).first()).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=/attendees|participants/i').first()).toBeVisible({ timeout: 10000 });

    // Assert - Verify attendee names are visible
    await expect(page.getByText('Active Member One').first()).toBeVisible();
    await expect(page.getByText('active1@test.com').first()).toBeVisible();
    await expect(page.getByText('Inactive Member One').first()).toBeVisible();
    await expect(page.getByText('inactive1@test.com').first()).toBeVisible();

    // Assert - Verify member status is displayed for each attendee
    // Check for ACTIVE status indicators (● symbol for active)
    const filledCircles = page.locator('text=/●/');
    const filledCircleCount = await filledCircles.count();
    expect(filledCircleCount).toBeGreaterThanOrEqual(3); // At least 3 active members

    // Check for INACTIVE status indicators (○ symbol for inactive)
    const hollowCircles = page.locator('text=/○/');
    const hollowCircleCount = await hollowCircles.count();
    expect(hollowCircleCount).toBeGreaterThanOrEqual(2); // At least 2 inactive members

    // Assert - Verify both confirmed and waitlisted members show member status
    // Look for confirmed attendee with active status
    const activeConfirmedSection = page.locator('div', { 
      has: page.getByText('Active Member One')
    });
    await expect(activeConfirmedSection.locator('text=/●.*active|actif/i').first()).toBeVisible();
    await expect(activeConfirmedSection.locator('text=/confirmed|confirmé/i').first()).toBeVisible();

    // Look for waitlisted attendee with inactive status
    const inactiveWaitlistedSection = page.locator('div', { 
      has: page.getByText('Waitlisted Inactive Member')
    });
    await expect(inactiveWaitlistedSection.locator('text=/○.*inactive|inactif/i').first()).toBeVisible();
    await expect(inactiveWaitlistedSection.locator('text=/waitlisted|en attente/i').first()).toBeVisible();
  });

  test('Registration summary displays member status alongside registration status for comprehensive attendee view', async ({ page }) => {
    // Arrange - Login as admin
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.evaluate((user) => {
      window.localStorage.setItem('userToken', JSON.stringify(user));
    }, adminUser);

    // Act - Navigate to event with attendees
    await page.goto('/events', { waitUntil: 'domcontentloaded' });
    const viewDetails = page.getByText(/view details|voir détails|details/i).first();
    await expect(viewDetails).toBeVisible({ timeout: 15000 });
    await viewDetails.click();

    // Wait for complete registration summary to load
    await expect(page.getByText(/description/i).first()).toBeVisible({ timeout: 10000 });
    const summaryTitle = page.locator('text=/résumé des inscriptions|registration summary/i').first();
    await expect(summaryTitle).toBeVisible({ timeout: 10000 });

    // Assert - Verify summary statistics are present
    await expect(page.locator('text=/inscriptions confirmées|confirmed registrations/i').first()).toBeVisible();
    
    // Look for the total registrations value within the registration summary section
    const summarySection = page.locator('text=/registration summary|résumé des inscriptions/i').first();
    await expect(summarySection).toBeVisible();

    // Assert - Verify attendee list section is visible
    await expect(page.locator('text=/attendees|participants/i').first()).toBeVisible({ timeout: 10000 });

    // Assert - For each attendee, verify both member status and registration status are visible
    const attendeeItems = page.locator('[style*="attendeeItem"]').or(
      page.locator('div').filter({ 
        hasText: /Active Member One|Inactive Member One|Active Member Two|Waitlisted Active Member|Waitlisted Inactive Member/ 
      })
    );
    const attendeeCount = await attendeeItems.count();
    expect(attendeeCount).toBeGreaterThanOrEqual(5);

    // Assert - Verify user information is displayed (name and email)
    await expect(page.getByText('active1@test.com').first()).toBeVisible();
    await expect(page.getByText('inactive1@test.com').first()).toBeVisible();
    await expect(page.getByText('active2@test.com').first()).toBeVisible();
    await expect(page.getByText('waitlisted-active@test.com').first()).toBeVisible();
    await expect(page.getByText('waitlisted-inactive@test.com').first()).toBeVisible();

    // Assert - Verify the combination of statuses:
    // 1. Active + Confirmed
    const activeConfirmed = page.locator('div', { has: page.getByText('Active Member One') });
    await expect(activeConfirmed.locator('text=/●/').first()).toBeVisible(); // Active indicator
    await expect(activeConfirmed.locator('text=/confirmed|confirmé/i').first()).toBeVisible();

    // 2. Inactive + Confirmed
    const inactiveConfirmed = page.locator('div', { has: page.getByText('Inactive Member One') });
    await expect(inactiveConfirmed.locator('text=/○/').first()).toBeVisible(); // Inactive indicator
    await expect(inactiveConfirmed.locator('text=/confirmed|confirmé/i').first()).toBeVisible();

    // 3. Active + Waitlisted
    const activeWaitlisted = page.locator('div', { has: page.getByText('Waitlisted Active Member') });
    await expect(activeWaitlisted.locator('text=/●/').first()).toBeVisible(); // Active indicator
    await expect(activeWaitlisted.locator('text=/waitlisted|en attente/i').first()).toBeVisible();
    await expect(activeWaitlisted.getByText(/#1/).first()).toBeVisible(); // Waitlist position

    // 4. Inactive + Waitlisted
    const inactiveWaitlisted = page.locator('div', { has: page.getByText('Waitlisted Inactive Member') });
    await expect(inactiveWaitlisted.locator('text=/○/').first()).toBeVisible(); // Inactive indicator
    await expect(inactiveWaitlisted.locator('text=/waitlisted|en attente/i').first()).toBeVisible();
    await expect(inactiveWaitlisted.getByText(/#2/).first()).toBeVisible(); // Waitlist position

    // Assert - Verify both active and inactive members are shown (no filtering)
    const allMemberStatuses = page.locator('text=/●.*active|○.*inactive/i');
    const statusCount = await allMemberStatuses.count();
    expect(statusCount).toBeGreaterThanOrEqual(5); // All 5 attendees should have visible member status
  });
});
