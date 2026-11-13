import { useEffect, useId, useState, type FormEvent } from 'react'
import { cambiarMedioPago } from '../api/alojamiento'
import Aviso from '../componentes/Aviso'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import { CampoBloque } from './Bloque'
import type { AlBloque } from './useBloque'
import { nombreMedio } from './formularios'

/**
 * Medios con que se registran los pagos (MPAG-06). La API solo devuelve los habilitados: el que se deshabilita sale de
 * la lista y se vuelve a habilitar escribiéndolo otra vez. Los pagos que ya lo usaron no cambian (MPAG-03).
 */
export default function BloqueMediosPago({ alojamiento, alGuardar, alCambiarSucio }: AlBloque) {
  const medios = alojamiento.mediosPago
  const [nuevo, setNuevo] = useState('')
  const [errorCampo, setErrorCampo] = useState<string>()
  const [enviando, setEnviando] = useState<string | null>(null)
  const [rechazo, setRechazo] = useState<(ErrorMostrado & { asunto: string }) | null>(null)
  const [hecho, setHecho] = useState<string | null>(null)
  const id = useId()

  // Lo escrito en «Agregar» y no enviado cuenta como un cambio sin guardar
  const sucio = nuevo.trim() !== ''
  useEffect(() => {
    alCambiarSucio(sucio)
  }, [sucio, alCambiarSucio])
  useEffect(() => () => alCambiarSucio(false), [alCambiarSucio])

  async function cambiar(medio: string, accion: 'habilitar' | 'deshabilitar') {
    setEnviando(medio)
    setRechazo(null)
    setHecho(null)
    try {
      alGuardar(await cambiarMedioPago(medio, accion))
      if (accion === 'habilitar') setNuevo('')
      setHecho(`${nombreMedio(medio.trim().toUpperCase())} ${accion === 'habilitar' ? 'habilitado' : 'deshabilitado'}`)
    } catch (causa) {
      const asunto = accion === 'habilitar' ? `No se agregó ${medio.trim()}` : `No se deshabilitó ${nombreMedio(medio)}`
      setRechazo({ asunto, ...errorParaMostrar(causa) })
    } finally {
      setEnviando(null)
    }
  }

  function agregar(evento: FormEvent) {
    evento.preventDefault()
    const medio = nuevo.trim()
    if (!medio) {
      setErrorCampo('Escribe el nombre del medio de pago.')
      return
    }
    if (medios.includes(medio.toUpperCase())) {
      setErrorCampo(`${nombreMedio(medio.toUpperCase())} ya está habilitado.`)
      return
    }
    void cambiar(medio, 'habilitar')
  }

  return (
    <section className="tarjeta bloque" aria-labelledby={id}>
      <div className="bloque-cuerpo">
        <header className="bloque-cabecera">
          <h2 id={id}>Medios de pago</h2>
          <p className="suave">Habilitados ({medios.length}). Cada cambio se guarda al hacerlo.</p>
        </header>
        {rechazo && <Aviso tipo="error" asunto={rechazo.asunto} mensaje={rechazo.mensaje} detalles={rechazo.detalles} />}
        <p role="status" className="bloque-hecho">
          {hecho && <><span className="bloque-sello bloque-sello-guardado" aria-hidden="true">✓</span>{hecho}</>}
        </p>
        {medios.length === 0 ? (
          <p className="suave">No hay medios de pago habilitados: recepción no puede registrar pagos.</p>
        ) : (
          <ul className="lista-acciones" aria-label="Medios de pago habilitados">
            {medios.map((medio) => (
              <li key={medio}>
                <span className="lista-acciones-nombre">{nombreMedio(medio)}</span>
                <span className="etiqueta etiqueta-activo"><span aria-hidden="true">✓</span> Habilitado</span>
                <button type="button" className="boton boton-secundario boton-chico" disabled={enviando !== null}
                  aria-label={`Deshabilitar: ${nombreMedio(medio)}`} onClick={() => void cambiar(medio, 'deshabilitar')}>
                  {enviando === medio ? 'Deshabilitando…' : 'Deshabilitar'}
                </button>
              </li>
            ))}
          </ul>
        )}
        <form className="agregar-medio" onSubmit={agregar} noValidate>
          <CampoBloque etiqueta="Agregar medio de pago" valor={nuevo} error={errorCampo} maxLength={40}
            pista="Por ejemplo, Nequi. Un medio deshabilitado se vuelve a habilitar escribiéndolo aquí."
            disabled={enviando !== null}
            alCambiar={(texto) => { setNuevo(texto); setErrorCampo(undefined) }} />
          <button type="submit" className="boton boton-secundario" disabled={enviando !== null}>
            {enviando !== null && enviando === nuevo.trim() ? 'Agregando…' : 'Agregar'}
          </button>
        </form>
      </div>
    </section>
  )
}
