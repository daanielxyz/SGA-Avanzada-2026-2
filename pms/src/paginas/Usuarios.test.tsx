import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Usuario } from '../api/usuarios'
import { paginaDe } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'

function usuario(id: string, nombre: string, correo: string, rol: Rol, cambios: Partial<Usuario> = {}): Usuario {
  return { id, nombre, correo, rol, telefono: '3001234567', tipoDocumento: 'CC', numeroDocumento: '1094000000',
    fechaNacimiento: '1990-05-02', activado: true, eliminado: false, ...cambios }
}

// La sesión de prueba es USR-1: Andrés es quien entra
const ANDRES = usuario('USR-1', 'Andrés Felipe Gómez', 'andres.gomez@ejemplo.co', 'ADMINISTRADOR')
const MARIA = usuario('USR-2', 'María Fernanda Ospina', 'maria.ospina@ejemplo.co', 'RECEPCIONISTA')
const CARLOS = usuario('USR-3', 'Carlos Restrepo', 'carlos.restrepo@ejemplo.co', 'RECEPCIONISTA', { activado: false })
const JORGE = usuario('USR-5', 'Jorge Muñoz', 'jorge.munoz@ejemplo.co', 'RECEPCIONISTA', { eliminado: true })
const LISTA = [ANDRES, MARIA, CARLOS, JORGE]

const error = (estado: number, codigo: string, mensaje: string, detalles: string[] = []): [number, unknown] =>
  [estado, { estado, codigo, mensaje, detalles }]

function api(rutas: Parameters<typeof laApiRespondePorRuta>[0] = {}) {
  return laApiRespondePorRuta({ '/api/usuarios': [200, paginaDe(LISTA)], ...rutas })
}

function abrir(rol: Rol = 'ADMINISTRADOR') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={['/usuarios']}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

const tabla = async () => screen.findByRole('table', { name: 'Usuarios del personal' })
const fila = async (nombre: string) => within(await tabla()).getByRole('row', { name: new RegExp(nombre) })
const envios = (fetch: ReturnType<typeof api>, metodo: string) => fetch.mock.calls
  .filter(([, opciones]) => (opciones as RequestInit | undefined)?.method === metodo)
  .map(([url, opciones]) => ({ url, cuerpo: (opciones as RequestInit).body ? JSON.parse(String((opciones as RequestInit).body)) : undefined }))

describe('usuarios', () => {
  it('muestra cada cuenta con su rol y su estado; la propia dice «Tú» y no se elimina, la eliminada no tiene acciones', async () => {
    api()
    abrir()
    const propia = await fila('Andrés')
    expect(propia).toHaveTextContent('Tú')
    expect(propia).toHaveTextContent('Administración')
    expect(propia).toHaveTextContent('Activa')
    expect(within(propia).getByRole('button', { name: 'Editar a Andrés Felipe Gómez' })).toBeInTheDocument()
    expect(within(propia).queryByRole('button', { name: /Eliminar/ })).not.toBeInTheDocument()

    expect(await fila('Carlos')).toHaveTextContent('Sin activar')
    expect(within(await fila('Carlos')).getByRole('button', { name: 'Eliminar a Carlos Restrepo' })).toBeInTheDocument()

    const eliminada = await fila('Jorge')
    expect(eliminada).toHaveTextContent('Eliminada')
    expect(within(eliminada).queryByRole('button')).not.toBeInTheDocument()
    expect(screen.queryByText('RECEPCIONISTA')).not.toBeInTheDocument()
  })

  it('recepción no la puede abrir', async () => {
    api()
    abrir('RECEPCIONISTA')
    expect(await screen.findByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })

  it('si la lista no carga, se puede reintentar', async () => {
    let veces = 0
    api({ '/api/usuarios': () => (veces++ === 0 ? error(500, 'ERROR_INTERNO', 'Falló') : [200, paginaDe(LISTA)]) })
    const usuario = abrir()
    expect(await screen.findByText('No pudimos cargar los usuarios', { exact: false })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await fila('María')).toBeInTheDocument()
  })

  it('crea una cuenta: revisa los campos, marca el correo repetido y al final dice a quién le llegó el código', async () => {
    let intentos = 0
    const fetch = api({
      '/api/usuarios': (metodo) => {
        if (metodo === 'GET') return [200, paginaDe(LISTA)]
        return intentos++ === 0
          ? error(409, 'DUPLICADO', 'Ya existe un usuario con el correo maria.ospina@ejemplo.co')
          : [201, usuario('USR-6', 'Carlos Restrepo', 'carlos.r@ejemplo.co', 'RECEPCIONISTA', { activado: false })]
      },
    })
    const persona = abrir()
    await tabla()
    await persona.click(screen.getByRole('button', { name: /Nuevo usuario/ }))
    const dialogo = screen.getByRole('dialog', { name: 'Nuevo usuario' })
    await persona.click(within(dialogo).getByRole('button', { name: 'Crear usuario' }))
    expect(within(dialogo).getByText('Escribe el nombre.')).toBeInTheDocument()
    expect(within(dialogo).getByText('Escribe el correo.')).toBeInTheDocument()
    expect(within(dialogo).getByText('Elige el rol.')).toBeInTheDocument()
    expect(envios(fetch, 'POST')).toHaveLength(0)

    await persona.type(within(dialogo).getByLabelText('Nombre'), 'Carlos Restrepo')
    await persona.type(within(dialogo).getByLabelText('Correo'), 'maria.ospina@ejemplo.co')
    await persona.type(within(dialogo).getByLabelText('Teléfono'), '3001234567')
    await persona.selectOptions(within(dialogo).getByLabelText('Tipo de documento'), 'CC')
    await persona.type(within(dialogo).getByLabelText('Número de documento'), '1094000000')
    await persona.type(within(dialogo).getByLabelText('Fecha de nacimiento'), '1990-05-02')
    await persona.click(within(dialogo).getByRole('radio', { name: /Recepción/ }))
    await persona.click(within(dialogo).getByRole('button', { name: 'Crear usuario' }))
    expect(await within(dialogo).findByText('Ya hay una cuenta con este correo.')).toBeInTheDocument()
    expect(within(dialogo).getByLabelText('Correo')).toHaveAttribute('aria-invalid', 'true')

    await persona.clear(within(dialogo).getByLabelText('Correo'))
    await persona.type(within(dialogo).getByLabelText('Correo'), ' carlos.r@ejemplo.co ')
    await persona.click(within(dialogo).getByRole('button', { name: 'Crear usuario' }))
    const creado = await screen.findByRole('dialog', { name: 'Usuario creado' })
    expect(creado).toHaveTextContent('Le enviamos a carlos.r@ejemplo.co un código para activar su cuenta. Vence en 24 horas.')
    expect(creado).toHaveTextContent('Sin activar')
    expect(envios(fetch, 'POST')[1].cuerpo).toEqual({
      nombre: 'Carlos Restrepo', telefono: '3001234567', tipoDocumento: 'CC', numeroDocumento: '1094000000',
      fechaNacimiento: '1990-05-02', rol: 'RECEPCIONISTA', correo: 'carlos.r@ejemplo.co',
    })
    await persona.click(within(creado).getAllByRole('button', { name: 'Cerrar' }).at(-1) as HTMLElement)
    expect(await screen.findByText(/tiene 24 horas para activar su cuenta/)).toBeInTheDocument()
  })

  it('al cambiar el rol de otra persona avisa antes de guardar que tendrá que volver a entrar', async () => {
    const fetch = api({ '/api/usuarios/USR-2': [200, { ...MARIA, rol: 'ADMINISTRADOR' }] })
    const persona = abrir()
    await persona.click(within(await fila('María')).getByRole('button', { name: /Editar/ }))
    const dialogo = screen.getByRole('dialog', { name: 'Editar usuario' })
    expect(dialogo).toHaveTextContent('maria.ospina@ejemplo.co')
    expect(within(dialogo).queryByRole('textbox', { name: 'Correo' })).not.toBeInTheDocument()
    expect(within(dialogo).queryByText(/María Fernanda tendrá que volver a entrar/)).not.toBeInTheDocument()

    await persona.click(within(dialogo).getByRole('radio', { name: /Administración/ }))
    expect(within(dialogo).getByText(/María Fernanda tendrá que volver a entrar/)).toBeInTheDocument()
    await persona.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }))

    expect(await screen.findByText('Los datos de María Fernanda Ospina quedaron guardados.')).toBeInTheDocument()
    expect(envios(fetch, 'PUT')[0]).toEqual({
      url: '/api/usuarios/USR-2',
      cuerpo: { nombre: 'María Fernanda Ospina', telefono: '3001234567', tipoDocumento: 'CC',
        numeroDocumento: '1094000000', fechaNacimiento: '1990-05-02', rol: 'ADMINISTRADOR' },
    })
    expect(await fila('María')).toHaveTextContent('Administración')
  })

  it('si el administrador cambia su propio rol, vuelve a «Iniciar sesión» con el aviso', async () => {
    api({ '/api/usuarios/USR-1': [200, { ...ANDRES, rol: 'RECEPCIONISTA' }] })
    const persona = abrir()
    await persona.click(within(await fila('Andrés')).getByRole('button', { name: /Editar/ }))
    const dialogo = screen.getByRole('dialog', { name: 'Editar usuario' })
    await persona.click(within(dialogo).getByRole('radio', { name: /Recepción/ }))
    expect(within(dialogo).getByText(/Tendrás que volver a entrar/)).toBeInTheDocument()
    await persona.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }))
    expect(await screen.findByText('Tu rol cambió a Recepción. Vuelve a entrar.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('elimina una cuenta, y si es el último administrador muestra el rechazo en el diálogo', async () => {
    let intentos = 0
    const fetch = api({
      '/api/usuarios/USR-3/eliminar': () => (intentos++ === 0
        ? error(409, 'REGLA_NEGOCIO', 'Es el último administrador: el alojamiento no puede quedarse sin uno')
        : [200, { ...CARLOS, eliminado: true }]),
    })
    const persona = abrir()
    await persona.click(within(await fila('Carlos')).getByRole('button', { name: /Eliminar/ }))
    const dialogo = screen.getByRole('dialog', { name: '¿Eliminar la cuenta de Carlos Restrepo?' })
    expect(dialogo).toHaveTextContent('Carlos Restrepo ya no podrá entrar al PMS. Sus acciones anteriores siguen registradas a su nombre.')
    await persona.click(within(dialogo).getByRole('button', { name: 'Eliminar cuenta' }))
    expect(await within(dialogo).findByText('Es el último administrador: el alojamiento no puede quedarse sin uno')).toBeInTheDocument()

    await persona.click(within(dialogo).getByRole('button', { name: 'Eliminar cuenta' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(await fila('Carlos')).toHaveTextContent('Eliminada')
    expect(within(await fila('Carlos')).queryByRole('button')).not.toBeInTheDocument()
    expect(envios(fetch, 'PUT').map((e) => e.url)).toEqual(['/api/usuarios/USR-3/eliminar', '/api/usuarios/USR-3/eliminar'])
  })
})
