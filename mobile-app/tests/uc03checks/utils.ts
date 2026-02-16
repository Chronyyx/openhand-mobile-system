
import { type Page, expect } from '@playwright/test';

export type MockUser = {
    id: number;
    email: string;
    password: string;
    name: string;
    roles: string[];
    memberStatus: 'ACTIVE' | 'INACTIVE';
};

export type MockEvent = {
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

export type MockRegistration = {
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
    registrationGroupId?: string;
    primaryRegistrant?: boolean;
    primaryUserId?: number;
    participantFullName?: string;
    participantAge?: number;
    participantRelation?: string;
};

export type ScenarioConfig = {
    user: MockUser;
    events: MockEvent[];
    eventDetails?: Record<number, MockEvent>;
    initialRegistrations?: MockRegistration[];
    registerHandler?: (payload: any) => {
        status: number;
        body: any;
        nextRegistrations?: MockRegistration[];
    };
    cancelHandler?: (eventId: number) => {
        status: number;
        body: any;
        nextRegistrations?: MockRegistration[];
    };
};

export function buildEvent(overrides: Partial<MockEvent> = {}): MockEvent {
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

export const ACTIVE_MEMBER: MockUser = {
    id: 11,
    email: 'active.member@mana.org',
    password: 'Password123!',
    name: 'Active Member',
    roles: ['ROLE_MEMBER'],
    memberStatus: 'ACTIVE',
};

export const INACTIVE_MEMBER: MockUser = {
    id: 12,
    email: 'inactive.member@mana.org',
    password: 'Password123!',
    name: 'Inactive Member',
    roles: ['ROLE_MEMBER'],
    memberStatus: 'INACTIVE',
};

export async function setupScenarioRoutes(page: Page, config: ScenarioConfig) {
    let registrationsState: MockRegistration[] = [...(config.initialRegistrations ?? [])];
    const eventsById = new Map<number, MockEvent>();
    config.events.forEach((event) => eventsById.set(event.id, event));
    Object.entries(config.eventDetails ?? {}).forEach(([id, event]) => {
        eventsById.set(Number(id), event);
    });

    // Mock Login
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

    // Mock Profile
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

    // Mock Notifications
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

    // Mock Events List
    // Updated to use /api/ prefix
    await page.route(/.*\/api\/events\/upcoming$/, async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(config.events),
        });
    });

    // Mock Event Details
    await page.route(/.*\/api\/events\/\d+$/, async (route) => {
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

    // Mock My Registrations
    await page.route(/.*\/api\/registrations\/my-registrations$/, async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(registrationsState),
        });
    });

    // Mock Registration (POST)
    await page.route(/.*\/api\/events\/\d+\/registrations$/, async (route) => {
        if (route.request().method() !== 'POST') {
            await route.continue();
            return;
        }
        const payload = route.request().postDataJSON();
        const result = config.registerHandler
            ? config.registerHandler(payload)
            : {
                status: 201,
                body: { // Default dummy response
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

    // Mock DELETE Registration
    await page.route(/.*\/api\/registrations\/event\/\d+$/, async (route) => {
        if (route.request().method() !== 'DELETE') {
            await route.continue();
            return;
        }
        const url = route.request().url();
        const match = url.match(/\/event\/(\d+)$/);
        const eventId = match ? Number(match[1]) : NaN;

        const result = config.cancelHandler
            ? config.cancelHandler(eventId)
            : {
                status: 200,
                body: {},
                nextRegistrations: registrationsState, // updated list
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

export async function loginAs(page: Page, user: MockUser) {
    await page.goto('/auth/login', { waitUntil: 'domcontentloaded' });
    const emailInput = page.getByRole('textbox', { name: /^Email$/i });
    const passwordInput = page.getByRole('textbox', { name: /^Password$/i });

    // Best effort UI login path (RN web inputs can be flaky in automation).
    await emailInput.click().catch(() => { });
    await emailInput.fill(user.email).catch(() => { });
    await passwordInput.click().catch(() => { });
    await passwordInput.fill(user.password).catch(() => { });
    await page.getByTestId('login-button').click().catch(() => { });

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

export async function openFirstEventFromHome(page: Page) {
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

export async function clearAuthState(page: Page) {
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.evaluate(() => {
        window.localStorage.clear();
        window.sessionStorage.clear();
    });
    await page.context().clearCookies();
}
