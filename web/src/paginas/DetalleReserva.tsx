import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import type { Alojamiento } from '../api/alojamiento'
import { consultarApartamento, type ApartamentoPublico } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import {
  cancelarReserva,
  ESTADOS,
  consultarCuentaDeReserva,
  consultarReserva,
  type Cancelacion,
  type CuentaDeReserva,
  type ReservaDelHuesped,
} from '../api/cuenta'
import Foto from '../busqueda/Foto'
import { enPesos, fechaCorta, horaDelDia, noches } from '../formato'

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
            mensaje: error instanceof ErrorApi ? error.message : 'Intenta de nuevo en un momento.',
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

  async function alCancelar() {
    const cancelacion = await cancelarReserva(codigo)
    setResultado(cancelacion)
    const [nueva, nuevaCuenta] = await Promise.allSettled([consultarReserva(codigo), consultarCuentaDeReserva(codigo)])
    setCarga({
      estado: 'listo',
      reserva: valorDe(nueva) ?? { ...reserva, estado: cancelacion.estado },
      cuenta: valorDe(nuevaCuenta),
      apartamento,
    })
  }

  return (
    <main className="ficha detalle-reserva">
      <Link to="/mis-reservas" className="volver">← Mis reservas</Link>
      <div className="detalle-cabeza">
        <Foto url={foto?.url} alt="" carga="eager" />
        <div>
          <span className={`estado-reserva estado-${reserva.estado.toLowerCase()}`}>{ESTADOS[reserva.estado]}</span>
          <h1>{apartamento?.nombre ?? reserva.apartamento}</h1>
          <p className="suave">Código {reserva.codigo}</p>
        </div>
      </div>

      {resultado && <ResultadoCancelacion resultado={resultado} />}
      {reserva.estado === 'PENDIENTE' && (
        <p className="aviso-cambio">
          Esta reserva aún no está pagada. Si el plazo para pagarla termina, las noches vuelven a quedar libres.
        </p>
      )}

      <section aria-labelledby="titulo-estadia">
        <h2 id="titulo-estadia">Tu estadía</h2>
        <dl className="datos">
          <dt>Llegada</dt>
          <dd>{fechaCorta(reserva.entrada)}{horaEntrada && `, desde las ${horaDelDia(horaEntrada)}`}</dd>
          <dt>Salida</dt>
          <dd>{fechaCorta(reserva.salida)}{horaSalida && `, hasta las ${horaDelDia(horaSalida)}`}</dd>
          <dt>Viajan</dt>
          <dd>{reserva.ocupantes.map((o) => o.nombre).join(', ')}</dd>
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
          <dt>Alojamiento, {n} {n === 1 ? 'noche' : 'noches'}</dt>
          <dd>{enPesos(reserva.valor.total)}</dd>
          {reserva.extras.parqueadero > 0 && (
            <>
              <dt>Parqueadero</dt>
              <dd>{enPesos(reserva.extras.parqueadero)}</dd>
            </>
          )}
          {reserva.extras.mascotas > 0 && (
            <>
              <dt>Mascotas</dt>
              <dd>{enPesos(reserva.extras.mascotas)}</dd>
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
      </section>

      {(reserva.estado === 'PENDIENTE' || reserva.estado === 'CONFIRMADA') && (
        <CancelarReserva alCancelar={alCancelar} />
      )}
    </main>
  )
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

function CancelarReserva({ alCancelar }: { alCancelar: () => Promise<void> }) {
  const [preguntando, setPreguntando] = useState(false)
  const [cancelando, setCancelando] = useState(false)
  const [problema, setProblema] = useState<string | null>(null)
  const panel = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (preguntando) panel.current?.focus()
  }, [preguntando])

  async function confirmar() {
    setProblema(null)
    setCancelando(true)
    try {
      await alCancelar()
    } catch (error) {
      setProblema(error instanceof ErrorApi ? error.message : 'No pudimos cancelar tu reserva. Intenta de nuevo.')
      setCancelando(false)
    }
  }

  if (!preguntando) {
    return (
      <button type="button" className="secundario peligroso" onClick={() => setPreguntando(true)}>
        Cancelar esta reserva
      </button>
    )
  }
  return (
    <div className="confirmar-cancelacion" ref={panel} tabIndex={-1} role="group" aria-labelledby="titulo-cancelar">
      <h2 id="titulo-cancelar">¿Cancelar esta reserva?</h2>
      <p>
        Las noches quedan libres de inmediato y no se puede deshacer. Se aplican las condiciones de cancelación que
        aceptaste al reservar. Lo pagado por parqueadero y mascotas se te devuelve.
      </p>
      {problema && <p role="alert" className="aviso">{problema}</p>}
      <div className="confirmar-acciones">
        <button type="button" className="boton-peligro" disabled={cancelando} onClick={confirmar}>
          {cancelando ? 'Cancelando…' : 'Sí, cancelar'}
        </button>
        <button type="button" className="secundario" disabled={cancelando} onClick={() => setPreguntando(false)}>
          No, conservarla
        </button>
      </div>
    </div>
  )
}
