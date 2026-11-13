import { useEffect, useState, type FormEvent } from 'react'
import type { Alojamiento } from '../api/alojamiento'
import { ErrorApi } from '../api/cliente'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import type { Errores } from './formularios'

/** Lo que cada bloque le cuenta a la página: si tiene cambios sin guardar y la respuesta de la API al guardar. */
export interface AlBloque {
  alojamiento: Alojamiento
  alGuardar: (nuevo: Alojamiento) => void
  alCambiarSucio: (sucio: boolean) => void
}

export type Estado = 'libre' | 'guardando' | 'guardado'

/**
 * El formulario de un bloque que se guarda solo. Parte de lo que dice la API y se compara con eso para saber si hay
 * cambios; al guardar, la respuesta es el nuevo punto de partida.
 *
 * @param desde   lo que muestra el formulario para un alojamiento
 * @param campoDe a qué campo pertenece un detalle de un 400 («anticipoPct: …»); nulo si a ninguno
 */
export function useBloque<F extends Record<string, unknown>>({ alojamiento, alGuardar, alCambiarSucio }: AlBloque, {
  desde,
  validar,
  campoDe = (campo) => campo,
}: {
  desde: (alojamiento: Alojamiento) => F
  validar: (form: F) => Errores<Extract<keyof F, string>>
  campoDe?: (campoApi: string) => string | null
}) {
  const [form, setForm] = useState<F>(() => desde(alojamiento))
  const [errores, setErrores] = useState<Errores<Extract<keyof F, string>>>({})
  const [estado, setEstado] = useState<Estado>('libre')
  const [rechazo, setRechazo] = useState<(ErrorMostrado & { asunto: string }) | null>(null)
  const sucio = JSON.stringify(form) !== JSON.stringify(desde(alojamiento))

  useEffect(() => {
    alCambiarSucio(sucio)
  }, [sucio, alCambiarSucio])
  useEffect(() => () => alCambiarSucio(false), [alCambiarSucio])

  function cambiar<C extends keyof F>(campo: C, valor: F[C]) {
    setForm((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
    if (estado === 'guardado') setEstado('libre')
  }

  /** Revisa, envía y, si la API lo acepta, toma su respuesta. El rechazo queda en el bloque. */
  async function guardar(evento: FormEvent, asunto: string, enviar: (form: F) => Promise<Alojamiento>) {
    evento.preventDefault()
    const encontrados = validar(form)
    setErrores(encontrados)
    setRechazo(null)
    if (Object.values(encontrados).some(Boolean)) {
      setEstado('libre')
      setRechazo({ asunto, mensaje: 'Revisa los campos marcados.', detalles: [] })
      return
    }
    setEstado('guardando')
    try {
      const nuevo = await enviar(form)
      alGuardar(nuevo)
      setForm(desde(nuevo))
      setEstado('guardado')
    } catch (causa) {
      setEstado('libre')
      const mostrado = errorParaMostrar(causa)
      if (causa instanceof ErrorApi && causa.estado === 400) {
        const porCampo: Record<string, string> = {}
        const otros: string[] = []
        for (const detalle of causa.detalles) {
          const campo = campoDe(detalle.split(':')[0].trim())
          if (campo && campo in form) porCampo[campo] = 'Revisa este dato.'
          else otros.push(detalle)
        }
        setErrores(porCampo as Errores<Extract<keyof F, string>>)
        setRechazo({ asunto, mensaje: 'Revisa los campos marcados.', detalles: otros })
      } else {
        setRechazo({ asunto, ...mostrado })
      }
    }
  }

  return { form, setForm, cambiar, errores, estado, rechazo, sucio, guardar }
}
