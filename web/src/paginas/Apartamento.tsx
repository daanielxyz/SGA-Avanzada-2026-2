import { useEffect, useState } from 'react'
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
import CamposEstancia from '../busqueda/CamposEstancia'
import { problemaDeEstadia } from '../busqueda/estadia'
import DesglosePrecio from '../busqueda/DesglosePrecio'
import { enPesos, fechaCorta, fechaDesdeHoy, masDias, noches } from '../formato'

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
          setCarga({ estado: 'error', mensaje: error instanceof ErrorApi ? error.message : 'Algo salió mal.' })
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
  const entrada = consulta.get('entrada') ?? apartamento.proximaNocheLibre ?? fechaDesdeHoy()
  const estadia: Estadia = { entrada, salida: consulta.get('salida') ?? masDias(entrada, 1) }
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
    <main>
      <Link to="/">← Todos los apartamentos</Link>
      <h1>{apartamento.nombre}</h1>
      <p className="suave">
        Hasta {apartamento.capacidad} {apartamento.capacidad === 1 ? 'persona' : 'personas'} ·{' '}
        {apartamento.dormitorios} {apartamento.dormitorios === 1 ? 'dormitorio' : 'dormitorios'}
      </p>
      {fotos.length > 0 && (
        <div className="galeria">
          {fotos.map((f, i) => (
            <img key={f.url} src={f.url} alt={`Foto ${i + 1} de ${apartamento.nombre}`}
              loading={i === 0 ? 'eager' : 'lazy'} />
          ))}
        </div>
      )}
      {apartamento.descripcion && <p>{apartamento.descripcion}</p>}
      {apartamento.caracteristicas.length > 0 && (
        <ul className="dotacion" aria-label="Dotación">
          {apartamento.caracteristicas.map((c) => (
            <li key={c}>{c}</li>
          ))}
        </ul>
      )}

      <h2>Tus fechas</h2>
      <div className="busqueda">
        <CamposEstancia estadia={estadia} grupo={grupo} umbral={umbral} alCambiar={cambiar} />
      </div>
      <PrecioDeLaEstancia apartamento={apartamento} estadia={estadia} grupo={grupo} alCambiar={cambiar} />
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
}

/** Se recalcula solo cada vez que cambian las fechas o el grupo. */
function PrecioDeLaEstancia({ apartamento, estadia, grupo, alCambiar }: PropsPrecio) {
  const [calculo, setCalculo] = useState<Calculo | null>(null)
  const { codigo, capacidad } = apartamento
  const { entrada, salida } = estadia
  const { adultos, ninos } = grupo
  const clave = `${codigo}|${entrada}|${salida}|${adultos}|${ninos}`
  const antesDeLlamar = problemaDeEstadia(estadia)
    ?? (adultos + ninos > capacidad ? `Este apartamento admite hasta ${capacidad} personas.` : null)

  useEffect(() => {
    if (antesDeLlamar) return
    let vigente = true // si cambian las fechas antes de que responda, se ignora la respuesta vieja
    consultarEstancia(codigo, { entrada, salida }, { adultos, ninos })
      .then((estancia) => vigente && setCalculo({ estado: 'listo', clave, estancia }))
      .catch((error: unknown) => vigente && setCalculo({
        estado: 'problema', clave,
        mensaje: error instanceof ErrorApi ? error.message : 'No pudimos calcular el precio.',
      }))
    return () => {
      vigente = false
    }
  }, [codigo, entrada, salida, adultos, ninos, clave, antesDeLlamar])

  if (antesDeLlamar) {
    return <p role="alert" className="aviso">{antesDeLlamar}</p>
  }
  if (calculo === null || calculo.clave !== clave) {
    return <p className="suave" aria-live="polite">Calculando el precio…</p>
  }
  if (calculo.estado === 'problema') {
    return <p role="alert" className="aviso">{calculo.mensaje}</p>
  }

  const { disponible, precio, proximaNocheLibre } = calculo.estancia
  if (!disponible) {
    const n = noches(estadia.entrada, estadia.salida)
    return (
      <div className="precio" aria-live="polite">
        <p className="aviso">No está libre en esas fechas.</p>
        {proximaNocheLibre ? (
          <button type="button" className="secundario"
            onClick={() => alCambiar({ entrada: proximaNocheLibre, salida: masDias(proximaNocheLibre, n) }, grupo)}>
            Ver desde el {fechaCorta(proximaNocheLibre)}
          </button>
        ) : (
          <p className="suave">No tiene noches libres en el próximo año.</p>
        )}
      </div>
    )
  }
  const n = precio.desglose.length
  return (
    <div className="precio" aria-live="polite">
      <p className="por-noche">
        <strong>{enPesos(precio.desglose[0].tarifa)}</strong> por adulto, noche del {fechaCorta(estadia.entrada)}
      </p>
      <p>
        Total {n} {n === 1 ? 'noche' : 'noches'}: <strong>{enPesos(precio.total)}</strong>
      </p>
      <DesglosePrecio precio={precio} />
    </div>
  )
}
