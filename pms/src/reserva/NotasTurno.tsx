import { useEffect, useId, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { atenderNota, escribirNota, listarNotas, NOTA_MAXIMA, type Nota } from '../api/notas'
import Aviso from '../componentes/Aviso'
import { fechaYHora } from './llegada/llegada'
import { errorParaMostrar, type ErrorMostrado } from './mensajes'

type Carga = { clave: string; notas: Nota[] } | { clave: string; error: unknown }

/**
 * Notas del turno (BL-08 · DEC-105): lo que un turno le deja dicho al siguiente sobre la reserva. Arriba las
 * pendientes; abajo, plegadas, las atendidas con quién y cuándo. Solo una reserva activa recibe notas y las atiende.
 *
 * @param activa       falso en una reserva terminada: se leen, pero no se escriben ni se atienden
 * @param soloPendientes en la llegada: sin las atendidas, con un enlace al detalle si no hay pendientes
 */
export default function NotasTurno({ codigo, activa, soloPendientes = false }: {
  codigo: string
  activa: boolean
  soloPendientes?: boolean
}) {
  const [intento, setIntento] = useState(0)
  const [carga, setCarga] = useState<Carga | null>(null)
  const [texto, setTexto] = useState('')
  const [faltaTexto, setFaltaTexto] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)
  const [atendiendo, setAtendiendo] = useState<string | null>(null)
  // Si otro turno ya la atendió, la nota pasa a «Atendidas» al releer la lista: el aviso va arriba, no junto a ella
  const [rechazo, setRechazo] = useState<{ texto: string; mensaje: string } | null>(null)
  const id = useId()
  const clave = `${codigo}#${intento}`

  useEffect(() => {
    let vigente = true
    listarNotas(codigo)
      .then((notas) => {
        if (vigente) setCarga({ clave, notas })
      })
      .catch((causa: unknown) => {
        if (vigente) setCarga({ clave, error: causa })
      })
    return () => {
      vigente = false
    }
  }, [codigo, clave])

  // Otro turno pudo escribir o atender notas desde otro equipo: al volver a esta pestaña se releen sin borrar la lista
  useEffect(() => {
    function alVolver() {
      if (document.visibilityState !== 'visible') return
      listarNotas(codigo)
        .then((notas) => setCarga((actual) => (actual && 'notas' in actual ? { clave: actual.clave, notas } : actual)))
        .catch(() => undefined)
    }
    document.addEventListener('visibilitychange', alVolver)
    return () => document.removeEventListener('visibilitychange', alVolver)
  }, [codigo])

  const actual = carga?.clave === clave ? carga : null
  const ponerNotas = (notas: Nota[]) => setCarga({ clave, notas })

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    if (!texto.trim()) {
      setFaltaTexto(true)
      return
    }
    setGuardando(true)
    try {
      ponerNotas(await escribirNota(codigo, texto.trim()))
      setTexto('')
    } catch (causa) {
      setError(errorParaMostrar(causa))
    } finally {
      setGuardando(false)
    }
  }

  async function atender(nota: Nota) {
    setAtendiendo(nota.id)
    setRechazo(null)
    try {
      ponerNotas(await atenderNota(codigo, nota.id))
    } catch (causa) {
      setRechazo({ texto: nota.texto, mensaje: errorParaMostrar(causa).mensaje })
      // Otro turno pudo atenderla: se vuelve a leer para que la lista quede al día
      listarNotas(codigo).then(ponerNotas).catch(() => undefined)
    } finally {
      setAtendiendo(null)
    }
  }

  const titulo = `${id}-titulo`
  return (
    <section className="tarjeta notas" aria-labelledby={titulo}>
      <h2 id={titulo}>Notas del turno</h2>
      {!actual && (
        <div aria-busy="true" className="notas-cargando">
          <span className="fantasma" />
          <span className="fantasma" />
          <p className="suave" role="status">Cargando las notas…</p>
        </div>
      )}
      {actual && 'error' in actual && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar las notas"
          mensaje={actual.error instanceof ErrorApi ? actual.error.message : 'Revisa tu conexión e inténtalo de nuevo.'}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      )}
      {actual && 'notas' in actual && (() => {
        const pendientes = actual.notas.filter((nota) => !nota.atendida)
        const atendidas = actual.notas.filter((nota) => nota.atendida)
        return (
          <>
            {error && <Aviso tipo="error" asunto="No se guardó la nota" mensaje={error.mensaje} detalles={error.detalles} />}
            {rechazo && (
              <Aviso
                tipo="error"
                asunto={`No se marcó «${resumen(rechazo.texto)}»`}
                mensaje={`${rechazo.mensaje} La lista ya está al día.`}
                accion={
                  <button type="button" className="boton boton-secundario boton-chico" onClick={() => setRechazo(null)}>
                    Entendido
                  </button>
                }
              />
            )}
            {actual.notas.length === 0 && !soloPendientes && <p className="suave">Todavía no hay notas.</p>}
            {pendientes.length === 0 && (actual.notas.length > 0 || soloPendientes) && (
              <p className="notas-sin-pendientes">
                Sin notas pendientes.
                {soloPendientes && atendidas.length > 0 && (
                  <Link to={`/reservas/${encodeURIComponent(codigo)}`}>Ver todas en la reserva <span aria-hidden="true">→</span></Link>
                )}
              </p>
            )}
            {pendientes.length > 0 && (
              <div>
                <h3 className="notas-grupo">Pendientes ({pendientes.length})</h3>
                <ul className="notas-lista">
                  {pendientes.map((nota) => (
                    <li key={nota.id} className="nota">
                      <div className="nota-cuerpo">
                        <p className="nota-texto">{nota.texto}</p>
                        <p className="nota-quien">{quien(nota.autorNombre, nota.escritaEn)}</p>
                      </div>
                      {activa && (
                        <button
                          type="button"
                          className="boton boton-secundario boton-chico"
                          aria-label={`Marcar como atendida: ${nota.texto}`}
                          disabled={Boolean(atendiendo)}
                          onClick={() => atender(nota)}
                        >
                          {atendiendo === nota.id ? 'Marcando…' : 'Marcar como atendida'}
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {activa && (
              <form className="notas-formulario" onSubmit={guardar} noValidate>
                <label className="campo-rotulo" htmlFor={`${id}-texto`}>Agregar nota</label>
                <textarea
                  id={`${id}-texto`}
                  className="campo-control campo-texto"
                  rows={2}
                  maxLength={NOTA_MAXIMA}
                  value={texto}
                  placeholder="Ej.: Pidieron cobijas extra"
                  disabled={guardando}
                  aria-invalid={faltaTexto}
                  aria-describedby={`${faltaTexto ? `${id}-error ` : ''}${id}-contador`}
                  onChange={(evento) => {
                    setTexto(evento.target.value)
                    setFaltaTexto(false)
                  }}
                />
                <div className="campo-contador">
                  {faltaTexto && <p className="campo-mensaje" id={`${id}-error`}>Escribe la nota antes de guardarla.</p>}
                  <p className="contador" id={`${id}-contador`}>{texto.length} / {NOTA_MAXIMA}</p>
                </div>
                <div className="notas-pie">
                  {guardando && <p className="suave" role="status">Guardando la nota…</p>}
                  <button type="submit" className="boton boton-secundario" disabled={guardando}>
                    {guardando ? 'Guardando…' : 'Guardar nota'}
                  </button>
                </div>
              </form>
            )}
            {!soloPendientes && atendidas.length > 0 && (
              <details className="notas-atendidas" open={pendientes.length === 0 || undefined}>
                <summary>
                  <span className="notas-flecha" aria-hidden="true">›</span> Atendidas ({atendidas.length})
                </summary>
                <ul className="notas-lista">
                  {atendidas.map((nota) => (
                    <li key={nota.id} className="nota nota-atendida">
                      <div className="nota-cuerpo">
                        <p className="nota-texto">{nota.texto}</p>
                        <p className="nota-quien">Escrita {quien(nota.autorNombre, nota.escritaEn, true)}</p>
                        {nota.atencion && (
                          <p className="nota-quien">
                            <span aria-hidden="true">✓</span> Atendida {quien(nota.atencion.autorNombre, nota.atencion.fechaHora, true)}
                          </p>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              </details>
            )}
            {!activa && actual.notas.length > 0 && (
              <p className="suave">La reserva terminó: sus notas solo se leen.</p>
            )}
          </>
        )
      })()}
    </section>
  )
}

/** «María Constanza · 12 nov 2026, 3:05 p. m.» o, con `por`, «por María Constanza · …». Sin nombre, solo cuándo. */
function quien(nombre: string | null, fecha: string, por = false): string {
  const cuando = fechaYHora(fecha)
  if (!nombre) return por ? `el ${cuando}` : cuando
  return `${por ? 'por ' : ''}${nombre} · ${cuando}`
}

/** Las primeras palabras de una nota, para nombrarla en un aviso sin repetirla entera. */
function resumen(texto: string): string {
  const limpio = texto.replace(/\s+/g, ' ').trim()
  return limpio.length > 40 ? `${limpio.slice(0, 40).trimEnd()}…` : limpio
}
