import { useId, useState, type FormEvent } from 'react'
import type { Alojamiento } from '../../api/alojamiento'
import {
  cambiarTitular,
  corregirRegistro,
  cambiarExtras,
  type Estancia,
  type Ocupante,
  type Reserva,
} from '../../api/reservas'
import { validarTitular, type Titular } from '../../api/titulares'
import Aviso from '../../componentes/Aviso'
import Dialogo from '../../componentes/Dialogo'
import CampoMotivo from '../../folio/CampoMotivo'
import { cantidad, dinero, fechaCorta, fechaHoy, noches } from '../../formato'
import { errorParaMostrar, type ErrorMostrado } from '../mensajes'
import Campo from '../nueva/Campo'
import { estimarExtras, MENSAJE_TELEFONO, telefonoValido, textoCobroVehiculo } from '../nueva/formulario'
import { documentoDe, fechaYHora, MOTIVO_MAXIMO, TIPOS_DOCUMENTO } from './llegada'

/** Solo dígitos, hasta tres, para los campos de cantidad. */
const soloCantidad = (texto: string) => texto.replace(/\D/g, '').slice(0, 3)

/**
 * Cambiar vehículos y mascotas (BL-23 · DEC-116). Solo cambian esos cargos, con los precios de hoy del hotel; el valor de
 * las noches queda como se congeló al reservar. La diferencia queda como ajuste en el folio.
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
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)
  const idParqueadero = useId()

  // DEC-116: los extras se cambian mientras la reserva esté pendiente o confirmada, también el día de la llegada
  if (reserva.estado !== 'PENDIENTE' && reserva.estado !== 'CONFIRMADA') {
    return (
      <Dialogo titulo="Vehículos y mascotas" subtitulo={<span className="codigo">{reserva.codigo}</span>} alCerrar={alCerrar}>
        <div className="dialogo-contenido">
          <div className="dialogo-cuerpo">
            <Aviso
              tipo="info"
              asunto="Ya no se puede cambiar"
              mensaje="Solo se cambian en una reserva pendiente o confirmada. Si llegó un vehículo o una mascota durante la estancia, cárgalo como un consumo en el folio."
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
  const totalNuevo = reserva.valor.total + extras.total
  const diferencia = extras.total - reserva.extras.total

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    if (errorCupo || errorMascotas) {
      evento.currentTarget.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
      return
    }
    setEnviando(true)
    try {
      alModificar(await cambiarExtras(reserva.codigo, { carros, motos, mascotas }))
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
            {!sinCambios && (
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
          <p className="dialogo-texto suave">
            El valor de las noches no cambia. Los vehículos y las mascotas se cobran con los precios de hoy y la diferencia
            queda en el folio.
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

/** Para comparar nombres escritos por personas distintas: sin tildes, mayúsculas ni espacios de más. */
function comparable(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().replace(/\s+/g, ' ').trim()
}

/**
 * El huésped de la reserva con el mismo documento, si el nombre escrito no es el suyo: un mismo documento no puede ser
 * de dos personas. Si se eligió de la lista «¿Es uno de los huéspedes?», es esa persona aunque recepción haya separado
 * o corregido su nombre (ronda 3). El nombre se compara con el de la reserva y con el del TRA.
 */
function huespedConOtroNombre(datos: DatosTitular, ocupantes: Ocupante[], elegido = ''): Ocupante | null {
  const numero = datos.numeroDocumento.trim()
  const mismo = ocupantes.find((ocupante) => ocupante.tipoDocumento === datos.tipoDocumento && ocupante.numeroDocumento === numero)
  if (!mismo || mismo.id === elegido) return null
  const escrito = comparable([datos.nombres, datos.primerApellido, datos.segundoApellido].join(' '))
  const delTra = comparable([mismo.nombres, mismo.primerApellido, mismo.segundoApellido].filter(Boolean).join(' '))
  return escrito === comparable(mismo.nombre) || escrito === delTra ? null : mismo
}

function erroresDeTitular(datos: DatosTitular, ocupantes: Ocupante[] = [], elegido = ''): Partial<Record<keyof DatosTitular, string>> {
  const errores: Partial<Record<keyof DatosTitular, string>> = {}
  const otro = datos.numeroDocumento.trim() ? huespedConOtroNombre(datos, ocupantes, elegido) : null
  if (otro) {
    errores.numeroDocumento = `Ese documento es de ${otro.nombre}, huésped de esta reserva. Si el titular es esa persona, `
      + 'elígela arriba; si no, revisa el documento.'
  }
  if (!datos.nombres.trim()) errores.nombres = 'Escribe los nombres.'
  if (!datos.primerApellido.trim()) errores.primerApellido = 'Escribe el primer apellido.'
  if (!datos.numeroDocumento.trim()) errores.numeroDocumento = 'Escribe el número de documento.'
  if (!datos.correo.trim()) errores.correo = 'Escribe el correo.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(datos.correo.trim())) errores.correo = 'Revisa el correo: le falta algo, como la @.'
  if (!datos.telefono.trim()) errores.telefono = 'Escribe el teléfono.'
  else if (!telefonoValido(datos.telefono)) errores.telefono = MENSAJE_TELEFONO
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

  const [huesped, setHuesped] = useState('')
  // Un niño con registro civil no puede ser el titular (DEC-110)
  const conDocumento = reserva.ocupantes.filter((ocupante) => ocupante.numeroDocumento && ocupante.tipoDocumento !== 'RC')

  const cambiar = (campo: keyof DatosTitular, valor: string) => {
    setDatos({ ...datos, [campo]: valor })
    setErrores({ ...errores, [campo]: undefined })
  }

  function elegirHuesped(id: string) {
    setHuesped(id)
    const elegido = conDocumento.find((ocupante) => ocupante.id === id)
    // Antes de BL-07 el nombre llegaba entero: va todo en Nombres para que recepción lo separe
    setDatos(elegido
      ? {
          ...datos,
          nombres: elegido.nombres ?? (elegido.primerApellido ? '' : elegido.nombre),
          primerApellido: elegido.primerApellido ?? '',
          segundoApellido: elegido.segundoApellido ?? '',
          tipoDocumento: elegido.tipoDocumento ?? datos.tipoDocumento,
          numeroDocumento: elegido.numeroDocumento ?? '',
        }
      : { ...datos, nombres: '', primerApellido: '', segundoApellido: '', numeroDocumento: '' })
    setErrores({})
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    const encontrados = erroresDeTitular(datos, reserva.ocupantes, huesped)
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
            {conDocumento.length > 0 && (
              <Campo
                etiqueta="¿Es uno de los huéspedes?"
                opciones={[['', 'No, es otra persona'], ...conDocumento.map((ocupante) => [ocupante.id, ocupante.nombre] as const)]}
                value={huesped}
                pista="Elegirlo llena su nombre y su documento tal como están en la reserva."
                onChange={(evento) => elegirHuesped(evento.target.value)}
              />
            )}
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
export function DialogoCorregirRegistro({ reserva, vigente, alCerrar, alCorregir }: {
  reserva: Reserva
  /** La hora del registro vigente, para partir de ella (DEC-114); vacía si no se pudo leer. */
  vigente?: string | null
  alCerrar: () => void
  alCorregir: (estancia: Estancia) => void
}) {
  const [fechaHora, setFechaHora] = useState(vigente ? vigente.slice(0, 16) : '')
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
            pista={vigente
              ? `Hoy dice ${fechaYHora(vigente)}. Entre la entrada (${fechaCorta(reserva.entrada)}) y este momento.`
              : `Entre la entrada (${fechaCorta(reserva.entrada)}) y este momento.`}
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
