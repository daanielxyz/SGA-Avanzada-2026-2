import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Alojamiento } from '../api/alojamiento'
import type { ApartamentoPublico, EstanciaPortal, Precio } from '../api/apartamentos'
import { fechaDesdeHoy, masDias } from '../formato'
import { borrarSesion, guardarSesion } from '../sesion/almacen'
import Reservar from './Reservar'

const ENTRADA = fechaDesdeHoy(10)
const SALIDA = masDias(ENTRADA, 2)

const precio = (pagan: number): Precio => ({
  desglose: [
    { noche: ENTRADA, tarifa: 100000, ocupantesFacturables: pagan, subtotal: 100000 * pagan },
    { noche: masDias(ENTRADA, 1), tarifa: 100000, ocupantesFacturables: pagan, subtotal: 100000 * pagan },
  ],
  total: 200000 * pagan,
})

const SUITE: ApartamentoPublico = {
  codigo: 'APT-101',
  nombre: 'Suite Jardín',
  descripcion: null,
  capacidad: 4,
  dormitorios: 2,
  imagenes: [],
  caracteristicas: ['Wifi'],
  precio: null,
  proximaNocheLibre: null,
}

const LIBRE: EstanciaPortal = { disponible: true, precio: precio(1), proximaNocheLibre: ENTRADA }

const ALOJAMIENTO: Alojamiento = {
  nombre: 'Hotel de Prueba',
  parametros: {
    umbralEdadFacturable: 12, horaEntrada: '15:00:00', horaSalida: '11:00:00', cupoParqueadero: 5,
    aceptaMascotas: true, limiteMascotas: 2,
    cargoMascota: 30000, cobroParqueadero: 'POR_NOCHE', cargoCarro: 15000, cargoMoto: 8000,
  },
}

const POLITICA = {
  tramos: [
    { antelacionMinHoras: 0, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 50, montoFijo: null } },
    { antelacionMinHoras: 48, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 0, montoFijo: null } },
  ],
  penalizacionNoShow: { base: 'VALOR_TOTAL', porcentaje: 100, montoFijo: null },
}

const APARTADA = {
  reserva: {
    codigo: 'RES-2026-00042', valor: precio(1), avisoCuenta: null, extras: { parqueadero: 0, mascotas: 0, total: 0 },
  },
  montoAPagar: 60000,
  pagarAntesDe: `${ENTRADA}T15:30:00`,
  clave: 'clave-de-la-reserva',
}

const CONFIRMADA = {
  codigo: 'RES-2026-00042', estado: 'CONFIRMADA', apartamento: 'APT-101', entrada: ENTRADA, salida: SALIDA,
  total: 200000,
}

const respuesta = (cuerpo: unknown, estado = 200) => new Response(JSON.stringify(cuerpo), { status: estado })

/** La API simulada responde según la ruta; `pagos` y `reservas` se consumen en orden. */
function laApi({ estancia = LIBRE, cotizacion = precio(1), reservas = [respuesta(APARTADA, 201)],
  pagos = [respuesta(CONFIRMADA)], liberaciones = [new Response(null, { status: 204 })], apartamento = SUITE,
  cuenta }: {
  estancia?: EstanciaPortal; cotizacion?: Precio; reservas?: Response[]; pagos?: Response[]
  liberaciones?: Response[]; apartamento?: ApartamentoPublico; cuenta?: unknown
} = {}) {
  const fetch = vi.fn((ruta: string) => {
    if (ruta === '/api/mi-cuenta') return Promise.resolve(cuenta ? respuesta(cuenta) : respuesta({}, 500))
    if (ruta.includes('/estancia')) return Promise.resolve(respuesta(estancia))
    if (ruta === '/api/cotizaciones') return Promise.resolve(respuesta(cotizacion))
    if (ruta === '/api/portal/politica') return Promise.resolve(respuesta(POLITICA))
    if (ruta.endsWith('/pago')) return Promise.resolve(pagos.shift() ?? respuesta({}, 500))
    if (ruta.endsWith('/liberar')) return Promise.resolve(liberaciones.shift() ?? respuesta({}, 500))
    if (ruta === '/api/portal/reservas') return Promise.resolve(reservas.shift() ?? respuesta({}, 500))
    return Promise.resolve(respuesta(apartamento))
  })
  vi.stubGlobal('fetch', fetch)
  return fetch
}

function cuerpoDe(fetch: ReturnType<typeof laApi>, ruta: string) {
  const llamada = fetch.mock.calls.map((c) => c as unknown as [string, RequestInit])
    .find(([r]) => r.endsWith(ruta))
  return JSON.parse(String(llamada?.[1].body))
}

function llamadaA(fetch: ReturnType<typeof laApi>, ruta: string) {
  return fetch.mock.calls.map((c) => c as unknown as [string, RequestInit]).find(([r]) => r.endsWith(ruta))?.[1]
}

function abrir(grupo = 'adultos=1&ninos=0', entrada = ENTRADA) {
  render(
    <MemoryRouter initialEntries={[`/apartamentos/APT-101/reservar?entrada=${entrada}&salida=${SALIDA}&${grupo}`]}>
      <Routes>
        <Route path="/apartamentos/:codigo/reservar" element={<Reservar alojamiento={ALOJAMIENTO} />} />
        <Route path="/apartamentos/:codigo" element={<h1>Página del apartamento</h1>} />
      </Routes>
    </MemoryRouter>,
  )
}

function llenarTitular() {
  fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Ana' } })
  fireEvent.change(screen.getByLabelText('Apellidos'), { target: { value: ' Ríos Gómez ' } })
  fireEvent.change(screen.getByLabelText('Fecha de nacimiento'), { target: { value: '1990-05-01' } })
  fireEvent.change(screen.getByLabelText('Número de documento'), { target: { value: '1094' } })
  fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'ana@correo.co' } })
  fireEvent.change(screen.getByLabelText('Teléfono'), { target: { value: '3001234567' } })
}

afterEach(() => {
  vi.unstubAllGlobals()
  borrarSesion()
})

describe('Reservar', () => {
  it('abre con el resumen de lo que se reserva y un campo de nombre por cada persona', async () => {
    laApi()
    abrir('adultos=2&ninos=1')

    expect(await screen.findByRole('heading', { level: 1, name: 'Tus datos' })).toBeInTheDocument()
    const resumen = screen.getByRole('complementary', { name: 'Suite Jardín' })
    expect(resumen).toHaveTextContent('2 adultos y 1 niño')
    expect(resumen).toHaveTextContent(/Total 2 noches:\s*\$\s*200\.000/)
    expect(screen.getByLabelText('Nombres de la persona 2')).toBeInTheDocument()
    expect(screen.getByLabelText('Fecha de nacimiento de la persona 3')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nombres de la persona 4')).not.toBeInTheDocument()
  })

  it('una línea de pasos dice dónde va: el actual resaltado y los hechos marcados', async () => {
    laApi()
    abrir()
    await screen.findByLabelText('Nombres')

    const pasos = screen.getByRole('list', { name: 'Pasos de la reserva' })
    expect(within(pasos).getAllByRole('listitem').map((p) => p.textContent)).toEqual(['1Tus datos', '2Pago',
      '3Confirmación'])
    expect(within(pasos).getByText('Tus datos').closest('li')).toHaveAttribute('aria-current', 'step')

    llenarTitular()
    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })

    expect(within(pasos).getByText('Pago').closest('li')).toHaveAttribute('aria-current', 'step')
    expect(within(pasos).getByText('Tus datos').closest('li')).toHaveTextContent('(hecho)')
  })

  it('sin salida en la dirección no abre el formulario: pide las fechas', async () => {
    laApi()
    render(
      <MemoryRouter initialEntries={[`/apartamentos/APT-101/reservar?entrada=${ENTRADA}&adultos=1`]}>
        <Routes>
          <Route path="/apartamentos/:codigo/reservar" element={<Reservar alojamiento={ALOJAMIENTO} />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { level: 1, name: 'Revisa tus fechas' })).toBeInTheDocument()
    expect(screen.getByText('Elige la llegada y la salida.')).toBeInTheDocument()
  })

  it('el límite de mascotas propio del apartamento reemplaza al del hotel (DEC-93)', async () => {
    laApi({ apartamento: { ...SUITE, limiteMascotas: 1 } })
    abrir()
    await screen.findByLabelText('Nombres')

    fireEvent.change(screen.getByLabelText('Mascotas'), { target: { value: '2' } })

    expect(screen.getByLabelText('Mascotas')).toHaveValue(1)
    expect(screen.getByText(/^Hasta 1;/)).toBeInTheDocument()
  })

  it('un apartamento que no recibe mascotas no las ofrece, aunque el hotel sí', async () => {
    laApi({ apartamento: { ...SUITE, limiteMascotas: 0 } })
    abrir()
    await screen.findByLabelText('Nombres')

    expect(screen.queryByLabelText('Mascotas')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Carros')).toBeInTheDocument()
  })

  it('el campo de mascotas no deja pasar del límite del hotel', async () => {
    laApi()
    abrir()
    await screen.findByLabelText('Nombres')

    fireEvent.change(screen.getByLabelText('Mascotas'), { target: { value: '5' } })

    expect(screen.getByLabelText('Mascotas')).toHaveValue(2)
  })

  it('si ya no está libre lo dice y permite volver, sin mostrar el formulario', async () => {
    laApi({ estancia: { ...LIBRE, disponible: false } })
    abrir()

    expect(await screen.findByRole('heading', { name: 'Ya no está libre en esas fechas' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Correo')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al apartamento' }).getAttribute('href'))
      .toContain('/apartamentos/APT-101?')
  })

  it('con fechas que cambian quién paga, recalcula y avisa antes de continuar (RN-06)', async () => {
    const fetch = laApi({ cotizacion: precio(2) })
    abrir('adultos=1&ninos=1')
    await screen.findByLabelText('Nombres')

    fireEvent.change(screen.getByLabelText('Fecha de nacimiento'), { target: { value: '1990-05-01' } })
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Fecha de nacimiento de la persona 2'), { target: { value: '2005-01-01' } })

    const aviso = await screen.findByRole('status')
    expect(aviso).toHaveTextContent(/Tu total cambió a\s*\$\s*400\.000/)
    expect(aviso).toHaveTextContent('pagan 2 personas')
    expect(screen.getByRole('complementary', { name: 'Suite Jardín' })).toHaveTextContent(/\$\s*400\.000/)
    expect(cuerpoDe(fetch, '/api/cotizaciones').fechasNacimiento).toEqual(['1990-05-01', '2005-01-01'])
  })

  it('si las fechas coinciden con lo que dijo al buscar, no avisa nada', async () => {
    const fetch = laApi({ cotizacion: precio(1) })
    abrir()
    await screen.findByLabelText('Nombres')

    fireEvent.change(screen.getByLabelText('Fecha de nacimiento'), { target: { value: '1990-05-01' } })

    await vi.waitFor(() => expect(fetch.mock.calls.some(([r]) => r === '/api/cotizaciones')).toBe(true))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('envía el titular como primer ocupante con documento, apellidos aparte del nombre, prefijo y extras', async () => {
    const fetch = laApi()
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()
    fireEvent.change(screen.getByLabelText('Carros'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Mascotas'), { target: { value: '1' } })

    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })

    expect(cuerpoDe(fetch, '/api/portal/reservas')).toEqual({
      apartamento: 'APT-101',
      entrada: ENTRADA,
      salida: SALIDA,
      titular: { nombres: 'Ana', primerApellido: 'Ríos Gómez', tipoDocumento: 'CC', numeroDocumento: '1094',
        correo: 'ana@correo.co', telefono: '+573001234567' },
      ocupantes: [{ nombres: 'Ana', primerApellido: 'Ríos Gómez', fechaNacimiento: '1990-05-01', tipoDocumento: 'CC',
        numeroDocumento: '1094' }],
      carros: 1,
      motos: 0,
      mascotas: 1,
    })
  })

  it('muestra la entrada y la salida del hotel sin dejar editarlas', async () => {
    laApi()
    abrir()
    await screen.findByLabelText('Nombres')

    expect(screen.getByText('Entrada desde').nextElementSibling).toHaveTextContent(/3:00/)
    expect(screen.getByText('Salida hasta').nextElementSibling).toHaveTextContent(/11:00/)
    expect(screen.queryByLabelText(/hora/i)).not.toBeInTheDocument()
  })

  it('parqueadero y mascotas suben el total, y al pagar el anticipo y el resto ya los incluyen', async () => {
    laApi({ reservas: [respuesta({
      ...APARTADA,
      reserva: { ...APARTADA.reserva, extras: { parqueadero: 46000, mascotas: 30000, total: 76000 } },
      montoAPagar: 82800,
    }, 201)] })
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()
    fireEvent.change(screen.getByLabelText('Carros'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Motos'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Mascotas'), { target: { value: '1' } })

    const resumen = screen.getByRole('complementary', { name: 'Suite Jardín' })
    expect(within(resumen).getByText('Parqueadero').nextElementSibling).toHaveTextContent(/\$\s*46\.000/)
    expect(within(resumen).getByText('Mascotas').nextElementSibling).toHaveTextContent(/\$\s*30\.000/)
    expect(resumen).toHaveTextContent(/Total 2 noches:\s*\$\s*276\.000/)
    expect(screen.getByLabelText('Carros')).toHaveAccessibleDescription(/\$\s*15\.000 por noche/)
    expect(screen.getByLabelText('Motos')).toHaveAccessibleDescription(/\$\s*8\.000 por noche/)

    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })

    expect(resumen).toHaveTextContent(/Total 2 noches:\s*\$\s*276\.000/)
    expect(screen.getByText('Pagas ahora').nextElementSibling).toHaveTextContent(/\$\s*82\.800/)
    expect(screen.getByText('Pagas al llegar').nextElementSibling).toHaveTextContent(/\$\s*193\.200/)
  })

  it('si el hotel cobra el parqueadero por estadía, no lo multiplica por las noches', async () => {
    laApi()
    render(
      <MemoryRouter initialEntries={[`/apartamentos/APT-101/reservar?entrada=${ENTRADA}&salida=${SALIDA}`]}>
        <Routes>
          <Route path="/apartamentos/:codigo/reservar" element={(
            <Reservar alojamiento={{
              ...ALOJAMIENTO,
              parametros: { ...ALOJAMIENTO.parametros, cobroParqueadero: 'POR_ESTADIA' },
            }} />
          )} />
        </Routes>
      </MemoryRouter>,
    )
    await screen.findByLabelText('Nombres')
    fireEvent.change(screen.getByLabelText('Carros'), { target: { value: '2' } })

    const resumen = screen.getByRole('complementary', { name: 'Suite Jardín' })
    expect(within(resumen).getByText('Parqueadero').nextElementSibling).toHaveTextContent(/\$\s*30\.000/)
    expect(screen.getByLabelText('Carros')).toHaveAccessibleDescription(/por toda la estadía/)
  })

  it('un hotel con parqueadero gratis no muestra precio ni línea en el resumen', async () => {
    laApi()
    render(
      <MemoryRouter initialEntries={[`/apartamentos/APT-101/reservar?entrada=${ENTRADA}&salida=${SALIDA}`]}>
        <Routes>
          <Route path="/apartamentos/:codigo/reservar" element={(
            <Reservar alojamiento={{
              ...ALOJAMIENTO,
              parametros: { ...ALOJAMIENTO.parametros, cargoCarro: 0, cargoMoto: 0 },
            }} />
          )} />
        </Routes>
      </MemoryRouter>,
    )
    await screen.findByLabelText('Nombres')
    fireEvent.change(screen.getByLabelText('Carros'), { target: { value: '1' } })

    const resumen = screen.getByRole('complementary', { name: 'Suite Jardín' })
    expect(within(resumen).queryByText('Parqueadero')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Carros')).not.toHaveAccessibleDescription()
  })

  it('el teléfono arranca en Colombia y se puede buscar otro país, que cambia el prefijo enviado', async () => {
    const fetch = laApi()
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()

    await userEvent.click(screen.getByRole('button', { name: /País del teléfono: Colombia, \+57/ }))
    await userEvent.type(screen.getByRole('combobox', { name: 'Buscar país' }), 'espana')
    expect(within(screen.getByRole('listbox')).getAllByRole('option')).toHaveLength(1)
    await userEvent.keyboard('{Enter}')

    expect(screen.getByRole('button', { name: /País del teléfono: España, \+34/ })).toBeInTheDocument()
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Teléfono'), { target: { value: '612345678' } })
    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })

    expect(cuerpoDe(fetch, '/api/portal/reservas').titular.telefono).toBe('+34612345678')
  })

  it('un número pegado con prefijo ajusta el país solo', async () => {
    laApi()
    abrir()
    await screen.findByLabelText('Nombres')

    fireEvent.change(screen.getByLabelText('Teléfono'), { target: { value: '+34 612 345 678' } })

    expect(screen.getByRole('button', { name: /País del teléfono: España/ })).toBeInTheDocument()
    expect(screen.getByLabelText('Teléfono')).toHaveValue('612345678')
  })

  it('un teléfono que no es del país elegido no deja continuar', async () => {
    const fetch = laApi()
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()
    const telefono = screen.getByLabelText('Teléfono')
    fireEvent.change(telefono, { target: { value: '123' } })
    fireEvent.blur(telefono)

    expect(telefono).toHaveAccessibleDescription(/no parece de Colombia/)
    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))

    expect(fetch.mock.calls.some(([ruta]) => ruta === '/api/portal/reservas')).toBe(false)
  })

  it('si el backend rechaza la reserva, muestra por qué y no pierde lo escrito', async () => {
    laApi({ reservas: [respuesta({
      estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'El apartamento ya está reservado en esas fechas', detalles: [],
    }, 409)] })
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()

    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('ya está reservado')
    expect(screen.getByLabelText('Correo')).toHaveValue('ana@correo.co')
    expect(screen.getByRole('button', { name: 'Continuar al pago' })).toBeEnabled()
  })

  it('un rechazo con motivo se explica para el huésped y dice qué hacer, no con el texto técnico', async () => {
    laApi({ reservas: [respuesta({
      estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'Disponibilidad: APT-101 retenido por RES-2026-00041',
      detalles: [], motivo: 'NO_DISPONIBLE',
    }, 409)] })
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()

    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))

    const aviso = await screen.findByRole('alert')
    expect(aviso).toHaveTextContent('Esas noches ya no están libres en este apartamento')
    expect(aviso).not.toHaveTextContent('RES-2026-00041')
  })

  it('datos inválidos dicen qué revisar con el nombre de cada campo', async () => {
    laApi({ reservas: [respuesta({
      estado: 400, codigo: 'VALIDACION', mensaje: 'La solicitud tiene datos inválidos',
      detalles: ['titular.correo: no tiene formato de correo'],
    }, 400)] })
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()

    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))

    const aviso = await screen.findByRole('alert')
    expect(aviso).toHaveTextContent('Revisa estos datos:')
    expect(within(aviso).getByRole('listitem')).toHaveTextContent('Correo: no tiene formato de correo')
  })

  it('el pago muestra cuánto y hasta cuándo, las condiciones de cancelación y luego confirma', async () => {
    const fetch = laApi()
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()
    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })).toHaveFocus()
    expect(screen.getByText('Pagas ahora').nextElementSibling).toHaveTextContent(/\$\s*60\.000/)
    expect(screen.getByText('Pagas al llegar').nextElementSibling).toHaveTextContent(/\$\s*140\.000/)
    const condiciones = await screen.findByRole('region', { name: 'Si necesitas cancelar' })
    const lineas = within(condiciones).getAllByRole('listitem').map((l) => l.textContent)
    expect(lineas).toEqual([
      'Con 48 horas o más de antelación: no se retiene nada.',
      'Con menos de 48 horas: se retiene el 50 % del total de la reserva.',
      'Si no llegas: se retiene el 100 % del total de la reserva.',
    ])

    await userEvent.click(screen.getByRole('button', { name: /^Pagar/ }))

    expect(await screen.findByRole('heading', { level: 1, name: '¡Tu reserva está confirmada!' })).toHaveFocus()
    expect(screen.getByText('RES-2026-00042')).toBeInTheDocument()
    expect(cuerpoDe(fetch, '/pago')).toEqual({ tokenPago: 'tok_prueba' })
    // DEC-93: paga con la clave que recibió al reservar, en una cabecera
    expect(llamadaA(fetch, '/pago')?.headers).toMatchObject({ 'X-Reserva-Clave': 'clave-de-la-reserva' })
  })

  it('volver desde el pago suelta la reserva con su clave y deja lo escrito para continuar otra vez', async () => {
    const fetch = laApi({ reservas: [respuesta(APARTADA, 201), respuesta(APARTADA, 201)] })
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()
    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })

    await userEvent.click(screen.getByRole('button', { name: '← Cambiar mis datos' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Tus datos' })).toBeInTheDocument()
    const liberacion = llamadaA(fetch, '/api/portal/reservas/RES-2026-00042/liberar')
    expect(liberacion?.method).toBe('PUT')
    expect(liberacion?.headers).toMatchObject({ 'X-Reserva-Clave': 'clave-de-la-reserva' })
    expect(screen.getByLabelText('Nombres')).toHaveValue('Ana')
    expect(screen.getByLabelText('Correo')).toHaveValue('ana@correo.co')

    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })).toBeInTheDocument()
  })

  it('si no puede soltar la reserva, se queda en el pago y lo dice', async () => {
    laApi({ liberaciones: [respuesta({}, 500)] })
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()
    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })

    await userEvent.click(screen.getByRole('button', { name: '← Cambiar mis datos' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos soltar tu reserva para volver')
    expect(screen.getByRole('heading', { level: 1, name: 'Paga para confirmar' })).toBeInTheDocument()
  })

  it('un pago rechazado se queda en el paso de pago, dice por qué y se puede reintentar', async () => {
    const fetch = laApi({ pagos: [
      respuesta({ estado: 409, codigo: 'PAGO_RECHAZADO', mensaje: 'Fondos insuficientes', detalles: [] }, 409),
      respuesta(CONFIRMADA),
    ] })
    abrir()
    await screen.findByLabelText('Nombres')
    llenarTitular()
    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })

    await userEvent.click(screen.getByRole('button', { name: 'Simular un pago rechazado' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Fondos insuficientes')
    expect(cuerpoDe(fetch, '/pago')).toEqual({ tokenPago: 'tok_rechazado' })
    await userEvent.click(screen.getByRole('button', { name: /^Pagar/ }))
    expect(await screen.findByRole('heading', { level: 1, name: '¡Tu reserva está confirmada!' })).toBeInTheDocument()
  })

  it('una llegada en el pasado no abre el formulario', async () => {
    laApi()
    abrir('adultos=1&ninos=0', fechaDesdeHoy(-1))

    expect(await screen.findByRole('heading', { name: 'Revisa tus fechas' })).toBeInTheDocument()
  })
})

describe('Quién reserva y quiénes se hospedan (DEC-97)', () => {
  const CUENTA = {
    id: 'TIT-1', nombre: 'Ana Ríos', nombres: 'Ana', primerApellido: 'Ríos', segundoApellido: null,
    tipoDocumento: 'CC', numeroDocumento: '1094', correo: 'ana@correo.co', telefono: '+573001234567', validado: false,
  }
  const MANANA = new Date(Date.now() + 86400000).toISOString()

  it('con sesión, quien reserva es la cuenta: se muestra sin campos y solo pide su fecha de nacimiento', async () => {
    guardarSesion({ token: 'token-de-prueba', expiraEn: MANANA, nombre: 'Ana Ríos' })
    const fetch = laApi({ cuenta: CUENTA })
    abrir()

    expect(await screen.findByText('Ana Ríos')).toBeInTheDocument()
    expect(screen.getByText('Cédula de ciudadanía 1094')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Corrígelo en Mi cuenta/ })).toHaveAttribute('href', '/mi-cuenta')
    expect(screen.queryByLabelText('Correo')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Número de documento')).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Fecha de nacimiento'), { target: { value: '1990-05-01' } })
    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })

    const cuerpo = cuerpoDe(fetch, '/api/portal/reservas')
    expect(cuerpo.titular).toEqual({ nombres: 'Ana', primerApellido: 'Ríos', tipoDocumento: 'CC',
      numeroDocumento: '1094', correo: 'ana@correo.co', telefono: '+573001234567' })
    expect(cuerpo.ocupantes).toEqual([{ nombres: 'Ana', primerApellido: 'Ríos', fechaNacimiento: '1990-05-01',
      tipoDocumento: 'CC', numeroDocumento: '1094' }])
  })

  it('si la cuenta no carga, pide los datos como sin sesión', async () => {
    guardarSesion({ token: 'token-de-prueba', expiraEn: MANANA, nombre: 'Ana Ríos' })
    laApi()
    abrir()

    expect(await screen.findByLabelText('Correo')).toBeInTheDocument()
    expect(screen.getByLabelText('Nombres')).toBeInTheDocument()
  })

  it('quien reserva para otros deja su contacto y los huéspedes van aparte', async () => {
    const fetch = laApi()
    abrir('adultos=1&ninos=0')
    await screen.findByLabelText('Nombres')

    await userEvent.click(screen.getByRole('checkbox', { name: 'Yo también me hospedo' }))
    expect(screen.queryByLabelText('Fecha de nacimiento')).not.toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Quienes se hospedan' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Marta' } })
    fireEvent.change(screen.getByLabelText('Apellidos'), { target: { value: 'Gómez' } })
    fireEvent.change(screen.getByLabelText('Número de documento'), { target: { value: '4040' } })
    fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'marta@correo.co' } })
    fireEvent.change(screen.getByLabelText('Teléfono'), { target: { value: '3001234567' } })
    fireEvent.change(screen.getByLabelText('Nombres de la persona 1'), { target: { value: 'Tomás' } })
    fireEvent.change(screen.getByLabelText('Apellidos de la persona 1'), { target: { value: 'Gómez' } })
    fireEvent.change(screen.getByLabelText('Fecha de nacimiento de la persona 1'), { target: { value: '1999-02-02' } })
    await userEvent.click(screen.getByRole('button', { name: 'Continuar al pago' }))
    await screen.findByRole('heading', { level: 1, name: 'Paga para confirmar' })

    const cuerpo = cuerpoDe(fetch, '/api/portal/reservas')
    expect(cuerpo.titular).toEqual({ nombres: 'Marta', primerApellido: 'Gómez', tipoDocumento: 'CC',
      numeroDocumento: '4040', correo: 'marta@correo.co', telefono: '+573001234567' })
    expect(cuerpo.ocupantes).toEqual([{ nombres: 'Tomás', primerApellido: 'Gómez', fechaNacimiento: '1999-02-02' }])
  })
})
