import { describe, expect, it, vi } from 'vitest'
import { sesionDe } from '../pruebas/sesionDePrueba'
import { alPerderSesion, guardarSesion, leerSesion, sesionPerdida } from './almacen'

describe('almacén de la sesión', () => {
  it('devuelve la sesión guardada mientras el token vale', () => {
    const sesion = sesionDe('RECEPCIONISTA')
    guardarSesion(sesion)

    expect(leerSesion()).toEqual(sesion)
  })

  it('olvida una sesión vencida', () => {
    guardarSesion(sesionDe('RECEPCIONISTA', { expiraEn: '2026-10-27T10:00:00Z' }))

    expect(leerSesion(new Date('2026-10-27T10:00:00Z'))).toBeNull()
    expect(localStorage.getItem('sga.pms.sesion')).toBeNull()
  })

  it('no acepta lo que no es una sesión del personal', () => {
    localStorage.setItem('sga.pms.sesion', JSON.stringify({ ...sesionDe('ADMINISTRADOR'), rol: 'HUESPED' }))
    expect(leerSesion()).toBeNull()

    localStorage.setItem('sga.pms.sesion', '{no es json')
    expect(leerSesion()).toBeNull()
  })

  it('al perder la sesión la borra y avisa a quien escucha', () => {
    guardarSesion(sesionDe('ADMINISTRADOR'))
    const aviso = vi.fn()
    const dejarDeEscuchar = alPerderSesion(aviso)

    sesionPerdida()

    expect(leerSesion()).toBeNull()
    expect(aviso).toHaveBeenCalledOnce()
    dejarDeEscuchar()
    sesionPerdida()
    expect(aviso).toHaveBeenCalledOnce()
  })
})
