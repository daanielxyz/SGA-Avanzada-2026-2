import { useState, type FormEvent, type ReactNode } from 'react'
import type { ServicioAdicional } from '../api/alojamiento'
import { cargarConsumo, type Folio } from '../api/folio'
import Aviso from '../componentes/Aviso'
import { dinero } from '../formato'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'

const CANTIDAD_MAXIMA = 99

/**
 * Cargar un consumo del catálogo con su cantidad (CAR-05 · BL-12 · DEC-107). El valor sale del catálogo y no se
 * edita; el cargo queda con la fecha de hoy. Sirve solo y como segundo paso de «Corregir».
 *
 * @param servicios   el catálogo ya filtrado: activos y con cobro
 * @param inicial     lo que se propone; un servicio que no está en `servicios` deja el campo vacío
 * @param encabezado  avisos propios de quien lo usa, arriba del formulario
 * @param alRegistrar recibe el folio ya actualizado
 */
export default function FormularioConsumo({
  codigo, servicios, inicial, encabezado, textoVolver, alVolver, alRegistrar, alEnviar,
}: {
  codigo: string
  servicios: ServicioAdicional[]
  inicial?: { servicioId: string | null; cantidad: number }
  encabezado?: ReactNode
  textoVolver: string
  alVolver: () => void
  alRegistrar: (folio: Folio) => void
  alEnviar: (enviando: boolean) => void
}) {
  const propuesto = inicial ? inicial.servicioId : (servicios[0]?.id ?? null)
  const [servicioId, setServicioId] = useState(
    propuesto && servicios.some((servicio) => servicio.id === propuesto) ? propuesto : '',
  )
  const [cantidad, setCantidad] = useState(String(inicial?.cantidad ?? 1))
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState<{ servicio?: string; cantidad?: string }>({})
  const [error, setError] = useState<ErrorMostrado | null>(null)

  const servicio = servicios.find((opcion) => opcion.id === servicioId) ?? null
  const unidades = Number(cantidad)
  const cantidadValida = Number.isInteger(unidades) && unidades >= 1 && unidades <= CANTIDAD_MAXIMA

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const encontrados: typeof errores = {}
    if (!servicio) encontrados.servicio = 'Elige un consumo.'
    if (!cantidadValida) encontrados.cantidad = `Escribe una cantidad de 1 a ${CANTIDAD_MAXIMA}.`
    setErrores(encontrados)
    setError(null)
    if (!servicio || !cantidadValida) return

    setEnviando(true)
    alEnviar(true)
    try {
      alRegistrar(await cargarConsumo(codigo, servicio.id, unidades))
    } catch (causa) {
      setError(errorParaMostrar(causa))
    } finally {
      setEnviando(false)
      alEnviar(false)
    }
  }

  return (
    <form className="dialogo-contenido" onSubmit={enviar} noValidate>
      <div className="dialogo-cuerpo">
        {error && <Aviso tipo="error" asunto="No se cargó el consumo" mensaje={error.mensaje} detalles={error.detalles} />}
        {encabezado}
        {servicios.length === 0 && (
          <Aviso
            tipo="info"
            asunto="No hay consumos activos"
            mensaje="El catálogo del hotel no tiene consumos para cargar."
          />
        )}
        <div>
          <label className="campo-rotulo" htmlFor="consumo-servicio">
            Consumo
          </label>
          <select
            id="consumo-servicio"
            className="campo-control"
            value={servicioId}
            disabled={enviando || servicios.length === 0}
            data-foco-inicial={servicios.length > 0 || undefined}
            aria-invalid={Boolean(errores.servicio)}
            aria-describedby={errores.servicio ? 'consumo-servicio-error' : undefined}
            onChange={(evento) => setServicioId(evento.target.value)}
          >
            {(servicioId === '' || servicios.length === 0) && (
              <option value="">{servicios.length === 0 ? 'No hay consumos' : 'Elige un consumo'}</option>
            )}
            {servicios.map((opcion) => (
              <option key={opcion.id} value={opcion.id}>
                {opcion.nombre} · {dinero(opcion.valor)}
              </option>
            ))}
          </select>
          {errores.servicio && <p className="campo-mensaje" id="consumo-servicio-error">{errores.servicio}</p>}
        </div>
        <div>
          <label className="campo-rotulo" htmlFor="consumo-cantidad">
            Cantidad
          </label>
          <input
            id="consumo-cantidad"
            className="campo-control"
            inputMode="numeric"
            autoComplete="off"
            value={cantidad}
            disabled={enviando || servicios.length === 0}
            aria-invalid={Boolean(errores.cantidad)}
            aria-describedby={errores.cantidad ? 'consumo-cantidad-error' : 'consumo-cantidad-pista'}
            onChange={(evento) => setCantidad(evento.target.value.replace(/\D/g, '').slice(0, 2))}
          />
          {errores.cantidad
            ? <p className="campo-mensaje" id="consumo-cantidad-error">{errores.cantidad}</p>
            : <p className="campo-pista" id="consumo-cantidad-pista">De 1 a {CANTIDAD_MAXIMA}.</p>}
        </div>
        {servicios.length > 0 && (
          <dl className="resumen-dialogo">
            <dt>Valor unitario</dt>
            <dd className="codigo">{servicio ? dinero(servicio.valor) : '—'}</dd>
            <dt>Total</dt>
            <dd className="codigo resumen-total" aria-live="polite">
              {servicio && cantidadValida ? dinero(servicio.valor * unidades) : '—'}
            </dd>
          </dl>
        )}
        <p className="campo-pista">El valor sale del catálogo y no se puede cambiar.</p>
        {enviando && <p className="suave" role="status">Cargando el consumo…</p>}
      </div>
      <div className="dialogo-pie">
        <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alVolver}>
          {textoVolver}
        </button>
        <button type="submit" className="boton" disabled={enviando || servicios.length === 0}>
          {enviando ? 'Cargando…' : 'Cargar consumo'}
        </button>
      </div>
    </form>
  )
}
