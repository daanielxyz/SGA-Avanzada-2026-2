import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router'
import { listarApartamentos, type FichaApartamento } from '../api/apartamentos'
import type { Pagina } from '../api/reservas'
import DialogoDatos from '../apartamentos/DialogoDatos'
import { dormitorios, fotoPrincipal, personas } from '../apartamentos/formulario'
import { EtiquetaVenta, Foto } from '../apartamentos/Piezas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import EstadoApartamento from '../componentes/EstadoApartamento'
import Icono from '../componentes/Icono'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { errorParaMostrar } from '../reserva/mensajes'

type Resultado = { clave: string; pagina: Pagina<FichaApartamento> } | { clave: string; error: unknown }

/**
 * Apartamentos del alojamiento, de 10 en 10, a la venta y retirados (sprint 4). Sin buscador ni filtros: la API no los
 * tiene. Desde aquí se crea uno nuevo; lo demás se cambia en su detalle.
 */
export default function Apartamentos() {
  const [parametros, setParametros] = useSearchParams()
  const { search } = useLocation()
  const navegar = useNavigate()
  const numero = Math.max(0, Number(parametros.get('pagina')) || 0)
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [creando, setCreando] = useState(false)
  const clave = `${numero}#${intento}`
  useTitulo('Apartamentos')

  useEffect(() => {
    let vigente = true
    listarApartamentos(numero)
      .then((pagina) => vigente && setResultado({ clave, pagina }))
      .catch((error: unknown) => vigente && setResultado({ clave, error }))
    return () => {
      vigente = false
    }
  }, [numero, clave])

  const actual = resultado?.clave === clave ? resultado : null
  const pagina = actual && 'pagina' in actual ? actual.pagina : null
  const nuevo = (
    <button type="button" className="boton" onClick={() => setCreando(true)}>
      <span aria-hidden="true">+</span> Nuevo apartamento
    </button>
  )

  return (
    <div className="apartamentos">
      <div className="apartamentos-encabezado">
        <h1>
          Apartamentos
          {pagina && <span className="apartamentos-cantidad"> ({pagina.totalElementos})</span>}
        </h1>
        {nuevo}
      </div>

      {!actual && <Cargando />}
      {actual && 'error' in actual && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar los apartamentos"
          mensaje={errorParaMostrar(actual.error).mensaje}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      )}
      {pagina && pagina.contenido.length === 0 && (
        numero > 0 ? (
          <BloqueVacio
            icono="apartamentos"
            titulo="Esta página ya no tiene apartamentos"
            accion={
              <button type="button" className="boton boton-secundario" onClick={() => setParametros({})}>
                Ir a la primera página
              </button>
            }
          >
            La lista es más corta de lo que era.
          </BloqueVacio>
        ) : (
          <BloqueVacio icono="apartamentos" titulo="Todavía no hay apartamentos" accion={nuevo}>
            Crea el primero con sus fotos y características. Nace retirado de la venta.
          </BloqueVacio>
        )
      )}
      {pagina && pagina.contenido.length > 0 && (
        <>
          <section aria-label="Apartamentos" className="tarjeta apartamentos-tabla">
            <div className="desplazable">
              <table className="tabla tabla-apartamentos">
                <caption className="solo-lectores">Apartamentos</caption>
                <thead>
                  <tr>
                    <th scope="col"><span className="solo-lectores">Foto</span></th>
                    <th scope="col">Código</th>
                    <th scope="col">Nombre</th>
                    <th scope="col">Capacidad</th>
                    <th scope="col">Dormitorios</th>
                    <th scope="col">Venta</th>
                    <th scope="col">Estado operativo</th>
                    <th scope="col"><span className="solo-lectores">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  {pagina.contenido.map((apartamento) => (
                    <tr key={apartamento.codigo}>
                      <td><Foto url={fotoPrincipal(apartamento.imagenes)?.url} /></td>
                      <td className="codigo enlace-codigo">{apartamento.codigo}</td>
                      <td className="celda-nombre" title={apartamento.nombre}>{apartamento.nombre}</td>
                      <td>{personas(apartamento)}</td>
                      <td className="numero">{apartamento.dormitorios}</td>
                      <td><EtiquetaVenta activo={apartamento.activo} /></td>
                      <td><EstadoApartamento estado={apartamento.estadoOperativo} etiqueta /></td>
                      <td className="derecha">
                        <Link
                          to={`/apartamentos/${encodeURIComponent(apartamento.codigo)}`}
                          state={{ volverA: search }}
                          className="boton boton-secundario boton-chico"
                          aria-label={`Ver ${apartamento.codigo}`}
                        >
                          Ver
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <ul className="apartamentos-tarjetas" aria-label="Apartamentos">
            {pagina.contenido.map((apartamento) => (
              <li key={apartamento.codigo}>
                <Link
                  to={`/apartamentos/${encodeURIComponent(apartamento.codigo)}`}
                  state={{ volverA: search }}
                  className="apartamento-tarjeta"
                >
                  <Foto url={fotoPrincipal(apartamento.imagenes)?.url} clase="miniatura miniatura-grande" />
                  <span className="apartamento-tarjeta-datos">
                    <span className="codigo apartamento-tarjeta-codigo">{apartamento.codigo}</span>
                    <strong className="apartamento-tarjeta-nombre">{apartamento.nombre}</strong>
                    <span className="suave">{personas(apartamento)} · {dormitorios(apartamento)}</span>
                    <span className="apartamento-tarjeta-estados">
                      <EtiquetaVenta activo={apartamento.activo} />
                      <EstadoApartamento estado={apartamento.estadoOperativo} etiqueta />
                    </span>
                  </span>
                  <Icono nombre="abajo" />
                </Link>
              </li>
            ))}
          </ul>

          <Paginador
            numero={pagina.numero}
            totalPaginas={pagina.totalPaginas}
            totalElementos={pagina.totalElementos}
            unidad={['apartamento', 'apartamentos']}
            alCambiar={(n) => setParametros({ pagina: String(n) })}
          />
        </>
      )}

      {creando && (
        <DialogoDatos
          alCerrar={() => setCreando(false)}
          alGuardar={(creado) => navegar(`/apartamentos/${encodeURIComponent(creado.codigo)}`, {
            state: { creado: true, volverA: search },
          })}
        />
      )}
    </div>
  )
}

function Cargando() {
  return (
    <div className="tarjeta" aria-busy="true">
      {Array.from({ length: 4 }, (_, fila) => (
        <div key={fila} className="fila-fantasma">
          {Array.from({ length: 6 }, (__, celda) => (
            <span key={celda} className="fantasma" />
          ))}
        </div>
      ))}
      <p className="carga-texto suave" role="status">
        Cargando apartamentos…
      </p>
    </div>
  )
}
