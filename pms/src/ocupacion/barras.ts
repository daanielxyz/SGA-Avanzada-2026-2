// Cuentas del tablero: qué columnas ocupa cada barra y en qué carril va. Cada columna es una noche: la de salida no se
// ocupa, así que una reserva del 14 al 16 ocupa las columnas 14 y 15 (EST-01).
import type { BloqueoOcupado, OcupacionApartamento, ReservaOcupada } from '../api/ocupacion'
import { noches, sumarDias } from '../formato'

interface BarraBase {
  clave: string
  /** Primera columna que ocupa, desde 0. */
  columna: number
  /** Cuántas columnas ocupa dentro del rango. */
  ancho: number
  /** Empieza antes del rango. */
  cortadaInicio: boolean
  /** Sigue después del rango. */
  cortadaFin: boolean
  /** Fila dentro del apartamento: solo es mayor que 0 si dos barras se cruzan. */
  carril: number
}

export type Barra =
  | (BarraBase & { tipo: 'reserva'; reserva: ReservaOcupada })
  | (BarraBase & { tipo: 'bloqueo'; bloqueo: BloqueoOcupado })

/** Las noches del rango: desde `desde` hasta la anterior a `hasta`. */
export function nochesDelRango(desde: string, hasta: string): string[] {
  return Array.from({ length: Math.max(noches(desde, hasta), 0) }, (_, indice) => sumarDias(desde, indice))
}

function ubicar(inicio: string, fin: string, desde: string, hasta: string) {
  const primera = inicio < desde ? desde : inicio
  const ultima = fin > hasta ? hasta : fin
  return {
    columna: noches(desde, primera),
    ancho: noches(primera, ultima),
    cortadaInicio: inicio < desde,
    cortadaFin: fin > hasta,
  }
}

/** Reparte carriles: cada barra va en el primero que ya esté libre en su primera noche. */
function asignarCarriles<T extends { columna: number; ancho: number }>(barras: T[], primero: number): (T & { carril: number })[] {
  const finDeCarril: number[] = []
  return barras.map((barra) => {
    const libre = finDeCarril.findIndex((fin) => fin <= barra.columna)
    const carril = libre >= 0 ? libre : finDeCarril.length
    finDeCarril[carril] = barra.columna + barra.ancho
    return { ...barra, carril: primero + carril }
  })
}

/**
 * Las barras de un apartamento, en orden. Una reserva y un bloqueo no deberían cruzarse, pero un bloqueo puede
 * registrarse sobre una reserva ya tomada: si pasa, cada una va en su carril y ninguna tapa a la otra. Las noches de
 * un no-show quedan libres y se pueden volver a vender (DEC-101): esas barras van siempre en los últimos carriles.
 */
export function barrasDe(fila: OcupacionApartamento, desde: string, hasta: string): Barra[] {
  const todas = ubicarTodas(fila, desde, hasta)
  const esNoShow = (barra: Barra) => barra.tipo === 'reserva' && barra.reserva.estado === 'NO_SHOW'
  const vigentes = asignarCarriles(todas.filter((barra) => !esNoShow(barra)), 0)
  const carrilesVigentes = Math.max(0, ...vigentes.map((barra) => barra.carril + 1))
  const noShows = asignarCarriles(todas.filter(esNoShow), carrilesVigentes)
  return [...vigentes, ...noShows].sort((a, b) => a.columna - b.columna || a.carril - b.carril)
}

function ubicarTodas({ reservas, bloqueos }: OcupacionApartamento, desde: string, hasta: string): Barra[] {
  const barras = [
    ...reservas.map((reserva) => ({
      ...ubicar(reserva.entrada, reserva.salida, desde, hasta),
      tipo: 'reserva' as const,
      clave: reserva.codigo,
      carril: 0,
      reserva,
    })),
    ...bloqueos.map((bloqueo) => ({
      ...ubicar(bloqueo.fechaInicio, bloqueo.fechaFin, desde, hasta),
      tipo: 'bloqueo' as const,
      clave: bloqueo.id,
      carril: 0,
      bloqueo,
    })),
  ]
    .filter((barra) => barra.ancho > 0)
    .sort((a, b) => a.columna - b.columna)
  return barras
}
