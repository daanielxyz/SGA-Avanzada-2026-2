import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import type { Alojamiento } from '../api/alojamiento'
import { consultarApartamento, type ApartamentoPublico } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { explicar } from '../api/mensajes'
import {
  cancelarReserva,
  ESTADOS,
  consultarCuentaDeReserva,
  consultarPagoPendiente,
  consultarReserva,
  simularCancelacion,
  type Cancelacion,
  type CuentaDeReserva,
  type PagoPendiente,
  type ReservaDelHuesped,
  type SimulacionCancelacion,
} from '../api/cuenta'
import Foto from '../busqueda/Foto'
import { enPesos, fechaCorta, horaDelDia, noches } from '../formato'
import PasoPago from '../reservar/PasoPago'

type Carga =
  | { estado: 'cargando' }
  | { estado: 'problema'; titulo: string; mensaje: string }
  | {
    estado: 'listo'
    reserva: ReservaDelHuesped
    cuenta: CuentaDeReserva | null
    apartamento: ApartamentoPublico | null
  }

const valorDe = <T,>(resultado: PromiseSettledResult<T>) => (resultado.status === 'fulfilled' ? resultado.value : null)

/** Una reserva del huésped: lo reservado, cómo va la cuenta y, si aún se puede, cancelarla (USU-04 · CU-14). */
export default function DetalleReserva({ alojamiento }: { alojamiento: Alojamiento | null }) {
  const { codigo = '' } = useParams()
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [resultado, setResultado] = useState<Cancelacion | null>(null)
  const [pagoRecibido, setPagoRecibido] = useState(false)

  useEffect(() => {
    let vigente = true
    consultarReserva(codigo)
      .then(async (reserva) => {
        // La reserva es lo esencial: si lo demás falla, se muestra sin esos datos
        const [cuenta, apartamento] = await Promise.allSettled([
          consultarCuentaDeReserva(codigo),
          consultarApartamento(reserva.apartamento),
        ])
        if (vigente) setCarga({ estado: 'listo', reserva, cuenta: valorDe(cuenta), apartamento: valorDe(apartamento) })
      })
      .catch((error: unknown) => {
        if (!vigente) return
        // Con una reserva ajena o inexistente se dice lo mismo: así no se averigua cuáles existen (USU-04)
        const noEsSuya = error instanceof ErrorApi && (error.estado === 403 || error.estado === 404)
        setCarga(noEsSuya
          ? { estado: 'problema', titulo: 'No encontramos esta reserva', mensaje: 'Revisa el código en tus reservas.' }
          : {
            estado: 'problema',
            titulo: 'No pudimos abrir tu reserva',
            mensaje: explicar(error, 'Intenta de nuevo en un momento.'),
          })
      })
    return () => {
      vigente = false
    }
  }, [codigo])

  if (carga.estado === 'cargando') {
    return <main className="ficha"><p className="suave">Cargando tu reserva…</p></main>
  }
  if (carga.estado === 'problema') {
    return (
      <main className="ficha">
        <h1>{carga.titulo}</h1>
        <p>{carga.mensaje}</p>
        <Link to="/mis-reservas">Ver mis reservas</Link>
      </main>
    )
  }

  const { reserva, cuenta, apartamento } = carga
  const foto = apartamento?.imagenes.find((i) => i.principal) ?? apartamento?.imagenes[0]
  const n = noches(reserva.entrada, reserva.salida)
  const horaEntrada = alojamiento?.parametros.horaEntrada
  const horaSalida = alojamiento?.parametros.horaSalida
  const terminada = !['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'].includes(reserva.estado)
  const sinCobro = reserva.estado === 'CANCELADA' || reserva.estado === 'NO_SHOW'

  // Después de cancelar o pagar se vuelve a pedir: lo que se muestra siempre es lo que dice el backend
  async function recargar(estado: ReservaDelHuesped['estado']) {
    const [nueva, nuevaCuenta] = await Promise.allSettled([consultarReserva(codigo), consultarCuentaDeReserva(codigo)])
    setCarga({
      estado: 'listo',
      reserva: valorDe(nueva) ?? { ...reserva, estado },
      cuenta: valorDe(nuevaCuenta),
      apartamento,
    })
  }

  async function alCancelar() {
    const cancelacion = await cancelarReserva(codigo)
    setResultado(cancelacion)
    setPagoRecibido(false)
    await recargar(cancelacion.estado)
  }

  async function alPagar() {
    setPagoRecibido(true)
    await recargar('CONFIRMADA')
  }

  return (
    <main className="ficha detalle-reserva">
      {/* Vuelve a la pestaña de donde vino: las terminadas están en «Anteriores» (DEC-108) */}
      <Link to={terminada ? '/mis-reservas?ver=anteriores' : '/mis-reservas'} className="volver">← Mis reservas</Link>
      <div className="detalle-cabeza">
        <Foto url={foto?.url} alt="" carga="eager" />
        <div>
          <span className={`estado-reserva estado-${reserva.estado.toLowerCase()}`}>{ESTADOS[reserva.estado]}</span>
          <h1>{apartamento?.nombre ?? reserva.apartamento}</h1>
          <p className="suave">Código {reserva.codigo}</p>
        </div>
      </div>

      {resultado && <ResultadoCancelacion resultado={resultado} />}
      {pagoRecibido && (
        <p className="aviso-cambio" role="status"><strong>Recibimos tu pago. Tu reserva quedó confirmada.</strong></p>
      )}
      {reserva.estado === 'PENDIENTE' && (
        <PagoDeReserva reserva={reserva} yaPagado={cuenta?.totalPagado ?? 0} alPagar={alPagar} />
      )}

      <section aria-labelledby="titulo-estadia">
        <h2 id="titulo-estadia">Tu estadía</h2>
        <dl className="datos">
          <dt>Llegada</dt>
          <dd>{fechaCorta(reserva.entrada)}{horaEntrada && `, desde las ${horaDelDia(horaEntrada)}`}</dd>
          <dt>Salida</dt>
          <dd>{fechaCorta(reserva.salida)}{horaSalida && `, hasta las ${horaDelDia(horaSalida)}`}</dd>
          <dt>Viajan</dt>
          <dd>
            {/* Una persona por renglón: un grupo grande se revisa nombre por nombre */}
            <ul className="lista-viajeros">
              {reserva.ocupantes.map((o) => <li key={o.id}>{o.nombre}</li>)}
            </ul>
          </dd>
          {reserva.carros > 0 && (
            <>
              <dt>Carros</dt>
              <dd>{reserva.carros}</dd>
            </>
          )}
          {reserva.motos > 0 && (
            <>
              <dt>Motos</dt>
              <dd>{reserva.motos}</dd>
            </>
          )}
          {reserva.mascotas > 0 && (
            <>
              <dt>Mascotas</dt>
              <dd>{reserva.mascotas}</dd>
            </>
          )}
        </dl>
      </section>

      <section aria-labelledby="titulo-cuenta">
        <h2 id="titulo-cuenta">Tu cuenta</h2>
        <dl className="datos datos-valores">
          {sinCobro ? (
            <>
              <dt>Valor de la reserva, que ya no se cobra</dt>
              <dd className="tachado">{enPesos(reserva.valor.total + reserva.extras.total)}</dd>
            </>
          ) : (
            <>
              <dt>Alojamiento, {n} {n === 1 ? 'noche' : 'noches'}</dt>
              <dd>{enPesos(reserva.valor.total)}</dd>
            </>
          )}
          {!sinCobro && reserva.extras.parqueadero > 0 && (
            <>
              <dt>Parqueadero</dt>
              <dd>{enPesos(reserva.extras.parqueadero)}</dd>
            </>
          )}
          {!sinCobro && reserva.extras.mascotas > 0 && (
            <>
              <dt>Mascotas</dt>
              <dd>{enPesos(reserva.extras.mascotas)}</dd>
            </>
          )}
          {cuenta && sinCobro && (
            <>
              <dt>{reserva.estado === 'NO_SHOW' ? 'Retenido por no llegar' : 'Retenido por cancelar'}</dt>
              <dd>{enPesos(retenidoDe(cuenta))}</dd>
            </>
          )}
          {cuenta && (
            <>
              <dt>Pagado</dt>
              <dd>{enPesos(cuenta.totalPagado)}</dd>
              <dt><strong>{SITUACION[cuenta.situacionSaldo]}</strong></dt>
              <dd><strong>{enPesos(cuenta.saldo)}</strong></dd>
            </>
          )}
        </dl>
        {sinCobro && (
          <p className="suave">
            Solo se cobra lo retenido según las condiciones que aceptaste; el saldo descuenta lo que ya pagaste.
          </p>
        )}
      </section>

      {(reserva.estado === 'PENDIENTE' || reserva.estado === 'CONFIRMADA') && (
        <CancelarReserva codigo={codigo} alCancelar={alCancelar} />
      )}
    </main>
  )
}

/**
 * En una reserva cancelada o sin llegada, la cuenta solo carga lo retenido: saldo = retenido − pagado (FOL-01). Así se
 * despeja sin pedir otro dato al backend.
 */
function retenidoDe({ totalPagado, saldo, situacionSaldo }: CuentaDeReserva) {
  if (situacionSaldo === 'A_FAVOR') return totalPagado - saldo
  if (situacionSaldo === 'PENDIENTE') return totalPagado + saldo
  return totalPagado
}

const SITUACION = {
  PENDIENTE: 'Falta por pagar',
  A_FAVOR: 'A tu favor',
  AL_DIA: 'Saldo',
}

function ResultadoCancelacion({ resultado: { retenido, saldo, situacionSaldo } }: { resultado: Cancelacion }) {
  return (
    <div className="aviso-cambio" role="status">
      <p><strong>Tu reserva quedó cancelada.</strong></p>
      <p>
        {retenido > 0
          ? `Según las condiciones que aceptaste, se retienen ${enPesos(retenido)}.`
          : 'No se retiene nada.'}
      </p>
      {situacionSaldo === 'A_FAVOR' && (
        <p>Tienes {enPesos(saldo)} a tu favor: el hotel se encarga de devolvértelos.</p>
      )}
      {situacionSaldo === 'PENDIENTE' && <p>Queda un saldo de {enPesos(saldo)} por pagar.</p>}
    </div>
  )
}

interface PropsPago {
  reserva: ReservaDelHuesped
  yaPagado: number
  alPagar: () => Promise<void>
}

/** Retomar el pago de una reserva pendiente, por ejemplo si se cerró la pantalla de pago (DEC-69 · DEC-91). */
function PagoDeReserva({ reserva, yaPagado, alPagar }: PropsPago) {
  const { codigo, valor, extras } = reserva
  const [pago, setPago] = useState<PagoPendiente | null>(null)
  const [problema, setProblema] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true
    consultarPagoPendiente(codigo)
      .then((p) => vigente && setPago(p))
      .catch((error: unknown) => vigente && setProblema(
        explicar(error, 'No pudimos revisar el pago de esta reserva. Intenta de nuevo en un momento.')))
    return () => {
      vigente = false
    }
  }, [codigo])

  if (problema) {
    return <p className="aviso-cambio" role="status">{problema}</p>
  }
  if (!pago) {
    return <p className="suave">Revisando el pago…</p>
  }
  return (
    <section aria-labelledby="titulo-pago">
      <h2 id="titulo-pago">Paga para confirmar</h2>
      <PasoPago yaPagado={yaPagado} alPagar={alPagar} condiciones={pago.condiciones} apartada={{
        reserva: { codigo, valor, extras, avisoCuenta: null },
        montoAPagar: pago.montoAPagar,
        pagarAntesDe: pago.pagarAntesDe,
      }} />
    </section>
  )
}

function ConsecuenciasDeCancelar({ simulacion: { retenido, saldo, situacionSaldo } }: {
  simulacion: SimulacionCancelacion
}) {
  return (
    <dl className="datos datos-valores">
      <dt>Se retiene</dt>
      <dd>{enPesos(retenido)}</dd>
      {situacionSaldo === 'A_FAVOR' && (
        <>
          <dt><strong>Te devuelven</strong></dt>
          <dd><strong>{enPesos(saldo)}</strong></dd>
        </>
      )}
      {situacionSaldo === 'PENDIENTE' && (
        <>
          <dt><strong>Quedarías debiendo</strong></dt>
          <dd><strong>{enPesos(saldo)}</strong></dd>
        </>
      )}
    </dl>
  )
}

function CancelarReserva({ codigo, alCancelar }: { codigo: string; alCancelar: () => Promise<void> }) {
  const [preguntando, setPreguntando] = useState(false)
  const [simulacion, setSimulacion] = useState<SimulacionCancelacion | null>(null)
  const [calculando, setCalculando] = useState(false)
  const [cancelando, setCancelando] = useState(false)
  const [problema, setProblema] = useState<string | null>(null)
  const panel = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (preguntando) panel.current?.focus()
  }, [preguntando])

  // Se calcula al abrir, y no antes: lo retenido depende de cuánto falte para la llegada (DEC-91)
  async function preguntar() {
    setPreguntando(true)
    setProblema(null)
    setSimulacion(null)
    setCalculando(true)
    try {
      setSimulacion(await simularCancelacion(codigo))
    } catch (error) {
      setProblema(explicar(error, 'No pudimos calcular cuánto se retendría. Intenta de nuevo en un momento.'))
    } finally {
      setCalculando(false)
    }
  }

  async function confirmar() {
    setProblema(null)
    setCancelando(true)
    try {
      await alCancelar()
    } catch (error) {
      setProblema(explicar(error, 'No pudimos cancelar tu reserva. Intenta de nuevo.'))
      setCancelando(false)
    }
  }

  if (!preguntando) {
    return (
      <button type="button" className="secundario peligroso" onClick={preguntar}>
        Cancelar esta reserva
      </button>
    )
  }
  return (
    <div className="confirmar-cancelacion" ref={panel} tabIndex={-1} role="group" aria-labelledby="titulo-cancelar">
      <h2 id="titulo-cancelar">¿Cancelar esta reserva?</h2>
      <p>Las noches quedan libres de inmediato y no se puede deshacer.</p>
      {calculando && <p className="suave">Calculando cuánto se retendría…</p>}
      {simulacion && <ConsecuenciasDeCancelar simulacion={simulacion} />}
      {problema && <p role="alert" className="aviso">{problema}</p>}
      <div className="confirmar-acciones">
        <button type="button" className="boton-peligro" disabled={cancelando || !simulacion} onClick={confirmar}>
          {cancelando ? 'Cancelando…' : 'Sí, cancelar'}
        </button>
        <button type="button" className="secundario" disabled={cancelando} onClick={() => setPreguntando(false)}>
          No, conservarla
        </button>
      </div>
    </div>
  )
}
