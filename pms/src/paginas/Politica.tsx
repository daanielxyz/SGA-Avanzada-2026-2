import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { consultarPoliticaVigente, listarPoliticas, type PaginaPoliticas, type Politica as DatosPolitica } from '../api/politicas'
import Aviso from '../componentes/Aviso'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import PoliticaContada from '../politica/PoliticaContada'
import SinPermiso from './SinPermiso'

type Carga = { vigente: DatosPolitica | null; pagina: PaginaPoliticas }

/** Si el alojamiento todavía no tiene política, la API responde 404: no es un error de la pantalla. */
function vigenteOVacia(): Promise<DatosPolitica | null> {
  return consultarPoliticaVigente().catch((causa: unknown) => {
    if (causa instanceof ErrorApi && causa.estado === 404) return null
    throw causa
  })
}

/**
 * Política de cancelación (CU-35 · POL-04), solo para el administrador. No se edita: cada versión nueva rige para las
 * reservas que se creen desde que se publica, y cada reserva guarda la suya.
 */
export default function Politica() {
  useTitulo('Política de cancelación')
  const { state } = useLocation() as { state: { publicada?: number } | null }
  const [pagina, setPagina] = useState(0)
  const [intento, setIntento] = useState(0)
  const [carga, setCarga] = useState<Carga | null>(null)
  const [error, setError] = useState<unknown>(null)

  useEffect(() => {
    let vigente = true
    Promise.all([vigenteOVacia(), listarPoliticas(pagina)])
      .then(([politica, lista]) => {
        if (!vigente) return
        setCarga({ vigente: politica, pagina: lista })
        setError(null)
      })
      .catch((causa: unknown) => {
        if (vigente) setError(causa)
      })
    return () => {
      vigente = false
    }
  }, [pagina, intento])

  if (error instanceof ErrorApi && error.estado === 403) return <SinPermiso />

  const encabezado = (
    <header className="politica-cabecera">
      <div>
        <h1>Política de cancelación</h1>
        <p className="suave">
          La política no se edita: cada versión nueva rige para las reservas que se creen desde que se publica. Cada
          reserva guarda la versión con la que se hizo.
        </p>
      </div>
      {carga?.vigente && <Link to="/politica/publicar" className="boton">Publicar nueva versión</Link>}
    </header>
  )

  if (error) {
    return (
      <section className="politica-pagina">
        {encabezado}
        <div className="tarjeta bloque-vacio" role="alert">
          <h2>No se pudo cargar la política</h2>
          <p className="suave">Revisa la conexión e intenta de nuevo.</p>
          <button type="button" className="boton" onClick={() => setIntento(intento + 1)}>Reintentar</button>
        </div>
      </section>
    )
  }

  if (!carga) {
    return (
      <section className="politica-pagina" aria-busy="true">
        {encabezado}
        <div className="tarjeta alojamiento-fantasma">
          <span className="fantasma fantasma-titulo" />
          <span className="fantasma" />
          <span className="fantasma" />
          <span className="fantasma" />
        </div>
        <p className="suave carga-texto" role="status">Cargando la política…</p>
      </section>
    )
  }

  if (!carga.vigente) {
    return (
      <section className="politica-pagina">
        {encabezado}
        <div className="tarjeta bloque-vacio">
          <h2>Todavía no hay política de cancelación</h2>
          <p className="suave">
            Sin una política publicada no se puede reservar: cada reserva guarda la versión con la que se hizo.
          </p>
          <Link to="/politica/publicar" className="boton">Publicar la primera versión</Link>
        </div>
      </section>
    )
  }

  const { vigente, pagina: lista } = carga
  const anteriores = lista.contenido.filter((politica) => politica.id !== vigente.id)
  const totalAnteriores = Math.max(lista.totalElementos - 1, 0)

  return (
    <section className="politica-pagina">
      {encabezado}
      {state?.publicada === vigente.version && (
        <Aviso tipo="exito" asunto={`Versión ${vigente.version} publicada`}
          mensaje="Rige para las reservas nuevas. Las reservas ya hechas conservan su versión." />
      )}

      <article className="tarjeta politica-vigente" aria-labelledby="politica-vigente">
        <h2 id="politica-vigente" className="politica-version">
          Versión {vigente.version}
          <span className="etiqueta etiqueta-activo"><span aria-hidden="true">●</span> Vigente</span>
        </h2>
        <PoliticaContada politica={vigente} />
      </article>

      {totalAnteriores > 0 && (
        <section className="politica-anteriores" aria-labelledby="politica-anteriores">
          <header>
            <h2 id="politica-anteriores">Versiones anteriores</h2>
            <p className="suave">Siguen aplicando a las reservas que se hicieron con ellas.</p>
          </header>
          <ul className="versiones">
            {anteriores.map((politica) => (
              <li key={politica.id}>
                <details className="avanzado version">
                  <summary><span className="avanzado-titulo">Versión {politica.version}</span></summary>
                  <div className="version-cuerpo">
                    <PoliticaContada politica={politica} nivel={4} />
                  </div>
                </details>
              </li>
            ))}
          </ul>
          {lista.totalPaginas > 1 && (
            <Paginador numero={lista.numero} totalPaginas={lista.totalPaginas} totalElementos={lista.totalElementos}
              unidad={['versión', 'versiones']} alCambiar={setPagina} />
          )}
        </section>
      )}
    </section>
  )
}
