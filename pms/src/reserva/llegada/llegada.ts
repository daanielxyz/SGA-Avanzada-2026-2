// Cuentas y textos del registro de llegada (CU-21). Sirven para que recepción vea qué falta antes de intentar: quien
// decide sigue siendo el backend, que responde 409 si algo cambió mientras tanto.
import type { Folio } from '../../api/folio'
import type { ModificacionReserva, Ocupante, Reserva } from '../../api/reservas'
import type { Titular } from '../../api/titulares'
import { edadEn, fechaCorta, fechaSinAnio, hora } from '../../formato'

/** «12 nov 2026, 2:45 p. m.» a partir de AAAA-MM-DDTHH:mm. */
export function fechaYHora(valor: string): string {
  return `${fechaCorta(valor)}, ${hora(valor.slice(11, 16))}`
}

/** La reserva completa como queda, con el mismo grupo: así cada huésped conserva sus datos del TRA (BL-07). */
export function modificacionCon(reserva: Reserva, vehiculos: { carros: number; motos: number; mascotas: number }): ModificacionReserva {
  return {
    apartamento: reserva.apartamento,
    entrada: reserva.entrada,
    salida: reserva.salida,
    ocupantes: reserva.ocupantes.map((ocupante) => ({
      ...(ocupante.nombres
        ? {
            nombres: ocupante.nombres,
            ...(ocupante.primerApellido ? { primerApellido: ocupante.primerApellido } : {}),
            ...(ocupante.segundoApellido ? { segundoApellido: ocupante.segundoApellido } : {}),
          }
        : { nombre: ocupante.nombre }),
      fechaNacimiento: ocupante.fechaNacimiento ?? '',
      ...(ocupante.tipoDocumento && ocupante.numeroDocumento
        ? { tipoDocumento: ocupante.tipoDocumento, numeroDocumento: ocupante.numeroDocumento }
        : {}),
      ...(ocupante.nacionalidad ? { nacionalidad: ocupante.nacionalidad } : {}),
      ...(ocupante.principal ? { principal: true } : {}),
    })),
    ...vehiculos,
  }
}

/** DEC-97: el huésped principal debe ser mayor de edad a la fecha de entrada (`Ocupante.MAYORIA_DE_EDAD`). */
export const MAYORIA_DE_EDAD = 18

/** Límite de `motivoSinPago` en `RegistrarLlegadaRequest` y del motivo de la corrección. */
export const MOTIVO_MAXIMO = 500

export { NOMBRE_ESTADO_OPERATIVO } from '../../componentes/estadosApartamento'

export const TIPOS_DOCUMENTO: readonly (readonly [string, string])[] = [
  ['CC', 'Cédula de ciudadanía'],
  ['CE', 'Cédula de extranjería'],
  ['PASAPORTE', 'Pasaporte'],
  ['TI', 'Tarjeta de identidad'],
]

export const GENEROS: readonly (readonly [string, string])[] = [
  ['FEMENINO', 'Femenino'],
  ['MASCULINO', 'Masculino'],
  ['OTRO', 'Otro'],
]

export const MOTIVOS_VIAJE: readonly (readonly [string, string])[] = [
  ['TURISMO_VACACIONES', 'Turismo y vacaciones'],
  ['EVENTOS', 'Eventos'],
  ['ESTUDIOS', 'Estudios'],
  ['TRATAMIENTO_MEDICO', 'Tratamiento médico'],
  ['ASUNTOS_JURIDICOS', 'Asuntos jurídicos'],
  ['NEGOCIOS', 'Negocios'],
  ['TRANSITO', 'Tránsito'],
  ['COMPRAS', 'Compras'],
  ['RELIGION', 'Religión'],
  ['VISITA_FAMILIARES_AMIGOS', 'Visita a familiares o amigos'],
]

/** «CC 1094123456» o, si no tiene, nada. El tipo se muestra con su sigla, como en el documento. */
export function documentoDe({ tipoDocumento, numeroDocumento }: { tipoDocumento: string | null; numeroDocumento: string | null }): string | null {
  if (!numeroDocumento) return null
  const tipo = tipoDocumento === 'PASAPORTE' ? 'Pasaporte' : tipoDocumento
  return `${tipo ?? ''} ${numeroDocumento}`.trim()
}

/** Lo que falta por cobrar de la estancia y sus extras (BL-22). Los consumos no cuentan: se cobran a la salida. */
export function faltaPorCobrar(reserva: Reserva, folio: Folio): number {
  return Math.max(0, reserva.valor.total + reserva.extras.total - folio.totalPagado)
}

/** «Completo», «Falta 1 dato», «Faltan 6 datos». Cada dato es uno de `DatoTra` del backend. */
export function textoTra(ocupante: Ocupante): string {
  const faltan = ocupante.datosTraFaltantes?.length ?? 0
  if (faltan === 0) return 'Completo'
  return faltan === 1 ? 'Falta 1 dato' : `Faltan ${faltan} datos`
}

export function tieneTraPendiente(ocupante: Ocupante): boolean {
  return (ocupante.datosTraFaltantes?.length ?? 0) > 0
}

/** «Hacer principal» solo en un mayor de edad a la fecha de entrada que no lo es ya (DEC-97). */
export function puedeSerPrincipal(ocupante: Ocupante, entrada: string): boolean {
  return !ocupante.principal && Boolean(ocupante.fechaNacimiento)
    && edadEn(ocupante.fechaNacimiento as string, entrada) >= MAYORIA_DE_EDAD
}

/**
 * «Validar identidad» solo si el titular se aloja: su documento coincide con el de un huésped. Si no se aloja no está
 * presente, así que no hay documento que revisar (DEC-98).
 */
export function titularSeAloja(titular: Pick<Titular, 'tipoDocumento' | 'numeroDocumento'>, ocupantes: Ocupante[]): boolean {
  return ocupantes.some((ocupante) =>
    ocupante.numeroDocumento === titular.numeroDocumento && ocupante.tipoDocumento === titular.tipoDocumento)
}

/** El siguiente huésped al que le falta algo, después de `actual` y dando la vuelta; nulo si ya no queda nadie. */
export function siguienteConFaltantes(ocupantes: Ocupante[], actual: string): Ocupante | null {
  const posicion = ocupantes.findIndex((ocupante) => ocupante.id === actual)
  const orden = [...ocupantes.slice(posicion + 1), ...ocupantes.slice(0, Math.max(posicion, 0))]
  return orden.find(tieneTraPendiente) ?? null
}

/** Por qué no se puede registrar todavía, en el orden en que recepción lo resuelve. */
export type Bloqueo = 'fecha' | 'apartamento' | 'anticipo' | 'pago' | null

export interface Situacion {
  /** Nulo si no se pudo consultar: entonces decide el backend. */
  estadoApartamento: string | null
  falta: number
  /** Solo en una reserva pendiente: lo que falta para poder confirmarla (RES-15). */
  faltaAnticipo: number
  antesDeEntrada: boolean
  sinCobro: boolean
  motivo: string
}

export function bloqueoDe({ estadoApartamento, falta, faltaAnticipo, antesDeEntrada, sinCobro, motivo }: Situacion): Bloqueo {
  if (antesDeEntrada) return 'fecha'
  if (estadoApartamento !== null && estadoApartamento !== 'PREPARADO') return 'apartamento'
  if (faltaAnticipo > 0) return 'anticipo'
  if (falta > 0 && !(sinCobro && motivo.trim())) return 'pago'
  return null
}

/** Lo que dice el botón principal del pie. */
export function textoDelBoton(reserva: Reserva, situacion: Situacion): string {
  if (situacion.antesDeEntrada) return `Disponible desde ${fechaSinAnio(reserva.entrada)}`
  const sinCobro = situacion.falta > 0 && situacion.sinCobro && situacion.motivo.trim() ? ' sin cobro' : ''
  return reserva.estado === 'PENDIENTE' ? `Confirmar y registrar llegada${sinCobro}` : `Registrar llegada${sinCobro}`
}

/** La pista bajo el botón cuando no se puede registrar. */
export function pistaDelBloqueo(bloqueo: Bloqueo, sinCobro: boolean): string | null {
  switch (bloqueo) {
    case 'fecha':
      return 'Ya puedes cobrar y completar los datos de los huéspedes.'
    case 'apartamento':
      return 'El apartamento todavía no está preparado.'
    case 'anticipo':
      return 'Para confirmar la reserva hay que cubrir el anticipo, aunque entre sin pagar la estancia.'
    case 'pago':
      return sinCobro ? 'Escribe el motivo para registrar sin cobro.' : 'Falta cobrar la estancia, o marca que entra sin pagar.'
    default:
      return null
  }
}
