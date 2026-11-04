// Lo que se escribe al crear una reserva y sus validaciones de formato. Sirven para corregir antes de enviar: quien
// decide es el backend, que sigue devolviendo sus 400 y 409.
import { edadEn, fechaCorta, fechaHoy } from '../../formato'

export const TIPOS_DE_DOCUMENTO = ['CC', 'CE', 'PASAPORTE', 'TI'] as const

export interface DatosEstancia {
  entrada: string
  salida: string
  ocupantes: string
}

export interface DatosTitular {
  nombre: string
  tipoDocumento: string
  numeroDocumento: string
  correo: string
  telefono: string
}

export interface DatosOcupante {
  nombre: string
  fechaNacimiento: string
  tipoDocumento: string
  numeroDocumento: string
  nacionalidad: string
}

/** Por campo: `entrada`, `titular.correo`, `ocupante.2.nombre`… */
export type Errores = Record<string, string>

export const ESTANCIA_VACIA: DatosEstancia = { entrada: '', salida: '', ocupantes: '1' }
export const TITULAR_VACIO: DatosTitular = { nombre: '', tipoDocumento: 'CC', numeroDocumento: '', correo: '', telefono: '' }
export const OCUPANTE_VACIO: DatosOcupante = {
  nombre: '', fechaNacimiento: '', tipoDocumento: '', numeroDocumento: '', nacionalidad: '',
}

// EST-01 · RN-02: la entrada no es pasada, la salida es posterior y hay al menos un ocupante
export function validarEstancia({ entrada, salida, ocupantes }: DatosEstancia, hoy = fechaHoy()): Errores {
  const errores: Errores = {}
  if (!entrada) errores.entrada = 'Escribe la fecha de entrada.'
  else if (entrada < hoy) errores.entrada = `La entrada no puede ser anterior a hoy (${fechaCorta(hoy)}).`
  if (!salida) errores.salida = 'Escribe la fecha de salida.'
  else if (entrada && salida <= entrada) errores.salida = 'La salida debe ser posterior a la entrada.'
  if (!(Number(ocupantes) >= 1)) errores.ocupantes = 'Debe haber al menos un ocupante.'
  return errores
}

/** Al corregir un campo, su error desaparece sin esperar a volver a enviar. */
export function sinError(errores: Errores, campo: string): Errores {
  const { [campo]: _quitado, ...resto } = errores
  return resto
}

/** Ajusta el grupo al número de ocupantes sin perder lo que ya se escribió. */
export function ajustarGrupo(grupo: DatosOcupante[], cantidad: number): DatosOcupante[] {
  return Array.from({ length: cantidad }, (_, indice) => grupo[indice] ?? OCUPANTE_VACIO)
}

function mismoDocumento(a: { tipoDocumento: string; numeroDocumento: string }, b: typeof a): boolean {
  return a.tipoDocumento === b.tipoDocumento && a.numeroDocumento.trim() === b.numeroDocumento.trim()
}

function tieneDocumento({ tipoDocumento, numeroDocumento }: DatosOcupante): boolean {
  return Boolean(tipoDocumento && numeroDocumento.trim())
}

// OCU-05: es la misma persona por documento si ambos lo tienen; si no, por nombre y fecha de nacimiento
function esRepetido(a: DatosOcupante, b: DatosOcupante): boolean {
  if (tieneDocumento(a) && tieneDocumento(b)) return mismoDocumento(a, b)
  return Boolean(a.nombre.trim())
    && a.nombre.trim().toLowerCase() === b.nombre.trim().toLowerCase()
    && a.fechaNacimiento === b.fechaNacimiento
}

function validarTitular(titular: DatosTitular): Errores {
  const errores: Errores = {}
  if (!titular.nombre.trim()) errores['titular.nombre'] = 'Escribe el nombre.'
  if (!titular.numeroDocumento.trim()) errores['titular.numeroDocumento'] = 'Escribe el número de documento.'
  if (!titular.correo.trim()) errores['titular.correo'] = 'Escribe el correo.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(titular.correo.trim())) {
    errores['titular.correo'] = 'Escribe un correo válido, por ejemplo nombre@dominio.com.'
  }
  if (!titular.telefono.trim()) errores['titular.telefono'] = 'Escribe el teléfono.'
  return errores
}

function validarOcupantes(grupo: DatosOcupante[], hoy: string): Errores {
  const errores: Errores = {}
  grupo.forEach((ocupante, indice) => {
    const campo = (nombre: string) => `ocupante.${indice}.${nombre}`
    if (!ocupante.nombre.trim()) errores[campo('nombre')] = 'Escribe el nombre.'
    if (!ocupante.fechaNacimiento) errores[campo('fechaNacimiento')] = 'Escribe la fecha de nacimiento.'
    else if (ocupante.fechaNacimiento > hoy) errores[campo('fechaNacimiento')] = 'No puede ser una fecha futura.'
    if (ocupante.numeroDocumento.trim() && !ocupante.tipoDocumento) errores[campo('tipoDocumento')] = 'Elige el tipo.'
    if (ocupante.tipoDocumento && !ocupante.numeroDocumento.trim()) errores[campo('numeroDocumento')] = 'Escribe el número.'
    const anterior = grupo.findIndex((otro, otroIndice) => otroIndice < indice && esRepetido(otro, ocupante))
    if (anterior >= 0) {
      const repetido = `Ya está en la lista (ocupante ${anterior + 1}).`
      if (tieneDocumento(ocupante) && tieneDocumento(grupo[anterior])) errores[campo('numeroDocumento')] = repetido
      else errores[campo('nombre')] = repetido
    }
  })
  return errores
}

/**
 * TIT-01 · OCU-02: el titular se reconoce entre los ocupantes por su documento y debe ser facturable a la fecha de
 * entrada. Si está pero no alcanza la edad, el error sale junto a él.
 */
function validarTitularEntreOcupantes(titular: DatosTitular, grupo: DatosOcupante[], entrada: string, umbral: number): Errores {
  if (!titular.numeroDocumento.trim()) return {}
  const indice = grupo.findIndex((ocupante) => tieneDocumento(ocupante) && mismoDocumento(ocupante, titular))
  if (indice < 0) {
    return { 'titular.alojamiento': 'El titular debe estar entre los ocupantes, con el mismo documento.' }
  }
  const { fechaNacimiento } = grupo[indice]
  if (!fechaNacimiento) return {}
  const edad = edadEn(fechaNacimiento, entrada)
  if (edad >= umbral) return {}
  return {
    [`ocupante.${indice}.fechaNacimiento`]:
      `Tendrá ${edad} ${edad === 1 ? 'año' : 'años'} al entrar. El titular debe ser un ocupante que paga: ${umbral} años o más.`,
  }
}

export function validarPersonas(titular: DatosTitular, grupo: DatosOcupante[], entrada: string, umbral: number, hoy = fechaHoy()): Errores {
  return {
    ...validarTitular(titular),
    ...validarTitularEntreOcupantes(titular, grupo, entrada, umbral),
    ...validarOcupantes(grupo, hoy),
  }
}

/** Con el titular alojado, su primer ocupante lleva sus datos; la fecha de nacimiento se escribe aparte. */
export function ocupanteDelTitular(titular: DatosTitular, ocupante: DatosOcupante): DatosOcupante {
  return {
    ...ocupante,
    nombre: titular.nombre,
    tipoDocumento: titular.tipoDocumento,
    numeroDocumento: titular.numeroDocumento,
  }
}

/** Qué le falta a la reserva para poder confirmarse después (RN-09 · RES-15). */
export function mensajeDeCreada(tieneHora: boolean, exigeAnticipo: boolean): string {
  const falta = [
    ...(tieneHora ? [] : ['la hora de llegada']),
    ...(exigeAnticipo ? ['cubrir el anticipo'] : []),
  ]
  if (falta.length === 0) return 'Queda pendiente y ya se puede confirmar.'
  return `Queda pendiente. Para confirmarla necesitas ${falta.join(' y ')}.`
}
