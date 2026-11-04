import { useState, type FormEvent } from 'react'
import { ErrorApi } from '../api/cliente'
import type { Folio } from '../api/folio'
import { confirmarReserva, indicarHoraLlegada, type Reserva } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import { dinero } from '../formato'
import FormularioPago from './FormularioPago'
import { faltaParaAnticipo, mediosDeRecepcion } from './pago'
import ResumenDialogo from './ResumenDialogo'
import type { DatosDePago } from './useDatosDePago'

/**
 * Confirmar (RN-08 · RN-09 · RES-15). Para la persona es una sola acción; por dentro guarda primero la hora, si cambió,
 * y luego confirma. Si falta el anticipo, el pago se registra aquí mismo, reemplazando el contenido de la ventana.
 *
 * @param alConfirmar la reserva quedó confirmada
 * @param alFallar    algo falló: la reserva pudo cambiar, así que hay que volver a leerla
 */
export default function DialogoConfirmar({ reserva, datos, alCerrar, alConfirmar, alFallar, alRegistrarPago }: {
  reserva: Reserva
  datos: DatosDePago | null
  alCerrar: () => void
  alConfirmar: () => void | Promise<void>
  alFallar: () => void
  alRegistrarPago: (folio: Folio) => void
}) {
  const horaGuardada = reserva.horaEstimadaLlegada?.slice(0, 5) ?? ''
  const [hora, setHora] = useState(horaGuardada)
  const [enPago, setEnPago] = useState(false)
  const [pagoRegistrado, setPagoRegistrado] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<{ mensaje: string; detalles: string[] } | null>(null)

  const falta = datos ? faltaParaAnticipo(reserva, datos.folio, datos.alojamiento) : 0
  const exigeAnticipo = Boolean(datos && datos.alojamiento.parametros.anticipoPct > 0)

  if (enPago && datos) {
    return (
      <Dialogo
        titulo="Registrar pago"
        subtitulo={<>{reserva.codigo} · Falta para el anticipo <span className="codigo">{dinero(falta)}</span></>}
        ocupado={enviando}
        alCerrar={alCerrar}
      >
        <FormularioPago
          codigo={reserva.codigo}
          medios={mediosDeRecepcion(datos.alojamiento)}
          montoInicial={falta}
          textoVolver="Volver a confirmar"
          alVolver={() => setEnPago(false)}
          alEnviar={setEnviando}
          alRegistrar={(folio) => {
            alRegistrarPago(folio)
            setPagoRegistrado(true)
            setEnPago(false)
          }}
        />
      </Dialogo>
    )
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      if (hora !== horaGuardada) await indicarHoraLlegada(reserva.codigo, hora)
      await confirmarReserva(reserva.codigo)
      await alConfirmar()
    } catch (causa) {
      setError(causa instanceof ErrorApi
        ? { mensaje: causa.message, detalles: causa.detalles }
        : { mensaje: 'Inténtalo de nuevo en un momento.', detalles: [] })
      alFallar()
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Dialogo
      titulo="Confirmar reserva"
      subtitulo="Revisa los datos antes de confirmar."
      ocupado={enviando}
      alCerrar={alCerrar}
    >
      <form className="dialogo-contenido" onSubmit={enviar} noValidate>
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto="No se pudo confirmar" mensaje={error.mensaje} detalles={error.detalles} />}
          {pagoRegistrado && (
            <Aviso
              tipo="exito"
              asunto="Pago registrado"
              mensaje={falta > 0 ? 'Todavía falta para cubrir el anticipo.' : 'El anticipo quedó cubierto.'}
            />
          )}
          <ResumenDialogo reserva={reserva} />
          <div>
            <label className="campo-rotulo" htmlFor="confirmar-hora">
              Hora estimada de llegada <span className="campo-rotulo-nota">(obligatoria)</span>
            </label>
            <input
              id="confirmar-hora"
              type="time"
              className="campo-control"
              required
              data-foco-inicial
              value={hora}
              disabled={enviando}
              aria-describedby={horaGuardada ? 'confirmar-hora-pista' : undefined}
              onChange={(evento) => setHora(evento.target.value)}
            />
            {horaGuardada && <p className="campo-pista" id="confirmar-hora-pista">Puedes cambiarla.</p>}
          </div>
          {falta > 0 && (
            <Aviso
              tipo="advertencia"
              asunto="Falta cubrir el anticipo"
              mensaje={`Faltan ${dinero(falta)} para confirmar.`}
              accion={
                <button
                  type="button"
                  className="boton boton-secundario boton-chico"
                  disabled={enviando}
                  onClick={() => setEnPago(true)}
                >
                  Registrar pago
                </button>
              }
            />
          )}
          {exigeAnticipo && falta === 0 && datos && (
            <p className="campo-pista">
              <span aria-hidden="true">✓ </span>Anticipo cubierto · pagado{' '}
              <span className="codigo">{dinero(datos.folio.totalPagado)}</span>
            </p>
          )}
          {enviando && <p className="suave" role="status">Confirmando…</p>}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>
            Volver
          </button>
          <button
            type="submit"
            className="boton"
            disabled={enviando || !hora || falta > 0 || reserva.estado !== 'PENDIENTE'}
          >
            {enviando ? 'Confirmando…' : 'Confirmar reserva'}
          </button>
        </div>
      </form>
    </Dialogo>
  )
}
