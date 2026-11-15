import { useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { activarCuenta } from '../api/usuarios'
import { enfocarPrimerError } from '../apartamentos/formulario'
import Aviso from '../componentes/Aviso'
import CampoClave from '../componentes/CampoClave'
import { useTitulo } from '../componentes/useTitulo'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import { campoDeFijar, errorDeCodigo, errorDeContrasena, errorDeCorreo, errorDeRepetida, hayErrores, REGLA_CONTRASENA } from '../usuarios/logica'
import { CampoCodigo, CampoCorreoEntrada, PaginaSinSesion } from '../usuarios/SinSesion'

interface Errores {
  correo?: string
  codigo?: string
  contrasena?: string
  repetida?: string
}

/**
 * «Activar mi cuenta», sin sesión (CU-05 · DEC-71): con el código de 24 horas que le llegó al crearla, la persona fija
 * su primera contraseña. Un código vencido no lo reenvía el administrador: se pide otro en «Olvidé mi contraseña».
 */
export default function ActivarCuenta() {
  const navegar = useNavigate()
  const [correo, setCorreo] = useState('')
  const [codigo, setCodigo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [repetida, setRepetida] = useState('')
  const [errores, setErrores] = useState<Errores>({})
  const [codigoNoVale, setCodigoNoVale] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<ErrorMostrado | null>(null)
  const formulario = useRef<HTMLFormElement | null>(null)
  useTitulo('Activar mi cuenta')

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    formulario.current = evento.currentTarget
    const encontrados: Errores = {
      correo: errorDeCorreo(correo),
      codigo: errorDeCodigo(codigo),
      contrasena: errorDeContrasena(contrasena),
      repetida: errorDeRepetida(contrasena, repetida),
    }
    setErrores(encontrados)
    setRechazo(null)
    setCodigoNoVale(false)
    if (hayErrores(encontrados)) {
      enfocarPrimerError(evento.currentTarget)
      return
    }
    setEnviando(true)
    try {
      await activarCuenta(correo.trim(), codigo, contrasena)
      navegar('/ingresar', { replace: true, state: { aviso: 'Tu cuenta quedó activa. Entra con tu correo y tu contraseña.' } })
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.codigo === 'CREDENCIALES_INVALIDAS') {
        setErrores({ codigo: 'El correo o el código no son válidos, o el código ya venció.' })
        setCodigoNoVale(true)
      } else if (causa instanceof ErrorApi && causa.estado === 400) {
        setErrores(campoDeFijar(causa))
      } else {
        setRechazo(errorParaMostrar(causa))
      }
      if (formulario.current) enfocarPrimerError(formulario.current)
    }
  }

  return (
    <PaginaSinSesion titulo="Activar mi cuenta" texto="Escribe el código que te llegó por correo y elige tu contraseña."
      ocupado={enviando} alEnviar={enviar}>
      {rechazo && <Aviso tipo="error" asunto="No se activó la cuenta" mensaje={rechazo.mensaje} detalles={rechazo.detalles} />}
      {codigoNoVale && (
        <div className="codigo-vencido">
          <Aviso tipo="info" asunto="¿Se venció el código?"
            mensaje="Después de 24 horas o de 5 intentos el código ya no sirve. Pide uno nuevo: con él también activas tu cuenta." />
          <Link to="/recuperar" state={{ correo: correo.trim() }} className="boton boton-secundario boton-bloque">
            Pedir un código nuevo
          </Link>
        </div>
      )}
      <CampoCorreoEntrada valor={correo} error={errores.correo} deshabilitado={enviando}
        alCambiar={(v) => {
          setCorreo(v)
          setErrores({ ...errores, correo: undefined })
        }} />
      <CampoCodigo valor={codigo} error={errores.codigo} pista="Vale 24 horas desde que se creó tu cuenta."
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
        {enviando ? 'Activando…' : 'Activar mi cuenta'}
      </button>
    </PaginaSinSesion>
  )
}
