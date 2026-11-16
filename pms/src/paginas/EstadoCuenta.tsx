import { useEffect, useState, type CSSProperties, type ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { consultarAlojamiento, type Alojamiento } from '../api/alojamiento'
import { ErrorApi } from '../api/cliente'
import { consultarFolio, type Folio } from '../api/folio'
import { consultarPolitica, type Politica } from '../api/politicas'
import { consultarReserva, NOMBRE_DEL_ESTADO, type Reserva } from '../api/reservas'
import { consultarTitular, type Titular } from '../api/titulares'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import { useTitulo } from '../componentes/useTitulo'
import {
  documentoLegible, lineasDeCargos, lineasDePagos, lugarDelAlojamiento, TEXTO_SALDO, totales,
} from '../estadoCuenta/logica'
import { dinero, fechaCorta, fechaHoy, hora, noches } from '../formato'
import { textoRetencion, textoTramo, tramosOrdenados } from '../reserva/politica'
import { lineaIva } from '../reserva/valor'

interface Datos {
  reserva: Reserva
  folio: Folio
  alojamiento: Alojamiento
  politica: Politica
  /** Nulo si no se pudo leer: el documento sigue con el nombre que trae la reserva. */
  titular: Titular | null
  /** Cuándo se preparó: si la página queda abierta, la hoja dice la hora en que se leyeron los datos. */
  emitido: Date
}

type Resultado = { clave: string; datos: Datos } | { clave: string; error: unknown }

async function leer(codigo: string): Promise<Datos> {
  const reserva = await consultarReserva(codigo)
  const [folio, alojamiento, politica, titular] = await Promise.all([
    consultarFolio(codigo),
    consultarAlojamiento(),
    consultarPolitica(reserva.politicaId),
    consultarTitular(reserva.titularId).catch(() => null),
  ])
  return { reserva, folio, alojamiento, politica, titular, emitido: new Date() }
}

/**
 * Estado de cuenta de una reserva (DEC-133), para imprimir o guardar en PDF desde el navegador. Lleva la marca y los
 * datos del alojamiento, el huésped y la estancia, cada cargo (con el IVA aparte) y cada pago, el total, el saldo, la
 * política congelada en la reserva y las normas de la casa. No es una factura electrónica (BL-02, MVP2).
 */
export default function EstadoCuenta() {
  const { codigo = '' } = useParams()
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const clave = `${codigo}#${intento}`
  useTitulo(`Estado de cuenta ${codigo}`)

  useEffect(() => {
    let vigente = true
    leer(codigo)
      .then((datos) => {
        if (vigente) setResultado({ clave, datos })
      })
      .catch((error: unknown) => {
        if (vigente) setResultado({ clave, error })
      })
    return () => {
      vigente = false
    }
  }, [codigo, clave])

  const actual = resultado?.clave === clave ? resultado : null
  const alFolio = `/reservas/${encodeURIComponent(codigo)}/folio`
  const volver = <Link to={alFolio} className="volver"><span aria-hidden="true">←</span> Volver al folio</Link>

  if (!actual) {
    return (
      <div className="detalle" aria-busy="true">
        <span className="fantasma fantasma-titulo" />
        <div className="tarjeta fantasma-datos">
          {Array.from({ length: 5 }, (_, fila) => <span key={fila} className="fantasma" />)}
        </div>
        <p className="suave" role="status">Preparando el estado de cuenta…</p>
      </div>
    )
  }

  if ('error' in actual) {
    if (actual.error instanceof ErrorApi && actual.error.estado === 404) {
      return (
        <BloqueVacio
          encabezado="h1"
          icono="reservaNoEncontrada"
          titulo="Reserva no encontrada"
          accion={<Link to="/reservas" className="boton"><span aria-hidden="true">←</span> Volver a reservas</Link>}
        >
          No existe una reserva con el código <span className="codigo codigo-resaltado">{codigo}</span>.
        </BloqueVacio>
      )
    }
    return (
      <div className="detalle">
        <div>{volver}</div>
        <Aviso
          tipo="error"
          asunto="No pudimos preparar el estado de cuenta"
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

  return (
    <div className="estado-cuenta-pagina">
      <div className="estado-cuenta-barra no-imprimir">
        {volver}
        <div className="estado-cuenta-imprimir">
          <button type="button" className="boton" onClick={() => window.print()}>
            Imprimir o guardar en PDF
          </button>
          <p className="campo-pista">Para el PDF, elige «Guardar como PDF» en el destino de la impresión.</p>
        </div>
      </div>
      <Documento datos={actual.datos} />
    </div>
  )
}

/** La hoja: lo único que sale al imprimir. */
function Documento({ datos }: { datos: Datos }) {
  const { reserva, folio, alojamiento, politica, titular, emitido } = datos
  const marca = alojamiento.marca
  const cargos = lineasDeCargos(folio)
  const pagos = lineasDePagos(folio)
  const suma = totales(folio)
  const ivaReserva = lineaIva(reserva.valor)
  const lugar = lugarDelAlojamiento(alojamiento.direccion, alojamiento.ciudad)
  const tramos = tramosOrdenados(politica)
  const nochesEstancia = noches(reserva.entrada, reserva.salida)
  // La marca del alojamiento (BL-05): su acento en la franja y su letra en los títulos, si la tiene
  const estilo = {
    '--hoja-acento': marca?.colores.acento,
    '--hoja-letra-titulo': marca?.fuentes.titulos ? `'${marca.fuentes.titulos}', var(--letra-titulo)` : undefined,
  } as CSSProperties

  return (
    <article className="hoja" style={estilo} aria-labelledby="hoja-titulo">
      <header className="hoja-cabecera">
        <div className="hoja-alojamiento">
          {/* Decorativo: el nombre va justo debajo. Si la dirección falla, no queda el ícono roto en la hoja */}
          {marca?.logoUrl && (
            <img className="hoja-logo" src={marca.logoUrl} alt="" onError={(evento) => { evento.currentTarget.hidden = true }} />
          )}
          <p className="hoja-nombre">{alojamiento.nombre}</p>
          {lugar && <p>{lugar}</p>}
          {alojamiento.rnt && <p>RNT {alojamiento.rnt}</p>}
        </div>
        <div className="hoja-titulo">
          <h1 id="hoja-titulo">Estado de cuenta</h1>
          <p>Reserva <span className="hoja-codigo">{reserva.codigo}</span></p>
          <p>Emitido el {fechaCorta(fechaHoy(emitido))}, {hora(`${emitido.getHours()}:${emitido.getMinutes()}`)}</p>
        </div>
      </header>

      <p className="hoja-no-factura">Este documento no es una factura electrónica.</p>

      <div className="hoja-partes">
        <section aria-labelledby="hoja-huesped">
          <h2 id="hoja-huesped">Huésped</h2>
          <dl className="hoja-datos">
            <Dato nombre="Titular">{titular?.nombre ?? reserva.titularNombre ?? reserva.titularId}</Dato>
            <Dato nombre="Documento">{documentoLegible(titular?.tipoDocumento, titular?.numeroDocumento)}</Dato>
            <Dato nombre="Correo">{titular?.correo}</Dato>
            <Dato nombre="Teléfono">{titular?.telefono}</Dato>
          </dl>
        </section>
        <section aria-labelledby="hoja-estancia">
          <h2 id="hoja-estancia">Estancia</h2>
          <dl className="hoja-datos">
            <Dato nombre="Apartamento">{reserva.apartamento}</Dato>
            <Dato nombre="Entrada">
              {fechaCorta(reserva.entrada)}, desde las {hora(alojamiento.parametros.horaEntrada)}
            </Dato>
            <Dato nombre="Salida">
              {fechaCorta(reserva.salida)}, hasta las {hora(alojamiento.parametros.horaSalida)}
            </Dato>
            <Dato nombre="Noches">{nochesEstancia}</Dato>
            <Dato nombre="Huéspedes">{reserva.ocupantes.length}</Dato>
            <Dato nombre="Estado">{NOMBRE_DEL_ESTADO[reserva.estado]}{folio.cerrado ? ' · cuenta cerrada' : ''}</Dato>
          </dl>
        </section>
      </div>

      <section aria-labelledby="hoja-cargos">
        <h2 id="hoja-cargos">Cargos</h2>
        {cargos.length === 0 ? (
          <p className="hoja-vacio">Sin cargos.</p>
        ) : (
          <table className="hoja-tabla" aria-labelledby="hoja-cargos">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Concepto</th>
                <th scope="col" className="hoja-valor">Valor</th>
              </tr>
            </thead>
            <tbody>
              {cargos.map((linea) => (
                <tr key={linea.id} className={linea.correccion ? 'hoja-correccion' : undefined}>
                  <td>{fechaCorta(linea.fecha)}</td>
                  <td>{linea.concepto}</td>
                  <td className="hoja-valor">{conSigno(linea.valor)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              {suma.conIva && (
                <>
                  <Total nombre="Subtotal sin IVA" valor={suma.subtotal} />
                  <Total nombre="IVA" valor={suma.iva} />
                </>
              )}
              {/* DEC-134: la reserva exenta lo dice, para que la hoja no parezca olvidar el IVA */}
              {!suma.conIva && ivaReserva?.valor === 0 && <Total nombre={ivaReserva.texto} valor={0} />}
              <Total nombre="Total" valor={suma.total} fuerte />
            </tfoot>
          </table>
        )}
      </section>

      <section aria-labelledby="hoja-pagos">
        <h2 id="hoja-pagos">Pagos</h2>
        {pagos.length === 0 ? (
          <p className="hoja-vacio">Sin pagos registrados.</p>
        ) : (
          <table className="hoja-tabla" aria-labelledby="hoja-pagos">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Movimiento</th>
                <th scope="col" className="hoja-valor">Valor</th>
              </tr>
            </thead>
            <tbody>
              {pagos.map((linea) => (
                <tr key={linea.id}>
                  <td>{fechaCorta(linea.fecha)}</td>
                  <td>{linea.concepto}</td>
                  <td className="hoja-valor">{conSigno(linea.valor)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <Total nombre="Total pagado" valor={folio.totalPagado} fuerte />
            </tfoot>
          </table>
        )}
      </section>

      <section aria-labelledby="hoja-resumen" className="hoja-resumen">
        <h2 id="hoja-resumen" className="solo-lectores">Resumen</h2>
        <dl>
          <div><dt>Total</dt><dd>{dinero(suma.total)}</dd></div>
          <div><dt>Total pagado</dt><dd>{dinero(folio.totalPagado)}</dd></div>
          <div className="hoja-saldo"><dt>{TEXTO_SALDO[folio.situacionSaldo]}</dt><dd>{dinero(folio.saldo)}</dd></div>
        </dl>
      </section>

      <section aria-labelledby="hoja-politica">
        <h2 id="hoja-politica">Política de cancelación</h2>
        <p className="hoja-nota">La que regía al hacer la reserva (versión {politica.version}).</p>
        <ul className="hoja-lista">
          {tramos.map((tramo, indice) => (
            <li key={`${tramo.antelacionMinHoras}-${indice}`}>
              {textoTramo(tramos, indice)}: {minuscula(textoRetencion(tramo.penalizacion))}.
            </li>
          ))}
          <li>Si el huésped no se presenta: {minuscula(textoRetencion(politica.penalizacionNoShow))}.</li>
        </ul>
      </section>

      {alojamiento.normas?.trim() && (
        <section aria-labelledby="hoja-normas">
          <h2 id="hoja-normas">Normas de la casa</h2>
          <p className="hoja-normas">{alojamiento.normas}</p>
        </section>
      )}

      <footer className="hoja-pie">
        <p>
          Este documento no es una factura electrónica: es un resumen de la cuenta de la reserva. La factura válida en
          Colombia es la electrónica validada por la DIAN.
        </p>
      </footer>
    </article>
  )
}

/** Un dato del huésped o de la estancia; si no hay valor, no se muestra la fila. */
function Dato({ nombre, children }: { nombre: string; children: ReactNode }) {
  if (children === null || children === undefined || children === '') return null
  return (
    <div>
      <dt>{nombre}</dt>
      <dd>{children}</dd>
    </div>
  )
}

function Total({ nombre, valor, fuerte = false }: { nombre: string; valor: number; fuerte?: boolean }) {
  return (
    <tr className={fuerte ? 'hoja-total' : undefined}>
      <th scope="row" colSpan={2}>{nombre}</th>
      <td className="hoja-valor">{conSigno(valor)}</td>
    </tr>
  )
}

/** «− $ 45.000»: el signo menos aparte, para que no se pierda pegado al símbolo. */
function conSigno(valor: number): string {
  return valor < 0 ? `− ${dinero(-valor)}` : dinero(valor)
}

/** «Se retiene…» queda bien después de dos puntos: «Menos de 48 horas antes de la entrada: se retiene el 50 %». */
function minuscula(texto: string): string {
  return texto.charAt(0).toLowerCase() + texto.slice(1)
}
