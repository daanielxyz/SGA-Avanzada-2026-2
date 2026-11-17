// El tema del PMS (DEC-139): noche, el de siempre, o día. Se guarda en este navegador, no en la cuenta:
// guardarlo en la cuenta pediría un cambio del núcleo del backend y no hace falta para el MVP. `index.html` lo aplica
// antes de pintar, con la misma clave, para que la pantalla no parpadee al abrir.

export type Tema = 'noche' | 'dia'

export const CLAVE_TEMA = 'sga.pms.tema'

/** El color de la barra del navegador en el celular, el mismo de la barra de arriba de cada tema. */
const COLOR_BARRA: Record<Tema, string> = { noche: '#06151e', dia: '#b8a078' }

/** El tema guardado. Sin nada guardado, con un valor raro o sin acceso al almacenamiento, noche. */
export function leerTema(): Tema {
  try {
    return localStorage.getItem(CLAVE_TEMA) === 'dia' ? 'dia' : 'noche'
  } catch {
    return 'noche'
  }
}

/** Pone el tema en la página: los colores salen de `:root[data-tema='dia']` en `src/index.css`. */
export function aplicarTema(tema: Tema): void {
  if (tema === 'dia') document.documentElement.dataset.tema = 'dia'
  else delete document.documentElement.dataset.tema
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', COLOR_BARRA[tema])
  document.querySelector('meta[name="color-scheme"]')?.setAttribute('content', tema === 'dia' ? 'light' : 'dark')
}

/**
 * Cambia el tema y lo recuerda. Si el navegador no deja guardar (modo privado, almacenamiento bloqueado), el tema
 * cambia igual por esta visita.
 */
export function elegirTema(tema: Tema): void {
  aplicarTema(tema)
  try {
    localStorage.setItem(CLAVE_TEMA, tema)
  } catch {
    // Sin almacenamiento no hay nada que recordar
  }
}
