import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { consultarAlojamiento } from '../api/alojamiento'
import { cambiarImagen, consultarFicha, type Bloqueo, type FichaApartamento, type Imagen } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import DialogoDatos from '../apartamentos/DialogoDatos'
import {
  DialogoBloqueo,
  DialogoCapacidad,
  DialogoFoto,
  DialogoLevantar,
  DialogoMascotas,
  DialogoVenta,
  type Hecho,
} from '../apartamentos/Dialogos'
import { MAXIMO_FOTOS, personas, separarBloqueos, textoMascotas } from '../apartamentos/formulario'
import { EtiquetaVenta, Foto } from '../apartamentos/Piezas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import EstadoApartamento from '../componentes/EstadoApartamento'
import { useTitulo } from '../componentes/useTitulo'
import { fechaCorta, fechaHoy } from '../formato'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'

type Resultado = { clave: string; apartamento: FichaApartamento } | { clave: string; error: unknown }
type Abierto = 'datos' | 'capacidad' | 'mascotas' | 'foto' | 'bloqueo' | 'venta' | { levantar: Bloqueo } | null

/** Lo que dice la API del hotel sobre mascotas; si no se pudo leer, se ofrece cambiar el límite igual. */
interface MascotasDelHotel {
  acepta: boolean
  limite: number | null
}

/**
 * El detalle de un apartamento para el administrador (sprint 4): sus datos, capacidad, mascotas, fotos, bloqueos y
 * venta, cada sección con su acción. El estado operativo solo se muestra: se cambia en Preparación.
 */
export default function FichaApartamento() {
  const { codigo = '' } = useParams()
  const { state } = useLocation() as { state: { volverA?: string; creado?: boolean } | null }
  const listado = `/apartamentos${state?.volverA ?? ''}`
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [abierto, setAbierto] = useState<Abierto>(null)
  const [hecho, setHecho] = useState<Omit<Hecho, 'apartamento'> | null>(null)
  const [mascotas, setMascotas] = useState<MascotasDelHotel>({ acepta: true, limite: null })
  const titulo = useRef<HTMLHeadingElement>(null)
  const clave = `${codigo}#${intento}`

  useEffect(() => {
    let vigente = true
    consultarFicha(codigo)
      .then((apartamento) => vigente && setResultado({ clave, apartamento }))
      .catch((error: unknown) => vigente && setResultado({ clave, error }))
    return () => {
      vigente = false
    }
  }, [codigo, clave])

  useEffect(() => {
    let vigente = true
    consultarAlojamiento()
      .then(({ parametros }) => vigente && setMascotas({ acepta: parametros.aceptaMascotas, limite: parametros.limiteMascotas }))
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [])

  const actual = resultado?.clave === clave ? resultado : null
  useTitulo(actual && 'apartamento' in actual ? `${actual.apartamento.codigo} · Apartamentos` : 'Apartamento')

  if (!actual) return <Cargando listado={listado} />
  if ('error' in actual) {
    if (actual.error instanceof ErrorApi && actual.error.estado === 404) {
      return (
        <BloqueVacio
          encabezado="h1"
          icono="noEncontrada"
          titulo="Apartamento no encontrado"
          accion={<Link to={listado} className="boton"><span aria-hidden="true">←</span> Volver a apartamentos</Link>}
        >
          No existe un apartamento con el código <span className="codigo codigo-resaltado">{codigo}</span>.
        </BloqueVacio>
      )
    }
    return (
      <div className="detalle">
        <Volver listado={listado} />
        <Aviso
          tipo="error"
          asunto="No pudimos cargar el apartamento"
          mensaje={errorParaMostrar(actual.error).mensaje}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      </div>
    )
  }

  const { apartamento } = actual
  const cerrar = () => setAbierto(null)
  // Si el botón que se usó ya no existe (un bloqueo levantado, una foto quitada), el foco vuelve al título y no al
  // comienzo de la página, para que el teclado y el lector de pantalla sigan aquí
  const recuperarFoco = () => requestAnimationFrame(() => {
    if (!document.activeElement || document.activeElement === document.body) titulo.current?.focus()
  })
  const cambiado = (nuevo: FichaApartamento) => {
    setResultado({ clave, apartamento: nuevo })
    recuperarFoco()
  }
  const alHacer = ({ apartamento: nuevo, ...aviso }: Hecho) => {
    cambiado(nuevo)
    setHecho(aviso)
    setAbierto(null)
  }
  const comunes = { apartamento, alCerrar: cerrar, alHacer }
  const { vigentes, anteriores } = separarBloqueos(apartamento.bloqueos, fechaHoy())

  return (
    <div className="detalle ficha-apartamento">
      <div className="barra-volver">
        <Volver listado={listado} />
      </div>

      <header className="ficha-cabecera">
        <p className="ficha-rotulo">Apartamento</p>
        <h1 ref={titulo} tabIndex={-1}>
          <span className="codigo">{apartamento.codigo}</span> · {apartamento.nombre}
        </h1>
        <div className="ficha-estados">
          <EtiquetaVenta activo={apartamento.activo} />
          <EstadoApartamento estado={apartamento.estadoOperativo} etiqueta />
        </div>
        <p className="campo-pista">El estado operativo se cambia en Preparación de apartamentos.</p>
      </header>

      {state?.creado && !hecho && (
        <Aviso tipo="exito" asunto="Apartamento creado" mensaje={`${apartamento.codigo} quedó creado y retirado de la venta. Para ponerlo a la venta necesita tarifa en todas las temporadas.`} />
      )}
      {hecho && <Aviso tipo="exito" asunto={hecho.asunto} mensaje={hecho.mensaje} />}
      {!apartamento.activo && (
        <Aviso tipo="advertencia" asunto="Retirado de la venta" mensaje="No aparece en la web ni se puede reservar." />
      )}

      <Seccion id="datos" titulo="Datos" accion={<Accion texto="Editar" al={() => setAbierto('datos')} />}>
        <dl className="tarjeta ficha-datos">
          <div>
            <dt>Nombre</dt>
            <dd className="texto-largo">{apartamento.nombre}</dd>
          </div>
          <div>
            <dt>Dormitorios</dt>
            <dd>{apartamento.dormitorios}</dd>
          </div>
          <div className="ficha-datos-ancho">
            <dt>Descripción</dt>
            <dd className="texto-largo ficha-descripcion">
              {apartamento.descripcion?.trim() || <span className="suave">Sin descripción</span>}
            </dd>
          </div>
          <div className="ficha-datos-ancho">
            <dt>Características</dt>
            <dd>
              <ul className="fichas">
                {apartamento.caracteristicas.map((texto) => <li key={texto} className="etiqueta ficha">{texto}</li>)}
              </ul>
            </dd>
          </div>
        </dl>
      </Seccion>

      <div className="ficha-pareja">
        <Seccion id="capacidad" titulo="Capacidad"
          accion={<Accion texto="Cambiar capacidad" al={() => setAbierto('capacidad')} />}>
          <p className="tarjeta ficha-valor">{personas(apartamento)}</p>
        </Seccion>
        <Seccion id="mascotas" titulo="Mascotas"
          accion={mascotas.acepta ? <Accion texto="Cambiar" etiqueta="Cambiar mascotas" al={() => setAbierto('mascotas')} /> : null}>
          {mascotas.acepta
            ? <p className="tarjeta ficha-valor">{textoMascotas(apartamento, mascotas.limite)}</p>
            : <p className="tarjeta ficha-valor ficha-valor-suave">El hotel no acepta mascotas (se cambia en Alojamiento).</p>}
        </Seccion>
      </div>

      <Fotos apartamento={apartamento} alCambiar={cambiado} alAgregar={() => setAbierto('foto')} />

      <Seccion id="bloqueos" titulo="Bloqueos" accion={<Accion texto="Nuevo bloqueo" al={() => setAbierto('bloqueo')} />}>
        <div className="tarjeta bloqueos">
          {vigentes.length === 0 && <p className="bloqueos-vacio suave">No hay bloqueos vigentes: todas las noches están a la venta.</p>}
          {vigentes.length > 0 && (
            <ul className="bloqueos-lista">
              {vigentes.map((bloqueo) => (
                <li key={bloqueo.id} className="bloqueo">
                  <FechasBloqueo bloqueo={bloqueo} />
                  <button type="button" className="boton boton-secundario boton-chico"
                    aria-label={`Levantar el bloqueo del ${fechaCorta(bloqueo.fechaInicio)}`}
                    onClick={() => setAbierto({ levantar: bloqueo })}>
                    Levantar
                  </button>
                </li>
              ))}
            </ul>
          )}
          {anteriores.length > 0 && (
            <details className="bloqueos-anteriores">
              <summary>Ver anteriores ({anteriores.length})</summary>
              <ul className="bloqueos-lista">
                {anteriores.map((bloqueo) => (
                  <li key={bloqueo.id} className="bloqueo bloqueo-anterior">
                    <FechasBloqueo bloqueo={bloqueo} />
                    <span className="suave">{bloqueo.vigente ? 'Terminado' : 'Levantado'}</span>
                  </li>
                ))}
              </ul>
            </details>
          )}
        </div>
      </Seccion>

      <Seccion id="venta" titulo="Venta">
        <div className="tarjeta ficha-venta">
          <div>
            <EtiquetaVenta activo={apartamento.activo} />
            <p className="suave">
              {apartamento.activo ? 'Aparece en la web y se puede reservar.' : 'No aparece en la web ni se puede reservar.'}
            </p>
          </div>
          <button type="button" className="boton boton-secundario" onClick={() => setAbierto('venta')}>
            {apartamento.activo ? 'Retirar de la venta' : 'Poner a la venta'}
          </button>
        </div>
      </Seccion>

      {abierto === 'datos' && (
        <DialogoDatos apartamento={apartamento} alCerrar={cerrar}
          alGuardar={(nuevo) => alHacer({ apartamento: nuevo, asunto: 'Datos guardados', mensaje: 'Nombre, descripción, dormitorios y características actualizados.' })} />
      )}
      {abierto === 'capacidad' && <DialogoCapacidad {...comunes} />}
      {abierto === 'mascotas' && <DialogoMascotas {...comunes} limiteDelHotel={mascotas.limite} />}
      {abierto === 'foto' && <DialogoFoto {...comunes} />}
      {abierto === 'bloqueo' && <DialogoBloqueo {...comunes} />}
      {abierto === 'venta' && <DialogoVenta {...comunes} />}
      {abierto && typeof abierto === 'object' && <DialogoLevantar {...comunes} bloqueo={abierto.levantar} />}
    </div>
  )
}

function Volver({ listado }: { listado: string }) {
  return (
    <Link to={listado} className="volver">
      <span aria-hidden="true">←</span> Apartamentos
    </Link>
  )
}

function Seccion({ id, titulo, accion, extra, children }: {
  id: string
  titulo: string
  accion?: ReactNode
  extra?: ReactNode
  children: ReactNode
}) {
  return (
    <section aria-labelledby={`titulo-${id}`} className="detalle-seccion">
      <div className="ficha-seccion-cabecera">
        <h2 id={`titulo-${id}`}>{titulo}{extra}</h2>
        {accion}
      </div>
      {children}
    </section>
  )
}

function Accion({ texto, etiqueta, al }: { texto: string; etiqueta?: string; al: () => void }) {
  return (
    <button type="button" className="boton boton-secundario boton-chico" aria-label={etiqueta} onClick={al}>
      {texto}
    </button>
  )
}

function FechasBloqueo({ bloqueo }: { bloqueo: Bloqueo }) {
  return (
    <div className="bloqueo-datos">
      <strong>
        {fechaCorta(bloqueo.fechaInicio)} <span aria-hidden="true">→</span>
        <span className="solo-lectores"> hasta el</span> {fechaCorta(bloqueo.fechaFin)}
      </strong>
      <span className="suave texto-largo">{bloqueo.motivo}</span>
    </div>
  )
}

/**
 * Las fotos, con la principal marcada. «Hacer principal» y «Quitar» van directo a la API, sin diálogo. La única foto no
 * se quita: acordado el 13 nov, todo apartamento tiene al menos una.
 */
function Fotos({ apartamento, alCambiar, alAgregar }: {
  apartamento: FichaApartamento
  alCambiar: (apartamento: FichaApartamento) => void
  alAgregar: () => void
}) {
  const [ocupada, setOcupada] = useState<string | null>(null)
  const [rechazo, setRechazo] = useState<(ErrorMostrado & { asunto: string }) | null>(null)
  const unica = apartamento.imagenes.length === 1

  async function cambiar(imagen: Imagen, accion: 'quitar' | 'principal') {
    setOcupada(imagen.url)
    setRechazo(null)
    try {
      alCambiar(await cambiarImagen(apartamento.codigo, accion, imagen.url))
    } catch (causa) {
      setRechazo({ asunto: accion === 'quitar' ? 'No se quitó la foto' : 'No se cambió la principal', ...errorParaMostrar(causa) })
    } finally {
      setOcupada(null)
    }
  }

  return (
    <Seccion
      id="fotos"
      titulo="Fotos"
      extra={<span className="ficha-seccion-cuenta"> {apartamento.imagenes.length} de {MAXIMO_FOTOS}</span>}
      accion={apartamento.imagenes.length < MAXIMO_FOTOS ? <Accion texto="Agregar foto" al={alAgregar} /> : null}
    >
      {rechazo && <Aviso tipo="error" asunto={rechazo.asunto} mensaje={rechazo.mensaje} detalles={rechazo.detalles} />}
      <div className="tarjeta fotos">
        {apartamento.imagenes.length === 0 ? (
          <p className="suave">Este apartamento no tiene fotos. Agrega al menos una para ponerlo a la venta.</p>
        ) : (
          <ul className="fotos-cuadricula">
            {apartamento.imagenes.map((imagen, indice) => (
              <li key={imagen.url} className={imagen.principal ? 'foto foto-principal' : 'foto'} aria-busy={ocupada === imagen.url}>
                <div className="foto-marco">
                  <Foto url={imagen.url} alt={`Foto ${indice + 1}${imagen.principal ? ', principal' : ''}`} clase="foto-imagen" />
                  {imagen.principal && <span className="etiqueta foto-sello"><span aria-hidden="true">★</span> Principal</span>}
                </div>
                <div className="foto-acciones">
                  {!imagen.principal && (
                    <button type="button" className="boton boton-secundario boton-chico" disabled={ocupada !== null}
                      aria-label={`Hacer principal la foto ${indice + 1}`} onClick={() => void cambiar(imagen, 'principal')}>
                      Hacer principal
                    </button>
                  )}
                  {!unica && (
                    <button type="button" className="boton boton-secundario boton-chico" disabled={ocupada !== null}
                      aria-label={`Quitar la foto ${indice + 1}`} onClick={() => void cambiar(imagen, 'quitar')}>
                      Quitar
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
        {unica && <p className="campo-pista">Es la única foto: agrega otra antes de quitar esta.</p>}
      </div>
    </Seccion>
  )
}

function Cargando({ listado }: { listado: string }) {
  return (
    <div className="detalle" aria-busy="true">
      <Volver listado={listado} />
      <span className="fantasma fantasma-titulo" />
      <div className="tarjeta fantasma-datos">
        {Array.from({ length: 4 }, (_, celda) => <span key={celda} className="fantasma" />)}
      </div>
      <p className="suave" role="status">Cargando el apartamento…</p>
    </div>
  )
}
