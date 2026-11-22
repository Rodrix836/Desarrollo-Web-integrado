import { test, expect } from '@playwright/test';
import { fechaLima, login, navegar } from './ayudas';

for (const ancho of [375, 768, 1280]) {
  test(`todas las páginas caben a ${ancho} px`, async ({ page, request }) => {
    test.setTimeout(90_000);
    await page.setViewportSize({ width: ancho, height: 900 });
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Inicia sesión' })).toBeVisible();
    async function comprobarAncho(ruta: string) {
      const medidas = await page.evaluate(() => ({ contenido: document.documentElement.scrollWidth, ventana: innerWidth }));
      expect(medidas.contenido, ruta).toBeLessThanOrEqual(medidas.ventana);
      // Un panel tampoco debe esconder una tabla que se desborda internamente.
      const tablas = await page.locator('.table-wrap').evaluateAll(paneles => paneles.every(p => p.scrollWidth <= p.clientWidth + 1));
      expect(tablas, 'Tabla completa en ' + ruta).toBeTruthy();
    }
    await comprobarAncho('/login');
    const sesion = await request.post('/api/auth/login', { data: { username: 'admin', password: 'admin123' } });
    const cabeceras = { Authorization: 'Bearer ' + (await sesion.json()).token };
    const alta = await request.post('/api/residuos', { headers: cabeceras, data: {
      tipoResiduo: 'PAPEL_CARTON', cantidad: 12.25, unidadMedida: 'kg', descripcion: 'Material responsivo ' + 'reciclable'.repeat(35),
      empresa: 'Cooperativa de reciclaje ' + 'Circular'.repeat(8), intercambioPor: 'METAL', disponible: true, fechaPublicacion: fechaLima(),
    } });
    expect(alta.status()).toBe(201);
    const residuo = await alta.json();
    const cuenta = await request.post('/api/auth/register', { data: { username: 'responsivo-' + Date.now(), nombre: 'Cuenta responsiva', password: 'clave123' } });
    expect(cuenta.status()).toBe(201);
    const cuentaDatos = await cuenta.json();
    const propuesta = await request.post('/api/solicitudes', { headers: { Authorization: 'Bearer ' + cuentaDatos.token }, data: { residuoId: residuo.id, mensaje: 'Propuesta'.repeat(55), ofrece: 'METAL', cantidadOfrecida: 2 } });
    expect(propuesta.status()).toBe(201);
    try {
      await login(page, 'admin');
      for (const [ruta, titulo] of [
        ['/', 'Hola,'], ['/residuos', 'Residuos'], ['/residuos/' + residuo.id, 'Papel y cartón'],
        ['/residuos/nuevo', 'Registrar residuo'], ['/residuos/' + residuo.id + '/editar', 'Editar residuo'],
        ['/consultas', 'Consultas'], ['/solicitudes', 'Solicitudes recibidas'], ['/usuarios', 'Usuarios'], ['/perfil', 'Mi perfil'],
      ]) {
        await page.goto(ruta);
        await expect(page.getByRole('heading', { name: new RegExp(titulo), level: 1 })).toBeVisible();
        await expect(page.getByRole('status').filter({ hasText: /Cargando|Consultando/ })).toHaveCount(0);
        await expect(page.getByRole('alert')).toHaveCount(0);
        await comprobarAncho(ruta);
      }
      await login(page, cuentaDatos.username, 'clave123');
      for (const ruta of ['/', '/residuos', '/residuos/' + residuo.id, '/consultas', '/solicitudes', '/perfil']) {
        await page.goto(ruta);
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
        await expect(page.getByRole('status').filter({ hasText: /Cargando|Consultando/ })).toHaveCount(0);
        await comprobarAncho('USER ' + ruta);
      }
    } finally {
      await request.delete('/api/residuos/' + residuo.id, { headers: cabeceras });
      const usuarios = await (await request.get('/api/usuarios', { headers: cabeceras })).json();
      const cuentaNueva = usuarios.find((u: { username: string }) => u.username === cuentaDatos.username);
      if (cuentaNueva) await request.delete('/api/usuarios/' + cuentaNueva.id, { headers: cabeceras });
    }
  });
}

test('el menú móvil abre, cierra, muestra la cuenta y permite navegar', async ({ page, isMobile }) => {
  if (!isMobile) await page.setViewportSize({ width: 375, height: 900 });
  await login(page, 'admin');
  const boton = page.getByRole('button', { name: 'Menú principal', exact: true });
  await expect(boton).toHaveAttribute('aria-controls', 'menu-principal');
  await expect(boton).toHaveAttribute('aria-expanded', 'false');
  await boton.click();
  await expect(boton).toHaveAttribute('aria-expanded', 'true');
  await expect(page.locator('#menu-principal .account')).toBeVisible();
  await expect(page.getByRole('button', { name: /Cerrar sesión/ })).toBeVisible();
  await boton.click();
  await expect(boton).toHaveAttribute('aria-expanded', 'false');
  await boton.click();
  await page.keyboard.press('Escape');
  await expect(boton).toHaveAttribute('aria-expanded', 'false');
  await expect(boton).toBeFocused();
  await navegar(page, 'Solicitudes recibidas');
  await expect(page).toHaveURL(/\/solicitudes$/);
  await expect(boton).toHaveAttribute('aria-expanded', 'false');
  await navegar(page, 'Usuarios');
  await expect(page.getByRole('heading', { name: 'Usuarios' })).toBeVisible();
  await expect(boton).toHaveAttribute('aria-expanded', 'false');
});
