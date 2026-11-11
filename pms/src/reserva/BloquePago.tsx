import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { ErrorApi } from '../api/cliente'
import type { SituacionSaldo } from '../api/folio'
import type { Reserva } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import Dato from '../componentes/Dato'
import { dinero } from '../formato'
import { estaTerminada, faltaParaAnticipo, otrosCargos, puedeRegistrarPago, valorRetenido } from './pago'
import type { DatosDePago, useDatosDePago } from './useDatosDePago'

const SALDO: Record<SituacionSaldo, { titulo: string; situacion: string; clase: string; forma: ReactNode }> = {
  PENDIENTE: {
    titulo: 'Saldo pendiente',
    situacion: 'Pendiente',
    clase: 'pendiente',
    forma: <path d="M6 1a5 5 0 0 1 0 10z" className="relleno" />,
  },
  A_FAVOR: { titulo: 'Saldo a favor', situacion: 'A favor', clase: 'en-curso', forma: <path d="M6 2v8M2 6h8" /> },
  AL_DIA: { titulo: 'Saldo', situacion: 'Al día', clase: 'confirmada', forma: <path d="M2 6.5 4.8 9.5 10 3" /> },
}

/**
 * Valor, extras, anticipo, lo pagado y el saldo de la reserva (RN-15 · RES-15 · DEC-89). Los movimientos están en el
 * folio, que se abre desde aquí.
 */
export default function BloquePago({ reserva, pago, alRegistrarPago }: {
  reserva: Reserva
  pago: ReturnType<typeof useDatosDePago>
  alRegistrarPago: () => void
}) {
  const { datos, cargando, error, reintentar } = pago
  return (
    <section aria-labelledby="titulo-pago" className="detalle-seccion pago">
      {!datos && <h2 id="titulo-pago">Pago</h2>}
      {cargando && (
        <div className="tarjeta pago-cargando" aria-busy="true">
          <span className="fantasma" />
          <span className="fantasma" />
          <p className="suave" role="status">
            Cargando el pago…
          </p>
        </div>
      )}
      {!cargando && !datos && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar el pago"
          mensaje={error instanceof ErrorApi ? error.message : 'Inténtalo de nuevo en un momento.'}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={reintentar}>
              Reintentar
            </button>
          }
        />
      )}
      {datos && <Contenido reserva={reserva} datos={datos} alRegistrarPago={alRegistrarPago} />}
    </section>
  )
}

function Contenido({ reserva, datos, alRegistrarPago }: {
  reserva: Reserva
  datos: DatosDePago
  alRegistrarPago: () => void
}) {
  const { folio } = datos
  const anticipo = reserva.anticipoExigido ?? 0
  const anticipoVigente = anticipo > 0 && (reserva.estado === 'PENDIENTE' || reserva.estado === 'CONFIRMADA')
  const falta = faltaParaAnticipo(reserva, folio)
  const otros = otrosCargos(folio)
  const retenido = valorRetenido(folio)
  const saldo = SALDO[folio.situacionSaldo]
  // Al cancelar o con el no-show, el backend anula los extras en el folio y lo pagado por ellos queda a favor (DEC-89)
  const extrasAnulados = reserva.estado === 'CANCELADA' || reserva.estado === 'NO_SHOW'
  const extra = (valor: number) => (
    <>
      {dinero(valor)}
      {extrasAnulados && <span className="suave"> · anulado</span>}
    </>
  )

  return (
    <>
      <div className="pago-cabecera">
        <h2 id="titulo-pago">Pago</h2>
        <div className="pago-acciones">
          <Link to={`/reservas/${encodeURIComponent(reserva.codigo)}/folio`} className="pago-ver-folio">
            Ver folio <span aria-hidden="true">→</span>
          </Link>
          {puedeRegistrarPago(reserva.estado, folio) && (
            <button type="button" className="boton boton-secundario pago-registrar" onClick={alRegistrarPago}>
              Registrar pago
            </button>
          )}
        </div>
      </div>
      <dl className="tarjeta datos datos-pago">
        <Dato nombre="Estancia" codigo>
          {dinero(reserva.valor.total)}
        </Dato>
        {reserva.extras.parqueadero > 0 && (
          <Dato nombre="Parqueadero" codigo>
            {extra(reserva.extras.parqueadero)}
          </Dato>
        )}
        {reserva.extras.mascotas > 0 && (
          <Dato nombre="Cargo por mascotas" codigo>
            {extra(reserva.extras.mascotas)}
          </Dato>
        )}
        {otros > 0 && (
          <Dato nombre="Consumos" codigo>
            {dinero(otros)}
          </Dato>
        )}
        {anticipoVigente && (
          <Dato nombre="Anticipo exigido" codigo>
            {dinero(anticipo)}
          </Dato>
        )}
        {estaTerminada(reserva.estado) && retenido > 0 && (
          <Dato nombre="Valor retenido" codigo>
            {dinero(retenido)}
          </Dato>
        )}
        <Dato nombre="Total pagado" codigo>
          {dinero(folio.totalPagado)}
        </Dato>
        <Dato nombre={saldo.titulo} codigo>
          {dinero(folio.saldo)}
        </Dato>
      </dl>
      <div className="pago-situacion">
        <span className={`etiqueta etiqueta-${saldo.clase}`}>
          <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
            {saldo.forma}
          </svg>
          Situación: {saldo.situacion}
        </span>
        {anticipoVigente && falta > 0 && (
          <span className="etiqueta etiqueta-no-show">
            <span aria-hidden="true">!</span> Falta para el anticipo: <span className="codigo">{dinero(falta)}</span>
          </span>
        )}
        {anticipoVigente && falta === 0 && (
          <span className="etiqueta etiqueta-confirmada">
            <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
              <path d="M2 6.5 4.8 9.5 10 3" />
            </svg>
            Anticipo cubierto
          </span>
        )}
      </div>
    </>
  )
}
