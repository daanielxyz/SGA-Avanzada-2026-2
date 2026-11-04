import { useEffect, useState, type FormEvent } from 'react'
import type { Alojamiento } from '../../api/alojamiento'
import type { ApartamentoEncontrado } from '../../api/apartamentos'
import { ErrorApi } from '../../api/cliente'
import { cotizarEstancia, crearReserva, type Cotizacion, type Reserva } from '../../api/reservas'
import Aviso from '../../componentes/Aviso'
import Dato from '../../componentes/Dato'
import TablaDesglose from '../../componentes/TablaDesglose'
import { cantidad, dinero, fechaCorta, noches } from '../../formato'
import Campo from './Campo'
import type { DatosEstancia, DatosOcupante, DatosTitular, Errores } from './formulario'

export interface DatosLlegada {
  hora: string
  vehiculos: string
  mascotas: string
}

type Falla = { mensaje: string; detalles: string[]; regla: boolean }

function fallaDe(causa: unknown): Falla {
  if (causa instanceof ErrorApi) return { mensaje: causa.message, detalles: causa.detalles, regla: causa.estado === 409 }
  return { mensaje: 'Inténtalo de nuevo en un momento.', detalles: [], regla: false }
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
  alCrear: (reserva: Reserva) => void
  alElegirOtro: () => void
  alCambiarFechas: () => void
  alRevisarPersonas: () => void
  alVolver: () => void
}) {
  const { aceptaMascotas, limiteMascotas, cargoMascota, cupoParqueadero } = alojamiento.parametros
  const [intento, setIntento] = useState(0)
  const [cotizacion, setCotizacion] = useState<{ intento: number; valor: Cotizacion } | { intento: number; falla: Falla } | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [falla, setFalla] = useState<Falla | null>(null)
  const [errores, setErrores] = useState<Errores>({})

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

  const actual = cotizacion?.intento === intento ? cotizacion : null
  const valor = actual && 'valor' in actual ? actual.valor : null
  const fallaCotizar = actual && 'falla' in actual ? actual.falla : null
  const mascotas = Number(llegada.mascotas || 0)
  const cargoMascotas = aceptaMascotas ? mascotas * cargoMascota : 0

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const encontrados: Errores = {}
    if (aceptaMascotas && mascotas > limiteMascotas) {
      encontrados.mascotas = `Este hotel acepta hasta ${cantidad(limiteMascotas, 'mascota', 'mascotas')}.`
    }
    setErrores(encontrados)
    setFalla(null)
    if (Object.keys(encontrados).length > 0) return

    setEnviando(true)
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
        vehiculos: cupoParqueadero > 0 ? Number(llegada.vehiculos || 0) : 0,
        mascotas: aceptaMascotas ? mascotas : 0,
      }))
    } catch (causa) {
      setFalla(fallaDe(causa))
      setEnviando(false)
      window.scrollTo?.({ top: 0 })
    }
  }

  const salidas = (
    <div className="paso-salidas">
      <button type="button" className="boton" onClick={alElegirOtro}>Elegir otro apartamento</button>
      <button type="button" className="boton boton-secundario" onClick={alCambiarFechas}>Cambiar fechas</button>
      <button type="button" className="boton boton-secundario" onClick={alRevisarPersonas}>Revisar personas</button>
    </div>
  )

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
          {falla.regla && salidas}
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
            {fallaCotizar.regla && salidas}
          </div>
        )}
        {valor && <TablaDesglose idTitulo="titulo-desglose-nueva" desglose={valor.desglose} total={valor.total} />}
      </section>

      <section className="tarjeta paso-tarjeta" aria-labelledby="titulo-llegada">
        <h3 id="titulo-llegada">Llegada, vehículos y mascotas</h3>
        <div className="paso-campos-3">
          <Campo
            etiqueta="Hora estimada de llegada"
            nota="opcional"
            type="time"
            value={llegada.hora}
            disabled={enviando}
            onChange={(evento) => alCambiarLlegada({ ...llegada, hora: evento.target.value })}
          />
          {cupoParqueadero > 0 && (
            <Campo
              etiqueta="Número de vehículos"
              inputMode="numeric"
              autoComplete="off"
              value={llegada.vehiculos}
              disabled={enviando}
              pista={`El parqueadero tiene ${cantidad(cupoParqueadero, 'cupo', 'cupos')} en total, compartidos entre las reservas.`}
              onChange={(evento) => alCambiarLlegada({ ...llegada, vehiculos: evento.target.value.replace(/\D/g, '').slice(0, 3) })}
            />
          )}
          {aceptaMascotas && (
            <Campo
              etiqueta="Número de mascotas"
              inputMode="numeric"
              autoComplete="off"
              value={llegada.mascotas}
              disabled={enviando}
              error={errores.mascotas}
              pista={`Este hotel acepta hasta ${cantidad(limiteMascotas, 'mascota', 'mascotas')}, con un cargo de ${dinero(cargoMascota)} por mascota, una vez por estancia.`}
              onChange={(evento) => {
                setErrores({})
                alCambiarLlegada({ ...llegada, mascotas: evento.target.value.replace(/\D/g, '').slice(0, 3) })
              }}
            />
          )}
        </div>
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
            <p>
              <span className="paso-total-rotulo">Total</span>
              <strong className="codigo">{dinero(valor.total)}</strong>
              {cargoMascotas > 0 && (
                <span className="paso-total-extra">
                  Más <span className="codigo">{dinero(cargoMascotas)}</span> por mascotas, que van aparte en el pago.
                </span>
              )}
            </p>
          )}
          <button type="submit" className="boton" disabled={enviando || !valor}>
            {enviando ? 'Creando reserva…' : 'Crear reserva'}
          </button>
        </div>
      </div>
    </form>
  )
}
