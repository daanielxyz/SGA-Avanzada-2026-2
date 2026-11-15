import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { pedirCodigoDeRecuperacion, restablecerContrasena } from '../api/usuarios'
import { enfocarPrimerError } from '../apartamentos/formulario'
import Aviso from '../componentes/Aviso'
import CampoClave from '../componentes/CampoClave'
import { useTitulo } from '../componentes/useTitulo'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import { DatoSoloLectura } from '../usuarios/CamposPersona'
import { campoDeFijar, errorDeCodigo, errorDeContrasena, errorDeCorreo, errorDeRepetida, hayErrores, REGLA_CONTRASENA } from '../usuarios/logica'
import { CampoCodigo, CampoCorreoEntrada, PaginaSinSesion } from '../usuarios/SinSesion'

interface Errores {
  correo?: string
  codigo?: string
  contrasena?: string
  repetida?: string
}

/**
 * «Olvidé mi contraseña», sin sesión y en dos pasos (CU-05 · USU-08): el correo, y después el código de 15 minutos con
 * la contraseña nueva. El paso 2 se ve igual exista o no el correo.
 */
export default function RecuperarContrasena() {
  const navegar = useNavigate()
  const desde = (useLocation().state as { correo?: string } | null)?.correo ?? ''
  const [paso, setPaso] = useState<1 | 2>(1)
  const [correo, setCorreo] = useState(desde)
  const [codigo, setCodigo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [repetida, setRepetida] = useState('')
  const [errores, setErrores] = useState<Errores>({})
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<ErrorMostrado | null>(null)
  useTitulo('Olvidé mi contraseña')

  async function pedirCodigo(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const error = errorDeCorreo(correo)
    setErrores({ correo: error })
    setRechazo(null)
    if (error) {
      enfocarPrimerError(formulario)
      return
    }
    setEnviando(true)
    try {
      await pedirCodigoDeRecuperacion(correo.trim())
      setCodigo('')
      setPaso(2)
    } catch (causa) {
      if (causa instanceof ErrorApi && causa.estado === 400) {
        setErrores({ correo: 'Escribe un correo válido, por ejemplo nombre@dominio.co.' })
        enfocarPrimerError(formulario)
      } else {
        setRechazo(errorParaMostrar(causa))
      }
    } finally {
      setEnviando(false)
    }
  }

  async function cambiar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const encontrados: Errores = {
      codigo: errorDeCodigo(codigo),
      contrasena: errorDeContrasena(contrasena),
      repetida: errorDeRepetida(contrasena, repetida),
    }
    setErrores(encontrados)
    setRechazo(null)
    if (hayErrores(encontrados)) {
      enfocarPrimerError(formulario)
      return
    }
    setEnviando(true)
    try {
      await restablecerContrasena(correo.trim(), codigo, contrasena)
      navegar('/ingresar', { replace: true, state: { aviso: 'Tu contraseña cambió. Entra con la nueva.' } })
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.codigo === 'CREDENCIALES_INVALIDAS') {
        setErrores({ codigo: 'El código no es válido o ya venció.' })
      } else if (causa instanceof ErrorApi && causa.estado === 400) {
        setErrores(campoDeFijar(causa))
      } else {
        setRechazo(errorParaMostrar(causa))
      }
      enfocarPrimerError(formulario)
    }
  }

  if (paso === 1) {
    return (
      <PaginaSinSesion titulo="Olvidé mi contraseña" paso="Paso 1 de 2"
        texto="Escribe el correo de tu cuenta y te enviamos un código." ocupado={enviando} alEnviar={pedirCodigo}>
        {rechazo && <Aviso tipo="error" asunto="No se envió el código" mensaje={rechazo.mensaje} detalles={rechazo.detalles} />}
        <CampoCorreoEntrada valor={correo} error={errores.correo} deshabilitado={enviando}
          alCambiar={(v) => {
            setCorreo(v)
            setErrores({})
          }} />
        <button type="submit" className="boton boton-bloque boton-grande" disabled={enviando}>
          {enviando ? 'Enviando…' : 'Enviar código'}
        </button>
      </PaginaSinSesion>
    )
  }

  return (
    <PaginaSinSesion titulo="Olvidé mi contraseña" paso="Paso 2 de 2" ocupado={enviando} alEnviar={cambiar}>
      <Aviso tipo="info" mensaje="Si el correo está registrado, te llegó un código de 6 dígitos. Vence en 15 minutos." />
      {rechazo && <Aviso tipo="error" asunto="No se cambió la contraseña" mensaje={rechazo.mensaje} detalles={rechazo.detalles} />}
      <DatoSoloLectura rotulo="Correo electrónico" valor={correo.trim()} />
      <CampoCodigo valor={codigo} error={errores.codigo} pista="Si no te llegó o ya venció, pide otro abajo."
        deshabilitado={enviando} alCambiar={(v) => {
          setCodigo(v)
          setErrores({ ...errores, codigo: undefined })
        }} />
      <CampoClave etiqueta="Contraseña nueva" autocompletar="new-password" valor={contrasena} error={errores.contrasena}
        pista={REGLA_CONTRASENA} deshabilitado={enviando} alCambiar={(v) => {
          setContrasena(v)
          setErrores({ ...errores, contrasena: undefined })
        }} />
      <CampoClave etiqueta="Repite la contraseña nueva" autocompletar="new-password" valor={repetida}
        error={errores.repetida} deshabilitado={enviando} alCambiar={(v) => {
          setRepetida(v)
          setErrores({ ...errores, repetida: undefined })
        }} />
      <button type="submit" className="boton boton-bloque boton-grande" disabled={enviando}>
        {enviando ? 'Guardando…' : 'Cambiar contraseña'}
      </button>
      <button type="button" className="enlace" disabled={enviando} onClick={() => {
        setPaso(1)
        setErrores({})
        setRechazo(null)
      }}>
        Pedir otro código o usar otro correo
      </button>
    </PaginaSinSesion>
  )
}
