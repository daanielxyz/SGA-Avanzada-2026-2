// Entrar y salir del PMS (CU-02 · CU-48 · DEC-67 · DEC-71).
import type { Sesion } from '../sesion/almacen'
import { esRolDelPersonal } from '../sesion/roles'
import { ErrorApi, pedir } from './cliente'

/** `SesionResponse` del backend. */
interface SesionRespuesta {
  token: string
  tipo: string
  expiraEn: string
  usuario: string
  nombre: string
  rol: string
}

/**
 * `POST /api/auth/login` con el correo y la contraseña del personal.
 *
 * @throws ErrorApi 401 `CREDENCIALES_INVALIDAS` si el correo o la contraseña no sirven, 400 `VALIDACION` si falta
 *         alguno, o 403 `SIN_PERMISO` si la cuenta no es del personal
 */
export async function iniciarSesion(correo: string, contrasena: string): Promise<Sesion> {
  const { token, expiraEn, usuario, nombre, rol } = await pedir<SesionRespuesta>('/api/auth/login', {
    method: 'POST',
    cuerpo: { correo, contrasena },
  })
  if (!esRolDelPersonal(rol)) {
    throw new ErrorApi({
      estado: 403,
      codigo: 'SIN_PERMISO',
      mensaje: 'Esta cuenta no es del personal del alojamiento.',
      detalles: [],
    })
  }
  return { token, expiraEn, usuario, nombre, rol }
}

/** `POST /api/auth/logout`: el token deja de valer en el backend. */
export async function cerrarSesion(): Promise<void> {
  await pedir('/api/auth/logout', { method: 'POST' })
}
