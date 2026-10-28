// Menú del PMS: cada opción la ve solo el rol que la puede usar, con la misma tabla del backend (PermisosPorRol,
// DEC-67). Si el backend cambia un permiso, se cambia aquí también; el backend sigue siendo quien decide (403).
import type { Rol } from './sesion/roles'

export type Grupo = 'Recepción' | 'Operación' | 'Configuración' | 'Mi cuenta'

export interface Opcion {
  titulo: string
  ruta: string
  grupo: Grupo
  roles: readonly Rol[]
}

const RECEPCION: readonly Rol[] = ['RECEPCIONISTA', 'ADMINISTRADOR']
const OPERACION: readonly Rol[] = ['PERSONAL_SERVICIO', 'ADMINISTRADOR']
const ADMINISTRACION: readonly Rol[] = ['ADMINISTRADOR']
const TODOS: readonly Rol[] = ['RECEPCIONISTA', 'PERSONAL_SERVICIO', 'ADMINISTRADOR']

export const OPCIONES: readonly Opcion[] = [
  { titulo: 'Reservas', ruta: '/reservas', grupo: 'Recepción', roles: RECEPCION },
  { titulo: 'Titulares', ruta: '/titulares', grupo: 'Recepción', roles: RECEPCION },
  { titulo: 'Preparación de apartamentos', ruta: '/preparacion', grupo: 'Operación', roles: OPERACION },
  { titulo: 'Novedades', ruta: '/novedades', grupo: 'Operación', roles: OPERACION },
  { titulo: 'Alojamiento', ruta: '/alojamiento', grupo: 'Configuración', roles: ADMINISTRACION },
  { titulo: 'Apartamentos', ruta: '/apartamentos', grupo: 'Configuración', roles: ADMINISTRACION },
  { titulo: 'Temporadas y tarifas', ruta: '/tarifas', grupo: 'Configuración', roles: ADMINISTRACION },
  { titulo: 'Política de cancelación', ruta: '/politica', grupo: 'Configuración', roles: ADMINISTRACION },
  { titulo: 'Usuarios', ruta: '/usuarios', grupo: 'Configuración', roles: ADMINISTRACION },
  { titulo: 'Canales de venta', ruta: '/canales', grupo: 'Configuración', roles: ADMINISTRACION },
  { titulo: 'Mi perfil', ruta: '/perfil', grupo: 'Mi cuenta', roles: TODOS },
]

/** Las opciones que ve un rol, agrupadas en el orden del menú. */
export function menuPara(rol: Rol): { grupo: Grupo; opciones: Opcion[] }[] {
  const grupos: { grupo: Grupo; opciones: Opcion[] }[] = []
  for (const opcion of OPCIONES.filter((o) => o.roles.includes(rol))) {
    const grupo = grupos.find((g) => g.grupo === opcion.grupo)
    if (grupo) grupo.opciones.push(opcion)
    else grupos.push({ grupo: opcion.grupo, opciones: [opcion] })
  }
  return grupos
}
