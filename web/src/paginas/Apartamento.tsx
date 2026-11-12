import { useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import {
  consultarApartamento,
  consultarEstancia,
  type ApartamentoPublico,
  type EstanciaPortal,
  type Estadia,
  type Grupo,
} from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { explicar } from '../api/mensajes'
import CamposEstancia from '../busqueda/CamposEstancia'
import type { ControlFechas } from '../fechas/SelectorFechas'
import { problemaDeEstadia } from '../busqueda/estadia'
import DesglosePrecio from '../busqueda/DesglosePrecio'
import Foto from '../busqueda/Foto'
import { enPesos, fechaCorta, fechaDesdeHoy, masDias, noches } from '../formato'

const NOCHES_CON_DETALLE_ABIERTO = 4

type Carga =
  | { estado: 'cargando' }
  | { estado: 'no-existe' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; apartamento: ApartamentoPublico }

/**
 * Página de un apartamento (CU-08 · CU-09): sus fotos y su dotación, y las fechas y personas que el visitante mueve
 * para ver el precio al momento. Abre con lo que traía la tarjeta (la dirección lleva `entrada`, `salida`, `adultos`
 * y `ninos`); sin eso, con su próxima noche libre y un adulto.
 *
 * @param umbral edad desde la que se paga, del alojamiento; vacía mientras llega
 */
export default function Apartamento({ umbral }: { umbral: number | null }) {
  const { codigo = '' } = useParams()
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })

  useEffect(() => {
    let vigente = true
    consultarApartamento(codigo)
      .then((apartamento) => vigente && setCarga({ estado: 'listo', apartamento }))
      .catch((error: unknown) => {
        if (!vigente) return
        if (error instanceof ErrorApi && error.codigo === 'NO_ENCONTRADO') {
          setCarga({ estado: 'no-existe' })
        } else {
          setCarga({
            estado: 'error',
            mensaje: explicar(error, 'No pudimos abrir este apartamento. Intenta de nuevo en un momento.'),
          })
        }
      })
    return () => {
      vigente = false
    }
  }, [codigo])

  if (carga.estado === 'cargando') {
    return <main><p className="suave">Cargando…</p></main>
  }
  if (carga.estado !== 'listo') {
    return (
      <main>
        <h1>{carga.estado === 'no-existe' ? 'No encontramos este apartamento' : 'No pudimos cargar el apartamento'}</h1>
        {carga.estado === 'error' && <p role="alert" className="aviso">{carga.mensaje}</p>}
        <Link to="/">Ver todos los apartamentos</Link>
      </main>
    )
  }
  return <Ficha apartamento={carga.apartamento} umbral={umbral} />
}

function Ficha({ apartamento, umbral }: { apartamento: ApartamentoPublico; umbral: number | null }) {
  const [consulta, setConsulta] = useSearchParams()
  // Sin fechas en la dirección, abre con su próxima noche libre y una noche; con solo la llegada, una noche desde
  // ella. Una fecha vacía en la dirección es que el visitante la limpió o aún no la elige: no se supone otra
  const entrada = consulta.get('entrada') ?? apartamento.proximaNocheLibre ?? fechaDesdeHoy()
  const salida = consulta.get('salida') ?? (entrada ? masDias(entrada, 1) : '')
  const estadia: Estadia = { entrada, salida }
  const fechas = useRef<ControlFechas>(null)
  const grupo: Grupo = {
    adultos: Math.max(1, Number(consulta.get('adultos')) || 1),
    ninos: Math.max(0, Number(consulta.get('ninos')) || 0),
  }
  // Las fechas y el grupo viven en la dirección: se puede recargar o compartir el enlace sin perderlos
  function cambiar(e: Estadia, g: Grupo) {
    setConsulta({ ...e, adultos: String(g.adultos), ninos: String(g.ninos) }, { replace: true })
  }

  const fotos = [...apartamento.imagenes].sort((a, b) => Number(b.principal) - Number(a.principal))

  return (
    <main className="ficha">
      <Link to="/" className="volver">← Todos los apartamentos</Link>
      {fotos.length > 0 && (
        <div className="galeria">
          {fotos.map((f, i) => (
            <Foto key={f.url} url={f.url} alt={`Foto ${i + 1} de ${apartamento.nombre}`}
              carga={i === 0 ? 'eager' : 'lazy'} />
          ))}
        </div>
      )}
      {/* En pantallas anchas, la información a la izquierda y el panel fijo a la derecha; en el celular, el panel va
          justo después del nombre, para ver el precio sin recorrer toda la descripción */}
      <div className="ficha-columnas">
        <div className="ficha-cabeza">
          <h1>{apartamento.nombre}</h1>
          <p className="suave">
            Hasta {apartamento.capacidad} {apartamento.capacidad === 1 ? 'persona' : 'personas'} ·{' '}
            {apartamento.dormitorios} {apartamento.dormitorios === 1 ? 'dormitorio' : 'dormitorios'}
          </p>
        </div>
        <aside className="panel-reserva" aria-labelledby="titulo-panel">
          <h2 id="titulo-panel">Tus fechas</h2>
          <div className="panel-campos">
            <CamposEstancia estadia={estadia} grupo={grupo} umbral={umbral} capacidad={apartamento.capacidad}
              codigo={apartamento.codigo} alinearCalendario="fin" refFechas={fechas} alCambiar={cambiar} />
          </div>
          <PrecioDeLaEstancia apartamento={apartamento} estadia={estadia} grupo={grupo} alCambiar={cambiar}
            alPedirFechas={() => fechas.current?.abrir()} />
        </aside>
        <div className="ficha-detalle">
          {apartamento.descripcion && <Descripcion texto={apartamento.descripcion} />}
          {apartamento.caracteristicas.length > 0 && (
            <>
              <h2>Qué incluye</h2>
              <ul className="dotacion" aria-label="Qué incluye">
                {apartamento.caracteristicas.map((c) => (
                  <li key={c}>{c}</li>
                ))}
              </ul>
            </>
          )}
        </div>
      </div>
    </main>
  )
}

/** La respuesta de la API y la consulta que la produjo: si no coincide con la de ahora, está calculando. */
type Calculo =
  | { estado: 'problema'; clave: string; mensaje: string }
  | { estado: 'listo'; clave: string; estancia: EstanciaPortal }

interface PropsPrecio {
  apartamento: ApartamentoPublico
  estadia: Estadia
  grupo: Grupo
  alCambiar: (estadia: Estadia, grupo: Grupo) => void
  /** Sin las dos fechas, el botón de reservar abre el calendario. */
  alPedirFechas: () => void
}

/** Se recalcula solo cada vez que cambian las fechas o el grupo. */
function PrecioDeLaEstancia({ apartamento, estadia, grupo, alCambiar, alPedirFechas }: PropsPrecio) {
  const [calculo, setCalculo] = useState<Calculo | null>(null)
  const [vuelta, setVuelta] = useState(0)
  const { codigo, capacidad } = apartamento
  const { entrada, salida } = estadia
  const { adultos, ninos } = grupo
  const clave = `${codigo}|${entrada}|${salida}|${adultos}|${ninos}`
  const faltanFechas = !entrada || !salida
  const antesDeLlamar = faltanFechas ? null : problemaDeEstadia(estadia)
    ?? (adultos + ninos > capacidad ? `Este apartamento admite hasta ${capacidad} personas.` : null)

  // Al volver a la pestaña se recalcula: una reserva pudo vencer o tomarse mientras tanto
  useEffect(() => {
    const alVolver = () => document.visibilityState === 'visible' && setVuelta((v) => v + 1)
    document.addEventListener('visibilitychange', alVolver)
    return () => document.removeEventListener('visibilitychange', alVolver)
  }, [])

  useEffect(() => {
    if (faltanFechas || antesDeLlamar) return
    let vigente = true // si cambian las fechas antes de que responda, se ignora la respuesta vieja
    consultarEstancia(codigo, { entrada, salida }, { adultos, ninos })
      .then((estancia) => vigente && setCalculo({ estado: 'listo', clave, estancia }))
      .catch((error: unknown) => vigente && setCalculo({
        estado: 'problema', clave,
        mensaje: explicar(error, 'No pudimos calcular el precio. Intenta de nuevo en un momento.'),
      }))
    return () => {
      vigente = false
    }
  }, [codigo, entrada, salida, adultos, ninos, clave, faltanFechas, antesDeLlamar, vuelta])

  if (faltanFechas) {
    return (
      <div className="precio">
        <p className="suave">El precio aparece al elegir la llegada y la salida.</p>
        <button type="button" className="accion-principal" onClick={alPedirFechas}>
          {entrada ? 'Elige la fecha de salida' : 'Elige tus fechas'}
        </button>
      </div>
    )
  }
  if (antesDeLlamar) {
    return <p role="alert" className="aviso">{antesDeLlamar}</p>
  }
  const calculando = calculo?.clave !== clave
  if (calculo === null || (calculando && calculo.estado === 'problema')) {
    return <p className="suave" aria-live="polite">Calculando el precio…</p>
  }
  if (calculo.estado === 'problema') {
    return <p role="alert" className="aviso">{calculo.mensaje}</p>
  }

  const { disponible, precio, proximaNocheLibre } = calculo.estancia
  if (!disponible) {
    // Sin ninguna noche libre en el año, no es culpa de las fechas elegidas: no se dice que esas no sirven
    if (proximaNocheLibre === null) {
      return (
        <p className="aviso" aria-live="polite" aria-busy={calculando}>
          Este apartamento no tiene fechas libres en el próximo año.
        </p>
      )
    }
    const n = noches(estadia.entrada, estadia.salida)
    return (
      <div className="precio" aria-live="polite" aria-busy={calculando}>
        <p className="aviso">No está libre en esas fechas.</p>
        <button type="button" className="secundario"
          onClick={() => alCambiar({ entrada: proximaNocheLibre, salida: masDias(proximaNocheLibre, n) }, grupo)}>
          Ver desde el {fechaCorta(proximaNocheLibre)}
        </button>
      </div>
    )
  }
  const n = precio.desglose.length
  return (
    // Mientras llega el nuevo, el precio anterior se queda atenuado: la página no salta
    <div className="precio" aria-live="polite" aria-busy={calculando}>
      <p className="total-estancia">
        Total {n} {n === 1 ? 'noche' : 'noches'}: <strong>{enPesos(precio.total)}</strong>
      </p>
      <p className="por-noche suave">
        {enPesos(precio.desglose[0].tarifa)} por adulto, noche del {fechaCorta(estadia.entrada)}
      </p>
      {/* Con una noche el detalle repite el total. Las estadías largas lo abren a pedido, para que el panel no crezca
          más que la pantalla */}
      {n > 1 && (
        <details key={n} open={n <= NOCHES_CON_DETALLE_ABIERTO}>
          <summary>Ver detalle por noche</summary>
          <DesglosePrecio precio={precio} />
        </details>
      )}
      <Link className="boton accion-principal" aria-disabled={calculando}
        to={`/apartamentos/${encodeURIComponent(codigo)}/reservar?${new URLSearchParams({ entrada, salida,
          adultos: String(adultos), ninos: String(ninos) })}`}>
        Reservar
      </Link>
    </div>
  )
}

/**
 * Una descripción larga se pliega a 8 líneas con «Leer más», para que «Qué incluye» y el precio queden a la vista;
 * una corta se ve entera y sin botón.
 */
function Descripcion({ texto }: { texto: string }) {
  const parrafo = useRef<HTMLParagraphElement>(null)
  const [abierta, setAbierta] = useState(false)
  const [larga, setLarga] = useState(false)

  useEffect(() => {
    const p = parrafo.current
    // Se mide plegada: si el texto no cabe en las 8 líneas, hace falta el botón
    if (p && !abierta) setLarga(p.scrollHeight > p.clientHeight + 1)
  }, [texto, abierta])

  return (
    <>
      <p ref={parrafo} className="descripcion-completa" data-plegada={!abierta}>{texto}</p>
      {larga && (
        <button type="button" className="secundario leer-mas" aria-expanded={abierta}
          onClick={() => setAbierta(!abierta)}>
          {abierta ? 'Leer menos' : 'Leer más'}
        </button>
      )}
    </>
  )
}
