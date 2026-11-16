// Los diálogos del detalle de un titular: editar sus datos y validar su identidad (pms/DISENO.md, «Sprint 5: titulares»).
import { useId, useState, type FormEvent } from 'react'
import { ErrorApi } from '../api/cliente'
import { actualizarTitular, validarTitular, type Titular } from '../api/titulares'
import DialogoFormulario, { CampoConContador, type Rechazo } from '../apartamentos/DialogoFormulario'
import { enfocarPrimerError } from '../apartamentos/formulario'
import { errorParaMostrar } from '../reserva/mensajes'
import Campo from '../reserva/nueva/Campo'
import { DatoSoloLectura } from '../usuarios/CamposPersona'
import {
  campoDelDetalle,
  cuerpoTitular,
  formDe,
  MAXIMO_APELLIDO,
  MAXIMO_CORREO,
  MAXIMO_NOMBRES,
  MAXIMO_TELEFONO,
  PISTA_DOCUMENTO,
  textoDocumento,
  validarTitular as revisarFormulario,
  type CampoTitular,
  type FormTitular,
} from './logica'

/** Nombre y contacto del titular. El documento no se edita aquí (TIT-05). */
export function DialogoEditarTitular({ titular, alCerrar, alGuardar }: {
  titular: Titular
  alCerrar: () => void
  alGuardar: (titular: Titular) => void
}) {
  const id = useId()
  const [form, setForm] = useState<FormTitular>(() => formDe(titular))
  const [errores, setErrores] = useState<Partial<Record<CampoTitular, string>>>({})
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)
  const asunto = 'No se guardaron los datos'

  const cambiar = (campo: keyof FormTitular) => (valor: string) => {
    setForm({ ...form, [campo]: valor })
    const sinContacto = campo === 'correo' || campo === 'telefono' ? { contacto: undefined } : {}
    setErrores({ ...errores, [campo]: undefined, ...sinContacto })
  }

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const encontrados = revisarFormulario(form)
    setErrores(encontrados)
    setRechazo(null)
    if (Object.values(encontrados).some(Boolean)) {
      enfocarPrimerError(formulario)
      return
    }
    setEnviando(true)
    try {
      alGuardar(await actualizarTitular(titular.id, cuerpoTitular(form)))
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.estado === 400) {
        const porCampo: Partial<Record<CampoTitular, string>> = {}
        const otros: string[] = []
        for (const detalle of causa.detalles) {
          const campo = campoDelDetalle(detalle)
          const mensaje = detalle.slice(detalle.indexOf(':') + 1).trim()
          if (campo) porCampo[campo] = mensaje.charAt(0).toUpperCase() + mensaje.slice(1)
          else otros.push(mensaje)
        }
        setErrores(porCampo)
        setRechazo({ asunto, mensaje: otros.length > 0 ? 'Revisa estos datos:' : 'Revisa los campos marcados.', detalles: otros })
        enfocarPrimerError(formulario)
      } else {
        setRechazo({ asunto, ...errorParaMostrar(causa) })
      }
    }
  }

  const sinContacto = Boolean(errores.contacto)
  // Un titular de antes de DEC-98 trae todo junto en Nombres
  const sinApellidos = !titular.primerApellido && !form.primerApellido.trim()
  return (
    <DialogoFormulario
      titulo="Editar datos"
      subtitulo={<span className="texto-largo">{titular.nombre}</span>}
      enviando={enviando}
      rechazo={rechazo}
      textoBoton="Guardar cambios"
      textoEnviando="Guardando…"
      alCerrar={alCerrar}
      alEnviar={enviar}
    >
      <div className="formulario-titular">
        {titular.validado && (
          <p className="campo-pista">La identidad está validada: el titular ya no puede cambiar su nombre desde la web.</p>
        )}
        <DatoSoloLectura rotulo="Documento" valor={textoDocumento(titular.tipoDocumento, titular.numeroDocumento)}
          pista={PISTA_DOCUMENTO} />
        <CampoConContador id={`${id}-nombres`} rotulo="Nombres" valor={form.nombres} maximo={MAXIMO_NOMBRES}
          error={errores.nombres} focoInicial deshabilitado={enviando} alCambiar={cambiar('nombres')}
          pista={sinApellidos ? 'Están juntos el nombre y los apellidos: deja aquí solo los nombres y escribe los apellidos aparte.' : undefined} />
        <div className="campos-2">
          <Campo etiqueta="Primer apellido" value={form.primerApellido} maxLength={MAXIMO_APELLIDO} autoComplete="off"
            error={errores.primerApellido} disabled={enviando} onChange={(e) => cambiar('primerApellido')(e.target.value)} />
          <Campo etiqueta="Segundo apellido" nota="opcional" value={form.segundoApellido} maxLength={MAXIMO_APELLIDO}
            autoComplete="off" error={errores.segundoApellido} disabled={enviando}
            onChange={(e) => cambiar('segundoApellido')(e.target.value)} />
        </div>
        <fieldset className="campo contacto-titular" aria-describedby={sinContacto ? `${id}-contacto` : undefined}>
          <legend className="solo-lectores">Contacto</legend>
          <Campo etiqueta="Correo" type="email" inputMode="email" spellCheck={false} value={form.correo}
            maxLength={MAXIMO_CORREO} autoComplete="off" error={errores.correo} invalido={sinContacto}
            disabled={enviando} onChange={(e) => cambiar('correo')(e.target.value)} />
          <Campo etiqueta="Teléfono" type="tel" inputMode="tel" value={form.telefono} maxLength={MAXIMO_TELEFONO}
            autoComplete="off" error={errores.telefono} invalido={sinContacto} disabled={enviando}
            onChange={(e) => cambiar('telefono')(e.target.value)} />
          {sinContacto
            ? <p className="campo-mensaje" id={`${id}-contacto`}>{errores.contacto}</p>
            : <p className="campo-pista">Deja al menos uno de los dos.</p>}
        </fieldset>
      </div>
    </DialogoFormulario>
  )
}

/**
 * Validar la identidad en persona (DEC-98): una sola vez y sin vuelta atrás. Si otra persona ya la validó, el
 * diálogo se cierra y el detalle se recarga con el aviso.
 */
export function DialogoValidarIdentidad({ titular, alCerrar, alValidar, alYaValidada }: {
  titular: Titular
  alCerrar: () => void
  alValidar: (titular: Titular) => void
  alYaValidada: () => void
}) {
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setEnviando(true)
    setRechazo(null)
    try {
      alValidar(await validarTitular(titular.id))
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.motivo === 'IDENTIDAD_VALIDADA') alYaValidada()
      else setRechazo({ asunto: 'No se validó la identidad', ...errorParaMostrar(causa) })
    }
  }

  return (
    <DialogoFormulario
      titulo={`¿Revisaste el documento original de ${titular.nombre}?`}
      enviando={enviando}
      rechazo={rechazo}
      textoBoton="Sí, validar identidad"
      textoEnviando="Validando…"
      alCerrar={alCerrar}
      alEnviar={enviar}
    >
      <p className="documento-grande codigo">{textoDocumento(titular.tipoDocumento, titular.numeroDocumento)}</p>
      <p>
        Confirma solo si lo tienes en la mano y coincide. No se puede deshacer, y desde entonces su nombre solo lo cambia
        el hotel.
      </p>
    </DialogoFormulario>
  )
}
