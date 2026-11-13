import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Folio } from '../api/folio'
import type { Ocupante, Reserva } from '../api/reservas'
import type { Titular } from '../api/titulares'
import { fechaHoy, fechaSinAnio, sumarDias } from '../formato'
import { reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'

const CODIGO = 'RES-2026-00061'
const RUTA = `/api/reservas/${CODIGO}`
const HOY = fechaHoy()

const ANA: Ocupante = {
  id: 'OCU-1', nombre: 'Ana Torres Vélez', nombres: 'Ana', primerApellido: 'Torres', segundoApellido: 'Vélez',
  fechaNacimiento: '1988-03-14', tipoDocumento: 'CC', numeroDocumento: '1094123456', nacionalidad: 'Colombia',
  principal: true,
  datosTraFaltantes: ['GENERO', 'PAIS_RESIDENCIA', 'CIUDAD_RESIDENCIA', 'CIUDAD_PROCEDENCIA', 'CIUDAD_DESTINO', 'MOTIVO_VIAJE'],
}
const JULIAN: Ocupante = {
  id: 'OCU-2', nombre: 'Julián Mejía Ríos', nombres: 'Julián', primerApellido: 'Mejía', segundoApellido: 'Ríos',
  fechaNacimiento: '1985-07-02', tipoDocumento: null, numeroDocumento: null, nacionalidad: null, principal: false,
  datosTraFaltantes: ['DOCUMENTO', 'NACIONALIDAD', 'GENERO', 'PAIS_RESIDENCIA', 'CIUDAD_RESIDENCIA', 'CIUDAD_PROCEDENCIA', 'CIUDAD_DESTINO', 'MOTIVO_VIAJE'],
}
const SOFIA: Ocupante = {
  id: 'OCU-3', nombre: 'Sofía Mejía Torres', nombres: 'Sofía', primerApellido: 'Mejía', segundoApellido: 'Torres',
  fechaNacimiento: '2017-09-09', tipoDocumento: 'TI', numeroDocumento: '1094987654', nacionalidad: 'Colombia',
  principal: false, datosTraFaltantes: [],
}

const ALOJAMIENTO = {
  nombre: 'Puerta al Sol',
  parametros: {
    anticipoPct: 50, horaLimiteNoShow: '22:00:00', umbralEdadFacturable: 12, aceptaMascotas: true, limiteMascotas: 2,
    cargoMascota: 30000, cupoParqueadero: 8, cobroParqueadero: 'POR_NOCHE', cargoCarro: 15000, cargoMoto: 8000,
  },
  mediosPago: ['EFECTIVO', 'TARJETA'],
}

const TITULAR: Titular = {
  id: 'TIT-7', nombre: 'Ana Torres Vélez', tipoDocumento: 'CC', numeroDocumento: '1094123456', correo: 'ana@ejemplo.com',
  telefono: '310 555 0101', validado: false, validadoEn: null, validadoPor: null,
}

function reservaDeLlegada(cambios: Partial<Reserva> = {}): Reserva {
  return reservaDePrueba({
    codigo: CODIGO,
    apartamento: 'APT-102',
    estado: 'CONFIRMADA',
    canalOrigen: 'DIRECTO',
    entrada: HOY,
    salida: sumarDias(HOY, 3),
    horaEstimadaLlegada: '15:00:00',
    ocupantes: [ANA, JULIAN, SOFIA],
    valor: {
      desglose: [0, 1, 2].map((dia) => ({
        noche: sumarDias(HOY, dia), temporada: 'TEM-1', tarifa: 150000, ocupantesFacturables: 2, subtotal: 300000,
      })),
      total: 900000,
    },
    carros: 1,
    motos: 0,
    mascotas: 1,
    extras: { parqueadero: 45000, mascotas: 30000, total: 75000 },
    anticipoExigido: 487500,
    ...cambios,
  })
}

function folioDe(totalPagado: number): Folio {
  const saldo = 975000 - totalPagado
  return {
    id: 'FOL-1', reserva: CODIGO, cargos: [], pagos: [], totalPagado, saldo: Math.abs(saldo),
    situacionSaldo: saldo > 0 ? 'PENDIENTE' : saldo < 0 ? 'A_FAVOR' : 'AL_DIA', cerrado: false,
  }
}

const RECHAZO = (motivo: string, mensaje: string): [number, unknown] => [
  409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje, detalles: [], motivo },
]

/** API simulada que recuerda lo que cambia, como la de verdad. */
function apiCon({ reserva = {}, pagado = 300000, estadoOperativo = 'PREPARADO', titular = {}, rechazar = {} }: {
  reserva?: Partial<Reserva>
  pagado?: number
  estadoOperativo?: string
  titular?: Partial<Titular>
  rechazar?: Record<string, [number, unknown]>
} = {}) {
  const estado = { reserva: reservaDeLlegada(reserva), titular: { ...TITULAR, ...titular } }
  const accion = (nombre: string, hacer: () => unknown) => (): [number, unknown] => rechazar[nombre] ?? [200, hacer()]
  const conReserva = (cambios: (actual: Reserva) => Partial<Reserva>) => () => {
    estado.reserva = { ...estado.reserva, ...cambios(estado.reserva) }
    return estado.reserva
  }
  return laApiRespondePorRuta({
    [RUTA]: () => [200, estado.reserva],
    [`${RUTA}/folio`]: [200, folioDe(pagado)],
    '/api/alojamiento': [200, ALOJAMIENTO],
    '/api/apartamentos/APT-102': [200, { codigo: 'APT-102', nombre: 'Apartamento Jardín', limiteMascotas: null, estadoOperativo }],
    '/api/titulares/TIT-7': () => [200, estado.titular],
    '/api/titulares/TIT-7/validar': accion('validar', () => {
      estado.titular = { ...estado.titular, validado: true, validadoEn: `${HOY}T15:24:00`, validadoPorNombre: 'Ana Gómez' }
      return estado.titular
    }),
    '/api/cotizaciones': [200, { desglose: [], total: 900000 }],
    [`${RUTA}/hora-llegada`]: accion('hora', conReserva(() => ({ horaEstimadaLlegada: '15:20:00' }))),
    [`${RUTA}/confirmar`]: accion('confirmar', conReserva(() => ({ estado: 'CONFIRMADA' }))),
    [`${RUTA}/registrar-llegada`]: accion('llegada', () => {
      estado.reserva = { ...estado.reserva, estado: 'EN_CURSO' }
      return { codigo: CODIGO, estado: 'EN_CURSO', apartamento: 'APT-102', estadoOperativo: 'OCUPADO', folioCerrado: false, saldo: 0, situacionSaldo: 'AL_DIA' }
    }),
    [`${RUTA}/ocupantes/OCU-2/datos-tra`]: accion('tra', conReserva((actual) => ({
      ocupantes: actual.ocupantes.map((o) => (o.id === 'OCU-2'
        ? { ...o, tipoDocumento: 'CC', numeroDocumento: '80123456', datosTraFaltantes: [] }
        : o)),
    }))),
    [`${RUTA}/ocupantes/OCU-2/principal`]: accion('principal', conReserva((actual) => ({
      ocupantes: actual.ocupantes.map((o) => ({ ...o, principal: o.id === 'OCU-2' })),
    }))),
    [`${RUTA}/estancia`]: [200, {
      codigo: CODIGO, estado: 'EN_CURSO', apartamento: 'APT-102', estadoOperativo: 'OCUPADO', folioCerrado: false, saldo: 0,
      situacionSaldo: 'AL_DIA', salida: null,
      registros: [
        { id: 'REG-1', fechaHora: `${HOY}T14:00:00`, autor: 'USR-2', autorNombre: 'Ana Gómez', anulado: true, motivoCorreccion: null, motivoSinPago: 'Cuenta por cobrar a una empresa' },
        { id: 'REG-2', fechaHora: `${HOY}T15:20:00`, autor: 'USR-1', autorNombre: 'Andrés Felipe Gómez', anulado: false, motivoCorreccion: 'Se registró tarde', motivoSinPago: 'Cuenta por cobrar a una empresa' },
      ],
    }],
    [`${RUTA}/extras`]: accion('extras', conReserva(() => ({ mascotas: 0, extras: { parqueadero: 45000, mascotas: 0, total: 45000 } }))),
  })
}

function llamadasA(fetch: ReturnType<typeof apiCon>, sufijo: string) {
  return fetch.mock.calls
    .filter(([url]) => String(url).endsWith(sufijo))
    .map(([, opciones]) => ({
      metodo: opciones?.method ?? 'GET',
      cuerpo: opciones?.body ? JSON.parse(String(opciones.body)) : undefined,
    }))
}

function abrir(direccion = `/reservas/${CODIGO}/llegada`, rol: Rol = 'RECEPCIONISTA') {
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

describe('registro de llegada (CU-21)', () => {
  it('dice arriba qué falta y no deja registrar sin cobrar la estancia (BL-22)', async () => {
    apiCon()
    abrir()

    const falta = await screen.findByRole('region', { name: 'Lo que falta' })
    expect(await within(falta).findByText(/APT-102 · Preparado/)).toBeInTheDocument()
    expect(within(falta).getByText('Falta por cobrar $ 675.000')).toBeInTheDocument()
    expect(within(falta).getByText(/Faltan datos de 2 de 3 huéspedes/)).toBeInTheDocument()
    const boton = screen.getByRole('button', { name: 'Registrar llegada' })
    expect(boton).toBeDisabled()
    expect(boton).toHaveAccessibleDescription('Falta cobrar la estancia. Solo el administrador puede dejar entrar sin pagarla.')
    expect(screen.queryByRole('checkbox', { name: 'Entra sin pagar la estancia' })).not.toBeInTheDocument()
  })

  it('el administrador sí puede marcar que entra sin pagar (DEC-112)', async () => {
    apiCon()
    abrir(undefined, 'ADMINISTRADOR')
    const boton = await screen.findByRole('button', { name: 'Registrar llegada' })
    expect(boton).toHaveAccessibleDescription('Falta cobrar la estancia, o marca que entra sin pagar.')
    expect(screen.getByRole('checkbox', { name: 'Entra sin pagar la estancia' })).toBeInTheDocument()
  })

  it('con el motivo de entrar sin pagar registra la llegada y vuelve al detalle con el aviso', async () => {
    const fetch = apiCon()
    const usuario = abrir(undefined, 'ADMINISTRADOR')

    await usuario.click(await screen.findByRole('checkbox', { name: 'Entra sin pagar la estancia' }))
    expect(screen.getByRole('button', { name: 'Registrar llegada' })).toHaveAccessibleDescription('Escribe el motivo para registrar sin cobro.')
    await usuario.type(screen.getByLabelText(/Motivo/), 'Cuenta por cobrar a una empresa')
    await usuario.click(screen.getByRole('button', { name: 'Registrar llegada sin cobro' }))

    expect(await screen.findByText('Llegada registrada', {}, { timeout: 4000 })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: CODIGO })).toBeInTheDocument()
    expect(llamadasA(fetch, '/registrar-llegada')).toEqual([
      { metodo: 'PUT', cuerpo: { motivoSinPago: 'Cuenta por cobrar a una empresa' } },
    ])
  }, 15000)

  it('con todo pagado registra sin motivo', async () => {
    const fetch = apiCon({ pagado: 975000 })
    const usuario = abrir()

    expect(await screen.findByText('Pagado $ 975.000')).toBeInTheDocument()
    expect(screen.queryByRole('checkbox', { name: 'Entra sin pagar la estancia' })).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Registrar llegada' }))

    await screen.findByText('Llegada registrada', {}, { timeout: 4000 })
    expect(llamadasA(fetch, '/registrar-llegada')).toEqual([{ metodo: 'PUT', cuerpo: undefined }])
  })

  it('con el apartamento sin preparar no deja registrar y dice quién lo prepara (RN-11)', async () => {
    apiCon({ pagado: 975000, estadoOperativo: 'EN_PREPARACION' })
    abrir()

    expect(await screen.findByText(/APT-102 · En preparación\. Debe prepararlo el personal de servicio\./)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Registrar llegada' })).toHaveAccessibleDescription('El apartamento todavía no está preparado.')
    expect(screen.getByRole('button', { name: 'Registrar llegada' })).toBeDisabled()
  })

  it('antes de la fecha de entrada se puede cobrar, pero no registrar (RN-10)', async () => {
    const entrada = sumarDias(HOY, 3)
    apiCon({ pagado: 975000, reserva: { entrada, salida: sumarDias(HOY, 6) } })
    abrir()

    const boton = await screen.findByRole('button', { name: `Disponible desde ${fechaSinAnio(entrada)}` })
    expect(boton).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Registrar pago' })).toBeEnabled()
  })

  it('un rechazo sale arriba y no se pierde el motivo escrito', async () => {
    apiCon({ rechazar: { llegada: RECHAZO('ESTADO_NO_PERMITIDO', 'El apartamento APT-102 no está preparado') } })
    const usuario = abrir(undefined, 'ADMINISTRADOR')

    await usuario.click(await screen.findByRole('checkbox', { name: 'Entra sin pagar la estancia' }))
    await usuario.type(screen.getByLabelText(/Motivo/), 'Empresa')
    await usuario.click(screen.getByRole('button', { name: 'Registrar llegada sin cobro' }))

    expect(await screen.findByText('Error · No se registró la llegada')).toBeInTheDocument()
    expect(screen.getByLabelText(/Motivo/)).toHaveValue('Empresa')
  })

  it('una reserva de mostrador sin hora se confirma con la hora de ahora y se registra (RN-09 · RES-15)', async () => {
    const fetch = apiCon({ pagado: 975000, reserva: { estado: 'PENDIENTE', horaEstimadaLlegada: null } })
    const usuario = abrir()

    expect(await screen.findByText('Anticipo cubierto')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Confirmar y registrar llegada' }))

    await screen.findByText('Llegada registrada', {}, { timeout: 4000 })
    const orden = fetch.mock.calls.map(([url]) => String(url)).filter((url) => /hora-llegada|confirmar|registrar-llegada/.test(url))
    expect(orden).toEqual([`${RUTA}/hora-llegada`, `${RUTA}/confirmar`, `${RUTA}/registrar-llegada`])
  })

  it('en la de mostrador, sin el anticipo no se puede confirmar aunque entre sin pagar', async () => {
    apiCon({ pagado: 0, reserva: { estado: 'PENDIENTE' } })
    const usuario = abrir(undefined, 'ADMINISTRADOR')

    expect(await screen.findByText('Cubrir el anticipo: $ 487.500')).toBeInTheDocument()
    await usuario.click(screen.getByRole('checkbox', { name: 'Entra sin pagar la estancia' }))
    await usuario.type(screen.getByLabelText(/Motivo/), 'Empresa')
    const boton = screen.getByRole('button', { name: 'Confirmar y registrar llegada sin cobro' })
    expect(boton).toBeDisabled()
    expect(boton).toHaveAccessibleDescription('Para confirmar la reserva hay que cubrir el anticipo, aunque entre sin pagar la estancia.')
  })

  it('si se confirmó pero la llegada se rechazó, lo dice y el botón vuelve a «Registrar llegada»', async () => {
    apiCon({
      pagado: 975000,
      reserva: { estado: 'PENDIENTE' },
      rechazar: { llegada: RECHAZO('ESTADO_NO_PERMITIDO', 'El apartamento no está preparado') },
    })
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Confirmar y registrar llegada' }))

    expect(await screen.findByText('Error · La reserva quedó confirmada, pero no se registró la llegada')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Registrar llegada' })).toBeInTheDocument()
  })

  it('una reserva que ya está en curso no se vuelve a registrar', async () => {
    apiCon({ reserva: { estado: 'EN_CURSO' } })
    abrir()

    expect(await screen.findByRole('heading', { level: 1, name: 'La llegada ya se registró' })).toBeInTheDocument()
  })
})

describe('huéspedes y titular en la llegada (BL-07 · DEC-97 · DEC-98)', () => {
  it('muestra el papel y el TRA de cada huésped; «Hacer principal» solo en mayores que no lo son', async () => {
    apiCon()
    abrir()

    const tabla = await screen.findByRole('table', { name: 'Huéspedes (3)' })
    const ana = within(tabla).getByRole('row', { name: /Ana Torres Vélez/ })
    expect(within(ana).getByText(/Huésped principal/)).toBeInTheDocument()
    expect(within(ana).getByText(/Faltan 6 datos/)).toBeInTheDocument()
    const sofia = within(tabla).getByRole('row', { name: /Sofía Mejía Torres/ })
    expect(within(sofia).getByText(/Completo/)).toBeInTheDocument()
    // Con todo completo, sus datos se corrigen; menor de edad, no puede ser principal
    expect(within(sofia).getAllByRole('button').map((boton) => boton.textContent)).toEqual(['Editar datos'])
    expect(screen.getByRole('button', { name: 'Hacer principal a Julián Mejía Ríos' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Hacer principal a Ana Torres Vélez' })).not.toBeInTheDocument()
  })

  it('«Hacer principal» cambia la etiqueta; un rechazo sale junto a la fila', async () => {
    apiCon()
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Hacer principal a Julián Mejía Ríos' }))
    const julian = await screen.findByRole('row', { name: /Julián Mejía Ríos.*Huésped principal/ })
    expect(julian).toBeInTheDocument()
  })

  it('si no se puede hacer principal, el motivo queda junto a la fila', async () => {
    apiCon({ rechazar: { principal: RECHAZO('HUESPED_PRINCIPAL_INVALIDO', 'Julián Mejía Ríos no tiene documento registrado') } })
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Hacer principal a Julián Mejía Ríos' }))
    const fila = screen.getByRole('row', { name: /Julián Mejía Ríos/ })
    expect(await within(fila).findByRole('alert')).toHaveTextContent('No se pudo hacer principal: Julián Mejía Ríos no tiene documento registrado')
  })

  it('completa el TRA: propone residencia y destino, guarda y sigue con el siguiente huésped', async () => {
    const fetch = apiCon()
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Completar datos de Julián Mejía Ríos' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Completar datos' })
    expect(within(dialogo).getByText('Huésped 2 de 3 · Julián Mejía Ríos')).toBeInTheDocument()

    await usuario.type(within(dialogo).getByLabelText('¿De dónde viene?'), 'Medellín')
    expect(within(dialogo).getByLabelText(/Ciudad de residencia/)).toHaveValue('Medellín')
    expect(within(dialogo).getByLabelText(/Ciudad de destino/)).toHaveValue('Medellín')
    expect(within(dialogo).getAllByText('Propuesta')).toHaveLength(2)
    await usuario.clear(within(dialogo).getByLabelText(/Ciudad de destino/))
    await usuario.type(within(dialogo).getByLabelText(/Ciudad de destino/), 'Bogotá')
    expect(within(dialogo).getAllByText('Propuesta')).toHaveLength(1)

    // El documento va completo o no va
    await usuario.selectOptions(within(dialogo).getByLabelText('Tipo de documento'), 'CC')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar y seguir' }))
    expect(within(dialogo).getByText('Escribe el número de documento.')).toBeInTheDocument()
    expect(llamadasA(fetch, '/datos-tra')).toEqual([])

    await usuario.type(within(dialogo).getByLabelText('Número de documento'), '80123456')
    await usuario.selectOptions(within(dialogo).getByLabelText('Motivo del viaje'), 'TURISMO_VACACIONES')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar y seguir' }))

    expect(await within(dialogo).findByText('Huésped 1 de 3 · Ana Torres Vélez')).toBeInTheDocument()
    expect(within(dialogo).getByText('Se guardaron los datos de Julián Mejía Ríos.')).toBeInTheDocument()
    expect(within(dialogo).getByLabelText(/Número de documento/)).toHaveAttribute('readonly')
    expect(within(dialogo).getByText('Un documento ya registrado no se cambia aquí.')).toBeInTheDocument()
    expect(llamadasA(fetch, '/datos-tra')).toEqual([{
      metodo: 'PUT',
      cuerpo: {
        nombres: 'Julián', primerApellido: 'Mejía', segundoApellido: 'Ríos', tipoDocumento: 'CC', numeroDocumento: '80123456',
        ciudadProcedencia: 'Medellín', ciudadResidencia: 'Medellín', ciudadDestino: 'Bogotá', motivoViaje: 'TURISMO_VACACIONES',
      },
    }])
  })

  it('un huésped con todo completo se edita, y cerrar con cambios sin guardar pregunta antes', async () => {
    const fetch = apiCon()
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Editar datos de Sofía Mejía Torres' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Editar datos' })
    expect(within(dialogo).getByText(/ya tiene todos sus datos del TRA/)).toBeInTheDocument()

    await usuario.type(within(dialogo).getByLabelText('Nacionalidad'), 'na')
    // Escape: el navegador lanza «cancel» en el dialog
    fireEvent(dialogo, new Event('cancel', { cancelable: true }))
    expect(within(dialogo).getByText(/Cambiaste datos de Sofía Mejía Torres/)).toBeInTheDocument()
    expect(within(dialogo).getByRole('button', { name: 'Seguir editando' })).toHaveFocus()

    await usuario.click(within(dialogo).getByRole('button', { name: 'Seguir editando' }))
    expect(within(dialogo).getByLabelText('Nacionalidad')).toHaveValue('Colombiana')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cerrar' }))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Descartar cambios' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(llamadasA(fetch, '/datos-tra')).toEqual([])
  })

  it('«Validar identidad» va en la fila del titular en la tabla de huéspedes (#19), y al validar dice cuándo', async () => {
    const fetch = apiCon()
    const usuario = abrir()

    const boton = await screen.findByRole('button', { name: /^Validar identidad de / })
    expect(boton.closest('table')).toHaveAccessibleName(/Huéspedes/)
    await usuario.click(boton)
    const dialogo = await screen.findByRole('dialog', { name: 'Validar identidad' })
    expect(within(dialogo).getByText('CC 1094123456')).toBeInTheDocument()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Validar identidad' }))

    expect(await screen.findByText(/Validada/)).toBeInTheDocument()
    // DEC-115: dice también quién la validó
    expect(screen.getByText(/, por Ana Gómez/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Validar identidad/ })).not.toBeInTheDocument()
    expect(llamadasA(fetch, '/validar')).toEqual([{ metodo: 'PUT', cuerpo: undefined }])
  })

  it('si el titular no se aloja no se ofrece validar su identidad', async () => {
    apiCon({ titular: { nombre: 'Ricardo Vélez Arango', numeroDocumento: '79555123' } })
    abrir()

    expect(await screen.findByText('Ricardo Vélez Arango')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Validar identidad/ })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cambiar titular' })).toBeInTheDocument()
  })

  it('cambiar el titular pide los datos junto al campo antes de enviar', async () => {
    const fetch = apiCon()
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar titular' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cambiar titular' })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cambiar titular' }))

    expect(within(dialogo).getByText('Escribe los nombres.')).toBeInTheDocument()
    expect(within(dialogo).getByText('Escribe el correo.')).toBeInTheDocument()
    expect(llamadasA(fetch, '/cambiar-titular')).toEqual([])
  })

  it('el documento de un huésped no se acepta con otro nombre, y elegir al huésped llena sus datos', async () => {
    const fetch = apiCon()
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar titular' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cambiar titular' })
    await usuario.type(within(dialogo).getByLabelText('Nombres'), 'Pedro')
    await usuario.type(within(dialogo).getByLabelText('Primer apellido'), 'Pérez')
    await usuario.type(within(dialogo).getByLabelText('Número de documento'), '1094987654')
    await usuario.selectOptions(within(dialogo).getByLabelText('Tipo de documento'), 'TI')
    await usuario.type(within(dialogo).getByLabelText('Correo'), 'pedro@ejemplo.com')
    await usuario.type(within(dialogo).getByLabelText('Teléfono'), '12')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cambiar titular' }))

    expect(within(dialogo).getByText(/Ese documento es de Sofía Mejía Torres, huésped de esta reserva/)).toBeInTheDocument()
    expect(within(dialogo).getByText(/Escribe un teléfono de 7 a 15 dígitos/)).toBeInTheDocument()
    expect(llamadasA(fetch, '/cambiar-titular')).toEqual([])

    await usuario.selectOptions(within(dialogo).getByLabelText('¿Es uno de los huéspedes?'), 'OCU-3')
    expect(within(dialogo).getByLabelText('Nombres')).toHaveValue('Sofía')
    expect(within(dialogo).getByLabelText('Número de documento')).toHaveValue('1094987654')

    // Elegida de la lista, es esa persona aunque recepción corrija cómo se escribe su nombre (ronda 3)
    await usuario.type(within(dialogo).getByLabelText('Nombres'), ' Andrea')
    await usuario.clear(within(dialogo).getByLabelText('Teléfono'))
    await usuario.type(within(dialogo).getByLabelText('Teléfono'), '3001234567')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cambiar titular' }))
    expect(within(dialogo).queryByText(/Ese documento es de/)).not.toBeInTheDocument()
  })
})

describe('vehículos y mascotas en la llegada (BL-23 · DEC-116)', () => {
  it('muestra la diferencia de los extras, sin tocar las noches, y manda solo los extras', async () => {
    const fetch = apiCon()
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar vehículos y mascotas' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Vehículos y mascotas' })
    expect(within(dialogo).getByRole('button', { name: 'Guardar cambios' })).toBeDisabled()

    await usuario.clear(within(dialogo).getByLabelText('Mascotas'))
    await usuario.type(within(dialogo).getByLabelText('Mascotas'), '0')
    expect(await within(dialogo).findByText('Se descuentan')).toBeInTheDocument()
    expect(within(dialogo).getByText('$ 30.000')).toBeInTheDocument()
    expect(within(dialogo).getByText(/El valor de las noches no cambia/)).toBeInTheDocument()
    expect(within(dialogo).queryByText('Cambiaron las tarifas')).not.toBeInTheDocument()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }))

    expect(await screen.findByText('Vehículos y mascotas actualizados')).toBeInTheDocument()
    expect(llamadasA(fetch, '/extras')).toEqual([{ metodo: 'PUT', cuerpo: { carros: 1, motos: 0, mascotas: 0 } }])
    expect(llamadasA(fetch, '/modificar')).toEqual([])
    expect(fetch.mock.calls.some(([url]) => String(url).startsWith('/api/cotizaciones'))).toBe(false)
  })

  it('el cupo del parqueadero se revisa junto al campo', async () => {
    apiCon()
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar vehículos y mascotas' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Vehículos y mascotas' })
    await usuario.clear(within(dialogo).getByLabelText('Carros'))
    await usuario.type(within(dialogo).getByLabelText('Carros'), '9')

    expect(within(dialogo).getByText('El parqueadero tiene 8 cupos en total, entre carros y motos.')).toBeInTheDocument()
    expect(within(dialogo).getByLabelText('Carros')).toBeInvalid()
  })
})

describe('la llegada desde el detalle', () => {
  it('una confirmada que entra hoy lleva a la llegada', async () => {
    apiCon()
    const usuario = abrir(`/reservas/${CODIGO}`)

    await usuario.click(await screen.findByRole('button', { name: 'Registrar llegada' }))
    expect(await screen.findByRole('region', { name: 'Lo que falta' })).toBeInTheDocument()
  })

  it('antes de la entrada el botón dice desde cuándo', async () => {
    const entrada = sumarDias(HOY, 3)
    apiCon({ reserva: { entrada, salida: sumarDias(HOY, 6) } })
    abrir(`/reservas/${CODIGO}`)

    const boton = await screen.findByRole('button', { name: 'Registrar llegada' })
    expect(boton).toBeDisabled()
    expect(boton).toHaveAccessibleDescription(`Disponible desde ${fechaSinAnio(entrada)}`)
  })

  it('una pendiente que entra hoy ofrece «Continuar llegada» primero', async () => {
    apiCon({ reserva: { estado: 'PENDIENTE' } })
    abrir(`/reservas/${CODIGO}`)

    const botones = await screen.findAllByRole('button', { name: /Continuar llegada|Confirmar|Cancelar/ })
    expect(botones.map((boton) => boton.textContent)).toEqual(['Continuar llegada', 'Confirmar', 'Cancelar'])
  })

  it('la tabla de ocupantes muestra el TRA y abre el mismo diálogo', async () => {
    apiCon()
    const usuario = abrir(`/reservas/${CODIGO}`)

    const tabla = await screen.findByRole('table', { name: 'Ocupantes (3)' })
    expect(within(tabla).getByRole('row', { name: /Julián Mejía Ríos/ })).toHaveTextContent('Faltan 8 datos')
    await usuario.click(within(tabla).getByRole('button', { name: 'Completar datos de Julián Mejía Ríos' }))
    expect(await screen.findByRole('dialog', { name: 'Completar datos' })).toBeInTheDocument()
  })

  it('solo administración corrige el registro de una reserva en curso, y no antes de la entrada', async () => {
    apiCon({ reserva: { estado: 'EN_CURSO' } })
    const usuario = abrir(`/reservas/${CODIGO}`, 'ADMINISTRADOR')

    await usuario.click(await screen.findByRole('button', { name: 'Corregir registro' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Corregir registro de llegada' })
    const campo = within(dialogo).getByLabelText('Fecha y hora real de llegada')
    await usuario.clear(campo)
    await usuario.type(campo, `${sumarDias(HOY, -1)}T10:00`)
    await usuario.type(within(dialogo).getByLabelText(/Motivo/), 'Se registró tarde')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Corregir registro' }))

    expect(within(dialogo).getByText(/No puede ser anterior a la entrada/)).toBeInTheDocument()
  })

  it('el detalle dice quién registró la llegada, si se corrigió y por qué entró sin pagar (DEC-114)', async () => {
    apiCon({ reserva: { estado: 'EN_CURSO' } })
    abrir(`/reservas/${CODIGO}`)

    const llegada = (await screen.findByText(/por Andrés Felipe Gómez/)).closest('.dato')!
    expect(llegada).toHaveTextContent(/3:20 p\. m\. por Andrés Felipe Gómez/)
    expect(llegada).toHaveTextContent(/Corregida: Se registró tarde \(antes decía .*2:00 p\. m\.\)/)
    expect(screen.getByText('Entró sin pagar la estancia').closest('.dato')).toHaveTextContent('Cuenta por cobrar a una empresa')
  })

  it('«Corregir registro» parte de la hora vigente (DEC-114)', async () => {
    apiCon({ reserva: { estado: 'EN_CURSO' } })
    const usuario = abrir(`/reservas/${CODIGO}`, 'ADMINISTRADOR')
    await screen.findByText(/por Andrés Felipe Gómez/)
    await usuario.click(screen.getByRole('button', { name: 'Corregir registro' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Corregir registro de llegada' })
    expect(within(dialogo).getByLabelText('Fecha y hora real de llegada')).toHaveValue(`${HOY}T15:20`)
  })

  it('una reserva confirmada no pide la llegada registrada', async () => {
    const fetch = apiCon()
    abrir(`/reservas/${CODIGO}`)
    await screen.findByRole('heading', { level: 1, name: CODIGO })
    expect(fetch.mock.calls.some(([url]) => String(url).endsWith('/estancia'))).toBe(false)
  })

  it('recepción no ve «Corregir registro»', async () => {
    apiCon({ reserva: { estado: 'EN_CURSO' } })
    abrir(`/reservas/${CODIGO}`)

    expect(await screen.findByRole('button', { name: 'Registrar salida' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Corregir registro' })).not.toBeInTheDocument()
  })
})
