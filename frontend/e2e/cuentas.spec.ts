import { test, expect, APIRequestContext } from '@playwright/test';
import { login, navegar, salir } from './ayudas';

async function cuentaPrueba(request: APIRequestContext) {
  const username = 'cuenta-e2e-' + Date.now();
  const alta = await request.post('/api/auth/register', { data: { username, password: 'clave123', nombre: 'Cuenta de prueba' } });
  expect(alta.status()).toBe(201);
  const admin = await request.post('/api/auth/login', { data: { username: 'admin', password: 'admin123' } });
  const headers = { Authorization: 'Bearer ' + (await admin.json()).token };
  const usuarios = await (await request.get('/api/usuarios', { headers })).json();
  const id = usuarios.find((u: { username: string }) => u.username === username).id;
  return { username, id, headers };
}

test('Mi perfil actualiza el nombre y valida el cambio de contraseña', async ({ page, request }) => {
  const cuenta = await cuentaPrueba(request);
  try {
    await login(page, cuenta.username, 'clave123');
    await page.goto('/perfil');
    await page.getByLabel('Nombre completo').fill('Nombre renovado');
    await page.getByRole('button', { name: 'Guardar nombre' }).click();
    await expect(page.getByRole('status')).toContainText('Nombre actualizado');
    await expect(page.getByRole('heading', { name: 'Nombre renovado' })).toBeVisible();
    await page.getByLabel('Contraseña actual', { exact: true }).fill('clave123');
    await page.getByLabel('Nueva contraseña', { exact: true }).fill('corta');
    await page.getByLabel('Confirmar nueva contraseña').fill('corta');
    await page.getByRole('button', { name: 'Actualizar contraseña' }).click();
    await expect(page.getByRole('alert')).toContainText('al menos 6');
    await page.getByLabel('Nueva contraseña', { exact: true }).fill('renovada123');
    await page.getByRole('button', { name: 'Actualizar contraseña' }).click();
    await expect(page.getByRole('alert')).toContainText('no coincide');
    await page.getByLabel('Confirmar nueva contraseña').fill('renovada123');
    await page.getByRole('button', { name: 'Actualizar contraseña' }).click();
    await expect(page.getByRole('status')).toContainText('Contraseña actualizada');
    await salir(page);
    await login(page, cuenta.username, 'renovada123');
    await expect(page.getByRole('heading', { name: 'Hola, Nombre renovado.' })).toBeVisible();
  } finally { await request.delete('/api/usuarios/' + cuenta.id, { headers: cuenta.headers }); }
});

test('ADMIN edita, desactiva, reactiva y elimina una cuenta', async ({ page, request }) => {
  const cuenta = await cuentaPrueba(request);
  try {
    await login(page, 'admin');
    await navegar(page, 'Usuarios');
    const propia = page.getByRole('row').filter({ has: page.getByText('admin', { exact: true }) });
    await expect(propia.getByRole('combobox')).toBeDisabled();
    await expect(propia.getByRole('switch')).toBeDisabled();
    await expect(propia.getByRole('button', { name: 'Eliminar', exact: true })).toBeDisabled();
    await expect(propia.getByRole('textbox')).toBeDisabled();
    let fila = page.getByRole('row').filter({ hasText: cuenta.username });
    await fila.getByRole('textbox').fill('Cuenta editada');
    await fila.getByRole('button', { name: 'Guardar nombre' }).click();
    await expect(page.getByRole('status')).toContainText('Nombre actualizado');
    await fila.getByRole('switch').click();
    await expect(fila.getByRole('switch')).toHaveAttribute('aria-checked', 'false');
    await salir(page);
    await page.getByLabel('Usuario', { exact: true }).fill(cuenta.username);
    await page.getByLabel('Contraseña', { exact: true }).fill('clave123');
    await page.getByRole('button', { name: 'Ingresar a ReCiclo' }).click();
    await expect(page.getByRole('alert')).toContainText('desactivada');
    await login(page, 'admin');
    await navegar(page, 'Usuarios');
    fila = page.getByRole('row').filter({ hasText: cuenta.username });
    await fila.getByRole('switch').click();
    await expect(fila.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
    page.once('dialog', dialogo => dialogo.accept());
    await fila.getByRole('button', { name: 'Eliminar', exact: true }).click();
    await expect(fila).toHaveCount(0);
  } finally { await request.delete('/api/usuarios/' + cuenta.id, { headers: cuenta.headers }); }
});

test('login explica el bloqueo por demasiados intentos', async ({ page, request }) => {
  const cuenta = await cuentaPrueba(request);
  try {
    for (let intento = 0; intento < 5; intento++) {
      await request.post('/api/auth/login', { data: { username: cuenta.username, password: 'incorrecta' } });
    }
    await page.goto('/login');
    await page.getByLabel('Usuario', { exact: true }).fill(cuenta.username);
    await page.getByLabel('Contraseña', { exact: true }).fill('incorrecta');
    await page.getByRole('button', { name: 'Ingresar a ReCiclo' }).click();
    await expect(page.getByRole('alert')).toContainText('Demasiados intentos');
  } finally { await request.delete('/api/usuarios/' + cuenta.id, { headers: cuenta.headers }); }
});
