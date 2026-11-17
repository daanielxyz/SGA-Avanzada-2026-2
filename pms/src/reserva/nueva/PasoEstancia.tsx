import { useState, type FormEvent } from 'react'
import Aviso from '../../componentes/Aviso'
import { cantidad, noches } from '../../formato'
import Campo from './Campo'
import { sinError, validarEstancia, type DatosEstancia, type Errores } from './formulario'
import SelectorEstancia from './SelectorEstancia'

/** Paso 1: cuándo y cuántos. */
export default function PasoEstancia({ datos, alCambiar, alSeguir }: {
  datos: DatosEstancia
  alCambiar: (datos: DatosEstancia) => void
  alSeguir: () => void
}) {
  const [errores, setErrores] = useState<Errores>({})
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
      {hayErrorDeFechas && <Aviso tipo="error" asunto="Faltan las fechas" mensaje="Elígelas en el calendario: el aviso está junto a cada una." />}
      <div className="paso-campos-3">
        <SelectorEstancia
          estancia={{ entrada: datos.entrada, salida: datos.salida }}
          errores={errores}
          alCambiar={({ entrada, salida }) => {
            setErrores((actuales) => sinError(sinError(actuales, 'entrada'), 'salida'))
            alCambiar({ ...datos, entrada, salida })
          }}
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
