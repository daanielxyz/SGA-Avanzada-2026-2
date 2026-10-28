import { describe, expect, it } from 'vitest'
import { menuPara } from './navegacion'

function titulos(rol: Parameters<typeof menuPara>[0]) {
  return menuPara(rol).flatMap(({ opciones }) => opciones.map((o) => o.titulo))
}

describe('menú por rol (PermisosPorRol · DEC-67)', () => {
  it('recepción ve reservas y titulares, no la operación ni la configuración', () => {
    expect(menuPara('RECEPCIONISTA').map((g) => g.grupo)).toEqual(['Recepción', 'Mi cuenta'])
    expect(titulos('RECEPCIONISTA')).toEqual(['Reservas', 'Titulares', 'Mi perfil'])
  })

  it('el personal de servicio ve la preparación y las novedades, no las reservas', () => {
    expect(titulos('PERSONAL_SERVICIO')).toEqual(['Preparación de apartamentos', 'Novedades', 'Mi perfil'])
  })

  it('el administrador lo ve todo, en el orden del menú', () => {
    expect(menuPara('ADMINISTRADOR').map((g) => g.grupo))
      .toEqual(['Recepción', 'Operación', 'Configuración', 'Mi cuenta'])
    expect(titulos('ADMINISTRADOR')).toContain('Usuarios')
  })
})
