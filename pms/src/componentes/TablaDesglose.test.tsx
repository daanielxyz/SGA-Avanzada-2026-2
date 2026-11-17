import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { Valor } from '../api/reservas'
import TablaDesglose from './TablaDesglose'

// Dos noches de $ 100.000
const NOCHES: Valor['desglose'] = [
  { noche: '2026-12-10', temporada: 'TEM-1', temporadaNombre: 'Base', tarifa: 100000, ocupantesFacturables: 1, subtotal: 100000 },
  { noche: '2026-12-11', temporada: 'TEM-1', temporadaNombre: 'Base', tarifa: 100000, ocupantesFacturables: 1, subtotal: 100000 },
]

function abrir(valor: Valor) {
  render(<><h2 id="titulo">Valor</h2><TablaDesglose idTitulo="titulo" valor={valor} /></>)
}

/** El valor de una línea por su nombre, con el espacio del signo de pesos normal. */
function valorDe(nombre: string) {
  return screen.getByText(nombre, { selector: 'dt' }).nextElementSibling?.textContent?.replace(/\s/g, ' ')
}

describe('el desglose del valor de una reserva', () => {
  it('DEC-134: si el IVA se suma, el alojamiento son las noches y el total lo lleva', () => {
    abrir({ desglose: NOCHES, total: 200000, alojamiento: 200000, ivaPorcentaje: 19, iva: 38000, totalConIva: 238000 })

    expect(valorDe('Alojamiento')).toBe('$ 200.000')
    expect(valorDe('IVA (19 %)')).toBe('$ 38.000')
    expect(screen.getByText('Total').nextElementSibling).toHaveTextContent('$ 238.000')
  })

  it('DEC-141: si las tarifas ya lo incluyen, el alojamiento va sin él y el total es el de las noches', () => {
    abrir({
      desglose: NOCHES, total: 200000, alojamiento: 168067, ivaPorcentaje: 19, iva: 31933, ivaIncluido: true,
      totalConIva: 200000,
    })

    expect(valorDe('Alojamiento')).toBe('$ 168.067')
    expect(valorDe('IVA (19 %)')).toBe('$ 31.933')
    expect(screen.getByText('Total').nextElementSibling).toHaveTextContent('$ 200.000')
  })

  it('una respuesta sin el alojamiento aparte usa las noches', () => {
    abrir({ desglose: NOCHES, total: 200000, ivaPorcentaje: 19, iva: 38000, totalConIva: 238000 })

    expect(valorDe('Alojamiento')).toBe('$ 200.000')
  })
})
