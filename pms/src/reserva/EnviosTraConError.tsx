import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { listarEnviosTra, type EnvioTra } from '../api/tra'
import type { Pagina } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import { cantidad } from '../formato'

/**
 * Aviso del listado de reservas cuando el TRA de alguna no llegó al Ministerio (DEC-130): cuántas y un enlace a cada una
 * (las 10 más recientes), para que ninguna se pierda. Si no hay o no se pudo consultar, no se muestra nada: no estorba
 * el listado.
 */
export default function EnviosTraConError() {
  const [pagina, setPagina] = useState<Pagina<EnvioTra> | null>(null)

  useEffect(() => {
    let vigente = true
    listarEnviosTra('CON_ERROR').then((encontrada) => vigente && setPagina(encontrada)).catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [])

  if (!pagina || pagina.totalElementos === 0) return null
  const total = pagina.totalElementos
  return (
    <Aviso
      tipo="advertencia"
      asunto={total === 1 ? 'El TRA de una reserva no llegó al Ministerio' : `El TRA de ${total} reservas no llegó al Ministerio`}
      mensaje="Abre cada una para ver el motivo y reintentar."
      accion={
        <ul className="envios-tra-error" aria-label="Reservas con el TRA sin enviar">
          {pagina.contenido.map((envio) => (
            <li key={envio.reserva}>
              <Link className="codigo" to={`/reservas/${encodeURIComponent(envio.reserva)}`}>{envio.reserva}</Link>
            </li>
          ))}
          {total > pagina.contenido.length && <li className="suave">y {cantidad(total - pagina.contenido.length, 'más', 'más')}</li>}
        </ul>
      }
    />
  )
}
