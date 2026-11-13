import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import type { Alojamiento } from '../../api/alojamiento'
import { consultarApartamento, type ApartamentoEncontrado } from '../../api/apartamentos'
import { ErrorApi } from '../../api/cliente'
import { cotizarEstancia, crearReserva, type Cotizacion, type Reserva } from '../../api/reservas'
import Aviso from '../../componentes/Aviso'
import Dato from '../../componentes/Dato'
import TablaDesglose from '../../componentes/TablaDesglose'
import { cantidad, dinero, fechaCorta, fechaHoy, horaEnPalabras, noches } from '../../formato'
import Campo from './Campo'
import { estimarExtras, salidasDelRechazo, textoCobroVehiculo, type DatosEstancia, type DatosOcupante, type DatosTitular } from './formulario'

export interface DatosLlegada {
  hora: string
  carros: string
  motos: string
  mascotas: string
}

/** «El anticipo incluye el parqueadero y las mascotas.», según lo que tenga la reserva (DEC-89). */
function textoAnticipo(extras: { parqueadero: number; mascotas: number }): string {
  const incluye = [extras.parqueadero > 0 && 'el parqueadero', extras.mascotas > 0 && 'las mascotas'].filter(Boolean)
  return `El anticipo incluye ${incluye.join(' y ')}.`
}

/** Solo dígitos, hasta tres, para los campos de cantidad. */
const soloCantidad = (texto: string) => texto.replace(/\D/g, '').slice(0, 3)

type Falla = { mensaje: string; detalles: string[]; regla: boolean; motivo: string | null }

function fallaDe(causa: unknown): Falla {
  if (causa instanceof ErrorApi) return { mensaje: causa.message, detalles: causa.detalles, regla: causa.estado === 409, motivo: causa.motivo }
  return { mensaje: 'Inténtalo de nuevo en un momento.', detalles: [], regla: false, motivo: null }
}

/**
 * Paso 4: el valor y lo que falta para crear (RN-04 · RP-02 · RP-03). El backend vuelve a comprobar todo al crear: otra
 * persona pudo tomar esas noches mientras tanto. Sus 409 llegan solo con un texto, así que se muestran en un aviso
 * general con las salidas posibles (`pms/DISENO.md`, «Para hablar con el equipo»), sin perder nada de lo escrito.
 */
export default function PasoResumen({ estancia, apartamento, titular, grupo, llegada, alojamiento, alCambiarLlegada, alCrear, alElegirOtro, alCambiarFechas, alRevisarPersonas, alVolver }: {
  estancia: DatosEstancia
  apartamento: ApartamentoEncontrado
  titular: DatosTitular
  grupo: DatosOcupante[]
  llegada: DatosLlegada
  alojamiento: Alojamiento
  alCambiarLlegada: (llegada: DatosLlegada) => void
  /** @param continuarLlegada el huésped está en el mostrador: se sigue a cobrar y registrar su llegada */
  alCrear: (reserva: Reserva, continuarLlegada: boolean) => void
  alElegirOtro: () => void
  alCambiarFechas: () => void
  alRevisarPersonas: () => void
  alVolver: () => void
}) {
  const { aceptaMascotas, limiteMascotas, cargoMascota, cupoParqueadero, cobroParqueadero, cargoCarro, cargoMoto } = alojamiento.parametros
  const [intento, setIntento] = useState(0)
  // El límite propio del apartamento (DEC-93). Mientras llega, el campo de mascotas espera; si no se pudo consultar,
  // rige el del hotel y el backend lo vuelve a revisar al crear
  const [limitePropio, setLimitePropio] = useState<{ codigo: string; limite: number | null } | null>(null)
  const idParqueadero = useId()
  const [cotizacion, setCotizacion] = useState<{ intento: number; valor: Cotizacion } | { intento: number; falla: Falla } | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [falla, setFalla] = useState<Falla | null>(null)
  // Reserva de mostrador: el huésped llegó sin reserva y entra hoy
  const entraHoy = estancia.entrada === fechaHoy()
  const continuarLlegada = useRef(true)

  useEffect(() => {
    let vigente = true
    cotizarEstancia({
      apartamento: apartamento.codigo,
      entrada: estancia.entrada,
      salida: estancia.salida,
      fechasNacimiento: grupo.map((ocupante) => ocupante.fechaNacimiento),
    })
      .then((valor) => {
        if (vigente) setCotizacion({ intento, valor })
      })
      .catch((causa: unknown) => {
        if (vigente) setCotizacion({ intento, falla: fallaDe(causa) })
      })
    return () => {
      vigente = false
    }
  }, [apartamento.codigo, estancia, grupo, intento])

  useEffect(() => {
    let vigente = true
    consultarApartamento(apartamento.codigo)
      .then((datos) => {
        if (vigente) setLimitePropio({ codigo: datos.codigo, limite: datos.limiteMascotas })
      })
      .catch(() => {
        // Sin el límite propio se usa el del hotel: el backend lo exige igual al crear
        if (vigente) setLimitePropio({ codigo: apartamento.codigo, limite: null })
      })
    return () => {
      vigente = false
    }
  }, [apartamento.codigo])

  const actual = cotizacion?.intento === intento ? cotizacion : null
  const valor = actual && 'valor' in actual ? actual.valor : null
  const fallaCotizar = actual && 'falla' in actual ? actual.falla : null

  const limiteListo = !aceptaMascotas || limitePropio?.codigo === apartamento.codigo
  const propio = limitePropio?.codigo === apartamento.codigo ? limitePropio.limite : null
  const limite = aceptaMascotas ? (propio ?? limiteMascotas) : 0
  const hayParqueadero = cupoParqueadero > 0
  const carros = hayParqueadero ? Number(llegada.carros || 0) : 0
  const motos = hayParqueadero ? Number(llegada.motos || 0) : 0
  const escritas = Number(llegada.mascotas || 0)
  const mascotas = limite > 0 ? escritas : 0
  // Escritas para otro apartamento que este no recibe: no se envían, y se avisa
  const descartadas = limiteListo && aceptaMascotas && limite === 0 ? escritas : 0
  // Los topes se revisan mientras se escribe: así el error aparece en el campo que lo causa, no solo al enviar
  const errorCupo = hayParqueadero && carros + motos > cupoParqueadero
    ? `El parqueadero tiene ${cantidad(cupoParqueadero, 'cupo', 'cupos')} en total, entre carros y motos.`
    : undefined
  const errorMascotas = limite > 0 && escritas > limite
    ? `Este apartamento acepta hasta ${cantidad(limite, 'mascota', 'mascotas')}.`
    : undefined
  const extras = estimarExtras(
    { cobroParqueadero, cargoCarro, cargoMoto, cargoMascota },
    { carros, motos, mascotas },
    noches(estancia.entrada, estancia.salida),
  )

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setFalla(null)
    if (errorCupo || errorMascotas) {
      evento.currentTarget.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
      return
    }

    setEnviando(true)
    const llegadaAhora = entraHoy && continuarLlegada.current
    try {
      alCrear(await crearReserva({
        apartamento: apartamento.codigo,
        entrada: estancia.entrada,
        salida: estancia.salida,
        canalOrigen: 'DIRECTO',
        titular: { ...titular, nombre: titular.nombre.trim(), correo: titular.correo.trim() },
        ocupantes: grupo.map((ocupante) => ({
          nombre: ocupante.nombre.trim(),
          fechaNacimiento: ocupante.fechaNacimiento,
          ...(ocupante.tipoDocumento ? { tipoDocumento: ocupante.tipoDocumento, numeroDocumento: ocupante.numeroDocumento.trim() } : {}),
          ...(ocupante.nacionalidad.trim() ? { nacionalidad: ocupante.nacionalidad.trim() } : {}),
        })),
        ...(llegada.hora ? { horaEstimadaLlegada: llegada.hora } : {}),
        carros,
        motos,
        mascotas,
      }), llegadaAhora)
    } catch (causa) {
      setFalla(fallaDe(causa))
      setEnviando(false)
      window.scrollTo?.({ top: 0 })
    }
  }

  // Solo las salidas que pueden resolver el rechazo; la primera es la principal
  const salidas = (motivo: string | null) => {
    const textos = {
      apartamento: { texto: 'Elegir otro apartamento', alPulsar: alElegirOtro },
      fechas: { texto: 'Cambiar fechas', alPulsar: alCambiarFechas },
      personas: { texto: 'Revisar personas', alPulsar: alRevisarPersonas },
    }
    return (
      <div className="paso-salidas">
        {salidasDelRechazo(motivo).map((salida, indice) => (
          <button
            key={salida}
            type="button"
            className={indice === 0 ? 'boton' : 'boton boton-secundario'}
            onClick={textos[salida].alPulsar}
          >
            {textos[salida].texto}
          </button>
        ))}
      </div>
    )
  }

  return (
    <form className="paso" onSubmit={enviar} noValidate>
      {falla && (
        <div className="paso-error">
          <Aviso
            tipo="error"
            asunto="No se creó la reserva"
            mensaje={`${falla.mensaje.replace(/\.?$/, '.')} No se perdió ningún dato.`}
            detalles={falla.detalles}
          />
          {falla.regla && salidas(falla.motivo)}
        </div>
      )}

      <section className="paso" aria-labelledby="titulo-resumen">
        <h2 id="titulo-resumen" tabIndex={-1}>Resumen</h2>
        <dl className="tarjeta datos">
          <Dato nombre="Apartamento">
            <span className="codigo">{apartamento.codigo}</span> · {apartamento.nombre}
          </Dato>
          <Dato nombre="Fechas">
            {fechaCorta(estancia.entrada)} → {fechaCorta(estancia.salida)} ·{' '}
            {cantidad(noches(estancia.entrada, estancia.salida), 'noche', 'noches')}
          </Dato>
          <Dato nombre="Ocupantes">{grupo.length}</Dato>
          <Dato nombre="Titular">
            {titular.nombre} · <span className="codigo">{titular.tipoDocumento} {titular.numeroDocumento}</span>
          </Dato>
        </dl>
      </section>

      <section className="paso" aria-labelledby="titulo-desglose-nueva">
        <h3 id="titulo-desglose-nueva">Desglose por noche</h3>
        {!actual && (
          <div className="tarjeta pago-cargando" aria-busy="true">
            <span className="fantasma" />
            <span className="fantasma" />
            <p className="suave" role="status">Calculando el valor…</p>
          </div>
        )}
        {fallaCotizar && (
          <div className="paso-error">
            <Aviso
              tipo="error"
              asunto="No pudimos calcular el valor"
              mensaje={fallaCotizar.mensaje}
              detalles={fallaCotizar.detalles}
              accion={
                <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
                  Reintentar
                </button>
              }
            />
            {fallaCotizar.regla && salidas(fallaCotizar.motivo)}
          </div>
        )}
        {valor && <TablaDesglose idTitulo="titulo-desglose-nueva" desglose={valor.desglose} total={valor.total} />}
      </section>

      <section className="tarjeta paso-tarjeta" aria-labelledby="titulo-llegada">
        <h3 id="titulo-llegada">Llegada, vehículos y mascotas</h3>
        <div className="paso-campos-2">
          <Campo
            etiqueta="Hora estimada de llegada"
            nota="opcional"
            type="time"
            value={llegada.hora}
            disabled={enviando}
            pista={llegada.hora ? horaEnPalabras(llegada.hora) : undefined}
            onChange={(evento) => alCambiarLlegada({ ...llegada, hora: evento.target.value })}
          />
          {aceptaMascotas && (!limiteListo || limite > 0) && (
            <Campo
              etiqueta="Número de mascotas"
              inputMode="numeric"
              autoComplete="off"
              value={llegada.mascotas}
              disabled={enviando || !limiteListo}
              error={errorMascotas}
              pista={limiteListo
                ? `Este apartamento acepta hasta ${cantidad(limite, 'mascota', 'mascotas')}, con un cargo de ${dinero(cargoMascota)} por mascota, una vez por estancia.`
                : 'Consultando cuántas mascotas admite este apartamento…'}
              onChange={(evento) => alCambiarLlegada({ ...llegada, mascotas: soloCantidad(evento.target.value) })}
            />
          )}
        </div>
        {aceptaMascotas && limiteListo && limite === 0 && (
          <p className="suave">
            Este apartamento no recibe mascotas
            {descartadas > 0 && `: la reserva se creará sin ${descartadas === 1 ? 'la que escribiste' : `las ${descartadas} que escribiste`}`}.
          </p>
        )}
        {hayParqueadero && (
          <fieldset className="paso-grupo" aria-describedby={`${idParqueadero}-${errorCupo ? 'error' : 'pista'}`}>
            <legend>Parqueadero</legend>
            {errorCupo ? (
              <p className="campo-mensaje" id={`${idParqueadero}-error`}>{errorCupo}</p>
            ) : (
              <p className="campo-pista" id={`${idParqueadero}-pista`}>
                Carros y motos comparten {cantidad(cupoParqueadero, 'cupo', 'cupos')} con las demás reservas.
              </p>
            )}
            <div className="paso-campos-2">
              <Campo
                etiqueta="Carros"
                inputMode="numeric"
                autoComplete="off"
                value={llegada.carros}
                disabled={enviando}
                invalido={Boolean(errorCupo)}
                pista={textoCobroVehiculo(cargoCarro, 'carro', cobroParqueadero)}
                onChange={(evento) => alCambiarLlegada({ ...llegada, carros: soloCantidad(evento.target.value) })}
              />
              <Campo
                etiqueta="Motos"
                inputMode="numeric"
                autoComplete="off"
                value={llegada.motos}
                disabled={enviando}
                invalido={Boolean(errorCupo)}
                pista={textoCobroVehiculo(cargoMoto, 'moto', cobroParqueadero)}
                onChange={(evento) => alCambiarLlegada({ ...llegada, motos: soloCantidad(evento.target.value) })}
              />
            </div>
          </fieldset>
        )}
        {!llegada.hora && (
          <Aviso tipo="info" asunto="Hora de llegada" mensaje="Puedes crear la reserva sin ella, pero la necesitarás para confirmarla." />
        )}
      </section>

      {enviando && <p className="suave" role="status">Creando la reserva…</p>}
      <div className="paso-pie">
        <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alVolver}>
          <span aria-hidden="true">←</span> Atrás
        </button>
        <div className="paso-total">
          {valor && (
            <p aria-live="polite" aria-atomic="true">
              <span className="paso-total-rotulo">Total</span>
              <strong className="codigo">{dinero(valor.total + extras.total)}</strong>
              {extras.total > 0 && (
                <span className="paso-total-extra">
                  Estancia <span className="codigo">{dinero(valor.total)}</span>
                  {extras.parqueadero > 0 && <> + parqueadero <span className="codigo">{dinero(extras.parqueadero)}</span></>}
                  {extras.mascotas > 0 && <> + mascotas <span className="codigo">{dinero(extras.mascotas)}</span></>}.{' '}
                  {textoAnticipo(extras)}
                </span>
              )}
            </p>
          )}
          {entraHoy ? (
            // La principal va primero: Enter en un campo envía con ella. El CSS la deja a la derecha en el computador
            <div className="paso-pie-botones">
              <button
                type="submit"
                className="boton"
                disabled={enviando || !valor || !limiteListo}
                onClick={() => { continuarLlegada.current = true }}
              >
                {enviando ? 'Creando reserva…' : 'Crear y continuar con la llegada'}
              </button>
              <button
                type="submit"
                className="boton boton-secundario"
                disabled={enviando || !valor || !limiteListo}
                onClick={() => { continuarLlegada.current = false }}
              >
                Crear reserva
              </button>
            </div>
          ) : (
            <button type="submit" className="boton" disabled={enviando || !valor || !limiteListo}>
              {enviando ? 'Creando reserva…' : 'Crear reserva'}
            </button>
          )}
        </div>
      </div>
    </form>
  )
}
