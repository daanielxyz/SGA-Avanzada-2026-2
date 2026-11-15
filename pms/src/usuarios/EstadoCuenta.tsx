import type { Usuario } from '../api/usuarios'
import { estadoCuenta } from './logica'

const ESTADOS = {
  activa: { simbolo: '✓', texto: 'Activa' },
  'sin-activar': { simbolo: '◐', texto: 'Sin activar' },
  eliminada: { simbolo: '✕', texto: 'Eliminada' },
} as const

/**
 * Un correo que, si no cabe, parte después de la arroba y antes de cada punto, no en mitad de una palabra.
 */
export function Correo({ correo }: { correo: string }) {
  const [usuario, dominio = ''] = correo.split('@')
  const conPuntos = (texto: string) => texto.split('.').map((parte, i) => (
    <span key={i}>{i > 0 && <><wbr />.</>}{parte}</span>
  ))
  return (
    <span className="correo">
      {conPuntos(usuario)}{dominio && <>@<wbr />{conPuntos(dominio)}</>}
    </span>
  )
}

/** «✓ Activa», «◐ Sin activar» o «✕ Eliminada»: el símbolo y la palabra, no solo el color. */
export default function EstadoCuenta({ usuario }: { usuario: Pick<Usuario, 'activado' | 'eliminado'> }) {
  const estado = estadoCuenta(usuario)
  const { simbolo, texto } = ESTADOS[estado]
  return (
    <span className={`etiqueta cuenta-${estado}`}>
      <span aria-hidden="true">{simbolo}</span> {texto}
    </span>
  )
}
