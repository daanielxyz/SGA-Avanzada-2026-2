import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { consultarApartamento, type Apartamento } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { consultarEstancia, consultarReserva, NOMBRE_DEL_CANAL, type Estancia, type Liquidacion, type Ocupante, type Reserva } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import Dato from '../componentes/Dato'
import Dialogo from '../componentes/Dialogo'
import EtiquetaEstado from '../componentes/EtiquetaEstado'
import TablaDesglose from '../componentes/TablaDesglose'
import { useTitulo } from '../componentes/useTitulo'
import { DialogoSalida } from '../folio/DialogosFolio'
import { cantidad, dinero, fechaCorta, fechaHoy, hora, noches, textoEdad } from '../formato'
import AccionesReserva, { type Accion } from '../reserva/AccionesReserva'
import DialogoTra from '../reserva/llegada/DialogoTra'
import { DialogoCorregirRegistro, DialogoVehiculos } from '../reserva/llegada/DialogosLlegada'
import { fechaYHora, textoTra, tieneTraPendiente } from '../reserva/llegada/llegada'
import LlegadaRegistrada from '../reserva/llegada/LlegadaRegistrada'
import BloquePago from '../reserva/BloquePago'
import NotasTurno from '../reserva/NotasTurno'
import PoliticaAplicada from '../reserva/PoliticaAplicada'
import DialogoConfirmar from '../reserva/DialogoConfirmar'
import DialogoLiquidar from '../reserva/DialogoLiquidar'
import FormularioPago from '../reserva/FormularioPago'
import { mediosDeRecepcion, montoSugerido } from '../reserva/pago'
import { useDatosDePago } from '../reserva/useDatosDePago'
import { useSesion } from '../sesion/contexto'

type Resultado = { clave: string; reserva: Reserva } | { clave: string; error: unknown }

/** Lo que manda «Nueva reserva» al terminar. */
interface ReservaRecienCreada {
  asunto: string
  mensaje: string
  avisoCuenta: string | null
}

type DialogoAbierto = Exclude<Accion, 'llegada'> | 'pago' | 'vehiculos' | { tra: string; volverASalida?: boolean }

/** Detalle de una reserva (CU-49): sus datos, su pago, su grupo, el valor por noche y lo que se puede hacer con ella. */
export default function DetalleReserva() {
  const { codigo = '' } = useParams()
  const ubicacion = useLocation()
  const navegar = useNavigate()
  const { sesion } = useSesion()
  const estadoRecibido = ubicacion.state as {
    volverA?: string
    creada?: ReservaRecienCreada
    /** Lo que manda la llegada al registrarla. */
    aviso?: { asunto: string; mensaje: string }
  } | null
  const volverA = estadoRecibido?.volverA ?? ''
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [apartamentoLeido, setApartamentoLeido] = useState<Apartamento | null>(null)
  const [dialogo, setDialogo] = useState<DialogoAbierto | null>(null)
  const [enviandoPago, setEnviandoPago] = useState(false)
  const [exito, setExito] = useState<{ asunto: string; mensaje: string } | null>(
    estadoRecibido?.creada ?? estadoRecibido?.aviso ?? null,
  )
  const [avisoCuenta] = useState(estadoRecibido?.creada?.avisoCuenta ?? null)
  const titulo = useRef<HTMLHeadingElement>(null)
  const enfocarTitulo = useRef(false)
  const pago = useDatosDePago(codigo)
  const clave = `${codigo}#${intento}`
  const listado = `/reservas${volverA}`
  useTitulo(codigo)

  // El aviso de recién creada se muestra una vez: al recargar ya no debe salir
  useEffect(() => {
    if (estadoRecibido?.creada || estadoRecibido?.aviso) {
      navegar(ubicacion.pathname, { replace: true, state: { volverA: estadoRecibido.volverA } })
    }
  }, [estadoRecibido, navegar, ubicacion.pathname])

  // Al terminar una acción, el botón que abrió el diálogo suele desaparecer: el foco queda en el título de la reserva
  useEffect(() => {
    if (dialogo === null && enfocarTitulo.current) {
      enfocarTitulo.current = false
      titulo.current?.focus()
    }
  }, [dialogo])

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

  // DEC-114: la llegada registrada solo existe desde que la reserva está en curso
  const conLlegada = resultado && 'reserva' in resultado && ['EN_CURSO', 'FINALIZADA'].includes(resultado.reserva.estado)
  const estadoLeido = resultado && 'reserva' in resultado ? resultado.reserva.estado : null
  const [estancia, setEstancia] = useState<{ clave: string; datos: Estancia | null; error: boolean } | null>(null)
  const [intentoEstancia, setIntentoEstancia] = useState(0)
  const claveEstancia = `${codigo}#${estadoLeido}#${intentoEstancia}`
  useEffect(() => {
    if (!conLlegada) return
    let vigente = true
    consultarEstancia(codigo)
      .then((datos) => {
        if (vigente) setEstancia({ clave: claveEstancia, datos, error: false })
      })
      .catch(() => {
        if (vigente) setEstancia({ clave: claveEstancia, datos: null, error: true })
      })
    return () => {
      vigente = false
    }
  }, [codigo, conLlegada, claveEstancia])
  const estanciaActual = estancia?.clave === claveEstancia ? estancia : null
  const registroVigente = estanciaActual?.datos?.registros?.find((registro) => !registro.anulado) ?? null

  const apartamento = resultado && 'reserva' in resultado ? resultado.reserva.apartamento : null
  useEffect(() => {
    if (!apartamento) return
    let vigente = true
    // Si no se puede leer el nombre, basta el código: no vale la pena un error en la pantalla
    consultarApartamento(apartamento)
      .then((datos) => {
        if (vigente) setApartamentoLeido({ ...datos, codigo: apartamento })
      })
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [apartamento])

  /** Vuelve a leer la reserva y su pago sin mostrar «cargando»: pudieron cambiar. */
  function actualizar() {
    const leerReserva = consultarReserva(codigo)
      .then((reserva) => setResultado({ clave, reserva }))
      .catch(() => undefined)
    return Promise.all([leerReserva, pago.recargar()])
  }

  const actual = resultado?.clave === clave ? resultado : null
  if (!actual) return <Cargando />

  if ('error' in actual) {
    if (actual.error instanceof ErrorApi && actual.error.estado === 404) {
      return (
        <BloqueVacio
          encabezado="h1"
          icono="reservaNoEncontrada"
          titulo="Reserva no encontrada"
          accion={
            <Link to={listado} className="boton">
              <span aria-hidden="true">←</span> Volver al listado de reservas
            </Link>
          }
        >
          No existe una reserva con el código <span className="codigo codigo-resaltado">{codigo}</span>.
        </BloqueVacio>
      )
    }
    return (
      <div className="detalle">
        <VolverAReservas listado={listado} />
        <Aviso
          tipo="error"
          asunto="No pudimos cargar la reserva"
          mensaje={actual.error instanceof ErrorApi ? actual.error.message : 'Revisa tu conexión e inténtalo de nuevo.'}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      </div>
    )
  }

  const { reserva } = actual
  const esAdministrador = sesion?.rol === 'ADMINISTRADOR'
  const apartamentoActual = apartamentoLeido?.codigo === reserva.apartamento ? apartamentoLeido : null
  const activa = reserva.estado === 'PENDIENTE' || reserva.estado === 'CONFIRMADA' || reserva.estado === 'EN_CURSO'
  // CU-17: solo se modifica antes de iniciar y hasta el día de la entrada
  const modificable = (reserva.estado === 'PENDIENTE' || reserva.estado === 'CONFIRMADA') && reserva.entrada >= fechaHoy()

  async function alConfirmar() {
    await actualizar()
    setExito({ asunto: 'La reserva quedó confirmada', mensaje: 'Ya no vence por falta de confirmación.' })
    enfocarTitulo.current = true
    setDialogo(null)
  }

  async function alLiquidar(liquidacion: Liquidacion, asunto: string) {
    await actualizar()
    const total = noches(reserva.entrada, reserva.salida)
    const retencion = liquidacion.retenido > 0 ? `Se retuvo ${dinero(liquidacion.retenido)}.` : 'No se retuvo ningún valor.'
    setExito({ asunto, mensaje: `Se liberó lo reservado (${cantidad(total, 'noche', 'noches')}). ${retencion}` })
    enfocarTitulo.current = true
  }

  return (
    <div className="detalle">
      {/* Fuera del bloque del título, para que quede fija al bajar por un detalle largo */}
      <div className="barra-volver">
        <VolverAReservas listado={listado} />
      </div>
      <div>
        <div className="detalle-titulo">
          <div className="detalle-codigo">
            <h1 className="codigo" ref={titulo} tabIndex={-1}>
              {reserva.codigo}
            </h1>
            <EtiquetaEstado estado={reserva.estado} />
          </div>
          <AccionesReserva
            reserva={reserva}
            alojamiento={pago.datos?.alojamiento ?? null}
            salidaDisponible={Boolean(pago.datos)}
            esAdministrador={esAdministrador}
            alElegir={(accion) => {
              if (accion === 'llegada') navegar(`/reservas/${encodeURIComponent(reserva.codigo)}/llegada`)
              else setDialogo(accion)
            }}
          />
        </div>
      </div>

      {exito && <Aviso tipo="exito" asunto={exito.asunto} mensaje={exito.mensaje} />}
      {reserva.estado === 'EN_CURSO' && reserva.salida < fechaHoy() && (
        <Aviso
          tipo="advertencia"
          asunto="Salida vencida"
          mensaje={`La salida era el ${fechaCorta(reserva.salida)} y no se ha registrado. Mientras siga en curso, ${reserva.apartamento} sigue ocupado y no se puede entregar a la próxima reserva.`}
        />
      )}
      {avisoCuenta && <Aviso tipo="info" asunto="Cuenta del titular" mensaje={avisoCuenta} />}

      <section aria-labelledby="titulo-datos" className="detalle-seccion">
        <h2 id="titulo-datos">Datos de la reserva</h2>
        <dl className="tarjeta datos">
          <Dato nombre="Apartamento">
            <span className="codigo">{reserva.apartamento}</span>
            {apartamentoActual && ` · ${apartamentoActual.nombre}`}
          </Dato>
          <Dato nombre="Titular">
            {reserva.titularNombre && <span className="dato-titular">{reserva.titularNombre}</span>}
            <span className="codigo titular-id">{reserva.titularId}</span>
          </Dato>
          <Dato nombre="Canal">{NOMBRE_DEL_CANAL[reserva.canalOrigen]}</Dato>
          <Dato nombre="Entrada">{fechaCorta(reserva.entrada)}</Dato>
          <Dato nombre="Salida">{fechaCorta(reserva.salida)}</Dato>
          <Dato nombre="Noches">{noches(reserva.entrada, reserva.salida)}</Dato>
          <Dato nombre="Hora estimada de llegada">
            {reserva.horaEstimadaLlegada ? (
              hora(reserva.horaEstimadaLlegada)
            ) : (
              <span className="suave">Sin informar</span>
            )}
          </Dato>
          {conLlegada && (
            <LlegadaRegistrada estancia={estanciaActual?.datos ?? null} error={Boolean(estanciaActual?.error)}
              alReintentar={() => setIntentoEstancia(intentoEstancia + 1)} />
          )}
          <Dato nombre="Fecha de creación">{fechaCorta(reserva.creadaEn)}</Dato>
          <Dato nombre="Política aplicada">
            <PoliticaAplicada politicaId={reserva.politicaId} codigo={reserva.codigo} />
          </Dato>
          {reserva.noShow?.autorNombre && (
            <Dato nombre="No se presentó">
              Declarado por {reserva.noShow.autorNombre} el {fechaYHora(reserva.noShow.fechaHora)}
            </Dato>
          )}
          <Dato nombre="Vehículos y mascotas">
            Carros: {reserva.carros} · Motos: {reserva.motos} · Mascotas: {reserva.mascotas}
            {modificable && pago.datos && (
              <button
                type="button"
                className="boton boton-secundario boton-chico dato-accion"
                aria-label="Cambiar vehículos y mascotas"
                onClick={() => setDialogo('vehiculos')}
              >
                Cambiar
              </button>
            )}
          </Dato>
        </dl>
      </section>

      <BloquePago reserva={reserva} pago={pago} alRegistrarPago={() => setDialogo('pago')} />

      <NotasTurno codigo={reserva.codigo} activa={activa} />

      <section aria-labelledby="titulo-ocupantes" className="detalle-seccion">
        <h2 id="titulo-ocupantes">Ocupantes ({reserva.ocupantes.length})</h2>
        <div className="tarjeta tabla-marco">
          <table className="tabla tabla-ocupantes" aria-labelledby="titulo-ocupantes">
            <thead>
              <tr>
                <th scope="col">Nombre</th>
                <th scope="col">Documento</th>
                <th scope="col">Nacionalidad</th>
                <th scope="col">Fecha de nacimiento</th>
                {activa && <th scope="col">Datos del TRA</th>}
                {activa && <th scope="col"><span className="solo-lectores">Acciones</span></th>}
              </tr>
            </thead>
            <tbody>
              {reserva.ocupantes.map((ocupante) => (
                <tr key={ocupante.id}>
                  <td className="ocupante-nombre">
                    {ocupante.nombre}
                    {ocupante.principal && (
                      <span className="etiqueta ocupante-principal"><span aria-hidden="true">★</span> Huésped principal</span>
                    )}
                  </td>
                  <td className="codigo">{documento(ocupante)}</td>
                  <td>{ocupante.nacionalidad ?? 'Sin dato'}</td>
                  <td className="ocupante-nacimiento">{nacimiento(ocupante, reserva.entrada)}</td>
                  {activa && (
                    <td className="ocupante-tra">
                      <span className="etiqueta">
                        <span aria-hidden="true">{tieneTraPendiente(ocupante) ? '!' : '✓'}</span> {textoTra(ocupante)}
                      </span>
                    </td>
                  )}
                  {activa && (
                    <td className="ocupante-acciones">
                      <button
                        type="button"
                        className="boton boton-secundario boton-chico"
                        aria-label={`${tieneTraPendiente(ocupante) ? 'Completar' : 'Editar'} datos de ${ocupante.nombre}`}
                        onClick={() => setDialogo({ tra: ocupante.id })}
                      >
                        {tieneTraPendiente(ocupante) ? 'Completar datos' : 'Editar datos'}
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section aria-labelledby="titulo-desglose" className="detalle-seccion">
        <h2 id="titulo-desglose">Desglose por noche</h2>
        <TablaDesglose idTitulo="titulo-desglose" desglose={reserva.valor.desglose} total={reserva.valor.total} />
      </section>

      {dialogo === 'confirmar' && (
        <DialogoConfirmar
          reserva={reserva}
          datos={pago.datos}
          alCerrar={() => setDialogo(null)}
          alConfirmar={alConfirmar}
          alFallar={actualizar}
          alRegistrarPago={pago.ponerFolio}
        />
      )}
      {(dialogo === 'cancelar' || dialogo === 'noShow') && (
        <DialogoLiquidar
          tipo={dialogo}
          reserva={reserva}
          alCerrar={() => setDialogo(null)}
          alTerminar={(liquidacion) =>
            alLiquidar(liquidacion, dialogo === 'cancelar' ? 'Reserva cancelada' : 'No-show declarado')}
          alFallar={actualizar}
        />
      )}
      {dialogo === 'salida' && pago.datos && (
        <DialogoSalida
          reserva={reserva}
          folio={pago.datos.folio}
          esAdministrador={esAdministrador}
          medios={mediosDeRecepcion(pago.datos.alojamiento)}
          alCerrar={() => {
            enfocarTitulo.current = true
            setDialogo(null)
          }}
          alCambiarFolio={pago.ponerFolio}
          alTerminar={actualizar}
          alFallar={actualizar}
          alCompletarTra={(ocupante) => setDialogo({ tra: ocupante, volverASalida: true })}
        />
      )}
      {dialogo === 'corregir' && (
        <DialogoCorregirRegistro
          reserva={reserva}
          vigente={registroVigente?.fechaHora}
          alCerrar={() => {
            enfocarTitulo.current = true
            setDialogo(null)
          }}
          alCorregir={(nueva) => {
            setEstancia({ clave: claveEstancia, datos: nueva, error: false })
            setExito({ asunto: 'Registro de llegada corregido', mensaje: 'El registro anterior queda anulado como historial.' })
          }}
        />
      )}
      {typeof dialogo === 'object' && dialogo !== null && (
        <DialogoTra
          reserva={reserva}
          ocupanteId={dialogo.tra}
          alCambiar={(nueva) => setResultado({ clave, reserva: nueva })}
          alCerrar={() => setDialogo(dialogo.volverASalida ? 'salida' : null)}
        />
      )}
      {dialogo === 'vehiculos' && pago.datos && (
        <DialogoVehiculos
          reserva={reserva}
          alojamiento={pago.datos.alojamiento}
          limiteApartamento={apartamentoActual?.limiteMascotas ?? null}
          alCerrar={() => setDialogo(null)}
          alModificar={(nueva) => {
            setResultado({ clave, reserva: nueva })
            void pago.recargar()
            setExito({ asunto: 'Vehículos y mascotas actualizados', mensaje: 'La diferencia quedó en el folio.' })
            setDialogo(null)
          }}
        />
      )}
      {dialogo === 'pago' && pago.datos && (
        <Dialogo
          titulo="Registrar pago"
          subtitulo={<>{reserva.codigo} · Saldo <span className="codigo">{dinero(pago.datos.folio.saldo)}</span></>}
          ocupado={enviandoPago}
          alCerrar={() => setDialogo(null)}
        >
          <FormularioPago
            codigo={reserva.codigo}
            medios={mediosDeRecepcion(pago.datos.alojamiento)}
            montoInicial={montoSugerido(reserva, pago.datos.folio)}
            textoVolver="Volver"
            alVolver={() => setDialogo(null)}
            alEnviar={setEnviandoPago}
            alRegistrar={(folio) => {
              pago.ponerFolio(folio)
              setExito({ asunto: 'Pago registrado', mensaje: 'El bloque Pago ya muestra el nuevo saldo.' })
              setDialogo(null)
            }}
          />
        </Dialogo>
      )}
    </div>
  )
}

function VolverAReservas({ listado }: { listado: string }) {
  return (
    <Link to={listado} className="volver">
      <span aria-hidden="true">←</span> Volver a reservas
    </Link>
  )
}

function documento({ tipoDocumento, numeroDocumento }: Ocupante): string {
  return numeroDocumento ? `${tipoDocumento ?? ''} ${numeroDocumento}`.trim() : 'Sin documento'
}

/** La edad se muestra a la fecha de entrada: es la que decide si el ocupante paga (OCU). */
function nacimiento({ fechaNacimiento }: Ocupante, entrada: string): string {
  if (!fechaNacimiento) return 'Sin dato'
  return `${fechaCorta(fechaNacimiento)} · ${textoEdad(fechaNacimiento, entrada)}`
}

function Cargando() {
  return (
    <div className="detalle" aria-busy="true">
      <div className="detalle-codigo">
        <span className="fantasma fantasma-titulo" />
        <span className="fantasma fantasma-etiqueta" />
      </div>
      <div className="tarjeta fantasma-datos">
        {Array.from({ length: 6 }, (_, celda) => (
          <span key={celda} className="fantasma" />
        ))}
      </div>
      <p className="suave" role="status">
        Cargando la reserva…
      </p>
    </div>
  )
}
