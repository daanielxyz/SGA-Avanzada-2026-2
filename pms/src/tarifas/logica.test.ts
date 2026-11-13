import { describe, expect, it } from 'vitest'
import type { Tarifa, Temporada } from '../api/tarifas'
import {
  conNombres,
  cuerpoTemporada,
  errorDeFechas,
  errorDeValor,
  hayErrores,
  leerPesos,
  ordenarTemporadas,
  pesosAlEscribir,
  textoEstancia,
  textoFechas,
  validarTemporada,
  vigentesPorTemporada,
} from './logica'

const base: Temporada = { id: 'TEM-1', nombre: 'Temporada base', fechaInicio: null, fechaFin: null, esBase: true,
  estanciaMinimaNoches: 0, activa: true }
const alta: Temporada = { id: 'TEM-2', nombre: 'Temporada alta fin de año', fechaInicio: '2026-12-15',
  fechaFin: '2027-01-15', esBase: false, estanciaMinimaNoches: 3, activa: true }
const santa: Temporada = { id: 'TEM-3', nombre: 'Semana Santa', fechaInicio: '2027-03-29', fechaFin: '2027-04-05',
  esBase: false, estanciaMinimaNoches: 2, activa: true }
const puente: Temporada = { id: 'TEM-4', nombre: 'Puente de noviembre', fechaInicio: '2026-11-14',
  fechaFin: '2026-11-17', esBase: false, estanciaMinimaNoches: 0, activa: false }
const calendario = [santa, puente, alta, base]

describe('temporadas', () => {
  it('ordena la base primero, luego por fecha, y aparta las inactivas', () => {
    const { activas, inactivas } = ordenarTemporadas(calendario)
    expect(activas.map((t) => t.id)).toEqual(['TEM-1', 'TEM-2', 'TEM-3'])
    expect(inactivas.map((t) => t.id)).toEqual(['TEM-4'])
  })

  it('escribe fechas y estancia como se leen', () => {
    expect(textoFechas(alta)).toBe('15 dic 2026 – 15 ene 2027')
    expect(textoFechas(base)).toBe('—')
    expect(textoEstancia(0)).toBe('Sin mínimo')
    expect(textoEstancia(1)).toBe('1 noche')
    expect(textoEstancia(3)).toBe('3 noches')
  })

  it('las fechas incluyen los dos días: compartir uno ya es solaparse (TEM-01 · TEM-02)', () => {
    expect(errorDeFechas('Reyes', '2027-01-15', '2027-01-20', calendario))
      .toBe('La temporada Reyes se solapa con Temporada alta fin de año (15 dic 2026 – 15 ene 2027). No pueden compartir ni un día.')
    expect(errorDeFechas('Reyes', '2027-01-16', '2027-01-20', calendario)).toBeNull()
    // Una inactiva no cuenta, y una temporada no se compara consigo misma
    expect(errorDeFechas('Puente', '2026-11-14', '2026-11-17', calendario)).toBeNull()
    expect(errorDeFechas('', '2026-12-10', '2027-01-20', calendario, 'TEM-2')).toBeNull()
    expect(errorDeFechas('', '2027-04-05', '2027-03-29', calendario))
      .toBe('El fin de la temporada no puede ser anterior al inicio.')
    expect(errorDeFechas('', '', '2027-03-29', calendario)).toBe('Elige el primer y el último día de la temporada.')
  })

  it('cambia los ids de la API por los nombres', () => {
    expect(conNombres('La temporada TEM-9 se solapa con TEM-2', calendario, 'Reyes'))
      .toBe('La temporada Reyes se solapa con Temporada alta fin de año')
    expect(conNombres('La temporada TEM-9 se solapa con TEM-3', calendario))
      .toBe('La temporada la temporada nueva se solapa con Semana Santa')
  })
})

describe('valores en pesos', () => {
  it('lee pesos enteros con o sin puntos de miles', () => {
    expect(leerPesos('180.000')).toBe(180000)
    expect(leerPesos('$ 180000')).toBe(180000)
    expect(leerPesos('1.250.000')).toBe(1250000)
    expect(leerPesos('180,5')).toBeNaN()
    expect(leerPesos('18.00')).toBeNaN()
    expect(pesosAlEscribir('180000')).toBe('180.000')
    expect(pesosAlEscribir('abc')).toBe('abc')
  })

  it('pide un valor mayor que cero (TAR-02)', () => {
    expect(errorDeValor('0')).toBe('Escribe un valor mayor que cero.')
    expect(errorDeValor('')).toBe('Escribe el valor por persona y noche.')
    expect(errorDeValor('', false)).toBeNull()
    expect(errorDeValor('12,5')).toMatch(/sin decimales/)
    expect(errorDeValor('90.000')).toBeNull()
  })
})

describe('nueva temporada', () => {
  const apartamentos = [
    { codigo: 'APT-101', nombre: 'Penthouse Mirador', activo: true },
    { codigo: 'APT-110', nombre: 'Mirador del Lago', activo: false },
  ]
  const form = { nombre: 'Reyes', inicio: '2027-01-16', fin: '2027-01-20', estancia: '0', tarifas: { 'APT-101': '150.000' } }

  it('exige la tarifa de los apartamentos a la venta; la de los retirados es opcional (TAR-03)', () => {
    expect(hayErrores(validarTemporada(form, calendario, apartamentos))).toBe(false)
    const sinTarifa = validarTemporada({ ...form, tarifas: {} }, calendario, apartamentos)
    expect(sinTarifa.tarifas).toEqual({ 'APT-101': 'Está a la venta: necesita su tarifa en esta temporada.' })
    const conCero = validarTemporada({ ...form, tarifas: { 'APT-101': '1', 'APT-110': '0' } }, calendario, apartamentos)
    expect(conCero.tarifas).toEqual({ 'APT-110': 'Escribe un valor mayor que cero.' })
  })

  it('pide nombre y estancia', () => {
    const errores = validarTemporada({ ...form, nombre: ' ', estancia: '' }, calendario, apartamentos)
    expect(errores.nombre).toBe('Escribe el nombre de la temporada.')
    expect(errores.estancia).toMatch(/0 si no hay mínimo/)
  })

  it('envía solo las tarifas escritas, en número', () => {
    expect(cuerpoTemporada({ ...form, tarifas: { 'APT-101': '150.000', 'APT-110': ' ' } })).toEqual({
      nombre: 'Reyes', fechaInicio: '2027-01-16', fechaFin: '2027-01-20', estanciaMinimaNoches: 0,
      tarifas: [{ apartamento: 'APT-101', valorPorOcupante: 150000 }],
    })
  })
})

it('toma la tarifa vigente de cada temporada', () => {
  const tarifa = (id: string, temporada: string, version: number): Tarifa => ({ id, apartamento: 'APT-101', temporada,
    valorPorOcupante: version * 1000, version, vigenteDesde: '2026-11-01' })
  const vigentes = vigentesPorTemporada([tarifa('T1', 'TEM-1', 1), tarifa('T3', 'TEM-2', 2), tarifa('T2', 'TEM-2', 1)])
  expect(vigentes.get('TEM-2')?.id).toBe('T3')
  expect(vigentes.get('TEM-1')?.id).toBe('T1')
})
