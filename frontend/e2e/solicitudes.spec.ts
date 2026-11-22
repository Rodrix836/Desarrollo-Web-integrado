import { test, expect } from '@playwright/test';
import { login, navegar, salir } from './ayudas';

test('USER propone un intercambio y ve la aceptación del ADMIN', async ({ page }) => {
  const empresa = 'Intercambio E2E ' + Date.now();
  await login(page, 'admin');
  await page.getByRole('link', { name: '+ Registrar residuo' }).click();
  await page.getByLabel('Habilitar intercambio').check();
  await page.getByRole('textbox', { name: /^Empresa/ }).fill(empresa);
  await page.getByLabel('Cantidad', { exact: false }).fill('5.5');
  await page.getByLabel('Preferencia de intercambio').selectOption('METAL');
  await page.getByRole('button', { name: 'Registrar material' }).click();
  await expect(page.getByText('Disponible', { exact: true })).toBeVisible();
  const detalle = page.url();
  await salir(page);
  await login(page, 'user');
  await page.goto(detalle);
  await page.getByLabel('Qué ofreces').selectOption('METAL');
  await page.getByLabel('Cantidad ofrecida').fill('2.5');
  await page.getByLabel('Mensaje (opcional)').fill('Tengo latas limpias para intercambiar.');
  await page.getByRole('button', { name: 'Enviar solicitud' }).click();
  await expect(page.getByRole('status')).toContainText('Solicitud enviada');
  await navegar(page, 'Mis solicitudes');
  let fila = page.getByRole('row').filter({ hasText: empresa });
  await expect(fila).toContainText('Pendiente');
  await salir(page);
  await login(page, 'admin');
  await navegar(page, 'Solicitudes recibidas');
  fila = page.getByRole('row').filter({ hasText: empresa });
  await fila.getByLabel('Respuesta opcional').fill('Podemos coordinar la entrega.');
  await fila.getByRole('button', { name: 'Aceptar', exact: true }).click();
  await expect(fila).toContainText('Aceptada');
  await salir(page);
  await login(page, 'user');
  await navegar(page, 'Mis solicitudes');
  fila = page.getByRole('row').filter({ hasText: empresa });
  await expect(fila).toContainText('Aceptada');
  await expect(fila).toContainText('Podemos coordinar la entrega.');
  await expect(fila.getByRole('button', { name: 'Cancelar solicitud' })).toHaveCount(0);
  await salir(page);
  await login(page, 'admin');
  await page.goto(detalle);
  page.once('dialog', dialogo => dialogo.accept());
  await page.getByRole('button', { name: 'Eliminar residuo' }).click();
  await expect(page).toHaveURL(/\/residuos$/);
});

test('USER puede cancelar su propuesta pendiente', async ({ page, request }) => {
  const sesion = await request.post('/api/auth/login', { data: { username: 'admin', password: 'admin123' } });
  const token = (await sesion.json()).token;
  const cabeceras = { Authorization: 'Bearer ' + token };
  const material = await request.post('/api/residuos', { headers: cabeceras, data: {
    tipoResiduo: 'METAL', cantidad: 3, unidadMedida: 'kg', descripcion: 'Cancelación E2E',
    empresa: 'Cancelación ' + Date.now(), intercambioPor: 'VIDRIO', disponible: true,
    fechaPublicacion: (await import('./ayudas')).fechaLima(),
  } });
  expect(material.status()).toBe(201);
  const residuo = await material.json();
  try {
    await login(page, 'user');
    await page.goto('/residuos/' + residuo.id);
    await page.getByRole('button', { name: 'Enviar solicitud' }).click();
    await expect(page.getByRole('status')).toContainText('Solicitud enviada');
    await navegar(page, 'Mis solicitudes');
    const fila = page.getByRole('row').filter({ hasText: residuo.empresa });
    await fila.getByRole('button', { name: 'Cancelar solicitud' }).click();
    await expect(fila).toContainText('Cancelada');
  } finally { await request.delete('/api/residuos/' + residuo.id, { headers: cabeceras }); }
});
