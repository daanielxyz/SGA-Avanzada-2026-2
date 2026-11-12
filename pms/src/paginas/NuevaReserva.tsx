import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { consultarAlojamiento, type Alojamiento } from '../api/alojamiento'
import type { ApartamentoEncontrado } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import type { Reserva } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import { useTitulo } from '../componentes/useTitulo'
import { cantidad, fechaCorta, noches } from '../formato'
import {
  ajustarGrupo,
  ESTANCIA_VACIA,
  mensajeDeCreada,
  OCUPANTE_VACIO,
  ocupanteDelTitular,
  TITULAR_VACIO,
  type DatosEstancia,
  type DatosOcupante,
  type DatosTitular,
} from '../reserva/nueva/formulario'
import PasoApartamento from '../reserva/nueva/PasoApartamento'
import PasoEstancia from '../reserva/nueva/PasoEstancia'
import PasoPersonas from '../reserva/nueva/PasoPersonas'
import PasoResumen, { type DatosLlegada } from '../reserva/nueva/PasoResumen'
import { useAvisoAlSalir } from '../reserva/nueva/useAvisoAlSalir'

const PASOS = ['Estancia', 'Apartamento', 'Personas', 'Resumen'] as const

/**
 * Crear una reserva directa en recepción (CU-11). Lo escrito vive aquí, así que moverse entre pasos no lo pierde. El
 * paso va en la dirección para que «Atrás» del navegador cambie de paso en vez de salir del formulario; lo escrito no
 * se guarda en el navegador porque trae datos personales, así que al recargar se empieza de nuevo, con aviso.
 */
export default function NuevaReserva() {
  useTitulo('Nueva reserva')
  const navegar = useNavigate()
  const [parametros, setParametros] = useSearchParams()
  const [alojamiento, setAlojamiento] = useState<Alojamiento | null>(null)
  const [errorAlojamiento, setErrorAlojamiento] = useState<unknown>(null)
  const [intento, setIntento] = useState(0)

  const [estancia, setEstancia] = useState<DatosEstancia>(ESTANCIA_VACIA)
  const [estanciaLista, setEstanciaLista] = useState<DatosEstancia | null>(null)
  const [apartamento, setApartamento] = useState<ApartamentoEncontrado | null>(null)
  const [titular, setTitular] = useState<DatosTitular>(TITULAR_VACIO)
  const [grupo, setGrupo] = useState<DatosOcupante[]>([OCUPANTE_VACIO])
  const [titularSeAloja, setTitularSeAloja] = useState(true)
  const [personasListas, setPersonasListas] = useState(false)
  const [llegada, setLlegada] = useState<DatosLlegada>({ hora: '', carros: '0', motos: '0', mascotas: '0' })
  const [otroApartamento, setOtroApartamento] = useState(false)
  const [creada, setCreada] = useState(false)
  // Llegar directo a un paso avanzado solo pasa al recargar o con un enlace viejo: lo escrito ya no está
  const [reiniciada] = useState(() => Number(parametros.get('paso')) > 1)

  const contenedor = useRef<HTMLDivElement>(null)
  const primeraVez = useRef(true)

  const maximo = !estanciaLista ? 1 : !apartamento ? 2 : !personasListas ? 3 : 4
  const pedido = Number(parametros.get('paso')) || 1
  const paso = Math.min(Math.max(pedido, 1), maximo)

  const hayAlgoEscrito = Boolean(estancia.entrada || estancia.salida || titular.nombre || estanciaLista)
  useAvisoAlSalir(hayAlgoEscrito && !creada)

  useEffect(() => {
    let vigente = true
    consultarAlojamiento()
      .then((datos) => {
        if (vigente) setAlojamiento(datos)
      })
      .catch((error: unknown) => {
        if (vigente) setErrorAlojamiento(error)
      })
    return () => {
      vigente = false
    }
  }, [intento])

  useEffect(() => {
    if (pedido !== paso) setParametros(paso === 1 ? {} : { paso: String(paso) }, { replace: true })
  }, [pedido, paso, setParametros])

  // Al cambiar de paso, el foco y la vista empiezan en el contenido nuevo
  useEffect(() => {
    if (primeraVez.current) {
      primeraVez.current = false
      return
    }
    contenedor.current?.querySelector<HTMLElement>('h2')?.focus({ preventScroll: true })
    window.scrollTo?.({ top: 0 })
  }, [paso])

  const irA = (destino: number) => setParametros(destino === 1 ? {} : { paso: String(destino) })

  const fechasConocidas = useMemo(
    () => (personasListas ? grupo.map((ocupante) => ocupante.fechaNacimiento) : []),
    [personasListas, grupo],
  )

  function seguirDesdeEstancia() {
    const cambio = !estanciaLista || (['entrada', 'salida', 'ocupantes'] as const).some((campo) => estanciaLista[campo] !== estancia[campo])
    if (cambio) {
      setEstanciaLista({ ...estancia })
      setApartamento(null)
      // La edad que decide quién paga se calcula a la fecha de entrada: hay que volver a revisar el grupo
      setPersonasListas(false)
      setGrupo(ajustarGrupo(grupo, Number(estancia.ocupantes)))
    }
    irA(2)
  }

  function elegirApartamento(elegido: ApartamentoEncontrado) {
    setApartamento(elegido)
    setOtroApartamento(false)
    irA(personasListas ? 4 : 3)
  }

  function cambiarTitular(nuevo: DatosTitular) {
    setTitular(nuevo)
    setPersonasListas(false)
    if (titularSeAloja) setGrupo((actual) => actual.map((o, i) => (i === 0 ? ocupanteDelTitular(nuevo, o) : o)))
  }

  function cambiarOcupante(indice: number, ocupante: DatosOcupante) {
    setPersonasListas(false)
    setGrupo((actual) => actual.map((o, i) => (i === indice ? ocupante : o)))
  }

  function cambiarSeAloja(seAloja: boolean) {
    setTitularSeAloja(seAloja)
    setPersonasListas(false)
    if (seAloja) setGrupo((actual) => actual.map((o, i) => (i === 0 ? ocupanteDelTitular(titular, o) : o)))
  }

  function alCrear(reserva: Reserva, continuarLlegada: boolean) {
    setCreada(true)
    if (continuarLlegada) {
      navegar(`/reservas/${reserva.codigo}/llegada`, {
        replace: true,
        state: {
          aviso: {
            asunto: `Reserva creada · ${reserva.codigo}`,
            mensaje: `Cobra y registra la llegada para terminar.${reserva.avisoCuenta ? ` ${reserva.avisoCuenta}` : ''}`,
          },
        },
      })
      return
    }
    navegar(`/reservas/${reserva.codigo}`, {
      replace: true,
      state: {
        creada: {
          asunto: `Reserva creada · ${reserva.codigo}`,
          mensaje: mensajeDeCreada(Boolean(llegada.hora), Boolean(alojamiento && alojamiento.parametros.anticipoPct > 0)),
          avisoCuenta: reserva.avisoCuenta ?? null,
        },
      },
    })
  }

  if (errorAlojamiento) {
    return (
      <div className="detalle">
        <VolverAReservas />
        <h1>Nueva reserva</h1>
        <Aviso
          tipo="error"
          asunto="No pudimos preparar el formulario"
          mensaje={errorAlojamiento instanceof ErrorApi ? errorAlojamiento.message : 'Inténtalo de nuevo en un momento.'}
          accion={
            <button
              type="button"
              className="boton boton-secundario boton-chico"
              onClick={() => {
                setErrorAlojamiento(null)
                setIntento(intento + 1)
              }}
            >
              Reintentar
            </button>
          }
        />
      </div>
    )
  }

  return (
    <div className="detalle nueva-reserva" ref={contenedor}>
      <div>
        <VolverAReservas />
        <h1>Nueva reserva</h1>
      </div>

      <Indicador paso={paso} />

      {paso > 1 && estanciaLista && (
        <p className="tarjeta paso-contexto">
          {apartamento && paso > 2 && (
            <strong><span className="codigo">{apartamento.codigo}</span> · {apartamento.nombre} · </strong>
          )}
          <strong>{fechaCorta(estanciaLista.entrada)} → {fechaCorta(estanciaLista.salida)}</strong> ·{' '}
          {cantidad(noches(estanciaLista.entrada, estanciaLista.salida), 'noche', 'noches')} ·{' '}
          {cantidad(Number(estanciaLista.ocupantes), 'ocupante', 'ocupantes')}
        </p>
      )}

      {paso === 1 && reiniciada && !hayAlgoEscrito && (
        <Aviso tipo="info" asunto="Empezamos de nuevo" mensaje="Lo escrito no se conserva al recargar la página." />
      )}

      {paso === 1 && <PasoEstancia datos={estancia} alCambiar={setEstancia} alSeguir={seguirDesdeEstancia} />}
      {paso === 2 && estanciaLista && (
        <PasoApartamento
          estancia={estanciaLista}
          fechasNacimiento={fechasConocidas}
          elegido={apartamento?.codigo ?? null}
          otroApartamento={otroApartamento}
          alElegir={elegirApartamento}
          alCambiarFechas={() => irA(1)}
          alVolver={() => irA(1)}
        />
      )}
      {paso === 3 && estanciaLista && alojamiento && (
        <PasoPersonas
          titular={titular}
          grupo={grupo}
          titularSeAloja={titularSeAloja}
          entrada={estanciaLista.entrada}
          alCambiarTitular={cambiarTitular}
          alCambiarOcupante={cambiarOcupante}
          alCambiarSeAloja={cambiarSeAloja}
          alSeguir={() => {
            setPersonasListas(true)
            irA(4)
          }}
          alVolver={() => irA(2)}
        />
      )}
      {paso === 3 && !alojamiento && <p className="suave" role="status">Preparando el formulario…</p>}
      {paso === 4 && estanciaLista && apartamento && alojamiento && (
        <PasoResumen
          estancia={estanciaLista}
          apartamento={apartamento}
          titular={titular}
          grupo={grupo}
          llegada={llegada}
          alojamiento={alojamiento}
          alCambiarLlegada={setLlegada}
          alCrear={alCrear}
          alElegirOtro={() => {
            setApartamento(null)
            setOtroApartamento(true)
            irA(2)
          }}
          alCambiarFechas={() => irA(1)}
          alRevisarPersonas={() => irA(3)}
          alVolver={() => irA(3)}
        />
      )}
    </div>
  )
}

function VolverAReservas() {
  return (
    <Link to="/reservas" className="volver">
      <span aria-hidden="true">←</span> Volver a reservas
    </Link>
  )
}

/** En el computador, los cuatro pasos con su nombre; en el celular, una barra y «Paso 2 de 4». */
function Indicador({ paso }: { paso: number }) {
  return (
    <nav aria-label="Progreso">
      <ol className="pasos">
        {PASOS.map((nombre, indice) => {
          const numero = indice + 1
          const estado = numero < paso ? 'hecho' : numero === paso ? 'actual' : 'pendiente'
          return (
            <li key={nombre} className={`pasos-item pasos-${estado}`} aria-current={numero === paso ? 'step' : undefined}>
              <span className="pasos-numero" aria-hidden="true">{numero < paso ? '✓' : numero}</span>
              <span className="pasos-nombre">{nombre}</span>
            </li>
          )
        })}
      </ol>
      <p className="pasos-texto" aria-hidden="true">
        <strong>Paso {paso} de {PASOS.length}</strong> · {PASOS[paso - 1]}
      </p>
    </nav>
  )
}
