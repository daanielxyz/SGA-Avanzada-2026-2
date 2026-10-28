import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { cerrarSesion, iniciarSesion } from '../api/sesion'
import { alPerderSesion, borrarSesion, guardarSesion, leerSesion, type Sesion } from './almacen'
import { ContextoSesion, type MotivoSinSesion, type ValorSesion } from './contexto'

/** Guarda quién entró al PMS y avisa a toda la aplicación cuando entra, sale o pierde la sesión. */
export default function ProveedorSesion({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(() => leerSesion())
  const [motivo, setMotivo] = useState<MotivoSinSesion>('sin-entrar')

  useEffect(
    () =>
      alPerderSesion(() => {
        setSesion(null)
        setMotivo('perdida')
      }),
    [],
  )

  const valor = useMemo<ValorSesion>(
    () => ({
      sesion,
      motivo,
      async entrar(correo, contrasena) {
        const nueva = await iniciarSesion(correo, contrasena)
        guardarSesion(nueva)
        setSesion(nueva)
      },
      async salir() {
        // Aunque el backend no responda, en este navegador la sesión se cierra igual
        await cerrarSesion().catch(() => undefined)
        borrarSesion()
        setSesion(null)
        setMotivo('salio')
      },
    }),
    [sesion, motivo],
  )

  return <ContextoSesion.Provider value={valor}>{children}</ContextoSesion.Provider>
}
