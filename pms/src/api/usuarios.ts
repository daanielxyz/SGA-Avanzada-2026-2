// Cuentas del personal: la lista del administrador, el perfil propio y fijar la contraseña con un código del correo
// (CU-03 · CU-04 · CU-05 · CU-06 · DEC-71).
import type { Rol } from '../sesion/roles'
import { pedir } from './cliente'
import type { Pagina } from './reservas'

export type TipoDocumentoPersonal = 'CC' | 'CE' | 'PASAPORTE' | 'TI'

/** `UsuarioResponse`. */
export interface Usuario {
  id: string
  nombre: string
  correo: string
  rol: Rol
  telefono: string | null
  tipoDocumento: TipoDocumentoPersonal | null
  numeroDocumento: string | null
  fechaNacimiento: string | null
  /** Ya fijó su contraseña con el código de activación. */
  activado: boolean
  eliminado: boolean
}

/** `DatosPersonalesRequest`: lo que cada persona cambia de sí misma. */
export interface DatosPersonales {
  nombre: string
  telefono: string
  tipoDocumento: TipoDocumentoPersonal
  numeroDocumento: string
  fechaNacimiento: string
}

/** `ModificarUsuarioRequest`: los datos más el rol, que solo cambia el administrador. */
export type DatosUsuario = DatosPersonales & { rol: Rol }

/** `CrearUsuarioRequest`: además, el correo, que después no se cambia. */
export type NuevoUsuario = DatosUsuario & { correo: string }

/** `GET /api/usuarios?pagina=`: todos, también los eliminados, por nombre y de 10 en 10. */
export function listarUsuarios(pagina: number): Promise<Pagina<Usuario>> {
  return pedir(`/api/usuarios?pagina=${pagina}`)
}

/** `POST /api/usuarios`: nace sin activar y le llega al correo el código para fijar su contraseña. */
export function crearUsuario(nuevo: NuevoUsuario): Promise<Usuario> {
  return pedir('/api/usuarios', { method: 'POST', cuerpo: nuevo })
}

/** `PUT /api/usuarios/{id}`: si cambia el rol, sus sesiones abiertas dejan de valer. */
export function modificarUsuario(id: string, datos: DatosUsuario): Promise<Usuario> {
  return pedir(`/api/usuarios/${encodeURIComponent(id)}`, { method: 'PUT', cuerpo: datos })
}

/** `PUT /api/usuarios/{id}/eliminar`: eliminación lógica; deja de entrar. */
export function eliminarUsuario(id: string): Promise<Usuario> {
  return pedir(`/api/usuarios/${encodeURIComponent(id)}/eliminar`, { method: 'PUT' })
}

/** `GET /api/usuarios/perfil`: la cuenta de quien tiene la sesión. */
export function consultarPerfil(): Promise<Usuario> {
  return pedir('/api/usuarios/perfil')
}

/** `PUT /api/usuarios/perfil`. */
export function actualizarPerfil(datos: DatosPersonales): Promise<Usuario> {
  return pedir('/api/usuarios/perfil', { method: 'PUT', cuerpo: datos })
}

/**
 * `PUT /api/usuarios/perfil/contrasena`. Todas sus sesiones, esta incluida, dejan de valer: hay que volver a entrar.
 *
 * @throws ErrorApi 401 `CREDENCIALES_INVALIDAS` si la actual no es la correcta (la sesión sigue abierta)
 */
export async function cambiarContrasena(actual: string, nueva: string): Promise<void> {
  await pedir('/api/usuarios/perfil/contrasena', { method: 'PUT', cuerpo: { actual, nueva } })
}

/** `POST /api/auth/activar`, sin sesión: fija la primera contraseña con el código de 24 horas. */
export async function activarCuenta(correo: string, codigo: string, contrasena: string): Promise<void> {
  await pedir('/api/auth/activar', { method: 'POST', cuerpo: { correo, codigo, contrasena } })
}

/**
 * `POST /api/auth/recuperar-contrasena`, sin sesión: responde igual exista o no el correo (USU-08). También le sirve a
 * una cuenta sin activar cuyo código venció.
 */
export async function pedirCodigoDeRecuperacion(correo: string): Promise<void> {
  await pedir('/api/auth/recuperar-contrasena', { method: 'POST', cuerpo: { correo } })
}

/** `POST /api/auth/restablecer-contrasena`, sin sesión: fija la contraseña con el código de 15 minutos. */
export async function restablecerContrasena(correo: string, codigo: string, contrasena: string): Promise<void> {
  await pedir('/api/auth/restablecer-contrasena', { method: 'POST', cuerpo: { correo, codigo, contrasena } })
}
