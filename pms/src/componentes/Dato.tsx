import type { ReactNode } from 'react'

/** Un nombre con su valor, dentro de un `dl` con la clase `datos`. */
export default function Dato({ nombre, codigo = false, children }: {
  nombre: string
  codigo?: boolean
  children: ReactNode
}) {
  return (
    <div className="dato">
      <dt>{nombre}</dt>
      <dd className={codigo ? 'codigo' : undefined}>{children}</dd>
    </div>
  )
}
