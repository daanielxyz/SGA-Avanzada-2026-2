import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import App from '../App'
import { paginaDe, reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

const RUTA = '/api/reservas/RES-2026-00042'

function abrir(direccion = '/reservas/RES-2026-00042', rol: Parameters<typeof entrarComo>[0] = 'RECEPCIONISTA') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
}

describe('detalle de una reserva (CU-49)', () => {
  it('muestra los datos de la reserva y el titular con su nombre', async () => {
    laApiRespondePorRuta({ [RUTA]: [200, reservaDePrueba({ vehiculos: 3, carros: 2, motos: 1, mascotas: 2 })] })
    abrir()

    expect(await screen.findByRole('heading', { level: 1, name: 'RES-2026-00042' })).toBeInTheDocument()
    expect(screen.getByText('No se presentó')).toBeInTheDocument()
    expect(screen.getByText('Titular').nextSibling).toHaveTextContent('Ana Ríos')
    expect(screen.getByText('Titular').nextSibling).toHaveTextContent('TIT-7')
    expect(screen.getByText('POL-3')).toBeInTheDocument()
    expect(screen.getByText('Carros').nextSibling).toHaveTextContent('2')
    expect(screen.getByText('Motos').nextSibling).toHaveTextContent('1')
    expect(screen.getByText('Mascotas').nextSibling).toHaveTextContent('2')
    expect(screen.getByText('Noches').nextSibling).toHaveTextContent('3')
  })

  it('sin hora de llegada dice que no se ha informado', async () => {
    laApiRespondePorRuta({ [RUTA]: [200, reservaDePrueba({ horaEstimadaLlegada: null })] })
    abrir()

    expect(await screen.findByText('Sin informar')).toBeInTheDocument()
  })

  it('con hora de llegada la muestra en 12 horas', async () => {
    laApiRespondePorRuta({ [RUTA]: [200, reservaDePrueba({ horaEstimadaLlegada: '16:30:00' })] })
    abrir()

    expect(await screen.findByText('4:30 p. m.')).toBeInTheDocument()
  })

  it('lista los ocupantes con la edad a la fecha de entrada', async () => {
    laApiRespondePorRuta({ [RUTA]: [200, reservaDePrueba()] })
    abrir()

    const ocupantes = await screen.findByRole('table', { name: 'Ocupantes (2)' })
    const luis = within(ocupantes).getByRole('row', { name: /Luis Ríos/ })
    expect(within(luis).getByText('CC 1094')).toBeInTheDocument()
    expect(within(luis).getByText('Colombia')).toBeInTheDocument()
    expect(within(luis).getByText('14 nov 2016 · 10 años')).toBeInTheDocument()
  })

  it('un ocupante sin documento ni fecha no rompe la pantalla', async () => {
    laApiRespondePorRuta({
      [RUTA]: [200, reservaDePrueba({
        ocupantes: [{
          id: 'OCU-1',
          nombre: 'Sin datos',
          fechaNacimiento: null,
          tipoDocumento: null,
          numeroDocumento: null,
          nacionalidad: null,
        }],
      })],
    })
    abrir()

    expect(await screen.findByText('Sin documento')).toBeInTheDocument()
    expect(screen.getAllByText('Sin dato')).toHaveLength(2)
  })

  it('desglosa solo las noches cobradas y el total congelado', async () => {
    laApiRespondePorRuta({ [RUTA]: [200, reservaDePrueba()] })
    abrir()

    const desglose = await screen.findByRole('table', { name: 'Desglose por noche' })
    expect(within(desglose).getAllByRole('row')).toHaveLength(4)
    expect(within(desglose).queryByText('17 nov 2026')).not.toBeInTheDocument()
    expect(screen.getByText('$ 1.250.000')).toBeInTheDocument()
  })

  it('una reserva que no existe lo explica y lleva al listado (404)', async () => {
    laApiRespondePorRuta({})
    abrir('/reservas/RES-2026-09999')

    expect(await screen.findByRole('heading', { level: 1, name: 'Reserva no encontrada' })).toBeInTheDocument()
    expect(screen.getByText('RES-2026-09999')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al listado de reservas' })).toHaveAttribute('href', '/reservas')
  })

  it('otro error se explica y se puede reintentar', async () => {
    laApiRespondePorRuta({
      [RUTA]: [500, { estado: 500, codigo: 'ERROR_INTERNO', mensaje: 'Algo falló en el servidor', detalles: [] }],
    })
    abrir()

    expect(await screen.findByRole('alert')).toHaveTextContent('Algo falló en el servidor')
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('un dato imposible no deja el PMS en blanco: se explica y el menú sigue ahí', async () => {
    const silencio = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    laApiRespondePorRuta({ [RUTA]: [200, { ...reservaDePrueba(), valor: null }] })
    abrir()

    expect(await screen.findByRole('heading', { name: 'Algo salió mal' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Menú principal' })).toBeInTheDocument()
    silencio.mockRestore()
  })

  it('el desglose ancho se puede recorrer con el teclado', async () => {
    laApiRespondePorRuta({ [RUTA]: [200, reservaDePrueba()] })
    abrir()

    const zona = await screen.findByRole('group', { name: 'Desglose por noche' })
    expect(zona).toHaveAttribute('tabindex', '0')
  })

  it('dice en la pestaña qué reserva está abierta', async () => {
    laApiRespondePorRuta({ [RUTA]: [200, reservaDePrueba()] })
    abrir()

    await screen.findByRole('heading', { level: 1, name: 'RES-2026-00042' })
    expect(document.title).toBe('RES-2026-00042 · Meridian')
  })

  it('volver conserva los filtros con los que se llegó', async () => {
    laApiRespondePorRuta({
      '/api/reservas': [200, paginaDe([reservaDePrueba()])],
      [RUTA]: [200, reservaDePrueba()],
    })
    abrir('/reservas?estado=CONFIRMADA')
    const usuario = userEvent.setup()

    const tabla = await screen.findByRole('table', { name: 'Reservas' })
    await usuario.click(within(tabla).getByRole('link', { name: 'RES-2026-00042' }))

    const volver = await screen.findByRole('link', { name: 'Volver a reservas' })
    expect(volver).toHaveAttribute('href', '/reservas?estado=CONFIRMADA')
  })

  it('el personal de servicio no puede abrir el detalle', () => {
    laApiRespondePorRuta({})
    abrir('/reservas/RES-2026-00042', 'PERSONAL_SERVICIO')

    expect(screen.getByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })
})
