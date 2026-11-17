import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import type { EnvioTra } from '../api/tra'
import { laApiRespondePorRuta, llamada } from '../pruebas/sesionDePrueba'
import EnvioTraReserva from './EnvioTraReserva'
import EnviosTraConError from './EnviosTraConError'

const RUTA = '/api/reservas/RES-2026-00042/tra'

function envio(cambios: Partial<EnvioTra>): EnvioTra {
  return {
    reserva: 'RES-2026-00042', estado: 'PENDIENTE', codigoMinisterio: null, acompanantesEnviados: 0, intentos: 0,
    ultimoIntento: null, ultimoError: null, enviadoEn: null, ...cambios,
  }
}

const NO_ENCONTRADO: [number, unknown] = [404, { estado: 404, codigo: 'NO_ENCONTRADO', mensaje: 'No existe', detalles: [] }]

function mostrar(faltanDatos = false) {
  render(<EnvioTraReserva codigo="RES-2026-00042" faltanDatos={faltanDatos} clave="1" />)
  return userEvent.setup()
}

describe('el TRA de una reserva (DEC-130)', () => {
  it('enviado: la fecha, a cuántos y el código del Ministerio, sin botones', async () => {
    laApiRespondePorRuta({ [RUTA]: [200, envio({ estado: 'ENVIADO', codigoMinisterio: '19', acompanantesEnviados: 2, intentos: 1, enviadoEn: '2026-11-17T15:40:00' })] })
    mostrar()
    expect(await screen.findByText('Enviado')).toBeInTheDocument()
    expect(screen.getByText('17 nov 2026, 3:40 p. m. · el huésped principal y 2 acompañantes')).toBeInTheDocument()
    expect(screen.getByText('19')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('con error: el motivo y «Reintentar», que deja el nuevo estado', async () => {
    const fetch = laApiRespondePorRuta({
      [RUTA]: [200, envio({ estado: 'CON_ERROR', intentos: 2, ultimoIntento: '2026-11-17T15:40:00', ultimoError: 'El Ministerio respondió 401: revisa el token' })],
      [`${RUTA}/enviar`]: [200, envio({ estado: 'ENVIADO', codigoMinisterio: '19', intentos: 3, enviadoEn: '2026-11-17T15:45:00' })],
    })
    const usuario = mostrar()
    expect(await screen.findByText('El Ministerio respondió 401: revisa el token')).toBeInTheDocument()
    expect(screen.getByText(/2 intentos/)).toBeInTheDocument()

    // «Reintentar» va dentro del aviso que dice el motivo
    const aviso = screen.getByText('El Ministerio respondió 401: revisa el token').closest('.aviso') as HTMLElement
    await usuario.click(within(aviso).getByRole('button', { name: 'Reintentar' }))
    // Recién enviado lo dice el aviso grande; la etiqueta «Enviado» no lo repite encima
    expect(await screen.findByText('TRA enviado')).toBeInTheDocument()
    expect(screen.queryByText('Enviado')).not.toBeInTheDocument()
    expect(llamada(fetch, 1)).toMatchObject({ url: `${RUTA}/enviar`, metodo: 'PUT' })
    // El botón ya no está: el foco vuelve al título del bloque
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Reporte al Ministerio (TRA)' })).toHaveFocus())
  })

  it('con datos incompletos lo dice y no ofrece enviar', async () => {
    laApiRespondePorRuta({ [RUTA]: [200, envio({})] })
    mostrar(true)
    expect(await screen.findByText('Pendiente')).toBeInTheDocument()
    expect(screen.getByText('Se envía solo cuando estén todos los datos del TRA de los ocupantes.')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('una reserva sin envío (llegó antes del cambio) se puede enviar ya; un 409 se muestra', async () => {
    laApiRespondePorRuta({
      [RUTA]: NO_ENCONTRADO,
      [`${RUTA}/enviar`]: [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'El TRA de la reserva RES-2026-00042 ya se envió', detalles: [] }],
    })
    const usuario = mostrar()
    expect(await screen.findByText('Pendiente')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Enviar ahora' }))
    expect(await screen.findByText('El TRA de la reserva RES-2026-00042 ya se envió')).toBeInTheDocument()
  })
})

describe('el aviso del listado con los TRA sin enviar (DEC-130)', () => {
  it('sin envíos con error no muestra nada', async () => {
    const fetch = laApiRespondePorRuta({ '/api/tra/envios': [200, { contenido: [], numero: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 }] })
    const { container } = render(<MemoryRouter><EnviosTraConError /></MemoryRouter>)
    await waitFor(() => expect(fetch).toHaveBeenCalled())
    expect(container).toBeEmptyDOMElement()
  })

  it('con envíos con error dice cuántos y enlaza a cada reserva', async () => {
    laApiRespondePorRuta({ '/api/tra/envios': [200, {
      contenido: [envio({ estado: 'CON_ERROR', ultimoError: 'x', reserva: 'RES-2026-00042' })], numero: 0, tamano: 10, totalElementos: 1, totalPaginas: 1,
    }] })
    render(<MemoryRouter><EnviosTraConError /></MemoryRouter>)
    expect(await screen.findByText(/El TRA de una reserva no llegó al Ministerio/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'RES-2026-00042' })).toHaveAttribute('href', '/reservas/RES-2026-00042')
  })
})
