import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Alojamiento } from '../api/alojamiento'
import type { ApartamentoPublico, Pagina } from '../api/apartamentos'
import type { PoliticaCancelacion } from '../api/reservas'
import Inicio from './Inicio'

function apartamento(codigo: string, nombre: string, foto?: string): ApartamentoPublico {
  return {
    codigo, nombre, descripcion: null, capacidad: 4, dormitorios: 2,
    imagenes: foto ? [{ url: foto, principal: true }] : [], caracteristicas: [],
    precio: { desglose: [{ noche: '2026-12-14', tarifa: 100000, ocupantesFacturables: 1, subtotal: 100000 }], total: 100000 },
    proximaNocheLibre: '2026-12-14',
  }
}

function pagina(contenido: ApartamentoPublico[], totalElementos = contenido.length): Pagina<ApartamentoPublico> {
  return { contenido, numero: 0, tamano: 10, totalElementos, totalPaginas: 1 }
}

const POLITICA: PoliticaCancelacion = {
  tramos: [
    { antelacionMinHoras: 48, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 0, montoFijo: null } },
    { antelacionMinHoras: 0, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 50, montoFijo: null } },
  ],
  penalizacionNoShow: { base: 'VALOR_TOTAL', porcentaje: 100, montoFijo: null },
}

const HOTEL: Alojamiento = {
  nombre: 'Hotel de Prueba', ciudad: 'Salento', descripcion: 'Descansa entre cafetales', direccion: 'Calle 5 # 3-10',
  latitud: 4.637, longitud: -75.571, normas: 'No fumar en las habitaciones', mediosPago: ['EFECTIVO', 'PAGO_EN_LINEA'],
  rnt: '12345',
  serviciosAdicionales: [
    { id: 'SRV-1', nombre: 'Parqueadero', generaCargo: false, valor: 0, activo: true },
    { id: 'SRV-2', nombre: 'Desayuno', generaCargo: true, valor: 25000, activo: true },
    { id: 'SRV-3', nombre: 'Lavandería', generaCargo: true, valor: 15000, activo: false },
  ],
  parametros: {
    umbralEdadFacturable: 12, horaEntrada: '15:00:00', horaSalida: '11:00:00', cupoParqueadero: 8,
    cobroParqueadero: 'POR_NOCHE', cargoCarro: 0, cargoMoto: 0, aceptaMascotas: true, limiteMascotas: 2,
    cargoMascota: 30000,
  },
}

/** La API simulada por ruta: los apartamentos y la política; con `null`, esa llamada falla. */
function laApiResponde(apartamentos: Pagina<ApartamentoPublico> | null, politica: PoliticaCancelacion | null = POLITICA) {
  const fetch = vi.fn((ruta: string) => {
    const cuerpo = ruta.startsWith('/api/portal/politica') ? politica : apartamentos
    return Promise.resolve(cuerpo === null
      ? new Response(JSON.stringify({ estado: 500, mensaje: 'falla' }), { status: 500 })
      : new Response(JSON.stringify(cuerpo), { status: 200 }))
  })
  vi.stubGlobal('fetch', fetch)
  return fetch
}

function DondeQuedo() {
  const { pathname, search } = useLocation()
  return <p>En {pathname}{search}</p>
}

function abrir(alojamiento: Alojamiento | null = HOTEL) {
  render(
    <MemoryRouter>
      <Routes>
        <Route path="/" element={<Inicio alojamiento={alojamiento} />} />
        <Route path="*" element={<DondeQuedo />} />
      </Routes>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

afterEach(() => vi.unstubAllGlobals())

describe('Presentación del hotel', () => {
  it('abre con el hotel: su nombre, ciudad, descripción y su portada (DEC-83)', async () => {
    laApiResponde(pagina([apartamento('APT-101', 'Suite Jardín', 'https://cdn.sga.co/suite.jpg')]))
    abrir({ ...HOTEL, marca: { portadaUrl: 'https://cdn.sga.co/portada.jpg' } })

    expect(screen.getByRole('heading', { level: 1, name: 'Hotel de Prueba' })).toBeInTheDocument()
    expect(screen.getAllByText('Salento')[0]).toBeInTheDocument()
    expect(screen.getByText('Descansa entre cafetales')).toBeInTheDocument()
    await screen.findByRole('article', { name: 'Suite Jardín' })
    expect(document.querySelector('.portada img')).toHaveAttribute('src', 'https://cdn.sga.co/portada.jpg')
  })

  it('sin portada propia usa la foto de un apartamento, y no muestra los valores que el hotel no ha escrito', async () => {
    laApiResponde(pagina([apartamento('APT-101', 'Suite Jardín', 'https://cdn.sga.co/suite.jpg')]))
    abrir({ ...HOTEL, ciudad: 'Por definir', descripcion: 'Por definir', direccion: 'Por definir', latitud: 0, longitud: 0 })

    await screen.findByRole('article', { name: 'Suite Jardín' })
    expect(document.querySelector('.portada img')).toHaveAttribute('src', 'https://cdn.sga.co/suite.jpg')
    expect(screen.queryByText('Por definir')).not.toBeInTheDocument()
    expect(screen.getByText(/Elige tus fechas y mira qué apartamentos están libres/)).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Cómo llegar' })).not.toBeInTheDocument()
  })

  it('el buscador lleva a los apartamentos libres con las fechas y el grupo en la dirección', async () => {
    laApiResponde(pagina([]))
    const persona = abrir()

    await persona.click(screen.getByRole('button', { name: /¿Cuándo vienes\?/ }))
    await persona.click(screen.getByRole('button', { name: 'Ver apartamentos libres' }))

    const destino = await screen.findByText(/^En \/apartamentos\?/)
    expect(destino.textContent).toMatch(/entrada=\d{4}-\d{2}-\d{2}&salida=\d{4}-\d{2}-\d{2}&adultos=2&ninos=0$/)
  })

  it('muestra hasta tres apartamentos, con su precio, y ofrece ver todos', async () => {
    const cuatro = ['A', 'B', 'C', 'D'].map((n, i) => apartamento(`APT-10${i}`, `Apartamento ${n}`))
    laApiResponde(pagina(cuatro, 12))
    abrir()

    const destacados = await screen.findByRole('region', { name: 'Dónde te puedes quedar' })
    expect(within(destacados).getAllByRole('article')).toHaveLength(3)
    expect(within(destacados).getAllByText(/\$\s*100\.000/)).toHaveLength(3)
    expect(within(destacados).getByRole('link', { name: 'Apartamento A' }))
      .toHaveAttribute('href', '/apartamentos/APT-100?entrada=2026-12-14&salida=2026-12-15')
    expect(within(destacados).getByRole('link', { name: 'Ver los 12 apartamentos' })).toHaveAttribute('href', '/apartamentos')
  })

  it('sin apartamentos, o si la lista falla, esa sección no aparece y se puede seguir reservando', async () => {
    laApiResponde(null, null)
    abrir()

    expect(await screen.findByRole('link', { name: 'Ver apartamentos' })).toHaveAttribute('href', '/apartamentos')
    expect(screen.queryByRole('region', { name: 'Dónde te puedes quedar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Si cambian tus planes' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /¿Cuándo vienes\?/ })).toBeInTheDocument()
  })


  it('presenta el hotel: su descripción, sus cifras, lo más común en sus apartamentos y fotos que no salieron arriba', async () => {
    const conCosas = (a: ApartamentoPublico, capacidad: number, caracteristicas: string[]) =>
      ({ ...a, capacidad, caracteristicas })
    laApiResponde(pagina([
      conCosas(apartamento('APT-1', 'Uno', 'https://cdn.sga.co/1.jpg'), 2, ['Wifi', 'Cocina']),
      conCosas(apartamento('APT-2', 'Dos', 'https://cdn.sga.co/2.jpg'), 6, ['Wifi', 'Balcón']),
      conCosas(apartamento('APT-3', 'Tres', 'https://cdn.sga.co/3.jpg'), 4, ['Wifi', 'Cocina']),
      conCosas(apartamento('APT-4', 'Cuatro', 'https://cdn.sga.co/4.jpg'), 3, ['Jacuzzi']),
    ], 12))
    abrir()

    const sobre = await screen.findByRole('region', { name: 'Sobre Hotel de Prueba' })
    expect(within(sobre).getByText('Descansa entre cafetales')).toBeInTheDocument()
    expect(within(sobre).getByText('apartamentos').nextSibling).toHaveTextContent('12')
    expect(within(sobre).getByText('personas en un apartamento').nextSibling).toHaveTextContent('Hasta 6')
    expect(within(sobre).getByText('ubicación').nextSibling).toHaveTextContent('Salento')
    expect(within(sobre).getAllByRole('listitem').map((li) => li.textContent))
      .toEqual(['Wifi', 'Cocina', 'Balcón', 'Jacuzzi'])
    // La del cuarto apartamento va primero: los tres primeros ya se vieron en «Dónde te puedes quedar»
    expect(within(sobre).getAllByRole('img').map((i) => i.getAttribute('src')))
      .toEqual(['https://cdn.sga.co/4.jpg', 'https://cdn.sga.co/1.jpg', 'https://cdn.sga.co/2.jpg'])
  })

  it('antes de reservar, una ficha por tema con los datos del hotel y solo sus servicios activos', async () => {
    laApiResponde(pagina([]))
    abrir()

    const antes = screen.getByRole('region', { name: 'Antes de reservar' })
    await within(antes).findByRole('region', { name: 'Si cambian tus planes' })
    const fichas = within(antes).getAllByRole('region').map((f) => f.getAttribute('aria-label'))
    expect(fichas).toEqual(['Llegada y salida', 'Si cambian tus planes', 'Formas de pago', 'Parqueadero', 'Mascotas',
      'Servicios del alojamiento', 'Normas de la casa'])
    const ficha = (nombre: string) => within(antes).getByRole('region', { name: nombre })
    expect(ficha('Llegada y salida')).toHaveTextContent(/Llegada desde3:00\sp\.\sm\.Salida hasta11:00\sa\.\sm\./)
    expect(ficha('Si cambian tus planes')).toHaveTextContent('Con 48 horas o más de antelación: no se retiene nada.')
    expect(ficha('Si cambian tus planes')).toHaveTextContent('Si no llegas: se retiene el 100 % del total de la reserva.')
    expect(ficha('Formas de pago')).toHaveTextContent('EfectivoPago en línea')
    expect(ficha('Parqueadero')).toHaveTextContent('Sin costo, sujeto a cupo.')
    expect(ficha('Mascotas')).toHaveTextContent(/Son bienvenidas, hasta 2 por reserva\. Cada una suma \$\s*30\.000/)
    // El parqueadero del catálogo ya tiene su ficha y la lavandería está inactiva
    expect(ficha('Servicios del alojamiento')).toHaveTextContent(/^Servicios del alojamientoDesayuno: \$\s*25\.000$/)
    expect(ficha('Normas de la casa')).toHaveTextContent('No fumar en las habitaciones')
  })

  it('un hotel que no recibe mascotas lo dice', () => {
    laApiResponde(pagina([]))
    abrir({ ...HOTEL, parametros: { ...HOTEL.parametros, aceptaMascotas: false } })

    expect(screen.getByRole('region', { name: 'Mascotas' })).toHaveTextContent('Este alojamiento no recibe mascotas.')
  })

  it('cómo llegar abre Google Maps en las coordenadas del hotel, junto al último llamado a reservar', () => {
    laApiResponde(pagina([]))
    abrir()

    const cierre = screen.getByRole('region', { name: '¿Ya sabes cuándo vienes?' })
    expect(within(cierre).getByRole('link', { name: 'Ver apartamentos' })).toHaveAttribute('href', '/apartamentos')
    expect(within(cierre).getByText('Calle 5 # 3-10, Salento')).toBeInTheDocument()
    expect(within(cierre).getByRole('link', { name: /Abrir en Google Maps/ }))
      .toHaveAttribute('href', 'https://www.google.com/maps/search/?api=1&query=4.637%2C-75.571')
  })

  it('la portada turna la foto del hotel y las de los apartamentos cada 6 s, y se puede pausar o elegir', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] })
    laApiResponde(pagina([apartamento('APT-1', 'Uno', 'https://cdn.sga.co/1.jpg'),
      apartamento('APT-2', 'Dos', 'https://cdn.sga.co/2.jpg')]))
    const persona = userEvent.setup()
    render(
      <MemoryRouter>
        <Inicio alojamiento={{ ...HOTEL, marca: { portadaUrl: 'https://cdn.sga.co/portada.jpg' } }} />
      </MemoryRouter>,
    )
    const fotos = await screen.findByRole('group', { name: 'Fotos de la portada' })
    const activa = () => document.querySelector('.portada img[data-activa="true"]')?.getAttribute('src')
    expect(within(fotos).getAllByRole('button', { name: /^Foto \d de 3$/ })).toHaveLength(3)
    expect(activa()).toBe('https://cdn.sga.co/portada.jpg')

    act(() => vi.advanceTimersByTime(6000))
    expect(activa()).toBe('https://cdn.sga.co/1.jpg')
    expect(within(fotos).getByRole('button', { name: 'Foto 2 de 3' })).toHaveAttribute('aria-current', 'true')

    await persona.click(within(fotos).getByRole('button', { name: 'Pausar' }))
    act(() => vi.advanceTimersByTime(20000))
    expect(activa()).toBe('https://cdn.sga.co/1.jpg')
    expect(within(fotos).getByRole('button', { name: 'Seguir' })).toHaveAttribute('aria-pressed', 'true')

    await persona.click(within(fotos).getByRole('button', { name: 'Foto 3 de 3' }))
    expect(activa()).toBe('https://cdn.sga.co/2.jpg')
    vi.useRealTimers()
  })

  it('con una sola foto no hay puntos ni pausa', async () => {
    laApiResponde(pagina([apartamento('APT-1', 'Uno')]))
    abrir({ ...HOTEL, marca: { portadaUrl: 'https://cdn.sga.co/portada.jpg' } })

    await screen.findByRole('article', { name: 'Uno' })
    expect(screen.queryByRole('group', { name: 'Fotos de la portada' })).not.toBeInTheDocument()
  })
})
