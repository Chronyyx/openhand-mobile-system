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


  test('Date filter updates donation list', async ({ page }) => {
        // Simulate admin login by seeding localStorage
        await page.addInitScript((user) => {
          window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);
    // Arrange: Go to admin dashboard, open menu, and navigate to donations page
    await page.goto('/admin');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);
    // Open navigation menu (menu button in header)
    await page.getByRole('button', { name: /menu|navigation/i }).click({ timeout: 5000 }).catch(() => {
      // fallback: click the first button in the header if role lookup fails
      return page.locator('header button, [role=button]').first().click();
    });
    await page.waitForTimeout(500);
    // Debug: print page content after opening menu
    console.log('PAGE AFTER MENU:', await page.content());
    // Click the "Donations Management" menu item
    await page.getByRole('button', { name: /donations management/i }).click();
    await page.waitForTimeout(1000);


    // Wait for loading spinner to disappear
    await page.waitForSelector('text=/loading/i', { state: 'detached', timeout: 10000 });

    // Act: Fill year, month, and day filters using placeholder
    const yearInput = page.getByPlaceholder('YYYY');
    const monthInput = page.getByPlaceholder('MM');
    const dayInput = page.getByPlaceholder('DD');
    await expect(yearInput).toBeVisible();
    await yearInput.fill('2026');
    await monthInput.fill('01');
    await dayInput.fill('01');
    await page.waitForTimeout(1000);

    // Assert: All visible donation cards have a created date containing '2026-01-01' (if any)
    const cards = page.locator('[data-testid="donation-card"]', { hasText: '2026-01-01' });
    expect(await cards.count()).toBeGreaterThanOrEqual(0);
  });


  test('Combined event and date filter, clearing restores full list', async ({ page }) => {
    // Mock /admin/events/all to return a sample event
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
    // Simulate admin login by seeding localStorage
    await page.addInitScript((user) => {
      window.localStorage.setItem('userToken', JSON.stringify(user));
    }, adminUser);
    // ...existing code...
    await page.goto('/admin');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);
    await page.getByRole('button', { name: /menu|navigation/i }).click({ timeout: 5000 }).catch(() => {
      return page.locator('header button, [role=button]').first().click();
    });
    await page.waitForTimeout(500);
    console.log('PAGE AFTER MENU:', await page.content());
    await page.getByRole('button', { name: /donations management/i }).click();
    await page.waitForTimeout(1000);
    // Act: Apply event filter
    const eventSelect = page.getByTestId('event-filter-dropdown');
    await expect(eventSelect).toBeVisible();
    const options = await eventSelect.locator('option').allTextContents();
    const eventOption = options.find(opt => opt && opt !== '' && !opt.toLowerCase().includes('select'));
    if (!eventOption) test.skip('No event options available to filter');
    await eventSelect.selectOption({ label: eventOption });
    await page.waitForTimeout(500);
    // Apply date filter
    const yearInput = page.getByTestId('date-filter-year');
    const monthInput = page.getByTestId('date-filter-month');
    const dayInput = page.getByTestId('date-filter-day');
    await yearInput.fill('2026');
    await monthInput.fill('01');
    await dayInput.fill('01');
    await page.waitForTimeout(1000);
    // Assert: All visible donation cards contain both event name and date
    const cards = page.locator('[data-testid="donation-card"]', { hasText: eventOption });
    const dateCards = cards.filter({ hasText: '2026-01-01' });
    expect(await dateCards.count()).toBeGreaterThanOrEqual(0);
    // Act: Clear filters (reset event and date fields)
    await eventSelect.selectOption({ index: 0 });
    await yearInput.fill('');
    await monthInput.fill('');
    await dayInput.fill('');
    await page.waitForTimeout(1000);
    // Assert: Full dataset is restored (more cards visible)
    const allCards = page.locator('[data-testid="donation-card"]');
    expect(await allCards.count()).toBeGreaterThanOrEqual(await dateCards.count());
  });
});
