import type { Titular } from '../api/titulares'
import { textoValidacion } from './logica'

/**
 * La identidad del titular (DEC-98): «✓ Validada» o «Sin validar» con borde punteado; nunca solo el color. Completa,
 * junto al título del detalle, dice cuándo y quién la validó.
 */
export default function EstadoIdentidad({ validado, titular, corto = false }: {
  validado: boolean
  titular?: Pick<Titular, 'validadoEn' | 'validadoPorNombre'>
  corto?: boolean
}) {
  if (!validado) return <span className="etiqueta identidad-sin-validar">Sin validar</span>
  return (
    <span className="etiqueta identidad-validada">
      <span aria-hidden="true">✓</span> {corto || !titular ? 'Validada' : textoValidacion(titular)}
    </span>
  )
}
