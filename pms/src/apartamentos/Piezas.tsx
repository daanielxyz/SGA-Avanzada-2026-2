// Piezas que comparten la lista y el detalle de apartamentos.
import { useState } from 'react'

/**
 * Una foto por su dirección. Sin dirección, o si no carga, un recuadro punteado con el ícono de imagen: nunca una
 * imagen rota.
 */
export function Foto({ url, alt = '', clase = 'miniatura' }: { url?: string; alt?: string; clase?: string }) {
  const [fallo, setFallo] = useState<string | null>(null)
  if (!url || fallo === url) {
    return (
      <span className={`${clase} foto-vacia`} role="img" aria-label={alt ? `${alt}: sin foto` : 'Sin foto'}>
        <svg viewBox="0 0 24 24" aria-hidden="true" className="icono">
          <rect x="3" y="5" width="18" height="14" rx="2" />
          <circle cx="9" cy="10" r="1.6" />
          <path d="m4 17 5-5 4 4 3-3 4 4" />
        </svg>
      </span>
    )
  }
  return <img className={clase} src={url} alt={alt} loading="lazy" onError={() => setFallo(url)} />
}

/** «✓ A la venta» o «○ Retirado»: la forma y la palabra, no solo el color. */
export function EtiquetaVenta({ activo }: { activo: boolean }) {
  return (
    <span className={`etiqueta ${activo ? 'venta-activa' : 'venta-retirada'}`}>
      <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
        {activo ? <path d="M2 6.5 4.8 9.5 10 3" /> : <circle cx="6" cy="6" r="4" />}
      </svg>
      {activo ? 'A la venta' : 'Retirado'}
    </span>
  )
}
