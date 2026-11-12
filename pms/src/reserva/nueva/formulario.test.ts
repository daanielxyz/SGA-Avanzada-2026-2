import { describe, expect, it } from 'vitest'
import {
  ajustarGrupo,
  estimarExtras,
  mensajeDeCreada,
  OCUPANTE_VACIO,
  salidasDelRechazo,
  textoCobroVehiculo,
  validarEstancia,
  validarPersonas,
  type DatosOcupante,
  type DatosTitular,
} from './formulario'

const HOY = '2026-11-12'
const ENTRADA = '2026-11-14'
const TITULAR: DatosTitular = {
  nombre: 'Ana Ríos', tipoDocumento: 'CC', numeroDocumento: '52481', correo: 'ana@correo.test', telefono: '3105550142',
}
const DE_TITULAR: DatosOcupante = {
  nombre: 'Ana Ríos', fechaNacimiento: '1990-03-02', tipoDocumento: 'CC', numeroDocumento: '52481', nacionalidad: '',
}
const DE_TITULAR_ADULTO: DatosOcupante = { ...OCUPANTE_VACIO, nombre: 'Pedro Ríos', fechaNacimiento: '1985-01-01' }
const validar = (grupo: DatosOcupante[], titular = TITULAR) => validarPersonas(titular, grupo, ENTRADA, HOY)

describe('validarEstancia (EST-01 · RN-02)', () => {
  it('acepta una estancia correcta', () => {
    expect(validarEstancia({ entrada: HOY, salida: '2026-11-13', ocupantes: '1' }, HOY)).toEqual({})
  })

  it('rechaza una entrada pasada, una salida que no es posterior y un grupo vacío', () => {
    const errores = validarEstancia({ entrada: '2026-11-11', salida: '2026-11-11', ocupantes: '0' }, HOY)
    expect(Object.keys(errores).sort()).toEqual(['entrada', 'ocupantes', 'salida'])
  })
})

describe('validarPersonas', () => {
  it('acepta al titular entre los ocupantes', () => {
    expect(validar([DE_TITULAR])).toEqual({})
  })

  it('DEC-97: el titular puede no alojarse', () => {
    expect(validar([{ ...DE_TITULAR, nombre: 'Luis Ríos', numeroDocumento: '999' }])).toEqual({})
  })

  it('DEC-97: el grupo necesita un mayor de edad a la fecha de entrada; el que cumple 18 ese día ya cuenta', () => {
    expect(validar([{ ...DE_TITULAR, fechaNacimiento: '2016-11-15' }])['grupo.mayor']).toContain('mayor de edad')
    expect(validar([{ ...DE_TITULAR, fechaNacimiento: '2008-11-15' }])['grupo.mayor']).toBeDefined()
    expect(validar([{ ...DE_TITULAR, fechaNacimiento: '2008-11-14' }])).toEqual({})
    expect(validar([{ ...DE_TITULAR, fechaNacimiento: '2016-11-15' }, DE_TITULAR_ADULTO])).toEqual({})
  })

  it('sin todas las fechas no revisa la mayoría de edad: sale primero el error de la fecha', () => {
    const errores = validar([{ ...DE_TITULAR, fechaNacimiento: '' }])
    expect(errores['ocupante.0.fechaNacimiento']).toBe('Escribe la fecha de nacimiento.')
    expect(errores['grupo.mayor']).toBeUndefined()
  })

  it('OCU-05: dos ocupantes con el mismo documento son la misma persona', () => {
    const errores = validar([DE_TITULAR, { ...DE_TITULAR, nombre: 'Otro nombre' }])
    expect(errores['ocupante.1.numeroDocumento']).toBe('Ya está en la lista (ocupante 1).')
  })

  it('OCU-05: sin documento, se comparan el nombre y la fecha de nacimiento', () => {
    const sinDocumento = { ...OCUPANTE_VACIO, nombre: 'Luis', fechaNacimiento: '2010-01-01' }
    expect(validar([DE_TITULAR, sinDocumento, { ...sinDocumento, nombre: 'LUIS' }])['ocupante.2.nombre'])
      .toBe('Ya está en la lista (ocupante 2).')
    expect(validar([DE_TITULAR, sinDocumento, { ...sinDocumento, fechaNacimiento: '2011-01-01' }])).toEqual({})
  })

  it('un documento incompleto se marca: tipo sin número o número sin tipo', () => {
    const errores = validar([
      DE_TITULAR,
      { ...OCUPANTE_VACIO, nombre: 'Uno', fechaNacimiento: '2000-01-01', tipoDocumento: 'CC' },
      { ...OCUPANTE_VACIO, nombre: 'Dos', fechaNacimiento: '2000-01-01', numeroDocumento: '123' },
    ])
    expect(errores['ocupante.1.numeroDocumento']).toBe('Escribe el número.')
    expect(errores['ocupante.2.tipoDocumento']).toBe('Elige el tipo.')
  })

  it('una fecha de nacimiento futura se marca', () => {
    const errores = validar([DE_TITULAR, { ...OCUPANTE_VACIO, nombre: 'Uno', fechaNacimiento: '2026-11-13' }])
    expect(errores['ocupante.1.fechaNacimiento']).toBe('No puede ser una fecha futura.')
  })

  it('el titular necesita todos sus datos y un correo válido', () => {
    const errores = validar([DE_TITULAR], { ...TITULAR, nombre: ' ', correo: 'ana@', telefono: '' })
    expect(errores['titular.nombre']).toBeDefined()
    expect(errores['titular.correo']).toContain('correo válido')
    expect(errores['titular.telefono']).toBeDefined()
  })
})

describe('ajustarGrupo', () => {
  it('conserva lo escrito al crecer y al reducirse', () => {
    expect(ajustarGrupo([DE_TITULAR], 2)).toEqual([DE_TITULAR, OCUPANTE_VACIO])
    expect(ajustarGrupo([DE_TITULAR, OCUPANTE_VACIO], 1)).toEqual([DE_TITULAR])
  })
})

describe('mensajeDeCreada (RN-09 · RES-15)', () => {
  it('dice solo lo que falta para confirmar', () => {
    expect(mensajeDeCreada(false, true)).toContain('la hora de llegada y cubrir el anticipo')
    expect(mensajeDeCreada(true, true)).toContain('necesitas cubrir el anticipo')
    expect(mensajeDeCreada(false, false)).toContain('necesitas la hora de llegada')
    expect(mensajeDeCreada(true, false)).toBe('Queda pendiente y ya se puede confirmar.')
  })
})

describe('estimarExtras (DEC-89)', () => {
  const PARAMETROS = { cobroParqueadero: 'POR_NOCHE' as const, cargoCarro: 10000, cargoMoto: 5000, cargoMascota: 30000 }

  it('por noche, cada vehículo paga todas las noches; cada mascota, una vez', () => {
    expect(estimarExtras(PARAMETROS, { carros: 2, motos: 1, mascotas: 1 }, 3)).toEqual({
      parqueadero: 75000, mascotas: 30000, total: 105000,
    })
  })

  it('por estancia, cada vehículo paga una sola vez', () => {
    expect(estimarExtras({ ...PARAMETROS, cobroParqueadero: 'POR_ESTADIA' }, { carros: 2, motos: 1, mascotas: 0 }, 3))
      .toEqual({ parqueadero: 25000, mascotas: 0, total: 25000 })
  })

  it('sin vehículos ni mascotas no suma nada', () => {
    expect(estimarExtras(PARAMETROS, { carros: 0, motos: 0, mascotas: 0 }, 5).total).toBe(0)
  })

  it('dice cómo se cobra cada vehículo, o que no tiene costo', () => {
    expect(textoCobroVehiculo(10000, 'carro', 'POR_NOCHE')).toBe('$ 10.000 por carro cada noche.')
    expect(textoCobroVehiculo(5000, 'moto', 'POR_ESTADIA')).toBe('$ 5.000 por moto por estancia.')
    expect(textoCobroVehiculo(0, 'moto', 'POR_NOCHE')).toBe('Sin costo.')
  })
})

describe('salidasDelRechazo (DEC-94)', () => {
  it('ofrece solo lo que puede resolver cada rechazo', () => {
    expect(salidasDelRechazo('NO_DISPONIBLE')).toEqual(['apartamento', 'fechas'])
    expect(salidasDelRechazo('ESTANCIA_MINIMA')).toEqual(['fechas'])
    expect(salidasDelRechazo('TITULAR_INVALIDO')).toEqual(['personas'])
  })

  it('sin motivo, o con uno que no conoce, ofrece todas', () => {
    expect(salidasDelRechazo(null)).toEqual(['apartamento', 'fechas', 'personas'])
    expect(salidasDelRechazo('OTRO_NUEVO')).toEqual(['apartamento', 'fechas', 'personas'])
  })
})
