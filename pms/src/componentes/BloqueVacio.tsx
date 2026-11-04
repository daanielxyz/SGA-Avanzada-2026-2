import type { ReactNode } from 'react'
import Icono, { type NombreIcono } from './Icono'

/** Lo que se ve en lugar de un contenido que no hay: una lista sin resultados o una reserva que no existe. */
export default function BloqueVacio({ encabezado: Encabezado = 'h2', icono, titulo, children, accion }: {
  /** `h1` cuando el bloque es toda la pantalla; `h2` cuando va dentro de otra. */
  encabezado?: 'h1' | 'h2'
  icono: NombreIcono
  titulo: string
  children: ReactNode
  accion?: ReactNode
}) {
  return (
    <section className="tarjeta bloque-vacio">
      <div className="icono-estado">
        <Icono nombre={icono} />
      </div>
      <Encabezado>{titulo}</Encabezado>
      <p className="suave">{children}</p>
      {accion}
    </section>
  )
}
