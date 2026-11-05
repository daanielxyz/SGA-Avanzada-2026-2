import { useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import type { Alojamiento } from '../api/alojamiento'
import {
  consultarApartamento,
  consultarEstancia,
  type ApartamentoPublico,
  type Estadia,
  type Grupo,
  type Precio,
} from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { SIN_EXTRAS, type Extras, type PagoAprobado, type ReservaApartada } from '../api/reservas'
import { problemaDeEstadia } from '../busqueda/estadia'
import { fechaDesdeHoy, masDias } from '../formato'
import Confirmacion from '../reservar/Confirmacion'
import PasoDatos from '../reservar/PasoDatos'
import PasoPago from '../reservar/PasoPago'
import Resumen from '../reservar/Resumen'

type Carga =
  | { estado: 'cargando' }
  | { estado: 'problema'; titulo: string; mensaje: string | null }
  | { estado: 'listo'; apartamento: ApartamentoPublico; precio: Precio }

type Paso =
  | { numero: 1 }
  | { numero: 2; apartada: ReservaApartada }
  | { numero: 3; apartada: ReservaApartada; pago: PagoAprobado }

const TITULOS = { 1: 'Tus datos', 2: 'Paga para confirmar', 3: '¡Tu reserva está confirmada!' }

/**
 * Reservar desde el portal (CU-10 · DEC-69): datos, pago y confirmación, con un resumen siempre a la vista. Abre con
 * las fechas y el grupo que traía la página del apartamento, y antes revisa que sigan libres.
 *
 * @param alojamiento hora de entrada y reglas de parqueadero y mascotas; vacío mientras llega
 */
export default function Reservar({ alojamiento }: { alojamiento: Alojamiento | null }) {
  const { codigo = '' } = useParams()
  const [consulta] = useSearchParams()
  const entrada = consulta.get('entrada') ?? fechaDesdeHoy()
  const salida = consulta.get('salida') ?? masDias(entrada, 1)
  const adultos = Math.max(1, Number(consulta.get('adultos')) || 1)
  const ninos = Math.max(0, Number(consulta.get('ninos')) || 0)
  const estadia: Estadia = { entrada, salida }
  const grupo: Grupo = { adultos, ninos }

  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [paso, setPaso] = useState<Paso>({ numero: 1 })
  const [precioVigente, setPrecioVigente] = useState<Precio | null>(null)
  const [extrasVigentes, setExtrasVigentes] = useState<Extras>(SIN_EXTRAS)
  const titulo = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    let vigente = true
    const problema = problemaDeEstadia({ entrada, salida })
    const cargar = async (): Promise<Carga> => {
      if (problema) {
        return { estado: 'problema', titulo: 'Revisa tus fechas', mensaje: problema }
      }
      const [apartamento, estancia] = await Promise.all([
        consultarApartamento(codigo),
        consultarEstancia(codigo, { entrada, salida }, { adultos, ninos }),
      ])
      return estancia.disponible
        ? { estado: 'listo', apartamento, precio: estancia.precio }
        : { estado: 'problema', titulo: 'Ya no está libre en esas fechas', mensaje: 'Prueba con otras fechas.' }
    }
    cargar()
      .then((c) => vigente && setCarga(c))
      .catch((error: unknown) => vigente && setCarga({
        estado: 'problema',
        titulo: 'No pudimos abrir tu reserva',
        mensaje: error instanceof ErrorApi ? error.message : 'Intenta de nuevo en un momento.',
      }))
    return () => {
      vigente = false
    }
  }, [codigo, entrada, salida, adultos, ninos])

  // Al cambiar de paso, el foco y la vista vuelven al título: el lector de pantalla y el celular siguen el flujo
  useEffect(() => {
    if (paso.numero > 1) {
      titulo.current?.focus()
      titulo.current?.scrollIntoView?.({ block: 'center' })
    }
  }, [paso.numero])

  const volver = `/apartamentos/${encodeURIComponent(codigo)}?${consulta}`
  if (carga.estado === 'cargando') {
    return <main className="ficha"><p className="suave">Cargando tu reserva…</p></main>
  }
  if (carga.estado === 'problema') {
    return (
      <main className="ficha">
        <h1>{carga.titulo}</h1>
        {carga.mensaje && <p>{carga.mensaje}</p>}
        <Link to={volver}>Volver al apartamento</Link>
      </main>
    )
  }

  return (
    <main className="ficha">
      {paso.numero === 1 && <Link to={volver} className="volver">← Volver al apartamento</Link>}
      <div className="reservar-columnas">
        <div className="reservar-principal">
          {paso.numero < 3 && <p className="suave paso-actual">Paso {paso.numero} de 2</p>}
          <h1 ref={titulo} tabIndex={-1}>{TITULOS[paso.numero]}</h1>
          {paso.numero === 1 && (
            <PasoDatos codigo={codigo} estadia={estadia} grupo={grupo} alojamiento={alojamiento}
              precioBuscado={carga.precio} alCambiarPrecio={setPrecioVigente} alCambiarExtras={setExtrasVigentes}
              alReservar={(apartada) => setPaso({ numero: 2, apartada })} />
          )}
          {paso.numero === 2 && (
            <PasoPago apartada={paso.apartada}
              alPagar={(pago) => setPaso({ numero: 3, apartada: paso.apartada, pago })} />
          )}
          {paso.numero === 3 && <Confirmacion pago={paso.pago} avisoCuenta={paso.apartada.reserva.avisoCuenta} />}
        </div>
        <Resumen apartamento={carga.apartamento} estadia={estadia} grupo={grupo}
          precio={paso.numero === 1 ? precioVigente ?? carga.precio : paso.apartada.reserva.valor}
          extras={paso.numero === 1 ? extrasVigentes : paso.apartada.reserva.extras} />
      </div>
    </main>
  )
}
