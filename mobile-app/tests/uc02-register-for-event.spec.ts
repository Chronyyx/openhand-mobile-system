import { expect, test, type Page } from '@playwright/test';

type MockUser = {
    id: number;
    email: string;
    password: string;
    name: string;
    roles: string[];
    memberStatus: 'ACTIVE' | 'INACTIVE';
};

type MockEvent = {
    id: number;
    title: string;
    description: string;
    startDateTime: string;
    endDateTime: string;
    locationName: string;
    address: string;
    status: 'OPEN' | 'NEARLY_FULL' | 'FULL' | 'CANCELLED' | 'COMPLETED';
    maxCapacity: number;
    currentRegistrations: number;
    category: string;
};

type MockRegistration = {
    id: number;
    userId: number;
    eventId: number;
    eventTitle: string;
    status: 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED' | 'REQUESTED';
    requestedAt: string;
    confirmedAt: string | null;
    cancelledAt: string | null;
    waitlistedPosition: number | null;
    eventStartDateTime: string | null;
    eventEndDateTime: string | null;
};

type ScenarioConfig = {
    user: MockUser;
    events: MockEvent[];
    eventDetails?: Record<number, MockEvent>;
    initialRegistrations?: MockRegistration[];
    registerHandler?: (payload: any) => {
        status: number;
        body: any;
        nextRegistrations?: MockRegistration[];
    };
};

function buildEvent(overrides: Partial<MockEvent> = {}): MockEvent {
    return {
        id: overrides.id ?? 101,
        title: overrides.title ?? 'MANA Gala 2026',
        description: overrides.description ?? 'Community event',
        startDateTime: overrides.startDateTime ?? '2026-04-10T18:00:00',
        endDateTime: overrides.endDateTime ?? '2026-04-10T20:00:00',
        locationName: overrides.locationName ?? 'MANA Center',
        address: overrides.address ?? '1910 Boulevard Rene-Levesque',
        status: overrides.status ?? 'OPEN',
        maxCapacity: overrides.maxCapacity ?? 100,
        currentRegistrations: overrides.currentRegistrations ?? 20,
        category: overrides.category ?? 'General',
    };
}

const ACTIVE_MEMBER: MockUser = {
    id: 11,
    email: 'active.member@mana.org',
    password: 'Password123!',
    name: 'Active Member',
    roles: ['ROLE_MEMBER'],
    memberStatus: 'ACTIVE',
};

const INACTIVE_MEMBER: MockUser = {
    id: 12,
    email: 'inactive.member@mana.org',
    password: 'Password123!',
    name: 'Inactive Member',
    roles: ['ROLE_MEMBER'],
    memberStatus: 'INACTIVE',
};

let hadUc02Failure = false;

async function setupScenarioRoutes(page: Page, config: ScenarioConfig) {
    let registrationsState: MockRegistration[] = [...(config.initialRegistrations ?? [])];
    const eventsById = new Map<number, MockEvent>();
    config.events.forEach((event) => eventsById.set(event.id, event));
    Object.entries(config.eventDetails ?? {}).forEach(([id, event]) => {
        eventsById.set(Number(id), event);
    });

    await page.route(/.*\/auth\/login$/, async (route) => {
        if (route.request().method() !== 'POST') {
            await route.continue();
            return;
        }
        const payload = route.request().postDataJSON() as { email?: string; password?: string };
        if (payload.email !== config.user.email || payload.password !== config.user.password) {
            await route.fulfill({
                status: 401,
                contentType: 'application/json',
                body: JSON.stringify({ message: 'Invalid credentials' }),
            });
            return;
        }

        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                token: `token-${config.user.id}`,
                refreshToken: `refresh-${config.user.id}`,
                type: 'Bearer',
                id: config.user.id,
                email: config.user.email,
                name: config.user.name,
                roles: config.user.roles,
                memberStatus: config.user.memberStatus,
            }),
        });
    });

    await page.route(/.*\/users\/profile$/, async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                id: config.user.id,
                email: config.user.email,
                name: config.user.name,
                roles: config.user.roles,
                memberStatus: config.user.memberStatus,
            }),
        });
    });

    await page.route(/.*\/notifications$/, async (route) => {
        if (route.request().method() === 'GET') {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([]),
            });
            return;
        }
        await route.continue();
    });

    await page.route(/.*\/notifications\/read-all$/, async (route) => {
        await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });

    await page.route(/.*\/events\/upcoming$/, async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(config.events),
        });
    });

    await page.route(/.*\/events\/\d+$/, async (route) => {
        if (route.request().method() !== 'GET') {
            await route.continue();
            return;
        }
        const url = route.request().url();
        const match = url.match(/\/events\/(\d+)$/);
        const eventId = match ? Number(match[1]) : NaN;
        const event = eventsById.get(eventId);
        if (!event) {
            await route.fulfill({
                status: 404,
                contentType: 'application/json',
                body: JSON.stringify({ message: `Event ${eventId} not found` }),
            });
            return;
        }
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(event),
        });
    });

    await page.route(/.*\/registrations\/my-registrations$/, async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(registrationsState),
        });
    });

    await page.route(/.*\/events\/\d+\/registrations$/, async (route) => {
        if (route.request().method() !== 'POST') {
            await route.continue();
            return;
        }
        const payload = route.request().postDataJSON();
        const result = config.registerHandler
            ? config.registerHandler(payload)
            : {
                status: 201,
                body: {
                    eventId: config.events[0]?.id ?? 0,
                    primaryRegistrant: {
                        registrationId: 1001,
                        fullName: config.user.name,
                        status: 'CONFIRMED',
                        waitlistedPosition: null,
                    },
                    participants: [
                        {
                            registrationId: 1001,
                            fullName: config.user.name,
                            status: 'CONFIRMED',
                            primaryRegistrant: true,
                        },
                    ],
                    remainingCapacity: 42,
                },
                nextRegistrations: registrationsState,
            };

        if (result.nextRegistrations) {
            registrationsState = result.nextRegistrations;
        }

        await route.fulfill({
            status: result.status,
            contentType: 'application/json',
            body: JSON.stringify(result.body),
        });
    });
}

async function loginAs(page: Page, user: MockUser) {
    await page.goto('/auth/login', { waitUntil: 'domcontentloaded' });
    const emailInput = page.getByRole('textbox', { name: /^Email$/i });
    const passwordInput = page.getByRole('textbox', { name: /^Password$/i });

    // Best effort UI login path (RN web inputs can be flaky in automation).
    await emailInput.click().catch(() => {});
    await emailInput.fill(user.email).catch(() => {});
    await passwordInput.click().catch(() => {});
    await passwordInput.fill(user.password).catch(() => {});
    await page.getByTestId('login-button').click().catch(() => {});

    const loggedInHomeButton = page.getByRole('button', { name: /browse events|parcourir|explorar eventos/i }).first();
    const loggedInLogoutButton = page.getByRole('button', { name: /log\s*out|logout|se déconnecter|cerrar sesión/i }).first();
    const loggedInEmail = page.getByText(new RegExp(user.email.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i')).first();
    const loginRegisterButton = page.getByRole('button', { name: /log in \/ register|log in|register|se connecter|iniciar sesión/i }).first();

    const loginSucceededViaUi = await Promise.race([
        loggedInHomeButton.isVisible({ timeout: 6000 }).catch(() => false),
        loggedInLogoutButton.isVisible({ timeout: 6000 }).catch(() => false),
    ]);

    const ensureStorageAuth = async () => {
        await page.evaluate((authUser) => {
            window.localStorage.setItem('userToken', JSON.stringify({
                token: `token-${authUser.id}`,
                refreshToken: `refresh-${authUser.id}`,
                type: 'Bearer',
                id: authUser.id,
                email: authUser.email,
                name: authUser.name,
                roles: authUser.roles,
                memberStatus: authUser.memberStatus,
            }));
        }, user);
    };

    if (!loginSucceededViaUi) {
        await ensureStorageAuth();
        await page.goto('/', { waitUntil: 'domcontentloaded' });
        await page.reload({ waitUntil: 'domcontentloaded' });
    }

    const authenticatedNow = await Promise.race([
        loggedInLogoutButton.isVisible({ timeout: 5000 }).catch(() => false),
        loggedInEmail.isVisible({ timeout: 5000 }).catch(() => false),
    ]);

    if (!authenticatedNow) {
        await ensureStorageAuth();
        await page.addInitScript((authUser) => {
            window.localStorage.setItem('userToken', JSON.stringify({
                token: `token-${authUser.id}`,
                refreshToken: `refresh-${authUser.id}`,
                type: 'Bearer',
                id: authUser.id,
                email: authUser.email,
                name: authUser.name,
                roles: authUser.roles,
                memberStatus: authUser.memberStatus,
            }));
        }, user);
        await page.goto('/', { waitUntil: 'domcontentloaded' });
        await page.reload({ waitUntil: 'domcontentloaded' });
    }

    const stillAnonymous = await loginRegisterButton.isVisible({ timeout: 3000 }).catch(() => false);
    if (stillAnonymous) {
        await ensureStorageAuth();
        await page.goto('/events', { waitUntil: 'domcontentloaded' });
    } else {
        await expect(loggedInHomeButton).toBeVisible({ timeout: 15000 });
    }
}

async function openFirstEventFromHome(page: Page) {
    const browseButton = page.getByRole('button', { name: /browse events|parcourir|explorar eventos/i }).first();
    if (await browseButton.isVisible({ timeout: 3000 }).catch(() => false)) {
        await browseButton.click();
    } else {
        await expect(page.getByRole('heading', { name: /events/i }).first()).toBeVisible({ timeout: 10000 });
    }
    const eventCardButton = page.getByRole('button', { name: /open\.|ouvert\.|abierto\./i }).first();
    if (await eventCardButton.isVisible({ timeout: 10000 }).catch(() => false)) {
        await eventCardButton.click();
        return;
    }

    const viewDetailsText = page.getByText(/view details|voir détails|ver detalles/i).first();
    await expect(viewDetailsText).toBeVisible({ timeout: 10000 });
    await viewDetailsText.click();
}

async function clickRegister(page: Page) {
    const registerButton = page.getByRole('button', {
        name: /register for this event|register|s'inscrire|join waitlist|rejoindre|unirse/i,
    }).first();
    await expect(registerButton).toBeVisible({ timeout: 10000 });
    await registerButton.click();
}

async function clickAddFamilyMember(page: Page, times = 1) {
    const addButton = page.getByRole('button', { name: /add family member|ajouter|agregar familiar/i }).first();
    await expect(addButton).toBeVisible({ timeout: 10000 });
    for (let i = 0; i < times; i++) {
        await addButton.click();
    }
}

test.describe('UC02 Register for Event', () => {
    test.describe.configure({ mode: 'serial' });
    test.afterEach(async ({}, testInfo) => {
        if (testInfo.status !== testInfo.expectedStatus) {
            hadUc02Failure = true;
        }
    });
    test('Register single member', async ({ page }) => {
        const event = buildEvent({ id: 201, status: 'OPEN' });

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [event],
            initialRegistrations: [],
            registerHandler: () => {
                const registration: MockRegistration = {
                    id: 9001,
                    userId: ACTIVE_MEMBER.id,
                    eventId: event.id,
                    eventTitle: event.title,
                    status: 'CONFIRMED',
                    requestedAt: new Date().toISOString(),
                    confirmedAt: new Date().toISOString(),
                    cancelledAt: null,
                    waitlistedPosition: null,
                    eventStartDateTime: event.startDateTime,
                    eventEndDateTime: event.endDateTime,
                };
                return {
                    status: 201,
                    body: {
                        eventId: event.id,
                        primaryRegistrant: {
                            registrationId: registration.id,
                            fullName: ACTIVE_MEMBER.name,
                            status: 'CONFIRMED',
                            waitlistedPosition: null,
                        },
                        participants: [
                            {
                                registrationId: registration.id,
                                fullName: ACTIVE_MEMBER.name,
                                status: 'CONFIRMED',
                                primaryRegistrant: true,
                            },
                        ],
                        remainingCapacity: 79,
                    },
                    nextRegistrations: [registration],
                };
            },
        });

        await loginAs(page, ACTIVE_MEMBER);
        await openFirstEventFromHome(page);
        await clickRegister(page);

        await expect(page.getByTestId('success-message')).toBeVisible();
        await expect(page.getByText(/undo|unregister|annuler/i)).toBeVisible();
        console.log('UC02 PASSED: Register single member');
    });

    test('Register with family', async ({ page }) => {
        const event = buildEvent({ id: 202, status: 'OPEN', maxCapacity: 10, currentRegistrations: 3 });

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [event],
            initialRegistrations: [],
            registerHandler: (payload) => {
                const familyMembers = payload?.familyMembers ?? [];
                const registration: MockRegistration = {
                    id: 9002,
                    userId: ACTIVE_MEMBER.id,
                    eventId: event.id,
                    eventTitle: event.title,
                    status: 'CONFIRMED',
                    requestedAt: new Date().toISOString(),
                    confirmedAt: new Date().toISOString(),
                    cancelledAt: null,
                    waitlistedPosition: null,
                    eventStartDateTime: event.startDateTime,
                    eventEndDateTime: event.endDateTime,
                };

                return {
                    status: 201,
                    body: {
                        eventId: event.id,
                        primaryRegistrant: {
                            registrationId: registration.id,
                            fullName: ACTIVE_MEMBER.name,
                            status: 'CONFIRMED',
                            waitlistedPosition: null,
                        },
                        participants: [
                            {
                                registrationId: registration.id,
                                fullName: ACTIVE_MEMBER.name,
                                status: 'CONFIRMED',
                                primaryRegistrant: true,
                            },
                            ...familyMembers.map((member: any, idx: number) => ({
                                registrationId: registration.id + idx + 1,
                                fullName: member.fullName,
                                status: 'CONFIRMED',
                                primaryRegistrant: false,
                            })),
                        ],
                        remainingCapacity: 4,
                    },
                    nextRegistrations: [registration],
                };
            },
        });

        await loginAs(page, ACTIVE_MEMBER);
        await openFirstEventFromHome(page);
        await clickAddFamilyMember(page, 2);

        await page.getByPlaceholder(/full name/i).nth(0).fill('Family Member One');
        await page.getByPlaceholder(/age/i).nth(0).fill('10');
        await page.getByPlaceholder(/full name/i).nth(1).fill('Family Member Two');
        await page.getByPlaceholder(/age/i).nth(1).fill('12');

        await clickRegister(page);

        await expect(page.getByTestId('success-message')).toBeVisible();
        await expect(page.getByText(/Family Member One/i)).toBeVisible();
        await expect(page.getByText(/Family Member Two/i)).toBeVisible();
        console.log('UC02 PASSED: Register with family');
    });

    test('Inactive member blocked', async ({ page }) => {
        const event = buildEvent({ id: 203, status: 'OPEN' });

        await setupScenarioRoutes(page, {
            user: INACTIVE_MEMBER,
            events: [event],
            initialRegistrations: [],
        });

        await loginAs(page, INACTIVE_MEMBER);
        await openFirstEventFromHome(page);

        await expect(page.getByTestId('error-message')).toBeVisible();
        await expect(page.getByRole('button', { name: /register for this event|register|s'inscrire|join waitlist|rejoindre|unirse/i })).toHaveCount(0);
        console.log('UC02 PASSED: Inactive member blocked');
    });

    test('Already registered blocked', async ({ page }) => {
        const event = buildEvent({ id: 204, status: 'OPEN' });
        const existingRegistration: MockRegistration = {
            id: 9004,
            userId: ACTIVE_MEMBER.id,
            eventId: event.id,
            eventTitle: event.title,
            status: 'CONFIRMED',
            requestedAt: new Date().toISOString(),
            confirmedAt: new Date().toISOString(),
            cancelledAt: null,
            waitlistedPosition: null,
            eventStartDateTime: event.startDateTime,
            eventEndDateTime: event.endDateTime,
        };

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [event],
            initialRegistrations: [],
            registerHandler: () => ({
                status: 409,
                body: {
                    status: 409,
                    error: 'Conflict',
                    message: 'Already Registered',
                },
                nextRegistrations: [existingRegistration],
            }),
        });

        await loginAs(page, ACTIVE_MEMBER);
        await openFirstEventFromHome(page);
        await clickRegister(page);

        await expect(page.getByText(/already registered|deja inscrit|ya estas registrado/i)).toBeVisible();
        console.log('UC02 PASSED: Already registered blocked');
    });

    test('Event completed blocked', async ({ page }) => {
        const summaryEvent = buildEvent({ id: 205, status: 'OPEN' });
        const completedDetail = buildEvent({ id: 205, status: 'COMPLETED' });

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [summaryEvent],
            eventDetails: { 205: completedDetail },
            initialRegistrations: [],
        });

        await loginAs(page, ACTIVE_MEMBER);
        await openFirstEventFromHome(page);

        await expect(page.getByText(/completed|termine|finalizado|archived/i).first()).toBeVisible();
        await expect(page.getByRole('button', { name: /register for this event|register|s'inscrire|join waitlist|rejoindre|unirse/i })).toHaveCount(0);
        console.log('UC02 PASSED: Event completed blocked');
    });

    test('Capacity exceeded blocked', async ({ page }) => {
        const event = buildEvent({ id: 206, status: 'OPEN', maxCapacity: 2, currentRegistrations: 2 });

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [event],
            initialRegistrations: [],
            registerHandler: () => ({
                status: 400,
                body: {
                    status: 400,
                    error: 'Bad Request',
                    message: 'capacity exceeded for group registration',
                },
            }),
        });

        await loginAs(page, ACTIVE_MEMBER);
        await openFirstEventFromHome(page);
        await clickAddFamilyMember(page, 2);

        await page.getByPlaceholder(/full name/i).nth(0).fill('Family A');
        await page.getByPlaceholder(/age/i).nth(0).fill('8');
        await page.getByPlaceholder(/full name/i).nth(1).fill('Family B');
        await page.getByPlaceholder(/age/i).nth(1).fill('9');

        await clickRegister(page);
        await expect(page.getByTestId('error-message')).toBeVisible();
        console.log('UC02 PASSED: Capacity exceeded blocked');
    });

    test('Event not found handled', async ({ page }) => {
        const event = buildEvent({ id: 207, status: 'OPEN' });

        await setupScenarioRoutes(page, {
            user: ACTIVE_MEMBER,
            events: [event],
            initialRegistrations: [],
        });

        await loginAs(page, ACTIVE_MEMBER);
        await page.goto('/events/999', { waitUntil: 'domcontentloaded' });

        await expect(page.getByText(/no events|no events found|unable to load|not found|aucun evenement|sin eventos/i)).toBeVisible();
        console.log('UC02 PASSED: Event not found handled');
    });

    test.afterAll(async () => {
        if (!hadUc02Failure) {
            console.log('ALL UC02 TESTS PASSED');
        }
    });
});
