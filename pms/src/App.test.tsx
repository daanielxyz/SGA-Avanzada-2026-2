import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App'
import { entrarComo, laApiResponde, laApiRespondePorRuta, llamada } from './pruebas/sesionDePrueba'
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
  await usuario.type(screen.getByLabelText('Correo electrónico'), correo)
  await usuario.type(screen.getByLabelText('Contraseña'), contrasena)
  await usuario.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('entrada del personal', () => {
  it('sin sesión, cualquier pantalla manda a entrar', () => {
    abrir('/reservas')

    expect(screen.getByRole('heading', { name: 'Te damos la bienvenida' })).toBeInTheDocument()
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

  it('si la sesión venció en un listado filtrado, al entrar vuelve con los mismos filtros', async () => {
    const fetch = laApiRespondePorRuta({ '/api/auth/login': [200, SESION_RECEPCION] })
    abrir('/reservas?estado=CONFIRMADA')

    await escribirYEntrar('luis@sga.co', 'Clave1234')

    await screen.findByRole('heading', { name: 'Reservas' })
    await waitFor(() =>
      expect(fetch.mock.calls.map(([url]) => url)).toContain('/api/reservas?estado=CONFIRMADA'),
    )
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

    expect(screen.getByLabelText('Correo electrónico')).toHaveAccessibleDescription('Escribe tu correo electrónico.')
    expect(screen.getByLabelText('Contraseña')).toHaveAccessibleDescription('Escribe tu contraseña.')
    expect(screen.getByLabelText('Correo electrónico')).toHaveFocus()
    expect(fetch).not.toHaveBeenCalled()
  })

  it('la contraseña se puede ver y volver a ocultar', async () => {
    abrir('/')
    const usuario = userEvent.setup()
    const contrasena = screen.getByLabelText('Contraseña')

    await usuario.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))
    expect(contrasena).toHaveAttribute('type', 'text')

    await usuario.click(screen.getByRole('button', { name: 'Ocultar contraseña' }))
    expect(contrasena).toHaveAttribute('type', 'password')
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

    expect(screen.getByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })

  it('el administrador abre la configuración', () => {
    entrarComo('ADMINISTRADOR')
    abrir('/usuarios')

    expect(screen.getByRole('heading', { name: 'Usuarios' })).toBeInTheDocument()
  })

  it('una dirección que no existe muestra la página de no encontrada', () => {
    entrarComo('ADMINISTRADOR')
    abrir('/no-existe')

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
  })

  it('muestra el nombre del alojamiento que trae la API', async () => {
    entrarComo('RECEPCIONISTA')
    const fetch = laApiResponde([200, { nombre: 'Hotel de Prueba' }])
    abrir('/')

    expect(await screen.findByText('Hotel de Prueba')).toBeInTheDocument()
    expect(llamada(fetch, 0).url).toBe('/api/alojamiento')
  })

  it('sin el nombre del alojamiento el panel sigue funcionando', async () => {
    entrarComo('RECEPCIONISTA')
    const fetch = laApiResponde([404, { estado: 404, codigo: 'NO_ENCONTRADO', mensaje: 'No existe', detalles: [] }])
    abrir('/')

    await waitFor(() => expect(fetch).toHaveBeenCalled())
    expect(screen.getByRole('heading', { name: 'Hola, Ana Gómez' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})

describe('navegar con teclado y lector de pantalla', () => {
  it('al cambiar de pantalla el foco pasa al contenido y cambia el título de la pestaña', async () => {
    entrarComo('RECEPCIONISTA')
    laApiRespondePorRuta({})
    abrir('/')
    expect(document.title).toBe('Inicio · Meridian')

    const menu = screen.getByRole('navigation', { name: 'Menú principal' })
    await userEvent.setup().click(within(menu).getByRole('link', { name: 'Titulares' }))

    expect(screen.getByRole('main')).toHaveFocus()
    expect(document.title).toBe('Titulares · Meridian')
  })

  it('el enlace de salto lleva directo al contenido', async () => {
    entrarComo('RECEPCIONISTA')
    laApiRespondePorRuta({})
    abrir('/')

    await userEvent.setup().click(screen.getByRole('link', { name: 'Saltar al contenido' }))

    expect(screen.getByRole('main')).toHaveFocus()
  })
})

describe('menú plegable del celular', () => {
  it('se abre con el botón y muestra quién entró y cómo salir', async () => {
    entrarComo('PERSONAL_SERVICIO')
    abrir('/')

    await userEvent.setup().click(screen.getByRole('button', { name: 'Abrir menú' }))

    const menu = screen.getByRole('navigation', { name: 'Menú principal' })
    expect(within(menu).getByRole('button', { name: 'Cerrar menú' })).toHaveFocus()
    expect(within(menu).getByText('Ana Gómez')).toBeInTheDocument()
    expect(within(menu).getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument()
  })

  it('con Escape se cierra y el foco vuelve al botón que lo abrió', async () => {
    entrarComo('PERSONAL_SERVICIO')
    abrir('/')
    const usuario = userEvent.setup()
    const abrirMenu = screen.getByRole('button', { name: 'Abrir menú' })

    await usuario.click(abrirMenu)
    await usuario.keyboard('{Escape}')

    expect(screen.queryByRole('button', { name: 'Cerrar menú' })).not.toBeInTheDocument()
    expect(abrirMenu).toHaveFocus()
    expect(abrirMenu).toHaveAttribute('aria-expanded', 'false')
  })

  it('elegir una opción abre esa pantalla y cierra el menú', async () => {
    entrarComo('PERSONAL_SERVICIO')
    abrir('/')
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: 'Abrir menú' }))
    const menu = screen.getByRole('navigation', { name: 'Menú principal' })
    await usuario.click(within(menu).getByRole('link', { name: 'Novedades' }))

    expect(screen.getByRole('heading', { name: 'Novedades' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cerrar menú' })).not.toBeInTheDocument()
  })
})

describe('salida', () => {
  it('salir cierra la sesión en el backend y vuelve a la entrada', async () => {
    entrarComo('RECEPCIONISTA')
    const fetch = laApiResponde([200, { nombre: 'Hotel de Prueba' }], [200, { mensaje: 'La sesión quedó cerrada.' }])
    abrir('/')

    await userEvent.setup().click(screen.getByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('heading', { name: 'Te damos la bienvenida' })).toBeInTheDocument()
    expect(llamada(fetch, 1)).toMatchObject({
      url: '/api/auth/logout', metodo: 'POST', cabeceras: { Authorization: 'Bearer token-RECEPCIONISTA' },
    })
    expect(leerSesion()).toBeNull()
  })

  it('si el backend deja de aceptar el token, vuelve a la entrada y lo explica', () => {
    entrarComo('ADMINISTRADOR')
    abrir('/usuarios')

    act(() => sesionPerdida())

    expect(screen.getByRole('heading', { name: 'Te damos la bienvenida' })).toBeInTheDocument()
    expect(screen.getByText('Tu sesión terminó. Vuelve a entrar.')).toBeInTheDocument()
  })
})
