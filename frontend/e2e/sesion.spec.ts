import { test, expect } from '@playwright/test';
import { login } from './ayudas';

test('renueva automáticamente el JWT cinco minutos antes de vencer', async ({ page }) => {
  await page.clock.install({ time: new Date() });
  let renovaciones = 0;
  page.on('request', peticion => {
    if (peticion.url().endsWith('/api/auth/refresh')) renovaciones++;
  });
  await login(page, 'user');
  const hastaRenovacion = await page.evaluate(() => {
    const token = sessionStorage.getItem('reciclo.token')!;
    const datos = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return datos.exp * 1000 - Date.now() - 5 * 60 * 1000;
  });
  expect(hastaRenovacion).toBeGreaterThan(1000);
  await page.clock.fastForward(hastaRenovacion - 1000);
  expect(renovaciones).toBe(0);
  const respuesta = page.waitForResponse(r => r.url().endsWith('/api/auth/refresh'));
  await page.clock.fastForward(2000);
  expect((await respuesta).status()).toBe(200);
  expect(renovaciones).toBe(1);
  await expect(page.getByRole('heading', { name: /Hola,/ })).toBeVisible();
});
