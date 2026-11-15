import { describe, expect, it } from 'vitest'
import type { RespuestaAsistente } from '../api/asistente'
import { bloques, errorDePregunta, historialPara, preguntaDelRechazo, rutaDeFuente, trozos, type Intercambio } from './logica'

const RESPUESTA: RespuestaAsistente = { respuesta: 'Así se hace.', fuentes: [], origen: 'IA', motivo: null, aviso: null }

describe('lógica del panel de ayuda (DEC-127)', () => {
  it('pide escribir la pregunta y no deja pasar de 1.000 caracteres', () => {
    expect(errorDePregunta('   ')).toBe('Escribe tu pregunta.')
    expect(errorDePregunta('a'.repeat(1000))).toBeNull()
    expect(errorDePregunta('a'.repeat(1001))).toMatch(/pasa de 1.000 caracteres/)
  })

  it('manda solo lo ya respondido, como pregunta y respuesta, y los más recientes', () => {
    const intercambios: Intercambio[] = [
      ...Array.from({ length: 6 }, (_, n): Intercambio => ({
        id: n, pregunta: `Pregunta ${n}`, estado: 'respondida', respuesta: { ...RESPUESTA, respuesta: `Respuesta ${n}` },
      })),
      { id: 7, pregunta: 'Sin respuesta', estado: 'fallida', error: 'Sin conexión' },
    ]

    const historial = historialPara(intercambios)

    expect(historial).toHaveLength(10)
    expect(historial[0]).toEqual({ autor: 'PERSONA', texto: 'Pregunta 1' })
    expect(historial.at(-1)).toEqual({ autor: 'ASISTENTE', texto: 'Respuesta 5' })
  })

  it('lleva cada fuente a su pantalla, con el código de la pantalla actual si lo necesita', () => {
    expect(rutaDeFuente('/reservas', '/')).toBe('/reservas')
    expect(rutaDeFuente('/reservas/{codigo}/folio', '/reservas/RES-2026-00042/llegada')).toBe('/reservas/RES-2026-00042/folio')
    expect(rutaDeFuente('/reservas/{codigo}', '/reservas/RES-2026-00042')).toBe('/reservas/RES-2026-00042')
    expect(rutaDeFuente('/reservas/{codigo}', '/preparacion')).toBeNull()
    expect(rutaDeFuente('/reservas/{codigo}', '/reservas/nueva')).toBeNull()
    expect(rutaDeFuente('', '/')).toBeNull()
  })

  it('arma la pregunta del «¿Por qué?» con el asunto y el mensaje del rechazo', () => {
    expect(preguntaDelRechazo('No se registró la llegada', 'El apartamento todavía no está preparado.'))
      .toBe('¿Por qué el sistema dice: «No se registró la llegada: El apartamento todavía no está preparado.»?')
  })

  it('entiende párrafos, listas y negritas, y nada más', () => {
    const texto = 'Para declarar el no-show:\n\n1. La reserva debe estar **confirmada**.\n2. Pulsa «Declarar no-show».\n'
      + '   Revisa la retención.\n\n- Una viñeta\n* Otra\n\n### Título <b>raro</b>'

    expect(bloques(texto)).toEqual([
      { tipo: 'parrafo', texto: 'Para declarar el no-show:' },
      { tipo: 'numerada', elementos: ['La reserva debe estar **confirmada**.', 'Pulsa «Declarar no-show». Revisa la retención.'] },
      { tipo: 'vinetas', elementos: ['Una viñeta', 'Otra'] },
      { tipo: 'parrafo', texto: 'Título <b>raro</b>' },
    ])
    expect(trozos('Debe estar **confirmada** hoy')).toEqual([
      { texto: 'Debe estar ', negrita: false },
      { texto: 'confirmada', negrita: true },
      { texto: ' hoy', negrita: false },
    ])
  })
})
