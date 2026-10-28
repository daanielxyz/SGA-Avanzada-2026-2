import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App'
import { entrarComo, laApiResponde, llamada } from './pruebas/sesionDePrueba'
import { leerSesion, sesionPerdida } from './sesion/almacen'
import ProveedorSesion from './sesion/ProveedorSesion'

const SESION_RECEPCION = {
  token: 'token-nuevo',
  tipo: 'Bearer',
  expiraEn: '2999-01-01T00:00:00Z',
  usuario: 'USR-2',
  nombre: 'Luis Pérez',
  rol: 'RECEPCIONISTA',
}

function abrir(direccion: string) {
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
}

async function escribirYEntrar(correo: string, contrasena: string) {
  const usuario = userEvent.setup()
  await usuario.type(screen.getByLabelText('Correo'), correo)
  await usuario.type(screen.getByLabelText('Contraseña'), contrasena)
  await usuario.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('entrada del personal', () => {
  it('sin sesión, cualquier pantalla manda a entrar', () => {
    abrir('/reservas')

    expect(screen.getByRole('heading', { name: 'Gestión del alojamiento' })).toBeInTheDocument()
  })

  it('con correo y contraseña válidos entra, guarda la sesión y vuelve a la pantalla que se pidió', async () => {
    const fetch = laApiResponde([200, SESION_RECEPCION])
    abrir('/reservas')

    await escribirYEntrar('luis@sga.co', 'Clave1234')

    expect(await screen.findByRole('heading', { name: 'Reservas' })).toBeInTheDocument()
    expect(llamada(fetch, 0)).toMatchObject({
      url: '/api/auth/login', metodo: 'POST', cuerpo: { correo: 'luis@sga.co', contrasena: 'Clave1234' },
    })
    expect(leerSesion()?.token).toBe('token-nuevo')
    expect(screen.getByText('Luis Pérez')).toBeInTheDocument()
  })

  it('con credenciales erradas muestra el mensaje del backend y no entra', async () => {
    laApiResponde([401, {
      estado: 401, codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'Correo o contraseña incorrectos', detalles: [],
    }])
    abrir('/')

    await escribirYEntrar('luis@sga.co', 'mala')

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos')
    expect(leerSesion()).toBeNull()
  })

  it('sin llenar los campos avisa sin llamar a la API', async () => {
    const fetch = laApiResponde()
    abrir('/')

    await userEvent.setup().click(screen.getByRole('button', { name: 'Entrar' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Escribe tu correo y tu contraseña.')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('si el backend no responde, lo dice', async () => {
    laApiResponde([502, undefined])
    abrir('/')

    await escribirYEntrar('luis@sga.co', 'Clave1234')

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos comunicarnos con el servidor')
  })
})

describe('panel según el rol', () => {
  it('recepción ve su menú y no el de configuración', () => {
    entrarComo('RECEPCIONISTA')
    abrir('/')

    const menu = screen.getByRole('navigation', { name: 'Menú principal' })
    expect(within(menu).getByRole('link', { name: 'Reservas' })).toBeInTheDocument()
    expect(within(menu).queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Hola, Ana Gómez' })).toBeInTheDocument()
  })

  it('una pantalla de otro rol, abierta por su dirección, dice que no hay permiso', () => {
    entrarComo('RECEPCIONISTA')
    abrir('/usuarios')

    expect(screen.getByRole('heading', { name: 'No tienes permiso para esta pantalla' })).toBeInTheDocument()
  })

  it('el administrador abre la configuración', () => {
    entrarComo('ADMINISTRADOR')
    abrir('/usuarios')

    expect(screen.getByRole('heading', { name: 'Usuarios' })).toBeInTheDocument()
  })

  it('una dirección que no existe muestra la página de no encontrada', () => {
    entrarComo('ADMINISTRADOR')
    abrir('/no-existe')

    expect(screen.getByRole('heading', { name: 'No encontramos esta página' })).toBeInTheDocument()
  })
})

describe('salida', () => {
  it('salir cierra la sesión en el backend y vuelve a la entrada', async () => {
    entrarComo('PERSONAL_SERVICIO')
    const fetch = laApiResponde([200, { mensaje: 'La sesión quedó cerrada.' }])
    abrir('/')

    await userEvent.setup().click(screen.getByRole('button', { name: 'Salir' }))

    expect(await screen.findByRole('heading', { name: 'Gestión del alojamiento' })).toBeInTheDocument()
    expect(llamada(fetch, 0)).toMatchObject({
      url: '/api/auth/logout', metodo: 'POST', cabeceras: { Authorization: 'Bearer token-PERSONAL_SERVICIO' },
    })
    expect(leerSesion()).toBeNull()
  })

  it('si el backend deja de aceptar el token, vuelve a la entrada y lo explica', () => {
    entrarComo('ADMINISTRADOR')
    abrir('/usuarios')

    act(() => sesionPerdida())

    expect(screen.getByRole('heading', { name: 'Gestión del alojamiento' })).toBeInTheDocument()
    expect(screen.getByText('Tu sesión terminó. Vuelve a entrar para continuar.')).toBeInTheDocument()
  })
})
