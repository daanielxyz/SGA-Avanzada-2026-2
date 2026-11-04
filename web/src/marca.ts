// La marca de cada hotel sobre el mismo esqueleto: llega por la API y reemplaza las variables de index.css (DEC-83 ·
// BL-05). Sin marca, o con campos vacíos, se quedan los valores neutros de index.css.
import type { Marca } from './api/alojamiento'

const FUENTE_RESPALDO = { titulos: 'Georgia, serif', texto: 'system-ui, sans-serif' }

function variables({ colores, fuentes, radio }: Marca): Record<string, string | null | undefined> {
  return {
    '--fondo': colores?.fondo,
    '--superficie': colores?.superficie,
    '--texto': colores?.texto,
    '--texto-suave': colores?.textoSuave,
    '--borde': colores?.borde,
    '--borde-campo': colores?.bordeCampo,
    '--acento': colores?.acento,
    '--texto-sobre-acento': colores?.textoSobreAcento,
    '--peligro': colores?.peligro,
    '--exito': colores?.exito,
    '--fuente-titulos': familia(fuentes?.titulos, FUENTE_RESPALDO.titulos),
    '--fuente-texto': familia(fuentes?.texto, FUENTE_RESPALDO.texto),
    '--radio-pequeno': radio?.pequeno,
    '--radio-mediano': radio?.mediano,
    '--radio-grande': radio?.grande,
  }
}

function familia(nombre: string | null | undefined, respaldo: string) {
  return nombre ? `"${limpiar(nombre)}", ${respaldo}` : null
}

// El nombre de la fuente lo escribe el hotel: sin comillas ni barras no puede salirse del valor de CSS
function limpiar(nombre: string) {
  return nombre.replace(/["\\]/g, '')
}

// Una sola hoja de Google Fonts con las dos familias; cada hotel carga solo las suyas
function cargarFuentes({ fuentes }: Marca) {
  const familias = [fuentes?.titulos, fuentes?.texto].filter((f): f is string => Boolean(f))
  document.getElementById('fuentes-marca')?.remove()
  if (familias.length === 0) {
    return
  }
  const consulta = [...new Set(familias)]
    .map((f) => `family=${encodeURIComponent(limpiar(f)).replace(/%20/g, '+')}:wght@400;500;600;700`)
    .join('&')
  const hoja = document.createElement('link')
  hoja.id = 'fuentes-marca'
  hoja.rel = 'stylesheet'
  hoja.href = `https://fonts.googleapis.com/css2?${consulta}&display=swap`
  document.head.append(hoja)
}

/** Pone la marca del hotel sobre el esqueleto; con `null`, vuelve a los valores neutros. */
export function aplicarMarca(marca: Marca | null) {
  const raiz = document.documentElement.style
  for (const [variable, valor] of Object.entries(variables(marca ?? {}))) {
    if (valor) {
      raiz.setProperty(variable, valor)
    } else {
      raiz.removeProperty(variable)
    }
  }
  cargarFuentes(marca ?? {})
  // La barra del navegador en el celular, del color de la cabecera
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', marca?.colores?.superficie ?? '#ffffff')
}
