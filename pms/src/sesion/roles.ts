// Roles del personal (ROL-01 · USU-03). El huésped no entra al PMS: tiene su propia web.

export const ROLES_DEL_PERSONAL = ['ADMINISTRADOR', 'RECEPCIONISTA', 'PERSONAL_SERVICIO'] as const

export type Rol = (typeof ROLES_DEL_PERSONAL)[number]

/** Cómo se le muestra cada rol a la persona. */
export const NOMBRE_DEL_ROL: Record<Rol, string> = {
  ADMINISTRADOR: 'Administración',
  RECEPCIONISTA: 'Recepción',
  PERSONAL_SERVICIO: 'Personal de servicio',
}

export function esRolDelPersonal(valor: string): valor is Rol {
  return (ROLES_DEL_PERSONAL as readonly string[]).includes(valor)
}
