// Lo que se escribe al crear una reserva y sus validaciones de formato. Sirven para corregir antes de enviar: quien
// decide es el backend, que sigue devolviendo sus 400 y 409.
import type { Alojamiento } from '../../api/alojamiento'
import type { NuevaReserva } from '../../api/reservas'
import { dinero, edadEn, fechaCorta, fechaHoy } from '../../formato'

/** Los del titular: quien responde por la reserva y entra a su cuenta no se identifica con registro civil (DEC-110). */
export const TIPOS_DE_DOCUMENTO = ['CC', 'CE', 'PASAPORTE', 'TI'] as const

/** Los de un ocupante: además, el registro civil de un niño menor de 7 años, que no tiene tarjeta de identidad. */
export const TIPOS_DE_DOCUMENTO_OCUPANTE: readonly (readonly [string, string])[] = [
  ['CC', 'CC'], ['CE', 'CE'], ['PASAPORTE', 'Pasaporte'], ['TI', 'TI'], ['RC', 'Registro civil'],
]

export interface DatosEstancia {
  entrada: string
  salida: string
  ocupantes: string
}

/** DEC-98: los nombres y los apellidos aparte, como los pide el reporte a Migración (TRA). */
export interface DatosTitular {
  nombres: string
  primerApellido: string
  segundoApellido: string
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
export const TITULAR_VACIO: DatosTitular = {
  nombres: '', primerApellido: '', segundoApellido: '', tipoDocumento: 'CC', numeroDocumento: '', correo: '', telefono: '',
}

/** Largos de la API (`TitularReservaRequest`, DEC-98). */
export const MAXIMO_NOMBRES = 120
export const MAXIMO_APELLIDO = 60

/** El nombre completo del titular, armado con sus partes como lo hace el backend (`Titular.nombreCompleto`). */
export function nombreDelTitular({ nombres, primerApellido, segundoApellido }: DatosTitular): string {
  return [nombres, primerApellido, segundoApellido].map((parte) => parte.trim()).filter(Boolean).join(' ')
}

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
  // DEC-98: el TRA pide los nombres y el primer apellido; el segundo es opcional
  if (!titular.nombres.trim()) errores['titular.nombres'] = 'Escribe los nombres.'
  if (!titular.primerApellido.trim()) errores['titular.primerApellido'] = 'Escribe el primer apellido.'
  if (!titular.numeroDocumento.trim()) errores['titular.numeroDocumento'] = 'Escribe el número de documento.'
  if (!titular.correo.trim()) errores['titular.correo'] = 'Escribe el correo.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(titular.correo.trim())) {
    errores['titular.correo'] = 'Escribe un correo válido, por ejemplo nombre@dominio.com.'
  }
  if (!titular.telefono.trim()) errores['titular.telefono'] = 'Escribe el teléfono.'
  else if (!telefonoValido(titular.telefono)) errores['titular.telefono'] = MENSAJE_TELEFONO
  return errores
}

export const MENSAJE_TELEFONO = 'Escribe un teléfono de 7 a 15 dígitos; puede llevar +, espacios, guiones o paréntesis.'

/** La misma regla del backend (`Titular`): dígitos con separadores comunes, un «+» solo al inicio, de 7 a 15 dígitos. */
export function telefonoValido(telefono: string): boolean {
  const limpio = telefono.trim()
  const digitos = limpio.replace(/\D/g, '').length
  return /^[\d\s()+-]+$/.test(limpio) && limpio.lastIndexOf('+') <= 0 && digitos >= 7 && digitos <= 15
}

function validarOcupantes(grupo: DatosOcupante[], entrada: string, hoy: string): Errores {
  const errores: Errores = {}
  grupo.forEach((ocupante, indice) => {
    const campo = (nombre: string) => `ocupante.${indice}.${nombre}`
    if (!ocupante.nombre.trim()) errores[campo('nombre')] = 'Escribe el nombre.'
    if (!ocupante.fechaNacimiento) errores[campo('fechaNacimiento')] = 'Escribe la fecha de nacimiento.'
    else if (ocupante.fechaNacimiento > hoy) errores[campo('fechaNacimiento')] = 'No puede ser una fecha futura.'
    // El documento de cada huésped se pide desde la reserva: el TRA lo exige y recepción no lo persigue después (#18)
    if (!ocupante.tipoDocumento) errores[campo('tipoDocumento')] = 'Elige el tipo.'
    if (!ocupante.numeroDocumento.trim()) errores[campo('numeroDocumento')] = 'Escribe el número.'
    const noAdmitido = mensajeDocumentoPorEdad(ocupante.tipoDocumento, ocupante.fechaNacimiento, entrada)
    if (noAdmitido) errores[campo('tipoDocumento')] = noAdmitido
    const anterior = grupo.findIndex((otro, otroIndice) => otroIndice < indice && esRepetido(otro, ocupante))
    if (anterior >= 0) {
      const repetido = `Ya está en la lista (ocupante ${anterior + 1}).`
      if (tieneDocumento(ocupante) && tieneDocumento(grupo[anterior])) errores[campo('numeroDocumento')] = repetido
      else errores[campo('nombre')] = repetido
    }
  })
  return errores
}

/** DEC-97: el huésped principal debe ser mayor de edad a la fecha de entrada (`Ocupante.MAYORIA_DE_EDAD`). */
export const MAYORIA_DE_EDAD = 18

/**
 * #18: el documento que puede tener alguien según su edad al entrar. Un menor no tiene cédula de ciudadanía y un adulto
 * no se identifica con registro civil; la tarjeta de identidad, el pasaporte y la cédula de extranjería no se limitan.
 * Sin la fecha de nacimiento o la de entrada no se puede saber, y se admiten todos.
 *
 * @returns qué decirle a la persona si ese tipo no corresponde a la edad; nulo si sí
 */
export function mensajeDocumentoPorEdad(tipo: string | null | undefined, nacimiento: string | null | undefined,
  entrada: string): string | null {
  if (!tipo || !nacimiento || !entrada) return null
  const mayor = edadEn(nacimiento, entrada) >= MAYORIA_DE_EDAD
  if (mayor && tipo === 'RC') return 'El registro civil es de niños: un adulto se identifica con cédula o pasaporte.'
  if (!mayor && tipo === 'CC') {
    return 'Un menor de edad no tiene cédula de ciudadanía: usa tarjeta de identidad, registro civil, pasaporte o CE.'
  }
  return null
}

/** Los tipos de documento que se ofrecen según la edad; el ya elegido se deja aunque no corresponda, con su error. */
export function tiposPorEdad<T extends readonly [string, string]>(tipos: readonly T[], nacimiento: string | null | undefined,
  entrada: string, elegido = ''): T[] {
  return tipos.filter(([tipo]) => !tipo || tipo === elegido || !mensajeDocumentoPorEdad(tipo, nacimiento, entrada))
}

/** DEC-97: si ya se sabe que el ocupante será mayor de edad el día de entrada; sin su fecha de nacimiento, no. */
export function esAdultoAlEntrar(ocupante: DatosOcupante, entrada: string): boolean {
  return Boolean(ocupante.fechaNacimiento && entrada) && edadEn(ocupante.fechaNacimiento, entrada) >= MAYORIA_DE_EDAD
}

/**
 * DEC-97: el huésped principal, uno solo y mayor de edad el día de entrada. Vale el que eligió recepción mientras
 * siga siendo adulto; si no eligió, o el elegido dejó de serlo (se corrigió su fecha o la de entrada), el primer
 * adulto, que es el titular cuando se aloja.
 *
 * @returns la posición del principal en el grupo; nula si todavía no hay ningún adulto
 */
export function principalDelGrupo(grupo: DatosOcupante[], entrada: string, elegido: number | null): number | null {
  if (elegido !== null && grupo[elegido] && esAdultoAlEntrar(grupo[elegido], entrada)) return elegido
  const primero = grupo.findIndex((ocupante) => esAdultoAlEntrar(ocupante, entrada))
  return primero >= 0 ? primero : null
}

/**
 * DEC-97: el titular ya no tiene que alojarse, pero el grupo debe traer un mayor de edad a la fecha de entrada, que
 * será el huésped principal. Solo se revisa cuando todas las fechas están escritas: si
 * falta alguna, el error de esa fecha sale primero.
 */
function validarMayorDeEdad(grupo: DatosOcupante[], entrada: string): Errores {
  if (!entrada || grupo.some((ocupante) => !ocupante.fechaNacimiento)) return {}
  const hayMayor = grupo.some((ocupante) => edadEn(ocupante.fechaNacimiento, entrada) >= MAYORIA_DE_EDAD)
  return hayMayor
    ? {}
    : { 'grupo.mayor': `El grupo necesita un mayor de edad (${MAYORIA_DE_EDAD} años o más al entrar): será el huésped principal.` }
}

export function validarPersonas(titular: DatosTitular, grupo: DatosOcupante[], entrada: string, hoy = fechaHoy()): Errores {
  return {
    ...validarTitular(titular),
    ...validarOcupantes(grupo, entrada, hoy),
    ...validarMayorDeEdad(grupo, entrada),
  }
}

/** Con el titular alojado, su primer ocupante lleva sus datos; la fecha de nacimiento se escribe aparte. */
export function ocupanteDelTitular(titular: DatosTitular, ocupante: DatosOcupante): DatosOcupante {
  return {
    ...ocupante,
    nombre: nombreDelTitular(titular),
    tipoDocumento: titular.tipoDocumento,
    numeroDocumento: titular.numeroDocumento,
  }
}

/** El titular como lo recibe la API: las partes recortadas y el nombre completo mientras quepa en `nombre` (DEC-98). */
export function titularParaEnviar(titular: DatosTitular): NuevaReserva['titular'] {
  const completo = nombreDelTitular(titular)
  const segundo = titular.segundoApellido.trim()
  return {
    ...(completo.length <= MAXIMO_NOMBRES ? { nombre: completo } : {}),
    nombres: titular.nombres.trim(),
    primerApellido: titular.primerApellido.trim(),
    ...(segundo ? { segundoApellido: segundo } : {}),
    tipoDocumento: titular.tipoDocumento,
    numeroDocumento: titular.numeroDocumento.trim(),
    correo: titular.correo.trim(),
    telefono: titular.telefono.trim(),
  }
}

/**
 * El grupo como lo recibe la API. Si el titular se aloja, el ocupante 1 lleva sus nombres y apellidos aparte, que
 * además pueden pasar de los 120 del nombre completo. Solo el principal va marcado (DEC-97).
 */
export function ocupantesParaEnviar(grupo: DatosOcupante[], titular: DatosTitular, titularSeAloja: boolean,
  principal: number | null): NuevaReserva['ocupantes'] {
  return grupo.map((ocupante, indice) => {
    const delTitular = titularSeAloja && indice === 0 ? titularParaEnviar(titular) : null
    const nombre = delTitular
      ? {
          ...(delTitular.nombre ? { nombre: delTitular.nombre } : {}),
          nombres: delTitular.nombres,
          primerApellido: delTitular.primerApellido,
          ...(delTitular.segundoApellido ? { segundoApellido: delTitular.segundoApellido } : {}),
        }
      : { nombre: ocupante.nombre.trim() }
    return {
      ...nombre,
      fechaNacimiento: ocupante.fechaNacimiento,
      ...(ocupante.tipoDocumento ? { tipoDocumento: ocupante.tipoDocumento, numeroDocumento: ocupante.numeroDocumento.trim() } : {}),
      ...(ocupante.nacionalidad.trim() ? { nacionalidad: ocupante.nacionalidad.trim() } : {}),
      principal: indice === principal,
    }
  })
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

type ParametrosExtras = Pick<
  Alojamiento['parametros'],
  'cobroParqueadero' | 'cargoCarro' | 'cargoMoto' | 'cargoMascota'
>

/**
 * Lo que sumarán el parqueadero y las mascotas, con la misma cuenta del backend (`ParametrosAlojamiento.calcularExtras`,
 * DEC-89): cada vehículo paga por noche o una vez por estancia, y cada mascota una vez. Es solo para mostrarlo: el
 * valor que se cobra lo congela el backend al crear.
 */
export function estimarExtras(
  parametros: ParametrosExtras,
  vehiculos: { carros: number; motos: number; mascotas: number },
  noches: number,
): { parqueadero: number; mascotas: number; total: number } {
  const veces = parametros.cobroParqueadero === 'POR_NOCHE' ? noches : 1
  const parqueadero = (vehiculos.carros * parametros.cargoCarro + vehiculos.motos * parametros.cargoMoto) * veces
  const mascotas = vehiculos.mascotas * parametros.cargoMascota
  return { parqueadero, mascotas, total: parqueadero + mascotas }
}

/** Cómo se cobra un vehículo, para la pista del campo: «$ 10.000 por carro cada noche» o «sin costo». */
export function textoCobroVehiculo(cargo: number, tipo: 'carro' | 'moto', cobro: ParametrosExtras['cobroParqueadero']): string {
  if (cargo <= 0) return 'Sin costo.'
  return `${dinero(cargo)} por ${tipo} ${cobro === 'POR_NOCHE' ? 'cada noche' : 'por estancia'}.`
}

export type SalidaDelRechazo = 'apartamento' | 'fechas' | 'personas'

const TODAS_LAS_SALIDAS: readonly SalidaDelRechazo[] = ['apartamento', 'fechas', 'personas']

const SALIDAS_POR_MOTIVO: Record<string, readonly SalidaDelRechazo[]> = {
  NO_DISPONIBLE: ['apartamento', 'fechas'],
  CAPACIDAD_EXCEDIDA: ['apartamento', 'personas'],
  ESTANCIA_MINIMA: ['fechas'],
  FECHAS_INVALIDAS: ['fechas'],
  TITULAR_INVALIDO: ['personas'],
  SIN_CUPO_PARQUEADERO: ['fechas'],
  MASCOTAS_NO_ADMITIDAS: ['apartamento'],
  SIN_TARIFA: ['apartamento', 'fechas'],
}

/** Qué puede cambiar recepción para resolver un rechazo al crear, según su motivo (DEC-94); sin motivo, todas. */
export function salidasDelRechazo(motivo: string | null): readonly SalidaDelRechazo[] {
  return (motivo && SALIDAS_POR_MOTIVO[motivo]) || TODAS_LAS_SALIDAS
}
