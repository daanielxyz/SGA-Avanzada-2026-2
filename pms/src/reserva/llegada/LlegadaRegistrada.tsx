import type { Estancia } from '../../api/reservas'
import Dato from '../../componentes/Dato'
import { fechaYHora } from './llegada'

/** «por Ana Gómez»; si esa cuenta ya no existe, sin nombre. */
function quien(nombre: string | null | undefined): string {
  return nombre ? ` por ${nombre}` : ''
}

/**
 * Quién registró la llegada, a qué hora, si se corrigió y por qué entró sin pagar; y la salida, si la hubo (DEC-114).
 * Va dentro del `dl` de «Datos de la reserva». Si no se pudo leer, lo dice sin tapar el resto del detalle.
 */
export default function LlegadaRegistrada({ estancia, error, alReintentar }: {
  estancia: Estancia | null
  error: boolean
  alReintentar: () => void
}) {
  if (error) {
    return (
      <Dato nombre="Llegada">
        <span className="campo-mensaje">No pudimos leer el registro de llegada.</span>
        <button type="button" className="boton boton-secundario boton-chico dato-accion" onClick={alReintentar}>Reintentar</button>
      </Dato>
    )
  }
  if (!estancia) {
    return <Dato nombre="Llegada"><span className="suave">Cargando…</span></Dato>
  }
  const registros = estancia.registros ?? []
  const vigente = registros.find((registro) => !registro.anulado)
  if (!vigente) return null
  const anteriores = registros.filter((registro) => registro.anulado)
  return (
    <>
      <Dato nombre="Llegada">
        {fechaYHora(vigente.fechaHora)}{quien(vigente.autorNombre)}
        {vigente.motivoCorreccion && (
          <span className="dato-nota">
            Corregida: {vigente.motivoCorreccion}
            {anteriores.length > 0 && ` (antes decía ${anteriores.map((r) => fechaYHora(r.fechaHora)).join('; ')})`}
          </span>
        )}
      </Dato>
      {vigente.motivoSinPago && <Dato nombre="Entró sin pagar la estancia">{vigente.motivoSinPago}</Dato>}
      {estancia.salida && (
        <Dato nombre="Salida registrada">{fechaYHora(estancia.salida.fechaHora)}{quien(estancia.salida.autorNombre)}</Dato>
      )}
    </>
  )
}
