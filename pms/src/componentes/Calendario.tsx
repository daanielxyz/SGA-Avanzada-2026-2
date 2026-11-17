import { useEffect, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { fechaHoy, sumarDias } from '../formato'
import { aTexto, desdeTexto, nombreDia, nombreMes, primeroDelMes, soloMes } from './fechasCalendario'

const NOMBRES_MES = Array.from({ length: 12 }, (_, m) =>
  soloMes.format(new Date(Date.UTC(2000, m, 1))).replace(/^./, (l) => l.toUpperCase()))
// Empieza el domingo, como el calendario colombiano y el de la web
const DIAS = [['D', 'domingo'], ['L', 'lunes'], ['M', 'martes'], ['M', 'miércoles'], ['J', 'jueves'], ['V', 'viernes'],
  ['S', 'sábado']]

/** Las semanas de un mes, con huecos antes del día 1 para que cada día caiga en su columna. */
function semanasDe(primero: string) {
  const inicio = desdeTexto(primero)
  const dias: (string | null)[] = Array.from({ length: inicio.getUTCDay() }, () => null)
  for (let d = primero; desdeTexto(d).getUTCMonth() === inicio.getUTCMonth(); d = sumarDias(d, 1)) dias.push(d)
  while (dias.length % 7 !== 0) dias.push(null)
  return Array.from({ length: dias.length / 7 }, (_, i) => dias.slice(i * 7, i * 7 + 7))
}

export interface EstadoDia {
  elegible: boolean
  /** Lo que el lector de pantalla dice después de la fecha: «libre», «ya pasó»… */
  texto: string
  /** Clases de más para pintar el día (por ejemplo, `reservada`). */
  clases?: (string | false | undefined)[]
}

/**
 * El calendario del PMS (el diseño de los bloqueos de Apartamentos, DEC-138): arriba lo elegido en palabras, el mes
 * entre dos flechas y los días en una tabla que se maneja con las flechas del teclado. Lo usan los bloqueos, las
 * fechas de una reserva nueva y todo campo de fecha (`CampoFecha`). No sabe de reglas: quien lo usa dice qué días se
 * eligen (`estadoDe`) y qué pasa al elegir uno.
 *
 * @param mes          el primer mes que se ve (AAAA-MM-01); lo guarda quien lo usa, para pedir datos de ese mes
 * @param primerMes    hasta dónde se puede ir hacia atrás (AAAA-MM-01)
 * @param ultimoMes    hasta dónde se puede ir hacia adelante (AAAA-MM-01): el último mes que se puede ver
 * @param inicio,fin   los extremos de lo elegido; entre ellos, las noches del rango
 * @param saltos       el mes y el año como listas, para ir lejos sin pasar mes a mes (una fecha de nacimiento)
 * @param enfocarAlAbrir el foco entra al día elegido (o al primero elegible) al aparecer
 */
export default function Calendario({
  etiqueta, mes, alCambiarMes, primerMes, ultimoMes, mesesVisibles = 1, estadoDe, inicio = '', fin = '', alElegir,
  ayuda, aviso, leyenda, saltos = false, enfocarAlAbrir = false, invalido = false, compacto = false,
}: {
  etiqueta: string
  mes: string
  alCambiarMes: (mes: string) => void
  primerMes: string
  ultimoMes: string
  mesesVisibles?: number
  estadoDe: (fecha: string) => EstadoDia
  inicio?: string
  fin?: string
  alElegir: (fecha: string) => void
  ayuda?: ReactNode
  aviso?: ReactNode
  leyenda?: ReactNode
  saltos?: boolean
  enfocarAlAbrir?: boolean
  invalido?: boolean
  compacto?: boolean
}) {
  const hoy = fechaHoy()
  const meses = Array.from({ length: mesesVisibles }, (_, i) => primeroDelMes(mes, i))
  const ultimoInicio = primeroDelMes(ultimoMes, -(mesesVisibles - 1))
  const [enfocada, setEnfocada] = useState(inicio || hoy)
  const moverFoco = useRef(enfocarAlAbrir)
  const contenedor = useRef<HTMLDivElement>(null)

  const visibles = meses.flatMap((m) => semanasDe(m).flat()).filter((d): d is string => d !== null)
  const elegibles = visibles.filter((d) => estadoDe(d).elegible)
  // Si el día enfocado no se ve, el primero elegible recibe el tabulador: el calendario siempre es alcanzable
  const conTabulador = visibles.includes(enfocada) ? enfocada : elegibles[0] ?? visibles[0]

  useEffect(() => {
    if (!moverFoco.current) return
    moverFoco.current = false
    contenedor.current?.querySelector<HTMLButtonElement>(`[data-fecha="${conTabulador}"]`)?.focus({ preventScroll: true })
  }, [conTabulador, mes])

  function irA(nuevo: string) {
    if (nuevo < primerMes) nuevo = primerMes
    if (nuevo > ultimoInicio) nuevo = ultimoInicio
    alCambiarMes(nuevo)
  }

  function elegir(fecha: string) {
    if (!estadoDe(fecha).elegible) return
    setEnfocada(fecha)
    alElegir(fecha)
  }

  function alTeclear(evento: KeyboardEvent<HTMLButtonElement>, fecha: string) {
    const pasos: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }
    const paso = pasos[evento.key]
    if (paso === undefined) return
    evento.preventDefault()
    const destino = sumarDias(fecha, paso)
    const mesDestino = primeroDelMes(destino)
    if (mesDestino < primerMes || mesDestino > ultimoMes) return
    // Si el día queda fuera de lo que se ve, se corre el calendario
    if (mesDestino < mes) irA(mesDestino)
    if (mesDestino > meses[meses.length - 1]) irA(primeroDelMes(mesDestino, -(mesesVisibles - 1)))
    moverFoco.current = true
    setEnfocada(destino)
  }

  const anios = (() => {
    const desde = desdeTexto(primerMes).getUTCFullYear()
    const hasta = desdeTexto(ultimoMes).getUTCFullYear()
    return Array.from({ length: hasta - desde + 1 }, (_, i) => hasta - i)
  })()
  const actual = desdeTexto(mes)

  return (
    // Con un error, el foco puede llegar aquí (enfocarPrimerError busca aria-invalid) y el lector lee la ayuda
    <div className={['calendario', compacto && 'calendario-compacto', invalido && 'invalido'].filter(Boolean).join(' ')}
      ref={contenedor} role="group" aria-label={etiqueta} aria-invalid={invalido} tabIndex={-1}>
      {ayuda && <p className="calendario-ayuda" aria-live="polite">{ayuda}</p>}
      <div className="calendario-cabecera">
        <button type="button" className="boton boton-secundario boton-chico calendario-flecha" aria-label="Mes anterior"
          disabled={mes <= primerMes} onClick={() => irA(primeroDelMes(mes, -1))}>
          <span aria-hidden="true">←</span>
        </button>
        {saltos && mesesVisibles === 1 ? (
          <span className="calendario-saltos">
            <select className="campo-control" aria-label="Mes" value={actual.getUTCMonth()}
              onChange={(e) => irA(aTexto(new Date(Date.UTC(actual.getUTCFullYear(), Number(e.target.value), 1))))}>
              {NOMBRES_MES.map((nombre, m) => <option key={nombre} value={m}>{nombre}</option>)}
            </select>
            <select className="campo-control" aria-label="Año" value={actual.getUTCFullYear()}
              onChange={(e) => irA(aTexto(new Date(Date.UTC(Number(e.target.value), actual.getUTCMonth(), 1))))}>
              {anios.map((anio) => <option key={anio} value={anio}>{anio}</option>)}
            </select>
          </span>
        ) : (
          meses.map((m) => <strong key={m} className="calendario-mes-nombre">{nombreMes(m)}</strong>)
        )}
        <button type="button" className="boton boton-secundario boton-chico calendario-flecha" aria-label="Mes siguiente"
          disabled={mes >= ultimoInicio} onClick={() => irA(primeroDelMes(mes, 1))}>
          <span aria-hidden="true">→</span>
        </button>
      </div>
      {aviso}
      <div className="calendario-meses" data-meses={mesesVisibles}>
        {meses.map((m) => (
          <table key={m} className="calendario-tabla">
            <caption className="solo-lectores">{nombreMes(m)}</caption>
            <thead>
              <tr>{DIAS.map(([corto, largo]) => <th key={largo} scope="col"><abbr title={largo}>{corto}</abbr></th>)}</tr>
            </thead>
            <tbody>
              {semanasDe(m).map((semana) => (
                <tr key={semana.find(Boolean) ?? m}>
                  {semana.map((fecha, i) => {
                    if (!fecha) return <td key={`vacio-${i}`} />
                    const { elegible, texto, clases = [] } = estadoDe(fecha)
                    const extremo = fecha === inicio || fecha === fin
                    const enRango = inicio !== '' && fin !== '' && fecha > inicio && fecha < fin
                    const clase = ['dia', ...clases, enRango && 'en-rango', extremo && 'extremo',
                      fecha === hoy && 'hoy'].filter(Boolean).join(' ')
                    return (
                      <td key={fecha}>
                        <button type="button" className={clase} data-fecha={fecha} aria-disabled={!elegible}
                          aria-pressed={extremo} tabIndex={fecha === conTabulador ? 0 : -1}
                          aria-label={`${nombreDia.format(desdeTexto(fecha))}, ${texto}`}
                          onClick={() => elegir(fecha)} onKeyDown={(e) => alTeclear(e, fecha)}>
                          {desdeTexto(fecha).getUTCDate()}
                        </button>
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        ))}
      </div>
      {leyenda}
    </div>
  )
}
