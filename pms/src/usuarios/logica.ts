// Reglas de los formularios de cuentas (pms/DISENO.md, «Sprint 4: usuarios»). Las mismas del backend
// (`CrearUsuarioRequest`, `DatosPersonalesRequest`, `PoliticaContrasena`), dichas antes de enviar.
import type { ErrorApi } from '../api/cliente'
import type { DatosPersonales, TipoDocumentoPersonal, Usuario } from '../api/usuarios'
import { fechaHoy } from '../formato'
import type { Rol } from '../sesion/roles'

export const MAXIMO_NOMBRE = 120
export const MAXIMO_CORREO = 150
export const MAXIMO_TELEFONO = 30
export const MAXIMO_DOCUMENTO = 30

export const TIPOS_DOCUMENTO: readonly (readonly [TipoDocumentoPersonal | '', string])[] = [
  ['', 'Elige'], ['CC', 'CC'], ['CE', 'CE'], ['PASAPORTE', 'Pasaporte'], ['TI', 'TI'],
]

/** Los roles en el orden del diálogo, con lo que puede hacer cada uno (el menú de `navegacion.ts`). */
export const ROLES: readonly { rol: Rol; nombre: string; explica: string }[] = [
  { rol: 'RECEPCIONISTA', nombre: 'Recepción', explica: 'Atiende las reservas, la ocupación, las llegadas y los titulares.' },
  { rol: 'PERSONAL_SERVICIO', nombre: 'Personal de servicio',
    explica: 'Ve los apartamentos por preparar y las novedades, y los marca como preparados.' },
  { rol: 'ADMINISTRADOR', nombre: 'Administración',
    explica: 'Todo lo de recepción y servicio, más la configuración del alojamiento y los usuarios.' },
]

export const REGLA_CONTRASENA = 'Al menos 8 caracteres, una mayúscula y un número.'

export type EstadoCuenta = 'activa' | 'sin-activar' | 'eliminada'

export function estadoCuenta(usuario: Pick<Usuario, 'activado' | 'eliminado'>): EstadoCuenta {
  if (usuario.eliminado) return 'eliminada'
  return usuario.activado ? 'activa' : 'sin-activar'
}

/** Lo que el formulario guarda mientras se escribe: todo como texto, el tipo de documento puede faltar. */
export interface FormPersona {
  nombre: string
  telefono: string
  tipoDocumento: TipoDocumentoPersonal | ''
  numeroDocumento: string
  fechaNacimiento: string
}

export type CampoPersona = keyof FormPersona
export type CampoUsuario = CampoPersona | 'correo' | 'rol'
export type Errores<C extends string> = Partial<Record<C, string>>

export const PERSONA_VACIA: FormPersona = { nombre: '', telefono: '', tipoDocumento: '', numeroDocumento: '', fechaNacimiento: '' }

export function formDe(usuario: Usuario): FormPersona {
  return {
    nombre: usuario.nombre,
    telefono: usuario.telefono ?? '',
    tipoDocumento: usuario.tipoDocumento ?? '',
    numeroDocumento: usuario.numeroDocumento ?? '',
    fechaNacimiento: usuario.fechaNacimiento ?? '',
  }
}

/** Los datos personales; los cinco son obligatorios en la API, y la fecha de nacimiento es anterior a hoy. */
export function validarPersona(form: FormPersona, hoy = fechaHoy()): Errores<CampoPersona> {
  const errores: Errores<CampoPersona> = {}
  if (!form.nombre.trim()) errores.nombre = 'Escribe el nombre.'
  if (!form.telefono.trim()) errores.telefono = 'Escribe el teléfono.'
  if (!form.tipoDocumento) errores.tipoDocumento = 'Elige el tipo de documento.'
  if (!form.numeroDocumento.trim()) errores.numeroDocumento = 'Escribe el número de documento.'
  if (!form.fechaNacimiento) errores.fechaNacimiento = 'Elige la fecha de nacimiento.'
  else if (form.fechaNacimiento >= hoy) errores.fechaNacimiento = 'La fecha de nacimiento tiene que ser anterior a hoy.'
  return errores
}

// Lo mínimo para que el correo sirva: algo, una arroba, un dominio con punto. El backend revisa lo demás
const FORMA_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function errorDeCorreo(correo: string): string | undefined {
  if (!correo.trim()) return 'Escribe el correo.'
  if (!FORMA_CORREO.test(correo.trim())) return 'Escribe un correo válido, por ejemplo nombre@dominio.co.'
  return undefined
}

export function cuerpoPersona(form: FormPersona): DatosPersonales {
  return {
    nombre: form.nombre.trim(),
    telefono: form.telefono.trim(),
    tipoDocumento: form.tipoDocumento as TipoDocumentoPersonal,
    numeroDocumento: form.numeroDocumento.trim(),
    fechaNacimiento: form.fechaNacimiento,
  }
}

/** Lo que le falta a una contraseña para cumplir la regla (`PoliticaContrasena`), o nada si la cumple. */
export function errorDeContrasena(contrasena: string): string | undefined {
  if (!contrasena) return 'Escribe la contraseña nueva.'
  const faltas = [
    contrasena.length < 8 && 'al menos 8 caracteres',
    !/\p{Lu}/u.test(contrasena) && 'una mayúscula',
    !/\d/.test(contrasena) && 'un número',
  ].filter(Boolean) as string[]
  if (faltas.length === 0) return undefined
  const lista = faltas.length === 1 ? faltas[0] : `${faltas.slice(0, -1).join(', ')} y ${faltas[faltas.length - 1]}`
  return `Le falta: ${lista}.`
}

export function errorDeRepetida(nueva: string, repetida: string): string | undefined {
  if (!repetida) return 'Repite la contraseña nueva.'
  return repetida === nueva ? undefined : 'Las dos contraseñas no son iguales.'
}

export function errorDeCodigo(codigo: string): string | undefined {
  if (!codigo) return 'Escribe el código que te llegó al correo.'
  return /^\d{6}$/.test(codigo) ? undefined : 'El código tiene 6 números.'
}

/** Solo los números de lo que se pega o se escribe en el código: un correo puede traerlo con espacios. */
export function codigoAlEscribir(texto: string): string {
  return texto.replace(/\D/g, '').slice(0, 6)
}

/** El campo de un detalle de un 400 («telefono: es obligatorio»), si es de este formulario. */
export function campoDelDetalle<C extends string>(detalle: string, campos: readonly C[]): C | null {
  const campo = detalle.split(':')[0].trim()
  return (campos as readonly string[]).includes(campo) ? (campo as C) : null
}

/** «Es obligatorio» a partir de «telefono: es obligatorio». */
export function mensajeDelDetalle(detalle: string): string {
  const separador = detalle.indexOf(': ')
  const mensaje = separador < 0 ? detalle : detalle.slice(separador + 2)
  return mensaje.charAt(0).toUpperCase() + mensaje.slice(1)
}

export function hayErrores(errores: object): boolean {
  return Object.values(errores).some(Boolean)
}

/** Cómo se nombra a alguien en un aviso: «Carlos» o, con tres palabras o más, «María Fernanda». */
export function nombreCorto(nombre: string): string {
  const partes = nombre.trim().split(/\s+/)
  return partes.length >= 3 ? partes.slice(0, 2).join(' ') : partes[0]
}

export type CampoConCodigo = 'correo' | 'codigo' | 'contrasena'

/** El campo de un detalle de un 400 de `FijarContrasenaRequest`. */
export function campoDeFijar(error: ErrorApi): Partial<Record<CampoConCodigo, string>> {
  const errores: Partial<Record<CampoConCodigo, string>> = {}
  for (const detalle of error.detalles) {
    const campo = detalle.split(':')[0].trim()
    if (campo === 'correo') errores.correo = 'Escribe un correo válido, por ejemplo nombre@dominio.co.'
    if (campo === 'codigo') errores.codigo = 'El código tiene 6 números.'
    if (campo === 'contrasena') errores.contrasena = 'Le falta: al menos 8 caracteres, una mayúscula y un número.'
  }
  return errores
}
