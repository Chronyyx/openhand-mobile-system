import { expect, test } from '@playwright/test';

const authUser = {
  token: 'existing-access-token',
  refreshToken: 'existing-refresh-token',
  type: 'Bearer',
  id: 1,
  email: 'member@mana.org',
  roles: ['ROLE_MEMBER'],
  name: 'Member User',
  phoneNumber: '+15145551234',
  gender: 'MALE',
  age: 30,
};

test.describe('Biometrics', () => {
  test('settings toggle enables and disables biometrics with deterministic local auth mock', async ({ page }) => {
    await page.addInitScript((storedUser) => {
      localStorage.setItem('userToken', JSON.stringify(storedUser));
      // @ts-expect-error test hook
      window.__LOCAL_AUTH_MOCK__ = {
        isAvailable: true,
        isEnrolled: true,
        authenticateSuccess: true,
      };
    }, authUser);

    let biometricsEnabled = false;
    await page.route('**/api/users/me/security-settings', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ biometricsEnabled }),
        });
        return;
      }
      if (route.request().method() === 'PUT') {
        const body = route.request().postDataJSON() as { biometricsEnabled: boolean };
        biometricsEnabled = body.biometricsEnabled;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ biometricsEnabled }),
        });
        return;
      }
      await route.continue();
    });

    await page.goto('/settings/biometrics');

    const toggle = page.locator('[data-testid="biometrics-toggle"]');
    await expect(toggle).toBeVisible();

    const enableRequest = page.waitForRequest((request) => {
      return request.url().includes('/api/users/me/security-settings') &&
        request.method() === 'PUT' &&
        request.postData()?.includes('"biometricsEnabled":true') === true;
    });
    await toggle.click();
    await enableRequest;

    await expect.poll(async () => {
      return page.evaluate(() => localStorage.getItem('biometricRefreshToken'));
    }).toBe('existing-refresh-token');

    const disableRequest = page.waitForRequest((request) => {
      return request.url().includes('/api/users/me/security-settings') &&
        request.method() === 'PUT' &&
        request.postData()?.includes('"biometricsEnabled":false') === true;
    });
    await toggle.click();
    await disableRequest;

    await expect.poll(async () => {
      return page.evaluate(() => localStorage.getItem('biometricRefreshToken'));
    }).toBeNull();
  });

  test('biometric login button appears and password fallback remains usable when biometric auth fails', async ({ page }) => {
    await page.addInitScript((storedUser) => {
      localStorage.setItem('biometricsEnabled', 'true');
      localStorage.setItem('biometricRefreshToken', 'stored-biometric-refresh');
      localStorage.setItem('biometricUserId', String(storedUser.id));
      // @ts-expect-error test hook
      window.__LOCAL_AUTH_MOCK__ = {
        isAvailable: true,
        isEnrolled: true,
        authenticateSuccess: false,
        error: 'authentication_failed',
      };
    }, authUser);

    await page.route('**/api/auth/login', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            token: 'password-access',
            refreshToken: 'password-refresh',
            type: 'Bearer',
            id: 1,
            email: 'member@mana.org',
            roles: ['ROLE_MEMBER'],
            name: 'Member User',
          }),
        });
        return;
      }
      await route.continue();
    });

    await page.goto('/auth/login');
    const biometricButton = page.getByTestId('biometric-login-button');
    await expect(biometricButton).toBeVisible();

    await biometricButton.click();
    await expect(page.getByText(/biometric authentication failed/i)).toBeVisible();

    await page.getByPlaceholder(/email or phone number/i).fill('member@mana.org');
    await page.getByPlaceholder(/password/i).fill('password123');
    await page.getByText(/log in/i).nth(1).click();
    await page.waitForURL('/');
  });

  test('biometric login success refreshes token and lands in authenticated state', async ({ page }) => {
    await page.addInitScript((storedUser) => {
      localStorage.setItem('biometricsEnabled', 'true');
      localStorage.setItem('biometricRefreshToken', 'stored-biometric-refresh');
      localStorage.setItem('biometricUserId', String(storedUser.id));
      // @ts-expect-error test hook
      window.__LOCAL_AUTH_MOCK__ = {
        isAvailable: true,
        isEnrolled: true,
        authenticateSuccess: true,
      };
    }, authUser);

    await page.route('**/api/auth/refreshtoken', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'biometric-access-token',
          refreshToken: 'rotated-biometric-refresh',
        }),
      });
    });

    await page.route('**/api/users/profile', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1,
          email: 'member@mana.org',
          roles: ['ROLE_MEMBER'],
          name: 'Member User',
          phoneNumber: '+15145551234',
          gender: 'MALE',
          age: 30,
          memberStatus: 'ACTIVE',
          statusChangedAt: null,
          profilePictureUrl: null,
          preferredLanguage: 'en',
        }),
      });
    });

    await page.goto('/auth/login');
    await page.getByTestId('biometric-login-button').click();

    await expect.poll(async () => {
      return page.evaluate(() => {
        const raw = localStorage.getItem('userToken');
        if (!raw) {
          return null;
        }
        return JSON.parse(raw).token as string;
      });
    }).toBe('biometric-access-token');
    await page.waitForURL('/');
  });
});
