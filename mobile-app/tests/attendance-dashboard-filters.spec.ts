import { test, expect, type Page } from '@playwright/test';

test.describe('Attendance Dashboard Filters', () => {
  const adminUser = {
    token: 'test-token-admin',
    type: 'Bearer',
    id: 1,
    email: 'admin@mana.org',
    roles: ['ROLE_ADMIN'],
  } as const;

  const attendanceEvents = [
    {
      eventId: 1,
      title: 'Zulu_Event',
      startDateTime: '2030-05-01T10:00:00Z',
      endDateTime: '2030-05-01T12:00:00Z',
      locationName: 'Centre MANA',
      address: '100 Main Street, Montreal',
      status: 'OPEN',
      maxCapacity: 100,
      registeredCount: 80,
      checkedInCount: 40,
      occupancyPercent: 40,
    },
    {
      eventId: 2,
      title: 'Alpha_Event',
      startDateTime: '2030-06-01T10:00:00Z',
      endDateTime: '2030-06-01T12:00:00Z',
      locationName: 'Centre MANA',
      address: '200 Main Street, Montreal',
      status: 'OPEN',
      maxCapacity: 100,
      registeredCount: 95,
      checkedInCount: 90,
      occupancyPercent: 90,
    },
    {
      eventId: 3,
      title: 'Echo_Event',
      startDateTime: '2030-07-01T10:00:00Z',
      endDateTime: '2030-07-01T12:00:00Z',
      locationName: 'Centre MANA',
      address: '300 Main Street, Montreal',
      status: 'OPEN',
      maxCapacity: 100,
      registeredCount: 30,
      checkedInCount: 10,
      occupancyPercent: 10,
    },
    {
      eventId: 4,
      title: 'Bravo_Event',
      startDateTime: '2030-05-15T10:00:00Z',
      endDateTime: '2030-05-15T12:00:00Z',
      locationName: 'Centre MANA',
      address: '400 Main Street, Montreal',
      status: 'OPEN',
      maxCapacity: 100,
      registeredCount: 70,
      checkedInCount: 60,
      occupancyPercent: 60,
    },
  ];

  const titleOrder = async (page: Page) => {
    const titles = await page.locator('[data-testid="attendance-event-title"]').allTextContents();
    return titles.map((title) => title.trim());
  };

  test.beforeEach(async ({ page }) => {
    await page.route('**/api/employee/attendance/events', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(attendanceEvents),
      });
    });

    await page.addInitScript((user) => {
      window.localStorage.setItem('userToken', JSON.stringify(user));
    }, adminUser);
  });

  test('Events are sorted by upcoming date by default', async ({ page }) => {
    await page.goto('/admin/attendance', { waitUntil: 'domcontentloaded' });

    await expect(page.getByTestId('attendance-event-title')).toHaveCount(4);

    await expect(page.getByTestId('attendance-event-title').first()).toHaveText('Zulu Event');
    await expect(await titleOrder(page)).toEqual([
      'Zulu Event',
      'Bravo Event',
      'Alpha Event',
      'Echo Event',
    ]);
  });

  test('Admin can change attendance sort order with filters', async ({ page }) => {
    await page.goto('/admin/attendance', { waitUntil: 'domcontentloaded' });

    const filterButton = page.getByTestId('attendance-filter-button');

    const applySort = async (key: string, expectedOrder: string[]) => {
      await filterButton.click();
      await page.getByTestId(`attendance-filter-option-${key}`).click();
      await expect(page.getByTestId('attendance-event-title').first()).toHaveText(expectedOrder[0]);
      await expect(await titleOrder(page)).toEqual(expectedOrder);
    };

    await applySort('latest', ['Echo Event', 'Alpha Event', 'Bravo Event', 'Zulu Event']);
    await applySort('alphaAsc', ['Alpha Event', 'Bravo Event', 'Echo Event', 'Zulu Event']);
    await applySort('alphaDesc', ['Zulu Event', 'Echo Event', 'Bravo Event', 'Alpha Event']);
    await applySort('occupancyLow', ['Echo Event', 'Zulu Event', 'Bravo Event', 'Alpha Event']);
    await applySort('occupancyHigh', ['Alpha Event', 'Bravo Event', 'Zulu Event', 'Echo Event']);
    await applySort('upcoming', ['Zulu Event', 'Bravo Event', 'Alpha Event', 'Echo Event']);
  });
});
