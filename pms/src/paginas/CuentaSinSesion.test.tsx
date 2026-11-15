import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

function abrir(ruta: string) {
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

const NO_VALE: [number, unknown] = [401, { estado: 401, codigo: 'CREDENCIALES_INVALIDAS',
  mensaje: 'El correo o el código no son válidos, o el código ya venció', detalles: [] }]

const cuerpoDe = (fetch: ReturnType<typeof laApiRespondePorRuta>, ruta: string) => {
  const llamada = fetch.mock.calls.filter(([url]) => url === ruta).at(-1) as [string, RequestInit]
  return JSON.parse(String(llamada[1].body))
}

describe('cuenta sin sesión', () => {
  it('«Iniciar sesión» lleva a recuperar la contraseña y a activar la cuenta', async () => {
    const persona = abrir('/ingresar')
    await persona.click(screen.getByRole('link', { name: 'Olvidé mi contraseña' }))
    expect(screen.getByRole('heading', { name: 'Olvidé mi contraseña' })).toBeInTheDocument()
    await persona.click(screen.getByRole('link', { name: 'Volver a iniciar sesión' }))
    await persona.click(screen.getByRole('link', { name: 'Activar mi cuenta' }))
    expect(screen.getByRole('heading', { name: 'Activar mi cuenta' })).toBeInTheDocument()
  })

  it('activa la cuenta; con un código que no vale ofrece pedir uno nuevo', async () => {
    let intentos = 0
    const fetch = laApiRespondePorRuta({
      '/api/auth/activar': () => (intentos++ === 0 ? NO_VALE : [200, { mensaje: 'La cuenta quedó activada.' }]),
    })
    const persona = abrir('/activar')
    await persona.click(screen.getByRole('button', { name: 'Activar mi cuenta' }))
    expect(screen.getByText('Escribe el correo.')).toBeInTheDocument()
    expect(screen.getByText('Escribe el código que te llegó al correo.')).toBeInTheDocument()
    expect(fetch).not.toHaveBeenCalled()

    await persona.type(screen.getByLabelText('Correo electrónico'), 'carlos.restrepo@ejemplo.co')
    await persona.type(screen.getByLabelText('Código de 6 dígitos'), '730 241')
    await persona.type(screen.getByLabelText('Contraseña nueva'), 'Salento2026')
    await persona.type(screen.getByLabelText('Repite la contraseña nueva'), 'Salento2026')
    await persona.click(screen.getByRole('button', { name: 'Activar mi cuenta' }))
    expect(await screen.findByText('El correo o el código no son válidos, o el código ya venció.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Pedir un código nuevo' })).toBeInTheDocument()

    await persona.click(screen.getByRole('button', { name: 'Activar mi cuenta' }))
    expect(await screen.findByText('Tu cuenta quedó activa. Entra con tu correo y tu contraseña.')).toBeInTheDocument()
    expect(cuerpoDe(fetch, '/api/auth/activar')).toEqual({
      correo: 'carlos.restrepo@ejemplo.co', codigo: '730241', contrasena: 'Salento2026',
    })
  })

  it('recupera la contraseña en dos pasos, con la misma respuesta exista o no el correo', async () => {
    let intentos = 0
    const fetch = laApiRespondePorRuta({
      '/api/auth/recuperar-contrasena': [202, { mensaje: 'Si el correo es de un usuario, le enviamos un código.' }],
      '/api/auth/restablecer-contrasena': () => (intentos++ === 0 ? NO_VALE : [200, { mensaje: 'Listo' }]),
    })
    const persona = abrir('/recuperar')
    expect(screen.getByText('Paso 1 de 2')).toBeInTheDocument()
    await persona.type(screen.getByLabelText('Correo electrónico'), 'luz.cardona@ejemplo')
    await persona.click(screen.getByRole('button', { name: 'Enviar código' }))
    expect(screen.getByText('Escribe un correo válido, por ejemplo nombre@dominio.co.')).toBeInTheDocument()

    await persona.type(screen.getByLabelText('Correo electrónico'), '.co')
    await persona.click(screen.getByRole('button', { name: 'Enviar código' }))
    expect(await screen.findByText('Paso 2 de 2')).toBeInTheDocument()
    expect(screen.getByText('Si el correo está registrado, te llegó un código de 6 dígitos. Vence en 15 minutos.')).toBeInTheDocument()
    expect(screen.getByText('luz.cardona@ejemplo.co')).toBeInTheDocument()

    await persona.type(screen.getByLabelText('Código de 6 dígitos'), '730241')
    await persona.type(screen.getByLabelText('Contraseña nueva'), 'Salento2026')
    await persona.type(screen.getByLabelText('Repite la contraseña nueva'), 'Salento2026')
    await persona.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
    expect(await screen.findByText('El código no es válido o ya venció.')).toBeInTheDocument()

    await persona.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
    expect(await screen.findByText('Tu contraseña cambió. Entra con la nueva.')).toBeInTheDocument()
    expect(cuerpoDe(fetch, '/api/auth/restablecer-contrasena')).toEqual({
      correo: 'luz.cardona@ejemplo.co', codigo: '730241', contrasena: 'Salento2026',
    })
  })

  it('desde el paso 2 se puede volver a pedir el código', async () => {
    laApiRespondePorRuta({ '/api/auth/recuperar-contrasena': [202, { mensaje: 'Enviado' }] })
    const persona = abrir('/recuperar')
    await persona.type(screen.getByLabelText('Correo electrónico'), 'luz.cardona@ejemplo.co')
    await persona.click(screen.getByRole('button', { name: 'Enviar código' }))
    await persona.click(await screen.findByRole('button', { name: 'Pedir otro código o usar otro correo' }))
    expect(screen.getByText('Paso 1 de 2')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo electrónico')).toHaveValue('luz.cardona@ejemplo.co')
  })
})
