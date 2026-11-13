import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { listarApartamentos, type FichaApartamento } from '../api/apartamentos'
import type { Pagina } from '../api/reservas'
import { consultarTemporadas, tarifasVigentes, type Tarifa, type Temporada } from '../api/tarifas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { dinero } from '../formato'
import { errorParaMostrar } from '../reserva/mensajes'
import {
  DialogoDesactivar,
  DialogoFechas,
  DialogoHistorial,
  DialogoTarifa,
  DialogoTemporada,
  Visto,
} from '../tarifas/Dialogos'
import { ordenarTemporadas, textoEstancia, textoFechas, vigentesPorTemporada } from '../tarifas/logica'

interface Datos {
  temporadas: Temporada[]
  pagina: Pagina<FichaApartamento>
  /** Las tarifas vigentes de cada apartamento de la página, por su código. */
  tarifas: Record<string, Tarifa[]>
}

type Resultado = { clave: string; datos: Datos } | { clave: string; error: unknown }

type Abierto =
  | { nueva: true }
  | { fechas: Temporada }
  | { desactivar: Temporada }
  | { tarifa: { apartamento: FichaApartamento; temporada: Temporada } }
  | { historial: FichaApartamento }

/**
 * Temporadas del alojamiento y la tarifa de cada apartamento en cada temporada activa (sprint 4, solo el
 * administrador). Las tarifas van de 10 en 10, como los apartamentos: una llamada por apartamento de la página.
 */
export default function Tarifas() {
  const [parametros, setParametros] = useSearchParams()
  const numero = Math.max(0, Number(parametros.get('pagina')) || 0)
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [abierto, setAbierto] = useState<Abierto | null>(null)
  const [hecho, setHecho] = useState<{ asunto: string; mensaje: string } | null>(null)
  const titulo = useRef<HTMLHeadingElement>(null)
  const clave = `${numero}#${intento}`
  useTitulo('Temporadas y tarifas')

  useEffect(() => {
    let vigente = true
    Promise.all([consultarTemporadas(), listarApartamentos(numero)])
      .then(async ([temporadas, pagina]) => {
        const listas = await Promise.all(pagina.contenido.map((apartamento) => tarifasVigentes(apartamento.codigo)))
        const tarifas = Object.fromEntries(pagina.contenido.map((apartamento, i) => [apartamento.codigo, listas[i]]))
        if (vigente) setResultado({ clave, datos: { temporadas, pagina, tarifas } })
      })
      .catch((error: unknown) => vigente && setResultado({ clave, error }))
    return () => {
      vigente = false
    }
  }, [numero, clave])

  const actual = resultado?.clave === clave ? resultado : null
  const datos = actual && 'datos' in actual ? actual.datos : null

  const cerrar = () => setAbierto(null)
  // Si el botón que se usó ya no está (una temporada desactivada), el foco vuelve al título
  const recuperarFoco = () => requestAnimationFrame(() => {
    if (!document.activeElement || document.activeElement === document.body) titulo.current?.focus()
  })
  const terminar = (aviso: { asunto: string; mensaje: string }, cambio?: (d: Datos) => Datos) => {
    if (datos && cambio) setResultado({ clave, datos: cambio(datos) })
    setHecho(aviso)
    setAbierto(null)
    recuperarFoco()
  }
  const conCalendario = (temporadas: Temporada[]) => (d: Datos) => ({ ...d, temporadas })

  return (
    <div className="tarifas">
      <h1 ref={titulo} tabIndex={-1}>Temporadas y tarifas</h1>

      {hecho && <Aviso tipo="exito" asunto={hecho.asunto} mensaje={hecho.mensaje} />}
      {!actual && <Cargando />}
      {actual && 'error' in actual && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar las temporadas y tarifas"
          mensaje={errorParaMostrar(actual.error).mensaje}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      )}

      {datos && (
        <>
          <Temporadas temporadas={datos.temporadas} alNueva={() => setAbierto({ nueva: true })}
            alCambiarFechas={(t) => setAbierto({ fechas: t })} alDesactivar={(t) => setAbierto({ desactivar: t })} />
          <TablaTarifas datos={datos} alParaPagina={(n) => setParametros({ pagina: String(n) })}
            alPrimeraPagina={() => setParametros({})}
            alTarifa={(apartamento, temporada) => setAbierto({ tarifa: { apartamento, temporada } })}
            alHistorial={(apartamento) => setAbierto({ historial: apartamento })} />
        </>
      )}

      {datos && abierto && 'nueva' in abierto && (
        <DialogoTemporada temporadas={datos.temporadas} alCerrar={cerrar}
          alCrear={(temporada) => {
            terminar({ asunto: 'Temporada creada', mensaje: `${temporada.nombre} rige para las reservas nuevas.` })
            setIntento(intento + 1)
          }} />
      )}
      {datos && abierto && 'fechas' in abierto && (
        <DialogoFechas temporada={abierto.fechas} temporadas={datos.temporadas} alCerrar={cerrar}
          alHacer={(temporadas) => terminar({ asunto: 'Fechas cambiadas',
            mensaje: `${abierto.fechas.nombre}: ${textoFechas(temporadas.find((t) => t.id === abierto.fechas.id) ?? abierto.fechas)}.` },
          conCalendario(temporadas))} />
      )}
      {datos && abierto && 'desactivar' in abierto && (
        <DialogoDesactivar temporada={abierto.desactivar} temporadas={datos.temporadas} alCerrar={cerrar}
          alHacer={(temporadas) => terminar({ asunto: 'Temporada desactivada',
            mensaje: `Las noches de ${abierto.desactivar.nombre} se cobran ahora con la temporada base.` },
          conCalendario(temporadas))} />
      )}
      {datos && abierto && 'tarifa' in abierto && (
        <DialogoTarifa
          apartamento={abierto.tarifa.apartamento}
          temporada={abierto.tarifa.temporada}
          vigente={vigentesPorTemporada(datos.tarifas[abierto.tarifa.apartamento.codigo] ?? []).get(abierto.tarifa.temporada.id)}
          temporadas={datos.temporadas}
          alCerrar={cerrar}
          alHacer={(tarifa) => terminar(
            { asunto: 'Tarifa guardada', mensaje: `${tarifa.apartamento} · ${abierto.tarifa.temporada.nombre}: ${dinero(tarifa.valorPorOcupante)} por persona y noche desde hoy.` },
            (d) => ({ ...d, tarifas: { ...d.tarifas, [tarifa.apartamento]: [
              ...(d.tarifas[tarifa.apartamento] ?? []).filter((t) => t.temporada !== tarifa.temporada), tarifa,
            ] } }),
          )}
        />
      )}
      {datos && abierto && 'historial' in abierto && (
        <DialogoHistorial apartamento={abierto.historial} temporadas={datos.temporadas}
          vigentes={new Set((datos.tarifas[abierto.historial.codigo] ?? [])
            // La de una temporada desactivada ya no se cobra: no se marca como vigente
            .filter((t) => datos.temporadas.some((temporada) => temporada.id === t.temporada && temporada.activa))
            .map((t) => t.id))}
          alCerrar={cerrar} />
      )}
    </div>
  )
}

function EstadoTemporada({ temporada }: { temporada: Temporada }) {
  if (!temporada.activa) {
    return (
      <span className="etiqueta temporada-inactiva">
        <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true"><circle cx="6" cy="6" r="4" /></svg>
        Inactiva
      </span>
    )
  }
  return <span className="etiqueta temporada-activa"><Visto />{temporada.esBase ? 'Siempre activa' : 'Activa'}</span>
}

const PISTA_BASE = 'Cubre los días que no caen en otra temporada'

function Temporadas({ temporadas, alNueva, alCambiarFechas, alDesactivar }: {
  temporadas: Temporada[]
  alNueva: () => void
  alCambiarFechas: (temporada: Temporada) => void
  alDesactivar: (temporada: Temporada) => void
}) {
  const { activas, inactivas } = ordenarTemporadas(temporadas)
  const acciones = (temporada: Temporada) => temporada.activa && !temporada.esBase && (
    <div className="temporada-acciones">
      <button type="button" className="boton boton-secundario boton-chico"
        aria-label={`Cambiar fechas de ${temporada.nombre}`} onClick={() => alCambiarFechas(temporada)}>
        Cambiar fechas
      </button>
      <button type="button" className="boton boton-peligro-borde boton-chico"
        aria-label={`Desactivar ${temporada.nombre}`} onClick={() => alDesactivar(temporada)}>
        Desactivar
      </button>
    </div>
  )
  const filas = (lista: Temporada[], nombreTabla: string) => (
    <>
      <div className="tarjeta desplazable tabla-temporadas-marco">
        <table className="tabla tabla-temporadas">
          <caption className="solo-lectores">{nombreTabla}</caption>
          <thead>
            <tr>
              <th scope="col">Temporada</th>
              <th scope="col">Fechas</th>
              <th scope="col">Estancia mínima</th>
              <th scope="col">Estado</th>
              <th scope="col"><span className="solo-lectores">Acciones</span></th>
            </tr>
          </thead>
          <tbody>
            {lista.map((temporada) => (
              <tr key={temporada.id} className={temporada.activa ? undefined : 'temporada-apagada'}>
                <td>
                  <div className="celda-temporada">
                    <strong className="texto-largo">{temporada.nombre}</strong>
                    {temporada.esBase && <span className="campo-pista">{PISTA_BASE}</span>}
                  </div>
                </td>
                <td className="sin-cortar">{textoFechas(temporada)}</td>
                <td className="sin-cortar">{temporada.esBase ? '—' : textoEstancia(temporada.estanciaMinimaNoches)}</td>
                <td><EstadoTemporada temporada={temporada} /></td>
                <td className="derecha">{acciones(temporada)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ul className="temporadas-tarjetas" aria-label={nombreTabla}>
        {lista.map((temporada) => (
          <li key={temporada.id} className={`tarjeta temporada-tarjeta${temporada.activa ? '' : ' temporada-apagada'}`}>
            <div className="temporada-tarjeta-cabecera">
              <strong className="texto-largo">{temporada.nombre}</strong>
              <EstadoTemporada temporada={temporada} />
            </div>
            {temporada.esBase
              ? <p className="campo-pista">{PISTA_BASE}.</p>
              : <p className="suave">{textoFechas(temporada)} · {textoEstancia(temporada.estanciaMinimaNoches)}</p>}
            {acciones(temporada)}
          </li>
        ))}
      </ul>
    </>
  )

  return (
    <section aria-labelledby="titulo-temporadas" className="detalle-seccion">
      <div className="ficha-seccion-cabecera">
        <h2 id="titulo-temporadas">Temporadas</h2>
        <button type="button" className="boton" onClick={alNueva}>
          <span aria-hidden="true">+</span> Nueva temporada
        </button>
      </div>
      {filas(activas, 'Temporadas activas')}
      {activas.length === 1 && (
        <p className="campo-pista">No hay otras temporadas: todas las noches se cobran con la temporada base.</p>
      )}
      {inactivas.length > 0 && (
        <details className="temporadas-inactivas">
          <summary>Ver inactivas ({inactivas.length})</summary>
          {filas(inactivas, 'Temporadas inactivas')}
        </details>
      )}
    </section>
  )
}

function CeldaTarifa({ apartamento, temporada, tarifa, alTocar }: {
  apartamento: FichaApartamento
  temporada: Temporada
  tarifa?: Tarifa
  alTocar: () => void
}) {
  const lapiz = (
    <svg className="icono icono-lapiz" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 20h4L19 9l-4-4L4 16v4Z" />
      <path d="m13.5 6.5 4 4" />
    </svg>
  )
  if (!tarifa) {
    return (
      <button type="button" className="celda-tarifa sin-tarifa" onClick={alTocar}
        aria-label={`Poner la tarifa de ${apartamento.codigo} en ${temporada.nombre}: sin tarifa`}>
        <span className="sello-sin-tarifa" aria-hidden="true">!</span> Sin tarifa {lapiz}
      </button>
    )
  }
  return (
    <button type="button" className="celda-tarifa" onClick={alTocar}
      aria-label={`Cambiar la tarifa de ${apartamento.codigo} en ${temporada.nombre}: ${dinero(tarifa.valorPorOcupante)}`}>
      <span className="numero">{dinero(tarifa.valorPorOcupante)}</span> {lapiz}
    </button>
  )
}

function NoSeVende() {
  return (
    <span className="no-se-vende">
      <span className="sello-sin-tarifa" aria-hidden="true">!</span> No se puede poner a la venta
    </span>
  )
}

function TablaTarifas({ datos, alParaPagina, alPrimeraPagina, alTarifa, alHistorial }: {
  datos: Datos
  alParaPagina: (pagina: number) => void
  alPrimeraPagina: () => void
  alTarifa: (apartamento: FichaApartamento, temporada: Temporada) => void
  alHistorial: (apartamento: FichaApartamento) => void
}) {
  const { activas } = ordenarTemporadas(datos.temporadas)
  const { pagina } = datos
  const filas = pagina.contenido.map((apartamento) => {
    const vigentes = vigentesPorTemporada(datos.tarifas[apartamento.codigo] ?? [])
    // Solo un retirado puede quedar sin tarifa (TAR-03): a uno a la venta la API se la exige en cada temporada nueva
    const falta = !apartamento.activo && activas.some((t) => !vigentes.has(t.id))
    return { apartamento, vigentes, falta }
  })
  const historial = (apartamento: FichaApartamento) => (
    <button type="button" className="boton boton-secundario boton-chico" aria-label={`Historial de ${apartamento.codigo}`}
      onClick={() => alHistorial(apartamento)}>
      Historial
    </button>
  )

  return (
    <section aria-labelledby="titulo-tarifas" className="detalle-seccion">
      <div>
        <h2 id="titulo-tarifas">Tarifas</h2>
        <p className="campo-pista">
          Valor por persona y noche. Tocar un valor lo cambia: rige para las reservas nuevas desde hoy y las reservas
          ya hechas no cambian.
        </p>
      </div>

      {pagina.contenido.length === 0 && (
        pagina.numero > 0 ? (
          <BloqueVacio icono="tarifas" titulo="Esta página ya no tiene apartamentos"
            accion={<button type="button" className="boton boton-secundario" onClick={alPrimeraPagina}>Ir a la primera página</button>}>
            La lista es más corta de lo que era.
          </BloqueVacio>
        ) : (
          <BloqueVacio icono="tarifas" titulo="Todavía no hay apartamentos"
            accion={<Link to="/apartamentos" className="boton boton-secundario">Ir a apartamentos</Link>}>
            Cuando crees uno, aquí le pones su tarifa en cada temporada.
          </BloqueVacio>
        )
      )}

      {pagina.contenido.length > 0 && (
        <>
          <div className="tarjeta desplazable tabla-tarifas-marco">
            <table className="tabla tabla-tarifas">
              <caption className="solo-lectores">Tarifas por apartamento y temporada</caption>
              <thead>
                <tr>
                  <th scope="col">Apartamento</th>
                  {activas.map((temporada) => <th key={temporada.id} scope="col">{temporada.nombre}</th>)}
                  <th scope="col"><span className="solo-lectores">Historial</span></th>
                </tr>
              </thead>
              <tbody>
                {filas.map(({ apartamento, vigentes, falta }) => (
                  <tr key={apartamento.codigo}>
                    <th scope="row" className="celda-apartamento">
                      <div>
                        <span><span className="codigo">{apartamento.codigo}</span> {apartamento.nombre}</span>
                        {falta && <NoSeVende />}
                      </div>
                    </th>
                    {activas.map((temporada) => (
                      <td key={temporada.id}>
                        <CeldaTarifa apartamento={apartamento} temporada={temporada} tarifa={vigentes.get(temporada.id)}
                          alTocar={() => alTarifa(apartamento, temporada)} />
                      </td>
                    ))}
                    <td className="derecha">{historial(apartamento)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="tarifas-tarjetas" aria-label="Tarifas por apartamento">
            {filas.map(({ apartamento, vigentes, falta }) => (
              <li key={apartamento.codigo} className="tarjeta tarifa-tarjeta">
                <div>
                  <strong><span className="codigo">{apartamento.codigo}</span> {apartamento.nombre}</strong>
                  {falta && <NoSeVende />}
                </div>
                <dl className="tarifa-renglones">
                  {activas.map((temporada) => (
                    <div key={temporada.id}>
                      <dt>{temporada.nombre}</dt>
                      <dd>
                        <CeldaTarifa apartamento={apartamento} temporada={temporada} tarifa={vigentes.get(temporada.id)}
                          alTocar={() => alTarifa(apartamento, temporada)} />
                      </dd>
                    </div>
                  ))}
                </dl>
                {historial(apartamento)}
              </li>
            ))}
          </ul>

          <Paginador numero={pagina.numero} totalPaginas={pagina.totalPaginas} totalElementos={pagina.totalElementos}
            unidad={['apartamento', 'apartamentos']} alCambiar={alParaPagina} />
        </>
      )}
    </section>
  )
}

function Cargando() {
  return (
    <div className="tarjeta" aria-busy="true">
      {Array.from({ length: 4 }, (_, fila) => (
        <div key={fila} className="fila-fantasma">
          {Array.from({ length: 5 }, (__, celda) => (
            <span key={celda} className="fantasma" />
          ))}
        </div>
      ))}
      <p className="carga-texto suave" role="status">
        Cargando temporadas y tarifas…
      </p>
    </div>
  )
}
