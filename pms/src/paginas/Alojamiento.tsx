import { useCallback, useEffect, useMemo, useState, type KeyboardEvent, type ReactNode } from 'react'
import { avisarNombre, consultarAlojamiento, type Alojamiento as DatosAlojamiento } from '../api/alojamiento'
import { ErrorApi } from '../api/cliente'
import BloqueMarca from '../alojamiento/BloqueMarca'
import BloqueMediosPago from '../alojamiento/BloqueMediosPago'
import BloqueReglas from '../alojamiento/BloqueReglas'
import BloqueServicios from '../alojamiento/BloqueServicios'
import { BloqueDatos, BloqueUbicacion } from '../alojamiento/BloqueDatos'
import type { AlBloque } from '../alojamiento/useBloque'
import Dialogo from '../componentes/Dialogo'
import { useTitulo } from '../componentes/useTitulo'
import { useAvisoAlSalir } from '../reserva/nueva/useAvisoAlSalir'
import SinPermiso from './SinPermiso'

type Pestana = 'datos' | 'reglas' | 'servicios' | 'medios' | 'marca'

const PESTANAS: readonly { clave: Pestana; titulo: string; resumen: string }[] = [
  { clave: 'datos', titulo: 'Datos', resumen: 'Nombre, dirección, normas y ubicación' },
  { clave: 'reglas', titulo: 'Reglas de la estancia', resumen: 'Horarios, anticipo, parqueadero y mascotas' },
  { clave: 'servicios', titulo: 'Servicios adicionales', resumen: 'Lo que se carga como consumo' },
  { clave: 'medios', titulo: 'Medios de pago', resumen: 'Con qué se registran los pagos' },
  { clave: 'marca', titulo: 'Marca', resumen: 'Cómo se ve la web de reservas' },
]

// El mismo ancho desde el que el menú queda fijo (src/index.css): por debajo, una lista que abre cada bloque
const CONSULTA_ANCHA = '(min-width: 800px)'

function useAncho(): boolean {
  const consulta = typeof window.matchMedia === 'function' ? window.matchMedia(CONSULTA_ANCHA) : null
  const [ancho, setAncho] = useState(consulta?.matches ?? true)
  useEffect(() => {
    if (!consulta) return
    const alCambiar = () => setAncho(consulta.matches)
    consulta.addEventListener('change', alCambiar)
    return () => consulta.removeEventListener('change', alCambiar)
  }, [consulta])
  return ancho
}

/**
 * Configuración del alojamiento (CU-50 · CU-51 · CU-53, SERV-01, BL-05), solo para el administrador. Cada bloque se
 * guarda por separado; si se cambia de bloque o se sale con cambios sin guardar, se pregunta.
 */
export default function Alojamiento() {
  const [alojamiento, setAlojamiento] = useState<DatosAlojamiento | null>(null)
  const [errorCarga, setErrorCarga] = useState<unknown>(null)
  const [intento, setIntento] = useState(0)
  const ancho = useAncho()
  const [elegida, setElegida] = useState<Pestana | null>(null)
  const [sucios, setSucios] = useState<Partial<Record<string, boolean>>>({})
  const [pendiente, setPendiente] = useState<Pestana | 'lista' | null>(null)
  useTitulo('Alojamiento')

  useEffect(() => {
    let vigente = true
    consultarAlojamiento()
      .then((datos) => {
        if (vigente) {
          setAlojamiento(datos)
          setErrorCarga(null)
        }
      })
      .catch((causa: unknown) => {
        if (vigente) setErrorCarga(causa)
      })
    return () => {
      vigente = false
    }
  }, [intento])

  const hayCambios = Object.values(sucios).some(Boolean)
  const salida = useAvisoAlSalir(hayCambios)
  // En el computador siempre hay una pestaña abierta; en el celular, la lista hasta que se elija un bloque
  const abierta: Pestana | null = elegida ?? (ancho ? 'datos' : null)

  const alGuardar = useCallback((nuevo: DatosAlojamiento) => {
    setAlojamiento((anterior) => {
      if (anterior && anterior.nombre !== nuevo.nombre) avisarNombre(nuevo.nombre)
      return nuevo
    })
  }, [])

  // Cada bloque avisa con su propia función, estable, para no volver a avisar en cada pintada
  const avisos = useMemo(() => Object.fromEntries(
    ['datos', 'ubicacion', 'reglas', 'servicios', 'medios', 'marca'].map((clave) => [
      clave,
      (sucio: boolean) => setSucios((actuales) => (actuales[clave] === sucio ? actuales : { ...actuales, [clave]: sucio })),
    ]),
  ) as Record<string, (sucio: boolean) => void>, [])

  function ir(destino: Pestana | 'lista') {
    if (destino === abierta) return
    if (hayCambios) {
      setPendiente(destino)
      return
    }
    setElegida(destino === 'lista' ? null : destino)
  }

  // Pestañas como las del sistema: un solo punto de tabulación y las flechas, Inicio y Fin para moverse entre ellas
  function moverConFlechas(evento: KeyboardEvent<HTMLButtonElement>) {
    const actual = PESTANAS.findIndex((p) => p.clave === abierta)
    const destinos: Record<string, number> = {
      ArrowRight: (actual + 1) % PESTANAS.length,
      ArrowLeft: (actual - 1 + PESTANAS.length) % PESTANAS.length,
      Home: 0,
      End: PESTANAS.length - 1,
    }
    const destino = destinos[evento.key]
    if (destino === undefined) return
    evento.preventDefault()
    ir(PESTANAS[destino].clave)
    document.getElementById(`pestana-${PESTANAS[destino].clave}`)?.focus()
  }

  if (errorCarga instanceof ErrorApi && errorCarga.estado === 403) return <SinPermiso />

  if (errorCarga) {
    return (
      <section className="alojamiento-pagina">
        <h1 className="titulo-pagina">Alojamiento</h1>
        <div className="tarjeta bloque-vacio" role="alert">
          <h2>No se pudo cargar el alojamiento</h2>
          <p className="suave">Revisa la conexión e intenta de nuevo.</p>
          <button type="button" className="boton" onClick={() => setIntento(intento + 1)}>Reintentar</button>
        </div>
      </section>
    )
  }

  if (!alojamiento) {
    return (
      <section className="alojamiento-pagina" aria-busy="true">
        <p className="rotulo-pagina">Alojamiento</p>
        <span className="fantasma fantasma-titulo" />
        <div className="tarjeta alojamiento-fantasma">
          <span className="fantasma" />
          <span className="fantasma" />
          <span className="fantasma" />
        </div>
        <p className="suave carga-texto" role="status">Cargando el alojamiento…</p>
      </section>
    )
  }

  const props = (clave: string): AlBloque => ({ alojamiento, alGuardar, alCambiarSucio: avisos[clave] })
  const BLOQUES: Record<Pestana, ReactNode> = {
    datos: (
      <>
        <BloqueDatos {...props('datos')} />
        <BloqueUbicacion {...props('ubicacion')} />
      </>
    ),
    reglas: <BloqueReglas {...props('reglas')} />,
    servicios: <BloqueServicios {...props('servicios')} />,
    medios: <BloqueMediosPago {...props('medios')} />,
    marca: <BloqueMarca {...props('marca')} />,
  }
  const lugar = [alojamiento.direccion, alojamiento.ciudad].filter(Boolean).join(' · ')
  const titulo = PESTANAS.find((p) => p.clave === abierta)?.titulo

  return (
    <section className="alojamiento-pagina">
      <header className="alojamiento-cabecera">
        <p className="rotulo-pagina">Alojamiento</p>
        <h1 className="titulo-pagina">{alojamiento.nombre}</h1>
        {lugar && <p className="suave">{lugar}</p>}
      </header>

      {ancho ? (
        <div className="alojamiento-pestanas" role="tablist" aria-label="Bloques del alojamiento">
          {PESTANAS.map((pestana) => (
            <button key={pestana.clave} type="button" role="tab" id={`pestana-${pestana.clave}`}
              aria-selected={abierta === pestana.clave} aria-controls="panel-alojamiento"
              tabIndex={abierta === pestana.clave ? 0 : -1} onClick={() => ir(pestana.clave)} onKeyDown={moverConFlechas}>
              {pestana.titulo}
            </button>
          ))}
        </div>
      ) : abierta === null ? (
        <nav aria-label="Bloques del alojamiento">
          <ul className="alojamiento-lista">
            {PESTANAS.map((pestana) => (
              <li key={pestana.clave}>
                <button type="button" onClick={() => ir(pestana.clave)}>
                  <span className="alojamiento-lista-titulo">{pestana.titulo}</span>
                  <span className="suave">{pestana.resumen}</span>
                  <span className="alojamiento-lista-flecha" aria-hidden="true">›</span>
                </button>
              </li>
            ))}
          </ul>
        </nav>
      ) : (
        <button type="button" className="enlace alojamiento-volver" onClick={() => ir('lista')}>
          <span aria-hidden="true">←</span> Todos los bloques
        </button>
      )}

      {abierta && (
        <div id="panel-alojamiento" className="alojamiento-panel" role={ancho ? 'tabpanel' : 'region'}
          aria-labelledby={ancho ? `pestana-${abierta}` : undefined} aria-label={ancho ? undefined : titulo}>
          {BLOQUES[abierta]}
        </div>
      )}

      {(pendiente || salida.destino) && (
        <Dialogo titulo="¿Salir sin guardar?" alCerrar={() => { setPendiente(null); salida.quedarse() }}>
          <div className="dialogo-contenido">
            <div className="dialogo-cuerpo">
              <p className="dialogo-texto">
                Hay cambios sin guardar en <strong>{titulo}</strong>. Si sales ahora, se pierden.
              </p>
            </div>
            <div className="dialogo-pie">
              <button type="button" className="boton boton-secundario" data-foco-inicial
                onClick={() => { setPendiente(null); salida.quedarse() }}>
                Seguir editando
              </button>
              <button type="button" className="boton boton-peligro" onClick={() => {
                if (salida.destino) {
                  setSucios({})
                  salida.salir()
                } else if (pendiente) {
                  setSucios({})
                  setElegida(pendiente === 'lista' ? null : pendiente)
                  setPendiente(null)
                }
              }}>
                Salir sin guardar
              </button>
            </div>
          </div>
        </Dialogo>
      )}
    </section>
  )
}
