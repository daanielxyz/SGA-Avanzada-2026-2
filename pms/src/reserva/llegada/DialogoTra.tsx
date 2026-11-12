import { useEffect, useRef, useState, type FormEvent } from 'react'
import { completarDatosTra, type DatosTra, type Ocupante, type Reserva } from '../../api/reservas'
import Aviso from '../../componentes/Aviso'
import Dialogo from '../../componentes/Dialogo'
import { fechaCorta } from '../../formato'
import { errorParaMostrar, type ErrorMostrado } from '../mensajes'
import Campo from '../nueva/Campo'
import { GENEROS, MOTIVOS_VIAJE, siguienteConFaltantes, TIPOS_DOCUMENTO, tieneTraPendiente } from './llegada'

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
 * Completar los datos del TRA de los huéspedes, uno tras otro (BL-07 · DEC-96). Se puede guardar a medias: lo que no se
 * envía se conserva. Un documento ya registrado no se cambia aquí: el backend lo rechaza.
 */
export default function DialogoTra({ reserva, ocupanteId, alCambiar, alCerrar }: {
  reserva: Reserva
  ocupanteId: string
  /** Cada vez que se guarda un huésped llega la reserva actualizada. */
  alCambiar: (reserva: Reserva) => void
  alCerrar: () => void
}) {
  const [actualId, setActualId] = useState(ocupanteId)
  const [guardado, setGuardado] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const ocupantes = reserva.ocupantes
  const posicion = ocupantes.findIndex((ocupante) => ocupante.id === actualId)
  const ocupante = ocupantes[posicion]
  if (!ocupante) return null

  return (
    <Dialogo
      titulo="Completar datos"
      subtitulo={`Huésped ${posicion + 1} de ${ocupantes.length} · ${ocupante.nombre}`}
      ocupado={enviando}
      alCerrar={alCerrar}
    >
      <FormularioTra
        key={ocupante.id}
        reserva={reserva}
        ocupante={ocupante}
        anterior={viajeAnterior(ocupantes, ocupante)}
        guardado={guardado}
        enfocarAlAbrir={ocupante.id !== ocupanteId}
        alEnviar={setEnviando}
        alGuardar={(nueva) => {
          alCambiar(nueva)
          const siguiente = siguienteConFaltantes(nueva.ocupantes, ocupante.id)
          if (!siguiente) {
            alCerrar()
            return
          }
          setGuardado(ocupante.nombre)
          setActualId(siguiente.id)
        }}
      />
    </Dialogo>
  )
}

function FormularioTra({ reserva, ocupante, anterior, guardado, enfocarAlAbrir, alEnviar, alGuardar }: {
  reserva: Reserva
  ocupante: Ocupante
  anterior: Ocupante | null
  guardado: string | null
  enfocarAlAbrir: boolean
  alEnviar: (enviando: boolean) => void
  alGuardar: (reserva: Reserva) => void
}) {
  const inicial = valoresDe(ocupante)
  const sinViaje = VIAJE.every((campo) => !inicial[campo])
  const heredado = Boolean(anterior && sinViaje)
  const [valores, setValores] = useState<Valores>(() =>
    heredado && anterior ? { ...inicial, ...Object.fromEntries(VIAJE.map((campo) => [campo, anterior[campo] ?? ''])) } : inicial)
  const [propuestas, setPropuestas] = useState<ReadonlySet<Propuesta>>(new Set())
  const [errorDocumento, setErrorDocumento] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)
  const primero = useRef<HTMLDivElement>(null)
  const documentoRegistrado = Boolean(ocupante.numeroDocumento)
  const pendientes = tieneTraPendiente(ocupante)
  const otros = reserva.ocupantes.some((otro) => otro.id !== ocupante.id && tieneTraPendiente(otro))

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

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    const tipo = valores.tipoDocumento
    const numero = valores.numeroDocumento.trim()
    if (!documentoRegistrado && Boolean(tipo) !== Boolean(numero)) {
      setErrorDocumento(tipo ? 'Escribe el número de documento.' : 'Elige el tipo de documento.')
      return
    }
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
    <form className="dialogo-contenido" onSubmit={enviar} noValidate>
      <div className="dialogo-cuerpo" ref={primero}>
        {error && <Aviso tipo="error" asunto="No se guardó" mensaje={error.mensaje} detalles={error.detalles} />}
        {guardado && !error && <Aviso tipo="exito" asunto="Datos guardados" mensaje={`Se guardaron los datos de ${guardado}.`} />}
        {!pendientes && (
          <Aviso tipo="exito" asunto="Sin datos pendientes" mensaje={`${ocupante.nombre} ya tiene todos sus datos del TRA.`} />
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
                <Campo etiqueta="Tipo de documento" nota="solo lectura" readOnly value={TIPOS_DOCUMENTO.find(([valor]) => valor === ocupante.tipoDocumento)?.[1] ?? ocupante.tipoDocumento ?? ''} />
                <Campo etiqueta="Número de documento" nota="solo lectura" readOnly value={ocupante.numeroDocumento ?? ''} />
              </>
            ) : (
              <>
                <Campo
                  etiqueta="Tipo de documento"
                  opciones={[ELIGE, ...TIPOS_DOCUMENTO]}
                  value={valores.tipoDocumento}
                  invalido={Boolean(errorDocumento) && !valores.tipoDocumento}
                  error={errorDocumento && !valores.tipoDocumento ? errorDocumento : undefined}
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
          {documentoRegistrado && <p className="campo-pista">Un documento ya registrado no se cambia aquí.</p>}
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
          {enviando ? 'Guardando…' : otros ? 'Guardar y seguir' : 'Guardar'}
        </button>
      </div>
    </form>
  )
}
