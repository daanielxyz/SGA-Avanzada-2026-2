import { describe, expect, it } from 'vitest'
import type { BloqueoOcupado, OcupacionApartamento, ReservaOcupada } from '../api/ocupacion'
import { barrasDe, nochesDelRango } from './barras'

const DESDE = '2026-11-12'
const HASTA = '2026-11-26'

function reserva(codigo: string, entrada: string, salida: string): ReservaOcupada {
  return { codigo, entrada, salida, estado: 'CONFIRMADA', canalOrigen: 'DIRECTO', titularId: 'TIT-1' }
}

function bloqueo(id: string, fechaInicio: string, fechaFin: string): BloqueoOcupado {
  return { id, fechaInicio, fechaFin, motivo: 'Pintura' }
}

function apartamento(reservas: ReservaOcupada[], bloqueos: BloqueoOcupado[] = []): OcupacionApartamento {
  return { apartamento: 'APT-101', nombre: 'Mirador', estadoOperativo: 'PREPARADO', reservas, bloqueos }
}

const barras = (dato: OcupacionApartamento) => barrasDe(dato, DESDE, HASTA)

describe('nochesDelRango (EST-01)', () => {
  it('no incluye la noche de `hasta`', () => {
    const lista = nochesDelRango(DESDE, HASTA)
    expect(lista).toHaveLength(14)
    expect(lista[0]).toBe('2026-11-12')
    expect(lista.at(-1)).toBe('2026-11-25')
  })

  it('un rango al revés no tiene noches', () => {
    expect(nochesDelRango(HASTA, DESDE)).toEqual([])
  })
})

describe('barrasDe (EST-01)', () => {
  it('una reserva del 14 al 16 ocupa dos columnas: la de salida no se pinta', () => {
    const [barra] = barras(apartamento([reserva('R1', '2026-11-14', '2026-11-16')]))
    expect(barra).toMatchObject({ columna: 2, ancho: 2, cortadaInicio: false, cortadaFin: false, carril: 0 })
  })

  it('una reserva de una noche ocupa una columna', () => {
    expect(barras(apartamento([reserva('R1', '2026-11-13', '2026-11-14')]))[0]).toMatchObject({ columna: 1, ancho: 1 })
  })

  it('una reserva que empieza antes del rango se corta al inicio', () => {
    const [barra] = barras(apartamento([reserva('R1', '2026-11-08', '2026-11-14')]))
    expect(barra).toMatchObject({ columna: 0, ancho: 2, cortadaInicio: true, cortadaFin: false })
  })

  it('una reserva que termina después del rango se corta al final', () => {
    const [barra] = barras(apartamento([reserva('R1', '2026-11-24', '2026-12-02')]))
    expect(barra).toMatchObject({ columna: 12, ancho: 2, cortadaInicio: false, cortadaFin: true })
  })

  it('una salida justo en `hasta` no está cortada', () => {
    expect(barras(apartamento([reserva('R1', '2026-11-24', HASTA)]))[0].cortadaFin).toBe(false)
  })

  it('reservas seguidas comparten el carril: una sale y otra entra el mismo día, sin solaparse', () => {
    const lista = barras(apartamento([reserva('R2', '2026-11-17', '2026-11-20'), reserva('R1', '2026-11-14', '2026-11-17')]))
    expect(lista.map((barra) => [barra.clave, barra.columna, barra.carril])).toEqual([['R1', 2, 0], ['R2', 5, 0]])
  })

  it('un bloqueo usa su propio intervalo, con el fin exclusivo (BLO-02)', () => {
    const [barra] = barras(apartamento([], [bloqueo('B1', '2026-11-18', '2026-11-21')]))
    expect(barra).toMatchObject({ tipo: 'bloqueo', columna: 6, ancho: 3 })
  })

  it('si un bloqueo cruza una reserva, cada uno va en su carril y ninguno tapa al otro', () => {
    const lista = barras(apartamento([reserva('R1', '2026-11-14', '2026-11-18')], [bloqueo('B1', '2026-11-16', '2026-11-20')]))
    expect(lista.map((barra) => barra.carril)).toEqual([0, 1])
  })

  it('lo que queda fuera del rango no se dibuja', () => {
    expect(barras(apartamento([reserva('R1', '2026-11-01', DESDE)]))).toEqual([])
  })
})
