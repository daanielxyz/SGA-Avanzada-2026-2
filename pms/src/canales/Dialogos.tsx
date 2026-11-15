// Los diálogos de «Canales de venta»: registrar, desactivar y resolver un conflicto (mockups S5Dialogo*; lo resuelto en
// pms/DISENO.md, «Sprint 5: canales de venta»).
import { useId, useState, type FormEvent } from 'react'
import { ErrorApi } from '../api/cliente'
import {
  desactivarCanal,
  registrarCanal,
  resolverConflicto,
  type Canal,
  type Conflicto,
  type TipoCanal,
} from '../api/canales'
import DialogoFormulario, { CampoConContador, type Rechazo } from '../apartamentos/DialogoFormulario'
import { enfocarPrimerError } from '../apartamentos/formulario'
import Aviso from '../componentes/Aviso'
import { errorParaMostrar } from '../reserva/mensajes'
import { estancia, MAXIMO_DECISION, MAXIMO_NOMBRE, MAXIMO_REFERENCIA, TIPOS } from './logica'
import { ConCodigos, ContactoConflicto } from './Piezas'

type Campo = 'nombre' | 'tipo' | 'referencia'

/** «nombre: es obligatorio» → el campo del formulario al que se refiere ese detalle de un 400. */
const CAMPO_DEL_DETALLE: Record<string, Campo> = { nombre: 'nombre', tipo: 'tipo', referenciaCredencial: 'referencia' }

/**
 * «Registrar canal». La referencia de la clave solo se pide (y solo se envía) si el canal es externo: es el nombre con
 * el que quedó guardada la clave en el servidor, nunca la clave (DEC-47).
 */
export function DialogoRegistrarCanal({ alCerrar, alRegistrar }: {
  alCerrar: () => void
  alRegistrar: (canal: Canal) => void
}) {
  const id = useId()
  const [nombre, setNombre] = useState('')
  const [tipo, setTipo] = useState<TipoCanal | ''>('')
  const [referencia, setReferencia] = useState('')
  const [errores, setErrores] = useState<Partial<Record<Campo, string>>>({})
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)
  const asunto = 'No se registró el canal'
  const externo = tipo === 'EXTERNO'

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const encontrados: Partial<Record<Campo, string>> = {
      nombre: nombre.trim() ? undefined : 'Escribe el nombre del canal.',
      tipo: tipo ? undefined : 'Elige el tipo.',
      referencia: externo && !referencia.trim() ? 'Escribe la referencia de la clave.' : undefined,
    }
    setErrores(encontrados)
    setRechazo(null)
    if (Object.values(encontrados).some(Boolean)) {
      enfocarPrimerError(formulario)
      return
    }
    setEnviando(true)
    try {
      alRegistrar(await registrarCanal({
        nombre: nombre.trim(),
        tipo: tipo as TipoCanal,
        ...(externo ? { referenciaCredencial: referencia.trim() } : {}),
      }))
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.estado === 400) {
        const porCampo: Partial<Record<Campo, string>> = {}
        const otros: string[] = []
        for (const detalle of causa.detalles) {
          const [clave, ...resto] = detalle.split(':')
          const campo = CAMPO_DEL_DETALLE[clave.trim()]
          const texto = resto.join(':').trim()
          if (campo) porCampo[campo] = texto.charAt(0).toUpperCase() + texto.slice(1) + '.'
          else otros.push(detalle)
        }
        setErrores(porCampo)
        setRechazo({ asunto, mensaje: otros.length > 0 ? 'Revisa estos datos:' : 'Revisa los campos marcados.', detalles: otros })
        enfocarPrimerError(formulario)
      } else {
        setRechazo({ asunto, ...errorParaMostrar(causa) })
      }
    }
  }

  return (
    <DialogoFormulario titulo="Registrar canal" subtitulo="El canal queda activo al registrarlo." enviando={enviando}
      rechazo={rechazo} textoBoton="Registrar canal" textoEnviando="Registrando…" alCerrar={alCerrar} alEnviar={enviar}>
      <CampoConContador id={`${id}-nombre`} rotulo="Nombre" valor={nombre} maximo={MAXIMO_NOMBRE} error={errores.nombre}
        focoInicial deshabilitado={enviando} alCambiar={(valor) => {
          setNombre(valor)
          setErrores({ ...errores, nombre: undefined })
        }} />

      <fieldset className="campo" aria-describedby={errores.tipo ? `${id}-tipo-error` : undefined}>
        <legend className="campo-rotulo">Tipo</legend>
        <div className="roles">
          {TIPOS.map((opcion) => (
            <label key={opcion.tipo} className={tipo === opcion.tipo ? 'rol-opcion elegido' : 'rol-opcion'}>
              <input type="radio" name={`${id}-tipo`} value={opcion.tipo} checked={tipo === opcion.tipo}
                disabled={enviando} aria-invalid={Boolean(errores.tipo)} onChange={() => {
                  setTipo(opcion.tipo)
                  setErrores({ ...errores, tipo: undefined, referencia: undefined })
                }} />
              <span>
                <strong>{opcion.nombre}</strong>
                <span className="campo-pista">{opcion.explica}</span>
              </span>
            </label>
          ))}
        </div>
        {errores.tipo && <p className="campo-mensaje" id={`${id}-tipo-error`}>{errores.tipo}</p>}
      </fieldset>

      {externo && (
        <div className="campo">
          <label className="campo-rotulo" htmlFor={`${id}-ref`}>Referencia de la clave</label>
          <input id={`${id}-ref`} className="campo-control campo-referencia" maxLength={MAXIMO_REFERENCIA}
            autoComplete="off" spellCheck={false} value={referencia} disabled={enviando}
            aria-invalid={Boolean(errores.referencia)}
            aria-describedby={`${errores.referencia ? `${id}-ref-error ` : ''}${id}-ref-contador ${id}-ref-pista`}
            onChange={(e) => {
              setReferencia(e.target.value)
              setErrores({ ...errores, referencia: undefined })
            }} />
          <div className="campo-contador">
            {errores.referencia ? <p className="campo-mensaje" id={`${id}-ref-error`}>{errores.referencia}</p> : <span />}
            <p className="contador" id={`${id}-ref-contador`}>{referencia.length} / {MAXIMO_REFERENCIA}</p>
          </div>
          <div id={`${id}-ref-pista`}>
            <Aviso tipo="info" mensaje="Escribe el nombre con el que el equipo técnico guardó la clave, no la clave." />
          </div>
        </div>
      )}
    </DialogoFormulario>
  )
}

/**
 * «¿Desactivar Booking?». Si otra persona ya lo desactivó (409), no hay nada que arreglar aquí: la lista se recarga y lo
 * dice (lo resuelto en pms/DISENO.md, punto 4).
 */
export function DialogoDesactivarCanal({ canal, alCerrar, alDesactivar, alYaInactivo }: {
  canal: Canal
  alCerrar: () => void
  alDesactivar: (canal: Canal) => void
  alYaInactivo: () => void
}) {
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setEnviando(true)
    setRechazo(null)
    try {
      alDesactivar(await desactivarCanal(canal.id))
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.estado === 409) alYaInactivo()
      else setRechazo({ asunto: 'No se desactivó el canal', ...errorParaMostrar(causa) })
    }
  }

  return (
    <DialogoFormulario titulo={`¿Desactivar ${canal.nombre}?`} enviando={enviando} rechazo={rechazo}
      textoBoton="Desactivar canal" textoEnviando="Desactivando…" peligro alCerrar={alCerrar} alEnviar={enviar}>
      <p className="dialogo-texto">
        <span className="texto-largo">{canal.nombre}</span> dejará de crear reservas nuevas. Las reservas que ya trajo no
        cambian.
      </p>
      <Aviso tipo="advertencia" mensaje="Esta acción no se puede deshacer desde el PMS." />
    </DialogoFormulario>
  )
}

/**
 * «Resolver conflicto»: deja registrado quién lo revisó, cuándo y qué se decidió (CONF-04). No cambia ninguna reserva
 * (CONF-02). Si otra persona ya lo resolvió (409), se cierra y la lista se recarga.
 */
export function DialogoResolverConflicto({ conflicto, nombreCanal, alCerrar, alResolver, alYaResuelto }: {
  conflicto: Conflicto
  nombreCanal: string
  alCerrar: () => void
  alResolver: (conflicto: Conflicto) => void
  alYaResuelto: () => void
}) {
  const id = useId()
  const [decision, setDecision] = useState('')
  const [error, setError] = useState<string | undefined>()
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    setRechazo(null)
    if (!decision.trim()) {
      setError('Escribe qué se decidió.')
      enfocarPrimerError(formulario)
      return
    }
    setEnviando(true)
    try {
      alResolver(await resolverConflicto(conflicto.id, decision.trim()))
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.estado === 409) alYaResuelto()
      else setRechazo({ asunto: 'No se resolvió el conflicto', ...errorParaMostrar(causa) })
    }
  }

  return (
    <DialogoFormulario titulo="Resolver conflicto" subtitulo="Queda registrado quién lo revisó, cuándo y qué se decidió."
      enviando={enviando} rechazo={rechazo} textoBoton="Marcar como resuelto" textoEnviando="Guardando…"
      alCerrar={alCerrar} alEnviar={enviar}>
      <dl className="resumen-dialogo">
        <dt>Canal</dt>
        <dd className="texto-largo">{nombreCanal}</dd>
        <dt>Reserva externa</dt>
        <dd className="codigo">{conflicto.idExterno}</dd>
        <dt>Apartamento</dt>
        <dd className="codigo">{conflicto.apartamento}</dd>
        <dt>Entrada y salida</dt>
        <dd>{estancia(conflicto)}</dd>
        <dt>Huésped</dt>
        <dd><ContactoConflicto conflicto={conflicto} /></dd>
        <dt className="resumen-ancho">Motivo</dt>
        <dd className="resumen-ancho"><ConCodigos texto={conflicto.motivo} /></dd>
      </dl>
      <CampoConContador id={`${id}-decision`} rotulo="¿Qué se decidió?" valor={decision} maximo={MAXIMO_DECISION}
        varias focoInicial error={error} deshabilitado={enviando}
        pista="Por ejemplo: «Llamé al huésped y lo pasamos al APT-104 por Booking»."
        alCambiar={(valor) => {
          setDecision(valor)
          setError(undefined)
        }} />
      <Aviso tipo="info" mensaje="Esto no cambia ninguna reserva. Si hay que mover o crear una, hazlo en Reservas." />
    </DialogoFormulario>
  )
}
