import { test, expect, type Page } from '@playwright/test';

test.describe('Attendance Attendees Filters', () => {
  const adminUser = {
    token: 'test-token-admin',
    type: 'Bearer',
    id: 1,
    email: 'admin@mana.org',
    roles: ['ROLE_ADMIN'],
  } as const;

  const eventDetail = {
    id: 1,
    title: 'Training_Session',
    description: 'A training session for staff.',
    startDateTime: '2030-06-01T10:00:00Z',
    endDateTime: '2030-06-01T12:00:00Z',
    locationName: 'Centre MANA',
    address: '123 Main Street, Montreal',
    status: 'OPEN',
    maxCapacity: 50,
    currentRegistrations: 12,
    category: 'FORMATION',
  };

  const attendeesResponse = {
    eventId: 1,
    registeredCount: 12,
    checkedInCount: 4,
    attendees: [
      {
        userId: 10,
        fullName: 'Zoe Zebra',
        email: 'alpha@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: false,
        checkedInAt: null,
      },
      {
        userId: 11,
        fullName: 'Amy Atlas',
        email: 'zulu@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: true,
        checkedInAt: '2030-06-01T10:05:00Z',
      },
      {
        userId: 12,
        fullName: 'Liam Lime',
        email: 'bravo@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: false,
        checkedInAt: null,
      },
      {
        userId: 13,
        fullName: 'Bella Blue',
        email: 'echo@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: true,
        checkedInAt: '2030-06-01T10:15:00Z',
      },
    ],
  };

  const nameOrder = async (page: Page) => {
    const names = await page.locator('[data-testid="attendance-attendee-name"]').allTextContents();
    return names.map((name) => name.trim());
  };

  test.beforeEach(async ({ page }) => {
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

    await page.addInitScript((user) => {
      window.localStorage.setItem('userToken', JSON.stringify(user));
    }, adminUser);
  });

  test('Admin can sort attendees by name or email', async ({ page }) => {
    await page.goto('/admin/attendance/1', { waitUntil: 'domcontentloaded' });

    await expect(page.getByTestId('attendance-attendee-name')).toHaveCount(4);

    await expect(await nameOrder(page)).toEqual([
      'Amy Atlas',
      'Bella Blue',
      'Liam Lime',
      'Zoe Zebra',
    ]);

    const filterButton = page.getByTestId('attendance-attendees-filter-button');

    const applySort = async (key: string, expectedOrder: string[]) => {
      await filterButton.click();
      await page.getByTestId(`attendance-attendees-filter-option-${key}`).click();
      await expect(page.getByTestId('attendance-attendee-name').first()).toHaveText(expectedOrder[0]);
      await expect(await nameOrder(page)).toEqual(expectedOrder);
    };

    await applySort('nameDesc', ['Zoe Zebra', 'Liam Lime', 'Bella Blue', 'Amy Atlas']);
    await applySort('emailAsc', ['Zoe Zebra', 'Liam Lime', 'Bella Blue', 'Amy Atlas']);
    await applySort('emailDesc', ['Amy Atlas', 'Bella Blue', 'Liam Lime', 'Zoe Zebra']);
    await applySort('nameAsc', ['Amy Atlas', 'Bella Blue', 'Liam Lime', 'Zoe Zebra']);
  });
});
