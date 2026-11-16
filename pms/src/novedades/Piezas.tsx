import type { EstadoNovedad, GravedadNovedad } from '../api/novedades'
import { NOMBRE_DE_LA_GRAVEDAD, NOMBRE_DEL_ESTADO } from './logica'

const BARRAS: Record<GravedadNovedad, number> = { ALTA: 3, MEDIA: 2, BAJA: 1 }
const SIMBOLO: Record<EstadoNovedad, string> = { ABIERTA: '○', EN_REVISION: '◐', CERRADA: '✓' }

/** La gravedad con tres barras (llenas según cuánto pesa) y su palabra: nunca solo el color. */
export function EtiquetaGravedad({ gravedad }: { gravedad: GravedadNovedad }) {
  const llenas = BARRAS[gravedad] ?? 0
  return (
    <span className={`etiqueta gravedad gravedad-${gravedad.toLowerCase()}`}>
      <span className="solo-lectores">Gravedad: </span>
      <svg className="gravedad-barras" viewBox="0 0 12 12" aria-hidden="true">
        {[0, 1, 2].map((i) => (
          <rect key={i} x={1 + i * 4} y={8 - i * 3} width="2.5" height={3 + i * 3} rx="0.5"
            className={i < llenas ? 'llena' : 'vacia'} />
        ))}
      </svg>
      {NOMBRE_DE_LA_GRAVEDAD[gravedad] ?? gravedad}
    </span>
  )
}

/** «○ Abierta», «◐ En revisión», «✓ Cerrada». */
export function EtiquetaEstadoNovedad({ estado }: { estado: EstadoNovedad }) {
  return (
    <span className={`etiqueta novedad-${estado.toLowerCase().replace('_', '-')}`}>
      <span className="solo-lectores">Estado: </span>
      <span aria-hidden="true">{SIMBOLO[estado]}</span> {NOMBRE_DEL_ESTADO[estado] ?? estado}
    </span>
  )
}
