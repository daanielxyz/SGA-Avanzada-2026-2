import { guardarIva } from '../api/alojamiento'
import { dinero } from '../formato'
import { Bloque, CampoBloque, Opciones } from './Bloque'
import { cuerpoIva, formIva, validarIva } from './formularios'
import { useBloque, type AlBloque } from './useBloque'

// La tarifa del ejemplo: una noche de $ 100.000
const EJEMPLO = 100_000

/**
 * El IVA de la estancia (DEC-133 · DEC-134 · DEC-141). Si se cobra, cada reserva nueva lleva el porcentaje sobre el
 * valor de las noches y el anticipo se calcula sobre el total con IVA. Las tarifas pueden ser el valor sin IVA (se le
 * suma) o el precio final (ya lo traen dentro). Exentos los que llegan con pasaporte; consumos y extras no llevan IVA.
 */
export default function BloqueIva(props: AlBloque) {
  const { form, cambiar, errores, estado, rechazo, sucio, guardar } = useBloque(props, { desde: formIva, validar: validarIva })
  return (
    <Bloque
      titulo="IVA de la estancia"
      descripcion="Si lo cobras, cada reserva muestra el alojamiento, el IVA y el total, y el anticipo se calcula sobre el total."
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
        {/* Apagado se oculta, pero se conserva y se envía: al volver a encenderlo sigue igual */}
        {form.cobraIva && (
          <>
            <CampoBloque etiqueta="Porcentaje" valor={form.porcentaje} despues="%" error={errores.porcentaje}
              inputMode="numeric" autoComplete="off" pista="En Colombia, el 19 %."
              alCambiar={(valor) => cambiar('porcentaje', valor.replace(/\D/g, '').slice(0, 3))} />
            <Opciones pregunta="Las tarifas que escribes"
              pista={ejemplo(form.incluidoEnTarifa, Number(form.porcentaje) || 0)}
              valor={form.incluidoEnTarifa ? 'incluido' : 'sumado'}
              opciones={[['sumado', 'Son sin IVA: se les suma'], ['incluido', 'Ya incluyen el IVA']]}
              alCambiar={(valor) => cambiar('incluidoEnTarifa', valor === 'incluido')} />
          </>
        )}
      </div>
    </Bloque>
  )
}

/** Cómo queda una noche de $ 100.000 con la opción elegida, redondeado al peso como el backend. */
function ejemplo(incluido: boolean, porcentaje: number): string {
  if (incluido) {
    const base = Math.round((EJEMPLO * 100) / (100 + porcentaje))
    return `Una noche de ${dinero(EJEMPLO)}: el huésped paga ${dinero(EJEMPLO)} (${dinero(base)} de alojamiento y `
      + `${dinero(EJEMPLO - base)} de IVA). Con pasaporte paga ${dinero(base)}.`
  }
  const iva = Math.round((EJEMPLO * porcentaje) / 100)
  return `Una noche de ${dinero(EJEMPLO)}: el huésped paga ${dinero(EJEMPLO + iva)} (${dinero(EJEMPLO)} de alojamiento `
    + `y ${dinero(iva)} de IVA). Con pasaporte paga ${dinero(EJEMPLO)}.`
}
