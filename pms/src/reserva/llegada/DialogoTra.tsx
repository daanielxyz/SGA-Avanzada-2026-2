import { useEffect, useRef, useState, type FormEvent } from 'react'
import { completarDatosTra, type DatosTra, type Ocupante, type Reserva } from '../../api/reservas'
import Aviso from '../../componentes/Aviso'
import Dialogo from '../../componentes/Dialogo'
import { fechaCorta } from '../../formato'
import { errorParaMostrar, type ErrorMostrado } from '../mensajes'
import Campo from '../nueva/Campo'
import { mensajeDocumentoPorEdad, tiposPorEdad } from '../nueva/formulario'
import { GENEROS, MOTIVOS_VIAJE, siguienteConFaltantes, TIPOS_DOCUMENTO_OCUPANTE, tieneTraPendiente } from './llegada'

type Valores = Required<{ [K in keyof DatosTra]: string }>
type CampoViaje = 'paisResidencia' | 'ciudadResidencia' | 'ciudadProcedencia' | 'ciudadDestino' | 'motivoViaje'
type Propuesta = 'ciudadResidencia' | 'ciudadDestino'

const VIAJE: readonly CampoViaje[] = ['ciudadProcedencia', 'ciudadResidencia', 'ciudadDestino', 'paisResidencia', 'motivoViaje']
const ELIGE = ['', 'Elige'] as const

function valoresDe(ocupante: Ocupante): Valores {
  // Antes de BL-07 el nombre llegaba entero: se deja en Nombres para que recepción lo separe
  const nombres = ocupante.nombres ?? (ocupante.primerApellido ? '' : ocupante.nombre)
  return {
    nombres: nombres ?? '',
    primerApellido: ocupante.primerApellido ?? '',
    segundoApellido: ocupante.segundoApellido ?? '',
    tipoDocumento: ocupante.tipoDocumento ?? '',
    numeroDocumento: ocupante.numeroDocumento ?? '',
    nacionalidad: ocupante.nacionalidad ?? '',
    genero: ocupante.genero ?? '',
    paisResidencia: ocupante.paisResidencia ?? '',
    ciudadResidencia: ocupante.ciudadResidencia ?? '',
    ciudadProcedencia: ocupante.ciudadProcedencia ?? '',
    ciudadDestino: ocupante.ciudadDestino ?? '',
    motivoViaje: ocupante.motivoViaje ?? '',
  }
}

/** El grupo suele viajar junto: del segundo huésped en adelante, el viaje se propone con el del anterior que lo tenga. */
function viajeAnterior(ocupantes: Ocupante[], actual: Ocupante): Ocupante | null {
  const posicion = ocupantes.findIndex((ocupante) => ocupante.id === actual.id)
  if (posicion <= 0) return null
  return ocupantes.slice(0, posicion).reverse().find((ocupante) => ocupante.ciudadProcedencia) ?? null
}

/**
 * Completar o corregir los datos del TRA de los huéspedes, uno tras otro (BL-07 · DEC-96). Se puede guardar a medias:
 * lo que no se envía se conserva. Un documento ya registrado no se cambia aquí: el backend lo rechaza. Si hay cambios
 * sin guardar, cerrar pregunta antes de descartarlos.
 */
export default function DialogoTra({ reserva, ocupanteId, alCambiar, alCerrar }: {
  reserva: Reserva
  ocupanteId: string
  /** Cada vez que se guarda un huésped llega la reserva actualizada. */
  alCambiar: (reserva: Reserva) => void
  alCerrar: () => void
}) {
  const [actualId, setActualId] = useState(ocupanteId)
  const [vistos, setVistos] = useState<readonly string[]>([ocupanteId])
  const [guardado, setGuardado] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [sucio, setSucio] = useState(false)
  const [preguntando, setPreguntando] = useState(false)
  // El título no cambia al pasar de huésped: cambiarlo movería el foco al encabezado y no al primer campo
  const [titulo] = useState(() => {
    const abierto = reserva.ocupantes.find((ocupante) => ocupante.id === ocupanteId)
    return abierto && !tieneTraPendiente(abierto) ? 'Editar datos' : 'Completar datos'
  })
  const ocupantes = reserva.ocupantes
  const posicion = ocupantes.findIndex((ocupante) => ocupante.id === actualId)
  const ocupante = ocupantes[posicion]
  if (!ocupante) return null

  function cerrar() {
    if (preguntando) setPreguntando(false)
    else if (sucio) setPreguntando(true)
    else alCerrar()
  }

  return (
    <Dialogo
      titulo={titulo}
      subtitulo={`Huésped ${posicion + 1} de ${ocupantes.length} · ${ocupante.nombre}`}
      ocupado={enviando}
      alCerrar={cerrar}
    >
      {preguntando && (
        <PreguntaDescartar
          nombre={ocupante.nombre}
          alSeguir={() => setPreguntando(false)}
          alDescartar={alCerrar}
        />
      )}
      <FormularioTra
        key={ocupante.id}
        reserva={reserva}
        ocupante={ocupante}
        anterior={viajeAnterior(ocupantes, ocupante)}
        guardado={guardado}
        hayOtro={Boolean(siguienteConFaltantes(ocupantes, ocupante.id, vistos))}
        enfocarAlAbrir={ocupante.id !== ocupanteId}
        oculto={preguntando}
        alEnviar={setEnviando}
        alEditar={setSucio}
        alGuardar={(nueva) => {
          alCambiar(nueva)
          setSucio(false)
          const siguiente = siguienteConFaltantes(nueva.ocupantes, ocupante.id, vistos)
          if (!siguiente) {
            alCerrar()
            return
          }
          setGuardado(ocupante.nombre)
          setVistos([...vistos, siguiente.id])
          setActualId(siguiente.id)
        }}
      />
    </Dialogo>
  )
}

/** Antes de cerrar con cambios sin guardar: la pregunta reemplaza al formulario, sin abrir otra ventana encima. */
function PreguntaDescartar({ nombre, alSeguir, alDescartar }: {
  nombre: string
  alSeguir: () => void
  alDescartar: () => void
}) {
  const seguir = useRef<HTMLButtonElement>(null)
  useEffect(() => seguir.current?.focus(), [])
  return (
    <div className="dialogo-contenido">
      <div className="dialogo-cuerpo">
        <Aviso
          tipo="advertencia"
          asunto="Cambios sin guardar"
          mensaje={`Cambiaste datos de ${nombre} que todavía no se guardan. Si cierras ahora, se pierden.`}
        />
      </div>
      <div className="dialogo-pie">
        <button type="button" className="boton boton-secundario" onClick={alDescartar}>Descartar cambios</button>
        <button type="button" className="boton" ref={seguir} onClick={alSeguir}>Seguir editando</button>
      </div>
    </div>
  )
}

function FormularioTra({ reserva, ocupante, anterior, guardado, hayOtro, enfocarAlAbrir, oculto, alEnviar, alEditar, alGuardar }: {
  reserva: Reserva
  ocupante: Ocupante
  anterior: Ocupante | null
  guardado: string | null
  /** Si al guardar se sigue con otro huésped. */
  hayOtro: boolean
  enfocarAlAbrir: boolean
  /** Mientras se pregunta si descartar: se conserva lo escrito, pero no se ve. */
  oculto: boolean
  alEnviar: (enviando: boolean) => void
  /** Si lo escrito difiere de lo que había al abrir. */
  alEditar: (sucio: boolean) => void
  alGuardar: (reserva: Reserva) => void
}) {
  const inicial = valoresDe(ocupante)
  const sinViaje = VIAJE.every((campo) => !inicial[campo])
  const heredado = Boolean(anterior && sinViaje)
  const [alAbrir] = useState<Valores>(() =>
    heredado && anterior ? { ...inicial, ...Object.fromEntries(VIAJE.map((campo) => [campo, anterior[campo] ?? ''])) } : inicial)
  const [valores, setValoresSinAviso] = useState<Valores>(alAbrir)
  const setValores = (cambio: (actuales: Valores) => Valores) => {
    const nuevos = cambio(valores)
    setValoresSinAviso(nuevos)
    alEditar((Object.keys(nuevos) as (keyof Valores)[]).some((campo) => nuevos[campo] !== alAbrir[campo]))
  }
  const [propuestas, setPropuestas] = useState<ReadonlySet<Propuesta>>(new Set())
  const [errorDocumento, setErrorDocumento] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)
  const primero = useRef<HTMLDivElement>(null)
  const documentoRegistrado = Boolean(ocupante.numeroDocumento)
  const pendientes = tieneTraPendiente(ocupante)

  // Al pasar al siguiente huésped el diálogo sigue abierto: el foco va a su primer campo
  useEffect(() => {
    if (enfocarAlAbrir) primero.current?.querySelector<HTMLElement>('input, select')?.focus()
  }, [enfocarAlAbrir])

  function cambiar(campo: keyof Valores, valor: string) {
    setValores((actuales) => ({ ...actuales, [campo]: valor }))
    if (campo === 'ciudadResidencia' || campo === 'ciudadDestino') {
      setPropuestas((actuales) => new Set([...actuales].filter((propuesta) => propuesta !== campo)))
    }
    if (campo === 'tipoDocumento' || campo === 'numeroDocumento') setErrorDocumento(null)
  }

  // Al escribir de dónde viene se proponen residencia y destino, mientras no se hayan escrito a mano
  function cambiarProcedencia(valor: string) {
    const libres = (['ciudadResidencia', 'ciudadDestino'] as const)
      .filter((campo) => !valores[campo] || propuestas.has(campo))
    setValores((actuales) => ({ ...actuales, ciudadProcedencia: valor, ...Object.fromEntries(libres.map((campo) => [campo, valor])) }))
    setPropuestas((actuales) => valor.trim()
      ? new Set([...actuales, ...libres])
      : new Set([...actuales].filter((campo) => !libres.includes(campo))))
  }

  // #18: la lista solo ofrece los de su edad; si ya traía otro, se avisa junto al campo
  const tipoNoAdmitido = documentoRegistrado ? null : mensajeDocumentoPorEdad(valores.tipoDocumento, ocupante.fechaNacimiento, reserva.entrada)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    const tipo = valores.tipoDocumento
    const numero = valores.numeroDocumento.trim()
    if (!documentoRegistrado && Boolean(tipo) !== Boolean(numero)) {
      setErrorDocumento(tipo ? 'Escribe el número de documento.' : 'Elige el tipo de documento.')
      return
    }
    if (tipoNoAdmitido) return
    // Solo se envía lo escrito: lo vacío se conserva en el backend
    const datos: DatosTra = Object.fromEntries(
      Object.entries(valores)
        .map(([campo, valor]) => [campo, valor.trim()] as const)
        .filter(([campo, valor]) => valor && !(documentoRegistrado && (campo === 'tipoDocumento' || campo === 'numeroDocumento'))),
    )
    setEnviando(true)
    alEnviar(true)
    try {
      const nueva = await completarDatosTra(reserva.codigo, ocupante.id, datos)
      setEnviando(false)
      alEnviar(false)
      alGuardar(nueva)
    } catch (causa) {
      setError(errorParaMostrar(causa))
      setEnviando(false)
      alEnviar(false)
    }
  }

  const propuesta = (campo: Propuesta) =>
    propuestas.has(campo) ? <span className="propuesta"><span aria-hidden="true">↺</span> Propuesta</span> : undefined

  return (
    <form className="dialogo-contenido" onSubmit={enviar} noValidate hidden={oculto}>
      <div className="dialogo-cuerpo" ref={primero}>
        {error && <Aviso tipo="error" asunto="No se guardó" mensaje={error.mensaje} detalles={error.detalles} />}
        {guardado && !error && <Aviso tipo="exito" asunto="Datos guardados" mensaje={`Se guardaron los datos de ${guardado}.`} />}
        {!pendientes && (
          <Aviso tipo="exito" asunto="Sin datos pendientes" mensaje={`${ocupante.nombre} ya tiene todos sus datos del TRA. Corrige aquí lo que haga falta.`} />
        )}

        <fieldset className="grupo-formulario" disabled={enviando}>
          <legend>Identidad</legend>
          <Campo
            etiqueta="Nombres"
            value={valores.nombres}
            autoComplete="off"
            pista={!ocupante.primerApellido && !ocupante.nombres ? 'Deja aquí solo los nombres y escribe los apellidos aparte.' : undefined}
            onChange={(evento) => cambiar('nombres', evento.target.value)}
          />
          <div className="campos-2">
            <Campo etiqueta="Primer apellido" value={valores.primerApellido} autoComplete="off" onChange={(evento) => cambiar('primerApellido', evento.target.value)} />
            <Campo etiqueta="Segundo apellido" nota="opcional" value={valores.segundoApellido} autoComplete="off" onChange={(evento) => cambiar('segundoApellido', evento.target.value)} />
          </div>
          <Campo
            etiqueta="Fecha de nacimiento"
            nota="solo lectura"
            readOnly
            value={ocupante.fechaNacimiento ? fechaCorta(ocupante.fechaNacimiento) : 'Sin dato'}
          />
          <div className="campos-2">
            {documentoRegistrado ? (
              <>
                <Campo etiqueta="Tipo de documento" nota="solo lectura" readOnly value={TIPOS_DOCUMENTO_OCUPANTE.find(([valor]) => valor === ocupante.tipoDocumento)?.[1] ?? ocupante.tipoDocumento ?? ''} />
                <Campo etiqueta="Número de documento" nota="solo lectura" readOnly value={ocupante.numeroDocumento ?? ''} />
              </>
            ) : (
              <>
                <Campo
                  etiqueta="Tipo de documento"
                  opciones={tiposPorEdad([ELIGE, ...TIPOS_DOCUMENTO_OCUPANTE], ocupante.fechaNacimiento, reserva.entrada, valores.tipoDocumento)}
                  value={valores.tipoDocumento}
                  invalido={(Boolean(errorDocumento) && !valores.tipoDocumento) || Boolean(tipoNoAdmitido)}
                  error={tipoNoAdmitido ?? (errorDocumento && !valores.tipoDocumento ? errorDocumento : undefined)}
                  onChange={(evento) => cambiar('tipoDocumento', evento.target.value)}
                />
                <Campo
                  etiqueta="Número de documento"
                  value={valores.numeroDocumento}
                  autoComplete="off"
                  maxLength={30}
                  error={errorDocumento && valores.tipoDocumento ? errorDocumento : undefined}
                  onChange={(evento) => cambiar('numeroDocumento', evento.target.value)}
                />
              </>
            )}
          </div>
          {documentoRegistrado && (
            <p className="campo-pista">
              Un documento ya registrado no se cambia aquí. Si está mal escrito, usa «Corregir documento» en la tabla de
              ocupantes del detalle.
            </p>
          )}
          <div className="campos-2">
            <Campo etiqueta="Nacionalidad" value={valores.nacionalidad} autoComplete="off" onChange={(evento) => cambiar('nacionalidad', evento.target.value)} />
            <Campo etiqueta="Género" opciones={[ELIGE, ...GENEROS]} value={valores.genero} onChange={(evento) => cambiar('genero', evento.target.value)} />
          </div>
        </fieldset>

        <fieldset className="grupo-formulario" disabled={enviando}>
          <legend>Viaje</legend>
          {heredado && anterior && (
            <p className="campo-pista">Igual que {anterior.nombre}. Cámbialo si es distinto.</p>
          )}
          <Campo
            etiqueta="¿De dónde viene?"
            value={valores.ciudadProcedencia}
            autoComplete="off"
            placeholder="Ciudad de procedencia"
            pista={!heredado ? 'Al escribirla se proponen la ciudad de residencia y la de destino.' : undefined}
            onChange={(evento) => cambiarProcedencia(evento.target.value)}
          />
          <div className="campos-2">
            <Campo etiqueta="Ciudad de residencia" marca={propuesta('ciudadResidencia')} value={valores.ciudadResidencia} autoComplete="off" onChange={(evento) => cambiar('ciudadResidencia', evento.target.value)} />
            <Campo etiqueta="Ciudad de destino" marca={propuesta('ciudadDestino')} value={valores.ciudadDestino} autoComplete="off" onChange={(evento) => cambiar('ciudadDestino', evento.target.value)} />
          </div>
          <Campo etiqueta="País de residencia" value={valores.paisResidencia} autoComplete="off" onChange={(evento) => cambiar('paisResidencia', evento.target.value)} />
          <Campo etiqueta="Motivo del viaje" opciones={[ELIGE, ...MOTIVOS_VIAJE]} value={valores.motivoViaje} onChange={(evento) => cambiar('motivoViaje', evento.target.value)} />
        </fieldset>
        {enviando && <p className="suave" role="status">Guardando los datos…</p>}
      </div>
      <div className="dialogo-pie">
        <button type="submit" className="boton" disabled={enviando}>
          {enviando ? 'Guardando…' : hayOtro ? 'Guardar y seguir' : 'Guardar'}
        </button>
      </div>
    </form>
  )
}
