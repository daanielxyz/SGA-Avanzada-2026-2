import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { FichaApartamento } from '../api/apartamentos'
import { alojamientoDePrueba, PARAMETROS } from '../pruebas/alojamientoDePrueba'
import { paginaDe } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta, llamada } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'
import { fechaHoy, sumarDias } from '../formato'

const hoy = fechaHoy()

function apartamento(cambios: Partial<FichaApartamento> = {}): FichaApartamento {
  return {
    codigo: 'APT-101',
    nombre: 'Penthouse Mirador',
    descripcion: 'Penthouse en el último piso.',
    capacidad: 6,
    dormitorios: 3,
    estadoOperativo: 'PREPARADO',
    activo: true,
    imagenes: [
      { url: 'https://fotos.ejemplo.com/101-1.jpg', principal: true },
      { url: 'https://fotos.ejemplo.com/101-2.jpg', principal: false },
    ],
    caracteristicas: ['Balcón', 'Wifi'],
    bloqueos: [
      { id: 'BLQ-1', fechaInicio: sumarDias(hoy, 7), fechaFin: sumarDias(hoy, 10), motivo: 'Mantenimiento de la cubierta', vigente: true },
      { id: 'BLQ-2', fechaInicio: sumarDias(hoy, -30), fechaFin: sumarDias(hoy, -27), motivo: 'Pintura', vigente: true },
    ],
    limiteMascotas: 2,
    ...cambios,
  }
}

const error = (estado: number, codigo: string, mensaje: string, detalles: string[] = []): [number, unknown] =>
  [estado, { estado, codigo, mensaje, detalles }]

function abrir(direccion = '/apartamentos', rol: Rol = 'ADMINISTRADOR') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

function api(rutas: Parameters<typeof laApiRespondePorRuta>[0] = {}, ficha = apartamento()) {
  return laApiRespondePorRuta({
    '/api/alojamiento': [200, alojamientoDePrueba()],
    '/api/apartamentos': [200, paginaDe([ficha])],
    '/api/apartamentos/APT-101': [200, ficha],
    ...rutas,
  })
}

describe('lista de apartamentos', () => {
  it('muestra la cantidad, la venta y el estado operativo con su palabra, y «Ver» abre el detalle', async () => {
    api({ '/api/apartamentos': [200, paginaDe([
      apartamento(),
      apartamento({ codigo: 'APT-110', nombre: 'Mirador del Lago', activo: false, estadoOperativo: 'FUERA_DE_SERVICIO', imagenes: [] }),
    ])] })
    const usuario = abrir()

    expect(await screen.findByRole('heading', { level: 1, name: /^Apartamentos/ })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Apartamentos (2)')
    const tabla = screen.getByRole('table', { name: 'Apartamentos' })
    const fila = within(tabla).getByRole('row', { name: /APT-110/ })
    expect(within(fila).getByText('Retirado')).toBeInTheDocument()
    expect(within(fila).getByText('Fuera de servicio')).toBeInTheDocument()
    expect(within(fila).getByRole('img', { name: 'Sin foto' })).toBeInTheDocument()
    expect(screen.queryByText('FUERA_DE_SERVICIO')).not.toBeInTheDocument()

    await usuario.click(within(tabla).getByRole('link', { name: 'Ver APT-101' }))
    expect(await screen.findByRole('heading', { level: 1, name: /APT-101 · Penthouse Mirador/ })).toBeInTheDocument()
  })

  it('sin apartamentos invita a crear el primero; si falla, ofrece reintentar', async () => {
    api({ '/api/apartamentos': [200, paginaDe([])] })
    abrir()
    expect(await screen.findByRole('heading', { name: 'Todavía no hay apartamentos' })).toBeInTheDocument()
  })

  it('si la API falla, explica y ofrece «Reintentar»', async () => {
    api({ '/api/apartamentos': error(500, 'ERROR_INTERNO', 'Error interno') })
    abrir()
    expect(await screen.findByText('No pudimos cargar los apartamentos', { exact: false })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})

describe('nuevo apartamento', () => {
  it('pide al menos una foto y una característica antes de enviar (acordado el 13 nov)', async () => {
    const fetch = api()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: /Nuevo apartamento/ }))
    const dialogo = await screen.findByRole('dialog', { name: 'Nuevo apartamento' })

    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear apartamento' }))

    expect(within(dialogo).getByText('Agrega al menos una foto.')).toBeInTheDocument()
    expect(within(dialogo).getByText('El apartamento debe declarar al menos una característica.')).toBeInTheDocument()
    expect(fetch.mock.calls.some(([, opciones]) => (opciones as RequestInit | undefined)?.method === 'POST')).toBe(false)
  })

  it('crea con «APT-» delante y lleva al detalle con el aviso de que nace retirado', async () => {
    const creado = apartamento({ codigo: 'APT-111', nombre: 'Loft Central', activo: false,
      estadoOperativo: 'PENDIENTE_PREPARACION', bloqueos: [], imagenes: [{ url: 'https://f.co/1.jpg', principal: true }] })
    const fetch = api({
      '/api/apartamentos': (metodo) => (metodo === 'POST' ? [201, creado] : [200, paginaDe([apartamento()])]),
      '/api/apartamentos/APT-111': [200, creado],
    })
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: /Nuevo apartamento/ }))
    const dialogo = await screen.findByRole('dialog', { name: 'Nuevo apartamento' })

    await usuario.type(within(dialogo).getByLabelText('Código'), '111')
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Loft Central')
    await usuario.type(within(dialogo).getByLabelText('Capacidad (personas)'), '4')
    await usuario.type(within(dialogo).getByLabelText('Dormitorios'), '2')
    await usuario.type(within(dialogo).getByLabelText('Características'), 'Wifi{Enter}')
    await usuario.type(within(dialogo).getByLabelText('Dirección de la imagen'), 'https://f.co/1.jpg')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear apartamento' }))

    expect(await screen.findByText('Apartamento creado')).toBeInTheDocument()
    expect(screen.getByText(/APT-111 quedó creado y retirado de la venta/)).toBeInTheDocument()
    const post = fetch.mock.calls.findIndex(([, opciones]) => (opciones as RequestInit | undefined)?.method === 'POST')
    expect(llamada(fetch, post).cuerpo).toEqual({
      codigo: 'APT-111', nombre: 'Loft Central', descripcion: '', capacidad: 4, dormitorios: 2,
      caracteristicas: ['Wifi'], imagenes: [{ url: 'https://f.co/1.jpg', principal: true }],
    })
  })

  it('un código repetido se dice junto al código', async () => {
    api({ '/api/apartamentos': (metodo) => (metodo === 'POST'
      ? error(409, 'DUPLICADO', 'Ya existe el apartamento APT-101')
      : [200, paginaDe([apartamento()])]) })
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: /Nuevo apartamento/ }))
    const dialogo = await screen.findByRole('dialog', { name: 'Nuevo apartamento' })
    await usuario.type(within(dialogo).getByLabelText('Código'), '101')
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Otro')
    await usuario.type(within(dialogo).getByLabelText('Capacidad (personas)'), '2')
    await usuario.type(within(dialogo).getByLabelText('Dormitorios'), '1')
    await usuario.type(within(dialogo).getByLabelText('Características'), 'Wifi{Enter}')
    await usuario.type(within(dialogo).getByLabelText('Dirección de la imagen'), 'https://f.co/1.jpg')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear apartamento' }))

    expect(await within(dialogo).findByText('Ya hay un apartamento con el código APT-101.')).toBeInTheDocument()
    expect(within(dialogo).getByLabelText('Código')).toHaveAttribute('aria-invalid', 'true')
  })
})

describe('detalle de un apartamento', () => {
  it('muestra cada sección; los bloqueos terminados quedan en «Ver anteriores»', async () => {
    api()
    abrir('/apartamentos/APT-101')

    expect(await screen.findByRole('heading', { level: 1, name: /APT-101 · Penthouse Mirador/ })).toBeInTheDocument()
    expect(screen.getByText('6 personas')).toBeInTheDocument()
    expect(screen.getByText('Hasta 2 mascotas')).toBeInTheDocument()
    const bloqueos = screen.getByRole('region', { name: 'Bloqueos' })
    expect(within(bloqueos).getByText('Mantenimiento de la cubierta')).toBeInTheDocument()
    expect(within(bloqueos).getAllByRole('button', { name: /Levantar/ })).toHaveLength(1)
    expect(within(bloqueos).getByText('Ver anteriores (1)')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retirar de la venta' })).toBeInTheDocument()
  })

  it('la única foto no se puede quitar', async () => {
    api({}, apartamento({ imagenes: [{ url: 'https://f.co/1.jpg', principal: true }] }))
    abrir('/apartamentos/APT-101')

    const fotos = await screen.findByRole('region', { name: /Fotos/ })
    expect(within(fotos).queryByRole('button', { name: /Quitar/ })).not.toBeInTheDocument()
    expect(within(fotos).getByText(/agrega otra antes de quitar esta/)).toBeInTheDocument()
  })

  it('al cambiar la capacidad lista las reservas que ya no caben, con su enlace', async () => {
    api({ '/api/apartamentos/APT-101/capacidad': [200, {
      apartamento: apartamento({ capacidad: 4 }), reservasAfectadas: ['RES-2026-00042', 'RES-2026-00051'],
    }] })
    const usuario = abrir('/apartamentos/APT-101')
    await usuario.click(await screen.findByRole('button', { name: 'Cambiar capacidad' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cambiar capacidad' })
    const campo = within(dialogo).getByLabelText('Capacidad (personas)')
    await usuario.clear(campo)
    await usuario.type(campo, '4')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }))

    const resultado = await screen.findByRole('dialog', { name: 'Capacidad cambiada' })
    expect(within(resultado).getByText('Quedó en 4 personas')).toBeInTheDocument()
    expect(within(resultado).getByRole('link', { name: 'RES-2026-00042' })).toHaveAttribute('href', '/reservas/RES-2026-00042')
  })

  it('si no se puede poner a la venta, muestra el mensaje de la API y ofrece ir a tarifas', async () => {
    api({ '/api/apartamentos/APT-101/activar': error(409, 'REGLA_NEGOCIO',
      'El apartamento necesita tarifa en todas las temporadas para activarse') }, apartamento({ activo: false }))
    const usuario = abrir('/apartamentos/APT-101')
    expect(await screen.findByText('No aparece en la web ni se puede reservar.', { selector: '.aviso p' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Poner a la venta' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Poner a la venta' })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Poner a la venta' }))

    expect(await within(dialogo).findByText(/necesita tarifa en todas las temporadas/)).toBeInTheDocument()
    expect(within(dialogo).getByRole('link', { name: 'Ir a temporadas y tarifas' })).toHaveAttribute('href', '/tarifas')
    expect(within(dialogo).getByRole('button', { name: 'Poner a la venta' })).toBeDisabled()
  })

  it('un bloqueo rechazado dice el porqué junto a las fechas', async () => {
    api({ '/api/apartamentos/APT-101/bloqueos': error(409, 'REGLA_NEGOCIO', 'El fin del bloqueo debe ser posterior al inicio') })
    const usuario = abrir('/apartamentos/APT-101')
    await usuario.click(await screen.findByRole('button', { name: 'Nuevo bloqueo' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Nuevo bloqueo' })
    await usuario.type(within(dialogo).getByLabelText('Desde'), sumarDias(hoy, 3))
    await usuario.type(within(dialogo).getByLabelText('Hasta'), sumarDias(hoy, 5))
    await usuario.type(within(dialogo).getByLabelText(/Motivo/), 'Pintura')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear bloqueo' }))

    expect(await within(dialogo).findByText('El fin del bloqueo debe ser posterior al inicio')).toBeInTheDocument()
    expect(within(dialogo).getByLabelText('Desde')).toHaveAttribute('aria-invalid', 'true')
  })

  it('si el hotel no acepta mascotas, lo dice sin ofrecer cambiarlo', async () => {
    api({ '/api/alojamiento': [200, alojamientoDePrueba({ parametros: { ...PARAMETROS, aceptaMascotas: false } })] })
    abrir('/apartamentos/APT-101')
    expect(await screen.findByText('El hotel no acepta mascotas (se cambia en Alojamiento).')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cambiar mascotas' })).not.toBeInTheDocument()
  })

  it('no existe: lo dice y ofrece volver', async () => {
    api({ '/api/apartamentos/APT-999': error(404, 'NO_ENCONTRADO', 'No existe el apartamento APT-999') })
    abrir('/apartamentos/APT-999')
    expect(await screen.findByRole('heading', { name: 'Apartamento no encontrado' })).toBeInTheDocument()
  })

  it('recepción no lo puede abrir', async () => {
    api()
    abrir('/apartamentos/APT-101', 'RECEPCIONISTA')
    expect(await screen.findByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })
})
