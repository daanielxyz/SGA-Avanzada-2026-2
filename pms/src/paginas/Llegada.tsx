import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { consultarApartamento, type Apartamento } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import {
  confirmarReserva,
  consultarReserva,
  designarPrincipal,
  indicarHoraLlegada,
  NOMBRE_DEL_ESTADO,
  registrarLlegada,
  type Reserva,
} from '../api/reservas'
import { consultarTitular, type Titular } from '../api/titulares'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import Dialogo from '../componentes/Dialogo'
import EtiquetaEstado from '../componentes/EtiquetaEstado'
import TablaDesglose from '../componentes/TablaDesglose'
import { useTitulo } from '../componentes/useTitulo'
import CampoMotivo from '../folio/CampoMotivo'
import { cantidad, dinero, edadEn, fechaCorta, fechaHoy, noches } from '../formato'
import FormularioPago from '../reserva/FormularioPago'
import NotasTurno from '../reserva/NotasTurno'
import DialogoTra from '../reserva/llegada/DialogoTra'
import { DialogoTitular, DialogoValidar, DialogoVehiculos } from '../reserva/llegada/DialogosLlegada'
import {
  bloqueoDe,
  documentoDe,
  faltaPorCobrar,
  fechaYHora,
  MOTIVO_MAXIMO,
  NOMBRE_ESTADO_OPERATIVO,
  pistaDelBloqueo,
  puedeSerPrincipal,
  textoDelBoton,
  textoTra,
  tieneTraPendiente,
  titularSeAloja,
  type Situacion,
} from '../reserva/llegada/llegada'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import { faltaParaAnticipo, mediosDeRecepcion } from '../reserva/pago'
import { useDatosDePago } from '../reserva/useDatosDePago'

type Resultado = { clave: string; reserva: Reserva } | { clave: string; error: unknown }
type DialogoAbierto = { tipo: 'tra'; ocupante: string } | { tipo: 'vehiculos' | 'titular' | 'validar' | 'pago' }
/** Lo que manda otra pantalla al llegar aquí, como «Nueva reserva» al crear una de mostrador. */
export interface AvisoAlLlegar {
  asunto: string
  mensaje: string
}

/** HH:mm de este momento: el huésped está en el mostrador, así que llega ahora (RN-09). */
function horaActual(ahora = new Date()): string {
  return `${String(ahora.getHours()).padStart(2, '0')}:${String(ahora.getMinutes()).padStart(2, '0')}`
}

/**
 * Registro de llegada (CU-21): en una sola página se cobra, se completan los datos del TRA y se registra. Arriba dice
 * qué falta para entregar el apartamento; el TRA incompleto no lo impide, pero bloquea la salida (DEC-96). Una reserva
 * de mostrador llega pendiente: se confirma y se registra con el mismo botón.
 */
export default function Llegada() {
  const { codigo = '' } = useParams()
  const ubicacion = useLocation()
  const navegar = useNavigate()
  const [avisoRecibido] = useState((ubicacion.state as { aviso?: AvisoAlLlegar } | null)?.aviso ?? null)
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [apartamento, setApartamento] = useState<{ codigo: string; datos: Apartamento | null } | null>(null)
  const [titular, setTitular] = useState<{ id: string; datos: Titular | null } | null>(null)
  const [dialogo, setDialogo] = useState<DialogoAbierto | null>(null)
  const [enviandoPago, setEnviandoPago] = useState(false)
  const [exito, setExito] = useState<AvisoAlLlegar | null>(null)
  const [sinCobro, setSinCobro] = useState(false)
  const [motivo, setMotivo] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [falla, setFalla] = useState<(ErrorMostrado & { confirmada: boolean }) | null>(null)
  const [principal, setPrincipal] = useState<{ ocupante: string; error: ErrorMostrado | null } | null>(null)
  const titulo = useRef<HTMLHeadingElement>(null)
  const pago = useDatosDePago(codigo)
  const clave = `${codigo}#${intento}`
  useTitulo(`Llegada ${codigo}`)

  // El aviso de llegada se muestra una vez: al recargar ya no debe salir
  useEffect(() => {
    if (avisoRecibido) navegar(ubicacion.pathname, { replace: true, state: null })
  }, [avisoRecibido, navegar, ubicacion.pathname])

  // Al cerrar un diálogo el foco vuelve al botón que lo abrió; si ese botón ya no está (el huésped quedó completo, la
  // identidad se validó), el foco iría al principio de la página: se lleva al título, donde está el aviso
  useEffect(() => {
    if (dialogo === null && document.activeElement === document.body) titulo.current?.focus()
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

  const reserva = resultado?.clave === clave && 'reserva' in resultado ? resultado.reserva : null
  const codigoApartamento = reserva?.apartamento ?? null
  const titularId = reserva?.titularId ?? null

  useEffect(() => {
    if (!codigoApartamento) return
    let vigente = true
    // Sin el apartamento la página sigue: si no está preparado, el backend lo rechaza al registrar
    consultarApartamento(codigoApartamento)
      .then((datos) => {
        if (vigente) setApartamento({ codigo: codigoApartamento, datos })
      })
      .catch(() => {
        if (vigente) setApartamento({ codigo: codigoApartamento, datos: null })
      })
    return () => {
      vigente = false
    }
  }, [codigoApartamento])

  useEffect(() => {
    if (!titularId) return
    let vigente = true
    consultarTitular(titularId)
      .then((datos) => {
        if (vigente) setTitular({ id: titularId, datos })
      })
      .catch(() => {
        if (vigente) setTitular({ id: titularId, datos: null })
      })
    return () => {
      vigente = false
    }
  }, [titularId])

  const actual = resultado?.clave === clave ? resultado : null
  const volver = (
    <Link to={`/reservas/${encodeURIComponent(codigo)}`} className="volver">
      <span aria-hidden="true">←</span> Volver a la reserva
    </Link>
  )

  if (!actual || (pago.cargando && !('error' in actual))) return <Cargando />

  if ('error' in actual) {
    if (actual.error instanceof ErrorApi && actual.error.estado === 404) {
      return (
        <BloqueVacio
          encabezado="h1"
          icono="reservaNoEncontrada"
          titulo="No encontramos esa reserva"
          accion={<Link to="/reservas" className="boton"><span aria-hidden="true">←</span> Volver a reservas</Link>}
        >
          No hay ninguna reserva con el código <span className="codigo codigo-resaltado">{codigo}</span>.
        </BloqueVacio>
      )
    }
    return <ErrorAlCargar volver={volver} error={actual.error} alReintentar={() => setIntento(intento + 1)} />
  }
  if (!pago.datos) return <ErrorAlCargar volver={volver} error={pago.error} alReintentar={pago.reintentar} />

  const { reserva: datosReserva } = actual
  const { folio, alojamiento } = pago.datos

  if (datosReserva.estado !== 'PENDIENTE' && datosReserva.estado !== 'CONFIRMADA') {
    return (
      <BloqueVacio
        encabezado="h1"
        icono="reservaNoEncontrada"
        titulo={datosReserva.estado === 'EN_CURSO' ? 'La llegada ya se registró' : 'Esta reserva ya no espera la llegada'}
        accion={<Link to={`/reservas/${encodeURIComponent(codigo)}`} className="boton"><span aria-hidden="true">←</span> Volver a la reserva</Link>}
      >
        La reserva <span className="codigo codigo-resaltado">{codigo}</span>{' '}
        {datosReserva.estado === 'EN_CURSO' ? 'ya está en curso.' : `quedó como «${NOMBRE_DEL_ESTADO[datosReserva.estado]}».`}
      </BloqueVacio>
    )
  }

  const hoy = fechaHoy()
  const apartamentoActual = apartamento?.codigo === datosReserva.apartamento ? apartamento.datos : null
  const titularActual = titular?.id === datosReserva.titularId ? titular : null
  const falta = faltaPorCobrar(datosReserva, folio)
  const faltaAnticipo = datosReserva.estado === 'PENDIENTE' ? faltaParaAnticipo(datosReserva, folio) : 0
  const situacion: Situacion = {
    estadoApartamento: apartamentoActual?.estadoOperativo ?? null,
    falta,
    faltaAnticipo,
    antesDeEntrada: hoy < datosReserva.entrada,
    sinCobro: falta > 0 && sinCobro,
    motivo,
  }
  const bloqueo = bloqueoDe(situacion)
  const pista = pistaDelBloqueo(bloqueo, situacion.sinCobro)
  const pendientesTra = datosReserva.ocupantes.filter(tieneTraPendiente).length
  const totalNoches = noches(datosReserva.entrada, datosReserva.salida)
  const aFavor = folio.situacionSaldo === 'A_FAVOR' ? folio.saldo : 0
  const enviarMotivo = situacion.sinCobro && motivo.trim() ? motivo.trim() : undefined

  /** Vuelve a leer todo sin mostrar «cargando»: otro turno pudo cobrar o preparar el apartamento. */
  function actualizar() {
    const leer = consultarReserva(codigo).then((nueva) => setResultado({ clave, reserva: nueva })).catch(() => undefined)
    const leerApartamento = consultarApartamento(datosReserva.apartamento)
      .then((datos) => setApartamento({ codigo: datos.codigo, datos }))
      .catch(() => undefined)
    return Promise.all([leer, leerApartamento, pago.recargar()])
  }

  const ponerReserva = (nueva: Reserva) => setResultado({ clave, reserva: nueva })

  async function registrar() {
    setFalla(null)
    setEnviando(true)
    let confirmada = false
    try {
      if (datosReserva.estado === 'PENDIENTE') {
        // RN-09: confirmar exige la hora de llegada; el huésped ya está aquí
        if (!datosReserva.horaEstimadaLlegada) await indicarHoraLlegada(codigo, horaActual())
        ponerReserva(await confirmarReserva(codigo))
        confirmada = true
      }
      await registrarLlegada(codigo, enviarMotivo)
      navegar(`/reservas/${encodeURIComponent(codigo)}`, {
        state: {
          aviso: {
            asunto: 'Llegada registrada',
            mensaje: `La reserva quedó en curso y ${datosReserva.apartamento} ocupado.${enviarMotivo ? ' Entró sin pagar la estancia: el motivo quedó en el registro.' : ''}`,
          },
        },
      })
    } catch (causa) {
      setFalla({ ...errorParaMostrar(causa), confirmada })
      setEnviando(false)
      titulo.current?.scrollIntoView?.({ block: 'start' })
      await actualizar()
    }
  }

  async function hacerPrincipal(ocupante: string) {
    setPrincipal({ ocupante, error: null })
    try {
      const nueva = await designarPrincipal(codigo, ocupante)
      ponerReserva(nueva)
      setPrincipal(null)
      const elegido = nueva.ocupantes.find((o) => o.id === ocupante)
      setExito({ asunto: 'Huésped principal cambiado', mensaje: `${elegido?.nombre ?? 'Ese huésped'} va de primero en el TRA.` })
      // Su botón desaparece: el foco va al título, junto al aviso
      titulo.current?.focus()
    } catch (causa) {
      setPrincipal({ ocupante, error: errorParaMostrar(causa) })
    }
  }

  const cerrar = () => setDialogo(null)
  const nombreApartamento = apartamentoActual ? ` · ${apartamentoActual.nombre}` : ''
  const estadoApartamento = situacion.estadoApartamento
  const titularDatos = titularActual?.datos ?? null

  return (
    <div className="detalle llegada">
      <div>
        {volver}
        <p className="folio-rotulo">Llegada</p>
        <div className="detalle-codigo">
          <h1 className="codigo" ref={titulo} tabIndex={-1}>{datosReserva.codigo}</h1>
          <EtiquetaEstado estado={datosReserva.estado} />
        </div>
        <p className="folio-subtitulo">
          <span className="codigo">{datosReserva.apartamento}</span>{nombreApartamento} · {fechaCorta(datosReserva.entrada)}{' '}
          <span aria-hidden="true">→</span><span className="solo-lectores">al</span> {fechaCorta(datosReserva.salida)} ·{' '}
          {cantidad(totalNoches, 'noche', 'noches')}
        </p>
      </div>

      {avisoRecibido && <Aviso tipo="info" asunto={avisoRecibido.asunto} mensaje={avisoRecibido.mensaje} />}
      {falla && (
        <Aviso
          tipo="error"
          asunto={falla.confirmada ? 'La reserva quedó confirmada, pero no se registró la llegada' : 'No se registró la llegada'}
          mensaje={`${falla.mensaje.replace(/\.?$/, '.')} Lo que escribiste se conserva.`}
          detalles={falla.detalles}
        />
      )}
      {exito && <Aviso tipo="exito" asunto={exito.asunto} mensaje={exito.mensaje} />}

      <section className="tarjeta lo-que-falta" aria-label="Lo que falta">
        <div>
          <h2>Para registrar la llegada</h2>
          <ul>
            {estadoApartamento === null ? (
              <Condicion tipo="aviso" titulo="Apartamento preparado">
                {apartamento?.codigo === datosReserva.apartamento
                  ? `No pudimos consultar el estado de ${datosReserva.apartamento}: al registrar se revisa.`
                  : `Consultando el estado de ${datosReserva.apartamento}…`}
              </Condicion>
            ) : (
              <Condicion tipo={estadoApartamento === 'PREPARADO' ? 'listo' : 'falta'} titulo="Apartamento preparado">
                {datosReserva.apartamento} · {NOMBRE_ESTADO_OPERATIVO[estadoApartamento] ?? estadoApartamento}
                {estadoApartamento !== 'PREPARADO' && '. Debe prepararlo el personal de servicio.'}
              </Condicion>
            )}
            {datosReserva.estado === 'PENDIENTE' && (datosReserva.anticipoExigido ?? 0) > 0 && (
              <Condicion
                tipo={faltaAnticipo > 0 ? 'falta' : 'listo'}
                titulo={faltaAnticipo > 0 ? `Cubrir el anticipo: ${dinero(faltaAnticipo)}` : 'Anticipo cubierto'}
              >
                {faltaAnticipo > 0 ? 'Lo exige la confirmación, aunque entre sin pagar la estancia.' : 'Ya se puede confirmar.'}
              </Condicion>
            )}
            <Condicion tipo={falta === 0 ? 'listo' : 'falta'} titulo="Estancia pagada">
              {falta === 0 ? `Pagado ${dinero(folio.totalPagado)}` : `Falta por cobrar ${dinero(falta)}`}
            </Condicion>
          </ul>
        </div>
        <div>
          <h2>No impide registrar</h2>
          <ul>
            <Condicion tipo={pendientesTra === 0 ? 'listo' : 'aviso'} titulo="Datos del TRA">
              {pendientesTra === 0
                ? `Completos para ${datosReserva.ocupantes.length === 1 ? 'el huésped' : `los ${datosReserva.ocupantes.length} huéspedes`}.`
                : `Faltan datos de ${pendientesTra} de ${cantidad(datosReserva.ocupantes.length, 'huésped', 'huéspedes')}. La salida quedará bloqueada hasta completar estos datos.`}
            </Condicion>
          </ul>
        </div>
      </section>

      <NotasTurno codigo={datosReserva.codigo} activa soloPendientes />

      <section aria-labelledby="titulo-cobro" className="detalle-seccion">
        <h2 id="titulo-cobro">Cobro</h2>
        <div className="cobro">
          <div className="detalle-seccion">
            <h3 id="titulo-desglose-llegada">Desglose por noche</h3>
            <TablaDesglose idTitulo="titulo-desglose-llegada" desglose={datosReserva.valor.desglose} total={datosReserva.valor.total} />
          </div>
          <div className="tarjeta falta-por-cobrar">
            <p className="folio-cifra-nombre">Falta por cobrar</p>
            <p className="codigo folio-cifra-grande">{dinero(falta)}</p>
            {aFavor > 0 && (
              <Aviso
                tipo="info"
                asunto={`Saldo a favor de ${dinero(aFavor)}`}
                mensaje="Se pagó de más tras un cambio en la reserva. Revísalo en el folio."
              />
            )}
            <dl className="lineas">
              <dt>Estancia ({cantidad(totalNoches, 'noche', 'noches')})</dt>
              <dd>{dinero(datosReserva.valor.total)}</dd>
              {datosReserva.extras.parqueadero > 0 && (
                <>
                  <dt>Parqueadero ({vehiculos(datosReserva)})</dt>
                  <dd>{dinero(datosReserva.extras.parqueadero)}</dd>
                </>
              )}
              {datosReserva.extras.mascotas > 0 && (
                <>
                  <dt>Mascotas ({datosReserva.mascotas})</dt>
                  <dd>{dinero(datosReserva.extras.mascotas)}</dd>
                </>
              )}
              <div className="lineas-separador" />
              <dt className="lineas-fuerte">Total</dt>
              <dd className="lineas-fuerte">{dinero(datosReserva.valor.total + datosReserva.extras.total)}</dd>
              <dt>Pagado</dt>
              <dd>{dinero(folio.totalPagado)}</dd>
            </dl>
            {!folio.cerrado && (
              <button type="button" className="boton boton-secundario boton-bloque" onClick={() => setDialogo({ tipo: 'pago' })}>
                Registrar pago
              </button>
            )}
            <Link to={`/reservas/${encodeURIComponent(codigo)}/folio`} className="pago-ver-folio">
              Ver folio <span aria-hidden="true">→</span>
            </Link>
          </div>
        </div>
      </section>

      <section aria-labelledby="titulo-huespedes" className="detalle-seccion">
        <h2 id="titulo-huespedes">Huéspedes ({datosReserva.ocupantes.length})</h2>
        <TablaHuespedes
          reserva={datosReserva}
          idTitulo="titulo-huespedes"
          principal={principal}
          alCompletar={(ocupante) => setDialogo({ tipo: 'tra', ocupante })}
          alHacerPrincipal={hacerPrincipal}
        />
      </section>

      <div className="llegada-pareja">
        <section aria-labelledby="titulo-titular" className="tarjeta bloque-llegada">
          <div className="bloque-llegada-cabecera">
            <h2 id="titulo-titular">Titular</h2>
            <div className="bloque-llegada-acciones">
              {titularDatos && !titularDatos.validado && titularSeAloja(titularDatos, datosReserva.ocupantes) && (
                <button type="button" className="boton boton-secundario boton-chico" onClick={() => setDialogo({ tipo: 'validar' })}>
                  Validar identidad
                </button>
              )}
              <button type="button" className="boton boton-secundario boton-chico" onClick={() => setDialogo({ tipo: 'titular' })}>
                Cambiar titular
              </button>
            </div>
          </div>
          <p className="suave">El titular responde por la reserva y su folio, y puede no alojarse.</p>
          {!titularActual && <p className="suave" role="status">Cargando el titular…</p>}
          {titularActual && !titularDatos && (
            <p className="suave">
              {datosReserva.titularNombre ?? datosReserva.titularId}. No pudimos cargar sus demás datos.
            </p>
          )}
          {titularDatos && (
            <dl className="datos-titular">
              <div><dt>Nombre</dt><dd>{titularDatos.nombre}</dd></div>
              <div><dt>Documento</dt><dd className="codigo">{documentoDe(titularDatos)}</dd></div>
              <div><dt>Correo</dt><dd>{titularDatos.correo ?? 'Sin dato'}</dd></div>
              <div><dt>Teléfono</dt><dd>{titularDatos.telefono ?? 'Sin dato'}</dd></div>
              <div className="datos-titular-ancho">
                <dt>Identidad</dt>
                <dd>
                  {titularDatos.validado ? (
                    <>
                      <span className="etiqueta"><span aria-hidden="true">✓</span> Validada</span>
                      {titularDatos.validadoEn && <span className="campo-pista"> El {fechaYHora(titularDatos.validadoEn)}</span>}
                    </>
                  ) : (
                    <span className="etiqueta"><span aria-hidden="true">○</span> Sin validar</span>
                  )}
                </dd>
              </div>
            </dl>
          )}
        </section>

        <section aria-labelledby="titulo-vehiculos" className="tarjeta bloque-llegada">
          <div className="bloque-llegada-cabecera">
            <h2 id="titulo-vehiculos">Vehículos y mascotas</h2>
            <button
              type="button"
              className="boton boton-secundario boton-chico"
              aria-label="Cambiar vehículos y mascotas"
              onClick={() => setDialogo({ tipo: 'vehiculos' })}
            >
              Cambiar
            </button>
          </div>
          <dl className="lineas">
            <dt>Parqueadero: {vehiculos(datosReserva) || 'sin vehículos'}</dt>
            <dd>{dinero(datosReserva.extras.parqueadero)}</dd>
            <dt>Mascotas: {datosReserva.mascotas} (una vez por estancia)</dt>
            <dd>{dinero(datosReserva.extras.mascotas)}</dd>
          </dl>
        </section>
      </div>

      <div className={situacion.sinCobro ? 'pie-llegada pie-llegada-abierto' : 'pie-llegada'} role="region" aria-label="Registrar llegada">
        {falta > 0 && (
          <div className="pie-llegada-sin-cobro">
            <label className="casilla">
              <input
                type="checkbox"
                checked={sinCobro}
                disabled={enviando}
                onChange={(evento) => setSinCobro(evento.target.checked)}
              />
              Entra sin pagar la estancia
            </label>
            {sinCobro && (
              <CampoMotivo
                id="motivo-sin-pago"
                rotulo="Motivo"
                valor={motivo}
                maximo={MOTIVO_MAXIMO}
                deshabilitado={enviando}
                pista="Ej.: Cuenta por cobrar a una empresa"
                alCambiar={setMotivo}
              />
            )}
          </div>
        )}
        <div>
          <p className="folio-cifra-nombre">Falta por cobrar</p>
          <p className="codigo pie-llegada-total">{dinero(falta)}</p>
        </div>
        <div className="pie-llegada-accion">
          <button
            type="button"
            className="boton"
            disabled={enviando || bloqueo !== null}
            aria-describedby={pista ? 'pista-llegada' : undefined}
            onClick={registrar}
          >
            {enviando ? 'Registrando llegada…' : textoDelBoton(datosReserva, situacion)}
          </button>
          {pista && !enviando && <p className="campo-pista" id="pista-llegada">{pista}</p>}
          {enviando && <p className="suave" role="status">Registrando la llegada…</p>}
        </div>
      </div>

      {dialogo?.tipo === 'tra' && (
        <DialogoTra reserva={datosReserva} ocupanteId={dialogo.ocupante} alCambiar={ponerReserva} alCerrar={cerrar} />
      )}
      {dialogo?.tipo === 'vehiculos' && (
        <DialogoVehiculos
          reserva={datosReserva}
          alojamiento={alojamiento}
          limiteApartamento={apartamentoActual?.limiteMascotas ?? null}
          alCerrar={cerrar}
          alModificar={(nueva) => {
            ponerReserva(nueva)
            void pago.recargar()
            setExito({ asunto: 'Vehículos y mascotas actualizados', mensaje: 'La diferencia quedó en el folio y el cobro ya la muestra.' })
            cerrar()
          }}
        />
      )}
      {dialogo?.tipo === 'titular' && (
        <DialogoTitular
          reserva={datosReserva}
          alCerrar={cerrar}
          alCambiar={(nueva) => {
            ponerReserva(nueva)
            setExito({ asunto: 'Titular cambiado', mensaje: `Ahora responde por la reserva ${nueva.titularNombre ?? nueva.titularId}.` })
            cerrar()
          }}
        />
      )}
      {dialogo?.tipo === 'validar' && titularDatos && (
        <DialogoValidar
          titular={titularDatos}
          alCerrar={cerrar}
          alValidar={(validado) => {
            setTitular({ id: validado.id, datos: validado })
            setExito({ asunto: 'Identidad validada', mensaje: `${validado.nombre} ya no podrá cambiar su nombre desde su cuenta.` })
            cerrar()
          }}
        />
      )}
      {dialogo?.tipo === 'pago' && (
        <Dialogo
          titulo="Registrar pago"
          subtitulo={<>{datosReserva.codigo} · Falta por cobrar <span className="codigo">{dinero(falta)}</span></>}
          ocupado={enviandoPago}
          alCerrar={cerrar}
        >
          <FormularioPago
            codigo={datosReserva.codigo}
            medios={mediosDeRecepcion(alojamiento)}
            montoInicial={falta}
            textoVolver="Volver"
            alVolver={cerrar}
            alEnviar={setEnviandoPago}
            alRegistrar={(nuevo) => {
              pago.ponerFolio(nuevo)
              setExito({ asunto: 'Pago registrado', mensaje: 'El cobro ya muestra lo que falta.' })
              cerrar()
            }}
          />
        </Dialogo>
      )}
    </div>
  )
}

function vehiculos({ carros, motos }: Reserva): string {
  return [carros > 0 && cantidad(carros, 'carro', 'carros'), motos > 0 && cantidad(motos, 'moto', 'motos')]
    .filter(Boolean)
    .join(' y ')
}

const ICONO = {
  listo: { clase: 'listo', simbolo: '✓', texto: 'Listo' },
  falta: { clase: 'falta', simbolo: '✕', texto: 'Falta' },
  aviso: { clase: 'aviso', simbolo: '!', texto: 'Pendiente' },
} as const

/** Una condición con su forma (✓, ✕, !) y su estado dicho en palabras: no depende solo del color. */
function Condicion({ tipo, titulo, children }: { tipo: keyof typeof ICONO; titulo: string; children: ReactNode }) {
  const icono = ICONO[tipo]
  return (
    <li>
      <span className={`condicion-icono condicion-${icono.clase}`} aria-hidden="true">{icono.simbolo}</span>
      <span>
        <span className="solo-lectores">{icono.texto}: </span>
        {titulo}
        <span className="condicion-detalle">{children}</span>
      </span>
    </li>
  )
}

/** Huéspedes con su papel y el estado de su TRA. La comparten la llegada y el detalle. */
export function TablaHuespedes({ reserva, idTitulo, principal = null, alCompletar, alHacerPrincipal }: {
  reserva: Reserva
  idTitulo: string
  principal?: { ocupante: string; error: ErrorMostrado | null } | null
  alCompletar: (ocupante: string) => void
  /** Sin ella no se ofrece «Hacer principal». */
  alHacerPrincipal?: (ocupante: string) => void
}) {
  return (
    <div className="tarjeta tabla-marco">
      <table className="tabla tabla-huespedes" aria-labelledby={idTitulo}>
        <thead>
          <tr>
            <th scope="col">Nombre</th>
            <th scope="col">Edad</th>
            <th scope="col">Documento</th>
            <th scope="col">Papel</th>
            <th scope="col">Datos del TRA</th>
            <th scope="col"><span className="solo-lectores">Acciones</span></th>
          </tr>
        </thead>
        <tbody>
          {reserva.ocupantes.map((ocupante) => {
            const documento = documentoDe(ocupante)
            const pendiente = tieneTraPendiente(ocupante)
            const enviando = principal?.ocupante === ocupante.id && !principal.error
            const error = principal?.ocupante === ocupante.id ? principal.error : null
            return (
              <tr key={ocupante.id}>
                <td className="huesped-nombre">{ocupante.nombre}</td>
                <td className="numero">
                  {ocupante.fechaNacimiento ? (
                    <><span className="solo-lectores">Edad: </span>{edadEn(ocupante.fechaNacimiento, reserva.entrada)}</>
                  ) : <span className="suave">Sin dato</span>}
                </td>
                <td>{documento ? <span className="codigo">{documento}</span> : <span className="suave">Sin documento</span>}</td>
                <td>
                  {ocupante.principal ? (
                    <span className="etiqueta"><span aria-hidden="true">★</span> Huésped principal</span>
                  ) : 'Acompañante'}
                </td>
                <td>
                  <span className="etiqueta">
                    <span aria-hidden="true">{pendiente ? '!' : '✓'}</span> {textoTra(ocupante)}
                  </span>
                </td>
                <td className="huesped-acciones">
                  {pendiente && (
                    <button
                      type="button"
                      className="boton boton-secundario boton-chico"
                      aria-label={`Completar datos de ${ocupante.nombre}`}
                      onClick={() => alCompletar(ocupante.id)}
                    >
                      Completar datos
                    </button>
                  )}
                  {alHacerPrincipal && puedeSerPrincipal(ocupante, reserva.entrada) && (
                    <button
                      type="button"
                      className="boton boton-secundario boton-chico"
                      aria-label={`Hacer principal a ${ocupante.nombre}`}
                      disabled={Boolean(principal && !principal.error)}
                      onClick={() => alHacerPrincipal(ocupante.id)}
                    >
                      {enviando ? 'Haciendo principal…' : 'Hacer principal'}
                    </button>
                  )}
                  {error && (
                    <p className="campo-mensaje" role="alert">No se pudo hacer principal: {error.mensaje}</p>
                  )}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function ErrorAlCargar({ volver, error, alReintentar }: { volver: ReactNode; error: unknown; alReintentar: () => void }) {
  return (
    <div className="detalle">
      <div>{volver}</div>
      <Aviso
        tipo="error"
        asunto="No pudimos cargar la llegada"
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
      <div className="tarjeta fantasma-datos">
        {Array.from({ length: 6 }, (_, celda) => (
          <span key={celda} className="fantasma" />
        ))}
      </div>
      <p className="suave" role="status">Cargando la llegada…</p>
    </div>
  )
}
