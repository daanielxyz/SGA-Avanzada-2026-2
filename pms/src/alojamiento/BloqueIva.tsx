import { guardarIva } from '../api/alojamiento'
import { Bloque, CampoBloque, Opciones } from './Bloque'
import { cuerpoIva, formIva, validarIva } from './formularios'
import { useBloque, type AlBloque } from './useBloque'

/**
 * El IVA de la estancia (DEC-133 · DEC-134). Las tarifas son la base, sin IVA; si se cobra, cada reserva nueva lleva
 * el porcentaje sobre el valor de las noches y el anticipo se calcula sobre el total con IVA. Exentos los que llegan
 * con pasaporte; consumos y extras no llevan IVA.
 */
export default function BloqueIva(props: AlBloque) {
  const { form, cambiar, errores, estado, rechazo, sucio, guardar } = useBloque(props, { desde: formIva, validar: validarIva })
  return (
    <Bloque
      titulo="IVA de la estancia"
      descripcion="Las tarifas son el valor sin IVA. Si lo cobras, cada reserva muestra el alojamiento, el IVA y el total, y el anticipo se calcula sobre el total."
      estado={estado}
      sucio={sucio}
      rechazo={rechazo}
      guardado="IVA guardado"
      boton="Guardar IVA"
      alEnviar={(evento) => guardar(evento, 'No se guardó el IVA', (f) => guardarIva(cuerpoIva(f)))}
    >
      <div className="campos-bloque">
        <Opciones pregunta="Cobrar IVA de la estancia"
          pista="Rige para las reservas nuevas: las ya creadas no cambian. Solo sobre las noches; quien llega con pasaporte no lo paga."
          valor={form.cobraIva ? 'si' : 'no'} opciones={[['si', 'Sí'], ['no', 'No']]}
          alCambiar={(valor) => cambiar('cobraIva', valor === 'si')} />
        {/* Apagado se oculta, pero se conserva y se envía: al volver a encenderlo sigue el mismo porcentaje */}
        {form.cobraIva && (
          <CampoBloque etiqueta="Porcentaje" valor={form.porcentaje} despues="%" error={errores.porcentaje}
            inputMode="numeric" autoComplete="off" pista="En Colombia, el 19 %."
            alCambiar={(valor) => cambiar('porcentaje', valor.replace(/\D/g, '').slice(0, 3))} />
        )}
      </div>
    </Bloque>
  )
}
