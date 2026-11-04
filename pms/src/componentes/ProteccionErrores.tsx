import { Component, type ReactNode } from 'react'
import { useLocation } from 'react-router'
import PaginaEstado from './PaginaEstado'

class Frontera extends Component<{ recargar: boolean; children: ReactNode }, { fallo: boolean }> {
  state = { fallo: false }

  static getDerivedStateFromError() {
    return { fallo: true }
  }

  render() {
    if (!this.state.fallo) return this.props.children
    return (
      <PaginaEstado
        icono="alerta"
        titulo="Algo salió mal"
        texto="No pudimos mostrar esta pantalla. Vuelve al inicio e inténtalo de nuevo."
        recargar={this.props.recargar}
      />
    )
  }
}

/**
 * Evita que un dato inesperado deje todo el PMS en blanco.
 *
 * @param global protege la aplicación entera: como también pudo fallar el marco, volver al inicio recarga la página.
 *               Sin él protege solo la pantalla, y al cambiar de dirección esa pantalla se vuelve a intentar.
 */
export default function ProteccionErrores({ global = false, children }: { global?: boolean; children: ReactNode }) {
  const { pathname } = useLocation()
  return (
    <Frontera key={global ? 'global' : pathname} recargar={global}>
      {children}
    </Frontera>
  )
}
