import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useLocation } from 'react-router'
import { preguntar as preguntarApi } from '../api/asistente'
import { ErrorApi } from '../api/cliente'
import { ContextoAsistente, type ValorAsistente } from './contexto'
import { historialPara, type Intercambio } from './logica'

const SIN_CONEXION = 'No se pudo enviar. Revisa tu conexión.'

/**
 * La conversación con el asistente de IA del PMS (DEC-127). Vive en la memoria de esta pestaña mientras dure la sesión: cerrar
 * el panel no la borra, pero recargar, cerrar sesión o «Empezar de nuevo» sí. No se guarda en el navegador, porque
 * puede traer datos de la operación.
 */
export default function ProveedorAsistente({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()
  const [abierto, setAbierto] = useState(false)
  const [intercambios, setIntercambios] = useState<Intercambio[]>([])
  const siguiente = useRef(1)
  const actuales = useRef<Intercambio[]>([])
  const ruta = useRef(pathname)

  useEffect(() => {
    actuales.current = intercambios
  }, [intercambios])
  useEffect(() => {
    ruta.current = pathname
  }, [pathname])

  const cambiar = useCallback((id: number, cambio: Partial<Intercambio>) => {
    // Si mientras tanto se empezó de nuevo, el intercambio ya no está y la respuesta se descarta
    setIntercambios((lista) => lista.map((i) => (i.id === id ? { ...i, ...cambio } : i)))
  }, [])

  const enviar = useCallback((id: number, pregunta: string, anteriores: readonly Intercambio[]) => {
    preguntarApi(pregunta, ruta.current, historialPara(anteriores))
      .then((respuesta) => cambiar(id, { estado: 'respondida', respuesta, error: undefined }))
      .catch((causa: unknown) => {
        const error = causa instanceof ErrorApi && causa.estado !== 0 ? causa.message : SIN_CONEXION
        cambiar(id, { estado: 'fallida', error })
      })
  }, [cambiar])

  const preguntar = useCallback((pregunta: string) => {
    const id = siguiente.current++
    const anteriores = actuales.current
    const nuevo: Intercambio = { id, pregunta, estado: 'esperando' }
    actuales.current = [...anteriores, nuevo]
    setIntercambios(actuales.current)
    setAbierto(true)
    enviar(id, pregunta, anteriores)
  }, [enviar])

  const reintentar = useCallback((id: number) => {
    const posicion = actuales.current.findIndex((i) => i.id === id)
    if (posicion < 0) return
    cambiar(id, { estado: 'esperando', error: undefined })
    enviar(id, actuales.current[posicion].pregunta, actuales.current.slice(0, posicion))
  }, [cambiar, enviar])

  const valor = useMemo<ValorAsistente>(() => ({
    abierto,
    intercambios,
    abrir: () => setAbierto(true),
    cerrar: () => setAbierto(false),
    preguntar,
    reintentar,
    empezarDeNuevo: () => {
      actuales.current = []
      setIntercambios([])
    },
  }), [abierto, intercambios, preguntar, reintentar])

  return <ContextoAsistente.Provider value={valor}>{children}</ContextoAsistente.Provider>
}
