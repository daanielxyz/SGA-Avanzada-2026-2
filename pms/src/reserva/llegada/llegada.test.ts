import { describe, expect, it } from 'vitest'
import type { Folio } from '../../api/folio'
import type { Ocupante } from '../../api/reservas'
import { reservaDePrueba } from '../../pruebas/reservasDePrueba'
import {
  bloqueoDe,
  faltaPorCobrar,
  puedeSerPrincipal,
  siguienteConFaltantes,
  textoDelBoton,
  textoTra,
  titularSeAloja,
  type Situacion,
} from './llegada'

const FOLIO: Folio = {
  id: 'FOL-1', reserva: 'RES-2026-00042', cargos: [], totalPagado: 300000, saldo: 0, situacionSaldo: 'PENDIENTE', cerrado: false,
}

const LISTA: Situacion = { estadoApartamento: 'PREPARADO', falta: 0, faltaAnticipo: 0, antesDeEntrada: false, sinCobro: false, motivo: '' }

function ocupante(cambios: Partial<Ocupante>): Ocupante {
  return {
    id: 'OCU-1', nombre: 'Ana Torres', fechaNacimiento: '1988-03-14', tipoDocumento: 'CC', numeroDocumento: '1094',
    nacionalidad: 'Colombia', datosTraFaltantes: [], ...cambios,
  }
}

describe('registro de llegada (CU-21)', () => {
  it('lo que falta por cobrar es la estancia y sus extras menos lo pagado, nunca negativo (BL-22)', () => {
    const reserva = reservaDePrueba({ extras: { parqueadero: 45000, mascotas: 30000, total: 75000 } })
    expect(faltaPorCobrar(reserva, FOLIO)).toBe(1250000 + 75000 - 300000)
    expect(faltaPorCobrar(reserva, { ...FOLIO, totalPagado: 2000000 })).toBe(0)
  })

  it('el bloqueo sale en el orden en que recepción lo resuelve: fecha, apartamento, anticipo y pago (RN-10 · RN-11)', () => {
    expect(bloqueoDe({ ...LISTA, antesDeEntrada: true, estadoApartamento: 'OCUPADO' })).toBe('fecha')
    expect(bloqueoDe({ ...LISTA, estadoApartamento: 'EN_PREPARACION', falta: 10 })).toBe('apartamento')
    expect(bloqueoDe({ ...LISTA, faltaAnticipo: 5, falta: 10, sinCobro: true, motivo: 'Empresa' })).toBe('anticipo')
    expect(bloqueoDe({ ...LISTA, falta: 10 })).toBe('pago')
    expect(bloqueoDe({ ...LISTA, falta: 10, sinCobro: true, motivo: '   ' })).toBe('pago')
    expect(bloqueoDe({ ...LISTA, falta: 10, sinCobro: true, motivo: 'Cuenta por cobrar a una empresa' })).toBeNull()
    expect(bloqueoDe(LISTA)).toBeNull()
  })

  it('si no se pudo consultar el apartamento no bloquea: decide el backend', () => {
    expect(bloqueoDe({ ...LISTA, estadoApartamento: null })).toBeNull()
  })

  it('el botón dice qué va a pasar', () => {
    const confirmada = reservaDePrueba({ estado: 'CONFIRMADA', entrada: '2026-11-15' })
    expect(textoDelBoton(confirmada, LISTA)).toBe('Registrar llegada')
    expect(textoDelBoton(confirmada, { ...LISTA, falta: 10, sinCobro: true, motivo: 'Empresa' })).toBe('Registrar llegada sin cobro')
    expect(textoDelBoton(confirmada, { ...LISTA, antesDeEntrada: true })).toBe('Disponible desde 15 nov')
    expect(textoDelBoton({ ...confirmada, estado: 'PENDIENTE' }, LISTA)).toBe('Confirmar y registrar llegada')
  })

  it('cuenta los datos del TRA que faltan (DEC-96)', () => {
    expect(textoTra(ocupante({}))).toBe('Completo')
    expect(textoTra(ocupante({ datosTraFaltantes: ['GENERO'] }))).toBe('Falta 1 dato')
    expect(textoTra(ocupante({ datosTraFaltantes: ['GENERO', 'MOTIVO_VIAJE'] }))).toBe('Faltan 2 datos')
  })

  it('solo un mayor de edad a la fecha de entrada que no es ya el principal puede serlo (DEC-97)', () => {
    expect(puedeSerPrincipal(ocupante({}), '2026-11-12')).toBe(true)
    expect(puedeSerPrincipal(ocupante({ principal: true }), '2026-11-12')).toBe(false)
    expect(puedeSerPrincipal(ocupante({ fechaNacimiento: '2008-11-13' }), '2026-11-12')).toBe(false)
    expect(puedeSerPrincipal(ocupante({ fechaNacimiento: '2008-11-12' }), '2026-11-12')).toBe(true)
    expect(puedeSerPrincipal(ocupante({ fechaNacimiento: null }), '2026-11-12')).toBe(false)
  })

  it('el titular se aloja si su documento es el de un huésped (DEC-98)', () => {
    const grupo = [ocupante({})]
    expect(titularSeAloja({ tipoDocumento: 'CC', numeroDocumento: '1094' }, grupo)).toBe(true)
    expect(titularSeAloja({ tipoDocumento: 'CC', numeroDocumento: '7955' }, grupo)).toBe(false)
  })

  it('después de guardar sigue con el próximo huésped al que le falte algo, dando la vuelta', () => {
    const grupo = [
      ocupante({ id: 'A', datosTraFaltantes: ['GENERO'] }),
      ocupante({ id: 'B' }),
      ocupante({ id: 'C', datosTraFaltantes: ['GENERO'] }),
    ]
    expect(siguienteConFaltantes(grupo, 'A')?.id).toBe('C')
    expect(siguienteConFaltantes(grupo, 'C')?.id).toBe('A')
    expect(siguienteConFaltantes([ocupante({ id: 'A' }), ocupante({ id: 'B' })], 'A')).toBeNull()
  })

  it('no vuelve a un huésped ya visto en el diálogo, aunque haya quedado a medias', () => {
    const grupo = [
      ocupante({ id: 'A', datosTraFaltantes: ['GENERO'] }),
      ocupante({ id: 'B', datosTraFaltantes: ['GENERO'] }),
    ]
    expect(siguienteConFaltantes(grupo, 'A', ['A'])?.id).toBe('B')
    expect(siguienteConFaltantes(grupo, 'B', ['A', 'B'])).toBeNull()
  })
})
