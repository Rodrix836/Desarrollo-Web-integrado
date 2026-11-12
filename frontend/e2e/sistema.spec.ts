import { test, expect } from '@playwright/test';

test('ADMIN registra, consulta, edita y elimina el mismo material', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('admin');
  await page.getByLabel('Contraseña', { exact: true }).fill('admin123');
  await page.getByRole('button', { name: 'Ingresar a ReCiclo' }).click();
  await expect(page.getByRole('heading', { name: /Hola,/ })).toBeVisible();
  await page.getByRole('link', { name: '+ Registrar residuo' }).click();
  await page.getByLabel('Cantidad').fill('2.125');
  await page.getByLabel('Empresa', { exact: false }).fill('Empresa E2E');
  await page.getByLabel('Descripción').fill('Material de prueba visual');
  await page.getByLabel('Disponible para intercambio').check();
  await page.getByRole('button', { name: 'Registrar material' }).click();
  await expect(page.getByText('2.125', { exact: false }).first()).toBeVisible();
  const detail = page.url();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Plástico' })).toBeVisible();
  await page.getByRole('link', { name: 'Editar residuo' }).click();
  await page.getByLabel('Cantidad').fill('3.75');
  await page.getByRole('button', { name: 'Guardar cambios' }).click();
  await expect(page.getByText('3.75', { exact: false }).first()).toBeVisible();
  await page.getByRole('link', { name: 'Consultas', exact: true }).click();
  await page.getByLabel('Empresa', { exact: true }).fill('Empresa E2E');
  await page.getByRole('button', { name: 'Buscar materiales' }).click();
  await expect(page.getByText('3.75 kg', { exact: false })).toBeVisible();
  await page.screenshot({ path: 'test-results/consultas-desktop.png', fullPage: true });
  await page.goto(detail);
  page.once('dialog', (dialog) => dialog.accept());
  await page.getByRole('button', { name: 'Eliminar residuo' }).click();
  await expect(page).toHaveURL(/\/residuos$/);
  await page.getByRole('link', { name: 'Usuarios', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Usuarios' })).toBeVisible();
  await expect(page.getByText('admin', { exact: true })).toBeVisible();
});

test('USER consulta y las rutas ADMIN están protegidas', async ({ page }) => {
  await page.goto('/residuos');
  await expect(page).toHaveURL(/login/);
  await page.getByLabel('Usuario', { exact: true }).fill('user');
  await page.getByLabel('Contraseña', { exact: true }).fill('user123');
  await page.getByRole('button', { name: 'Ingresar a ReCiclo' }).click();
  await expect(page).toHaveURL(/\/residuos$/);
  await expect(page.getByRole('link', { name: '+ Registrar residuo' })).toHaveCount(0);
  await page.goto('/usuarios');
  await expect(page.getByRole('heading', { name: /Hola,/ })).toBeVisible();
  await page.getByRole('button', { name: /Cerrar sesión/ }).click();
  await expect(page).toHaveURL(/login/);
});

test('registro, login inválido y vista móvil', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('user');
  await page.getByLabel('Contraseña', { exact: true }).fill('incorrecta');
  await page.getByRole('button', { name: 'Ingresar a ReCiclo' }).click();
  await expect(page.getByRole('alert')).toContainText('incorrectos');
  await page.getByRole('button', { name: 'Crear una cuenta' }).click();
  await page.getByLabel('Nombre completo').fill('Usuario de prueba');
  await page.getByLabel('Usuario', { exact: true }).fill('e2e-' + Date.now());
  await page.getByLabel('Contraseña', { exact: true }).fill('clave123');
  await page.getByRole('button', { name: /^Crear cuenta/ }).click();
  await expect(page.getByRole('heading', { name: /Hola,/ })).toBeVisible();
  await page.screenshot({ path: 'test-results/home-mobile.png', fullPage: true });
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
  ).toBeTruthy();
});

test('ADMIN cambia el rol de una cuenta registrada', async ({ page, request }) => {
  const username = 'rol-e2e-' + Date.now();
  const registro = await request.post('/api/auth/register', {
    data: { username, password: 'clave123', nombre: 'Cuenta de rol' },
  });
  expect(registro.status()).toBe(201);
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('admin');
  await page.getByLabel('Contraseña', { exact: true }).fill('admin123');
  await page.getByRole('button', { name: 'Ingresar a ReCiclo' }).click();
  await page.getByRole('link', { name: 'Usuarios', exact: true }).click();
  const row = page.getByRole('row').filter({ hasText: username });
  await row.getByRole('combobox').selectOption('ADMIN');
  await row.getByRole('button', { name: 'Guardar rol' }).click();
  await expect(page.getByRole('status')).toContainText('Rol actualizado');
  await page.getByRole('button', { name: /Cerrar sesión/ }).click();
  await page.getByLabel('Usuario', { exact: true }).fill(username);
  await page.getByLabel('Contraseña', { exact: true }).fill('clave123');
  await page.getByRole('button', { name: 'Ingresar a ReCiclo' }).click();
  await expect(page.getByRole('link', { name: 'Usuarios', exact: true })).toBeVisible();
});
