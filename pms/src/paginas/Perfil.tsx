import { useEffect, useState, type FormEvent } from 'react'
import { ErrorApi } from '../api/cliente'
import { actualizarPerfil, cambiarContrasena, consultarPerfil, type Usuario } from '../api/usuarios'
import { enfocarPrimerError } from '../apartamentos/formulario'
import Aviso from '../componentes/Aviso'
import CampoClave from '../componentes/CampoClave'
import { useTitulo } from '../componentes/useTitulo'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import { useSesion } from '../sesion/contexto'
import { NOMBRE_DEL_ROL } from '../sesion/roles'
import { elegirTema, leerTema, type Tema } from '../tema'
import CamposPersona, { DatoSoloLectura } from '../usuarios/CamposPersona'
import {
  campoDelDetalle,
  cuerpoPersona,
  errorDeContrasena,
  errorDeRepetida,
  formDe,
  hayErrores,
  mensajeDelDetalle,
  REGLA_CONTRASENA,
  validarPersona,
  type CampoPersona,
  type Errores,
  type FormPersona,
} from '../usuarios/logica'

const CAMPOS: readonly CampoPersona[] = ['nombre', 'telefono', 'tipoDocumento', 'numeroDocumento', 'fechaNacimiento']

/**
 * Mi perfil (CU-03 · CU-04): cualquier rol cambia sus datos y su contraseña; el correo y el rol, solo el administrador.
 * También elige aquí la apariencia del PMS, día o noche.
 */
export default function Perfil() {
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<{ intento: number; usuario?: Usuario; error?: unknown } | null>(null)
  useTitulo('Mi perfil')

  useEffect(() => {
    let vigente = true
    consultarPerfil()
      .then((usuario) => vigente && setResultado({ intento, usuario }))
      .catch((error: unknown) => vigente && setResultado({ intento, error }))
    return () => {
      vigente = false
    }
  }, [intento])

  const actual = resultado?.intento === intento ? resultado : null
  return (
    <div className="perfil">
      <h1>Mi perfil</h1>
      {!actual && (
        <div className="tarjeta perfil-bloque" aria-busy="true">
          <p className="carga-texto suave" role="status">Cargando tus datos…</p>
        </div>
      )}
      {actual?.error !== undefined && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar tus datos"
          mensaje={errorParaMostrar(actual.error).mensaje}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      )}
      {actual?.usuario && (
        <>
          <MisDatos usuario={actual.usuario} />
          <Contrasena />
        </>
      )}
      <Apariencia />
    </div>
  )
}

const TEMAS: readonly { tema: Tema; nombre: string; explica: string }[] = [
  { tema: 'noche', nombre: 'Noche', explica: 'Fondo oscuro. Descansa la vista con poca luz.' },
  { tema: 'dia', nombre: 'Día', explica: 'Fondo claro. Se lee mejor con mucha luz.' },
]

/**
 * Apariencia: día o noche. No depende de los datos del perfil, así que está aunque no carguen. Cambia al momento y se
 * guarda en este navegador (`src/tema.ts`), no en la cuenta.
 */
function Apariencia() {
  const [tema, setTema] = useState<Tema>(leerTema)

  function elegir(nuevo: Tema) {
    elegirTema(nuevo)
    setTema(nuevo)
  }

  return (
    <section className="tarjeta perfil-bloque" aria-labelledby="titulo-apariencia">
      <div className="perfil-bloque-cuerpo">
        <h2 id="titulo-apariencia">Apariencia</h2>
        <fieldset className="apariencia">
          <legend className="solo-lectores">Tema del PMS</legend>
          <div className="apariencia-opciones">
            {TEMAS.map((opcion) => (
              <label key={opcion.tema} className="apariencia-opcion">
                <input type="radio" name="tema" value={opcion.tema} checked={tema === opcion.tema}
                  onChange={() => elegir(opcion.tema)} />
                <span className="apariencia-textos">
                  <strong>{opcion.nombre}</strong>
                  <span className="campo-pista">{opcion.explica}</span>
                </span>
                <span className={`apariencia-muestra apariencia-muestra-${opcion.tema}`} aria-hidden="true" />
              </label>
            ))}
          </div>
        </fieldset>
        <p className="campo-pista">Se guarda en este navegador: en otro computador o celular lo eliges de nuevo.</p>
      </div>
    </section>
  )
}

function MisDatos({ usuario }: { usuario: Usuario }) {
  const { renombrar } = useSesion()
  const [form, setForm] = useState<FormPersona>(() => formDe(usuario))
  const [errores, setErrores] = useState<Errores<CampoPersona>>({})
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<ErrorMostrado | null>(null)
  const [guardado, setGuardado] = useState(false)

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const encontrados = validarPersona(form)
    setErrores(encontrados)
    setRechazo(null)
    setGuardado(false)
    if (hayErrores(encontrados)) {
      enfocarPrimerError(formulario)
      return
    }
    setEnviando(true)
    try {
      const nuevo = await actualizarPerfil(cuerpoPersona(form))
      setForm(formDe(nuevo))
      renombrar(nuevo.nombre)
      setGuardado(true)
    } catch (causa) {
      if (causa instanceof ErrorApi && causa.estado === 400) {
        const porCampo: Errores<CampoPersona> = {}
        const otros: string[] = []
        for (const detalle of causa.detalles) {
          const campo = campoDelDetalle(detalle, CAMPOS)
          if (campo) porCampo[campo] = mensajeDelDetalle(detalle)
          else otros.push(mensajeDelDetalle(detalle))
        }
        setErrores(porCampo)
        setRechazo({ mensaje: otros.length > 0 ? 'Revisa estos datos:' : 'Revisa los campos marcados.', detalles: otros })
        enfocarPrimerError(formulario)
      } else {
        setRechazo(errorParaMostrar(causa))
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="tarjeta perfil-bloque" aria-labelledby="titulo-mis-datos">
      <form onSubmit={enviar} noValidate aria-busy={enviando}>
        <div className="perfil-bloque-cuerpo">
          <h2 id="titulo-mis-datos">Mis datos</h2>
          {rechazo && <Aviso tipo="error" asunto="No se guardaron tus datos" mensaje={rechazo.mensaje} detalles={rechazo.detalles} />}
          <CamposPersona form={form} errores={errores} deshabilitado={enviando} enRejilla
            alCambiar={(cambio) => {
              setForm({ ...form, ...cambio })
              setErrores({ ...errores, ...Object.fromEntries(Object.keys(cambio).map((c) => [c, undefined])) })
              setGuardado(false)
            }} />
          <div className="campos-persona-rejilla">
            <DatoSoloLectura rotulo="Correo" valor={usuario.correo} />
            <DatoSoloLectura rotulo="Rol" valor={NOMBRE_DEL_ROL[usuario.rol]} />
          </div>
          <p className="campo-pista">El correo y el rol solo los cambia Administración.</p>
        </div>
        <div className="perfil-bloque-pie">
          {/* El aviso se anuncia apenas aparece: el lector no tiene que ir a buscarlo */}
          <p className="perfil-guardado" role="status">
            {guardado && <><span aria-hidden="true">✓</span> Tus datos quedaron guardados</>}
          </p>
          <button type="submit" className="boton" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar cambios'}
          </button>
        </div>
      </form>
    </section>
  )
}

function Contrasena() {
  const { terminar } = useSesion()
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [repetida, setRepetida] = useState('')
  const [errores, setErrores] = useState<{ actual?: string; nueva?: string; repetida?: string }>({})
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<ErrorMostrado | null>(null)

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const encontrados = {
      actual: actual ? undefined : 'Escribe tu contraseña actual.',
      nueva: errorDeContrasena(nueva),
      repetida: errorDeRepetida(nueva, repetida),
    }
    setErrores(encontrados)
    setRechazo(null)
    if (hayErrores(encontrados)) {
      enfocarPrimerError(formulario)
      return
    }
    setEnviando(true)
    try {
      await cambiarContrasena(actual, nueva)
      // El backend cierra todas sus sesiones, esta también (DEC-71)
      terminar('Tu contraseña cambió. Entra con la nueva.')
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.codigo === 'CREDENCIALES_INVALIDAS') {
        setErrores({ actual: 'La contraseña actual no es correcta.' })
        enfocarPrimerError(formulario)
      } else {
        setRechazo(errorParaMostrar(causa))
      }
    }
  }

  return (
    <section className="tarjeta perfil-bloque" aria-labelledby="titulo-contrasena">
      <form onSubmit={enviar} noValidate aria-busy={enviando}>
        <div className="perfil-bloque-cuerpo perfil-contrasena">
          <h2 id="titulo-contrasena">Contraseña</h2>
          {rechazo && <Aviso tipo="error" asunto="No se cambió la contraseña" mensaje={rechazo.mensaje} detalles={rechazo.detalles} />}
          <CampoClave etiqueta="Contraseña actual" autocompletar="current-password" valor={actual} error={errores.actual}
            deshabilitado={enviando} alCambiar={(v) => {
              setActual(v)
              setErrores({ ...errores, actual: undefined })
            }} />
          <CampoClave etiqueta="Contraseña nueva" autocompletar="new-password" valor={nueva} error={errores.nueva}
            pista={REGLA_CONTRASENA} deshabilitado={enviando} alCambiar={(v) => {
              setNueva(v)
              setErrores({ ...errores, nueva: undefined })
            }} />
          <CampoClave etiqueta="Repite la contraseña nueva" autocompletar="new-password" valor={repetida}
            error={errores.repetida} deshabilitado={enviando} alCambiar={(v) => {
              setRepetida(v)
              setErrores({ ...errores, repetida: undefined })
            }} />
          <p className="campo-pista">Al cambiarla se cierran tus sesiones abiertas, también esta: vuelves a entrar con la nueva.</p>
        </div>
        <div className="perfil-bloque-pie">
          <span />
          <button type="submit" className="boton" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Cambiar contraseña'}
          </button>
        </div>
      </form>
    </section>
  )
}
