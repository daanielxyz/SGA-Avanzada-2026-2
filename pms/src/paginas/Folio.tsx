import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { ErrorApi } from '../api/cliente'
import type { Cargo, Folio as DatosFolio, Pago, SituacionSaldo } from '../api/folio'
import { consultarReserva, type Reserva } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import EtiquetaEstado from '../componentes/EtiquetaEstado'
import { useTitulo } from '../componentes/useTitulo'
import { dinero, fechaCorta, hora } from '../formato'
import { DialogoConsumo, DialogoCorregir, DialogoRevertirCargo } from '../folio/DialogosCargo'
import { DialogoCerrar, DialogoMovimientoDePago, DialogoRevertirPago, DialogoSalida } from '../folio/DialogosFolio'
import { accionesDelFolio, cargosNetos, consumosDelCatalogo, type AccionFolio } from '../folio/movimientos'
import { TablaCargos, TablaPagos } from '../folio/TablasMovimientos'
import { mediosDeRecepcion } from '../reserva/pago'
import { useDatosDePago } from '../reserva/useDatosDePago'
import { useSesion } from '../sesion/contexto'

type Resultado = { clave: string; reserva: Reserva } | { clave: string; error: unknown }

type DialogoAbierto =
  | { tipo: AccionFolio }
  | { tipo: 'revertirCargo' | 'corregir'; cargo: Cargo }
  | { tipo: 'revertirPago'; pago: Pago }

const TEXTO_DE_ACCION: Record<AccionFolio, string> = {
  pago: 'Registrar pago',
  devolucion: 'Registrar devolución',
  consumo: 'Cargar consumo',
  salida: 'Registrar salida',
  cerrar: 'Cerrar folio',
}

const SALDO: Record<SituacionSaldo, { texto: string; clase: string; forma: ReactNode }> = {
  PENDIENTE: { texto: 'Saldo pendiente', clase: 'pendiente', forma: <path d="M6 1a5 5 0 0 1 0 10z" className="relleno" /> },
  A_FAVOR: { texto: 'Saldo a favor', clase: 'en-curso', forma: <path d="M6 2v8M2 6h8" /> },
  AL_DIA: { texto: 'Al día', clase: 'confirmada', forma: <path d="M2 6.5 4.8 9.5 10 3" /> },
}

/**
 * Folio de una reserva (CU-27 · RN-15 · RN-16): saldo, cargos y pagos, con sus correcciones, y lo que recepción puede
 * hacer con él: cargar consumos, cobrar, devolver, revertir, cerrar y registrar la salida.
 */
export default function Folio() {
  const { codigo = '' } = useParams()
  const { sesion } = useSesion()
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [dialogo, setDialogo] = useState<DialogoAbierto | null>(null)
  const [exito, setExito] = useState<{ asunto: string; mensaje: string } | null>(null)
  const titulo = useRef<HTMLHeadingElement>(null)
  const enfocarTitulo = useRef(false)
  const pago = useDatosDePago(codigo)
  const clave = `${codigo}#${intento}`
  useTitulo(`Folio ${codigo}`)

  useEffect(() => {
    let vigente = true
    consultarReserva(codigo)
      .then((reserva) => {
        if (vigente) setResultado({ clave, reserva })
      })
      .catch((error: unknown) => {
        if (vigente) setResultado({ clave, error })
      })
    return () => {
      vigente = false
    }
  }, [codigo, clave])

  // Al terminar una acción, el botón que abrió el diálogo puede desaparecer: el foco vuelve al título
  useEffect(() => {
    if (dialogo === null && enfocarTitulo.current) {
      enfocarTitulo.current = false
      titulo.current?.focus()
    }
  }, [dialogo])

  const actual = resultado?.clave === clave ? resultado : null
  const volver = <Link to={`/reservas/${encodeURIComponent(codigo)}`} className="volver"><span aria-hidden="true">←</span> Volver a la reserva</Link>

  if (!actual || (pago.cargando && !('error' in actual))) return <Cargando />

  if ('error' in actual) {
    if (actual.error instanceof ErrorApi && actual.error.estado === 404) {
      return (
        <BloqueVacio
          encabezado="h1"
          icono="reservaNoEncontrada"
          titulo="Reserva no encontrada"
          accion={
            <Link to="/reservas" className="boton">
              <span aria-hidden="true">←</span> Volver a reservas
            </Link>
          }
        >
          No existe una reserva con el código <span className="codigo codigo-resaltado">{codigo}</span>.
        </BloqueVacio>
      )
    }
    return <ErrorAlCargar volver={volver} error={actual.error} alReintentar={() => setIntento(intento + 1)} />
  }

  if (!pago.datos) {
    return <ErrorAlCargar volver={volver} error={pago.error} alReintentar={pago.reintentar} />
  }

  const { reserva } = actual
  const { folio, alojamiento } = pago.datos
  const esAdministrador = sesion?.rol === 'ADMINISTRADOR'
  const medios = mediosDeRecepcion(alojamiento)
  const servicios = consumosDelCatalogo(alojamiento.serviciosAdicionales)
  const acciones = accionesDelFolio(reserva.estado, folio)

  /** Vuelve a leer la reserva y el folio sin mostrar «cargando»: pudieron cambiar en otro turno. */
  function actualizar() {
    const leerReserva = consultarReserva(codigo)
      .then((nueva) => setResultado({ clave, reserva: nueva }))
      .catch(() => undefined)
    return Promise.all([leerReserva, pago.recargar()])
  }

  /** Una acción terminó: el folio nuevo llega en la respuesta. */
  function terminar(nuevo: DatosFolio, asunto: string, mensaje = 'El folio ya muestra el nuevo saldo.') {
    pago.ponerFolio(nuevo)
    setExito({ asunto, mensaje })
    enfocarTitulo.current = true
    setDialogo(null)
  }

  // Al cerrar sin terminar se vuelve a leer: si la acción falló, puede que otro turno haya cambiado el folio
  const cerrarDialogo = () => {
    setDialogo(null)
    void actualizar()
  }
  const saldo = SALDO[folio.situacionSaldo]

  return (
    <div className="detalle folio">
      <div>
        {volver}
        <div className="detalle-titulo">
          <div>
            <p className="folio-rotulo">Folio</p>
            <div className="detalle-codigo">
              <h1 className="codigo" ref={titulo} tabIndex={-1}>
                {reserva.codigo}
              </h1>
              <EtiquetaEstado estado={reserva.estado} />
              {folio.cerrado && (
                <span className="etiqueta">
                  <span aria-hidden="true">■</span> Folio cerrado
                </span>
              )}
            </div>
            <p className="folio-subtitulo">
              <span className="codigo">{reserva.apartamento}</span> · Titular: {reserva.titularNombre ?? reserva.titularId}
            </p>
          </div>
          {acciones.length > 0 && (
            <div className="acciones-reserva">
              {acciones.map((accion, posicion) => (
                <button
                  key={accion}
                  type="button"
                  className={posicion === 0 ? 'boton' : 'boton boton-secundario'}
                  onClick={() => setDialogo({ tipo: accion })}
                >
                  {TEXTO_DE_ACCION[accion]}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {exito && <Aviso tipo="exito" asunto={exito.asunto} mensaje={exito.mensaje} />}

      <section aria-label="Resumen del folio" className="folio-resumen">
        <div className="tarjeta folio-cifra folio-saldo">
          <p className="folio-cifra-nombre">Saldo</p>
          <p className="codigo folio-cifra-grande">{dinero(folio.saldo)}</p>
          <span className={`etiqueta etiqueta-${saldo.clase}`}>
            <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
              {saldo.forma}
            </svg>
            {saldo.texto}
          </span>
        </div>
        <div className="tarjeta folio-cifra">
          <p className="folio-cifra-nombre">Cargos</p>
          <p className="codigo folio-cifra-media">{dinero(cargosNetos(folio))}</p>
        </div>
        <div className="tarjeta folio-cifra">
          <p className="folio-cifra-nombre">Total pagado</p>
          <p className="codigo folio-cifra-media">{dinero(folio.totalPagado)}</p>
        </div>
      </section>

      {folio.autorizacion && (
        <section aria-labelledby="titulo-autorizacion" className="tarjeta folio-autorizacion">
          <h2 id="titulo-autorizacion">
            <span aria-hidden="true">■</span> Cerrado con autorización
          </h2>
          <dl className="resumen-dialogo">
            {folio.autorizacion.autorNombre && (
              <>
                <dt>Autorizó</dt>
                <dd>{folio.autorizacion.autorNombre}</dd>
              </>
            )}
            <dt>Fecha y hora</dt>
            <dd>
              {fechaCorta(folio.autorizacion.fechaHora)}, {hora(folio.autorizacion.fechaHora.slice(11, 16))}
            </dd>
            <dt>Motivo</dt>
            <dd>{folio.autorizacion.motivo}</dd>
          </dl>
        </section>
      )}

      <TablaCargos
        folio={folio}
        estado={reserva.estado}
        alCorregir={(cargo) => setDialogo({ tipo: 'corregir', cargo })}
        alRevertir={(cargo) => setDialogo({ tipo: 'revertirCargo', cargo })}
      />
      <TablaPagos folio={folio} alRevertir={(pagoElegido) => setDialogo({ tipo: 'revertirPago', pago: pagoElegido })} />

      {(dialogo?.tipo === 'pago' || dialogo?.tipo === 'devolucion') && (
        <DialogoMovimientoDePago
          tipo={dialogo.tipo}
          codigo={reserva.codigo}
          folio={folio}
          medios={medios}
          alCerrar={cerrarDialogo}
          alRegistrar={(nuevo) => terminar(nuevo, dialogo.tipo === 'pago' ? 'Pago registrado' : 'Devolución registrada')}
        />
      )}
      {dialogo?.tipo === 'consumo' && (
        <DialogoConsumo
          codigo={reserva.codigo}
          servicios={servicios}
          alCerrar={cerrarDialogo}
          alRegistrar={(nuevo) => terminar(nuevo, 'Consumo cargado')}
        />
      )}
      {dialogo?.tipo === 'revertirCargo' && (
        <DialogoRevertirCargo
          codigo={reserva.codigo}
          cargo={dialogo.cargo}
          alCerrar={cerrarDialogo}
          alRevertir={(nuevo) => terminar(nuevo, 'Cargo revertido')}
        />
      )}
      {dialogo?.tipo === 'corregir' && (
        <DialogoCorregir
          codigo={reserva.codigo}
          cargo={dialogo.cargo}
          servicios={servicios}
          alCerrar={() => {
            enfocarTitulo.current = true
            cerrarDialogo()
          }}
          alCambiar={pago.ponerFolio}
          alTerminar={(nuevo) => terminar(nuevo, 'Consumo corregido')}
        />
      )}
      {dialogo?.tipo === 'revertirPago' && (
        <DialogoRevertirPago
          codigo={reserva.codigo}
          pago={dialogo.pago}
          alCerrar={cerrarDialogo}
          alRevertir={(nuevo) => terminar(nuevo, 'Pago revertido')}
        />
      )}
      {dialogo?.tipo === 'cerrar' && (
        <DialogoCerrar
          reserva={reserva}
          folio={folio}
          esAdministrador={esAdministrador}
          alCerrar={cerrarDialogo}
          alCerrarFolio={(nuevo) => terminar(nuevo, 'Folio cerrado', 'Ya no admite movimientos.')}
          alFallar={actualizar}
        />
      )}
      {dialogo?.tipo === 'salida' && (
        <DialogoSalida
          reserva={reserva}
          folio={folio}
          esAdministrador={esAdministrador}
          medios={medios}
          alCerrar={() => {
            enfocarTitulo.current = true
            cerrarDialogo()
          }}
          alCambiarFolio={pago.ponerFolio}
          alTerminar={actualizar}
          alFallar={actualizar}
        />
      )}
    </div>
  )
}

function ErrorAlCargar({ volver, error, alReintentar }: { volver: ReactNode; error: unknown; alReintentar: () => void }) {
  return (
    <div className="detalle">
      <div>{volver}</div>
      <Aviso
        tipo="error"
        asunto="No pudimos cargar el folio"
        mensaje={error instanceof ErrorApi ? error.message : 'Revisa tu conexión e inténtalo de nuevo.'}
        accion={
          <button type="button" className="boton boton-secundario boton-chico" onClick={alReintentar}>
            Reintentar
          </button>
        }
      />
    </div>
  )
}

function Cargando() {
  return (
    <div className="detalle" aria-busy="true">
      <div className="detalle-codigo">
        <span className="fantasma fantasma-titulo" />
        <span className="fantasma fantasma-etiqueta" />
      </div>
      <div className="folio-resumen">
        {Array.from({ length: 3 }, (_, cifra) => (
          <div key={cifra} className="tarjeta folio-cifra">
            <span className="fantasma" />
          </div>
        ))}
      </div>
      <div className="tarjeta fantasma-datos">
        {Array.from({ length: 4 }, (_, fila) => (
          <span key={fila} className="fantasma" />
        ))}
      </div>
      <p className="suave" role="status">
        Cargando el folio…
      </p>
    </div>
  )
}
