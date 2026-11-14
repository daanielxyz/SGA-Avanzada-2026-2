import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Usuario } from '../api/usuarios'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import { leerSesion } from '../sesion/almacen'
import ProveedorSesion from '../sesion/ProveedorSesion'

const MARIA: Usuario = { id: 'USR-1', nombre: 'Ana Gómez', correo: 'ana.gomez@ejemplo.co', rol: 'RECEPCIONISTA',
  telefono: '3001234567', tipoDocumento: 'CC', numeroDocumento: '1094000000', fechaNacimiento: '1990-05-02',
  activado: true, eliminado: false }

function abrir(rutas: Parameters<typeof laApiRespondePorRuta>[0]) {
  const fetch = laApiRespondePorRuta({ '/api/usuarios/perfil': [200, MARIA], ...rutas })
  entrarComo('RECEPCIONISTA')
  render(
    <MemoryRouter initialEntries={['/perfil']}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return { fetch, persona: userEvent.setup() }
}

const bloque = async (nombre: string) => screen.findByRole('region', { name: nombre })

describe('mi perfil', () => {
  it('guarda los datos propios; el correo y el rol solo se leen, y la cabecera toma el nombre nuevo', async () => {
    const { fetch, persona } = abrir({
      '/api/usuarios/perfil': (metodo) => [200, metodo === 'PUT' ? { ...MARIA, nombre: 'Ana María Gómez' } : MARIA],
    })
    const datos = await bloque('Mis datos')
    expect(within(datos).getByText('ana.gomez@ejemplo.co')).toBeInTheDocument()
    expect(within(datos).getByText('Recepción')).toBeInTheDocument()
    expect(within(datos).queryByRole('textbox', { name: 'Correo' })).not.toBeInTheDocument()

    const nombre = within(datos).getByLabelText('Nombre')
    await persona.clear(nombre)
    await persona.click(within(datos).getByRole('button', { name: 'Guardar cambios' }))
    expect(within(datos).getByText('Escribe el nombre.')).toBeInTheDocument()

    await persona.type(nombre, 'Ana María Gómez')
    await persona.click(within(datos).getByRole('button', { name: 'Guardar cambios' }))
    expect(await within(datos).findByText('Tus datos quedaron guardados')).toBeInTheDocument()
    const [, opciones] = fetch.mock.calls.find(([, o]) => (o as RequestInit | undefined)?.method === 'PUT') as [string, RequestInit]
    expect(JSON.parse(String(opciones.body))).toEqual({ nombre: 'Ana María Gómez', telefono: '3001234567',
      tipoDocumento: 'CC', numeroDocumento: '1094000000', fechaNacimiento: '1990-05-02' })
    expect(leerSesion()?.nombre).toBe('Ana María Gómez')
  })

  it('la contraseña nueva muestra la regla antes de escribir y revisa que coincidan', async () => {
    const { fetch, persona } = abrir({})
    const clave = await bloque('Contraseña')
    expect(clave).toHaveTextContent('Al menos 8 caracteres, una mayúscula y un número.')
    await persona.type(within(clave).getByLabelText('Contraseña actual'), 'Vieja2026')
    await persona.type(within(clave).getByLabelText('Contraseña nueva'), 'corta')
    await persona.type(within(clave).getByLabelText('Repite la contraseña nueva'), 'otra')
    await persona.click(within(clave).getByRole('button', { name: 'Cambiar contraseña' }))
    expect(within(clave).getByText('Le falta: al menos 8 caracteres, una mayúscula y un número.')).toBeInTheDocument()
    expect(within(clave).getByText('Las dos contraseñas no son iguales.')).toBeInTheDocument()
    expect(fetch.mock.calls.some(([url]) => String(url).includes('contrasena'))).toBe(false)
  })

  it('si la actual no es correcta lo dice junto a ese campo y la sesión sigue abierta', async () => {
    const { persona } = abrir({
      '/api/usuarios/perfil/contrasena': [400, { estado: 400, codigo: 'CREDENCIALES_INVALIDAS',
        mensaje: 'La contraseña actual no es correcta', detalles: ['actual: no es la contraseña actual'] }],
    })
    const clave = await bloque('Contraseña')
    await persona.type(within(clave).getByLabelText('Contraseña actual'), 'Errada2026')
    await persona.type(within(clave).getByLabelText('Contraseña nueva'), 'Salento2026')
    await persona.type(within(clave).getByLabelText('Repite la contraseña nueva'), 'Salento2026')
    await persona.click(within(clave).getByRole('button', { name: 'Cambiar contraseña' }))
    expect(await within(clave).findByText('La contraseña actual no es correcta.')).toBeInTheDocument()
    expect(within(clave).getByLabelText('Contraseña actual')).toHaveAttribute('aria-invalid', 'true')
    expect(leerSesion()).not.toBeNull()
    expect(screen.getByRole('heading', { name: 'Mi perfil' })).toBeInTheDocument()
  })

  it('al cambiarla vuelve a «Iniciar sesión» con el aviso, porque el backend cierra todas sus sesiones', async () => {
    const { persona } = abrir({
      '/api/usuarios/perfil/contrasena': [200, { mensaje: 'La contraseña quedó cambiada. Vuelva a iniciar sesión.' }],
    })
    const clave = await bloque('Contraseña')
    await persona.type(within(clave).getByLabelText('Contraseña actual'), 'Vieja2026')
    await persona.type(within(clave).getByLabelText('Contraseña nueva'), 'Salento2026')
    await persona.type(within(clave).getByLabelText('Repite la contraseña nueva'), 'Salento2026')
    await persona.click(within(clave).getByRole('button', { name: 'Mostrar contraseña nueva' }))
    expect(within(clave).getByLabelText('Contraseña nueva')).toHaveAttribute('type', 'text')
    await persona.click(within(clave).getByRole('button', { name: 'Cambiar contraseña' }))
    expect(await screen.findByText('Tu contraseña cambió. Entra con la nueva.')).toBeInTheDocument()
    expect(leerSesion()).toBeNull()
  })
})
