import type { Politica } from '../api/politicas'
import { textoRetencion, textoTramo, tramosOrdenados } from '../reserva/politica'

/**
 * La política en frases, de la mayor antelación a la menor, y el no-show aparte (RN-13 · DEC-104). La usan «Ver
 * política» del detalle, la pantalla de la política y su vista previa, para que las tres digan lo mismo.
 */
export default function PoliticaContada({ politica, nivel = 3 }: { politica: Politica; nivel?: 3 | 4 }) {
  const tramos = tramosOrdenados(politica)
  const Titulo = nivel === 3 ? 'h3' : 'h4'
  return (
    <>
      <Titulo className="notas-grupo">Si se cancela</Titulo>
      <ul className="tramos">
        {tramos.map((tramo, indice) => (
          <li key={`${tramo.antelacionMinHoras}-${indice}`}>
            <span>{textoTramo(tramos, indice)}</span>
            <strong>{textoRetencion(tramo.penalizacion)}</strong>
          </li>
        ))}
      </ul>
      <Titulo className="notas-grupo">Si no se presenta</Titulo>
      <ul className="tramos">
        <li>
          <span>El huésped no llega</span>
          <strong>{textoRetencion(politica.penalizacionNoShow)}</strong>
        </li>
      </ul>
    </>
  )
}
