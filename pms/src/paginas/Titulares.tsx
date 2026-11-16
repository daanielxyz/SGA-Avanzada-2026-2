import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import type { Pagina } from '../api/reservas'
import { buscarTitulares, type Titular } from '../api/titulares'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { errorParaMostrar } from '../reserva/mensajes'
import EstadoIdentidad from '../titulares/EstadoIdentidad'
import { Correo } from '../usuarios/EstadoCuenta'
import { MAXIMO_BUSQUEDA, textoDocumento } from '../titulares/logica'

type Resultado = { clave: string; pagina: Pagina<Titular> } | { clave: string; error: unknown }

/**
 * Titulares (DEC-131): recepción los busca por nombre o documento y abre su detalle. La búsqueda y la página viven
 * en la dirección, así «← Titulares» vuelve al mismo lugar.
 */
export default function Titulares() {
  const [parametros, setParametros] = useSearchParams()
  const { search } = useLocation()
  const buscado = parametros.get('buscar') ?? ''
  const numero = Math.max(0, Number(parametros.get('pagina')) || 0)
  const [texto, setTexto] = useState(buscado)
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const clave = `${buscado}#${numero}#${intento}`
  useTitulo('Titulares')

  useEffect(() => {
    let vigente = true
    buscarTitulares(buscado, numero)
      .then((pagina) => vigente && setResultado({ clave, pagina }))
      .catch((error: unknown) => vigente && setResultado({ clave, error }))
    return () => {
      vigente = false
    }
  }, [buscado, numero, clave])

  const actual = resultado?.clave === clave ? resultado : null
  const pagina = actual && 'pagina' in actual ? actual.pagina : null

  function buscar(evento: FormEvent) {
    evento.preventDefault()
    setParametros(texto.trim() ? { buscar: texto.trim() } : {})
  }
  function limpiar() {
    setTexto('')
    setParametros({})
  }

  const enlace = (titular: Titular, contenido: ReactNode, clase: string) => (
    <Link to={`/titulares/${encodeURIComponent(titular.id)}`} state={{ volverA: search }} className={clase}>
      {contenido}
    </Link>
  )

  return (
    <div className="titulares">
      <h1>Titulares</h1>

      <form className="tarjeta buscador" role="search" aria-label="Buscar titulares" onSubmit={buscar}>
        <label className="campo-rotulo" htmlFor="buscar-titular">Busca por nombre o documento</label>
        <div className="buscador-fila">
          <input id="buscar-titular" className="campo-control" type="search" autoComplete="off" spellCheck={false}
            maxLength={MAXIMO_BUSQUEDA} value={texto} onChange={(e) => setTexto(e.target.value)} />
          <button type="submit" className="boton">Buscar</button>
          {buscado && <button type="button" className="boton boton-secundario" onClick={limpiar}>Limpiar</button>}
        </div>
      </form>

      {!actual && <Cargando />}
      {actual && 'error' in actual && (
        <Aviso tipo="error" asunto="No se pudieron cargar los titulares" mensaje={errorParaMostrar(actual.error).mensaje}
          accion={<button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>Reintentar</button>} />
      )}

      {pagina && pagina.contenido.length === 0 && (
        buscado ? (
          <BloqueVacio icono="buscar" titulo={`Ningún titular coincide con «${buscado}»`}
            accion={<button type="button" className="boton boton-secundario" onClick={limpiar}>Limpiar búsqueda</button>}>
            Prueba con otra parte del nombre o con el número de documento.
          </BloqueVacio>
        ) : pagina.numero > 0 ? (
          <BloqueVacio icono="titulares" titulo="Esta página ya no tiene titulares"
            accion={<button type="button" className="boton boton-secundario" onClick={() => setParametros({})}>Ir a la primera página</button>}>
            La lista es más corta de lo que era.
          </BloqueVacio>
        ) : (
          <BloqueVacio icono="titulares" titulo="Todavía no hay titulares">
            Los titulares aparecen cuando se hace su primera reserva.
          </BloqueVacio>
        )
      )}

      {pagina && pagina.contenido.length > 0 && (
        <>
          <div className="tarjeta desplazable tabla-titulares-marco">
            <table className="tabla tabla-titulares">
              <caption className="solo-lectores">Titulares{buscado ? ` que coinciden con «${buscado}»` : ''}</caption>
              <thead>
                <tr>
                  <th scope="col">Nombre</th>
                  <th scope="col">Documento</th>
                  <th scope="col">Contacto</th>
                  <th scope="col">Identidad</th>
                </tr>
              </thead>
              <tbody>
                {pagina.contenido.map((titular) => (
                  <tr key={titular.id} className="fila-enlace">
                    <td className="texto-largo">{enlace(titular, titular.nombre, 'fila-enlace-nombre')}</td>
                    <td className="codigo sin-cortar">{textoDocumento(titular.tipoDocumento, titular.numeroDocumento)}</td>
                    <td><Contacto titular={titular} /></td>
                    <td>
                      <span className="fila-enlace-final">
                        <EstadoIdentidad validado={titular.validado} corto />
                        <span className="fila-enlace-flecha" aria-hidden="true">›</span>
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="titulares-tarjetas" aria-label="Titulares">
            {pagina.contenido.map((titular) => (
              <li key={titular.id}>
                {enlace(titular, (
                  <>
                    <span className="titular-tarjeta-fila">
                      <strong className="texto-largo">{titular.nombre}</strong>
                      <EstadoIdentidad validado={titular.validado} corto />
                    </span>
                    <span className="codigo">{textoDocumento(titular.tipoDocumento, titular.numeroDocumento)}</span>
                    <Contacto titular={titular} />
                  </>
                ), 'tarjeta titular-tarjeta')}
              </li>
            ))}
          </ul>

          <Paginador numero={pagina.numero} totalPaginas={pagina.totalPaginas} totalElementos={pagina.totalElementos}
            unidad={['titular', 'titulares']}
            alCambiar={(n) => setParametros(buscado ? { buscar: buscado, pagina: String(n) } : { pagina: String(n) })} />
        </>
      )}
    </div>
  )
}

/** El correo y el teléfono, el que haya; lo que falta se dice a los lectores de pantalla. */
function Contacto({ titular }: { titular: Titular }) {
  return (
    <span className="contacto-titular-celda">
      {titular.correo ? <span className="correo"><Correo correo={titular.correo} /></span> : <span className="solo-lectores">Sin correo</span>}
      {titular.telefono ? <span>{titular.telefono}</span> : <span className="solo-lectores">Sin teléfono</span>}
    </span>
  )
}

function Cargando() {
  return (
    <div className="tarjeta" aria-busy="true">
      {Array.from({ length: 4 }, (_, fila) => (
        <div key={fila} className="fila-fantasma">
          {Array.from({ length: 4 }, (__, celda) => <span key={celda} className="fantasma" />)}
        </div>
      ))}
      <p className="carga-texto suave" role="status">Cargando titulares…</p>
    </div>
  )
}
