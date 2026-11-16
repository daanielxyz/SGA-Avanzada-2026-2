import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { listarReservas, type Pagina, type Reserva } from '../api/reservas'
import { consultarTitular, type Titular as DatosDelTitular } from '../api/titulares'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import EtiquetaEstado from '../componentes/EtiquetaEstado'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { dinero, fechaCorta } from '../formato'
import { errorParaMostrar } from '../reserva/mensajes'
import { totalConIva } from '../reserva/valor'
import { DialogoEditarTitular, DialogoValidarIdentidad } from '../titulares/Dialogos'
import EstadoIdentidad from '../titulares/EstadoIdentidad'
import { Correo } from '../usuarios/EstadoCuenta'
import { PISTA_DOCUMENTO, textoDocumento } from '../titulares/logica'

type Leido<T> = { clave: string; datos: T } | { clave: string; error: unknown }

/**
 * Un titular (DEC-131): sus datos, su identidad y sus reservas. Los dos bloques cargan por separado: si falla uno, el
 * otro se ve. Recepción edita su nombre y su contacto, y valida su identidad; el documento se corrige desde una reserva.
 */
export default function Titular() {
  const { id = '' } = useParams()
  const volverA = (useLocation().state as { volverA?: string } | null)?.volverA ?? ''
  const [intento, setIntento] = useState(0)
  const [titular, setTitular] = useState<Leido<DatosDelTitular> | null>(null)
  const [abierto, setAbierto] = useState<'editar' | 'validar' | null>(null)
  const [aviso, setAviso] = useState<{ tipo: 'exito' | 'info'; asunto: string; mensaje: string } | null>(null)
  const encabezado = useRef<HTMLHeadingElement>(null)
  const clave = `${id}#${intento}`

  useEffect(() => {
    let vigente = true
    consultarTitular(id)
      .then((datos) => vigente && setTitular({ clave, datos }))
      .catch((error: unknown) => vigente && setTitular({ clave, error }))
    return () => {
      vigente = false
    }
  }, [id, clave])

  const actual = titular?.clave === clave ? titular : null
  const datos = actual && 'datos' in actual ? actual.datos : null
  useTitulo(datos?.nombre ?? 'Titular')

  const volver = (
    <Link to={`/titulares${volverA}`} className="volver"><span aria-hidden="true">←</span> Titulares</Link>
  )
  const terminar = (nuevo: DatosDelTitular, avisoNuevo: typeof aviso) => {
    setTitular({ clave, datos: nuevo })
    setAviso(avisoNuevo)
    setAbierto(null)
    requestAnimationFrame(() => encabezado.current?.focus())
  }

  if (actual && 'error' in actual && actual.error instanceof ErrorApi && actual.error.estado === 404) {
    return (
      <BloqueVacio encabezado="h1" icono="titulares" titulo="Este titular no existe"
        accion={<Link to={`/titulares${volverA}`} className="boton"><span aria-hidden="true">←</span> Titulares</Link>}>
        Puede que el enlace esté mal escrito o que el código no sea de este alojamiento.
      </BloqueVacio>
    )
  }

  return (
    <div className="titular">
      <div className="barra-volver">{volver}</div>
      {!actual && <p className="suave" role="status" aria-busy="true">Cargando el titular…</p>}

      {datos && (
        <div className="titular-encabezado">
          <h1 ref={encabezado} tabIndex={-1} className="texto-largo">{datos.nombre}</h1>
          <EstadoIdentidad validado={datos.validado} titular={datos} />
        </div>
      )}
      {aviso && <Aviso tipo={aviso.tipo} asunto={aviso.asunto} mensaje={aviso.mensaje} />}

      <section aria-labelledby="titulo-datos-titular" className="detalle-seccion">
        <div className="ficha-seccion-cabecera">
          <h2 id="titulo-datos-titular">Datos</h2>
          {datos && (
            <div className="titular-acciones">
              {!datos.validado && (
                <button type="button" className="boton boton-secundario" onClick={() => setAbierto('validar')}>
                  Validar identidad
                </button>
              )}
              <button type="button" className="boton boton-secundario" onClick={() => setAbierto('editar')}>
                Editar datos
              </button>
            </div>
          )}
        </div>
        {actual && 'error' in actual && (
          <Aviso tipo="error" asunto="No se pudieron cargar los datos del titular" mensaje={errorParaMostrar(actual.error).mensaje}
            accion={<button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>Reintentar</button>} />
        )}
        {datos && <DatosTitular titular={datos} />}
      </section>

      <ReservasDelTitular titularId={id} />

      {datos && abierto === 'editar' && (
        <DialogoEditarTitular titular={datos} alCerrar={() => setAbierto(null)}
          alGuardar={(nuevo) => terminar(nuevo, { tipo: 'exito', asunto: 'Datos guardados', mensaje: `Los datos de ${nuevo.nombre} quedaron guardados.` })} />
      )}
      {datos && abierto === 'validar' && (
        <DialogoValidarIdentidad titular={datos} alCerrar={() => setAbierto(null)}
          alValidar={(nuevo) => terminar(nuevo, { tipo: 'exito', asunto: 'Identidad validada', mensaje: 'Desde ahora su nombre solo lo cambia el hotel.' })}
          alYaValidada={() => {
            setAbierto(null)
            setAviso({ tipo: 'info', asunto: 'La identidad ya estaba validada', mensaje: 'Otra persona la validó antes. Los datos se recargaron.' })
            setIntento(intento + 1)
          }} />
      )}
    </div>
  )
}

function DatosTitular({ titular }: { titular: DatosDelTitular }) {
  // Un titular de antes de DEC-98 trae el nombre entero: se muestra junto, sin inventar apellidos
  const separado = Boolean(titular.nombres)
  const dato = (nombre: string, valor: ReactNode, faltante: string, pista?: string) => (
    <div className="dato">
      <dt>{nombre}</dt>
      <dd className="texto-largo">
        {valor ? valor : <><span aria-hidden="true">—</span><span className="solo-lectores">{faltante}</span></>}
        {pista && <span className="dato-nota">{pista}</span>}
      </dd>
    </div>
  )
  return (
    <dl className="tarjeta datos">
      {separado ? (
        <>
          {dato('Nombres', titular.nombres, 'Sin nombres')}
          {dato('Primer apellido', titular.primerApellido, 'Sin primer apellido')}
          {dato('Segundo apellido', titular.segundoApellido, 'Sin segundo apellido')}
        </>
      ) : dato('Nombre', titular.nombre, 'Sin nombre')}
      {dato('Documento', textoDocumento(titular.tipoDocumento, titular.numeroDocumento), '', PISTA_DOCUMENTO)}
      {dato('Correo', titular.correo && <Correo correo={titular.correo} />, 'Sin correo')}
      {dato('Teléfono', titular.telefono, 'Sin teléfono')}
    </dl>
  )
}

/** Las reservas del titular, de la más reciente a la más antigua, de 10 en 10 (`GET /api/reservas?titular=`). */
function ReservasDelTitular({ titularId }: { titularId: string }) {
  const [numero, setNumero] = useState(0)
  const [intento, setIntento] = useState(0)
  const [leidas, setLeidas] = useState<Leido<Pagina<Reserva>> | null>(null)
  const clave = `${titularId}#${numero}#${intento}`

  useEffect(() => {
    let vigente = true
    const consulta = new URLSearchParams({ titular: titularId, pagina: String(numero) })
    listarReservas(consulta.toString())
      .then((datos) => vigente && setLeidas({ clave, datos }))
      .catch((error: unknown) => vigente && setLeidas({ clave, error }))
    return () => {
      vigente = false
    }
  }, [titularId, numero, clave])

  const actual = leidas?.clave === clave ? leidas : null
  const pagina = actual && 'datos' in actual ? actual.datos : null
  return (
    <section aria-labelledby="titulo-reservas-titular" className="detalle-seccion">
      <h2 id="titulo-reservas-titular">
        Reservas{pagina && <span className="suave"> ({pagina.totalElementos})</span>}
      </h2>
      {!actual && <p className="suave" role="status">Cargando sus reservas…</p>}
      {actual && 'error' in actual && (
        <Aviso tipo="error" asunto="No se pudieron cargar sus reservas" mensaje={errorParaMostrar(actual.error).mensaje}
          accion={<button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>Reintentar</button>} />
      )}
      {pagina && pagina.contenido.length === 0 && (
        <p className="tarjeta bloque-sin-datos suave">Este titular todavía no tiene reservas.</p>
      )}
      {pagina && pagina.contenido.length > 0 && (
        <>
          <div className="tarjeta desplazable tabla-reservas-titular-marco">
            <table className="tabla tabla-reservas-titular">
              <caption className="solo-lectores">Reservas del titular</caption>
              <thead>
                <tr>
                  <th scope="col">Código</th>
                  <th scope="col">Apto.</th>
                  <th scope="col">Entrada y salida</th>
                  <th scope="col">Estado</th>
                  <th scope="col" className="derecha">Total</th>
                </tr>
              </thead>
              <tbody>
                {pagina.contenido.map((reserva) => (
                  <tr key={reserva.codigo}>
                    <td><Link to={`/reservas/${reserva.codigo}`} className="codigo enlace-codigo">{reserva.codigo}</Link></td>
                    <td className="codigo">{reserva.apartamento}</td>
                    <td>
                      <span className="estancia">{fechaCorta(reserva.entrada)}</span>
                      <span className="estancia suave">{fechaCorta(reserva.salida)}</span>
                    </td>
                    <td><EtiquetaEstado estado={reserva.estado} /></td>
                    <td className="derecha codigo importe">{dinero(totalConIva(reserva.valor))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <ul className="reservas-titular-tarjetas" aria-label="Reservas del titular">
            {pagina.contenido.map((reserva) => (
              <li key={reserva.codigo}>
                <Link to={`/reservas/${reserva.codigo}`} className="reserva-tarjeta">
                  <span className="reserva-tarjeta-fila">
                    <span className="codigo reserva-tarjeta-codigo">{reserva.codigo}</span>
                    <EtiquetaEstado estado={reserva.estado} />
                  </span>
                  <span className="reserva-tarjeta-linea">
                    <span className="codigo">{reserva.apartamento}</span> · {fechaCorta(reserva.entrada)} → {fechaCorta(reserva.salida)}
                  </span>
                  <span className="codigo importe">{dinero(totalConIva(reserva.valor))}</span>
                </Link>
              </li>
            ))}
          </ul>
          <Paginador numero={pagina.numero} totalPaginas={pagina.totalPaginas} totalElementos={pagina.totalElementos}
            alCambiar={setNumero} />
        </>
      )}
    </section>
  )
}
