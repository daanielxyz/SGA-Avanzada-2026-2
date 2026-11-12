import type { ReactNode } from 'react'
import { NOMBRE_ESTADO_OPERATIVO } from './estadosApartamento'

// Una forma por estado operativo: el color acompaña, pero no es lo único que los separa
const FORMAS: Partial<Record<string, ReactNode>> = {
  PREPARADO: <path d="M2 6.5 4.8 9.5 10 3" />,
  OCUPADO: <circle cx="6" cy="6" r="3" className="relleno" />,
  PENDIENTE_PREPARACION: <path d="M6 1a5 5 0 0 1 0 10z" className="relleno" />,
  EN_PREPARACION: <path d="M6 2a4 4 0 1 1-4 4M6 3.5V6l1.8 1" />,
  FUERA_DE_SERVICIO: <path d="M6 1.5a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9zM2.8 2.8l6.4 6.4" />,
}

/** El estado operativo de un apartamento con su forma y su nombre. `etiqueta` lo dibuja como píldora. */
export default function EstadoApartamento({ estado, etiqueta = false }: { estado: string; etiqueta?: boolean }) {
  const clase = `estado-apartamento estado-apartamento-${estado.toLowerCase().replaceAll('_', '-')}`
  return (
    <span className={etiqueta ? `etiqueta ${clase}` : `ocupacion-estado ${clase}`}>
      <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
        {FORMAS[estado] ?? <circle cx="6" cy="6" r="2" className="relleno" />}
      </svg>
      {NOMBRE_ESTADO_OPERATIVO[estado] ?? estado}
    </span>
  )
}
