import { useState, type FormEvent } from 'react'
import { ErrorApi } from '../api/cliente'
import { registrarDevolucion, registrarPago, type Folio } from '../api/folio'
import Aviso from '../componentes/Aviso'
import { fechaCorta, fechaHoy, nombreLegible } from '../formato'
import { mensajeDeRechazo } from './mensajes'

const MILES = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 })

type Errores = Partial<Record<'medio' | 'monto' | 'fecha', string>>

const TEXTOS = {
  pago: {
    llamar: registrarPago,
    boton: 'Registrar pago',
    enviando: 'Registrando el pago…',
    rechazo: 'No se registró el pago',
    fecha: 'Escribe la fecha del pago.',
  },
  devolucion: {
    llamar: registrarDevolucion,
    boton: 'Registrar devolución',
    enviando: 'Registrando la devolución…',
    rechazo: 'No se registró la devolución',
    fecha: 'Escribe la fecha de la devolución.',
  },
} as const

/**
 * Contenido de «Registrar pago» o «Registrar devolución» dentro de un `Dialogo` (PAG-02 · PAG-06 · PAG-07 · MPAG-02).
 * Va aparte de la ventana para usarse solo o dentro de otro diálogo. Los errores de formato salen junto al campo; lo
 * que rechace el backend, arriba.
 *
 * @param tipo         una devolución registra el dinero que ya se le entregó al huésped por fuera del sistema
 * @param montoInicial lo que se propone pagar; 0 deja el campo vacío
 * @param alRegistrar  recibe el folio ya actualizado
 * @param alEnviar     avisa si hay un envío en curso, para que la ventana no se cierre
 */
export default function FormularioPago({
  tipo = 'pago', codigo, medios, montoInicial, textoVolver, alVolver, alRegistrar, alEnviar,
}: {
  tipo?: keyof typeof TEXTOS
  codigo: string
  medios: string[]
  montoInicial: number
  textoVolver: string
  alVolver: () => void
  alRegistrar: (folio: Folio) => void
  alEnviar: (enviando: boolean) => void
}) {
  const textos = TEXTOS[tipo]
  const hoy = fechaHoy()
  const [medio, setMedio] = useState(medios[0] ?? '')
  const [monto, setMonto] = useState(montoInicial > 0 ? String(montoInicial) : '')
  const [fecha, setFecha] = useState(hoy)
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState<Errores>({})
  const [error, setError] = useState<{ mensaje: string; detalles: string[] } | null>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const encontrados: Errores = {}
    if (!medio) encontrados.medio = 'Elige el medio de pago.'
    if (Number(monto) <= 0) encontrados.monto = 'Escribe un monto mayor que $ 0.'
    if (!fecha) encontrados.fecha = textos.fecha
    else if (fecha > hoy) encontrados.fecha = `La fecha no puede ser posterior a hoy (${fechaCorta(hoy)}).`
    setErrores(encontrados)
    setError(null)
    if (Object.keys(encontrados).length > 0) return

    setEnviando(true)
    alEnviar(true)
    try {
      alRegistrar(await textos.llamar(codigo, { medio, monto: Number(monto), fecha }))
    } catch (causa) {
      setError(causa instanceof ErrorApi
        ? { mensaje: mensajeDeRechazo(causa), detalles: causa.detalles }
        : { mensaje: 'Inténtalo de nuevo en un momento.', detalles: [] })
    } finally {
      setEnviando(false)
      alEnviar(false)
    }
  }

  return (
    <form className="dialogo-contenido" onSubmit={enviar} noValidate>
      <div className="dialogo-cuerpo">
        {error && <Aviso tipo="error" asunto={textos.rechazo} mensaje={error.mensaje} detalles={error.detalles} />}
        {tipo === 'devolucion' && (
          <p className="dialogo-texto">Registra el dinero que ya le entregaste al huésped: el sistema no hace el desembolso.</p>
        )}
        {medios.length === 0 && (
          <Aviso tipo="advertencia" mensaje="El alojamiento no tiene medios de pago para recepción." />
        )}
        <div>
          <label className="campo-rotulo" htmlFor="pago-medio">
            Medio de pago
          </label>
          <select
            id="pago-medio"
            className="campo-control"
            value={medio}
            disabled={enviando}
            aria-invalid={Boolean(errores.medio)}
            aria-describedby={errores.medio ? 'pago-medio-error' : undefined}
            onChange={(evento) => setMedio(evento.target.value)}
          >
            {medios.map((opcion) => (
              <option key={opcion} value={opcion}>
                {nombreLegible(opcion)}
              </option>
            ))}
          </select>
          {errores.medio && <p className="campo-mensaje" id="pago-medio-error">{errores.medio}</p>}
        </div>
        <div>
          <label className="campo-rotulo" htmlFor="pago-monto">
            Monto (COP)
          </label>
          <input
            id="pago-monto"
            className="campo-control"
            inputMode="numeric"
            autoComplete="off"
            data-foco-inicial
            value={monto && MILES.format(Number(monto))}
            disabled={enviando}
            aria-invalid={Boolean(errores.monto)}
            aria-describedby={errores.monto ? 'pago-monto-error' : undefined}
            onChange={(evento) => setMonto(evento.target.value.replace(/\D/g, '').slice(0, 12))}
          />
          {errores.monto && <p className="campo-mensaje" id="pago-monto-error">{errores.monto}</p>}
        </div>
        <div>
          <label className="campo-rotulo" htmlFor="pago-fecha">
            Fecha
          </label>
          <input
            id="pago-fecha"
            type="date"
            className="campo-control"
            max={hoy}
            value={fecha}
            disabled={enviando}
            aria-invalid={Boolean(errores.fecha)}
            aria-describedby={errores.fecha ? 'pago-fecha-error' : 'pago-fecha-pista'}
            onChange={(evento) => setFecha(evento.target.value)}
          />
          {errores.fecha
            ? <p className="campo-mensaje" id="pago-fecha-error">{errores.fecha}</p>
            : <p className="campo-pista" id="pago-fecha-pista">Hoy por defecto. No puede ser una fecha futura.</p>}
        </div>
        {enviando && <p className="suave" role="status">{textos.enviando}</p>}
      </div>
      <div className="dialogo-pie">
        <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alVolver}>
          {textoVolver}
        </button>
        <button type="submit" className="boton" disabled={enviando || medios.length === 0}>
          {enviando ? 'Registrando…' : textos.boton}
        </button>
      </div>
    </form>
  )
}
