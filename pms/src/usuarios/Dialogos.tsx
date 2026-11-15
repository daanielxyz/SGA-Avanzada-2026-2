// Los diálogos de «Usuarios»: crear, editar y eliminar una cuenta (pms/DISENO.md, «Sprint 4: usuarios»).
import { useId, useState, type FormEvent } from 'react'
import { ErrorApi } from '../api/cliente'
import { crearUsuario, eliminarUsuario, modificarUsuario, type Usuario } from '../api/usuarios'
import DialogoFormulario, { type Rechazo } from '../apartamentos/DialogoFormulario'
import { enfocarPrimerError } from '../apartamentos/formulario'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import { errorParaMostrar } from '../reserva/mensajes'
import { NOMBRE_DEL_ROL, type Rol } from '../sesion/roles'
import CamposPersona, { DatoSoloLectura } from './CamposPersona'
import EstadoCuenta, { Correo } from './EstadoCuenta'
import {
  campoDelDetalle,
  cuerpoPersona,
  errorDeCorreo,
  formDe,
  hayErrores,
  mensajeDelDetalle,
  MAXIMO_CORREO,
  nombreCorto,
  PERSONA_VACIA,
  ROLES,
  validarPersona,
  type CampoUsuario,
  type Errores,
  type FormPersona,
} from './logica'

const CAMPOS: readonly CampoUsuario[] = ['nombre', 'correo', 'telefono', 'tipoDocumento', 'numeroDocumento', 'fechaNacimiento', 'rol']

/** Los tres roles, cada uno con lo que puede hacer. */
function OpcionesRol({ rol, error, alElegir }: { rol: Rol | ''; error?: string; alElegir: (rol: Rol) => void }) {
  const id = useId()
  return (
    <fieldset className="campo" aria-describedby={error ? `${id}-error` : undefined}>
      <legend className="campo-rotulo">Rol</legend>
      <div className="roles">
        {ROLES.map((opcion) => (
          <label key={opcion.rol} className={rol === opcion.rol ? 'rol-opcion elegido' : 'rol-opcion'}>
            <input type="radio" name={`${id}-rol`} value={opcion.rol} checked={rol === opcion.rol}
              aria-invalid={Boolean(error)} onChange={() => alElegir(opcion.rol)} />
            <span>
              <strong>{opcion.nombre}</strong>
              <span className="campo-pista">{opcion.explica}</span>
            </span>
          </label>
        ))}
      </div>
      {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
    </fieldset>
  )
}

/**
 * «Nuevo usuario» o «Editar usuario». Al crear, el diálogo termina mostrando a quién le llegó el código; al editar, se
 * cierra. Si cambia el rol, avisa antes de guardar que esa persona tendrá que volver a entrar.
 *
 * @param propio el usuario que se edita es quien tiene la sesión
 */
export function DialogoUsuario({ usuario, propio = false, alCerrar, alGuardar }: {
  usuario?: Usuario
  propio?: boolean
  alCerrar: () => void
  alGuardar: (usuario: Usuario, creado: boolean) => void
}) {
  const nuevo = !usuario
  const [form, setForm] = useState<FormPersona>(usuario ? formDe(usuario) : PERSONA_VACIA)
  const [correo, setCorreo] = useState('')
  const [rol, setRol] = useState<Rol | ''>(usuario?.rol ?? '')
  const [errores, setErrores] = useState<Errores<CampoUsuario>>({})
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)
  const [creado, setCreado] = useState<Usuario | null>(null)
  const asunto = nuevo ? 'No se creó el usuario' : 'No se guardaron los cambios'
  const cambiaRol = Boolean(usuario && rol && rol !== usuario.rol)

  if (creado) {
    return (
      <Dialogo titulo="Usuario creado" alCerrar={() => alGuardar(creado, true)}>
        <div className="dialogo-cuerpo">
          <Aviso tipo="exito" asunto="Cuenta creada"
            mensaje={`Le enviamos a ${creado.correo} un código para activar su cuenta. Vence en 24 horas.`} />
          <dl className="resumen-usuario">
            <div><dt>Nombre</dt><dd className="texto-largo">{creado.nombre}</dd></div>
            <div><dt>Rol</dt><dd>{NOMBRE_DEL_ROL[creado.rol]}</dd></div>
            <div><dt>Estado</dt><dd><EstadoCuenta usuario={creado} /></dd></div>
          </dl>
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton" data-foco-inicial onClick={() => alGuardar(creado, true)}>Cerrar</button>
        </div>
      </Dialogo>
    )
  }

  const cambiar = (cambio: Partial<FormPersona>) => {
    setForm({ ...form, ...cambio })
    setErrores({ ...errores, ...Object.fromEntries(Object.keys(cambio).map((campo) => [campo, undefined])) })
  }

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const encontrados: Errores<CampoUsuario> = {
      ...validarPersona(form),
      correo: nuevo ? errorDeCorreo(correo) : undefined,
      rol: rol ? undefined : 'Elige el rol.',
    }
    setErrores(encontrados)
    setRechazo(null)
    if (hayErrores(encontrados)) {
      enfocarPrimerError(formulario)
      return
    }
    const datos = { ...cuerpoPersona(form), rol: rol as Rol }
    setEnviando(true)
    try {
      if (usuario) {
        alGuardar(await modificarUsuario(usuario.id, datos), false)
      } else {
        setCreado(await crearUsuario({ ...datos, correo: correo.trim() }))
        setEnviando(false)
      }
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.codigo === 'DUPLICADO') {
        setErrores({ correo: 'Ya hay una cuenta con este correo.' })
        setRechazo({ asunto: `${asunto}: revisa el campo marcado`, mensaje: 'El error está junto al campo.', detalles: [] })
        enfocarPrimerError(formulario)
      } else if (causa instanceof ErrorApi && causa.estado === 400) {
        const porCampo: Errores<CampoUsuario> = {}
        const otros: string[] = []
        for (const detalle of causa.detalles) {
          const campo = campoDelDetalle(detalle, CAMPOS)
          if (campo) porCampo[campo] = mensajeDelDetalle(detalle)
          else otros.push(mensajeDelDetalle(detalle))
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
    <DialogoFormulario
      titulo={nuevo ? 'Nuevo usuario' : 'Editar usuario'}
      subtitulo={nuevo ? 'La cuenta nace sin activar.' : <span className="texto-largo">{usuario.nombre}</span>}
      enviando={enviando}
      rechazo={rechazo}
      textoBoton={nuevo ? 'Crear usuario' : 'Guardar cambios'}
      textoEnviando={nuevo ? 'Creando…' : 'Guardando…'}
      alCerrar={alCerrar}
      alEnviar={enviar}
    >
      <div className="formulario-usuario">
        <CamposPersona form={form} errores={errores} deshabilitado={enviando} focoInicial alCambiar={cambiar}
          despuesDelNombre={nuevo ? (
            <CampoCorreo correo={correo} error={errores.correo} deshabilitado={enviando}
              alCambiar={(valor) => {
                setCorreo(valor)
                setErrores({ ...errores, correo: undefined })
              }} />
          ) : (
            <DatoSoloLectura rotulo="Correo" valor={usuario.correo} pista="El correo no se puede cambiar." />
          )} />

        <OpcionesRol rol={rol} error={errores.rol} alElegir={(elegido) => {
          setRol(elegido)
          setErrores({ ...errores, rol: undefined })
        }} />
        {cambiaRol && usuario && (
          <Aviso tipo="advertencia"
            asunto={propio ? 'Tendrás que volver a entrar' : `${nombreCorto(usuario.nombre)} tendrá que volver a entrar`}
            mensaje={propio
              ? 'Al cambiar tu rol se cierran tus sesiones abiertas, también esta.'
              : 'Al cambiar el rol se cierran sus sesiones abiertas.'} />
        )}
      </div>
    </DialogoFormulario>
  )
}

function CampoCorreo({ correo, error, deshabilitado, alCambiar }: {
  correo: string
  error?: string
  deshabilitado: boolean
  alCambiar: (correo: string) => void
}) {
  const id = useId()
  return (
    <div className="campo">
      <label className="campo-rotulo" htmlFor={id}>Correo</label>
      <input id={id} className="campo-control" type="email" inputMode="email" autoComplete="off" spellCheck={false}
        maxLength={MAXIMO_CORREO} value={correo} disabled={deshabilitado} aria-invalid={Boolean(error)}
        aria-describedby={`${error ? `${id}-error ` : ''}${id}-pista`}
        onChange={(e) => alCambiar(e.target.value)} />
      {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
      <p className="campo-pista" id={`${id}-pista`}>Le llegará un código para activar su cuenta. El correo no se cambia después.</p>
    </div>
  )
}

/** «¿Eliminar la cuenta de …?»: no se deshace, y lo que hizo sigue registrado a su nombre. */
export function DialogoEliminarUsuario({ usuario, alCerrar, alEliminar }: {
  usuario: Usuario
  alCerrar: () => void
  alEliminar: (usuario: Usuario) => void
}) {
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setEnviando(true)
    setRechazo(null)
    try {
      alEliminar(await eliminarUsuario(usuario.id))
    } catch (causa) {
      setEnviando(false)
      setRechazo({ asunto: 'No se eliminó la cuenta', ...errorParaMostrar(causa) })
    }
  }

  return (
    <DialogoFormulario
      titulo={`¿Eliminar la cuenta de ${usuario.nombre}?`}
      subtitulo={<><Correo correo={usuario.correo} /> · {NOMBRE_DEL_ROL[usuario.rol]}</>}
      enviando={enviando}
      rechazo={rechazo}
      textoBoton="Eliminar cuenta"
      textoEnviando="Eliminando…"
      peligro
      alCerrar={alCerrar}
      alEnviar={enviar}
    >
      <p>
        {usuario.nombre} ya no podrá entrar al PMS. Sus acciones anteriores siguen registradas a su nombre.
      </p>
      <p className="campo-pista">No se puede deshacer.</p>
    </DialogoFormulario>
  )
}
