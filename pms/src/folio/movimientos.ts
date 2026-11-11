// Cómo se muestran los movimientos del folio (RN-15 · RN-16). El backend los manda en el orden en que se hicieron;
// aquí se ordenan para que cada movimiento revertido quede junto a su reverso y se calculan los totales con su signo.
import type { ServicioAdicional } from '../api/alojamiento'
import type { Cargo, Folio, Pago } from '../api/folio'
import type { EstadoReserva } from '../api/reservas'

/** Una fila de la tabla: un movimiento y, si lo revirtieron, su reverso justo debajo. */
export interface Fila<T> {
  movimiento: T
  reverso: T | null
}

/** El valor con su signo: un AJUSTE que disminuye resta del saldo (RN-15). */
export function valorConSigno(cargo: Cargo): number {
  return cargo.tipo === 'AJUSTE' && cargo.sentido === 'DISMINUYE' ? -cargo.valor : cargo.valor
}

/** Un REVERSO (de un abono o una devolución) resta de lo pagado. */
export function montoConSigno(pago: Pago): number {
  return pago.tipo === 'REVERSO' ? -pago.monto : pago.monto
}

/** Lo que suman los cargos, ya con sus correcciones: con lo pagado, da el saldo. */
export function cargosNetos(folio: Folio): number {
  return folio.cargos.reduce((suma, cargo) => suma + valorConSigno(cargo), 0)
}

/** Los cargos en su orden, cada uno con su corrección debajo; la corrección no se repite en su propio lugar. */
export function filasDeCargos(cargos: Cargo[]): Fila<Cargo>[] {
  const correcciones = new Map(cargos.filter((c) => c.corrigeA).map((c) => [c.corrigeA as string, c]))
  const ubicadas = new Set(cargos.filter((c) => correcciones.has(c.id)).map((c) => correcciones.get(c.id)?.id))
  return cargos
    .filter((cargo) => !ubicadas.has(cargo.id))
    .map((cargo) => ({ movimiento: cargo, reverso: correcciones.get(cargo.id) ?? null }))
}

/** Los pagos en su orden, cada abono revertido con su reverso debajo. Una devolución va sola, en su lugar. */
export function filasDePagos(pagos: Pago[]): Fila<Pago>[] {
  const reversos = new Map(pagos.filter((p) => p.reversaDe).map((p) => [p.reversaDe as string, p]))
  const ubicados = new Set(pagos.filter((p) => reversos.has(p.id)).map((p) => reversos.get(p.id)?.id))
  return pagos
    .filter((pago) => !ubicados.has(pago.id))
    .map((pago) => ({ movimiento: pago, reverso: reversos.get(pago.id) ?? null }))
}

/** «Agua × 2»: el concepto no trae la cantidad (DEC-107). */
export function nombreDelCargo(cargo: Cargo): string {
  return (cargo.cantidad ?? 1) > 1 ? `${cargo.concepto} × ${cargo.cantidad}` : cargo.concepto
}

/**
 * El backend escribe «Corrección de CAR-5: motivo», pero recepción no ve los códigos de los cargos: se cambia por el
 * nombre del cargo corregido. Si el texto no empieza así, se deja tal cual.
 */
export function conceptoVisible(cargo: Cargo, cargos: Cargo[]): string {
  const original = cargo.corrigeA ? cargos.find((c) => c.id === cargo.corrigeA) : undefined
  const prefijo = `Corrección de ${cargo.corrigeA}`
  if (original && cargo.concepto.startsWith(prefijo)) {
    return `Corrección de ${nombreDelCargo(original)}${cargo.concepto.slice(prefijo.length)}`
  }
  return nombreDelCargo(cargo)
}

const TIPO_DE_CARGO: Record<string, string> = {
  HOSPEDAJE: 'Hospedaje',
  SERVICIO_ADICIONAL: 'Servicio',
  PENALIDAD_CANCELACION: 'Penalidad',
}

/** «Servicio», «Corrección · disminuye», «Ajuste · aumenta». Un ajuste sin `corrigeA` lo hizo el sistema (RN-14). */
export function tipoDelCargo(cargo: Cargo): string {
  if (cargo.tipo !== 'AJUSTE') return TIPO_DE_CARGO[cargo.tipo] ?? cargo.tipo
  const sentido = cargo.sentido === 'DISMINUYE' ? 'disminuye' : 'aumenta'
  return `${cargo.corrigeA ? 'Corrección' : 'Ajuste'} · ${sentido}`
}

/** «Abono», «Reverso del abono» o «Devolución»: un REVERSO sin `reversaDe` es una devolución (PAG-06). */
export function tipoDelPago(pago: Pago): string {
  if (pago.tipo !== 'REVERSO') return 'Abono'
  return pago.reversaDe ? 'Reverso del abono' : 'Devolución'
}

// El backend carga los extras de la reserva al folio como servicios con estos conceptos (`Folio.cargarExtras`); las
// reservas anteriores a DEC-89 traen «Mascotas (n)».
export function esExtraDeLaReserva(cargo: Cargo): boolean {
  return cargo.concepto === 'Parqueadero' || cargo.concepto === 'Mascotas' || cargo.concepto.startsWith('Mascotas (')
}

/** Un consumo del catálogo (minibar, lavandería…): el único cargo que se puede volver a cargar desde el folio. */
export function esConsumo(cargo: Cargo): boolean {
  return cargo.tipo === 'SERVICIO_ADICIONAL' && !esExtraDeLaReserva(cargo)
}

/** Lo que se puede hacer con un cargo. Una corrección, o un cargo ya corregido, no admite nada (CAR-03). */
export function accionesDelCargo(fila: Fila<Cargo>, estado: EstadoReserva, folio: Folio) {
  const libre = !folio.cerrado && !fila.reverso && !fila.movimiento.corrigeA
  return {
    revertir: libre,
    // Hospedaje, extras y penalidad no se vuelven a cargar desde el folio: su valor cambia modificando la reserva
    corregir: libre && estado === 'EN_CURSO' && esConsumo(fila.movimiento),
  }
}

/** Solo un abono sin revertir se revierte; un reverso o una devolución no (PAG-03). */
export function puedeRevertirPago(fila: Fila<Pago>, folio: Folio): boolean {
  return !folio.cerrado && fila.movimiento.tipo === 'ABONO' && !fila.reverso
}

/** Los consumos que recepción puede cargar: activos y con cobro. */
export function consumosDelCatalogo(servicios: ServicioAdicional[] = []): ServicioAdicional[] {
  return servicios.filter((servicio) => servicio.activo && servicio.generaCargo)
}

/** El servicio del catálogo de un consumo ya cargado: el backend guarda el nombre del servicio como concepto. */
export function servicioDelCargo(cargo: Cargo, servicios: ServicioAdicional[]): ServicioAdicional | null {
  return servicios.find((servicio) => servicio.nombre === cargo.concepto) ?? null
}

export type AccionFolio = 'pago' | 'devolucion' | 'consumo' | 'salida' | 'cerrar'

/**
 * Lo que se ofrece arriba del folio, en orden y con la principal primero. Cargar consumos y registrar la salida solo
 * con la reserva en curso; cerrar sin salida, solo en una cancelada o con no-show (CU-28).
 */
export function accionesDelFolio(estado: EstadoReserva, folio: Folio): AccionFolio[] {
  if (folio.cerrado) return []
  const enCurso = estado === 'EN_CURSO'
  const terminada = estado === 'FINALIZADA' || estado === 'CANCELADA' || estado === 'NO_SHOW'
  // Igual que `puedeRegistrarPago`: una reserva terminada solo recibe pagos si todavía debe (PAG-06)
  const pago = !terminada || folio.situacionSaldo === 'PENDIENTE'
  // Solo se devuelve lo que sobra, y nunca más de lo abonado (TPAG-02)
  const devolucion = folio.situacionSaldo === 'A_FAVOR' && folio.totalPagado > 0
  const cerrar = estado === 'CANCELADA' || estado === 'NO_SHOW'

  const principal: AccionFolio | null = cerrar
    ? 'cerrar'
    : devolucion
      ? 'devolucion'
      : folio.situacionSaldo === 'PENDIENTE' && pago
        ? 'pago'
        : enCurso
          ? 'salida'
          : null
  const todas: AccionFolio[] = [
    ...(pago ? (['pago'] as const) : []),
    ...(devolucion ? (['devolucion'] as const) : []),
    ...(enCurso ? (['consumo', 'salida'] as const) : []),
    ...(cerrar ? (['cerrar'] as const) : []),
  ]
  return principal ? [principal, ...todas.filter((accion) => accion !== principal)] : todas
}

/** Textos del backend para cada dato del TRA (`DatoTra`, DEC-96). */
const DATO_TRA: Record<string, string> = {
  DOCUMENTO: 'documento',
  PRIMER_APELLIDO: 'primer apellido',
  NACIONALIDAD: 'nacionalidad',
  GENERO: 'género',
  PAIS_RESIDENCIA: 'país de residencia',
  CIUDAD_RESIDENCIA: 'ciudad de residencia',
  CIUDAD_PROCEDENCIA: 'procedencia',
  CIUDAD_DESTINO: 'destino',
  MOTIVO_VIAJE: 'motivo del viaje',
}

export function datoTraLegible(dato: string): string {
  return DATO_TRA[dato] ?? dato.replaceAll('_', ' ').toLowerCase()
}
