import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { RespuestaAsistente } from '../api/asistente'
import Aviso from '../componentes/Aviso'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import type { Rol } from '../sesion/roles'
import ProveedorSesion from '../sesion/ProveedorSesion'
import PanelAsistente from './PanelAsistente'
import ProveedorAsistente from './ProveedorAsistente'

const RUTA = '/api/asistente/preguntas'

const NO_SHOW: RespuestaAsistente = {
  respuesta: 'Para declarar el no-show:\n\n1. La reserva debe estar **confirmada**.\n2. Pulsa «Declarar no-show».',
  fuentes: [
    { id: 'reservas-cancelar-noshow', titulo: 'Cancelar una reserva o declarar el no-show', pantalla: '/reservas' },
    { id: 'reservas-detalle', titulo: 'El detalle de una reserva', pantalla: '/reservas/{codigo}' },
  ],
  origen: 'IA',
  motivo: null,
  aviso: null,
}

const DEL_MANUAL: RespuestaAsistente = {
  respuesta: 'Esto dice el manual en «Registrar la llegada»:\n\nPulsa «Registrar llegada».',
  fuentes: [{ id: 'reservas-llegada', titulo: 'Registrar la llegada', pantalla: '/reservas/{codigo}/llegada' }],
  origen: 'LOCAL',
  motivo: 'NO_DISPONIBLE',
  aviso: 'El asistente no respondió: te respondo con el manual.',
}

/** La API simulada: el asistente responde, en orden, lo que se le pase; el resto de rutas, lo mínimo. */
function apiCon(...respuestas: RespuestaAsistente[]) {
  const cola = [...respuestas]
  return laApiRespondePorRuta({ [RUTA]: () => [200, cola.shift() ?? NO_SHOW] })
}

function preguntasEnviadas(fetch: ReturnType<typeof vi.fn>) {
  return fetch.mock.calls
    .filter(([url]) => url === RUTA)
    .map(([, opciones]) => JSON.parse(String((opciones as RequestInit).body)))
}

function abrir(rol: Rol = 'RECEPCIONISTA', direccion = '/') {
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

describe('ayuda del PMS (DEC-127)', () => {
  it('se abre desde la cabecera con las preguntas sugeridas del rol', async () => {
    apiCon()
    const usuario = abrir('PERSONAL_SERVICIO')

    await usuario.click(await screen.findByRole('button', { name: 'Ayuda' }))

    const panel = screen.getByRole('complementary', { name: 'Ayuda del PMS' })
    expect(within(panel).getByRole('heading', { name: '¿En qué te ayudo?' })).toBeInTheDocument()
    expect(within(panel).getByRole('button', { name: '¿Por cuál apartamento empiezo?' })).toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: '¿Cómo registro una llegada?' })).not.toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: 'Empezar de nuevo' })).not.toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tu pregunta' })).toHaveFocus()
    expect(screen.getByRole('button', { name: 'Ayuda' })).toHaveAttribute('aria-expanded', 'true')
  })

  it('responde con el texto, las negritas y las fuentes, y manda la conversación anterior', async () => {
    const fetch = apiCon(NO_SHOW, DEL_MANUAL)
    const usuario = abrir('RECEPCIONISTA', '/reservas/RES-2026-00042')

    await usuario.click(await screen.findByRole('button', { name: 'Ayuda' }))
    await usuario.type(screen.getByRole('textbox', { name: 'Tu pregunta' }), '¿Cómo declaro el no-show?{Enter}')

    const panel = screen.getByRole('complementary', { name: 'Ayuda del PMS' })
    expect(await within(panel).findByText('confirmada', { selector: 'strong' })).toBeInTheDocument()
    expect(within(panel).getAllByRole('listitem').length).toBeGreaterThanOrEqual(2)
    expect(within(panel).getByRole('link', { name: /Cancelar una reserva o declarar el no-show/ }))
      .toHaveAttribute('href', '/reservas')
    expect(within(panel).getByRole('link', { name: /El detalle de una reserva/ }))
      .toHaveAttribute('href', '/reservas/RES-2026-00042')

    await usuario.type(screen.getByRole('textbox', { name: 'Tu pregunta' }), '¿Y la llegada?{Enter}')

    expect(await within(panel).findByText('El asistente no respondió: te respondo con el manual.')).toBeInTheDocument()
    const [primera, segunda] = preguntasEnviadas(fetch)
    expect(primera).toEqual({ pregunta: '¿Cómo declaro el no-show?', pantalla: '/reservas/RES-2026-00042', historial: [] })
    expect(segunda.historial).toEqual([
      { autor: 'PERSONA', texto: '¿Cómo declaro el no-show?' },
      { autor: 'ASISTENTE', texto: NO_SHOW.respuesta },
    ])
  })

  it('una sugerencia se envía como si se escribiera, y «Empezar de nuevo» borra la conversación', async () => {
    const fetch = apiCon()
    const usuario = abrir('ADMINISTRADOR')

    await usuario.click(await screen.findByRole('button', { name: 'Ayuda' }))
    await usuario.click(screen.getByRole('button', { name: '¿Cómo pongo un apartamento a la venta?' }))
    await screen.findByText('confirmada', { selector: 'strong' })
    await usuario.click(screen.getByRole('button', { name: 'Empezar de nuevo' }))

    expect(preguntasEnviadas(fetch)[0].pregunta).toBe('¿Cómo pongo un apartamento a la venta?')
    expect(screen.queryByText('confirmada', { selector: 'strong' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '¿En qué te ayudo?' })).toBeInTheDocument()
  })

  it('sin conexión deja la pregunta con «Reintentar», que la vuelve a enviar', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Ayuda' }))
    const conRed = fetch.getMockImplementation()
    fetch.mockImplementationOnce(() => Promise.reject(new TypeError('Failed to fetch')))

    await usuario.type(screen.getByRole('textbox', { name: 'Tu pregunta' }), '¿Cómo declaro el no-show?{Enter}')

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo enviar. Revisa tu conexión.')
    fetch.mockImplementation(conRed as never)
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('confirmada', { selector: 'strong' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
  })

  it('no envía una pregunta vacía ni una de más de 1.000 caracteres', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Ayuda' }))

    await usuario.click(screen.getByRole('button', { name: 'Preguntar' }))
    expect(screen.getByText('Escribe tu pregunta.')).toBeInTheDocument()

    const campo = screen.getByRole('textbox', { name: 'Tu pregunta' })
    await usuario.click(campo)
    await usuario.paste('a'.repeat(1001))
    expect(screen.getByText('1.001 / 1.000')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Preguntar' }))

    expect(screen.getByText(/pasa de 1.000 caracteres/)).toBeInTheDocument()
    expect(campo).toHaveAttribute('aria-invalid', 'true')
    expect(preguntasEnviadas(fetch)).toHaveLength(0)
  })

  it('se cierra con Escape, conserva la conversación y el foco vuelve a «Ayuda»', async () => {
    apiCon()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Ayuda' }))
    await usuario.type(screen.getByRole('textbox', { name: 'Tu pregunta' }), '¿Cómo declaro el no-show?{Enter}')
    await screen.findByText('confirmada', { selector: 'strong' })

    await usuario.keyboard('{Escape}')

    expect(screen.queryByRole('complementary', { name: 'Ayuda del PMS' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ayuda' })).toHaveFocus()
    await usuario.click(screen.getByRole('button', { name: 'Ayuda' }))
    expect(screen.getByText('confirmada', { selector: 'strong' })).toBeInTheDocument()
  })
})

describe('«¿Por qué?» en un rechazo (DEC-127)', () => {
  function conAviso(accion?: boolean) {
    entrarComo('RECEPCIONISTA')
    render(
      <MemoryRouter initialEntries={['/reservas/RES-2026-00061/llegada']}>
        <ProveedorSesion>
          <ProveedorAsistente>
            <Aviso tipo="error" asunto="No se registró la llegada" mensaje="El apartamento todavía no está preparado."
              accion={accion ? <button type="button">Reintentar</button> : undefined} />
            <PanelAsistente />
          </ProveedorAsistente>
        </ProveedorSesion>
      </MemoryRouter>,
    )
    return userEvent.setup()
  }

  it('abre la ayuda y pregunta por el mensaje del rechazo', async () => {
    const fetch = apiCon()
    const usuario = conAviso()

    await usuario.click(screen.getByRole('button', { name: /¿Por qué\?/ }))

    expect(screen.getByRole('complementary', { name: 'Ayuda del PMS' })).toBeInTheDocument()
    await waitFor(() => expect(preguntasEnviadas(fetch)).toHaveLength(1))
    expect(preguntasEnviadas(fetch)[0]).toMatchObject({
      pregunta: '¿Por qué el sistema dice: «No se registró la llegada: El apartamento todavía no está preparado.»?',
      pantalla: '/reservas/RES-2026-00061/llegada',
    })
  })

  it('no aparece en un error que se resuelve reintentando', () => {
    conAviso(true)

    expect(screen.queryByRole('button', { name: /¿Por qué\?/ })).not.toBeInTheDocument()
  })
})
