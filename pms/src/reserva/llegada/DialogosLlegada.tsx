import { useEffect, useId, useState, type FormEvent } from 'react'
import type { Alojamiento } from '../../api/alojamiento'
import {
  cambiarTitular,
  corregirRegistro,
  cotizarEstancia,
  modificarReserva,
  type Estancia,
  type Reserva,
} from '../../api/reservas'
import { validarTitular, type Titular } from '../../api/titulares'
import Aviso from '../../componentes/Aviso'
import Dialogo from '../../componentes/Dialogo'
import CampoMotivo from '../../folio/CampoMotivo'
import { cantidad, dinero, fechaCorta, fechaHoy, noches } from '../../formato'
import { errorParaMostrar, type ErrorMostrado } from '../mensajes'
import Campo from '../nueva/Campo'
import { estimarExtras, textoCobroVehiculo } from '../nueva/formulario'
import { documentoDe, fechaYHora, modificacionCon, MOTIVO_MAXIMO, TIPOS_DOCUMENTO } from './llegada'

/** Solo dígitos, hasta tres, para los campos de cantidad. */
const soloCantidad = (texto: string) => texto.replace(/\D/g, '').slice(0, 3)

/**
 * Cambiar vehículos y mascotas (BL-23 · TAR-06). El backend recalcula toda la estancia con las tarifas de hoy, así que
 * el total nuevo se cotiza antes de guardar; la diferencia queda como ajuste en el folio.
 *
 * @param limiteApartamento mascotas que admite este apartamento; nulo = las del alojamiento (DEC-93)
 */
export function DialogoVehiculos({ reserva, alojamiento, limiteApartamento, alCerrar, alModificar }: {
  reserva: Reserva
  alojamiento: Alojamiento
  limiteApartamento: number | null
  alCerrar: () => void
  alModificar: (reserva: Reserva) => void
}) {
  const { aceptaMascotas, limiteMascotas, cargoMascota, cupoParqueadero, cobroParqueadero, cargoCarro, cargoMoto } = alojamiento.parametros
  const [valores, setValores] = useState({ carros: String(reserva.carros), motos: String(reserva.motos), mascotas: String(reserva.mascotas) })
  const [estancia, setEstancia] = useState<{ total: number } | 'error' | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)
  const idParqueadero = useId()
  const yaEntro = reserva.entrada < fechaHoy()

  useEffect(() => {
    if (yaEntro) return
    let vigente = true
    cotizarEstancia({
      apartamento: reserva.apartamento,
      entrada: reserva.entrada,
      salida: reserva.salida,
      fechasNacimiento: reserva.ocupantes.map((ocupante) => ocupante.fechaNacimiento ?? ''),
    })
      .then((cotizacion) => {
        if (vigente) setEstancia({ total: cotizacion.total })
      })
      .catch(() => {
        if (vigente) setEstancia('error')
      })
    return () => {
      vigente = false
    }
  }, [reserva.apartamento, reserva.entrada, reserva.salida, reserva.ocupantes, yaEntro])

  if (yaEntro) {
    return (
      <Dialogo titulo="Vehículos y mascotas" subtitulo={<span className="codigo">{reserva.codigo}</span>} alCerrar={alCerrar}>
        <div className="dialogo-contenido">
          <div className="dialogo-cuerpo">
            <Aviso
              tipo="info"
              asunto="Ya no se puede modificar"
              mensaje={`La entrada fue el ${fechaCorta(reserva.entrada)}: una reserva solo se modifica hasta el día de su entrada.`}
            />
          </div>
          <div className="dialogo-pie">
            <button type="button" className="boton boton-secundario" onClick={alCerrar}>Volver</button>
          </div>
        </div>
      </Dialogo>
    )
  }

  const limite = aceptaMascotas ? (limiteApartamento ?? limiteMascotas) : 0
  const hayParqueadero = cupoParqueadero > 0
  const carros = Number(valores.carros || 0)
  const motos = Number(valores.motos || 0)
  const mascotas = Number(valores.mascotas || 0)
  const errorCupo = hayParqueadero && carros + motos > cupoParqueadero
    ? `El parqueadero tiene ${cantidad(cupoParqueadero, 'cupo', 'cupos')} en total, entre carros y motos.`
    : undefined
  const errorMascotas = mascotas > limite
    ? (limite > 0 ? `Este apartamento acepta hasta ${cantidad(limite, 'mascota', 'mascotas')}.` : 'Este apartamento no recibe mascotas.')
    : undefined
  const sinCambios = carros === reserva.carros && motos === reserva.motos && mascotas === reserva.mascotas
  const totalNoches = noches(reserva.entrada, reserva.salida)
  const extras = estimarExtras({ cobroParqueadero, cargoCarro, cargoMoto, cargoMascota }, { carros, motos, mascotas }, totalNoches)
  const totalActual = reserva.valor.total + reserva.extras.total
  const cotizada = estancia && estancia !== 'error' ? estancia.total : null
  const totalNuevo = cotizada === null ? null : cotizada + extras.total
  const diferencia = totalNuevo === null ? null : totalNuevo - totalActual

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    if (errorCupo || errorMascotas) {
      evento.currentTarget.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
      return
    }
    setEnviando(true)
    try {
      alModificar(await modificarReserva(reserva.codigo, modificacionCon(reserva, { carros, motos, mascotas })))
    } catch (causa) {
      setError(errorParaMostrar(causa))
      setEnviando(false)
    }
  }

  return (
    <Dialogo titulo="Vehículos y mascotas" subtitulo={<span className="codigo">{reserva.codigo}</span>} ocupado={enviando} alCerrar={alCerrar}>
      <form className="dialogo-contenido" onSubmit={enviar} noValidate>
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto="No se guardó el cambio" mensaje={error.mensaje} detalles={error.detalles} />}
          {hayParqueadero && (
            <fieldset className="paso-grupo" disabled={enviando} aria-describedby={`${idParqueadero}-${errorCupo ? 'error' : 'pista'}`}>
              <legend>Parqueadero</legend>
              {errorCupo ? (
                <p className="campo-mensaje" id={`${idParqueadero}-error`}>{errorCupo}</p>
              ) : (
                <p className="campo-pista" id={`${idParqueadero}-pista`}>
                  Carros y motos comparten {cantidad(cupoParqueadero, 'cupo', 'cupos')} con las demás reservas.
                </p>
              )}
              <div className="campos-2">
                <Campo
                  etiqueta="Carros"
                  inputMode="numeric"
                  autoComplete="off"
                  data-foco-inicial
                  value={valores.carros}
                  invalido={Boolean(errorCupo)}
                  pista={textoCobroVehiculo(cargoCarro, 'carro', cobroParqueadero)}
                  onChange={(evento) => setValores({ ...valores, carros: soloCantidad(evento.target.value) })}
                />
                <Campo
                  etiqueta="Motos"
                  inputMode="numeric"
                  autoComplete="off"
                  value={valores.motos}
                  invalido={Boolean(errorCupo)}
                  pista={textoCobroVehiculo(cargoMoto, 'moto', cobroParqueadero)}
                  onChange={(evento) => setValores({ ...valores, motos: soloCantidad(evento.target.value) })}
                />
              </div>
            </fieldset>
          )}
          {aceptaMascotas && (limite > 0 || reserva.mascotas > 0) && (
            <Campo
              etiqueta="Mascotas"
              inputMode="numeric"
              autoComplete="off"
              disabled={enviando}
              value={valores.mascotas}
              error={errorMascotas}
              pista={`Hasta ${cantidad(limite, 'mascota', 'mascotas')}, ${dinero(cargoMascota)} por mascota, una vez por estancia.`}
              onChange={(evento) => setValores({ ...valores, mascotas: soloCantidad(evento.target.value) })}
            />
          )}

          <div className="resumen-cambio" aria-live="polite" aria-atomic="true">
            {estancia === null && <p className="suave" role="status">Calculando el valor con las tarifas de hoy…</p>}
            {estancia === 'error' && (
              <p className="suave">No pudimos calcular el total nuevo. Al guardar, el valor se calcula igual con las tarifas de hoy.</p>
            )}
            {totalNuevo !== null && diferencia !== null && (
              <dl className="lineas">
                <dt>Total actual</dt>
                <dd>{dinero(totalActual)}</dd>
                <dt>Total nuevo</dt>
                <dd>{dinero(totalNuevo)}</dd>
                <div className="lineas-separador" />
                <dt className="lineas-fuerte">{diferencia >= 0 ? 'Se agregan' : 'Se descuentan'}</dt>
                <dd className="lineas-fuerte">{dinero(Math.abs(diferencia))}</dd>
              </dl>
            )}
          </div>
          {cotizada !== null && cotizada !== reserva.valor.total && (
            <Aviso
              tipo="advertencia"
              asunto="Cambiaron las tarifas"
              mensaje={`La estancia pasa de ${dinero(reserva.valor.total)} a ${dinero(cotizada)} con las tarifas de hoy, aunque solo cambies vehículos o mascotas.`}
            />
          )}
          <p className="dialogo-texto suave">
            El valor de la estancia se vuelve a calcular con las tarifas de hoy y la diferencia queda en el folio.
          </p>
          {enviando && <p className="suave" role="status">Guardando el cambio…</p>}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>Volver</button>
          <button type="submit" className="boton" disabled={enviando || sinCambios}>
            {enviando ? 'Guardando…' : 'Guardar cambios'}
          </button>
        </div>
      </form>
    </Dialogo>
  )
}

const TITULAR_VACIO = { nombres: '', primerApellido: '', segundoApellido: '', tipoDocumento: 'CC', numeroDocumento: '', correo: '', telefono: '' }
type DatosTitular = typeof TITULAR_VACIO

function erroresDeTitular(datos: DatosTitular): Partial<Record<keyof DatosTitular, string>> {
  const errores: Partial<Record<keyof DatosTitular, string>> = {}
  if (!datos.nombres.trim()) errores.nombres = 'Escribe los nombres.'
  if (!datos.primerApellido.trim()) errores.primerApellido = 'Escribe el primer apellido.'
  if (!datos.numeroDocumento.trim()) errores.numeroDocumento = 'Escribe el número de documento.'
  if (!datos.correo.trim()) errores.correo = 'Escribe el correo.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(datos.correo.trim())) errores.correo = 'Revisa el correo: le falta algo, como la @.'
  if (!datos.telefono.trim()) errores.telefono = 'Escribe el teléfono.'
  return errores
}

/** Cambiar el titular: quien responde por la reserva y su folio (TIT-01 · DEC-97). Los huéspedes y los pagos no cambian. */
export function DialogoTitular({ reserva, alCerrar, alCambiar }: {
  reserva: Reserva
  alCerrar: () => void
  alCambiar: (reserva: Reserva) => void
}) {
  const [datos, setDatos] = useState(TITULAR_VACIO)
  const [errores, setErrores] = useState<Partial<Record<keyof DatosTitular, string>>>({})
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)

  const cambiar = (campo: keyof DatosTitular, valor: string) => {
    setDatos({ ...datos, [campo]: valor })
    setErrores({ ...errores, [campo]: undefined })
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    const encontrados = erroresDeTitular(datos)
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) {
      const formulario = evento.currentTarget
      requestAnimationFrame(() => formulario.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus())
      return
    }
    setEnviando(true)
    try {
      alCambiar(await cambiarTitular(reserva.codigo, {
        nombres: datos.nombres.trim(),
        primerApellido: datos.primerApellido.trim(),
        ...(datos.segundoApellido.trim() ? { segundoApellido: datos.segundoApellido.trim() } : {}),
        tipoDocumento: datos.tipoDocumento,
        numeroDocumento: datos.numeroDocumento.trim(),
        correo: datos.correo.trim(),
        telefono: datos.telefono.trim(),
      }))
    } catch (causa) {
      setError(errorParaMostrar(causa))
      setEnviando(false)
    }
  }

  return (
    <Dialogo titulo="Cambiar titular" subtitulo={<span className="codigo">{reserva.codigo}</span>} ocupado={enviando} alCerrar={alCerrar}>
      <form className="dialogo-contenido" onSubmit={enviar} noValidate>
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto="No se cambió el titular" mensaje={error.mensaje} detalles={error.detalles} />}
          <p className="dialogo-texto">
            El titular nuevo responde por la reserva y su folio, y la verá en su cuenta. Los huéspedes y los pagos no cambian.
            Si ya hay un titular con ese documento, se usa ese.
          </p>
          <fieldset className="grupo-formulario" disabled={enviando}>
            <legend className="solo-lectores">Datos del titular nuevo</legend>
            <Campo etiqueta="Nombres" autoComplete="off" data-foco-inicial value={datos.nombres} error={errores.nombres} maxLength={120} onChange={(evento) => cambiar('nombres', evento.target.value)} />
            <div className="campos-2">
              <Campo etiqueta="Primer apellido" autoComplete="off" value={datos.primerApellido} error={errores.primerApellido} maxLength={60} onChange={(evento) => cambiar('primerApellido', evento.target.value)} />
              <Campo etiqueta="Segundo apellido" nota="opcional" autoComplete="off" value={datos.segundoApellido} maxLength={60} onChange={(evento) => cambiar('segundoApellido', evento.target.value)} />
            </div>
            <div className="campos-2">
              <Campo etiqueta="Tipo de documento" opciones={TIPOS_DOCUMENTO} value={datos.tipoDocumento} onChange={(evento) => cambiar('tipoDocumento', evento.target.value)} />
              <Campo etiqueta="Número de documento" autoComplete="off" value={datos.numeroDocumento} error={errores.numeroDocumento} maxLength={30} onChange={(evento) => cambiar('numeroDocumento', evento.target.value)} />
            </div>
            <Campo etiqueta="Correo" type="email" autoComplete="off" value={datos.correo} error={errores.correo} maxLength={150} onChange={(evento) => cambiar('correo', evento.target.value)} />
            <Campo etiqueta="Teléfono" type="tel" autoComplete="off" value={datos.telefono} error={errores.telefono} maxLength={30} onChange={(evento) => cambiar('telefono', evento.target.value)} />
          </fieldset>
          {enviando && <p className="suave" role="status">Cambiando el titular…</p>}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>Volver</button>
          <button type="submit" className="boton" disabled={enviando}>{enviando ? 'Cambiando…' : 'Cambiar titular'}</button>
        </div>
      </form>
    </Dialogo>
  )
}

/** Validar la identidad del titular, con el documento en la mano (DEC-98). No se deshace. */
export function DialogoValidar({ titular, alCerrar, alValidar }: {
  titular: Titular
  alCerrar: () => void
  alValidar: (titular: Titular) => void
}) {
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)

  async function validar() {
    setError(null)
    setEnviando(true)
    try {
      alValidar(await validarTitular(titular.id))
    } catch (causa) {
      setError(errorParaMostrar(causa))
      setEnviando(false)
    }
  }

  return (
    <Dialogo titulo="Validar identidad" ocupado={enviando} alCerrar={alCerrar}>
      <div className="dialogo-contenido">
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto="No se validó" mensaje={error.mensaje} detalles={error.detalles} />}
          <dl className="resumen-dialogo">
            <dt>Nombre</dt>
            <dd>{titular.nombre}</dd>
            <dt>Documento</dt>
            <dd className="codigo">{documentoDe(titular)}</dd>
          </dl>
          <p className="dialogo-texto">
            Confirma que revisaste el documento en persona. Esto no se deshace y, desde ahora, el huésped no podrá cambiar su
            nombre desde su cuenta.
          </p>
          {enviando && <p className="suave" role="status">Validando…</p>}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>Volver</button>
          <button type="button" className="boton" disabled={enviando} onClick={validar}>
            {enviando ? 'Validando…' : 'Validar identidad'}
          </button>
        </div>
      </div>
    </Dialogo>
  )
}

/** AAAA-MM-DDTHH:mm del momento actual, en la zona del navegador, para el tope del campo. */
function ahoraLocal(ahora = new Date()): string {
  return `${fechaHoy(ahora)}T${String(ahora.getHours()).padStart(2, '0')}:${String(ahora.getMinutes()).padStart(2, '0')}`
}

/**
 * Corregir la hora real de llegada (solo administración, reserva en curso). El registro actual no se borra: queda
 * anulado como historial (DEC-61).
 */
export function DialogoCorregirRegistro({ reserva, alCerrar, alCorregir }: {
  reserva: Reserva
  alCerrar: () => void
  alCorregir: (estancia: Estancia) => void
}) {
  const [fechaHora, setFechaHora] = useState('')
  const [motivo, setMotivo] = useState('')
  const [errores, setErrores] = useState<{ fechaHora?: string; motivo?: string }>({})
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)
  const [hecho, setHecho] = useState<string | null>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    const encontrados: typeof errores = {}
    if (!fechaHora) encontrados.fechaHora = 'Escribe la fecha y la hora.'
    else if (fechaHora.slice(0, 10) < reserva.entrada) encontrados.fechaHora = `No puede ser anterior a la entrada (${fechaCorta(reserva.entrada)}).`
    else if (fechaHora > ahoraLocal()) encontrados.fechaHora = 'No puede ser posterior a este momento.'
    if (!motivo.trim()) encontrados.motivo = 'Escribe el motivo.'
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return
    setEnviando(true)
    try {
      const estancia = await corregirRegistro(reserva.codigo, fechaHora, motivo.trim())
      const vigente = estancia.registros?.find((registro) => !registro.anulado)
      setHecho(fechaYHora(vigente?.fechaHora ?? fechaHora))
      setEnviando(false)
      alCorregir(estancia)
    } catch (causa) {
      setError(errorParaMostrar(causa))
      setEnviando(false)
    }
  }

  if (hecho) {
    return (
      <Dialogo titulo="Registro corregido" subtitulo={<span className="codigo">{reserva.codigo}</span>} alCerrar={alCerrar}>
        <div className="dialogo-contenido">
          <div className="dialogo-cuerpo">
            <Aviso tipo="exito" asunto="Llegada corregida" mensaje={`La llegada quedó registrada el ${hecho}. El registro anterior queda como historial.`} />
          </div>
          <div className="dialogo-pie">
            <button type="button" className="boton" onClick={alCerrar}>Cerrar</button>
          </div>
        </div>
      </Dialogo>
    )
  }

  return (
    <Dialogo titulo="Corregir registro de llegada" subtitulo={<span className="codigo">{reserva.codigo}</span>} ocupado={enviando} alCerrar={alCerrar}>
      <form className="dialogo-contenido" onSubmit={enviar} noValidate>
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto="No se corrigió" mensaje={error.mensaje} detalles={error.detalles} />}
          <Campo
            etiqueta="Fecha y hora real de llegada"
            type="datetime-local"
            data-foco-inicial
            disabled={enviando}
            value={fechaHora}
            min={`${reserva.entrada}T00:00`}
            max={ahoraLocal()}
            error={errores.fechaHora}
            pista={`Entre la entrada (${fechaCorta(reserva.entrada)}) y este momento.`}
            onChange={(evento) => {
              setFechaHora(evento.target.value)
              setErrores({ ...errores, fechaHora: undefined })
            }}
          />
          <CampoMotivo
            id="corregir-registro-motivo"
            rotulo="Motivo"
            valor={motivo}
            maximo={MOTIVO_MAXIMO}
            deshabilitado={enviando}
            error={errores.motivo}
            alCambiar={(valor) => {
              setMotivo(valor)
              setErrores({ ...errores, motivo: undefined })
            }}
          />
          <p className="dialogo-texto suave">El registro actual no se borra: queda anulado como historial.</p>
          {enviando && <p className="suave" role="status">Corrigiendo el registro…</p>}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>Volver</button>
          <button type="submit" className="boton" disabled={enviando}>{enviando ? 'Corrigiendo…' : 'Corregir registro'}</button>
        </div>
      </form>
    </Dialogo>
  )
}
