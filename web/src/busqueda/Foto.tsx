import { useState } from 'react'

interface Props {
  url: string | undefined
  alt: string
  carga?: 'eager' | 'lazy'
}

/**
 * Una foto del apartamento. Si no tiene o no carga (el backend acepta cero fotos y una dirección puede dejar de
 * existir), queda un recuadro gris del mismo tamaño, para que la fila o la galería no se desarmen.
 */
export default function Foto({ url, alt, carga = 'lazy' }: Props) {
  const [fallo, setFallo] = useState(false)
  if (!url || fallo) {
    return <div className="sin-foto" aria-hidden="true" />
  }
  return <img src={url} alt={alt} loading={carga} onError={() => setFallo(true)} />
}
