import { useState, type FormEvent } from 'react'
import Aviso from '../../componentes/Aviso'
import { cantidad, fechaHoy, noches } from '../../formato'
import Campo from './Campo'
import { sinError, validarEstancia, type DatosEstancia, type Errores } from './formulario'

/** Paso 1: cuándo y cuántos. */
export default function PasoEstancia({ datos, alCambiar, alSeguir }: {
  datos: DatosEstancia
  alCambiar: (datos: DatosEstancia) => void
  alSeguir: () => void
}) {
  const [errores, setErrores] = useState<Errores>({})
  const hoy = fechaHoy()
  const validas = datos.entrada && datos.salida > datos.entrada
  const hayErrorDeFechas = Boolean(errores.entrada || errores.salida)

  function cambiar(campo: keyof DatosEstancia, valor: string) {
    setErrores((actuales) => sinError(actuales, campo))
    alCambiar({ ...datos, [campo]: valor })
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const encontrados = validarEstancia(datos)
    setErrores(encontrados)
    if (Object.keys(encontrados).length === 0) alSeguir()
  }

  return (
    <form className="tarjeta paso-tarjeta" onSubmit={enviar} noValidate>
      <h2 tabIndex={-1}>¿Cuándo y cuántos?</h2>
      {hayErrorDeFechas && <Aviso tipo="error" asunto="Revisa las fechas marcadas" mensaje="Los errores están junto a cada fecha." />}
      <div className="paso-campos-3">
        <Campo
          etiqueta="Entrada"
          type="date"
          min={hoy}
          value={datos.entrada}
          error={errores.entrada}
          onChange={(evento) => cambiar('entrada', evento.target.value)}
        />
        <Campo
          etiqueta="Salida"
          type="date"
          min={datos.entrada || hoy}
          value={datos.salida}
          error={errores.salida}
          pista="La noche de salida no se cobra."
          onChange={(evento) => cambiar('salida', evento.target.value)}
        />
        <Campo
          etiqueta="Número de ocupantes"
          inputMode="numeric"
          autoComplete="off"
          value={datos.ocupantes}
          error={errores.ocupantes}
          onChange={(evento) => cambiar('ocupantes', evento.target.value.replace(/\D/g, '').slice(0, 3))}
        />
      </div>
      <p>
        <span className="etiqueta">
          {validas ? cantidad(noches(datos.entrada, datos.salida), 'noche', 'noches') : 'Noches: —'}
        </span>
      </p>
      <div className="paso-pie paso-pie-final">
        <button type="submit" className="boton">
          Siguiente: apartamento <span aria-hidden="true">→</span>
        </button>
      </div>
    </form>
  )
}
