import { describe, expect, it, vi } from 'vitest'
import { alPerderSesion, leerSesion } from '../sesion/almacen'
import { entrarComo, laApiResponde, llamada } from '../pruebas/sesionDePrueba'
import { ErrorApi, pedir } from './cliente'

const SIN_TOKEN = { estado: 401, codigo: 'NO_AUTENTICADO', mensaje: 'Debes iniciar sesión', detalles: [] }

describe('pedir', () => {
  it('devuelve el JSON cuando la API responde bien', async () => {
    laApiResponde([200, [{ codigo: 'RES-2026-00001' }]])

    await expect(pedir('/api/reservas')).resolves.toEqual([{ codigo: 'RES-2026-00001' }])
  })

  it('envía el cuerpo como JSON', async () => {
    const fetch = laApiResponde([200, {}])

    await pedir('/api/auth/login', { method: 'POST', cuerpo: { correo: 'ana@sga.co' } })

    expect(llamada(fetch, 0).cuerpo).toEqual({ correo: 'ana@sga.co' })
    expect(llamada(fetch, 0).cabeceras).toMatchObject({ 'Content-Type': 'application/json' })
  })

  it('con sesión, envía el token del personal', async () => {
    entrarComo('RECEPCIONISTA')
    const fetch = laApiResponde([200, []])

    await pedir('/api/reservas')

    expect(llamada(fetch, 0).cabeceras).toMatchObject({ Authorization: 'Bearer token-RECEPCIONISTA' })
  })

  it('sin sesión, no envía token', async () => {
    const fetch = laApiResponde([200, []])

    await pedir('/api/alojamiento')

    expect(llamada(fetch, 0).cabeceras).not.toHaveProperty('Authorization')
  })

  it('convierte el error del backend en ErrorApi con su mensaje y sus detalles', async () => {
    laApiResponde([400, { estado: 400, codigo: 'VALIDACION', mensaje: 'Datos inválidos', detalles: ['correo: es obligatorio'] }])

    await expect(pedir('/api/auth/login')).rejects.toMatchObject({
      estado: 400, codigo: 'VALIDACION', message: 'Datos inválidos', detalles: ['correo: es obligatorio'],
    })
  })

  it('si el backend rechaza el token, cierra la sesión y avisa', async () => {
    entrarComo('ADMINISTRADOR')
    const aviso = vi.fn()
    alPerderSesion(aviso)
    laApiResponde([401, SIN_TOKEN])

    await expect(pedir('/api/usuarios')).rejects.toMatchObject({ codigo: 'NO_AUTENTICADO' })

    expect(leerSesion()).toBeNull()
    expect(aviso).toHaveBeenCalledOnce()
  })

  it('un 401 sin token (contraseña errada) no es una sesión perdida', async () => {
    const aviso = vi.fn()
    alPerderSesion(aviso)
    laApiResponde([401, { ...SIN_TOKEN, codigo: 'CREDENCIALES_INVALIDAS' }])

    await expect(pedir('/api/auth/login', { method: 'POST' })).rejects.toBeInstanceOf(ErrorApi)

    expect(aviso).not.toHaveBeenCalled()
  })

  it('si el servidor no responde, avisa que no hay conexión', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(pedir('/api/reservas')).rejects.toMatchObject({ codigo: 'SIN_CONEXION' })
  })

  it('si la respuesta no es JSON (backend apagado detrás de Vite), avisa que no hay conexión', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Bad Gateway', { status: 502 })))

    await expect(pedir('/api/reservas')).rejects.toMatchObject({ codigo: 'SIN_CONEXION' })
  })
})
