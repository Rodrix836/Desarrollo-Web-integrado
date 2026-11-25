import { test, expect, Page, Locator } from '@playwright/test';

const etiquetas = ['Plástico', 'Papel y cartón', 'Vidrio', 'Metal', 'Orgánico'];

async function sesion(page: Page, rol: 'ADMIN' | 'USER') {
  await page.addInitScript((rol) => {
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
    sessionStorage.setItem('reciclo.token', `prueba.${payload}.prueba`);
    sessionStorage.setItem('reciclo.sesion', JSON.stringify({ username: 'prueba', nombre: 'Prueba', rol }));
  }, rol);
}

async function comprobarCatalogo(selector: Locator) {
  await expect(selector.locator('option[value="OTROS"]')).toHaveCount(0);
  await expect(selector.locator('option:not([value=""])')).toHaveText(etiquetas);
}

test.beforeEach(async ({ page }) => {
  // Respuestas aisladas: estas pruebas no modifican las bases de desarrollo.
  let historico = {
    id: 2, tipoResiduo: 'OTROS', cantidad: 2.75, unidadMedida: 'kg', fecha: '2026-11-16',
    descripcion: 'Registro anterior', empresa: 'Empresa anterior', intercambioPor: 'OTROS',
    disponible: true, fechaPublicacion: '2026-11-16', estadoPublicacion: 'DISPONIBLE',
  };
  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const ruta = new URL(request.url()).pathname;
    if (ruta === '/api/auth/me') {
      const rol = await page.evaluate(() => JSON.parse(sessionStorage.getItem('reciclo.sesion')!).rol);
      await route.fulfill({ json: { username: 'prueba', nombre: 'Prueba', rol } });
    } else if (ruta === '/api/residuos/2') {
      if (request.method() === 'PUT') historico = { ...historico, ...request.postDataJSON() };
      await route.fulfill({ json: historico });
    } else if (ruta === '/api/residuos/1') {
      await route.fulfill({ json: { ...historico, id: 1, tipoResiduo: 'PLASTICO', intercambioPor: 'METAL' } });
    } else {
      await route.fulfill({ json: [] });
    }
  });
});

test('registro, consultas e inventario ofrecen cinco categorías y la cantidad permanece en kg', async ({ page }, testInfo) => {
  await sesion(page, 'ADMIN');
  await page.goto('/residuos/nuevo');
  await expect(page.getByRole('heading', { name: 'Registrar residuo' })).toBeVisible();
  await comprobarCatalogo(page.getByLabel('Tipo de residuo'));
  await expect(page.getByLabel('Cantidad')).toHaveAccessibleName('Cantidad (kg) *');
  await page.getByLabel('Habilitar intercambio').check();
  await comprobarCatalogo(page.getByLabel('Preferencia de intercambio'));
  await page.screenshot({ path: testInfo.outputPath('formulario-sin-otros.png'), fullPage: true });

  await page.goto('/consultas');
  await expect(page.getByRole('heading', { name: 'Consultas', exact: true })).toBeVisible();
  await comprobarCatalogo(page.getByLabel('Material', { exact: true }));
  await comprobarCatalogo(page.getByLabel('Preferencia de intercambio'));
  await page.screenshot({ path: testInfo.outputPath('consultas-sin-otros.png'), fullPage: true });

  await page.goto('/residuos');
  await expect(page.getByLabel('Filtrar por tipo')).toBeVisible();
  await comprobarCatalogo(page.getByLabel('Filtrar por tipo'));
  await comprobarCatalogo(page.getByLabel('Preferencia de intercambio'));
});

test('USER propone únicamente materiales del catálogo vigente', async ({ page }) => {
  await sesion(page, 'USER');
  await page.goto('/residuos/1');
  await comprobarCatalogo(page.getByLabel('Qué ofreces'));
  await expect(page.getByLabel('Cantidad ofrecida en kg')).toBeVisible();
});

test('editar un histórico exige reclasificar sin cambiar su cantidad ni unidad', async ({ page }) => {
  await sesion(page, 'ADMIN');
  await page.goto('/residuos/2/editar');
  await expect(page.getByRole('status')).toContainText('categoría retirada');
  await expect(page.getByLabel('Tipo de residuo')).toHaveValue('');
  await expect(page.getByLabel('Preferencia de intercambio')).toHaveValue('');
  await comprobarCatalogo(page.getByLabel('Tipo de residuo'));
  await comprobarCatalogo(page.getByLabel('Preferencia de intercambio'));
  await expect(page.getByLabel('Cantidad')).toHaveValue('2.75');
  await page.getByRole('button', { name: 'Guardar cambios' }).click();
  await expect(page.getByRole('alert')).toContainText('Completa los campos obligatorios');
  await expect(page).toHaveURL(/\/2\/editar$/);

  await page.getByLabel('Tipo de residuo').selectOption('VIDRIO');
  await page.getByLabel('Preferencia de intercambio').selectOption('METAL');
  const solicitud = page.waitForRequest((r) => r.method() === 'PUT' && r.url().endsWith('/api/residuos/2'));
  await page.getByRole('button', { name: 'Guardar cambios' }).click();
  expect((await solicitud).postDataJSON()).toMatchObject({
    tipoResiduo: 'VIDRIO', intercambioPor: 'METAL', cantidad: 2.75, unidadMedida: 'kg',
  });
  await expect(page.getByRole('heading', { name: 'Vidrio', exact: true })).toBeVisible();
});
