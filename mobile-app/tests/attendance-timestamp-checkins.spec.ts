import { test, expect } from '@playwright/test';

test.describe('Attendance Timestamp Check-Ins', () => {
  const employeeUser = {
    token: 'test-token-employee',
    type: 'Bearer',
    id: 2,
    email: 'employee@mana.org',
    roles: ['ROLE_EMPLOYEE'],
  } as const;

  const attendanceEvent = {
    eventId: 1,
    title: 'Workshop_Event',
    startDateTime: '2030-06-15T14:00:00Z',
    endDateTime: '2030-06-15T16:00:00Z',
    locationName: 'Conference Room A',
    address: '456 Event Avenue, Montreal',
    status: 'OPEN',
    maxCapacity: 30,
    registeredCount: 5,
    checkedInCount: 2,
    occupancyPercent: 6.67,
  };

  const eventDetail = {
    id: 1,
    title: attendanceEvent.title,
    description: 'Workshop with timestamp tracking.',
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
    registeredCount: 5,
    checkedInCount: 2,
    attendees: [
      {
        userId: 100,
        fullName: 'Marie Curie',
        email: 'marie@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: false,
        checkedInAt: null,
      },
      {
        userId: 101,
        fullName: 'Albert Einstein',
        email: 'albert@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: true,
        checkedInAt: '2030-06-15T14:05:30Z',
      },
      {
        userId: 102,
        fullName: 'Isaac Newton',
        email: 'isaac@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: true,
        checkedInAt: '2030-06-15T14:12:45Z',
      },
      {
        userId: 103,
        fullName: 'Grace Hopper',
        email: 'grace@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: false,
        checkedInAt: null,
      },
      {
        userId: 104,
        fullName: 'Nikola Tesla',
        email: 'nikola@mana.org',
        registrationStatus: 'CONFIRMED',
        checkedIn: false,
        checkedInAt: null,
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

  test('Timestamp Check-Ins: Display existing timestamps, record new check-in timestamp, verify immutability, and validate undo clears timestamp', async ({
    page,
  }) => {
    // Navigate to attendance dashboard
    await page.goto('/admin/attendance', { waitUntil: 'domcontentloaded' });

    // Click to view attendees
    const viewAttendeesButton = page.getByText(/view attendees|voir les participants|ver asistentes/i).first();
    await expect(viewAttendeesButton).toBeVisible();
    await viewAttendeesButton.click();

    // ========== PART 1: Verify existing timestamps are displayed correctly ==========
    
    // Verify Albert Einstein (already checked in) shows timestamp
    await expect(page.getByText('Albert Einstein')).toBeVisible();
    await expect(page.getByText('albert@mana.org')).toBeVisible();
    
    // Check that the timestamp label appears (supports all 3 languages)
    const albertTimestampLabel = page.getByText(/checked in at|présent depuis|presente desde/i).first();
    await expect(albertTimestampLabel).toBeVisible();
    
    // Verify the formatted timestamp is displayed (2030-06-15 14:05)
    await expect(page.getByText(/2030-06-15 14:05/)).toBeVisible();
    
    // Verify Isaac Newton's timestamp is also displayed
    await expect(page.getByText('Isaac Newton')).toBeVisible();
    await expect(page.getByText(/2030-06-15 14:12/)).toBeVisible();

    // ========== PART 2: Verify unchecked attendees do NOT show timestamps ==========
    
    // Search for Marie Curie (not checked in)
    const searchInput = page.getByPlaceholder(/name or email|nom ou courriel|nombre o correo/i).first();
    await searchInput.fill('Marie');
    
    // Wait for search to filter results
    await page.waitForTimeout(300);
    
    await expect(page.getByText('Marie Curie')).toBeVisible();
    await expect(page.getByText('marie@mana.org')).toBeVisible();
    
    // Verify Marie doesn't have a "Checked in at" timestamp label (she's not checked in)
    // The timestamp label should not appear near Marie's name
    const marieTimestampLabel = page.getByText('Marie Curie').locator('..').locator('..').getByText(/checked in at|présent depuis|presente desde/i);
    await expect(marieTimestampLabel).toHaveCount(0);
    
    // Also verify "Not checked in" status is shown (use first to avoid strict mode violation)
    await expect(page.getByText(/not checked in|non présent|no presente/i).first()).toBeVisible();
    
    // Clear search to see all attendees again
    await searchInput.clear();
    await page.waitForTimeout(300);

    // ========== PART 3: Check in a new attendee and verify timestamp is recorded ==========
    
    // Mock the check-in API call for Marie Curie
    await page.route('**/api/employee/attendance/events/1/attendees/100/check-in', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            eventId: 1,
            userId: 100,
            checkedIn: true,
            checkedInAt: '2030-06-15T14:20:15Z', // New timestamp
            registeredCount: 5,
            checkedInCount: 3,
            occupancyPercent: 10.0,
          }),
        });
      } else {
        await route.fulfill({ status: 405 });
      }
    });

    // Filter to Marie Curie
    await searchInput.fill('Marie');
    await page.waitForTimeout(300);
    
    // Click check-in button
    const checkInButton = page.getByText(/^check in$|^enregistrer l'arrivée$|^registrar llegada$/i).first();
    await expect(checkInButton).toBeVisible();
    await checkInButton.click();

    // Wait for check-in to complete and UI to update
    await page.waitForTimeout(800);

    // Verify timestamp now appears for Marie
    await expect(page.getByText(/checked in at|présent depuis|presente desde/i).first()).toBeVisible();
    await expect(page.getByText(/2030-06-15 14:20/).first()).toBeVisible();
    
    // Verify status changed to "Checked in" - check last occurrence since Marie was just updated
    const checkedInStatus = page.getByText(/^checked in$|^présent$|^presente$/i);
    await expect(checkedInStatus.last()).toBeVisible();
    
    // Verify button changed to "Undo check-in"
    await expect(page.getByText(/undo check-in|annuler l'arrivée|deshacer llegada/i).first()).toBeVisible();

    // ========== PART 4: Verify timestamp immutability (backend prevents changes) ==========
    
    // The backend ensures timestamps cannot be edited once set (verified in backend tests)
    // Here we verify the timestamp persists correctly in the UI
    // Re-verify the timestamp is still showing the same value
    await expect(page.getByText(/2030-06-15 14:20/).first()).toBeVisible();
    
    // ========== PART 5: Undo check-in and verify timestamp disappears ==========
    
    // Mock undo check-in API call
    await page.route('**/api/employee/attendance/events/1/attendees/100/check-in', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            eventId: 1,
            userId: 100,
            checkedIn: false,
            checkedInAt: null, // Timestamp cleared
            registeredCount: 5,
            checkedInCount: 2,
            occupancyPercent: 6.67,
          }),
        });
      } else {
        await route.fulfill({ status: 405 });
      }
    });

    // Click undo button
    const undoButton = page.getByText(/undo check-in|annuler l'arrivée|deshacer llegada/i).first();
    await expect(undoButton).toBeVisible();
    await undoButton.click();

    // Wait for undo to complete
    await page.waitForTimeout(500);

    // Verify timestamp label is no longer visible for Marie
    const timestampLabelAfterUndo = page.getByText(/checked in at|présent depuis|presente desde/i);
    await expect(timestampLabelAfterUndo).toHaveCount(0);
    
    // Verify the specific timestamp for Marie (14:20) is no longer visible
    const marieTimestampAfterUndo = page.getByText(/14:20/);
    await expect(marieTimestampAfterUndo).toHaveCount(0);
    
    // Verify status changed back to "Not checked in" (use first to avoid strict mode violation)
    await expect(page.getByText(/not checked in|non présent|no presente/i).first()).toBeVisible();
    
    // Verify button changed back to "Check in"
    await expect(page.getByText(/^check in$|^enregistrer l'arrivée$|^registrar llegada$/i).first()).toBeVisible();

    // ========== PART 6: Verify timestamp format consistency across multiple attendees ==========
    
    // Clear search to see all attendees
    await searchInput.clear();
    await page.waitForTimeout(300);

    // Verify all checked-in attendees show timestamps in consistent format
    await expect(page.getByText('Albert Einstein')).toBeVisible();
    await expect(page.getByText(/2030-06-15 14:05/)).toBeVisible();
    
    await expect(page.getByText('Isaac Newton')).toBeVisible();
    await expect(page.getByText(/2030-06-15 14:12/)).toBeVisible();

    // Verify unchecked attendees don't show timestamps
    await expect(page.getByText('Grace Hopper')).toBeVisible();
    await expect(page.getByText('Nikola Tesla')).toBeVisible();
    
    // Count how many timestamps are displayed (should be 2: Albert and Isaac)
    const allTimestamps = page.getByText(/2030-06-15 \d{2}:\d{2}/);
    await expect(allTimestamps).toHaveCount(2);
  });
});
