import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { cerrarSesion, iniciarSesion } from '../api/sesion'
import { alPerderSesion, borrarSesion, guardarSesion, leerSesion, type Sesion } from './almacen'
import { ContextoSesion, type MotivoSinSesion, type ValorSesion } from './contexto'

/** Guarda quién entró al PMS y avisa a toda la aplicación cuando entra, sale o pierde la sesión. */
export default function ProveedorSesion({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(() => leerSesion())
  const [motivo, setMotivo] = useState<MotivoSinSesion>('sin-entrar')
  const [aviso, setAviso] = useState<string | null>(null)
  useEffect(
    () =>
      alPerderSesion(() => {
        setSesion(null)
        // Si ya se cerró por un cambio de la cuenta, el 401 que llega después no cambia lo que se le dice
        setMotivo((actual) => (actual === 'cuenta-cambiada' ? actual : 'perdida'))
      }),
    [],
  )

  const valor = useMemo<ValorSesion>(
    () => ({
      sesion,
      motivo,
      aviso,
      async entrar(correo, contrasena) {
        const nueva = await iniciarSesion(correo, contrasena)
        guardarSesion(nueva)
        setSesion(nueva)
        setAviso(null)
      },
      async salir() {
        // Aunque el backend no responda, en esta pestaña la sesión se cierra igual
        await cerrarSesion().catch(() => undefined)
        borrarSesion()
        setSesion(null)
        setMotivo('salio')
        setAviso(null)
      },
      terminar(texto) {
        borrarSesion()
        setSesion(null)
        setMotivo('cuenta-cambiada')
        setAviso(texto)
      },
      renombrar(nombre) {
        if (!sesion || sesion.nombre === nombre) return
        const renombrada = { ...sesion, nombre }
        guardarSesion(renombrada)
        setSesion(renombrada)
      },
    }),
    [sesion, motivo, aviso],
  )

  return <ContextoSesion.Provider value={valor}>{children}</ContextoSesion.Provider>
}
