import PaginaEstado from '../componentes/PaginaEstado'

/** Se muestra cuando alguien abre la dirección de una pantalla que su rol no puede usar (403 `SIN_PERMISO`). */
export default function SinPermiso() {
  return (
    <PaginaEstado
      icono="candado"
      titulo="No tienes permiso para ver esta pantalla"
      texto="Pídele al administrador que revise tu rol."
    />
  )
}
