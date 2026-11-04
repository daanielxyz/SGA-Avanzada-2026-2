import { useEffect, useState, type ReactNode } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { consultarReserva, NOMBRE_DEL_CANAL, type Ocupante, type Reserva } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import EtiquetaEstado from '../componentes/EtiquetaEstado'
import { dinero, edadEn, fechaCorta, hora, noches } from '../formato'

type Resultado = { clave: string; reserva: Reserva } | { clave: string; error: unknown }

/** Detalle de una reserva (CU-49): sus datos, su grupo y el valor congelado por noche. */
export default function DetalleReserva() {
  const { codigo = '' } = useParams()
  const volverA = (useLocation().state as { volverA?: string } | null)?.volverA ?? ''
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const clave = `${codigo}#${intento}`
  const listado = `/reservas${volverA}`

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

  const actual = resultado?.clave === clave ? resultado : null
  if (!actual) return <Cargando />

  if ('error' in actual) {
    if (actual.error instanceof ErrorApi && actual.error.estado === 404) {
      return (
        <BloqueVacio
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
  return (
    <div className="detalle">
      <div>
        <VolverAReservas listado={listado} />
        <div className="detalle-titulo">
          <div className="detalle-codigo">
            <h1 className="codigo">{reserva.codigo}</h1>
            <EtiquetaEstado estado={reserva.estado} />
          </div>
          {/* TODO(equipo): confirmar, cancelar y no-show llegan en el sprint 2 */}
          <div className="espacio-reservado">Espacio reservado · acciones (sprint 2)</div>
        </div>
      </div>

      <section aria-labelledby="titulo-datos" className="detalle-seccion">
        <h2 id="titulo-datos">Datos de la reserva</h2>
        <dl className="tarjeta datos">
          <Dato nombre="Apartamento" codigo>
            {reserva.apartamento}
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
          <Dato nombre="Vehículos">{reserva.vehiculos}</Dato>
          <Dato nombre="Mascotas">{reserva.mascotas}</Dato>
        </dl>
      </section>

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
        <div className="tarjeta tabla-marco">
          <div className="desplazable">
            <table className="tabla tabla-desglose" aria-labelledby="titulo-desglose">
              <thead>
                <tr>
                  <th scope="col">Noche</th>
                  <th scope="col">Temporada</th>
                  <th scope="col" className="derecha">
                    Tarifa por ocupante facturable
                  </th>
                  <th scope="col" className="derecha">
                    Ocupantes facturables
                  </th>
                  <th scope="col" className="derecha">
                    Subtotal
                  </th>
                </tr>
              </thead>
              <tbody>
                {reserva.valor.desglose.map((noche) => (
                  <tr key={noche.noche}>
                    <td>{fechaCorta(noche.noche)}</td>
                    <td>{noche.temporada}</td>
                    <td className="derecha codigo">{dinero(noche.tarifa)}</td>
                    <td className="derecha numero">{noche.ocupantesFacturables}</td>
                    <td className="derecha codigo">{dinero(noche.subtotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="desglose-total">
            <span className="desglose-total-rotulo">Total</span>
            <span className="codigo desglose-total-valor">{dinero(reserva.valor.total)}</span>
          </p>
        </div>
      </section>
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

function Dato({ nombre, codigo = false, children }: {
  nombre: string
  codigo?: boolean
  children: ReactNode
}) {
  return (
    <div className="dato">
      <dt>{nombre}</dt>
      <dd className={codigo ? 'codigo' : undefined}>{children}</dd>
    </div>
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
