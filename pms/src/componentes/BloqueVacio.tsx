import type { ReactNode } from 'react'
import Icono, { type NombreIcono } from './Icono'

/** Lo que se ve en lugar de un contenido que no hay: una lista sin resultados o una reserva que no existe. */
export default function BloqueVacio({ icono, titulo, children, accion }: {
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
      <h2>{titulo}</h2>
      <p className="suave">{children}</p>
      {accion}
    </section>
  )
}
