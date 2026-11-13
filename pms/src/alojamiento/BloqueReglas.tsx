import { useCallback, type ReactNode } from 'react'
import { guardarParametros, type Alojamiento } from '../api/alojamiento'
import Aviso from '../componentes/Aviso'
import { Bloque, CampoBloque, Opciones } from './Bloque'
import { useBloque, type AlBloque } from './useBloque'
import { cuerpoReglas, formReglas, validarReglas, type CampoReglas, type FormReglas } from './formularios'

function Grupo({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section className="grupo-reglas" aria-label={titulo}>
      <h3 className="grupo-reglas-titulo">{titulo}</h3>
      <div className="campos-bloque">{children}</div>
    </section>
  )
}

/**
 * Horarios, anticipo, plazos, edad que paga, parqueadero, mascotas y los mínimos del hotel (CU-51). La API los recibe
 * todos juntos y los reemplaza, así que hay un solo botón y siempre se envían los 21, aunque mascotas esté oculto.
 */
export default function BloqueReglas(props: AlBloque) {
  const desde = useCallback((alojamiento: Alojamiento) => formReglas(alojamiento.parametros), [])
  const { form, cambiar, errores, estado, rechazo, sucio, guardar } = useBloque(props, { desde, validar: validarReglas })

  /** Un número con su unidad y la línea que explica su efecto. */
  function numero(campo: Exclude<CampoReglas, 'pagoEnLinea' | 'cobroParqueadero' | 'aceptaMascotas'>, etiqueta: string,
    pista: string, unidad: { antes?: string; despues?: string } = {}) {
    return (
      <CampoBloque etiqueta={etiqueta} valor={form[campo]} error={errores[campo]} pista={pista} inputMode="numeric"
        {...unidad} alCambiar={(valor) => cambiar(campo, valor as FormReglas[typeof campo])} />
    )
  }

  function horaDe(campo: 'horaEntrada' | 'horaSalida' | 'horaLimiteNoShow', etiqueta: string, pista: string) {
    return (
      <CampoBloque etiqueta={etiqueta} type="time" valor={form[campo]} error={errores[campo]} pista={pista}
        alCambiar={(valor) => cambiar(campo, valor)} />
    )
  }

  return (
    <Bloque
      titulo="Reglas de la estancia"
      descripcion="Se guardan todas juntas con un solo botón."
      estado={estado}
      sucio={sucio}
      rechazo={rechazo}
      guardado="Reglas guardadas"
      boton="Guardar reglas"
      alEnviar={(evento) => guardar(evento, 'No se guardaron las reglas', (f) => guardarParametros(cuerpoReglas(f)))}
    >
      <Aviso tipo="info" asunto="Aplica a las reservas nuevas"
        mensaje="Los cambios de anticipo, cargos y horas aplican a las reservas que se hagan desde ahora. Lo ya reservado no cambia." />

      <Grupo titulo="Horarios">
        {horaDe('horaEntrada', 'Hora de entrada', 'Desde esta hora el huésped puede ocupar el apartamento.')}
        {horaDe('horaSalida', 'Hora de salida', 'Hasta esta hora el huésped puede quedarse el día de la salida.')}
        {horaDe('horaLimiteNoShow', 'Hora límite de llegada',
          'Después de esta hora, una reserva pendiente ya no se confirma y una confirmada puede declararse no-show.')}
        {numero('tiempoPreparacionHoras', 'Tiempo de preparación entre reservas',
          'Tiempo que queda libre entre una salida y la siguiente llegada al mismo apartamento.', { despues: 'horas' })}
      </Grupo>

      <Grupo titulo="Reservas y pagos">
        {numero('anticipoPct', 'Anticipo para confirmar', 'Parte del total que se debe pagar para que una reserva quede confirmada. 0 = no se exige.', { despues: '%' })}
        {numero('plazoConfirmacionHoras', 'Plazo para confirmar una pendiente', 'Tiempo que tiene una reserva pendiente para pagar el anticipo.', { despues: 'horas' })}
        <Opciones pregunta="Pago en línea" pista="Lo que se le pide pagar a quien reserva por la web."
          valor={form.pagoEnLinea} opciones={[['ANTICIPO', 'Anticipo'], ['TOTAL', 'Total']]}
          alCambiar={(valor) => cambiar('pagoEnLinea', valor)} />
        {numero('plazoPagoEnLineaMinutos', 'Plazo del pago en línea', 'Tiempo que tiene quien reserva por la web para terminar el pago.', { despues: 'minutos' })}
        {numero('umbralEdadFacturable', 'Edad desde la que se cobra', 'Los huéspedes de esta edad en adelante pagan como ocupantes; los menores no.', { despues: 'años' })}
      </Grupo>

      <Grupo titulo="Parqueadero">
        {numero('cupoParqueadero', 'Cupos', 'Carros y motos que caben a la vez. Escribe 0 si no hay parqueadero.')}
        <Opciones pregunta="Cobro" pista="Por noche se multiplica por las noches; por estadía se cobra una sola vez."
          valor={form.cobroParqueadero} opciones={[['POR_NOCHE', 'Por noche'], ['POR_ESTADIA', 'Por estadía']]}
          alCambiar={(valor) => cambiar('cobroParqueadero', valor)} />
        {numero('cargoCarro', 'Valor por carro', 'Lo que se cobra por cada carro. 0 = no se cobra.', { antes: '$' })}
        {numero('cargoMoto', 'Valor por moto', 'Lo que se cobra por cada moto. 0 = no se cobra.', { antes: '$' })}
      </Grupo>

      <Grupo titulo="Mascotas">
        <Opciones pregunta="Acepta mascotas" pista="Si eliges No, se ocultan el máximo y el cargo."
          valor={form.aceptaMascotas ? 'si' : 'no'} opciones={[['si', 'Sí'], ['no', 'No']]}
          alCambiar={(valor) => cambiar('aceptaMascotas', valor === 'si')} />
        {form.aceptaMascotas && (
          <>
            {numero('limiteMascotas', 'Máximo por reserva', 'Cantidad de mascotas que se admite en una reserva.')}
            {numero('cargoMascota', 'Cargo por estancia', 'Se cobra una vez por estancia, por cada mascota. 0 = no se cobra.', { antes: '$' })}
          </>
        )}
      </Grupo>

      <details className="avanzado" open={MINIMOS.some((campo) => errores[campo]) || undefined}>
        <summary>
          <span className="avanzado-titulo">Avanzado · Mínimos del hotel</span>
          <span className="avanzado-pista">
            Medios de pago, servicios adicionales, temporadas, tramos de la política y capacidades distintas que exige el
            hotel. Valores de la ficha del curso; un hotel real puede bajarlos.
          </span>
        </summary>
        <div className="campos-bloque">
          {numero('minimoMediosPago', 'Medios de pago', 'Debe haber al menos esta cantidad de medios de pago habilitados.')}
          {numero('minimoServiciosAdicionales', 'Servicios adicionales activos', 'Debe haber al menos esta cantidad de servicios activos.')}
          {numero('minimoTemporadas', 'Temporadas', 'Debe haber al menos esta cantidad de temporadas.')}
          {numero('minimoTramosCancelacion', 'Tramos de la política de cancelación', 'Cada versión nueva de la política debe tener al menos estos tramos. 1 o más.')}
          {numero('minimoCapacidadesDistintas', 'Capacidades distintas de apartamentos', 'Debe haber apartamentos para al menos esta cantidad de capacidades distintas.')}
        </div>
      </details>
    </Bloque>
  )
}

const MINIMOS = ['minimoMediosPago', 'minimoServiciosAdicionales', 'minimoTemporadas', 'minimoTramosCancelacion',
  'minimoCapacidadesDistintas'] as const
