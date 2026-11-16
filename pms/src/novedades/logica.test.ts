import { describe, expect, it } from 'vitest'
import type { Novedad } from '../api/novedades'
import { accionSiguiente, hace, listaDe, reportada, resumen, textoPaso } from './logica'

const AHORA = new Date('2026-11-17T10:40:00')

const VIDRIO: Novedad = {
  id: 'NOV-3', apartamento: 'APT-104', descripcion: 'Vidrio de la ventana del cuarto roto, con riesgo de cortarse',
  gravedad: 'ALTA', estado: 'ABIERTA',
  historial: [{ estado: 'ABIERTA', autor: 'USR-4', autorNombre: 'Luz Marina Cardona', fechaHora: '2026-11-17T09:15:00' }],
}

describe('lógica de novedades', () => {
  it('cuenta cuánto hace en minutos, horas o días', () => {
    expect(hace('2026-11-17T10:40:00', AHORA)).toBe('hace un momento')
    expect(hace('2026-11-17T10:25:00', AHORA)).toBe('hace 15 minutos')
    expect(hace('2026-11-17T09:15:00', AHORA)).toBe('hace 1 hora')
    expect(hace('2026-11-15T16:02:00', AHORA)).toBe('hace 1 día')
    expect(hace('2026-11-10T08:00:00', AHORA)).toBe('hace 7 días')
  })

  it('dice quién la reportó, o que esa persona ya no está', () => {
    expect(reportada(VIDRIO, AHORA)).toBe('Reportada hace 1 hora por Luz Marina Cardona')
    const sinNombre = { ...VIDRIO, historial: [{ ...VIDRIO.historial[0], autorNombre: null }] }
    expect(reportada(sinNombre, AHORA)).toBe('Reportada hace 1 hora por una persona que ya no está en el sistema')
    expect(textoPaso(sinNombre.historial[0])).toEqual({
      estado: 'Abierta', quien: 'Una persona que ya no está en el sistema', cuando: '17 nov 2026, 9:15 a. m.',
    })
  })

  it('la acción siguiente va en un solo sentido y una cerrada no tiene', () => {
    expect(accionSiguiente('ABIERTA')?.texto).toBe('Pasar a revisión')
    expect(accionSiguiente('EN_REVISION')?.texto).toBe('Cerrar')
    expect(accionSiguiente('EN_REVISION')?.confirmar).toBe('Cerrar novedad')
    expect(accionSiguiente('CERRADA')).toBeNull()
  })

  it('recorta la descripción para el aviso y lee los filtros separados por comas', () => {
    expect(resumen(VIDRIO)).toBe('APT-104 · Vidrio de la ventana del cuarto roto…')
    expect(resumen({ apartamento: 'APT-107', descripcion: 'Falta una toalla' })).toBe('APT-107 · Falta una toalla')
    expect(listaDe('ABIERTA, EN_REVISION,,ABIERTA')).toEqual(['ABIERTA', 'EN_REVISION'])
    expect(listaDe(null)).toEqual([])
  })
})
