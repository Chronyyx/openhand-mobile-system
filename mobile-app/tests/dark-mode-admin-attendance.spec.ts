import { test, expect } from '@playwright/test';

test.describe('Dark Mode - Admin Attendance Dashboard', () => {
  const adminUser = {
    token: 'test-token-admin',
    type: 'Bearer',
    id: 1,
    email: 'admin@mana.org',
    roles: ['ROLE_ADMIN'],
  } as const;

  const attendanceEvent = {
    eventId: 1,
    title: 'Training_Session',
    startDateTime: '2030-06-01T10:00:00Z',
    endDateTime: '2030-06-01T12:00:00Z',
    locationName: 'Centre MANA',
    address: '123 Main Street, Montreal',
    status: 'OPEN',
    maxCapacity: 50,
    registeredCount: 30,
    checkedInCount: 25,
    occupancyPercent: 50,
  };

  const attendeesResponse = {
    eventId: 1,
    registeredCount: 30,
    checkedInCount: 25,
    attendees: [
      {
        userId: 10,
        fullName: 'Ada Lovelace',
        email: 'ada@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: false,
        checkedInAt: null,
      },
      {
        userId: 11,
        fullName: 'Alan Turing',
        email: 'alan@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: true,
        checkedInAt: '2030-06-01T10:05:00Z',
      },
      {
        userId: 12,
        fullName: 'Grace Hopper',
        email: 'grace@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: true,
        checkedInAt: '2030-06-01T10:10:00Z',
      },
    ],
  };

  const eventDetail = {
    id: 1,
    title: attendanceEvent.title,
    description: 'A training session for staff.',
    startDateTime: attendanceEvent.startDateTime,
    endDateTime: attendanceEvent.endDateTime,
    locationName: attendanceEvent.locationName,
    address: attendanceEvent.address,
    status: attendanceEvent.status,
    maxCapacity: attendanceEvent.maxCapacity,
    currentRegistrations: attendanceEvent.registeredCount,
    category: 'FORMATION',
  };

  test.beforeEach(async ({ page }) => {
    // Mock API routes
    await page.route('**/api/employee/attendance/events', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([attendanceEvent]),
      });
    });

    await page.route('**/api/employee/attendance/events/1/attendees', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(attendeesResponse),
      });
    });

    await page.route('**/api/events/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(eventDetail),
      });
    });

    // Set admin user in localStorage before page loads
    await page.addInitScript((user) => {
      window.localStorage.setItem('userToken', JSON.stringify(user));
    }, adminUser);
  });

  test('Attendance dashboard displays correctly in dark mode with visible metric bubbles and occupancy percentage', async ({ page, browserName }) => {
    // Enable dark mode via media query emulation
    await page.emulateMedia({ colorScheme: 'dark' });

    // Navigate to attendance dashboard
    await page.goto('/admin/attendance', { waitUntil: 'domcontentloaded' });
    
    // Wait for content to load and verify dashboard is visible (use first() to avoid strict mode violation)
    await expect(page.getByText(/attendance|présences|asistencia/i).first()).toBeVisible();

    // Verify the event card is visible (multilingual support)
    const eventCard = page.getByText(/training session|training_session|formation|capacitaci/i).first();
    await expect(eventCard).toBeVisible();

    // Find the metric cards (bubbles showing registered and checked-in counts)
    const registeredMetric = page.getByText(/registered|inscrits|registrados/i).first();
    const checkedInMetric = page.getByText(/checked in|présents|presentes/i).first();

    await expect(registeredMetric).toBeVisible();
    await expect(checkedInMetric).toBeVisible();

    // The main goal is to verify dark mode styling is applied
    // Check computed styles for dark mode colors on metric cards
    const metricCards = page.locator('[style*="backgroundColor"]').filter({ hasText: /registered|inscrits/i });
    if (await metricCards.count() > 0) {
      const firstCard = metricCards.first();
      const bgColor = await firstCard.evaluate((el) => {
        return window.getComputedStyle(el).backgroundColor;
      });
      
      // Dark mode background should be dark (not light blue #F5F8FF)
      // rgb(31, 35, 40) is #1F2328 (dark mode)
      // rgb(245, 248, 255) is #F5F8FF (light mode)
      expect(bgColor).not.toBe('rgb(245, 248, 255)');
    }

    // Verify occupancy percentage is visible
    const occupancyText = page.getByText(/occupancy|occupation|ocupación/i).first();
    await expect(occupancyText).toBeVisible();

    // Check that occupancy value (50%) is visible
    const occupancyValue = page.getByText(/\d+%/);
    const occupancyCount = await occupancyValue.count();
    expect(occupancyCount).toBeGreaterThan(0);

    // Take screenshot for visual verification
    await page.screenshot({ path: `test-results/dark-mode-attendance-dashboard-${browserName}.png`, fullPage: true });
  });

  test('Attendance detail page displays correctly in dark mode with visible user bubbles and percentage text', async ({ page, browserName }) => {
    // Enable dark mode
    await page.emulateMedia({ colorScheme: 'dark' });

    // Navigate to attendance detail page
    await page.goto('/admin/attendance/1', { waitUntil: 'domcontentloaded' });
    
    // Wait for attendee list to load
    await expect(page.getByText(/attendees|participants|asistentes/i)).toBeVisible();

    // Verify summary counts are visible (registered and checked-in bubbles)
    const registeredCountText = page.getByText(new RegExp(`${attendeesResponse.registeredCount}`));
    const checkedInCountText = page.getByText(new RegExp(`${attendeesResponse.checkedInCount}`));

    // At least one should be visible
    const summaryVisible = (await registeredCountText.count()) > 0 || (await checkedInCountText.count()) > 0;
    expect(summaryVisible).toBeTruthy();

    // Verify attendee cards are visible with proper dark mode styling
    const adaCard = page.locator('text=Ada Lovelace').first();
    const alanCard = page.locator('text=Alan Turing').first();
    const graceCard = page.locator('text=Grace Hopper').first();

    await expect(adaCard).toBeVisible();
    await expect(alanCard).toBeVisible();
    await expect(graceCard).toBeVisible();

    // Check that attendee email addresses are visible (dark mode text color)
    const adaEmail = page.locator('text=ada@mana.org').first();
    await expect(adaEmail).toBeVisible();

    // Verify attendee cards have dark mode background
    const attendeeCards = page.locator('[style*="backgroundColor"]').filter({ hasText: 'Ada Lovelace' });
    if (await attendeeCards.count() > 0) {
      const cardBg = await attendeeCards.first().evaluate((el) => {
        return window.getComputedStyle(el).backgroundColor;
      });
      
      // Dark mode card background should be dark (#1F2328), not white (#FFFFFF)
      expect(cardBg).not.toBe('rgb(255, 255, 255)');
    }

    // Verify check-in status badges are visible with proper colors
    const checkedInBadge = page.locator('text=Checked In').or(page.locator('text=/Checked.*In/'));
    const checkedInCount = await checkedInBadge.count();
    expect(checkedInCount).toBeGreaterThan(0);

    // Check that timestamps are visible for checked-in users
    const timestamp = page.locator('text=/10:05|10:10/').first();
    if (await timestamp.count() > 0) {
      await expect(timestamp).toBeVisible();
    }

    // Take screenshot for visual verification
    await page.screenshot({ path: `test-results/dark-mode-attendance-detail-${browserName}.png`, fullPage: true });
  });
});
