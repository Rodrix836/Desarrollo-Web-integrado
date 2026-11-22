import { expect, Page } from '@playwright/test';

export async function abrirMenu(page: Page) {
  await expect(page.locator('.page h1')).toBeVisible();
  const boton = page.getByRole('button', { name: 'Menú principal', exact: true });
  if (await boton.isVisible() && await boton.getAttribute('aria-expanded') === 'false') await boton.click();
}
export async function navegar(page: Page, nombre: string) {
  await abrirMenu(page);
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('link', { name: nombre, exact: true }).click();
}
export async function salir(page: Page) {
  await abrirMenu(page);
  await page.getByRole('button', { name: /Cerrar sesión/ }).click();
}
export async function login(page: Page, username: string, password?: string) {
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill(username);
  await page.getByLabel('Contraseña', { exact: true }).fill(password ?? (username === 'admin' ? 'admin123' : 'user123'));
  await page.getByRole('button', { name: 'Ingresar a ReCiclo' }).click();
  await expect(page.getByRole('heading', { name: /Hola,/ })).toBeVisible();
}
export function fechaLima(): string {
  const partes = new Intl.DateTimeFormat('en', { timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
  const parte = (tipo: string) => partes.find(p => p.type === tipo)!.value;
  return `${parte('year')}-${parte('month')}-${parte('day')}`;
}
