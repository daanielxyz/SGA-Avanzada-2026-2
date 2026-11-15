import { useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { Link, useLocation } from 'react-router'
import type { RespuestaAsistente } from '../api/asistente'
import Icono from '../componentes/Icono'
import { useSesion } from '../sesion/contexto'
import { ID_BOTON_AYUDA, ID_PANEL_AYUDA } from './BotonAyuda'
import { useAsistente, type ValorAsistente } from './contexto'
import { errorDePregunta, LARGO_CONTADOR, LARGO_MAXIMO, rutaDeFuente, SUGERENCIAS, type Intercambio } from './logica'
import TextoRespuesta from './TextoRespuesta'

const MILES = new Intl.NumberFormat('es-CO')
/** Pasado este tiempo esperando, se avisa que puede tardar un poco más (administración tarda hasta unos 10 s). */
const ESPERA_LARGA_MS = 4000
/** El mismo ancho desde el que el menú queda fijo y el panel va al lado del PMS (`src/index.css`). */
const CELULAR = '(max-width: 799.98px)'

/**
 * El panel de ayuda del PMS (DEC-127, mockups S5Asistente): a la derecha del PMS en el computador, sin velo, y a
 * pantalla completa en el celular. Solo explica: preguntar, empezar de nuevo y cerrar.
 */
export default function PanelAsistente() {
  const asistente = useAsistente()
  if (!asistente?.abierto) return null
  return <Contenido asistente={asistente} />
}

function Contenido({ asistente }: { asistente: ValorAsistente }) {
  const { intercambios, cerrar, preguntar, reintentar, empezarDeNuevo } = asistente
  const { sesion } = useSesion()
  const conversacion = useRef<HTMLDivElement>(null)
  const [enfoque, setEnfoque] = useState(0)
  const idTitulo = useId()

  // Al cerrar, el foco vuelve a donde estaba (por ejemplo, «Ayuda» o «¿Por qué?»); al abrir lo toma el campo (Pie)
  useEffect(() => {
    const antes = document.activeElement instanceof HTMLElement ? document.activeElement : null
    return () => {
      const destino = antes && antes.isConnected ? antes : document.getElementById(ID_BOTON_AYUDA)
      destino?.focus()
    }
  }, [])

  // Lo último queda a la vista
  const cuantos = intercambios.length
  const ultimoEstado = intercambios.at(-1)?.estado
  useEffect(() => {
    const caja = conversacion.current
    if (caja) caja.scrollTop = caja.scrollHeight
  }, [cuantos, ultimoEstado])

  function alTeclear(evento: KeyboardEvent<HTMLElement>) {
    if (evento.key === 'Escape') cerrar()
  }

  // En el celular el panel tapa el PMS: al seguir una fuente se cierra para ver la pantalla
  const alSeguirFuente = () => {
    if (window.matchMedia?.(CELULAR).matches) cerrar()
  }

  return (
    <aside id={ID_PANEL_AYUDA} className="asistente" aria-labelledby={idTitulo} onKeyDown={alTeclear}>
      <div className="asistente-cabecera">
        <h2 id={idTitulo}>Ayuda del PMS</h2>
        {intercambios.length > 0 && (
          <button type="button" className="boton boton-secundario boton-chico" onClick={() => {
            empezarDeNuevo()
            setEnfoque((n) => n + 1)
          }}>
            Empezar de nuevo
          </button>
        )}
        <button type="button" className="boton boton-secundario boton-chico asistente-cerrar" aria-label="Cerrar la ayuda"
          onClick={cerrar}>
          <Icono nombre="cerrar" />
          <span className="asistente-cerrar-texto" aria-hidden="true">Cerrar</span>
        </button>
      </div>

      <div className="asistente-conversacion" ref={conversacion} role="log" aria-live="polite" aria-label="Conversación">
        {intercambios.length === 0 && sesion && (
          <div className="asistente-inicio">
            <h3>¿En qué te ayudo?</h3>
            <ul className="asistente-sugerencias">
              {SUGERENCIAS[sesion.rol].map((sugerencia) => (
                <li key={sugerencia}>
                  <button type="button" className="boton boton-secundario boton-bloque asistente-sugerencia"
                    onClick={() => preguntar(sugerencia)}>
                    {sugerencia}
                  </button>
                </li>
              ))}
            </ul>
            <p className="asistente-nota">
              Respondo con el manual del PMS. Solo explico: no hago cambios en el sistema. La conversación se borra al
              recargar la página o cerrar sesión.
            </p>
          </div>
        )}
        {intercambios.map((intercambio) => (
          <IntercambioVisto key={intercambio.id} intercambio={intercambio} alReintentar={reintentar}
            alSeguirFuente={alSeguirFuente} />
        ))}
      </div>

      <Pie enfoque={enfoque} esperando={intercambios.some((i) => i.estado === 'esperando')} alPreguntar={preguntar} />
    </aside>
  )
}

function IntercambioVisto({ intercambio, alReintentar, alSeguirFuente }: {
  intercambio: Intercambio
  alReintentar: (id: number) => void
  alSeguirFuente: () => void
}) {
  const fallida = intercambio.estado === 'fallida'
  return (
    <>
      <p className={fallida ? 'asistente-pregunta asistente-pregunta-fallida' : 'asistente-pregunta'}>
        <span className="solo-lectores">Tu pregunta: </span>
        {intercambio.pregunta}
      </p>
      {intercambio.estado === 'esperando' && <Esperando />}
      {fallida && (
        <div className="asistente-falla">
          <p className="campo-mensaje" role="alert">{intercambio.error}</p>
          <button type="button" className="boton boton-secundario boton-chico" onClick={() => alReintentar(intercambio.id)}>
            Reintentar
          </button>
        </div>
      )}
      {intercambio.estado === 'respondida' && intercambio.respuesta && (
        <Respuesta respuesta={intercambio.respuesta} alSeguirFuente={alSeguirFuente} />
      )}
    </>
  )
}

function Esperando() {
  const [larga, setLarga] = useState(false)
  useEffect(() => {
    const espera = window.setTimeout(() => setLarga(true), ESPERA_LARGA_MS)
    return () => window.clearTimeout(espera)
  }, [])
  return (
    <p className="asistente-espera" role="status">
      <Icono nombre="buscar" />
      {larga ? 'Sigo buscando en el manual; puede tardar unos segundos más…' : 'Buscando en el manual…'}
    </p>
  )
}

function Respuesta({ respuesta, alSeguirFuente }: { respuesta: RespuestaAsistente; alSeguirFuente: () => void }) {
  const { pathname } = useLocation()
  return (
    <div className="asistente-respuesta">
      <span className="solo-lectores">Respuesta: </span>
      {respuesta.aviso && (
        <div className="aviso aviso-info asistente-aviso">
          <span className="aviso-sello" aria-hidden="true">i</span>
          <p>{respuesta.aviso}</p>
        </div>
      )}
      <TextoRespuesta texto={respuesta.respuesta} />
      {respuesta.fuentes.length > 0 && (
        <div className="asistente-fuentes">
          <p className="asistente-fuentes-titulo">Según el manual:</p>
          <ul>
            {respuesta.fuentes.map((fuente) => {
              const ruta = rutaDeFuente(fuente.pantalla, pathname)
              return (
                <li key={fuente.id}>
                  <Icono nombre="documento" />
                  {ruta ? (
                    <Link to={ruta} onClick={alSeguirFuente}>
                      {fuente.titulo} <span aria-hidden="true">→</span>
                    </Link>
                  ) : (
                    <span>{fuente.titulo}</span>
                  )}
                </li>
              )
            })}
          </ul>
        </div>
      )}
    </div>
  )
}

/** @param enfoque cada vez que cambia, el foco vuelve al campo (al abrir y al empezar de nuevo) */
function Pie({ enfoque, esperando, alPreguntar }: {
  enfoque: number
  esperando: boolean
  alPreguntar: (pregunta: string) => void
}) {
  const campo = useRef<HTMLTextAreaElement>(null)
  const [texto, setTexto] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    campo.current?.focus()
  }, [enfoque])
  const idPista = useId()
  const idError = useId()
  const largo = texto.length

  // El campo crece con el texto hasta unas cinco líneas; después se desplaza (el tope lo pone el CSS)
  useEffect(() => {
    const control = campo.current
    if (!control) return
    control.style.height = 'auto'
    control.style.height = `${control.scrollHeight}px`
  }, [texto])

  function enviar(evento?: FormEvent) {
    evento?.preventDefault()
    if (esperando) return
    const encontrado = errorDePregunta(texto)
    setError(encontrado)
    if (encontrado) {
      campo.current?.focus()
      return
    }
    alPreguntar(texto.trim())
    setTexto('')
  }

  function alTeclear(evento: KeyboardEvent<HTMLTextAreaElement>) {
    if (evento.key === 'Enter' && !evento.shiftKey && !evento.nativeEvent.isComposing) {
      evento.preventDefault()
      enviar()
    }
  }

  return (
    <form className="asistente-pie" onSubmit={enviar} noValidate>
      <div className="asistente-preguntar">
        <label className="solo-lectores" htmlFor={`${idPista}-campo`}>Tu pregunta</label>
        <textarea
          id={`${idPista}-campo`}
          ref={campo}
          className="campo-control asistente-campo"
          rows={1}
          placeholder="Escribe tu pregunta"
          value={texto}
          aria-invalid={Boolean(error)}
          aria-describedby={[error ? idError : null, idPista].filter(Boolean).join(' ')}
          onChange={(evento) => {
            setTexto(evento.target.value)
            if (error) setError(null)
          }}
          onKeyDown={alTeclear}
        />
        <button type="submit" className="boton" disabled={esperando}>Preguntar</button>
      </div>
      {error && <p className="campo-mensaje" id={idError}>{error}</p>}
      <div className="asistente-pie-pistas">
        <p className="campo-pista" id={idPista}>No escribas datos de huéspedes: nombres, documentos, correos ni teléfonos.</p>
        {largo > LARGO_CONTADOR && (
          <p className={largo > LARGO_MAXIMO ? 'asistente-contador asistente-contador-pasado' : 'asistente-contador'}>
            {MILES.format(largo)} / {MILES.format(LARGO_MAXIMO)}
          </p>
        )}
      </div>
    </form>
  )
}
