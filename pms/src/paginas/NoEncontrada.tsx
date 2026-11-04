import PaginaEstado from '../componentes/PaginaEstado'

/** Se muestra cuando la dirección no corresponde a ninguna pantalla. */
export default function NoEncontrada() {
  return (
    <PaginaEstado
      icono="noEncontrada"
      titulo="Página no encontrada"
      texto="La ruta que buscas no existe o cambió de lugar."
      accion="Ir al inicio"
    />
  )
}
