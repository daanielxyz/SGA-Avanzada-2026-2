import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { listarCanales, listarConflictosPendientes, type Canal, type Conflicto } from '../api/canales'
import type { Pagina } from '../api/reservas'
import { DialogoDesactivarCanal, DialogoRegistrarCanal, DialogoResolverConflicto } from '../canales/Dialogos'
import { detectado, estancia, NOMBRE_DEL_TIPO, sePuedeDesactivar, tieneBitacora } from '../canales/logica'
import { ConCodigos, ContactoConflicto, EstadoCanal } from '../canales/Piezas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { errorParaMostrar } from '../reserva/mensajes'

type Resultado<T> = { clave: string; pagina: Pagina<T> } | { clave: string; error: unknown }

type Abierto = { registrar: true } | { desactivar: Canal } | { resolver: Conflicto }

/** Carga una página y la vuelve a pedir cuando cambia la clave (número de página o un reintento). */
function usePagina<T>(cargar: (numero: number) => Promise<Pagina<T>>, numero: number, intento: number) {
  const [resultado, setResultado] = useState<Resultado<T> | null>(null)
  const clave = `${numero}#${intento}`
  useEffect(() => {
    let vigente = true
    cargar(numero)
      .then((pagina) => vigente && setResultado({ clave, pagina }))
      .catch((error: unknown) => vigente && setResultado({ clave, error }))
    return () => {
      vigente = false
    }
  }, [cargar, numero, clave])
  return resultado?.clave === clave ? resultado : null
}

/**
 * «Canales de venta» (sprint 5, solo administración): primero los conflictos pendientes, que son lo urgente (un canal
 * externo vendió una noche que no cabía y el huésped cree que tiene reserva), y debajo los canales. Cada bloque carga y
 * falla por separado.
 */
export default function Canales() {
  const [parametros, setParametros] = useSearchParams()
  const numeroCanales = Math.max(0, Number(parametros.get('pagina')) || 0)
  const numeroConflictos = Math.max(0, Number(parametros.get('conflictos')) || 0)
  const [intentoCanales, setIntentoCanales] = useState(0)
  const [intentoConflictos, setIntentoConflictos] = useState(0)
  const canales = usePagina(listarCanales, numeroCanales, intentoCanales)
  const conflictos = usePagina(listarConflictosPendientes, numeroConflictos, intentoConflictos)
  const [abierto, setAbierto] = useState<Abierto | null>(null)
  const [hecho, setHecho] = useState<{ tipo: 'exito' | 'advertencia'; asunto: string; mensaje: string } | null>(null)
  const titulo = useRef<HTMLHeadingElement>(null)
  useTitulo('Canales de venta')

  const paginaCanales = canales && 'pagina' in canales ? canales.pagina : null
  const paginaConflictos = conflictos && 'pagina' in conflictos ? conflictos.pagina : null
  // El conflicto solo trae el id del canal: el nombre sale de los canales ya cargados
  const nombreCanal = (id: string) => paginaCanales?.contenido.find((canal) => canal.id === id)?.nombre ?? id

  const cambiarPagina = (clave: 'pagina' | 'conflictos', numero: number) => {
    const siguientes = new URLSearchParams(parametros)
    if (numero > 0) siguientes.set(clave, String(numero))
    else siguientes.delete(clave)
    setParametros(siguientes)
  }

  const terminar = (aviso: NonNullable<typeof hecho>, recargar: 'canales' | 'conflictos') => {
    setHecho(aviso)
    setAbierto(null)
    if (recargar === 'canales') setIntentoCanales((n) => n + 1)
    else setIntentoConflictos((n) => n + 1)
    // El botón que abrió el diálogo puede haber desaparecido (el conflicto resuelto, el canal ya sin «Desactivar»)
    requestAnimationFrame(() => {
      if (!document.activeElement || document.activeElement === document.body) titulo.current?.focus()
    })
  }

  const acciones = (canal: Canal) => (
    <div className="canal-acciones">
      {tieneBitacora(canal) && (
        <Link className="boton boton-secundario boton-chico" to={`/canales/${encodeURIComponent(canal.id)}/bitacora`}
          aria-label={`Ver bitácora de ${canal.nombre}`}>
          Ver bitácora
        </Link>
      )}
      {sePuedeDesactivar(canal) && (
        <button type="button" className="boton boton-peligro-borde boton-chico" aria-label={`Desactivar ${canal.nombre}`}
          onClick={() => setAbierto({ desactivar: canal })}>
          Desactivar
        </button>
      )}
    </div>
  )

  return (
    <div className="canales">
      <h1 ref={titulo} tabIndex={-1}>Canales de venta</h1>
      {hecho && <Aviso tipo={hecho.tipo} asunto={hecho.asunto} mensaje={hecho.mensaje} />}

      <section className="canales-bloque" aria-labelledby="t-conflictos">
        <div className="canales-bloque-titulo">
          <h2 id="t-conflictos">
            Conflictos pendientes{paginaConflictos && paginaConflictos.totalElementos > 0 ? ` · ${paginaConflictos.totalElementos}` : ''}
          </h2>
        </div>
        {!conflictos && <Cargando texto="Cargando conflictos…" />}
        {conflictos && 'error' in conflictos && (
          <Aviso tipo="error" asunto="No pudimos cargar los conflictos" mensaje={errorParaMostrar(conflictos.error).mensaje}
            accion={<Reintentar alPulsar={() => setIntentoConflictos((n) => n + 1)} />} />
        )}
        {paginaConflictos && paginaConflictos.contenido.length === 0 && (
          <p className="conflictos-vacio">
            <span aria-hidden="true">✓</span> No hay conflictos pendientes. Cuando un canal externo intente vender una noche
            ocupada, aparecerá aquí.
          </p>
        )}
        {paginaConflictos && paginaConflictos.contenido.length > 0 && (
          <>
            <p className="canales-bloque-pista">
              Un canal externo vendió algo que no cabía y el sistema lo rechazó para no vender dos veces. El huésped cree
              que tiene reserva: llámalo o reubícalo y deja anotado qué se decidió.
            </p>
            <ol className="conflictos">
              {paginaConflictos.contenido.map((conflicto) => (
                <li key={conflicto.id} className="tarjeta conflicto">
                  <div className="conflicto-cabecera">
                    <span className="conflicto-sello" aria-hidden="true">!</span>
                    <h3>
                      <span className="texto-largo">{nombreCanal(conflicto.canalId)}</span> · Reserva externa{' '}
                      <span className="codigo">{conflicto.idExterno}</span>
                    </h3>
                  </div>
                  <dl className="conflicto-datos">
                    <div><dt>Apartamento</dt><dd className="codigo">{conflicto.apartamento}</dd></div>
                    <div><dt>Entrada y salida</dt><dd>{estancia(conflicto)}</dd></div>
                    <div><dt>Huésped</dt><dd><ContactoConflicto conflicto={conflicto} /></dd></div>
                    <div><dt>Detectado</dt><dd>{detectado(conflicto.detectadoEn)}</dd></div>
                  </dl>
                  <p className="conflicto-motivo">
                    <span className="conflicto-motivo-rotulo">Motivo</span>
                    <ConCodigos texto={conflicto.motivo} />
                  </p>
                  <button type="button" className="boton boton-chico conflicto-resolver"
                    aria-label={`Resolver el conflicto de ${conflicto.idExterno}`}
                    onClick={() => setAbierto({ resolver: conflicto })}>
                    Resolver
                  </button>
                </li>
              ))}
            </ol>
            {paginaConflictos.totalPaginas > 1 && (
              <Paginador numero={paginaConflictos.numero} totalPaginas={paginaConflictos.totalPaginas}
                totalElementos={paginaConflictos.totalElementos} unidad={['conflicto', 'conflictos']}
                alCambiar={(n) => cambiarPagina('conflictos', n)} />
            )}
          </>
        )}
      </section>

      <section className="canales-bloque" aria-labelledby="t-canales">
        <div className="canales-bloque-titulo">
          <h2 id="t-canales">Canales</h2>
          <button type="button" className="boton" onClick={() => setAbierto({ registrar: true })}>
            <span aria-hidden="true">+</span> Registrar canal
          </button>
        </div>
        {!canales && <Cargando texto="Cargando canales…" />}
        {canales && 'error' in canales && (
          <Aviso tipo="error" asunto="No pudimos cargar los canales" mensaje={errorParaMostrar(canales.error).mensaje}
            accion={<Reintentar alPulsar={() => setIntentoCanales((n) => n + 1)} />} />
        )}
        {paginaCanales && paginaCanales.contenido.length === 0 && (
          <BloqueVacio icono="canales" titulo={paginaCanales.totalElementos > 0 ? 'Esta página ya no tiene canales' : 'Todavía no hay canales'}
            accion={paginaCanales.totalElementos > 0
              ? <button type="button" className="boton boton-secundario" onClick={() => cambiarPagina('pagina', 0)}>Ir a la primera página</button>
              : undefined}>
            {paginaCanales.totalElementos > 0
              ? 'La lista es más corta de lo que era.'
              : 'Registra el portal web, recepción y cada plataforma externa por la que entren reservas.'}
          </BloqueVacio>
        )}
        {paginaCanales && paginaCanales.contenido.length > 0 && (
          <>
            <div className="tarjeta tabla-marco tabla-canales-marco">
              <table className="tabla tabla-canales">
                <caption className="solo-lectores">Canales de venta</caption>
                <thead>
                  <tr>
                    <th scope="col">Nombre</th>
                    <th scope="col">Tipo</th>
                    <th scope="col">Referencia de la clave</th>
                    <th scope="col">Estado</th>
                    <th scope="col"><span className="solo-lectores">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  {paginaCanales.contenido.map((canal) => (
                    <tr key={canal.id} className={canal.activo ? undefined : 'canal-inactivo'}>
                      <td><span className="canal-nombre texto-largo">{canal.nombre}</span></td>
                      <td>{NOMBRE_DEL_TIPO[canal.tipo]}</td>
                      <td><Referencia canal={canal} /></td>
                      <td><EstadoCanal canal={canal} /></td>
                      <td>{acciones(canal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <ul className="canales-tarjetas" aria-label="Canales de venta">
              {paginaCanales.contenido.map((canal) => (
                <li key={canal.id} className={`tarjeta canal-tarjeta${canal.activo ? '' : ' canal-inactivo'}`}>
                  <div className="canal-tarjeta-cabecera">
                    <span className="canal-nombre texto-largo">{canal.nombre}</span>
                    <EstadoCanal canal={canal} />
                  </div>
                  <p className="suave">{NOMBRE_DEL_TIPO[canal.tipo]}</p>
                  {canal.referenciaCredencial && <p>Referencia de la clave: <Referencia canal={canal} /></p>}
                  {acciones(canal)}
                </li>
              ))}
            </ul>

            <Paginador numero={paginaCanales.numero} totalPaginas={paginaCanales.totalPaginas}
              totalElementos={paginaCanales.totalElementos} unidad={['canal', 'canales']}
              alCambiar={(n) => cambiarPagina('pagina', n)} />
          </>
        )}
      </section>

      {abierto && 'registrar' in abierto && (
        <DialogoRegistrarCanal alCerrar={() => setAbierto(null)}
          alRegistrar={(canal) => terminar({ tipo: 'exito', asunto: 'Canal registrado', mensaje: `${canal.nombre} quedó activo.` }, 'canales')} />
      )}
      {abierto && 'desactivar' in abierto && (
        <DialogoDesactivarCanal canal={abierto.desactivar} alCerrar={() => setAbierto(null)}
          alDesactivar={(canal) => terminar({ tipo: 'exito', asunto: 'Canal desactivado', mensaje: `${canal.nombre} quedó inactivo.` }, 'canales')}
          alYaInactivo={() => terminar({ tipo: 'advertencia', asunto: 'El canal ya estaba inactivo',
            mensaje: `Otra persona desactivó ${abierto.desactivar.nombre}. La lista quedó al día.` }, 'canales')} />
      )}
      {abierto && 'resolver' in abierto && (
        <DialogoResolverConflicto conflicto={abierto.resolver} nombreCanal={nombreCanal(abierto.resolver.canalId)}
          alCerrar={() => setAbierto(null)}
          alResolver={(conflicto) => terminar({ tipo: 'exito', asunto: 'Conflicto resuelto',
            mensaje: `Quedó anotado qué se decidió con la reserva externa ${conflicto.idExterno}.` }, 'conflictos')}
          alYaResuelto={() => terminar({ tipo: 'advertencia', asunto: 'Este conflicto ya lo resolvió otra persona',
            mensaje: 'La lista de conflictos quedó al día.' }, 'conflictos')} />
      )}
    </div>
  )
}

function Referencia({ canal }: { canal: Canal }) {
  return canal.referenciaCredencial
    ? <span className="codigo canal-referencia">{canal.referenciaCredencial}</span>
    : <span aria-label="Sin referencia">—</span>
}

function Reintentar({ alPulsar }: { alPulsar: () => void }) {
  return <button type="button" className="boton boton-secundario boton-chico" onClick={alPulsar}>Reintentar</button>
}

function Cargando({ texto }: { texto: string }) {
  return (
    <div className="tarjeta canales-cargando" aria-busy="true">
      {Array.from({ length: 3 }, (_, fila) => (
        <div key={fila} className="fila-fantasma">
          {Array.from({ length: 4 }, (__, celda) => <span key={celda} className="fantasma" />)}
        </div>
      ))}
      <p className="carga-texto suave" role="status">{texto}</p>
    </div>
  )
}
