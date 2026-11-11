import { useState, type FormEvent } from 'react'
import { cerrarFolio, revertirPago, type Folio, type Pago, type SituacionSaldo } from '../api/folio'
import { registrarSalida, type Estancia, type Reserva } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import EtiquetaEstado from '../componentes/EtiquetaEstado'
import { cantidad, dinero, fechaCorta, nombreLegible, noches } from '../formato'
import FormularioPago from '../reserva/FormularioPago'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import CampoMotivo from './CampoMotivo'
import { datoTraLegible } from './movimientos'

const MOTIVO_AUTORIZACION_MAXIMO = 500

const TITULO_DEL_SALDO: Record<SituacionSaldo, string> = {
  PENDIENTE: 'Saldo pendiente',
  A_FAVOR: 'Saldo a favor',
  AL_DIA: 'Al día',
}

/** El saldo destacado dentro de un diálogo: lo que decide si se puede cerrar o salir (RN-17). */
function SaldoDialogo({ folio }: { folio: Folio }) {
  return (
    <div className="saldo-dialogo">
      <span>{TITULO_DEL_SALDO[folio.situacionSaldo]}</span>
      <strong className="codigo saldo-dialogo-valor">{dinero(folio.saldo)}</strong>
    </div>
  )
}

/** Un folio abierto con saldo distinto de cero solo se cierra con autorización del administrador (RN-17 · AUTC-01). */
function tieneSaldo(folio: Folio): boolean {
  return !folio.cerrado && folio.situacionSaldo !== 'AL_DIA'
}

/** Registrar pago o devolución desde el folio, con el formulario del sprint 2. */
export function DialogoMovimientoDePago({ tipo, codigo, folio, medios, alCerrar, alRegistrar }: {
  tipo: 'pago' | 'devolucion'
  codigo: string
  folio: Folio
  medios: string[]
  alCerrar: () => void
  alRegistrar: (folio: Folio) => void
}) {
  const [enviando, setEnviando] = useState(false)
  const propuesto = tipo === 'devolucion'
    ? (folio.situacionSaldo === 'A_FAVOR' ? Math.min(folio.saldo, folio.totalPagado) : 0)
    : (folio.situacionSaldo === 'PENDIENTE' ? folio.saldo : 0)
  return (
    <Dialogo
      titulo={tipo === 'pago' ? 'Registrar pago' : 'Registrar devolución'}
      subtitulo={<><span className="codigo">{codigo}</span> · {TITULO_DEL_SALDO[folio.situacionSaldo]}{' '}
        <span className="codigo">{dinero(folio.saldo)}</span></>}
      ocupado={enviando}
      alCerrar={alCerrar}
    >
      <FormularioPago
        tipo={tipo}
        codigo={codigo}
        medios={medios}
        montoInicial={propuesto}
        textoVolver="Volver"
        alVolver={alCerrar}
        alEnviar={setEnviando}
        alRegistrar={alRegistrar}
      />
    </Dialogo>
  )
}

/** Revertir un abono mal registrado (RN-16 · PAG-03). La API no recibe motivo; el autor lo pone la sesión (DEC-100). */
export function DialogoRevertirPago({ codigo, pago, alCerrar, alRevertir }: {
  codigo: string
  pago: Pago
  alCerrar: () => void
  alRevertir: (folio: Folio) => void
}) {
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)

  async function revertir() {
    setEnviando(true)
    setError(null)
    try {
      alRevertir(await revertirPago(codigo, pago.id))
    } catch (causa) {
      setError(errorParaMostrar(causa))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Dialogo titulo="Revertir pago" subtitulo={<span className="codigo">{codigo}</span>} ocupado={enviando} alCerrar={alCerrar}>
      <div className="dialogo-contenido">
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto="No se revirtió" mensaje={error.mensaje} detalles={error.detalles} />}
          <dl className="resumen-dialogo">
            <dt>Medio</dt>
            <dd>{nombreLegible(pago.medio)}</dd>
            <dt>Fecha</dt>
            <dd>{fechaCorta(pago.fecha)}</dd>
            <dt>Monto</dt>
            <dd className="codigo">{dinero(pago.monto)}</dd>
          </dl>
          <p className="dialogo-texto">
            El pago no se borra: queda tachado junto a su reverso. Usa esto si el pago se registró por error; si le
            entregas dinero al huésped, registra una devolución.
          </p>
          {enviando && <p className="suave" role="status">Revirtiendo el pago…</p>}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" data-foco-inicial disabled={enviando} onClick={alCerrar}>
            Volver
          </button>
          <button type="button" className="boton boton-peligro" disabled={enviando} onClick={revertir}>
            {enviando ? 'Revirtiendo…' : 'Revertir pago'}
          </button>
        </div>
      </div>
    </Dialogo>
  )
}

/**
 * Cerrar el folio sin salida, en una reserva cancelada o con no-show (CU-28). Con saldo, solo el administrador, con
 * el motivo de su autorización (RN-17 · AUTC-01).
 */
export function DialogoCerrar({ reserva, folio, esAdministrador, alCerrar, alCerrarFolio, alFallar }: {
  reserva: Reserva
  folio: Folio
  esAdministrador: boolean
  alCerrar: () => void
  alCerrarFolio: (folio: Folio) => void
  alFallar: () => void
}) {
  const [motivo, setMotivo] = useState('')
  const [faltaMotivo, setFaltaMotivo] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)
  const conSaldo = tieneSaldo(folio)
  const bloqueado = conSaldo && !esAdministrador

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const texto = motivo.trim()
    setFaltaMotivo(conSaldo && !texto)
    setError(null)
    if (bloqueado || (conSaldo && !texto)) return
    setEnviando(true)
    try {
      alCerrarFolio(await cerrarFolio(reserva.codigo, conSaldo ? texto : undefined))
    } catch (causa) {
      setError(errorParaMostrar(causa))
      alFallar()
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Dialogo
      titulo="Cerrar folio"
      subtitulo={<><span className="codigo">{reserva.codigo}</span> · <EtiquetaEstado estado={reserva.estado} /></>}
      ocupado={enviando}
      alCerrar={alCerrar}
    >
      <form className="dialogo-contenido" onSubmit={enviar} noValidate>
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto="No se cerró el folio" mensaje={error.mensaje} detalles={error.detalles} />}
          <dl className="resumen-dialogo">
            <dt>Titular</dt>
            <dd>{reserva.titularNombre ?? reserva.titularId}</dd>
            <dt>Apartamento</dt>
            <dd className="codigo">{reserva.apartamento}</dd>
          </dl>
          <SaldoDialogo folio={folio} />
          {!conSaldo && <p className="dialogo-texto">El folio quedará cerrado y sin acciones.</p>}
          {bloqueado && (
            <Aviso
              tipo="advertencia"
              asunto="No puedes cerrar un folio con saldo"
              mensaje="Primero cobra o devuelve el saldo, o pide autorización a un administrador."
            />
          )}
          {conSaldo && esAdministrador && (
            <>
              <p className="dialogo-texto">El folio tiene saldo. Para cerrarlo así, deja el motivo de tu autorización.</p>
              <CampoMotivo
                id="cerrar-motivo"
                rotulo="Motivo de la autorización"
                valor={motivo}
                maximo={MOTIVO_AUTORIZACION_MAXIMO}
                focoInicial
                deshabilitado={enviando}
                error={faltaMotivo ? 'Escribe el motivo de la autorización.' : undefined}
                alCambiar={setMotivo}
              />
            </>
          )}
          {enviando && <p className="suave" role="status">Cerrando el folio…</p>}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>
            Volver
          </button>
          {!bloqueado && (
            <button type="submit" className="boton" disabled={enviando}>
              {enviando ? 'Cerrando…' : conSaldo ? 'Autorizar y cerrar' : 'Cerrar folio'}
            </button>
          )}
        </div>
      </form>
    </Dialogo>
  )
}

const ESTADO_OPERATIVO: Record<string, string> = {
  PENDIENTE_PREPARACION: 'Pendiente de preparación',
}

/**
 * Registrar la salida (CU-22 · CU-38 · SAL-03): cierra el folio, finaliza la reserva y deja el apartamento pendiente de
 * preparación. Queda bloqueada si faltan datos del TRA (DEC-96) o si hay saldo y quien registra no es administrador
 * (RN-17); recepción puede cobrar o devolver ahí mismo.
 *
 * @param alCambiarFolio un pago o una devolución hechos desde aquí
 * @param alTerminar     la salida quedó registrada: hay que volver a leer la reserva y el folio
 * @param alFallar       algo falló: la reserva o el folio pudieron cambiar
 */
export function DialogoSalida({ reserva, folio, esAdministrador, medios, alCerrar, alCambiarFolio, alTerminar, alFallar }: {
  reserva: Reserva
  folio: Folio
  esAdministrador: boolean
  medios: string[]
  alCerrar: () => void
  alCambiarFolio: (folio: Folio) => void
  alTerminar: () => unknown
  alFallar: () => void
}) {
  const [motivo, setMotivo] = useState('')
  const [faltaMotivo, setFaltaMotivo] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)
  const [cobro, setCobro] = useState<'pago' | 'devolucion' | null>(null)
  const [resultado, setResultado] = useState<Estancia | null>(null)

  const faltantes = reserva.ocupantes.filter((ocupante) => (ocupante.datosTraFaltantes ?? []).length > 0)
  const conSaldo = tieneSaldo(folio)
  const bloqueada = faltantes.length > 0 || (conSaldo && !esAdministrador)

  if (resultado) {
    return (
      <Dialogo titulo="Salida registrada" subtitulo={<span className="codigo">{reserva.codigo}</span>} alCerrar={alCerrar}>
        <div className="dialogo-contenido">
          <div className="dialogo-cuerpo">
            <Aviso tipo="exito" asunto="Salida registrada" mensaje="El folio y la reserva ya muestran cómo quedaron." />
            <dl className="resumen-dialogo">
              <dt>Reserva</dt>
              <dd><EtiquetaEstado estado={resultado.estado} /></dd>
              <dt>Folio</dt>
              <dd>{resultado.folioCerrado ? 'Cerrado' : 'Abierto'}</dd>
              <dt>Apartamento</dt>
              <dd>
                <span className="codigo">{resultado.apartamento}</span> ·{' '}
                {ESTADO_OPERATIVO[resultado.estadoOperativo] ?? nombreLegible(resultado.estadoOperativo)}
              </dd>
            </dl>
          </div>
          <div className="dialogo-pie">
            <button type="button" className="boton" data-foco-inicial onClick={alCerrar}>
              Cerrar
            </button>
          </div>
        </div>
      </Dialogo>
    )
  }

  if (cobro) {
    return (
      <DialogoMovimientoDePago
        tipo={cobro}
        codigo={reserva.codigo}
        folio={folio}
        medios={medios}
        alCerrar={() => setCobro(null)}
        alRegistrar={(nuevo) => {
          alCambiarFolio(nuevo)
          setCobro(null)
        }}
      />
    )
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const texto = motivo.trim()
    setFaltaMotivo(conSaldo && esAdministrador && !texto)
    setError(null)
    if (bloqueada || (conSaldo && !texto)) return
    setEnviando(true)
    try {
      const estancia = await registrarSalida(reserva.codigo, conSaldo ? texto : undefined)
      setResultado(estancia)
      await alTerminar()
    } catch (causa) {
      setError(errorParaMostrar(causa))
      alFallar()
    } finally {
      setEnviando(false)
    }
  }

  const total = noches(reserva.entrada, reserva.salida)
  return (
    <Dialogo titulo="Registrar salida" subtitulo={<span className="codigo">{reserva.codigo}</span>} ocupado={enviando} alCerrar={alCerrar}>
      <form className="dialogo-contenido" onSubmit={enviar} noValidate>
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto="No se registró la salida" mensaje={error.mensaje} detalles={error.detalles} />}
          <dl className="resumen-dialogo">
            <dt>Apartamento</dt>
            <dd className="codigo">{reserva.apartamento}</dd>
            <dt>Fechas</dt>
            <dd>
              {fechaCorta(reserva.entrada)} → {fechaCorta(reserva.salida)} · {cantidad(total, 'noche', 'noches')}
            </dd>
          </dl>
          <SaldoDialogo folio={folio} />
          {faltantes.length > 0 && (
            <div className="aviso aviso-advertencia" role="status" id="salida-falta-tra">
              <span className="aviso-sello" aria-hidden="true">!</span>
              <div className="aviso-cuerpo">
                <strong>Advertencia · Faltan datos del TRA</strong>
                <p>Complétalos antes de registrar la salida:</p>
                <ul>
                  {faltantes.map((ocupante) => (
                    <li key={ocupante.id}>
                      <strong>{ocupante.nombre}:</strong>{' '}
                      {(ocupante.datosTraFaltantes ?? []).map(datoTraLegible).join(', ')}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          )}
          {conSaldo && !esAdministrador && (
            <div id="salida-con-saldo">
              <Aviso
                tipo="advertencia"
                asunto={folio.situacionSaldo === 'A_FAVOR' ? 'Hay saldo a favor' : 'Hay saldo pendiente'}
                mensaje={folio.situacionSaldo === 'A_FAVOR'
                  ? 'Devuélvelo antes de registrar la salida.'
                  : 'Cóbralo antes de registrar la salida.'}
                accion={
                  <button
                    type="button"
                    className="boton boton-secundario boton-chico"
                    onClick={() => setCobro(folio.situacionSaldo === 'A_FAVOR' ? 'devolucion' : 'pago')}
                  >
                    {folio.situacionSaldo === 'A_FAVOR' ? 'Registrar devolución' : 'Registrar pago'}
                  </button>
                }
              />
            </div>
          )}
          <p className="dialogo-texto">
            Se cierra el folio, la reserva queda finalizada y el apartamento pasa a pendiente de preparación.
          </p>
          {conSaldo && esAdministrador && faltantes.length === 0 && (
            <CampoMotivo
              id="salida-motivo"
              rotulo="Motivo de la autorización"
              valor={motivo}
              maximo={MOTIVO_AUTORIZACION_MAXIMO}
              pista="Por qué se autoriza la salida con saldo"
              focoInicial
              deshabilitado={enviando}
              error={faltaMotivo ? 'Escribe el motivo de la autorización.' : undefined}
              alCambiar={setMotivo}
            />
          )}
          {enviando && <p className="suave" role="status">Registrando la salida…</p>}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>
            Volver
          </button>
          <button
            type="submit"
            className="boton"
            disabled={enviando || bloqueada || reserva.estado !== 'EN_CURSO'}
            aria-describedby={[
              faltantes.length > 0 ? 'salida-falta-tra' : null,
              conSaldo && !esAdministrador ? 'salida-con-saldo' : null,
            ].filter(Boolean).join(' ') || undefined}
          >
            {enviando ? 'Registrando…' : conSaldo && esAdministrador && faltantes.length === 0
              ? 'Autorizar y registrar salida'
              : 'Registrar salida'}
          </button>
        </div>
      </form>
    </Dialogo>
  )
}
