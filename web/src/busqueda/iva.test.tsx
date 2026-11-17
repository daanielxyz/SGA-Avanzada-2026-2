import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { ApartamentoPublico, Precio } from '../api/apartamentos'
import { SIN_EXTRAS } from '../api/reservas'
import Resumen from '../reservar/Resumen'
import DesglosePrecio from './DesglosePrecio'
import { MemoryRouter } from 'react-router'
import TarjetaApartamento from './TarjetaApartamento'
import { tarifaSinIva, valorEstancia, valorSinIva } from './iva'

// Dos noches de un adulto a 100.000 cada una
const NOCHES = [
  { noche: '2026-12-10', tarifa: 100000, ocupantesFacturables: 1, subtotal: 100000 },
  { noche: '2026-12-11', tarifa: 100000, ocupantesFacturables: 1, subtotal: 100000 },
]
const SIN_IVA: Precio = { desglose: NOCHES, total: 200000 }
// DEC-134: el 19 % se suma a las tarifas
const SUMADO: Precio = {
  desglose: NOCHES, total: 200000, alojamiento: 200000, ivaPorcentaje: 19, iva: 38000, ivaIncluido: false,
  totalConIva: 238000,
}
// DEC-141: las tarifas ya lo traen; base más IVA dan exacto el precio
const INCLUIDO: Precio = {
  desglose: NOCHES, total: 200000, alojamiento: 168067, ivaPorcentaje: 19, iva: 31933, ivaIncluido: true,
  totalConIva: 200000,
}

const SUITE: ApartamentoPublico = {
  codigo: 'APT-101', nombre: 'Suite Jardín', descripcion: null, capacidad: 2, dormitorios: 1, imagenes: [],
  caracteristicas: [], precio: null, proximaNocheLibre: null,
}

function resumen(precio: Precio) {
  render(<Resumen apartamento={SUITE} estadia={{ entrada: '2026-12-10', salida: '2026-12-12' }}
    grupo={{ adultos: 1, ninos: 0 }} precio={precio} extras={SIN_EXTRAS} />)
  return screen.getByRole('complementary')
}

/** El valor de una línea de la lista, por su nombre. */
function valorDe(caja: HTMLElement, nombre: string) {
  const termino = within(caja).getByText(nombre, { selector: 'dt' })
  // enPesos separa el signo con un espacio que no se parte
  return termino.nextElementSibling?.textContent?.replace(/\s/g, ' ')
}

describe('el IVA en el precio', () => {
  it('sin IVA, lo que se paga es la suma de las noches', () => {
    expect(valorEstancia(SIN_IVA)).toBe(200000)
    expect(tarifaSinIva(SIN_IVA, 100000)).toBe(100000)
  })

  it('si se suma, el total lo lleva y sin IVA quedan las tarifas', () => {
    expect(valorEstancia(SUMADO)).toBe(238000)
    expect(valorSinIva(SUMADO)).toBe(200000)
    expect(tarifaSinIva(SUMADO, 100000)).toBe(100000)
  })

  it('si ya va incluido, el total es el de las tarifas y sin IVA se les quita', () => {
    expect(valorEstancia(INCLUIDO)).toBe(200000)
    expect(valorSinIva(INCLUIDO)).toBe(168067)
    expect(tarifaSinIva(INCLUIDO, 100000)).toBe(84034)
  })

  it('el resumen dice cuánto es alojamiento, cuánto IVA y el total', () => {
    const caja = resumen(SUMADO)

    expect(valorDe(caja, 'Alojamiento')).toBe('$ 200.000')
    expect(valorDe(caja, 'IVA (19 %)')).toBe('$ 38.000')
    expect(within(caja).getByText(/Total 2 noches/)).toHaveTextContent('$ 238.000')
    expect(within(caja).getByText('Quien llega con pasaporte no paga IVA.')).toBeInTheDocument()
  })

  it('con el IVA incluido, el alojamiento va sin él y el total es el precio de las tarifas', () => {
    const caja = resumen(INCLUIDO)

    expect(valorDe(caja, 'Alojamiento')).toBe('$ 168.067')
    expect(valorDe(caja, 'IVA (19 %)')).toBe('$ 31.933')
    expect(within(caja).getByText(/Total 2 noches/)).toHaveTextContent('$ 200.000')
  })

  it('una reserva exenta lo dice en vez de cobrarlo', () => {
    const caja = resumen({ ...INCLUIDO, iva: 0, ivaExento: true, totalConIva: 168067 })

    expect(valorDe(caja, 'IVA (19 %)')).toBe('Exento')
    expect(within(caja).getByText(/Total 2 noches/)).toHaveTextContent('$ 168.067')
    expect(within(caja).getByText(/Sin IVA: quien llega con pasaporte no lo paga/)).toBeInTheDocument()
  })

  it('sin IVA, el resumen no muestra líneas de IVA', () => {
    const caja = resumen(SIN_IVA)

    expect(within(caja).queryByText(/IVA/)).not.toBeInTheDocument()
    expect(within(caja).getByText(/Total 2 noches/)).toHaveTextContent('$ 200.000')
  })

  it('el detalle por noche suma el IVA antes del total, o dice cuánto incluye', () => {
    const { unmount } = render(<DesglosePrecio precio={SUMADO} />)
    expect(screen.getByRole('rowheader', { name: 'IVA (19 %)' }).nextElementSibling).toHaveTextContent('$ 38.000')
    expect(screen.getByRole('rowheader', { name: 'Total' }).nextElementSibling).toHaveTextContent('$ 238.000')
    unmount()

    render(<DesglosePrecio precio={INCLUIDO} />)
    expect(screen.getByRole('rowheader', { name: 'Total' }).nextElementSibling).toHaveTextContent('$ 200.000')
    expect(screen.getByRole('rowheader', { name: 'Incluye IVA (19 %)' }).nextElementSibling)
      .toHaveTextContent('$ 31.933')
  })
})

describe('mientras se comparan apartamentos, el precio va sin IVA (DEC-141)', () => {
  function tarjeta(precio: Precio, conBusqueda = false) {
    render(
      <MemoryRouter>
        <TarjetaApartamento apartamento={{ ...SUITE, precio }} busqueda={conBusqueda
          ? { estadia: { entrada: '2026-12-10', salida: '2026-12-12' }, grupo: { adultos: 1, ninos: 0 } }
          : undefined} />
      </MemoryRouter>,
    )
    return screen.getByRole('article')
  }

  it('la tarifa por adulto sale sin el IVA que trae dentro, con «+ IVA»', () => {
    const fila = tarjeta(INCLUIDO)

    expect(fila).toHaveTextContent('$ 84.034')
    expect(fila).toHaveTextContent('por adulto y noche + IVA')
    expect(fila).not.toHaveTextContent('$ 100.000')
  })

  it('con fechas, el total del grupo también va sin IVA', () => {
    const fila = tarjeta(SUMADO, true)

    expect(fila).toHaveTextContent('$ 200.000')
    expect(fila).toHaveTextContent('+ IVA')
    expect(fila).not.toHaveTextContent('$ 238.000')
  })

  it('si el alojamiento no cobra IVA, no dice nada del IVA', () => {
    expect(tarjeta(SIN_IVA)).not.toHaveTextContent('IVA')
  })
})
