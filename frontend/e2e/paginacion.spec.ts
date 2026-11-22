import { test, expect } from '@playwright/test';
import { login } from './ayudas';

test('pagina diez registros filtrados y vuelve a la primera página al filtrar', async ({ page, request }) => {
  const sesion = await request.post('/api/auth/login', { data: { username: 'admin', password: 'admin123' } });
  const headers = { Authorization: 'Bearer ' + (await sesion.json()).token };
  const prefijo = 'Paginación E2E ' + Date.now();
  const ids: number[] = [];
  try {
    for (let n = 0; n < 11; n++) {
      const respuesta = await request.post('/api/residuos', { headers, data: { tipoResiduo: 'VIDRIO', cantidad: 1,
        unidadMedida: 'kg', descripcion: prefijo + ' registro ' + n, disponible: false, empresa: '', intercambioPor: '', fechaPublicacion: null } });
      expect(respuesta.status()).toBe(201);
      ids.push((await respuesta.json()).id);
    }
    await login(page, 'admin');
    await page.goto('/residuos');
    await page.getByLabel('Buscar material o empresa').fill(prefijo);
    await expect(page.getByRole('status')).toHaveText('Página 1 de 2');
    await expect(page.locator('tbody tr')).toHaveCount(10);
    await page.getByRole('button', { name: 'Siguiente' }).click();
    await expect(page.getByRole('status')).toHaveText('Página 2 de 2');
    await expect(page.locator('tbody tr')).toHaveCount(1);
    await page.getByLabel('Filtrar por tipo').selectOption('VIDRIO');
    await expect(page.getByRole('status')).toHaveText('Página 1 de 2');
    await expect(page.getByRole('button', { name: 'Anterior', exact: true })).toBeDisabled();
    await page.getByLabel('Buscar material o empresa').fill('Sin coincidencias ' + prefijo);
    await expect(page.getByRole('heading', { name: 'No encontramos coincidencias' })).toBeVisible();
  } finally {
    for (const id of ids) await request.delete('/api/residuos/' + id, { headers });
  }
});
