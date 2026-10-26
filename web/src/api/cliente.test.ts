import { afterEach, describe, expect, it, vi } from 'vitest'
import { ErrorApi, pedir } from './cliente'

function responder(estado: number, cuerpo?: unknown) {
  const texto = cuerpo === undefined ? null : JSON.stringify(cuerpo)
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(texto, { status: estado })))
}

afterEach(() => vi.unstubAllGlobals())

describe('pedir', () => {
  it('devuelve el JSON cuando la API responde bien', async () => {
    responder(200, [{ codigo: 'APT-101' }])

    await expect(pedir('/api/apartamentos')).resolves.toEqual([{ codigo: 'APT-101' }])
  })

  it('envía el cuerpo como JSON', async () => {
    responder(200, {})

    await pedir('/api/cotizaciones', { method: 'POST', cuerpo: { apartamento: 'APT-101' } })

    const [, opciones] = vi.mocked(fetch).mock.calls[0]
    expect(opciones?.body).toBe('{"apartamento":"APT-101"}')
    expect(opciones?.headers).toMatchObject({ 'Content-Type': 'application/json' })
  })

  it('convierte el error del backend en ErrorApi con su mensaje', async () => {
    responder(409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'El apartamento no está libre', detalles: [] })

    await expect(pedir('/api/cotizaciones')).rejects.toMatchObject({
      estado: 409, codigo: 'REGLA_NEGOCIO', message: 'El apartamento no está libre',
    })
  })

  it('conserva los errores por campo de una validación', async () => {
    responder(400, { estado: 400, codigo: 'VALIDACION', mensaje: 'Datos inválidos', detalles: ['entrada: es obligatoria'] })

    await expect(pedir('/api/cotizaciones')).rejects.toMatchObject({ detalles: ['entrada: es obligatoria'] })
  })

  it('si el servidor no responde, avisa que no hay conexión', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    const error = await pedir('/api/apartamentos').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ErrorApi)
    expect(error).toMatchObject({ codigo: 'SIN_CONEXION' })
  })

  it('si la respuesta no es JSON (backend apagado detrás de Vite), avisa que no hay conexión', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Bad Gateway', { status: 502 })))

    await expect(pedir('/api/apartamentos')).rejects.toMatchObject({ codigo: 'SIN_CONEXION' })
  })
})
