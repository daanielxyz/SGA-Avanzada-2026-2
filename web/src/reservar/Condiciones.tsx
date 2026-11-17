import type { Penalizacion, PoliticaCancelacion } from '../api/reservas'
import { enPesos } from '../formato'

/** La sección del paso de pago: la política de cancelación con su título (DEC-41). */
export default function Condiciones({ politica }: { politica: PoliticaCancelacion }) {
  return (
    <section aria-labelledby="titulo-condiciones" className="condiciones">
      <h2 id="titulo-condiciones">Si necesitas cancelar</h2>
      <ListaCondiciones politica={politica} />
    </section>
  )
}

/**
 * La política de cancelación del hotel en palabras (DEC-41): cuánto se retiene según la antelación y si no llega. La
 * usan el paso de pago y la presentación del hotel, con el mismo texto.
 */
export function ListaCondiciones({ politica }: { politica: PoliticaCancelacion }) {
  // De la mayor antelación a la menor: se lee en el orden en que pasa el tiempo
  const tramos = [...politica.tramos].sort((a, b) => b.antelacionMinHoras - a.antelacionMinHoras)
  return (
    <ul>
      {tramos.map((t, i) => (
        <li key={t.antelacionMinHoras}>
          <strong>{plazo(t.antelacionMinHoras, tramos[i - 1]?.antelacionMinHoras)}:</strong>{' '}
          {retencion(t.penalizacion)}
        </li>
      ))}
      <li><strong>Si no llegas:</strong> {retencion(politica.penalizacionNoShow)}</li>
    </ul>
  )
}

// Cada tramo vale desde su antelación mínima hasta la del tramo anterior, que es mayor
function plazo(horas: number, horasDelAnterior: number | undefined) {
  if (horasDelAnterior === undefined) {
    return horas > 0 ? `Con ${horas} horas o más de antelación` : 'Hasta la llegada'
  }
  return horas > 0 ? `Entre ${horas} y ${horasDelAnterior} horas antes` : `Con menos de ${horasDelAnterior} horas`
}

const SOBRE: Record<Penalizacion['base'], string> = {
  VALOR_TOTAL: 'del total de la reserva',
  PAGADO: 'de lo que hayas pagado',
  ANTICIPO_EXIGIDO: 'del anticipo',
}

// DEC-41: cada hotel la define por porcentaje o por monto fijo
function retencion({ base, porcentaje, montoFijo }: Penalizacion) {
  if (montoFijo !== null) {
    return montoFijo > 0 ? `se retienen ${enPesos(montoFijo)}.` : 'no se retiene nada.'
  }
  return porcentaje ? `se retiene el ${porcentaje} % ${SOBRE[base]}.` : 'no se retiene nada.'
}
