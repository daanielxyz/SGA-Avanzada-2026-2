import { describe, expect, it } from 'vitest'
import type { Canal } from '../api/canales'
import {
  detectado,
  enlaceTelefono,
  errorDeRango,
  estancia,
  mensajeLegible,
  partirCodigos,
  rangoInicial,
  sePuedeDesactivar,
  tieneBitacora,
} from './logica'

const canal = (tipo: Canal['tipo'], activo = true): Canal =>
  ({ id: 'CAN-1', nombre: 'X', tipo, referenciaCredencial: tipo === 'EXTERNO' ? 'X_API_KEY' : null, activo })

describe('canales: lógica', () => {
  it('solo un canal externo activo se desactiva, y solo los externos tienen bitácora', () => {
    expect(sePuedeDesactivar(canal('EXTERNO'))).toBe(true)
    expect(sePuedeDesactivar(canal('EXTERNO', false))).toBe(false)
    expect(sePuedeDesactivar(canal('PORTAL'))).toBe(false)
    expect(sePuedeDesactivar(canal('DIRECTO'))).toBe(false)
    expect(tieneBitacora(canal('EXTERNO', false))).toBe(true)
    expect(tieneBitacora(canal('DIRECTO'))).toBe(false)
  })

  it('la estancia no cuenta la noche de salida (EST-01)', () => {
    expect(estancia({ entrada: '2026-11-20', salida: '2026-11-23' })).toBe('20 nov 2026 → 23 nov 2026 · 3 noches')
    expect(estancia({ entrada: '2026-11-28', salida: '2026-11-29' })).toBe('28 nov 2026 → 29 nov 2026 · 1 noche')
  })

  it('dice cuánto lleva esperando un conflicto y cuándo llegó', () => {
    const ahora = new Date(2026, 10, 17, 10, 40)
    expect(detectado('2026-11-16T21:12:00', ahora)).toBe('hace 13 horas · 16 nov, 9:12 p. m.')
    expect(detectado('2026-11-17T10:15:00', ahora)).toBe('hace 25 minutos · 17 nov, 10:15 a. m.')
    expect(detectado('2026-11-17T10:40:20', ahora)).toBe('hace un momento · 17 nov, 10:40 a. m.')
    expect(detectado('2026-11-14T08:00:00', ahora)).toBe('hace 3 días · 14 nov, 8:00 a. m.')
  })

  it('el enlace para llamar deja solo los dígitos y el + del principio', () => {
    expect(enlaceTelefono('310 555 0142')).toBe('tel:3105550142')
    expect(enlaceTelefono(' +57 (310) 555-0142 ')).toBe('tel:+573105550142')
  })

  it('la bitácora abre con los últimos 7 días y no consulta si «Hasta» es antes de «Desde»', () => {
    expect(rangoInicial(new Date(2026, 10, 17, 10, 40))).toEqual({ desde: '2026-11-11', hasta: '2026-11-17' })
    expect(errorDeRango('2026-11-10', '2026-11-17')).toBeNull()
    expect(errorDeRango('2026-11-17', '2026-11-17')).toBeNull()
    expect(errorDeRango('2026-11-17', '2026-11-10')).toEqual({ campo: 'hasta', mensaje: '«Hasta» no puede ser antes de «Desde».' })
    expect(errorDeRango('', '2026-11-10')?.campo).toBe('desde')
  })

  it('muestra el mensaje del canal con sangría si es JSON, y tal cual si no', () => {
    expect(mensajeLegible('{"id":"BK-1","noches":2}')).toBe('{\n  "id": "BK-1",\n  "noches": 2\n}')
    expect(mensajeLegible('<xml>no es json</xml>')).toBe('<xml>no es json</xml>')
  })

  it('separa los códigos del sistema para mostrarlos en letra de código', () => {
    expect(partirCodigos('El apartamento APT-103 ya está reservado en esas fechas (RES-2026-00042)')).toEqual([
      { texto: 'El apartamento ', codigo: false },
      { texto: 'APT-103', codigo: true },
      { texto: ' ya está reservado en esas fechas (', codigo: false },
      { texto: 'RES-2026-00042', codigo: true },
      { texto: ')', codigo: false },
    ])
    expect(partirCodigos('6 apartamentos disponibles')).toEqual([{ texto: '6 apartamentos disponibles', codigo: false }])
  })
})
