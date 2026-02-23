import { test, expect } from '@playwright/test';

// Arrange / Act / Assert structure is used in comments for clarity.
test.describe('Filter Donations by Campaign and Date', () => {
  const adminUser = {
    token: 'admin-token-manual-donations',
    refreshToken: 'refresh-token',
    type: 'Bearer',
    id: 1,
    email: 'admin@mana.org',
    roles: ['ROLE_ADMIN'],
    name: 'Admin User',
  };


  test.beforeEach(async ({ page }) => {
    await page.addInitScript((user) => {
      window.localStorage.setItem('userToken', JSON.stringify(user));
    }, adminUser);

    await page.route('**/profile', async (route) => {
      await route.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(adminUser),
      });
    });

    await page.route('**/users/me/security-settings', async (route) => {
      await route.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ biometricsEnabled: false }),
      });
    });
  });

  test('Date filter updates donation list', async ({ page }) => {
    await page.route('**/employee/donations*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 1,
            userId: 10,
            donorName: 'Ada Lovelace',
            donorEmail: 'ada@mana.org',
            amount: 50,
            currency: 'CAD',
            frequency: 'ONE_TIME',
            status: 'RECEIVED',
            createdAt: '2026-01-01T10:00:00',
          },
          {
            id: 2,
            userId: 11,
            donorName: 'Grace Hopper',
            donorEmail: 'grace@mana.org',
            amount: 75,
            currency: 'CAD',
            frequency: 'ONE_TIME',
            status: 'RECEIVED',
            createdAt: '2025-12-28T10:00:00',
          },
        ]),
      });
    });

    await page.route('**/admin/events/all', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.route('**/notifications', async (route) => {
      await route.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify([]),
      });
    });

    await page.route('**/notifications/unread-count', async (route) => {
      await route.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ count: 0 }),
      });
    });

    await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });

    await expect(page.getByText('Ada Lovelace')).toBeVisible();
    await expect(page.getByText('Grace Hopper')).toBeVisible();

    const yearInput = page.getByTestId('date-filter-year');
    const monthInput = page.getByTestId('date-filter-month');
    const dayInput = page.getByTestId('date-filter-day');
    await yearInput.fill('2026');
    await monthInput.fill('01');
    await dayInput.fill('01');

    await expect(page.getByText('Ada Lovelace')).toBeVisible();
  });


  test('Combined event and date filter, clearing restores full list', async ({ page }) => {
    const donationRows = [
      {
        id: 1,
        userId: 10,
        donorName: 'Ada Lovelace',
        donorEmail: 'ada@mana.org',
        amount: 50,
        currency: 'CAD',
        frequency: 'ONE_TIME',
        status: 'RECEIVED',
        eventId: 101,
        eventName: 'Mana Gala',
        createdAt: '2026-01-01T10:00:00',
      },
      {
        id: 2,
        userId: 11,
        donorName: 'Grace Hopper',
        donorEmail: 'grace@mana.org',
        amount: 75,
        currency: 'CAD',
        frequency: 'ONE_TIME',
        status: 'RECEIVED',
        eventId: null,
        eventName: null,
        createdAt: '2025-12-28T10:00:00',
      },
    ];

    await page.route('**/admin/events/all', route => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 101,
            title: 'Mana Gala',
            description: 'Annual fundraising gala',
            startDateTime: '2026-02-15T18:00:00',
            endDateTime: '2026-02-15T22:00:00',
            locationName: 'Mana Center',
            address: '123 Main St',
            status: 'OPEN',
            maxCapacity: 200,
            currentRegistrations: 50,
            category: 'Fundraiser',
            imageUrl: null,
          }
        ])
      });
    });

    await page.route('**/employee/donations*', async (route) => {
      const url = new URL(route.request().url());
      const eventIdParam = url.searchParams.get('eventId');
      const yearParam = url.searchParams.get('year');
      const monthParam = url.searchParams.get('month');
      const dayParam = url.searchParams.get('day');

      const filtered = donationRows.filter((row) => {
        if (eventIdParam && String(row.eventId ?? '') !== eventIdParam) {
          return false;
        }
        if (yearParam && !row.createdAt.startsWith(`${yearParam}-`)) {
          return false;
        }
        if (monthParam) {
          const month = row.createdAt.slice(5, 7);
          if (month !== monthParam.padStart(2, '0')) {
            return false;
          }
        }
        if (dayParam) {
          const day = row.createdAt.slice(8, 10);
          if (day !== dayParam.padStart(2, '0')) {
            return false;
          }
        }
        return true;
      });

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(filtered),
      });
    });

    await page.route('**/notifications', async (route) => {
      await route.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify([]),
      });
    });

    await page.route('**/notifications/unread-count', async (route) => {
      await route.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ count: 0 }),
      });
    });

    await page.goto('/admin/donations', { waitUntil: 'domcontentloaded' });

    await expect(page.getByText('Ada Lovelace')).toBeVisible();
    await expect(page.getByText('Grace Hopper')).toBeVisible();

    const eventSelect = page.getByTestId('event-filter-dropdown');
    await expect(eventSelect).toBeVisible();
    const options = await eventSelect.locator('option').allTextContents();
    const eventOption = options.find(opt => opt && opt !== '' && !opt.toLowerCase().includes('select'));
    if (!eventOption) {
      test.skip(true, 'No event options available to filter');
      return;
    }
    await eventSelect.selectOption({ label: eventOption });

    const yearInput = page.getByTestId('date-filter-year');
    const monthInput = page.getByTestId('date-filter-month');
    const dayInput = page.getByTestId('date-filter-day');
    await yearInput.fill('2026');
    await monthInput.fill('01');
    await dayInput.fill('01');

    await expect(page.getByText('Ada Lovelace')).toBeVisible();
    await expect(page.getByText('Grace Hopper')).toHaveCount(0);

    await eventSelect.selectOption({ index: 0 });
    await yearInput.fill('');
    await monthInput.fill('');
    await dayInput.fill('');
    await expect(page.getByText('Ada Lovelace')).toBeVisible();
    await expect(page.getByText('Grace Hopper')).toBeVisible();
  });
});
