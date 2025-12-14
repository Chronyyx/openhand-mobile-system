import { test, expect } from '@playwright/test';

test.describe('Admin events management', () => {
    test('Admin can duplicate an event from the action menu', async ({ page }) => {
        const adminUser = {
            token: 'admin-token',
            type: 'Bearer',
            id: 1,
            email: 'admin@mana.org',
            roles: ['ROLE_ADMIN'],
        };

        const baseEvent = {
            id: 1,
            title: 'Admin Test Event',
            description: 'A sample event used for duplication.',
            startDateTime: '2030-12-24T10:00:00',
            endDateTime: '2030-12-24T12:00:00',
            locationName: 'Centre MANA',
            address: '1910 Boulevard René-Lévesque, Montréal',
            status: 'OPEN',
            maxCapacity: 50,
            currentRegistrations: 10,
            category: 'GALA',
        };

        let duplicatedEvent: typeof baseEvent | null = null;
        let upcomingRequests = 0;

        await page.route('**/api/events/upcoming', async (route) => {
            upcomingRequests += 1;
            const events = [baseEvent];
            if (duplicatedEvent) {
                events.push(duplicatedEvent);
            }

            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(events),
            });
        });

        await page.route('**/api/admin/events', async (route) => {
            const requestBody = JSON.parse(route.request().postData() ?? '{}');
            duplicatedEvent = {
                ...baseEvent,
                ...requestBody,
                id: 999,
                status: 'OPEN',
                currentRegistrations: 0,
            };

            await route.fulfill({
                status: 201,
                contentType: 'application/json',
                body: JSON.stringify(duplicatedEvent),
            });
        });

        await page.goto('/', { waitUntil: 'domcontentloaded' });
        await page.evaluate((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, adminUser);

        await page.goto('/admin/events', { waitUntil: 'domcontentloaded' });
        await expect(page.getByText(baseEvent.title)).toBeVisible({ timeout: 15000 });

        await page.getByLabel(/more actions|autres actions|más acciones/i).first().click();
        const duplicateOption = page
            .getByText(/duplicate event|dupliquer l[’']événement|duplicar evento/i)
            .first();
        await expect(duplicateOption).toBeVisible();

        const dialogPromise = page
            .waitForEvent('dialog')
            .then((dialog) => dialog.dismiss().catch(() => undefined));

        await duplicateOption.click();

        await page.waitForResponse(
            (response) =>
                response.url().includes('/api/admin/events') &&
                response.request().method() === 'POST',
        );

        await dialogPromise;
        await expect.poll(() => upcomingRequests).toBeGreaterThanOrEqual(2);

        await expect(page.getByText(baseEvent.title)).toHaveCount(2);
    });
});
