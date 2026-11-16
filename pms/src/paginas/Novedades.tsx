import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { listarCodigosDeApartamentos } from '../api/apartamentos'
import { listarNovedades, type FiltroNovedades, type Novedad } from '../api/novedades'
import type { Pagina } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import { textoDeEleccion } from '../componentes/eleccion'
import Icono from '../componentes/Icono'
import Paginador from '../componentes/Paginador'
import SeleccionVarios from '../componentes/SeleccionVarios'
import { useTitulo } from '../componentes/useTitulo'
import { DialogoAvanzarNovedad, DialogoHistorialNovedad, DialogoReportarNovedad } from '../novedades/Dialogos'
import {
  accionSiguiente,
  ATAJOS_ESTADO,
  listaDe,
  OPCIONES_ESTADO,
  OPCIONES_GRAVEDAD,
  POR_ATENDER,
  reportada,
  resumen,
} from '../novedades/logica'
import { EtiquetaEstadoNovedad, EtiquetaGravedad } from '../novedades/Piezas'
import { errorParaMostrar } from '../reserva/mensajes'

type Resultado = { clave: string; pagina: Pagina<Novedad> } | { clave: string; error: unknown }
type Abierto = { reportar: true } | { historial: Novedad } | { avanzar: Novedad }
type AvisoPantalla = { tipo: 'exito' | 'info'; asunto: string; mensaje: string }

/**
 * Sin `estado` en la dirección se ven las por atender, que es lo que el personal de servicio busca al entrar; con
 * `estado=` vacío, todas.
 */
function filtroDe(parametros: URLSearchParams): FiltroNovedades {
  return {
    estados: parametros.has('estado') ? listaDe(parametros.get('estado')) : [...POR_ATENDER],
    gravedades: listaDe(parametros.get('gravedad')),
    apartamentos: listaDe(parametros.get('apartamento')),
  }
}

function parametrosDe(filtro: FiltroNovedades): URLSearchParams {
  const parametros = new URLSearchParams({ estado: filtro.estados.join(',') })
  if (filtro.gravedades.length) parametros.set('gravedad', filtro.gravedades.join(','))
  if (filtro.apartamentos.length) parametros.set('apartamento', filtro.apartamentos.join(','))
  return parametros
}

/**
 * Novedades de mantenimiento (DEC-132): las de todos los apartamentos en una lista, para el personal de servicio y la
 * administración. Reportar, avanzar un paso y ver el historial; nada de editar ni borrar (NOV-06).
 */
export default function Novedades() {
  const [parametros, setParametros] = useSearchParams()
  const filtro = filtroDe(parametros)
  const numero = Math.max(0, Number(parametros.get('pagina')) || 0)
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [abierto, setAbierto] = useState<Abierto | null>(null)
  const [aviso, setAviso] = useState<AvisoPantalla | null>(null)
  const titulo = useRef<HTMLHeadingElement>(null)
  const consulta = `${parametrosDe(filtro)}#${numero}`
  const clave = `${consulta}#${intento}`
  useTitulo('Novedades')

  useEffect(() => {
    let vigente = true
    listarNovedades(filtroDe(new URLSearchParams(consulta.split('#')[0])), numero)
      .then((pagina) => vigente && setResultado({ clave, pagina }))
      .catch((error: unknown) => vigente && setResultado({ clave, error }))
    return () => {
      vigente = false
    }
  }, [consulta, numero, clave])

  const actual = resultado?.clave === clave ? resultado : null
  const pagina = actual && 'pagina' in actual ? actual.pagina : null
  const porAtender = parametros.get('estado') === null

  const terminar = (nuevo: AvisoPantalla) => {
    setAviso(nuevo)
    setAbierto(null)
    setIntento(intento + 1)
    requestAnimationFrame(() => {
      if (!document.activeElement || document.activeElement === document.body) titulo.current?.focus()
    })
  }
  const irA = (n: number) => {
    const siguientes = parametrosDe(filtro)
    siguientes.set('pagina', String(n))
    setParametros(siguientes)
  }

  return (
    <div className="novedades">
      <div className="reservas-encabezado">
        <h1 ref={titulo} tabIndex={-1}>Novedades</h1>
        <button type="button" className="boton" onClick={() => setAbierto({ reportar: true })}>
          <span aria-hidden="true">+</span> Reportar novedad
        </button>
      </div>

      <Filtros key={parametrosDe(filtro).toString()} filtro={filtro}
        alAplicar={(nuevo) => setParametros(parametrosDe(nuevo))}
        alLimpiar={() => setParametros(new URLSearchParams({ estado: '' }))} />

      {aviso && <Aviso tipo={aviso.tipo} asunto={aviso.asunto} mensaje={aviso.mensaje} />}
      {!actual && <Cargando />}
      {actual && 'error' in actual && (
        <Aviso tipo="error" asunto="No se pudieron cargar las novedades" mensaje={errorParaMostrar(actual.error).mensaje}
          accion={<button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>Reintentar</button>} />
      )}

      {pagina && pagina.contenido.length === 0 && (
        pagina.numero > 0 ? (
          <BloqueVacio icono="novedades" titulo="Esta página ya no tiene novedades"
            accion={<button type="button" className="boton boton-secundario" onClick={() => irA(0)}>Ir a la primera página</button>}>
            La lista es más corta de lo que era.
          </BloqueVacio>
        ) : porAtender && !filtro.gravedades.length && !filtro.apartamentos.length ? (
          <BloqueVacio icono="novedades" titulo="No hay novedades por atender">
            Si encuentras algo en un apartamento, repórtalo aquí.
          </BloqueVacio>
        ) : (
          <BloqueVacio icono="buscar" titulo="Ninguna novedad coincide con los filtros"
            accion={<button type="button" className="boton boton-secundario" onClick={() => setParametros(new URLSearchParams({ estado: '' }))}>Limpiar filtros</button>}>
            Prueba con otro estado, otra gravedad u otro apartamento.
          </BloqueVacio>
        )
      )}

      {pagina && pagina.contenido.length > 0 && (
        <>
          <div className="tarjeta desplazable tabla-novedades-marco">
            <table className="tabla tabla-novedades">
              <caption className="solo-lectores">Novedades</caption>
              <thead>
                <tr>
                  <th scope="col">Apto.</th>
                  <th scope="col">Gravedad</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Descripción</th>
                  <th scope="col">Reportada</th>
                  <th scope="col"><span className="solo-lectores">Acción</span></th>
                </tr>
              </thead>
              <tbody>
                {pagina.contenido.map((novedad) => (
                  <tr key={novedad.id} className={`novedad-fila${novedad.gravedad === 'ALTA' ? ' novedad-alta' : ''}`}>
                    <td><AbrirHistorial novedad={novedad} alAbrir={() => setAbierto({ historial: novedad })} /></td>
                    <td><EtiquetaGravedad gravedad={novedad.gravedad} /></td>
                    <td><EtiquetaEstadoNovedad estado={novedad.estado} /></td>
                    <td><span className="novedad-descripcion texto-largo">{novedad.descripcion}</span></td>
                    <td className="suave novedad-quien">{reportada(novedad)}</td>
                    <td className="derecha">
                      <Accion novedad={novedad} chico alAvanzar={() => setAbierto({ avanzar: novedad })} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="novedades-tarjetas" aria-label="Novedades">
            {pagina.contenido.map((novedad) => (
              <li key={novedad.id} className={`tarjeta novedad-tarjeta novedad-fila${novedad.gravedad === 'ALTA' ? ' novedad-alta' : ''}`}>
                <div className="novedad-tarjeta-cabecera">
                  <AbrirHistorial novedad={novedad} alAbrir={() => setAbierto({ historial: novedad })} />
                  <EtiquetaGravedad gravedad={novedad.gravedad} />
                </div>
                <EtiquetaEstadoNovedad estado={novedad.estado} />
                <p className="novedad-descripcion texto-largo">{novedad.descripcion}</p>
                <p className="suave novedad-quien">{reportada(novedad)}</p>
                <Accion novedad={novedad} alAvanzar={() => setAbierto({ avanzar: novedad })} />
              </li>
            ))}
          </ul>

          <Paginador numero={pagina.numero} totalPaginas={pagina.totalPaginas} totalElementos={pagina.totalElementos}
            unidad={['novedad', 'novedades']} alCambiar={irA} />
        </>
      )}

      {abierto && 'reportar' in abierto && (
        <DialogoReportarNovedad alCerrar={() => setAbierto(null)}
          alReportar={(novedad) => terminar({ tipo: 'exito', asunto: 'Novedad reportada', mensaje: resumen(novedad) })} />
      )}
      {abierto && 'historial' in abierto && (
        <DialogoHistorialNovedad novedad={abierto.historial} alCerrar={() => setAbierto(null)}
          alAvanzar={(novedad) => setAbierto({ avanzar: novedad })} />
      )}
      {abierto && 'avanzar' in abierto && (
        <DialogoAvanzarNovedad novedad={abierto.avanzar} alCerrar={() => setAbierto(null)}
          alAvanzar={(novedad) => terminar({ tipo: 'exito',
            asunto: novedad.estado === 'CERRADA' ? 'Novedad cerrada' : 'Pasó a revisión',
            mensaje: resumen(novedad) })}
          alYaAvanzo={() => terminar({ tipo: 'info', asunto: 'Esta novedad ya avanzó',
            mensaje: 'Puede que otra persona la haya movido. La lista se recargó.' })} />
      )}
    </div>
  )
}

/** El apartamento abre el historial; su capa cubre toda la fila o la tarjeta, y la acción queda por encima. */
function AbrirHistorial({ novedad, alAbrir }: { novedad: Novedad; alAbrir: () => void }) {
  return (
    <button type="button" className="novedad-abrir codigo" onClick={alAbrir}
      aria-label={`${novedad.apartamento}: ver el historial de la novedad`}>
      {novedad.apartamento}
    </button>
  )
}

function Accion({ novedad, chico = false, alAvanzar }: { novedad: Novedad; chico?: boolean; alAvanzar: () => void }) {
  const accion = accionSiguiente(novedad.estado)
  if (!accion) return null
  return (
    <button type="button" className={chico ? 'boton boton-chico novedad-accion' : 'boton boton-bloque boton-grande novedad-accion'}
      aria-label={`${accion.texto}: novedad de ${novedad.apartamento}`} onClick={alAvanzar}>
      {accion.texto}
    </button>
  )
}

/** Estado (con «Por atender»), gravedad y apartamento, cada uno de varios valores, como en Reservas (DEC-119). */
function Filtros({ filtro, alAplicar, alLimpiar }: {
  filtro: FiltroNovedades
  alAplicar: (filtro: FiltroNovedades) => void
  alLimpiar: () => void
}) {
  const [borrador, setBorrador] = useState(filtro)
  const [abiertos, setAbiertos] = useState(false)
  const [apartamentos, setApartamentos] = useState<string[]>([])

  useEffect(() => {
    let vigente = true
    // Si la lista no carga, el filtro queda con los de la dirección y la pantalla sigue funcionando
    listarCodigosDeApartamentos().then((codigos) => vigente && setApartamentos(codigos)).catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [])

  const aplicados = [
    filtro.estados.length ? textoDeEleccion(filtro.estados, [...OPCIONES_ESTADO], ATAJOS_ESTADO) : null,
    filtro.gravedades.length ? textoDeEleccion(filtro.gravedades, OPCIONES_GRAVEDAD) : null,
    filtro.apartamentos.length ? filtro.apartamentos.join(', ') : null,
  ].filter(Boolean)
  const opcionesApartamento = [...filtro.apartamentos.filter((c) => !apartamentos.includes(c)), ...apartamentos]

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    alAplicar(borrador)
  }

  return (
    <div className="filtros">
      <button type="button" className="boton boton-secundario boton-bloque boton-filtros" aria-expanded={abiertos}
        aria-controls="filtros-novedades" onClick={() => setAbiertos(!abiertos)}>
        <span>
          Filtros{aplicados.length > 0 && ` (${aplicados.length})`}
          {aplicados.length > 0 && <span className="filtros-resumen">{aplicados.join(' · ')}</span>}
        </span>
        <Icono nombre="abajo" />
      </button>
      <form id="filtros-novedades" role="search" aria-label="Filtros de novedades" noValidate onSubmit={enviar}
        className={abiertos ? 'tarjeta filtros-cuerpo abierto' : 'tarjeta filtros-cuerpo'}>
        <div className="filtros-campos">
          <SeleccionVarios rotulo="Estado" opciones={OPCIONES_ESTADO} atajos={ATAJOS_ESTADO} elegidos={borrador.estados}
            alCambiar={(estados) => setBorrador({ ...borrador, estados })} />
          <SeleccionVarios rotulo="Gravedad" opciones={OPCIONES_GRAVEDAD} elegidos={borrador.gravedades}
            alCambiar={(gravedades) => setBorrador({ ...borrador, gravedades })} />
          <SeleccionVarios rotulo="Apartamento" opciones={opcionesApartamento.map((c) => [c, c] as const)}
            elegidos={borrador.apartamentos} alCambiar={(apartamentosElegidos) => setBorrador({ ...borrador, apartamentos: apartamentosElegidos })} />
          <div className="filtros-botones">
            <button type="button" className="boton boton-secundario" onClick={alLimpiar}>Limpiar</button>
            <button type="submit" className="boton">Filtrar</button>
          </div>
        </div>
      </form>
    </div>
  )
}

function Cargando() {
  return (
    <div className="tarjeta" aria-busy="true">
      {Array.from({ length: 3 }, (_, fila) => (
        <div key={fila} className="fila-fantasma">
          {Array.from({ length: 4 }, (__, celda) => <span key={celda} className="fantasma" />)}
        </div>
      ))}
      <p className="carga-texto suave" role="status">Cargando novedades…</p>
    </div>
  )
}
