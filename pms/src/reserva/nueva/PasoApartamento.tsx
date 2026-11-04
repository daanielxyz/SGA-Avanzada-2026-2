import { useEffect, useState } from 'react'
import { buscarDisponibles, type ApartamentoEncontrado } from '../../api/apartamentos'
import { ErrorApi } from '../../api/cliente'
import type { Pagina } from '../../api/reservas'
import Aviso from '../../componentes/Aviso'
import BloqueVacio from '../../componentes/BloqueVacio'
import Paginador from '../../componentes/Paginador'
import { cantidad, dinero, fechaCorta } from '../../formato'
import type { DatosEstancia } from './formulario'

type Resultado = { clave: string; pagina: Pagina<ApartamentoEncontrado> } | { clave: string; error: unknown }

/**
 * Paso 2: los apartamentos disponibles (RN-01 · RN-02 · RN-07). Elegir uno avanza de una vez: con diez tarjetas, un
 * botón «Siguiente» quedaría muy lejos.
 *
 * @param fechasNacimiento con ellas el valor ya cuenta solo a los que pagan; sin ellas es un estimado
 * @param otroApartamento  el nombre del que se perdió, cuando se llega desde un rechazo al crear
 */
export default function PasoApartamento({ estancia, fechasNacimiento, elegido, otroApartamento, alElegir, alCambiarFechas, alVolver }: {
  estancia: DatosEstancia
  fechasNacimiento: string[]
  elegido: string | null
  otroApartamento: boolean
  alElegir: (apartamento: ApartamentoEncontrado) => void
  alCambiarFechas: () => void
  alVolver: () => void
}) {
  const [numero, setNumero] = useState(0)
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const clave = `${numero}#${intento}`

  useEffect(() => {
    let vigente = true
    buscarDisponibles({ ...estancia, ocupantes: Number(estancia.ocupantes), fechasNacimiento, pagina: numero })
      .then((pagina) => {
        if (vigente) setResultado({ clave, pagina })
      })
      .catch((error: unknown) => {
        if (vigente) setResultado({ clave, error })
      })
    return () => {
      vigente = false
    }
  }, [clave, estancia, fechasNacimiento, numero])

  const actual = resultado?.clave === clave ? resultado : null
  const resumen = `${fechaCorta(estancia.entrada)} al ${fechaCorta(estancia.salida)}`

  return (
    <div className="paso">
      {otroApartamento && (
        <Aviso
          tipo="advertencia"
          asunto="Elige otro apartamento"
          mensaje="Conservamos las fechas, las personas, la hora de llegada, los vehículos y las mascotas."
        />
      )}
      {fechasNacimiento.length === 0 && (
        <Aviso
          tipo="info"
          asunto="Valor estimado"
          mensaje="El valor definitivo se calcula en el resumen, cuando se conocen las edades de los ocupantes."
        />
      )}
      <h2 tabIndex={-1}>Apartamentos disponibles</h2>

      {!actual && (
        <>
          <p className="suave" role="status">Buscando apartamentos disponibles…</p>
          <div className="apartamentos" aria-busy="true">
            {Array.from({ length: 3 }, (_, indice) => (
              <div key={indice} className="tarjeta apartamento-fantasma">
                <span className="fantasma apartamento-foto" />
                <span className="fantasma" />
                <span className="fantasma" />
              </div>
            ))}
          </div>
        </>
      )}

      {actual && 'error' in actual && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar los apartamentos"
          mensaje={actual.error instanceof ErrorApi ? actual.error.message : 'Inténtalo de nuevo en un momento.'}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      )}

      {actual && 'pagina' in actual && actual.pagina.contenido.length === 0 && (
        <BloqueVacio
          icono="apartamentos"
          titulo="No hay apartamentos disponibles"
          accion={
            <button type="button" className="boton" onClick={alCambiarFechas}>
              Cambiar fechas
            </button>
          }
        >
          Para {cantidad(Number(estancia.ocupantes), 'ocupante', 'ocupantes')} del {resumen} no queda ninguno libre.
        </BloqueVacio>
      )}

      {actual && 'pagina' in actual && actual.pagina.contenido.length > 0 && (
        <>
          <ul className="apartamentos">
            {actual.pagina.contenido.map((apartamento) => (
              <li key={apartamento.codigo}>
                <Tarjeta apartamento={apartamento} elegido={apartamento.codigo === elegido} alElegir={alElegir} />
              </li>
            ))}
          </ul>
          <Paginador
            numero={actual.pagina.numero}
            totalPaginas={actual.pagina.totalPaginas}
            totalElementos={actual.pagina.totalElementos}
            unidad={['apartamento', 'apartamentos']}
            alCambiar={setNumero}
          />
        </>
      )}

      <div className="paso-pie">
        <button type="button" className="boton boton-secundario" onClick={alVolver}>
          <span aria-hidden="true">←</span> Atrás
        </button>
      </div>
    </div>
  )
}

function Tarjeta({ apartamento, elegido, alElegir }: {
  apartamento: ApartamentoEncontrado
  elegido: boolean
  alElegir: (apartamento: ApartamentoEncontrado) => void
}) {
  const [fotoFallo, setFotoFallo] = useState(false)
  const { codigo, nombre, capacidad, dormitorios, imagenPrincipal, caracteristicas, valorEstimado } = apartamento
  return (
    <article className={`tarjeta apartamento${elegido ? ' apartamento-elegido' : ''}`}>
      {imagenPrincipal && !fotoFallo ? (
        <img className="apartamento-foto" src={imagenPrincipal} alt="" loading="lazy" onError={() => setFotoFallo(true)} />
      ) : (
        <div className="apartamento-foto apartamento-sin-foto">Sin foto</div>
      )}
      <div className="apartamento-cuerpo">
        <div className="apartamento-encabezado">
          <h3>{nombre}</h3>
          <span className="codigo">{codigo}</span>
        </div>
        <p className="apartamento-datos">
          Hasta {cantidad(capacidad, 'persona', 'personas')} · {cantidad(dormitorios, 'dormitorio', 'dormitorios')}
        </p>
        {caracteristicas.length > 0 && (
          <ul className="apartamento-caracteristicas">
            {caracteristicas.map((caracteristica) => (
              <li key={caracteristica}>{caracteristica}</li>
            ))}
          </ul>
        )}
        <div className="apartamento-valor">
          <span>Valor estimado</span>
          <strong className="codigo">{dinero(valorEstimado)}</strong>
        </div>
        <button
          type="button"
          className={`boton boton-bloque${elegido ? '' : ' boton-secundario'}`}
          aria-label={`${elegido ? 'Continuar con' : 'Elegir'} ${nombre}`}
          onClick={() => alElegir(apartamento)}
        >
          {elegido ? <><span aria-hidden="true">✓</span> Seleccionado: continuar</> : 'Elegir este apartamento'}
        </button>
      </div>
    </article>
  )
}
