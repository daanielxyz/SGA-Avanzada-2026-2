import { useEffect, useRef, useState } from 'react'
import { consultarEnvioTra, enviarTra, type EnvioTra } from '../api/tra'
import Aviso from '../componentes/Aviso'
import Dato from '../componentes/Dato'
import { cantidad } from '../formato'
import { fechaYHora } from './llegada/llegada'
import { errorParaMostrar } from './mensajes'

type Resultado = { clave: string; envio: EnvioTra | null } | { clave: string; error: unknown }

const ESTADOS = {
  ENVIADO: { simbolo: '✓', texto: 'Enviado' },
  PENDIENTE: { simbolo: '◐', texto: 'Pendiente' },
  CON_ERROR: { simbolo: '!', texto: 'Con error' },
} as const

/**
 * «Reporte al Ministerio (TRA)» en el detalle de una reserva que ya llegó (DEC-130): si ya se envió, si está pendiente
 * (sale solo cuando estén todos los datos) o si falló, con el motivo y «Reintentar».
 *
 * @param faltanDatos a algún ocupante le falta un dato del TRA: entonces todavía no se puede enviar
 * @param clave       cambia cuando cambian los datos de la reserva, para volver a leer el envío
 */
export default function EnvioTraReserva({ codigo, faltanDatos, clave }: {
  codigo: string
  faltanDatos: boolean
  clave: string
}) {
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<string | null>(null)
  const [enviado, setEnviado] = useState(false)
  const titulo = useRef<HTMLHeadingElement>(null)
  const actualClave = `${codigo}#${clave}#${intento}`

  useEffect(() => {
    let vigente = true
    consultarEnvioTra(codigo)
      .then((envio) => vigente && setResultado({ clave: actualClave, envio }))
      .catch((error: unknown) => vigente && setResultado({ clave: actualClave, error }))
    return () => {
      vigente = false
    }
  }, [codigo, actualClave])

  const actual = resultado?.clave === actualClave ? resultado : null
  const envio = actual && 'envio' in actual ? actual.envio : null
  // Sin envío (la reserva llegó antes de DEC-130) cuenta como pendiente: «Enviar ahora» lo crea
  const estado = envio?.estado ?? 'PENDIENTE'

  async function enviarYa() {
    setEnviando(true)
    setRechazo(null)
    setEnviado(false)
    try {
      const nuevo = await enviarTra(codigo)
      setResultado({ clave: actualClave, envio: nuevo })
      setEnviado(nuevo.estado === 'ENVIADO')
      // El botón ya no está: el foco vuelve al título del bloque, no se pierde en la página
      requestAnimationFrame(() => titulo.current?.focus())
    } catch (causa) {
      setRechazo(errorParaMostrar(causa).mensaje)
    } finally {
      setEnviando(false)
    }
  }

  // La acción va dentro del aviso que dice por qué hace falta, como «Reintentar» en el resto del PMS
  const boton = (texto: string) => (
    <button type="button" className="boton boton-secundario boton-chico" disabled={enviando} onClick={enviarYa}>
      {enviando ? 'Enviando…' : texto}
    </button>
  )

  return (
    <section aria-labelledby="titulo-tra" className="detalle-seccion">
      <h2 id="titulo-tra" ref={titulo} tabIndex={-1}>Reporte al Ministerio (TRA)</h2>
      {!actual && <p className="suave" role="status">Consultando el envío…</p>}
      {actual && 'error' in actual && (
        <Aviso tipo="error" asunto="No pudimos consultar el envío del TRA" mensaje={errorParaMostrar(actual.error).mensaje}
          accion={<button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>Reintentar</button>} />
      )}
      {actual && 'envio' in actual && (
        <div className="tarjeta envio-tra">
          <p className="envio-tra-estado">
            <span className="solo-lectores">Estado: </span>
            <span className={`etiqueta envio-tra-${estado.toLowerCase()}`}>
              <span aria-hidden="true">{ESTADOS[estado].simbolo}</span> {ESTADOS[estado].texto}
            </span>
          </p>
          <dl className="datos">
            {estado === 'ENVIADO' && envio?.enviadoEn && (
              <Dato nombre="Fecha de envío">
                {fechaYHora(envio.enviadoEn)} · {envio.acompanantesEnviados === 0
                  ? 'solo el huésped principal'
                  : `el huésped principal y ${cantidad(envio.acompanantesEnviados, 'acompañante', 'acompañantes')}`}
              </Dato>
            )}
            {envio?.codigoMinisterio && <Dato nombre="Código del Ministerio" codigo>{envio.codigoMinisterio}</Dato>}
            {envio && envio.intentos > 0 && estado !== 'ENVIADO' && envio.ultimoIntento && (
              <Dato nombre="Último intento">{fechaYHora(envio.ultimoIntento)} · {cantidad(envio.intentos, 'intento', 'intentos')}</Dato>
            )}
          </dl>
          {enviado && <Aviso tipo="exito" asunto="TRA enviado" mensaje="El Ministerio recibió la tarjeta de registro." />}
          {estado !== 'ENVIADO' && faltanDatos && (
            <Aviso tipo="info" mensaje="Se envía solo cuando estén todos los datos del TRA de los ocupantes." />
          )}
          {estado === 'PENDIENTE' && !faltanDatos && (
            <Aviso tipo="info" mensaje="Los datos están completos: se envía solo en los próximos minutos, o ya mismo."
              accion={boton('Enviar ahora')} />
          )}
          {estado === 'CON_ERROR' && !faltanDatos && (
            <Aviso tipo="advertencia" asunto="El Ministerio no lo recibió" mensaje={envio?.ultimoError ?? 'No se pudo enviar.'}
              accion={boton('Reintentar')} />
          )}
          {rechazo && <Aviso tipo="error" asunto="No se envió el TRA" mensaje={rechazo} />}
        </div>
      )}
    </section>
  )
}
