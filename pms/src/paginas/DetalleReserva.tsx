import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { consultarApartamento } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { consultarReserva, NOMBRE_DEL_CANAL, type Liquidacion, type Ocupante, type Reserva } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import Dato from '../componentes/Dato'
import Dialogo from '../componentes/Dialogo'
import EtiquetaEstado from '../componentes/EtiquetaEstado'
import TablaDesglose from '../componentes/TablaDesglose'
import { useTitulo } from '../componentes/useTitulo'
import { cantidad, dinero, edadEn, fechaCorta, hora, noches } from '../formato'
import AccionesReserva, { type Accion } from '../reserva/AccionesReserva'
import BloquePago from '../reserva/BloquePago'
import DialogoConfirmar from '../reserva/DialogoConfirmar'
import DialogoLiquidar from '../reserva/DialogoLiquidar'
import FormularioPago from '../reserva/FormularioPago'
import { mediosDeRecepcion, montoSugerido } from '../reserva/pago'
import { useDatosDePago } from '../reserva/useDatosDePago'

type Resultado = { clave: string; reserva: Reserva } | { clave: string; error: unknown }

/** Lo que manda «Nueva reserva» al terminar. */
interface ReservaRecienCreada {
  asunto: string
  mensaje: string
  avisoCuenta: string | null
}

/** Detalle de una reserva (CU-49): sus datos, su pago, su grupo, el valor por noche y lo que se puede hacer con ella. */
export default function DetalleReserva() {
  const { codigo = '' } = useParams()
  const ubicacion = useLocation()
  const navegar = useNavigate()
  const estadoRecibido = ubicacion.state as { volverA?: string; creada?: ReservaRecienCreada } | null
  const volverA = estadoRecibido?.volverA ?? ''
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [nombreApartamento, setNombreApartamento] = useState<{ codigo: string; nombre: string } | null>(null)
  const [dialogo, setDialogo] = useState<Accion | 'pago' | null>(null)
  const [enviandoPago, setEnviandoPago] = useState(false)
  const [exito, setExito] = useState<{ asunto: string; mensaje: string } | null>(estadoRecibido?.creada ?? null)
  const [avisoCuenta] = useState(estadoRecibido?.creada?.avisoCuenta ?? null)
  const titulo = useRef<HTMLHeadingElement>(null)
  const enfocarTitulo = useRef(false)
  const pago = useDatosDePago(codigo)
  const clave = `${codigo}#${intento}`
  const listado = `/reservas${volverA}`
  useTitulo(codigo)

  // El aviso de recién creada se muestra una vez: al recargar ya no debe salir
  useEffect(() => {
    if (estadoRecibido?.creada) {
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

  const apartamento = resultado && 'reserva' in resultado ? resultado.reserva.apartamento : null
  useEffect(() => {
    if (!apartamento) return
    let vigente = true
    // Si no se puede leer el nombre, basta el código: no vale la pena un error en la pantalla
    consultarApartamento(apartamento)
      .then(({ nombre }) => {
        if (vigente) setNombreApartamento({ codigo: apartamento, nombre })
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
      <div>
        <VolverAReservas listado={listado} />
        <div className="detalle-titulo">
          <div className="detalle-codigo">
            <h1 className="codigo" ref={titulo} tabIndex={-1}>
              {reserva.codigo}
            </h1>
            <EtiquetaEstado estado={reserva.estado} />
          </div>
          <AccionesReserva reserva={reserva} alojamiento={pago.datos?.alojamiento ?? null} alElegir={setDialogo} />
        </div>
      </div>

      {exito && <Aviso tipo="exito" asunto={exito.asunto} mensaje={exito.mensaje} />}
      {avisoCuenta && <Aviso tipo="info" asunto="Cuenta del titular" mensaje={avisoCuenta} />}

      <section aria-labelledby="titulo-datos" className="detalle-seccion">
        <h2 id="titulo-datos">Datos de la reserva</h2>
        <dl className="tarjeta datos">
          <Dato nombre="Apartamento">
            <span className="codigo">{reserva.apartamento}</span>
            {nombreApartamento?.codigo === reserva.apartamento && ` · ${nombreApartamento.nombre}`}
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
          <Dato nombre="Fecha de creación">{fechaCorta(reserva.creadaEn)}</Dato>
          <Dato nombre="Política aplicada" codigo>
            {reserva.politicaId}
          </Dato>
          <Dato nombre="Carros">{reserva.carros}</Dato>
          <Dato nombre="Motos">{reserva.motos}</Dato>
          <Dato nombre="Mascotas">{reserva.mascotas}</Dato>
        </dl>
      </section>

      <BloquePago reserva={reserva} pago={pago} alRegistrarPago={() => setDialogo('pago')} />

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
              </tr>
            </thead>
            <tbody>
              {reserva.ocupantes.map((ocupante) => (
                <tr key={ocupante.id}>
                  <td className="ocupante-nombre">{ocupante.nombre}</td>
                  <td className="codigo">{documento(ocupante)}</td>
                  <td>{ocupante.nacionalidad ?? 'Sin dato'}</td>
                  <td className="ocupante-nacimiento">{nacimiento(ocupante, reserva.entrada)}</td>
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
            montoInicial={montoSugerido(reserva, pago.datos.folio, pago.datos.alojamiento)}
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
  const edad = edadEn(fechaNacimiento, entrada)
  return `${fechaCorta(fechaNacimiento)} · ${edad} ${edad === 1 ? 'año' : 'años'}`
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
