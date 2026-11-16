// Lo que la pantalla de Titulares calcula sin hablar con la API (pms/DISENO.md, «Sprint 5: titulares»).
import type { DatosTitular, Titular } from '../api/titulares'
import { fechaYHora } from '../reserva/llegada/llegada'

export const MAXIMO_BUSQUEDA = 100
export const MAXIMO_NOMBRES = 120
export const MAXIMO_APELLIDO = 60
export const MAXIMO_CORREO = 150
export const MAXIMO_TELEFONO = 30

/** El documento no se edita en Titulares: se corrige desde una reserva (DEC-120). */
export const PISTA_DOCUMENTO = 'Si está mal escrito, corrígelo desde una de sus reservas con «Corregir documento».'

const NOMBRE_DEL_TIPO: Record<string, string> = { PASAPORTE: 'Pasaporte' }
const MILES = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 })

/**
 * «CC 1.094.000», «Pasaporte AB123456». La API guarda el número sin puntos: se separan los miles solo si es puro
 * número, porque un pasaporte lleva letras.
 */
export function textoDocumento(tipo: string, numero: string): string {
  const legible = /^\d+$/.test(numero) && tipo !== 'PASAPORTE' ? MILES.format(Number(numero)) : numero
  return `${NOMBRE_DEL_TIPO[tipo] ?? tipo} ${legible}`
}

/** «Identidad validada · 14 nov 2026, 3:24 p. m., por María Fernanda Ospina»; sin nombre si esa cuenta ya no existe. */
export function textoValidacion(titular: Pick<Titular, 'validadoEn' | 'validadoPorNombre'>): string {
  if (!titular.validadoEn) return 'Identidad validada'
  const quien = titular.validadoPorNombre ? `, por ${titular.validadoPorNombre}` : ''
  return `Identidad validada · ${fechaYHora(titular.validadoEn)}${quien}`
}

export interface FormTitular {
  nombres: string
  primerApellido: string
  segundoApellido: string
  correo: string
  telefono: string
}

export type CampoTitular = keyof FormTitular | 'contacto'

/**
 * El formulario a partir del titular. Uno de antes de DEC-98 trae el nombre entero en `nombre`: se deja en Nombres
 * para que recepción separe los apellidos.
 */
export function formDe(titular: Titular): FormTitular {
  const separado = Boolean(titular.nombres)
  return {
    nombres: separado ? titular.nombres ?? '' : titular.nombre,
    primerApellido: separado ? titular.primerApellido ?? '' : '',
    segundoApellido: separado ? titular.segundoApellido ?? '' : '',
    correo: titular.correo ?? '',
    telefono: titular.telefono ?? '',
  }
}

// Lo mínimo para que el correo sirva: algo, una arroba, un dominio con punto. El backend revisa lo demás
const FORMA_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * Las reglas del backend dichas antes de enviar: los nombres y al menos un contacto (DEC-49). Los apellidos no son
 * obligatorios: un titular de antes los tiene dentro de los nombres.
 */
export function validarTitular(form: FormTitular): Partial<Record<CampoTitular, string>> {
  const errores: Partial<Record<CampoTitular, string>> = {}
  if (!form.nombres.trim()) errores.nombres = 'Escribe los nombres.'
  if (form.correo.trim() && !FORMA_CORREO.test(form.correo.trim())) {
    errores.correo = 'Escribe un correo válido, por ejemplo nombre@dominio.co.'
  }
  if (!form.correo.trim() && !form.telefono.trim()) errores.contacto = 'Deja al menos un correo o un teléfono.'
  return errores
}

export function cuerpoTitular(form: FormTitular): DatosTitular {
  const opcional = (texto: string) => texto.trim() || null
  return {
    nombres: form.nombres.trim(),
    primerApellido: opcional(form.primerApellido),
    segundoApellido: opcional(form.segundoApellido),
    correo: opcional(form.correo),
    telefono: opcional(form.telefono),
  }
}

/** El campo de un detalle de un 400 («correo: no tiene formato de correo»), si es de este formulario. */
export function campoDelDetalle(detalle: string): keyof FormTitular | null {
  const campo = detalle.split(':')[0].trim()
  return (['nombres', 'primerApellido', 'segundoApellido', 'correo', 'telefono'] as const)
    .find((c) => c === campo) ?? null
}
