// Menú del PMS: cada opción la ve solo el rol que la puede usar, con la misma tabla del backend (PermisosPorRol,
// DEC-67). Si el backend cambia un permiso, se cambia aquí también; el backend sigue siendo quien decide (403).
import type { NombreIcono } from './componentes/Icono'
import type { Rol } from './sesion/roles'

export type Grupo = 'Recepción' | 'Operación' | 'Configuración' | 'Mi cuenta'

export interface Opcion {
  titulo: string
  /** La línea que la explica en los accesos de la bienvenida. */
  descripcion: string
  icono: NombreIcono
  ruta: string
  grupo: Grupo
  roles: readonly Rol[]
}

export const RECEPCION: readonly Rol[] = ['RECEPCIONISTA', 'ADMINISTRADOR']
const OPERACION: readonly Rol[] = ['PERSONAL_SERVICIO', 'ADMINISTRADOR']
const ADMINISTRACION: readonly Rol[] = ['ADMINISTRADOR']
const TODOS: readonly Rol[] = ['RECEPCIONISTA', 'PERSONAL_SERVICIO', 'ADMINISTRADOR']

export const OPCIONES: readonly Opcion[] = [
  {
    titulo: 'Reservas',
    descripcion: 'Consulta y revisa las reservas.',
    icono: 'reservas',
    ruta: '/reservas',
    grupo: 'Recepción',
    roles: RECEPCION,
  },
  {
    titulo: 'Ocupación',
    descripcion: 'Qué apartamentos están ocupados cada noche.',
    icono: 'ocupacion',
    ruta: '/ocupacion',
    grupo: 'Recepción',
    roles: RECEPCION,
  },
  {
    titulo: 'Titulares',
    descripcion: 'Datos de quienes reservan.',
    icono: 'titulares',
    ruta: '/titulares',
    grupo: 'Recepción',
    roles: RECEPCION,
  },
  {
    titulo: 'Preparación de apartamentos',
    descripcion: 'Lo que toca limpiar y preparar.',
    icono: 'preparacion',
    ruta: '/preparacion',
    grupo: 'Operación',
    roles: OPERACION,
  },
  {
    titulo: 'Novedades',
    descripcion: 'Lo que hay que atender en los apartamentos.',
    icono: 'novedades',
    ruta: '/novedades',
    grupo: 'Operación',
    roles: OPERACION,
  },
  {
    titulo: 'Alojamiento',
    descripcion: 'Datos, parámetros y servicios.',
    icono: 'alojamiento',
    ruta: '/alojamiento',
    grupo: 'Configuración',
    roles: ADMINISTRACION,
  },
  {
    titulo: 'Apartamentos',
    descripcion: 'Capacidad, características y fotos.',
    icono: 'apartamentos',
    ruta: '/apartamentos',
    grupo: 'Configuración',
    roles: ADMINISTRACION,
  },
  {
    titulo: 'Temporadas y tarifas',
    descripcion: 'Temporadas y valor por noche.',
    icono: 'tarifas',
    ruta: '/tarifas',
    grupo: 'Configuración',
    roles: ADMINISTRACION,
  },
  {
    titulo: 'Política de cancelación',
    descripcion: 'Plazos y valores al cancelar.',
    icono: 'politica',
    ruta: '/politica',
    grupo: 'Configuración',
    roles: ADMINISTRACION,
  },
  {
    titulo: 'Usuarios',
    descripcion: 'Cuentas y roles del personal.',
    icono: 'usuarios',
    ruta: '/usuarios',
    grupo: 'Configuración',
    roles: ADMINISTRACION,
  },
  {
    titulo: 'Canales de venta',
    descripcion: 'Canales externos y sus conflictos.',
    icono: 'canales',
    ruta: '/canales',
    grupo: 'Configuración',
    roles: ADMINISTRACION,
  },
  {
    titulo: 'Mi perfil',
    descripcion: 'Tu cuenta y tus datos.',
    icono: 'perfil',
    ruta: '/perfil',
    grupo: 'Mi cuenta',
    roles: TODOS,
  },
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
