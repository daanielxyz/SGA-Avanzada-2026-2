// La cuenta de cada reserva en el tablero (BL-09 · DEC-101): el color de la barra. El estado manda sobre la cuenta:
// una reserva finalizada es «ya salió» aunque tenga saldo, y una no-show es «no se presentó».
import type { ReservaOcupada } from '../api/ocupacion'
import { dinero } from '../formato'

export type Cuenta = 'al-dia' | 'con-saldo' | 'debe-todo' | 'salio' | 'no-show'

export const NOMBRE_DE_LA_CUENTA: Record<Cuenta, string> = {
  'al-dia': 'Al día',
  'con-saldo': 'Con saldo',
  'debe-todo': 'Debe todo',
  salio: 'Ya salió',
  'no-show': 'No se presentó',
}

/** El orden de la leyenda. */
export const CUENTAS: readonly Cuenta[] = ['al-dia', 'con-saldo', 'debe-todo', 'salio', 'no-show']

/** Nulo si la API todavía no manda la cuenta (respuestas de antes de DEC-101): la barra queda con el color del estado. */
export function cuentaDe(reserva: ReservaOcupada): Cuenta | null {
  if (reserva.estado === 'NO_SHOW') return 'no-show'
  if (reserva.estado === 'FINALIZADA') return 'salio'
  switch (reserva.situacionSaldo) {
    case 'AL_DIA':
    case 'A_FAVOR':
      return 'al-dia'
    case 'PENDIENTE':
      return (reserva.totalPagado ?? 0) > 0 ? 'con-saldo' : 'debe-todo'
    default:
      return null
  }
}

/** La segunda línea de la barra: la cuenta en palabras, para no depender del color. */
export function textoCortoDeCuenta(reserva: ReservaOcupada): string | null {
  const cuenta = cuentaDe(reserva)
  if (!cuenta) return null
  if (cuenta === 'al-dia' && reserva.situacionSaldo === 'A_FAVOR') return 'A favor'
  return NOMBRE_DE_LA_CUENTA[cuenta]
}

/** La cuenta con su valor, para la descripción: «Con saldo: debe $ 675.000», «A favor $ 40.000 · devolver». */
export function textoDeCuenta(reserva: ReservaOcupada): string | null {
  const cuenta = cuentaDe(reserva)
  if (!cuenta) return null
  const saldo = reserva.saldo ?? 0
  if (cuenta === 'al-dia') return reserva.situacionSaldo === 'A_FAVOR' && saldo > 0 ? `A favor ${dinero(saldo)} · devolver` : 'Al día'
  if (cuenta === 'con-saldo') return `Con saldo: debe ${dinero(saldo)}`
  if (cuenta === 'debe-todo') return saldo > 0 ? `Debe todo: ${dinero(saldo)}` : 'Debe todo'
  return NOMBRE_DE_LA_CUENTA[cuenta]
}
