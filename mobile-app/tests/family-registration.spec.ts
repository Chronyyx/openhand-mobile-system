import { test, expect } from '@playwright/test';

test.describe('Family Registration Flow', () => {
    test('Member can register with multiple family members in one registration', async ({ page }) => {
        const memberUser = {
            token: 'test-token-member',
            type: 'Bearer',
            id: 7,
            email: 'member@example.com',
            roles: ['ROLE_MEMBER'],
            name: 'Member User',
        };

        const event = {
            id: 1,
            title: 'family_event',
            description: 'Family event description',
            startDateTime: '2026-02-10T18:00:00',
            endDateTime: '2026-02-10T20:00:00',
            locationName: 'Community Hall',
            address: '123 Main St',
            status: 'OPEN',
            maxCapacity: 50,
            currentRegistrations: 10,
        };

        await page.route('**/users/profile', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    ...memberUser,
                    preferredLanguage: 'en',
                }),
            });
        });

        await page.route('**/events/upcoming', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([event]),
            });
        });

        await page.route('**/events/1', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(event),
            });
        });

        await page.route('**/registrations/my-registrations', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([]),
            });
        });

        await page.route('**/events/1/registrations', async (route) => {
            if (route.request().method() !== 'POST') {
                await route.continue();
                return;
            }

            const body = route.request().postDataJSON() as {
                familyMembers?: { fullName: string; age?: number }[];
            };

            expect(body.familyMembers?.length).toBe(2);
            expect(body.familyMembers?.[0]?.fullName).toBe('Jane Doe');
            expect(body.familyMembers?.[1]?.fullName).toBe('Mark Doe');

            await route.fulfill({
                status: 201,
                contentType: 'application/json',
                body: JSON.stringify({
                    eventId: event.id,
                    primaryRegistrant: {
                        registrationId: 101,
                        fullName: memberUser.name,
                        age: 30,
                        dateOfBirth: null,
                        relation: null,
                        primaryRegistrant: true,
                        status: 'CONFIRMED',
                        waitlistedPosition: null,
                    },
                    participants: [
                        {
                            registrationId: 101,
                            fullName: memberUser.name,
                            age: 30,
                            dateOfBirth: null,
                            relation: null,
                            primaryRegistrant: true,
                            status: 'CONFIRMED',
                            waitlistedPosition: null,
                        },
                        {
                            registrationId: 102,
                            fullName: 'Jane Doe',
                            age: 12,
                            dateOfBirth: null,
                            relation: 'Child',
                            primaryRegistrant: false,
                            status: 'CONFIRMED',
                            waitlistedPosition: null,
                        },
                        {
                            registrationId: 103,
                            fullName: 'Mark Doe',
                            age: 8,
                            dateOfBirth: null,
                            relation: 'Child',
                            primaryRegistrant: false,
                            status: 'CONFIRMED',
                            waitlistedPosition: null,
                        },
                    ],
                    remainingCapacity: 37,
                }),
            });
        });

        await page.addInitScript((user) => {
            window.localStorage.setItem('userToken', JSON.stringify(user));
        }, memberUser);

        await page.goto('/events', { waitUntil: 'domcontentloaded' });
        const viewDetails = page.getByText(/view details|voir d\xE9tails|details/i).first();
        await expect(viewDetails).toBeVisible({ timeout: 15000 });
        await viewDetails.click();

        await expect(page.getByText(/description/i)).toBeVisible({ timeout: 10000 });

        const addFamilyButton = page.getByText(/add family member|ajouter un membre/i);
        await expect(addFamilyButton).toBeVisible({ timeout: 10000 });
        await addFamilyButton.click();
        await addFamilyButton.click();

        const fullNameInputs = page.getByPlaceholder(/full name|nom complet/i);
        await expect(fullNameInputs).toHaveCount(2);
        await fullNameInputs.nth(0).fill('Jane Doe');
        await fullNameInputs.nth(1).fill('Mark Doe');

        const ageInputs = page.getByPlaceholder(/age/i);
        await expect(ageInputs).toHaveCount(2);
        await ageInputs.nth(0).fill('12');
        await ageInputs.nth(1).fill('8');

        const totalParticipantsLabel = page.getByText(/total participants|participants au total/i);
        await expect(totalParticipantsLabel).toBeVisible();
        const totalParticipantsValue = totalParticipantsLabel.locator('..').locator('text=/\\d+/').first();
        await expect(totalParticipantsValue).toHaveText(/3/);

        const registerButton = page.getByText(/register|s'inscrire|join waitlist/i).first();
        await expect(registerButton).toBeVisible({ timeout: 10000 });
        await registerButton.click();

        await expect(page.getByText(/participants/i)).toBeVisible({ timeout: 10000 });
        await expect(page.getByText('Jane Doe')).toBeVisible();
        await expect(page.getByText('Mark Doe')).toBeVisible();
        await expect(page.getByText(memberUser.name)).toBeVisible();
    });
});
