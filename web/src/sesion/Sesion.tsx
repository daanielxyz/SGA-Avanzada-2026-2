import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router'
import { alPerderSesion, borrarSesion, guardarSesion, leerSesion, type Sesion } from './almacen'
import { ContextoSesion, useSesion } from './useSesion'

/** Guarda quién entró, para que la cabecera y las páginas del huésped lo sepan. */
export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(() => leerSesion())
  const [perdida, setPerdida] = useState(false)

  useEffect(() => alPerderSesion(() => {
    setSesion(null)
    setPerdida(true)
  }), [])

  const entrar = useCallback((nueva: Sesion) => {
    guardarSesion(nueva)
    setSesion(nueva)
    setPerdida(false)
  }, [])

  const salir = useCallback(() => {
    borrarSesion()
    setSesion(null)
    setPerdida(false)
  }, [])

  const valor = useMemo(() => ({ sesion, perdida, entrar, salir }), [sesion, perdida, entrar, salir])
  return <ContextoSesion value={valor}>{children}</ContextoSesion>
}

/** Las páginas del huésped: sin sesión lleva a entrar, y al entrar vuelve a la página que quería ver. */
export function RutasDeHuesped() {
  const { sesion } = useSesion()
  const { pathname, search } = useLocation()
  if (!sesion) {
    return <Navigate to={`/entrar?volver=${encodeURIComponent(pathname + search)}`} replace />
  }
  return <Outlet />
}
