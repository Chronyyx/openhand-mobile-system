import { test, expect } from '@playwright/test';

test.describe('Attendance Dashboard', () => {
  const employeeUser = {
    token: 'test-token-employee',
    type: 'Bearer',
    id: 2,
    email: 'employee@mana.org',
    roles: ['ROLE_EMPLOYEE'],
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
    registeredCount: 12,
    checkedInCount: 4,
    occupancyPercent: 8,
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

  const attendeesResponse = {
    eventId: 1,
    registeredCount: 12,
    checkedInCount: 4,
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
    ],
  };

  test.beforeEach(async ({ page }) => {
    await page.route('**/api/employee/attendance/events/1/attendees', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(attendeesResponse),
      });
    });

    await page.route('**/api/employee/attendance/events', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([attendanceEvent]),
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
    }, employeeUser);
  });

  test('Employee can open attendance dashboard and check in attendees', async ({ page }) => {
    await page.route('**/api/employee/attendance/events/1/attendees/10/check-in', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            eventId: 1,
            userId: 10,
            checkedIn: true,
            checkedInAt: '2030-06-01T10:10:00Z',
            registeredCount: 12,
            checkedInCount: 5,
            occupancyPercent: 10,
          }),
        });
      } else {
        await route.fulfill({ status: 405 });
      }
    });

    await page.goto('/admin/attendance', { waitUntil: 'domcontentloaded' });

    await expect(page.getByText(/attendance|présences|asistencia/i)).toBeVisible();
    await expect(page.getByText(/training session|formation|capacitaci/i)).toBeVisible();

    const viewAttendees = page.getByText(/view attendees|voir les participants|ver asistentes/i).first();
    await expect(viewAttendees).toBeVisible();
    await viewAttendees.click();

    await expect(page.getByText('Ada Lovelace')).toBeVisible();
    await expect(page.getByText('ada@mana.org')).toBeVisible();

    const searchInput = page.getByPlaceholder(/name or email|nom ou courriel|nombre o correo/i).first();
    await searchInput.fill('Ada');
    await expect(page.getByText('Alan Turing')).toHaveCount(0);

    const checkInButton = page.getByText(/check in|enregistrer l'arrivée|registrar llegada/i).first();
    await checkInButton.click();

    await expect(page.getByText(/undo check-in|annuler l'arrivée|deshacer llegada/i)).toBeVisible();
  });

  test('Employee can undo a check-in and filter attendees', async ({ page }) => {
    await page.route('**/api/employee/attendance/events/1/attendees/11/check-in', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            eventId: 1,
            userId: 11,
            checkedIn: false,
            checkedInAt: null,
            registeredCount: 12,
            checkedInCount: 3,
            occupancyPercent: 6,
          }),
        });
      } else {
        await route.fulfill({ status: 405 });
      }
    });

    await page.goto('/admin/attendance', { waitUntil: 'domcontentloaded' });
    await page.getByText(/view attendees|voir les participants|ver asistentes/i).first().click();

    const searchInput = page.getByPlaceholder(/name or email|nom ou courriel|nombre o correo/i).first();
    await searchInput.fill('Alan');

    const undoButton = page.getByText(/undo check-in|annuler l'arrivée|deshacer llegada/i).first();
    await undoButton.click();

    await expect(page.getByText(/check in|enregistrer l'arrivée|registrar llegada/i)).toBeVisible();
    await expect(page.getByText(/undo check-in|annuler l'arrivée|deshacer llegada/i)).toHaveCount(0);

    await searchInput.fill('Ada');

    await expect(page.getByText('Ada Lovelace')).toBeVisible();
    await expect(page.getByText('Alan Turing')).toHaveCount(0);
  });
});
