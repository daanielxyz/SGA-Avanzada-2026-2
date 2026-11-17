import { describe, expect, it } from 'vitest'
import type { RespuestaAsistente } from '../api/asistente'
import {
  bloques, destinoDeFuente, errorDePregunta, historialPara, preguntaDelRechazo, rutaDeFuente, trozos, type Intercambio,
} from './logica'

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

  it('una fuente solo es enlace si lleva a otra pantalla', () => {
    expect(destinoDeFuente('/tarifas', '/reservas', 'ADMINISTRADOR')).toEqual({ tipo: 'pantalla', ruta: '/tarifas' })
    expect(destinoDeFuente('/reservas/{codigo}/folio', '/reservas/RES-2026-00042', 'RECEPCIONISTA'))
      .toEqual({ tipo: 'pantalla', ruta: '/reservas/RES-2026-00042/folio' })
    // Ya se está ahí: un enlace no movería a nadie
    expect(destinoDeFuente('/reservas', '/reservas', 'RECEPCIONISTA')).toEqual({ tipo: 'aqui' })
    expect(destinoDeFuente('/reservas/{codigo}', '/reservas/RES-2026-00042/', 'RECEPCIONISTA')).toEqual({ tipo: 'aqui' })
  })

  it('una fuente de una reserva, preguntada fuera de una, dice por dónde se llega', () => {
    expect(destinoDeFuente('/reservas/{codigo}/folio', '/', 'RECEPCIONISTA'))
      .toEqual({ tipo: 'desde', ruta: '/reservas', nombre: 'Reservas', aqui: false })
    expect(destinoDeFuente('/reservas/{codigo}/llegada', '/reservas', 'ADMINISTRADOR'))
      .toEqual({ tipo: 'desde', ruta: '/reservas', nombre: 'Reservas', aqui: true })
    expect(destinoDeFuente('/reservas/{codigo}', '/reservas/nueva', 'RECEPCIONISTA'))
      .toEqual({ tipo: 'desde', ruta: '/reservas', nombre: 'Reservas', aqui: false })
    // El personal de servicio no tiene «Reservas»: no se le ofrece un camino que no puede seguir
    expect(destinoDeFuente('/reservas/{codigo}', '/preparacion', 'PERSONAL_SERVICIO')).toEqual({ tipo: 'ninguno' })
    expect(destinoDeFuente('', '/', 'ADMINISTRADOR')).toEqual({ tipo: 'ninguno' })
    expect(destinoDeFuente('/inventada/{codigo}', '/', 'ADMINISTRADOR')).toEqual({ tipo: 'ninguno' })
  })

  it('un enlace de Markdown a una pantalla del PMS es enlace; a cualquier otro sitio, solo su texto', () => {
    expect(trozos('Ve a [Temporadas y tarifas](/tarifas) y pulsa **Guardar**.')).toEqual([
      { texto: 'Ve a ', negrita: false },
      { texto: 'Temporadas y tarifas', negrita: false, ruta: '/tarifas' },
      { texto: ' y pulsa ', negrita: false },
      { texto: 'Guardar', negrita: true },
      { texto: '.', negrita: false },
    ])
    expect(trozos('[las activas](/reservas?estado=CONFIRMADA,EN_CURSO)')[0].ruta).toBe('/reservas?estado=CONFIRMADA,EN_CURSO')
    for (const destino of ['https://ejemplo.com', '//ejemplo.com/x', 'javascript:alert(1)', '/reservas/{codigo}', 'tarifas', '']) {
      const partes = trozos(`Mira [esto](${destino}).`)
      expect(partes.some((parte) => parte.ruta !== undefined)).toBe(false)
      expect(partes[1]).toEqual({ texto: 'esto', negrita: false })
    }
    expect(trozos('Un corchete [suelto] no es enlace.')).toEqual([{ texto: 'Un corchete [suelto] no es enlace.', negrita: false }])
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
