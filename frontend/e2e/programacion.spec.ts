import { test, expect, Page } from '@playwright/test';

function fechaLima(dias = 0): string {
  const partes = new Intl.DateTimeFormat('en', {
    timeZone: 'America/Lima',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const valor = (tipo: string) => Number(partes.find((p) => p.type === tipo)!.value);
  return new Date(Date.UTC(valor('year'), valor('month') - 1, valor('day') + dias))
    .toISOString()
    .slice(0, 10);
}
function fechaVisible(fecha: string): string {
  const [anio, mes, dia] = fecha.split('-');
  return `${dia}/${mes}/${anio}`;
}
async function login(page: Page, username: string) {
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill(username);
  await page
    .getByLabel('Contraseña', { exact: true })
    .fill(username === 'admin' ? 'admin123' : 'user123');
  await page.getByRole('button', { name: 'Ingresar a ReCiclo' }).click();
  await expect(page.getByRole('heading', { name: /Hola,/ })).toBeVisible();
}

test('la sección está condicionada al check y admite guardar sin publicación', async ({ page }) => {
  await login(page, 'admin');
  await page.getByRole('link', { name: '+ Registrar residuo' }).click();
  await expect(page.getByRole('textbox', { name: /^Empresa/ })).toBeDisabled();
  await expect(page.getByLabel('Preferencia de intercambio')).toBeDisabled();
  await expect(page.getByLabel('Fecha de publicación')).toBeDisabled();
  await expect(page.getByLabel('Cantidad')).toHaveAccessibleName('Cantidad (kg) *');
  await page.getByLabel('Descripción').fill('Inventario sin publicar');
  await page.getByRole('button', { name: 'Registrar material' }).click();
  await expect(page.getByText('No publicado', { exact: true })).toBeVisible();
  page.once('dialog', (dialog) => dialog.accept());
  await page.getByRole('button', { name: 'Eliminar residuo' }).click();
  await expect(page).toHaveURL(/\/residuos$/);
});

test('programar, filtrar, reprogramar, cancelar y publicar conserva la visibilidad USER', async ({
  page,
  browser,
}) => {
  test.setTimeout(90_000);
  const empresa = 'Programación E2E ' + Date.now();
  const manana = fechaLima(1);
  const despues = fechaLima(2);
  await page.setViewportSize({ width: 1440, height: 1000 });
  await login(page, 'admin');
  await page.getByRole('link', { name: '+ Registrar residuo' }).click();
  await page.getByLabel('Habilitar intercambio').check();
  await page.getByRole('textbox', { name: /^Empresa/ }).fill(empresa);
  await page.getByLabel('Preferencia de intercambio').selectOption('METAL');
  await page.getByLabel('Fecha de publicación').fill(manana);
  await page.getByLabel('Cantidad').fill('7.2575');
  await page.screenshot({ path: 'test-results/programacion-formulario.png', fullPage: true });
  await page.getByRole('button', { name: 'Registrar material' }).click();
  await expect(page.getByText('Programado', { exact: true })).toBeVisible();
  const detalle = page.url();

  await page.getByRole('link', { name: 'Residuos', exact: true }).click();
  await page.getByLabel('Estado de publicación').selectOption('PROGRAMADO');
  await page.getByLabel('Preferencia de intercambio').selectOption('METAL');
  await page.getByLabel('Buscar material o empresa').fill(empresa);
  await expect(page.getByRole('row').filter({ hasText: empresa })).toHaveCount(1);
  await page.getByRole('button', { name: 'Actualizar', exact: true }).click();
  await expect(page.getByLabel('Estado de publicación')).toHaveValue('PROGRAMADO');
  await expect(page.getByLabel('Preferencia de intercambio')).toHaveValue('METAL');

  const context = await browser.newContext({
    baseURL: process.env['RECICLO_E2E_URL'] ?? 'http://localhost:4200',
  });
  const user = await context.newPage();
  await login(user, 'user');
  let aviso = user.getByRole('article').filter({ hasText: empresa });
  await expect(aviso).toContainText(fechaVisible(manana));
  await user.getByRole('link', { name: 'Residuos', exact: true }).click();
  await expect(user.getByRole('row').filter({ hasText: empresa })).toHaveCount(0);
  await user.goto(detalle);
  await expect(user.getByRole('alert')).toContainText('No existe');

  await page.goto(detalle);
  await page.getByRole('link', { name: 'Editar residuo' }).click();
  await page.getByLabel('Fecha de publicación').fill(despues);
  await page.getByRole('button', { name: 'Guardar cambios' }).click();
  await expect(page).toHaveURL(detalle);
  await user.goto('/');
  aviso = user.getByRole('article').filter({ hasText: empresa });
  await expect(aviso).toContainText(fechaVisible(despues));
  await expect(aviso).toHaveCount(1);
  await user.setViewportSize({ width: 390, height: 844 });
  await user.screenshot({ path: 'test-results/programacion-avisos-movil.png', fullPage: true });
  expect(
    await user.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
  ).toBeTruthy();

  await page.getByRole('link', { name: 'Editar residuo' }).click();
  await page.getByLabel('Habilitar intercambio').uncheck();
  await expect(page.getByLabel('Fecha de publicación')).toHaveValue('');
  await expect(page.getByLabel('Preferencia de intercambio')).toHaveValue('');
  await page.getByRole('button', { name: 'Guardar cambios' }).click();
  await expect(page.getByText('No publicado', { exact: true })).toBeVisible();
  await user.getByRole('button', { name: 'Actualizar', exact: true }).click();
  await expect(user.getByRole('article').filter({ hasText: empresa })).toHaveCount(0);

  await page.getByRole('link', { name: 'Editar residuo' }).click();
  await page.getByLabel('Habilitar intercambio').check();
  await page.getByLabel('Preferencia de intercambio').selectOption('PAPEL_CARTON');
  await page.getByLabel('Fecha de publicación').fill(fechaLima());
  await page.getByRole('button', { name: 'Guardar cambios' }).click();
  await expect(page.getByText('Disponible', { exact: true })).toBeVisible();
  await user.goto('/residuos');
  await expect(user.getByRole('row').filter({ hasText: empresa })).toHaveCount(1);
  await expect(user.getByRole('article').filter({ hasText: empresa })).toContainText('desde hoy');
  await user.getByRole('link', { name: 'Consultas', exact: true }).click();
  await user.getByLabel('Empresa', { exact: true }).fill(empresa);
  await user.getByLabel('Preferencia de intercambio').selectOption('PAPEL_CARTON');
  await user.getByRole('button', { name: 'Buscar materiales' }).click();
  await expect(user.getByRole('link').filter({ hasText: empresa })).toHaveCount(1);

  page.once('dialog', (dialog) => dialog.accept());
  await page.getByRole('button', { name: 'Eliminar residuo' }).click();
  await expect(page).toHaveURL(/\/residuos$/);
  await user.goto('/');
  await expect(user.getByRole('article').filter({ hasText: empresa })).toHaveCount(0);
  await context.close();
});
